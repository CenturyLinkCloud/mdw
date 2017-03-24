/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.event;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.PackageAware;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class DefaultExternalEventHandler implements ExternalEventHandler, PackageAware, RegisteredService, ExternalEventHandlerErrorResponse {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

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
