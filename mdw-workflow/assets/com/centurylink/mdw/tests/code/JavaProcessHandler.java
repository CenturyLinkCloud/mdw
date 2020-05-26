package com.centurylink.mdw.tests.code;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.services.request.ProcessRunHandler;

import java.util.Map;

@Handler(match=RequestHandler.Routing.Path, path="/test/JavaProcessHandler")
public class JavaProcessHandler extends ProcessRunHandler {

    @Override
    public String getProcess(Request request, Object message, Map<String,String> headers) {
        return "com.centurylink.mdw.tests.code/RequestHandlers.proc";
    }
}
