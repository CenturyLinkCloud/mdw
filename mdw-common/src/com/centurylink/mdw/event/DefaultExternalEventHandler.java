/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.process.PackageAware;
import com.centurylink.mdw.model.value.process.PackageVO;

public class DefaultExternalEventHandler implements ExternalEventHandler, PackageAware, RegisteredService, ExternalEventHandlerErrorResponse {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    private PackageVO pkg;
    public PackageVO getPackage() { return pkg; }
    public void setPackage(PackageVO pkg) { this.pkg = pkg; }

    /*
     * This handleEventMessage is for future use, whenever we determine that a
     * message never actually matches any event handler
     *
     * @see
     * com.centurylink.mdw.event.ExternalEventHandler#handleEventMessage(java
     * .lang.String, java.lang.Object, java.util.Map)
     */
    @Override
    public String handleEventMessage(String msg, Object msgobj, Map<String, String> metainfo)
            throws EventHandlerException {
        if (logger.isDebugEnabled())
            logger.debug("DefaultExternalEventHandler message:\n" + msg);

        throw new EventHandlerException("This is a test exception");
    }

    /*
     * This method is called whenever an unparseable message is received by the
     * ListenerHelper. <p> Default functionality is to send a standard MDW error
     * message </p>
     *
     * @see
     * com.centurylink.mdw.listener.ListenerHelper#processEvent(java.lang.String
     * , java.util.Map)
     *
     * @see com.centurylink.mdw.event.ExternalEventHandlerErrorResponse#
     * createErrorResponse(java.lang.String, java.util.Map, java.lang.Throwable)
     */
    @Override
    public String createErrorResponse(String request, Map<String, String> metaInfo,
            Throwable eventHandlerException) {
        if (request.charAt(0) == '{') {
            return createStandardResponseJson(-1,
                    "Cannot parse request", metaInfo.get(Listener.METAINFO_REQUEST_ID));
        }
        else {
            return createStandardResponse(-1,
                    "Cannot parse request", metaInfo.get(Listener.METAINFO_REQUEST_ID));
        }
    }

    public String createStandardResponse(int statusCode, String statusMessage, String requestId) {
        MDWStatusMessageDocument responseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMsg = responseDoc.addNewMDWStatusMessage();
        statusMsg.setStatusCode(statusCode);
        if (statusMessage != null)
            statusMsg.setStatusMessage(statusMessage);
        if (requestId != null)
            statusMsg.setRequestID(requestId);
        return responseDoc.xmlText(new XmlOptions().setSavePrettyPrint()
                .setSavePrettyPrintIndent(2));
    }

    public String createStandardResponseJson(int statusCode, String statusMessage, String requestId) {
        JSONObject responseObj = new JSONObject();
        String response;
        try {
            responseObj.put("StatusCode", statusCode);
            if (statusMessage != null)
                responseObj.put("StatusMessage", statusMessage);
            MDWStatusMessageDocument responseDoc = MDWStatusMessageDocument.Factory.newInstance();
            MDWStatusMessage statusMsg = responseDoc.addNewMDWStatusMessage();
            statusMsg.setStatusCode(statusCode);
            if (statusMessage != null)
                statusMsg.setStatusMessage(statusMessage);
            if (requestId != null)
                responseObj.put("RequestID", requestId);
            response = responseObj.toString();
        }
        catch (JSONException e) {
            response = "{\"ERROR\":\"error in generating JSON response\"}";
        }
        return response;
    }

}
