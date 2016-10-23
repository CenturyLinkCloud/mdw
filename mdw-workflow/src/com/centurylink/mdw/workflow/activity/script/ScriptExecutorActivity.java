/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.script;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.ScriptActivity;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Executes a script.
 */
@Tracked(LogLevel.TRACE)
public class ScriptExecutorActivity extends DefaultActivityImpl implements ScriptActivity {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String RULE = "Rule";
    public static final String SCRIPT_LANGUAGE = "SCRIPT";

   /**
     * Execute rules in Magic Box rule language.
     *
     * @throws ActivityException
     */
    public void execute() throws ActivityException {

        try {
            String language = getAttributeValue(SCRIPT_LANGUAGE);
            if (language == null)
                language = GROOVY;
            String script = getAttributeValue(RULE);

            if (StringHelper.isEmpty(script)){
                throw new  ActivityException("Script content has not been defined");
            }

            Object retObj = executeScript(script, language);

            if (retObj != null)
                setReturnCode(retObj.toString());
        }
        catch (ActivityException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }
}
