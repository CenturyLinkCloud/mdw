/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.script;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.groovy.GroovyNaming;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractEvaluator;

/**
 * Class that evaluates the value of a script expression
 */
@Tracked(LogLevel.TRACE)
public class ScriptEvaluator extends AbstractEvaluator  {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String EXPRESSION = "Expression";
    public static final String SCRIPT_LANGUAGE = "SCRIPT";

	private String scriptLanguage;
	private String expression;

   /**
     * Evaluates a condition and returns the result of the evaluation. The condition is an expression
     * in a supported script/rule language.  WorkTransition can be controlled using the result.
     */
	@Override
    public Object evaluate() throws ActivityException {

        try {
            scriptLanguage = getAttributeValue(SCRIPT_LANGUAGE);
            expression = getAttributeValue(EXPRESSION);
            if (StringHelper.isEmpty(expression)){
                throw new ActivityException("Expression content has not been defined");
            }

            String name = GroovyNaming.getValidClassName(getActivityName() + "_" + getActivityId());
            String executor = getProperty("MDWFramework.ScriptExecutors/Groovy");
            Object obj = null;
            if ("com.centurylink.mdw.script.GroovyExecutorCompatible".equals(executor)) {
                Map<String,Object> addlBindings = new HashMap<String,Object>();
                addlBindings.put("activity", this);
                obj = evaluateExpression(name, scriptLanguage, expression, addlBindings);
            }
            else {
                obj = evaluateExpression(name, scriptLanguage, expression);
                if ((obj == null || obj.toString().isEmpty()) && isBooleanExpression(scriptLanguage, expression))
                    obj = Boolean.FALSE;
            }

            return obj;
        }
        catch (ExecutionException ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

	protected boolean isBooleanExpression(String language, String expression) throws ActivityException {
        for (Variable varVO: getMainProcessDefinition().getVariables()) {
            if (Boolean.class.getName().equals(varVO.getVariableType())) {
                if (JAVA_EL.equals(language)) {
                    if (expression.equals("#{" + varVO.getName() + "}"))
                        return true;
                }
                else {
                    if (expression.equals(varVO.getName()))
                        return true;
                }
            }
        }
        return false;
	}
}
