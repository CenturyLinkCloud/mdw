package com.centurylink.mdw.tests.code

import com.centurylink.mdw.annotations.Handler
import com.centurylink.mdw.model.request.Request
import com.centurylink.mdw.request.RequestHandler
import com.centurylink.mdw.services.request.ProcessRunHandler

@Handler(match=RequestHandler.Routing.Path, path="/test/KtProcessHandler")
class KtProcessHandler : ProcessRunHandler() {

    override fun getProcess(request: Request, message: Any, headers: Map<String,String>): String {
        return "com.centurylink.mdw.tests.code/RequestHandlers.proc"
    }
}