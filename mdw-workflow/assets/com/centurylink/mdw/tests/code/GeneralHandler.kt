package com.centurylink.mdw.tests.code

import com.centurylink.mdw.annotations.Handler
import com.centurylink.mdw.model.request.Request
import com.centurylink.mdw.model.request.Response
import com.centurylink.mdw.request.RequestHandler
import com.centurylink.mdw.request.RequestHandlerException
import com.centurylink.mdw.services.request.BaseHandler
import org.json.JSONObject

@Handler(match=RequestHandler.Routing.Path, path="/test/KtGeneralHandler")
class GeneralHandler : BaseHandler() {

    override fun handleRequest(request: Request, message: Any, headers: Map<String,String>): Response {
        val responseJson = JSONObject()
        responseJson.put("greeting", "Hello, ${(message as JSONObject).getString("name")}")
        val response = Response(responseJson.toString(2))
        response.statusCode = 201
        return response
    }
}