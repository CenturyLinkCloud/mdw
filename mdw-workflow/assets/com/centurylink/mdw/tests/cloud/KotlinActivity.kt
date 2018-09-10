package com.centurylink.mdw.tests.cloud

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.util.log.StandardLogger
import com.centurylink.mdw.util.timer.Tracked
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl
import com.centurylink.mdw.annotations.Activity;

@Activity("KotlinActivity")
class KotlinActivity : DefaultActivityImpl() {

    override fun execute(runtimeContext: ActivityRuntimeContext): Any? {
        setValue("updated", "updated from kotlin")
        return null
    }
}