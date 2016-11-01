/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener;

//import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.event.DefaultExternalEventHandler;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.event.EventHandlerRegistry;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.event.ExternalEventHandlerErrorResponse;
import com.centurylink.mdw.event.UnparseableMessageException;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageAware;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.service.data.event.EventHandlerCache;
import com.centurylink.mdw.service.handler.ServiceRequestHandler;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.ServiceHandler;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.centurylink.mdw.xml.XmlPath;

public class ListenerHelper {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Status code 0, often used to designate success.
     */
    public static final int RETURN_STATUS_SUCCESS = 0;
    /**
     * Status code -1, often used to designate general failure without specific
     * code.
     */
    public static final int RETURN_STATUS_FAILURE = -1;

    /**
     * Status code -2, used to indicate the error that the input data cannot be
     * parsed by the generic XML Bean.
     */
    public static final int RETURN_STATUS_NON_XML = -2;
    /**
     * Status code -3, used to indicate the error that there is no external
     * event handler configured for the message.
     */
    public static final int RETURN_STATUS_NO_HANDLER = -3;

    static final String START_PROCESS_HANDLER = "START_PROCESS";
    static final String NOTIFY_PROCESS_HANDLER = "NOTIFY_PROCESS";

    private ExternalEvent findEventHandler(String request, Object requestDoc, Map<String,String> metaInfo)
    throws XmlException, JSONException {
        if (requestDoc instanceof XmlObject) {
            XmlObject xmlBean = (XmlObject) requestDoc;
            String rootname = XmlPath.getRootNodeName(xmlBean);
            if (rootname.startsWith("_mdw_"))
                return null; // internal request
            List<ExternalEvent> bucket = EventHandlerCache.getExternalEvents(rootname);
            if (bucket != null) {
                if ("ActionRequest".equals(rootname)) {
                    // compatibility for ESOWF -- prefer qualified matches
                    ExternalEvent frameworkActionHandler = null;
                    for (ExternalEvent e : bucket) {
                        String v = e.getXpath().evaluate(xmlBean);
                        if (v != null) {
                            if ("ActionRequest".equals(e.getEventName()))
                                frameworkActionHandler = e;
                            else
                                return e;
                        }
                    }
                    if (frameworkActionHandler != null)
                        return frameworkActionHandler;
                }
                else {
                    for (ExternalEvent e : bucket) {
                        String v = e.getXpath().evaluate(xmlBean);
                        if (v != null)
                            return e;
                    }
                }
            }
            bucket = EventHandlerCache.getExternalEvents("*");
            if (bucket != null) {
                for (ExternalEvent e : bucket) {
                    String v = XmlPath.evaluate(xmlBean, e.getEventName());
                    if (v != null)
                        return e;
                }
            }
        }

        if (isForFallbackHandler(request))
            return EventHandlerCache.fallbackHandler;
        else
            return EventHandlerCache.serviceHandler;
    }

    private boolean isForFallbackHandler(String request) {
        return request != null && request.startsWith("<_mdw");
    }

    private String parseHandlerSpec(String spec, Map<String,String> metainfo) {
        int k = spec.indexOf('?');
        if (k < 0)
            return spec.trim();
        String clsname = spec.substring(0, k).trim();
        String[] args = spec.substring(k + 1).split("&");
        for (int i = 0; i < args.length; i++) {
            k = args[i].indexOf('=');
            if (k >= 0) {
                metainfo.put(args[i].substring(0, k), args[i].substring(k + 1));
            }
        }
        return clsname;
    }

    /**
     * This method is provided to listeners to handle external messages in a
     * protocol-independent way. The method performs the following things:
     * <ul>
     * <li>It parses the request message into generic XML bean</li>
     * <li>It determines an external event handler based on their configuration
     * and the content of the message. If a matching external event handler is
     * not found, {@link FallbackEventHandler} will be used.</li>
     * <li>It logs the message in DOCUMENT table with owner type
     * LISTENER_REQUEST</li>
     * <li>It invokes the event handler.</li>
     * <li>It logs the response message returned by the event handler, if not
     * null, in DOCUMENT table with owner type LISTENER_RESPONSE and return the
     * message to the listener</li>
     * </ul>
     * If any of the above step throws exception, the method captures it and
     * will generate a standard message (see
     * {@link #createStandardResponse(int, String, String)}), with -1
     * (RETURN_STATUS_FAILURE) as the status code and the message of the
     * exception as the status message.
     * <p>
     * Note that if the message cannot even been parsed by the generic XML bean
     * (not a well formed XML message), the message will still be logged in the
     * DOCUMENT table, and DefaultEventHandler will be invoked.
     *
     * @param request
     *            the external message, must be an XML string.
     * @param metaInfo
     *            listener specific meta information, populated by listeners.
     *            There are some common meta information entries that every
     *            listener must populate
     * @return response message. The listener will use this message to respond
     *         to the message sender if the protocol requires to do so. The
     *         response message is created by external event handlers so that
     *         they can be customized more easily. If it is certain that the
     *         listeners receiving the message will never need to respond, the
     *         response message can be set to null (by external event handlers).
     * @see FallbackEventHandler
     */
    public String processEvent(String request, Map<String,String> metaInfo) {
        String altResponse = null;
        Long eeid = 0L;

        try {
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                String altRequest = (String) monitor.onRequest(request, metaInfo);
                if (altRequest != null)
                    request = altRequest;
            }

            if (!StringHelper.isEmpty(request) && persistMessage(metaInfo))
                eeid = createRequestDocument(request, 0L);

            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                CodeTimer timer = new CodeTimer(monitor.getClass().getSimpleName() + ".onHandle()", true);
                altResponse = (String)monitor.onHandle(request, metaInfo);
                timer.stopAndLogTiming("");
                if (altResponse != null) {
                    if (persistMessage(metaInfo))
                        createResponseDocument(altResponse, eeid);
                    return altResponse;
                }
            }

            // mechanism for invoking Camel routes based on MDW listeners
            ServiceHandler serviceHandler = getServiceHandler(request, metaInfo);
            if (serviceHandler != null) {
                Object responseObj = serviceHandler.invoke(request, metaInfo);
                for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                    altResponse = (String)monitor.onResponse(responseObj, metaInfo);
                    if (altResponse != null)
                        responseObj = altResponse;
                }

                String response = responseObj == null ? null : responseObj.toString();

                if (persistMessage(metaInfo) && !StringHelper.isEmpty(response)) {
                    createResponseDocument(response, eeid);
                }

                return response;
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(request, metaInfo, ex);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                altResponse = (String)monitor.onError(ex, metaInfo);
                if (altResponse != null)
                    return altResponse;  // TODO: persist onError's altResponse?
            }

            return createErrorResponse(request, metaInfo, new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage()));
        }

        // Parse the incoming message
        Object msgdoc = getParsedMessage(request, metaInfo);
        ExternalEventHandler handler = null;
        try {
            // find event handler specification
            ExternalEvent eventHandler = findEventHandler(request, msgdoc, metaInfo);

            if (eventHandler == null) {
                FallbackEventHandler defaultHandler = new FallbackEventHandler();
                return defaultHandler.handleSpecialEventMessage((XmlObject) msgdoc);
            }

            // parse handler specification - must before checking persistence flag
            String clsname = parseHandlerSpec(eventHandler.getEventHandler(), metaInfo);
            metaInfo.put(Listener.METAINFO_DOCUMENT_ID, eeid.toString());

            // invoke event handler
            if (clsname.equals(START_PROCESS_HANDLER)) {
                clsname = ProcessStartEventHandler.class.getName();
            }
            else if (clsname.equals(NOTIFY_PROCESS_HANDLER)) {
                clsname = NotifyWaitingActivityEventHandler.class.getName();
            }

            if (clsname.equals(FallbackEventHandler.class.getName())) {
                handler = new FallbackEventHandler();
            }
            else {
                String packageName = metaInfo.get(Listener.METAINFO_PACKAGE_NAME);
                if (packageName == null)
                    packageName = eventHandler.getPackageName(); // currently only populated for VCS assets
                Package pkg = PackageCache.getPackage(packageName);
                if (pkg == null && ServiceRequestHandler.class.getName().equals(clsname)) {
                    // can happen during bootstrap scenario -- just try regular reflection
                    handler = Class.forName(clsname).asSubclass(ExternalEventHandler.class).newInstance();
                }
                else {
                    handler = pkg.getEventHandler(clsname, request, metaInfo);
                    if (!pkg.isDefaultPackage() && handler instanceof PackageAware)
                        ((PackageAware) handler).setPackage(pkg);
                }
            }

            if (handler == null)
                throw new EventHandlerException("Unable to create event handler for class: " + clsname);

            String response = handler.handleEventMessage(request, msgdoc, metaInfo);

            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                altResponse = (String)monitor.onResponse(response, metaInfo);
                if (altResponse != null)
                    response = altResponse;
            }

            if (persistMessage(metaInfo) && !StringHelper.isEmpty(response)) {
                createResponseDocument(altResponse == null ? response: altResponse, eeid);
            }
            return response;
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(request, metaInfo, ex);
        }
        catch (Exception e) {
            logger.severeException("Exception in ListenerHelper.processEvent()", e);
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                altResponse = (String)monitor.onError(e, metaInfo);
                if (altResponse != null)
                    return altResponse;  // TODO: persist onError's altResponse?
            }

            if (handler instanceof ExternalEventHandlerErrorResponse) {
                metaInfo.put(Listener.METAINFO_ERROR_RESPONSE, Listener.METAINFO_ERROR_RESPONSE_VALUE);
                return ((ExternalEventHandlerErrorResponse)handler).createErrorResponse(request, metaInfo, e);
            }
            else {
                return createErrorResponse(request, metaInfo, new ServiceException(ServiceException.INTERNAL_ERROR, e.getMessage()));
            }
        }
    }

    private boolean persistMessage(Map<String,String> metaInfo) {
        return !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_PERSISTENCE))
                && !Listener.CONTENT_TYPE_DOWNLOAD.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));
    }

    private Long createRequestDocument(String request, Long handlerId) throws EventHandlerException {
        String docType = isJson(request) ? JSONObject.class.getName() : XmlObject.class.getName();
        return createDocument(docType, request, OwnerType.LISTENER_REQUEST, handlerId).getDocumentId();
    }

    private Long createResponseDocument(String response, Long ownerId) throws EventHandlerException {
        String docType = String.class.getName();
        return createDocument(docType, response, OwnerType.LISTENER_RESPONSE, ownerId).getDocumentId();
    }

    /**
     * <p>
     * Performs the following:
     * <li>Looks up any defined DefaultEventHandler to determine the error
     * message to send back</li>
     * <li>If one is found, then return any custom response</li>
     * </p>
     *
     * @param request
     * @param jsonMessage
     * @param metaInfo
     * @return String with the error response
     */
    public String createErrorResponse(String request, Map<String,String> metaInfo, ServiceException ex) {
        /**
         * First check for a default event handler custom error response
         */
        ExternalEventHandlerErrorResponse errorResponseHandler = getDefaultEventHandlerService();
        if (errorResponseHandler != null) {
            return errorResponseHandler.createErrorResponse(request, metaInfo,
                    new UnparseableMessageException("Unable to parse request : " + request));
        }

        String contentType = metaInfo.get(Listener.METAINFO_CONTENT_TYPE);
        if (contentType == null && request != null && !request.isEmpty())
            contentType = request.trim().startsWith("{") ? Listener.CONTENT_TYPE_JSON : Listener.CONTENT_TYPE_XML;
        if (contentType == null)
            contentType = Listener.CONTENT_TYPE_XML; // compatibility

        metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));

        StatusMessage statusMsg = new StatusMessage();
        statusMsg.setCode(ex.getErrorCode());
        statusMsg.setMessage(ex.getMessage());
        if (contentType.equals(Listener.CONTENT_TYPE_JSON)) {
            return statusMsg.getJsonString();
        }
        else {
            return statusMsg.getXml();
        }
    }

    public String createAckResponse(String request, Map<String,String> metaInfo) {
        String contentType = metaInfo.get(Listener.METAINFO_CONTENT_TYPE);
        if (contentType == null && request != null && !request.isEmpty())
            contentType = request.trim().startsWith("{") ? Listener.CONTENT_TYPE_JSON : Listener.CONTENT_TYPE_XML;
        if (contentType == null)
            contentType = Listener.CONTENT_TYPE_XML; // compatibility

        StatusMessage statusMsg = new StatusMessage();
        if (contentType.equals(Listener.CONTENT_TYPE_JSON)) {
            return statusMsg.getJsonString();
        }
        else {
            return statusMsg.getXml();
        }
    }

    /**
     * @param request
     * @param jsonMessage
     * @param metaInfo
     * @return
     */
    private Object getParsedMessage(String request, Map<String,String> metaInfo) {
        if (request == null)
            return null;
        boolean jsonMessage = request.charAt(0) == '{';
        Object msgdoc = null;
        if (jsonMessage) {
            try {
                msgdoc = new JSONObject(request);
            }
            catch (JSONException e) {
                // Just log
                logger.severeException(e.getMessage(), e);
            }
        }
        else {
            // xmlbean parsing is quicker than dom, especially for large documents
            msgdoc = parseXmlBean(request);
        }
        return msgdoc;
    }

    /**
     * <p>
     * Looks up any registered services for DefaultEventHandler and returns any
     * that implement ExternalEventHandlerErrorResponse
     * </p>
     *
     * @return ExternalEventHandlerErrorResponse
     */
    private ExternalEventHandlerErrorResponse getDefaultEventHandlerService() {
        List<DefaultExternalEventHandler> defaultHandlers = EventHandlerRegistry.getInstance()
                .getDefaultEventHandlers();
        for (DefaultExternalEventHandler defaultHandler : defaultHandlers) {
            if (defaultHandler instanceof ExternalEventHandlerErrorResponse) {
                return (ExternalEventHandlerErrorResponse) defaultHandler;
            }
        }
        return null;
    }

    private XmlObject parseXmlBean(String request) {
        CodeTimer timer = new CodeTimer("ListenerHelper.parseXmlBean()", true);
        try {
            XmlObject xmlBean = XmlObject.Factory.parse(request, Compatibility.namespaceOptions());
            timer.stopAndLogTiming("");
            return xmlBean;
        }
        catch (XmlException ex) {
            logger.severeException(ex.getMessage(), ex);
            timer.stopAndLogTiming("unparseable");
            return null;
        }
    }

    public DocumentReference createDocument(String docType, Object document, String ownerType, Long ownerId)
                    throws EventHandlerException {
        return createDocument(docType, document, null, ownerType, ownerId);
    }

    /**
     * This method is used to create a document from external messages. The
     * document reference returned can be sent as parameters to start or inform
     * processes.
     *
     * @param docType
     *            this should be variable type if the document reference is to
     *            be bound to variables.
     * @param document
     *            The document object itself, such as an XML bean document
     *            (subclass of XmlObject)
     * @param pkg
     *            The workflow package whose classloader and configuration
     *            should be used in serializing the doc object
     * @param ownerType
     *            this should be OwnerType.LISTENER_REQUEST if the message is
     *            received from external system and OwnerType.LISTENER_RESPONSE
     *            if the message is to be sent as response back to the external
     *            systems.
     * @param ownerId
     *            This should be the external event handler ID for
     *            LISTENER_REQUEST and request document ID for LISTENER_RESPONSE
     * @param processInstanceId
     *            this is the ID of the process instance the message is going to
     *            be delivered to. If that information is not available, pass
     *            new Long(0) to it. You can update the information using
     *            updateDocumentInfo later on.
     * @param searchKey1
     *            user defined search key. Pass null if you do not need custom
     *            search key
     * @param searchKey2
     *            another custom search key. Pass null if you do not need it.
     * @return document reference that refers to the newly created document.
     * @throws EventHandlerException
     */
    public DocumentReference createDocument(String docType, Object document, Package pkg,
            String ownerType, Long ownerId) throws EventHandlerException {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            Long docid = eventMgr.createDocument(docType, ownerType, ownerId, document, pkg);
            return new DocumentReference(docid);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new EventHandlerException(0, ex.getMessage(), ex);
        }
    }

    public void updateDocumentContent(DocumentReference docref, Object doc, String type)
            throws ActivityException {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            eventMgr.updateDocumentContent(docref.getDocumentId(), doc, type);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public ServiceHandler getServiceHandler(String request, Map<String, String> metaInfo)
            throws EventException {
        EventManager eventMgr = ServiceLocator.getEventManager();
        String protocol = metaInfo.get(Listener.METAINFO_PROTOCOL);
        if (protocol == null)
            return null; // at a minimum must match on protocol
        String path = metaInfo.get(Listener.METAINFO_REQUEST_PATH);

        return eventMgr.getServiceHandler(protocol, path);
    }

    /**
     * A utility method to build process start JMS message.
     *
     * @param processId
     * @param eventInstId
     * @param masterRequestId
     * @param parameters
     * @return
     */
    public InternalEvent buildProcessStartMessage(Long processId, Long eventInstId,
            String masterRequestId, Map<String, String> parameters) {
        InternalEvent evMsg = InternalEvent.createProcessStartMessage(processId,
                OwnerType.DOCUMENT, eventInstId, masterRequestId, null, null, null);
        evMsg.setParameters(parameters);
        evMsg.setCompletionCode(StartActivity.STANDARD_START); // completion
                                                               // code -
                                                               // indicating
                                                               // standard start
        return evMsg;
    }

    /**
     * Helper method to obtain process ID from process name.
     *
     * @param procname
     * @return process ID of the latest version of the process with the given
     *         name
     * @throws Exception
     */
    public Long getProcessId(String procname) throws Exception {
        EventManager eventMgr = ServiceLocator.getEventManager();
        Long procId = eventMgr.findProcessId(procname, 0);
        return procId;
    }

    public static boolean isJson(String message) {
        return message.charAt(0) == '{';
    }
}
