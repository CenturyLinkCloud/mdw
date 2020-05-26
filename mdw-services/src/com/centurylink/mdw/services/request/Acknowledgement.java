package com.centurylink.mdw.services.request;

import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.service.data.ServicePaths;

import java.util.Map;

/**
 * Standard acknowledgement response.
 */
public class Acknowledgement extends Response {

    public Acknowledgement(String requestContent, Map<String,String> headers) {
        this(requestContent, headers, null);
    }

    public Acknowledgement(String requestContent, Map<String,String> headers, String responseMessage) {
        String contentType = headers.get(Listener.METAINFO_CONTENT_TYPE);
        if (contentType == null && requestContent != null && !requestContent.isEmpty())
            contentType = requestContent.trim().startsWith("{") ? Listener.CONTENT_TYPE_JSON : Listener.CONTENT_TYPE_XML;
        if (contentType == null)
            contentType = Listener.CONTENT_TYPE_XML; // compatibility

        StatusMessage statusMsg;
        if (responseMessage == null)
            statusMsg = new StatusMessage(Status.OK.getCode(), Status.OK.getMessage());
        else
            statusMsg = new StatusMessage(Status.OK.getCode(), responseMessage);

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
