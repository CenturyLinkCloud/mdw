package com.centurylink.mdw.services.request;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.service.data.ServicePaths;

import java.util.Map;

/**
 * Standard error response.
 */
public class ErrorResponse extends Response {

    public ErrorResponse(String requestContent, Map<String,String> headers, ServiceException cause) {
        String contentType = headers.get(Listener.METAINFO_CONTENT_TYPE);
        if (contentType == null && requestContent != null && !requestContent.isEmpty())
            contentType = requestContent.trim().startsWith("{") ? Listener.CONTENT_TYPE_JSON : Listener.CONTENT_TYPE_XML;
        if (contentType == null)
            contentType = Listener.CONTENT_TYPE_XML; // compatibility

        headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(cause.getCode()));

        StatusMessage statusMsg = new StatusMessage();
        statusMsg.setCode(cause.getCode());
        statusMsg.setMessage(cause.getMessage());
        setStatusCode(statusMsg.getCode());
        setStatusMessage(statusMsg.getMessage());
        setPath(ServicePaths.getInboundResponsePath(headers));
        if (contentType.equals(Listener.CONTENT_TYPE_JSON)) {
            setContent(statusMsg.getJsonString());
        }
        else {
            setContent(statusMsg.getXml());
        }
    }
}
