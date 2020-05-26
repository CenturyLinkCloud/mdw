package com.centurylink.mdw.services.request;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.request.RequestHandlerException;

import java.util.Map;

/**
 * Handler for notifying a waiting workflow process.
 */
public abstract class ProcessNotifyHandler extends BaseHandler {

    @Override
    public Response handleRequest(Request request, Object message, Map<String,String> headers) {
        try {
            String eventName = getEventName(request, message, headers);
            Long requestId = new Long(headers.get(Listener.METAINFO_DOCUMENT_ID));
            int delay = PropertyManager.getIntegerProperty(PropertyNames.ACTIVITY_RESUME_DELAY, 2);
            Integer status = notifyProcesses(eventName, requestId, request.getContent(), delay);
            return new Acknowledgement(request.getContent(), headers, "Notified process with result: " + status);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return getErrorResponse(request, message, headers, ex);
        }
    }

    /**
     * Unique event name to send.
     */
    protected abstract String getEventName(Request request, Object message, Map<String,String> headers)
            throws RequestHandlerException;
}
