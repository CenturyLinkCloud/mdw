/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.process;

import java.util.Map;

import org.apache.camel.Message;
import org.apache.camel.component.cxf.CxfPayload;
import org.w3c.dom.Node;

import com.centurylink.mdw.camel.MdwCamelException;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.service.types.Status;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.xml.DomHelper;

/**
 * Default process launch handler implementation.  Can be extended to customize parsing of the request
 * document object, and for extracting process input variables from the request message.
 */
public class DefaultProcessLaunchHandler extends ExternalEventHandlerBase implements ProcessLaunchHandler {

    public String getMasterRequestId(Message request) {
        return request.getHeader(Listener.METAINFO_MASTER_REQUEST_ID, String.class);
    }

    public ProcessVO getProcess(Message request) throws MdwCamelException {
        String processName = request.getHeader(Listener.METAINFO_PROCESS_NAME, String.class);
        return ProcessVOCache.getProcessVO(processName, 0);
    }

    public String getRequestDocumentType(Message request) throws MdwCamelException {
        ProcessVO processVO = getProcess(request);
        VariableVO var = processVO.getVariable("request");
        if (var == null)
            return StringDocument.class.getName();
        else
            return var.getVariableType();
    }

    public Object initializeRequestDocument(Message request) throws MdwCamelException {
        return request.getBody(String.class);
    }

    public Object invoke(Long processId, Long owningDocId, String masterRequestId, Message request,
            Map<String,Object> parameters) throws Exception {

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

        ProcessVO processVO = ProcessVOCache.getProcessVO(processId);

        if (processVO.getProcessType().equals(ProcessVisibilityConstant.SERVICE)) {
            String responseVarName = getResponseVariable();
            return invokeServiceProcess(processId, owningDocId, masterRequestId, requestStr, parameters, responseVarName);
        }
        else {
            Long instanceId = launchProcess(processId, owningDocId, masterRequestId, parameters);
            return getResponse(0, "Process '" + processVO.getLabel() + "' instance ID " + instanceId + " launched.");
        }
    }

    public Map<String,Object> getProcessParameters(Message request) {
        return null;
    }

    public String getRequestVariable() {
        return "request";
    }

    public String getResponseVariable() {
        return "response";
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

    /**
     * This is for non-Camel style event handlers.
     * It is not used here.  Overriding has no effect in the context of a Camel route.
     */
    public String handleEventMessage(String message, Object messageObj, Map<String,String> metaInfo) throws EventHandlerException {
        return null;
    }
}
