package com.centurylink.mdw.tests.code

import com.centurylink.mdw.annotations.Handler
import com.centurylink.mdw.model.request.Request
import com.centurylink.mdw.request.RequestHandler
import com.centurylink.mdw.services.request.ProcessNotifyHandler
import org.json.JSONObject

@Handler(match=RequestHandler.Routing.Path, path="/test/KtNotifyHandler")
class KtNotifyHandler : ProcessNotifyHandler() {

    override fun getEventName(request: Request, message: Any, headers: Map<String,String>): String {
        val masterRequestId = (message as JSONObject).getString("masterRequestId")
        return "RequestHandlers-${masterRequestId}"
    }
}