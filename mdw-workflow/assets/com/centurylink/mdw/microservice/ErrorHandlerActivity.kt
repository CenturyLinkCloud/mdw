package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.util.log.StandardLogger
import com.centurylink.mdw.util.timer.Tracked
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl
import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.common.service.ServiceException
import com.centurylink.mdw.model.StatusResponse

@Activity(value="Microservice Error Handler", icon="com.centurylink.mdw.microservice/error.png")
class ErrorHandlerActivity : DefaultActivityImpl() {

    override fun execute(runtimeContext: ActivityRuntimeContext): Any? {
        val isSynchronous = runtimeContext.variables["synchronous"]
        val serviceResponse = runtimeContext.variables["serviceResponse"]
        if (isSynchronous == true && serviceResponse is StatusResponse) {
            // let the response propagate back to the caller
            return null
        }
        else {
            System.out.println("RUNTIME CONTEXT: " + runtimeContext.json.toString(2))
            return null // TODO
        }
    }
}