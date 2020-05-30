package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl
import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.model.StatusResponse

@Activity(value="Microservice Error Handler", icon="com.centurylink.mdw.microservice/error.png")
class ErrorHandlerActivity : DefaultActivityImpl() {

    // TODO no condition
    override fun execute(runtimeContext: ActivityRuntimeContext): Any? {
        val isSynchronous = runtimeContext.values["synchronous"]
        val serviceResponse = runtimeContext.values["serviceResponse"]
        if (isSynchronous == true && serviceResponse is StatusResponse) {
            // let the response propagate back to the caller
            return null
        }
        else {
            runtimeContext.logError("RUNTIME CONTEXT: " + runtimeContext.json.toString(2))
            return null // TODO
        }
    }
}