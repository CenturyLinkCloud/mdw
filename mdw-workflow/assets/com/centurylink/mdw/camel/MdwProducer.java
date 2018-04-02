/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.camel.impl.DefaultProducer;
import org.w3c.dom.Element;

import com.centurylink.mdw.camel.CxfNotifyHandler;
import com.centurylink.mdw.camel.CxfProcessLaunchHandler;
import com.centurylink.mdw.camel.DefaultNotifyHandler;
import com.centurylink.mdw.camel.NotifyHandler;
import com.centurylink.mdw.camel.DefaultProcessLaunchHandler;
import com.centurylink.mdw.camel.ProcessLaunchHandler;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.xml.DomHelper;

public class MdwProducer extends DefaultProducer {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private MdwEndpoint endpoint;

    public MdwProducer(MdwEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        Message request = exchange.getIn();
        request.setHeader("MdwCamelRequestType", endpoint.getRequestType().toString());

        Object response = null;

        try {
            switch (endpoint.getRequestType()) {
                case process:
                    response = launchProcess(request);
                    break;
                case notify:
                    response = notify(request);
                    break;
                default:
                    break;
            }

        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
            exchange.setException(t);
        }

        if (exchange.getPattern().isOutCapable())
            handleResponse(exchange, response);
    }

    protected Object launchProcess(Message request) throws Exception {
        String masterRequestId = getValue(endpoint.getMasterRequestId(), request);
        if (masterRequestId != null && !masterRequestId.isEmpty())
            request.setHeader(Listener.METAINFO_MDW_REQUEST_ID, masterRequestId);

        Package pkg = null;
        String processName = endpoint.getName();
        int slash = processName.indexOf('/');
        if (slash > 0) {
            String pkgName = processName.substring(0, slash);
            pkg = PackageCache.getPackage(pkgName);
            processName = processName.substring(slash + 1);
        }
        if (processName != null && !processName.isEmpty())
            request.setHeader(Listener.METAINFO_PROCESS_NAME, processName);

        ProcessLaunchHandler launchHandler;
        String handlerClass = endpoint.getHandlerClass();
        if (handlerClass == null) {
            if ("org.apache.camel.component.cxf.CxfPayload".equals(request.getBody().getClass().getName()))
                launchHandler = new CxfProcessLaunchHandler();
            else
                launchHandler = new DefaultProcessLaunchHandler();
        }
        else {
            try {
                launchHandler = (ProcessLaunchHandler) CompiledJavaCache.getInstance(handlerClass, getClass().getClassLoader(), pkg);
                // for dynamic java workflow pkg is that of the handler
                String pkgName = handlerClass.substring(0, handlerClass.lastIndexOf('.'));
                pkg = PackageCache.getPackage(pkgName);
            }
            catch (ClassNotFoundException ex) {
                // not located as dynamic java
                    throw ex;
            }
        }
        launchHandler.setPackage(pkg);

        try {
            String docType = launchHandler.getRequestDocumentType(request);
            Object requestDoc = launchHandler.initializeRequestDocument(request);
            DocumentReference docRef = storeDocument(docType, requestDoc, pkg);
            Long docId = docRef.getDocumentId();
            request.setHeader(Listener.METAINFO_DOCUMENT_ID, docId.toString());

            Process process = launchHandler.getProcess(request);
            request.setHeader(Listener.METAINFO_PROCESS_NAME, process.getName());
            masterRequestId = launchHandler.getMasterRequestId(request);
            request.setHeader(Listener.METAINFO_MDW_REQUEST_ID, masterRequestId);

            Map<String,Object> parameters = launchHandler.getProcessParameters(request);
            if (parameters == null)
                parameters = new HashMap<String,Object>();

            String requestVarName = launchHandler.getRequestVariable();
            if (requestVarName != null) {
                Variable requestVar = process.getVariable(requestVarName);
                if (requestVar != null) {
                    if (!VariableTranslator.isDocumentReferenceVariable(pkg, requestVar.getType()))
                        throw new MdwCamelException("Process variable 'request' for process '" + process.getName() + "' must be Document type");
                    Integer varCat = requestVar.getVariableCategory();
                    if (varCat.intValue() != Variable.CAT_INPUT && varCat.intValue() != Variable.CAT_INOUT)
                        throw new MdwCamelException("Process variable 'request' for process '" + process.getName() + "' must be Input type");
                    parameters.put(requestVarName, requestDoc);
                }
            }

            logger.info("Starting MDW process '" + process.getLabel() + "' through Camel route.");

            return launchHandler.invoke(process.getId(), docId, masterRequestId, request, parameters);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            // request.getExchange().setException(ex);
            return launchHandler.getResponse(-1, ex.toString());
        }
    }

    protected Object notify(Message request) throws Exception {
        String eventId = getValue(endpoint.getEventId(), request);
        if (eventId != null && !eventId.isEmpty())
            request.setHeader(Listener.METAINFO_EVENT_ID, eventId);

        Package pkg = null;
        NotifyHandler notifyHandler;
        String handlerClass = endpoint.getHandlerClass();
        if (handlerClass == null) {
            if ("org.apache.camel.component.cxf.CxfPayload".equals(request.getBody().getClass().getName()))
                notifyHandler = new CxfNotifyHandler();
            else
                notifyHandler = new DefaultNotifyHandler();
        }
        else {
            try {
                notifyHandler = (NotifyHandler) CompiledJavaCache.getInstance(handlerClass, getClass().getClassLoader(), null);
                // for dynamic java workflow pkg is that of the handler
                String pkgName = handlerClass.substring(0, handlerClass.lastIndexOf('.'));
                pkg = PackageCache.getPackage(pkgName);
            }
            catch (ClassNotFoundException ex) {
                    throw ex;
            }
        }

        notifyHandler.setPackage(pkg);

        try {
            String docType = notifyHandler.getRequestDocumentType(request);
            Object requestDoc = notifyHandler.initializeRequestDocument(request);
            DocumentReference docRef = storeDocument(docType, requestDoc, pkg);
            Long docId = docRef.getDocumentId();
            request.setHeader(Listener.METAINFO_DOCUMENT_ID, docId.toString());

            eventId = notifyHandler.getEventId(request);
            int delay = notifyHandler.getDelay();

            logger.info("MDW notify with event: '" + eventId + "' through Camel route.");

            return notifyHandler.notify(eventId, docId, request, delay);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            // request.getExchange().setException(ex);
            return notifyHandler.getResponse(-1, ex.toString());
        }
    }

    protected String getValue(String input, Message request) {
        if (input == null)
            return null;

        return new Expression(endpoint.getCamelContext(), input, endpoint.getNamespaces()).substitute(request);
    }

    protected DocumentReference storeDocument(String docType, Object document, Package pkg) throws MdwCamelException {
        EventServices eventMgr = ServiceLocator.getEventServices();
        try {
            String ownerType = OwnerType.LISTENER_REQUEST;
            Long ownerId = 0L;
            Long docId = eventMgr.createDocument(docType, ownerType, ownerId, document, pkg);
            return new DocumentReference(docId);
        }
        catch (DataAccessException ex) {
            throw new MdwCamelException(ex.getMessage(), ex);
        }
    }

    protected Map<String,String> getMetaInfo(Message request) {
        Map<String,String> metaInfo = new HashMap<String,String>();
        for (String key : request.getHeaders().keySet()) {
            metaInfo.put(key, request.getHeader(key, String.class));
        }
        return metaInfo;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleResponse(Exchange exchange, Object response) throws Exception {
        if ("org.apache.camel.component.cxf.CxfPayload".equals(exchange.getIn().getBody().getClass().getName())) {
            if ("org.apache.camel.component.cxf.CxfPayload".equals(response.getClass().getName())) {
                exchange.getOut().setBody(response, CxfPayload.class);
            }
            else {
                List<Element> outElements = new ArrayList<Element>();
                if (response instanceof Element) {
                    outElements.add((Element)response);
                }
                else if (response instanceof String) {
                    outElements.add(DomHelper.toDomDocument((String)response).getDocumentElement());
                }
                CxfPayload responsePayload = new CxfPayload(null, outElements);
                exchange.getOut().setBody(responsePayload);
            }
        }
        else {
            if (response instanceof String) {
                exchange.getOut().setBody(response, String.class);
            }
        }
    }

}
