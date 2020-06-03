package com.centurylink.mdw.tests.variables;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.services.request.ProcessRunHandler;

import java.util.Map;

@Handler(match=RequestHandler.Routing.Path, path="/variablesTest/Jsonable")
public class JsonableRequestHandler extends ProcessRunHandler {

    @Override
    public String getProcess(Request request, Object message, Map<String,String> headers) {
        return "com.centurylink.mdw.tests.variables/Jsonable.proc";
    }

    @Override
    protected Map<String,Object> getInputValues(Request request, Object message, Map<String, String> headers) throws RequestHandlerException {
        Map<String,Object> values = super.getInputValues(request, message, headers);
        values.put("testCase", "Request Handler");
        return values;
    }
}
