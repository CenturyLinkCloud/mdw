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
     * Execute scripts in supported languages.
     */
    public void execute() throws ActivityException {

        try {
            String language = getLanguage();
            String script = getScript();
            Object retObj = executeScript(script, language, null, null);

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

    protected String getLanguage() {
        String language = getAttributeValue(SCRIPT_LANGUAGE);
        if (language == null)
            language = GROOVY;
        return language;
    }

    protected String getScript() throws ActivityException {
        String script = getAttributeValue(RULE);
        if (StringHelper.isEmpty(script)){
            throw new  ActivityException("Script content has not been defined");
        }
        return script;
    }
}
