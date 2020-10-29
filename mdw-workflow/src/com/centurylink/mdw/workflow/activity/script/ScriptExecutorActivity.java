package com.centurylink.mdw.workflow.activity.script;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.ScriptActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.apache.commons.lang.StringUtils;

/**
 * Executes a script.
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Execute Script", category=ScriptActivity.class, icon="com.centurylink.mdw.base/script.gif",
        pagelet="com.centurylink.mdw.base/scriptExecutor.pagelet")
public class ScriptExecutorActivity extends DefaultActivityImpl implements ScriptActivity {

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
            getLogger().error(ex.getMessage(), ex);
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
        if (StringUtils.isBlank(script)){
            throw new  ActivityException("Script content has not been defined");
        }
        return script;
    }
}
