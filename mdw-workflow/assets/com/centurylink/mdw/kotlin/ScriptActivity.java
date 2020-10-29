package com.centurylink.mdw.kotlin;

import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity;

@Tracked(LogLevel.TRACE)
@Activity(value="Kotlin Script", category=com.centurylink.mdw.activity.types.ScriptActivity.class,
        icon="com.centurylink.mdw.kotlin/kotlin.png", pagelet="com.centurylink.mdw.kotlin/script.pagelet")
public class ScriptActivity extends ScriptExecutorActivity {

    protected String getLanguage() {
        return "Kotlin Script";
    }

}
