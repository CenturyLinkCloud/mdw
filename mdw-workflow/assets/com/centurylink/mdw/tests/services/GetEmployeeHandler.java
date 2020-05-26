package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler.Routing;
import com.centurylink.mdw.services.request.ProcessRunHandler;

import java.util.Map;

@Handler(match=Routing.Content, path="GetEmployee")
public class GetEmployeeHandler extends ProcessRunHandler {

    @Override
    public String getProcess(Request request, Object message, Map<String,String> headers) {
        return "com.centurylink.mdw.tests.services/FindEmployee.proc";
    }
}
