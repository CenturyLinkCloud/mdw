package com.centurylink.mdw.event;

import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;

import java.util.Map;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.request.RequestHandlerException}
 */
@Deprecated
public interface EventHandler extends RequestHandler {
    Response handleEventMessage(Request request, Object message, Map<String,String> headers)
    throws EventHandlerException;

    default Response handleRequest(Request request, Object message, Map<String,String> headers)
            throws RequestHandlerException {
        return handleEventMessage(request, message, headers);
    }
}
