/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.activity.script;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptNaming;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractEvaluator;
import org.apache.commons.lang.StringUtils;

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
            logger.severeException(ex.getMessage(), ex);
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
