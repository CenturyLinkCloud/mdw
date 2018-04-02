/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.Map;

import org.apache.camel.Message;
import org.apache.camel.component.cxf.CxfPayload;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.service.types.Status;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.xml.DomHelper;

public class DefaultNotifyHandler extends ExternalEventHandlerBase implements NotifyHandler {

    public static final int DEFAULT_DELAY = 2;

    public String getRequestDocumentType(Message request) throws MdwCamelException {
        return StringDocument.class.getName();
    }

    public Object initializeRequestDocument(Message request) throws MdwCamelException {
        return request.getBody(String.class);
    }

    public String getEventId(Message request) {
        return request.getHeader(Listener.METAINFO_EVENT_ID, String.class);
    }

    public Object notify(String eventId, Long docId, Message request, int delay) {
        try {
            String requestStr = null;
            if ("org.apache.camel.component.cxf.CxfPayload".equals(request.getBody().getClass().getName())) {
                // special handling to extract XML
                @SuppressWarnings("rawtypes")
                CxfPayload cxfPayload = (CxfPayload) request.getBody();
                requestStr = DomHelper.toXml((Node)cxfPayload.getBody().get(0));
            }
            else {
                requestStr = request.getBody(String.class);
            }

            notifyProcesses(eventId, docId, requestStr, delay);
            return getResponse(0, "Success");
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return getResponse(1, ex.toString());
        }
    }

    public String getResponse(int code, String message) {
        try {
            Status status = new Status();
            status.setCode(code);
            status.setMessage(message);
            return marshalJaxb(status, null);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return ex.toString();  // what else can we do
        }
    }

    public int getDelay() {
        int delay = DEFAULT_DELAY;
        String av = PropertyManager.getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
        if (av != null) {
            try {
                delay = Integer.parseInt(av);
                if (delay < 0)
                    delay = 0;
                else if (delay > 300)
                    delay = 300;
            }
            catch (Exception ex) {
                logger.severe("Activity resume delay spec is not an integer");
            }
        }
        return delay;
    }

    /**
     * This is for non-Camel style event handlers.
     * It is not used here.  Overriding has no effect in the context of a Camel route.
     */
    public String handleEventMessage(String message, Object messageObj, Map<String,String> metaInfo) throws EventHandlerException {
        return null;
    }
}
