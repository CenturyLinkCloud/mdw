package com.centurylink.mdw.workflow.activity.script;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptNaming;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractEvaluator;
import org.apache.commons.lang.StringUtils;

/**
 * Class that evaluates the value of a script expression
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Expression Evaluator", icon="shape:decision",
        pagelet="com.centurylink.mdw.base/scriptEvaluator.pagelet")
public class ScriptEvaluator extends AbstractEvaluator  {

    public static final String EXPRESSION = "Expression";
    public static final String SCRIPT_LANGUAGE = "SCRIPT";

    private String scriptLanguage;
    @SuppressWarnings("squid:S1845")
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
            if (StringUtils.isBlank(expression)){
                throw new ActivityException("Expression content has not been defined");
            }

            String name = ScriptNaming.getValidName(getProcessDefinition().getLabel() + "_"
                    + getActivityName() + "_" + getActivityId());
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            Object obj = evaluateExpression(name, scriptLanguage, expression);
            if ((obj == null || obj.toString().isEmpty()) && isBooleanExpression(scriptLanguage, expression))
                obj = Boolean.FALSE;
            return obj;
        }
        catch (ExecutionException ex) {
            getLogger().error(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    protected boolean isBooleanExpression(String language, String expression) throws ActivityException {
        for (Variable varVO: getMainProcessDefinition().getVariables()) {
            if (Boolean.class.getName().equals(varVO.getType())) {
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
