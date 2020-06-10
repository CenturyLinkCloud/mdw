package com.centurylink.mdw.tests.variables;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.services.request.ProcessRunHandler;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@Handler(match=RequestHandler.Routing.Path, path="/variablesTest/Gson")
public class GsonRequestHandler extends ProcessRunHandler {

    @Override
    public String getProcess(Request request, Object message, Map<String,String> headers) {
        return "com.centurylink.mdw.tests.variables/Gson.proc";
    }

    @Override
    protected Map<String,Object> getInputValues(Request request, Object message, Map<String,String> headers) {
        Map<String,Object> values = new HashMap<>();
        values.put("testCase", "Request Handler");
        values.put("request", new Mountain((JSONObject)message));
        return values;
    }
}
