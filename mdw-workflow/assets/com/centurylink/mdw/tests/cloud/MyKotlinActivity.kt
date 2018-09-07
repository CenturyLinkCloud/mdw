package com.centurylink.mdw.tests.cloud

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.util.log.StandardLogger
import com.centurylink.mdw.util.timer.Tracked
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl

@Tracked(StandardLogger.LogLevel.TRACE)
class MyKotlinActivity : DefaultActivityImpl() {

    override fun execute(runtimeContext: ActivityRuntimeContext): Any? {
        setVariableValue("stringVar", "myValue")
        return "myOutcome"
    }
}