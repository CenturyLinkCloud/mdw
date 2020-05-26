package com.centurylink.mdw.python;

import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity;

@Tracked(StandardLogger.LogLevel.TRACE)
@Activity(value="Python Script", category=com.centurylink.mdw.activity.types.ScriptActivity.class,
        icon="com.centurylink.mdw.python/python.png", pagelet="com.centurylink.mdw.python/script.pagelet")
public class ScriptActivity extends ScriptExecutorActivity {

    protected String getLanguage() {
        return "Python";
    }
}
