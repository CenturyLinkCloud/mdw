package com.centurylink.mdw.event;

import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;

import java.util.Map;

/**
 * @deprecated
 * implement {@link com.centurylink.mdw.request.RequestHandler}
 */
@Deprecated
public interface ExternalEventHandler extends RequestHandler {

    @Deprecated
    String handleEventMessage(String msg, Object msgobj, Map<String,String> metainfo)
    throws EventHandlerException;

    default Response handleRequest(Request request, Object message, Map<String,String> headers) throws RequestHandlerException {
        return new Response(handleEventMessage(request.getContent(), message, headers));
    }
}
