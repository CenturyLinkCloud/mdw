package com.centurylink.mdw.tests.code;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.services.request.ProcessNotifyHandler;
import org.json.JSONObject;

import java.util.Map;

@Handler(match=RequestHandler.Routing.Path, path="/test/JavaNotifyHandler")
public class JavaNotifyHandler extends ProcessNotifyHandler {

    @Override
    protected String getEventName(Request request, Object message, Map<String,String> headers) {
        String masterRequestId = ((JSONObject)message).getString("masterRequestId");
        return "RequestHandlers-" + masterRequestId;
    }
}
