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
package com.centurylink.mdw.listener;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.cloud.CloudClassLoader;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.event.EventHandler;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.service.data.event.EventHandlerCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.handler.ServiceRequestHandler;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.ServiceHandler;
import com.centurylink.mdw.services.rest.RestService;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.centurylink.mdw.xml.XmlPath;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import java.io.StringReader;

public class ListenerHelper {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Status code 0, often used to designate success.
     */
    public static final int RETURN_STATUS_SUCCESS = 0;

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
            else if ("ActionRequest".equals(rootname)) {
                String v = EventHandlerCache.regressionTestHandler.getXpath().evaluate(xmlBean);
                if (v != null)
                    return EventHandlerCache.regressionTestHandler;
            }
            List<ExternalEvent> bucket = EventHandlerCache.getExternalEvents(rootname);
            if (bucket != null) {
                for (ExternalEvent e : bucket) {
                    String v = e.getXpath().evaluate(xmlBean);
                    if (v != null)
                        return e;
                }
            }
            bucket = EventHandlerCache.getExternalEvents("*");
            if (bucket != null) {
                for (ExternalEvent e : bucket) {
                    String v = XmlPath.evaluate(xmlBean, e.getMessagePattern());
                    if (v != null)
                        return e;
                }
            }
        }

        if (isForFallbackHandler(request, metaInfo))
            return EventHandlerCache.fallbackHandler;
        else
            return EventHandlerCache.serviceHandler;
    }

    private boolean isForFallbackHandler(String request, Map<String,String> metaInfo) {
        return request != null && (request.startsWith("<_mdw") || !Listener.METAINFO_PROTOCOL_REST.equals(metaInfo.get(Listener.METAINFO_PROTOCOL)));
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
     * The user identified in the AuthenticatedUser headers must be authorized to
     * perform this action.  For HTTP, MDW removes this header (if it exists) from
     * every request and then populates based on session authentication or HTTP Basic/Bearer.
     */
    private boolean isAuthorized(EventHandler handler, Map<String,String> headers)
    throws AuthorizationException {
        // Exclude unless it is REST or SOAP (i.e. HTTP)
        if (!Listener.METAINFO_PROTOCOL_REST.equals(headers.get(Listener.METAINFO_PROTOCOL)) && !Listener.METAINFO_PROTOCOL_SOAP.equals(headers.get(Listener.METAINFO_PROTOCOL)))
            return true;

        String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
        User user = null;

        List<String> roles = handler.getRoles();
        if (roles != null && !roles.isEmpty()) {
            if (userId != null)
                user = UserGroupCache.getUser(userId);
            if (user == null)
                throw new AuthorizationException(ServiceException.NOT_AUTHORIZED, "Event Handler " + handler.getClass().getSimpleName() + " requires authenticated user");
            for (String role : roles) {
                if (user.hasRole(role))
                    return true;
            }
            throw new AuthorizationException(ServiceException.NOT_AUTHORIZED, "User: " + userId + " not authorized for: " + handler.getClass().getSimpleName());
        }
        return true;
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
        Response altResponse = null;
        Long eeid = 0L;
        Request requestDoc = new Request(eeid);
        Set<String> reqMetaInfo = new HashSet<String>(metaInfo.keySet());
        long requestTime = System.currentTimeMillis();

        try {
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                String altRequest = (String) monitor.onRequest(request, metaInfo);
                if (altRequest != null)
                    request = altRequest;
            }

            if (persistMessage(metaInfo)) {
                eeid = createRequestDocument(request, 0L, metaInfo.get(Listener.METAINFO_REQUEST_PATH));
                requestDoc.setId(eeid);
            }

            // persist meta even if no request doc
            if (persistMeta(metaInfo))
                requestDoc.setMeta(createRequestMetaDocument(metaInfo, reqMetaInfo, eeid));

            // log request-id so that it can easily be located
            if (logger.isDebugEnabled()) {
                StringBuilder reqMsg = new StringBuilder(">> ");
                String method = metaInfo.get(Listener.METAINFO_HTTP_METHOD);
                if (method != null)
                    reqMsg.append(method).append(" request ");
                else
                    reqMsg.append("Request ");
                if (eeid > 0)
                    reqMsg.append(eeid).append(" ");
                String protocol = metaInfo.get(Listener.METAINFO_PROTOCOL);
                if (protocol != null)
                    reqMsg.append("over ").append(protocol).append(" ");
                String path = metaInfo.get(Listener.METAINFO_REQUEST_PATH);
                if (path != null)
                    reqMsg.append("on path '").append(path).append("'");
                if (eeid > 0 && !"AppSummary".equals(path)) // don't log health/ping
                    logger.debug(reqMsg.toString());
                else
                    logger.mdwDebug(reqMsg.toString());
            }

            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                CodeTimer timer = new CodeTimer(monitor.getClass().getSimpleName() + ".onHandle()", true);
                Object obj = monitor.onHandle(request, metaInfo);
                timer.stopAndLogTiming("");
                if (obj != null) {
                    if (obj instanceof Response)
                        altResponse = (Response)obj;
                    else
                        altResponse = new Response((String)obj);

                    if (altResponse.getStatusCode() == null)
                        altResponse.setStatusCode(getResponseCode(metaInfo));

                    if (persistMessage(metaInfo)) {
                        Long ownerId = createResponseDocument(altResponse, eeid);
                        if (persistMeta(metaInfo)) {
                            altResponse.setMeta(createResponseMeta(metaInfo, reqMetaInfo, ownerId, requestTime));
                        }
                    }
                    return altResponse.getContent();
                }
            }

            // mechanism for invoking Camel routes based on MDW listeners
            ServiceHandler serviceHandler = getServiceHandler(request, metaInfo);
            if (serviceHandler != null) {
                Object responseObj = serviceHandler.invoke(request, metaInfo);
                for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                    Object obj = monitor.onResponse(responseObj, metaInfo);
                    if (obj != null)
                        responseObj = obj;
                }

                Response response = responseObj == null ? null : responseObj instanceof Response ? (Response)responseObj : new Response(responseObj.toString());

                if (response.getStatusCode() == null)
                    response.setStatusCode(getResponseCode(metaInfo));

                if (persistMessage(metaInfo) && !StringHelper.isEmpty(response.getContent())) {
                    Long ownerId = createResponseDocument(response, eeid);
                    if (persistMeta(metaInfo)) {
                        response.setMeta(createResponseMeta(metaInfo, reqMetaInfo, ownerId, requestTime));
                    }
                }

                return response.getContent();
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            Response response = createErrorResponse(request, metaInfo, ex);
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, eeid), requestTime);
            }
            catch (Throwable e) {
                logger.severeException("Failed to persist response", e);
            }
            return response.getContent();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            Response response = null;
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                Object obj = monitor.onError(ex, metaInfo);
                if (obj != null) {
                    if (obj instanceof Response)
                        altResponse = (Response)obj;
                    else
                        altResponse = new Response((String)obj);

                    response = altResponse;
                    if (response.getStatusCode() == null)
                        response.setStatusCode(getResponseCode(metaInfo));
                }
            }
            if (response == null)
                response = createErrorResponse(request, metaInfo, new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage()));

            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, eeid), requestTime);
            }
            catch (Throwable e) {
                logger.severeException("Failed to persist response", e);
            }
            return response.getContent();
        }
        finally {
            if (Thread.currentThread().getContextClassLoader() instanceof CloudClassLoader)
                ApplicationContext.resetContextClassLoader();
        }

        // Parse the incoming message
        Object msgdoc = getParsedMessage(request, metaInfo);
        EventHandler handler = null;
        try {
            // find event handler specification
            ExternalEvent eventHandler = findEventHandler(request, msgdoc, metaInfo);

            if (eventHandler == null) {
                FallbackEventHandler defaultHandler = new FallbackEventHandler();
                if (isAuthorized(defaultHandler, metaInfo))  // Throws exception if not authorized
                    // Need authorization for incoming HTTP requests
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
                String packageName = eventHandler.getPackageName();
                Package pkg = PackageCache.getPackage(packageName);
                if (pkg == null && ServiceRequestHandler.class.getName().equals(clsname)) {
                    // can happen during bootstrap scenario -- just try regular reflection
                    handler = Class.forName(clsname).asSubclass(EventHandler.class).newInstance();
                }
                else {
                    handler = pkg.getEventHandler(clsname);
                    if (!pkg.isDefaultPackage() && handler instanceof ExternalEventHandlerBase)
                        ((ExternalEventHandlerBase)handler).setPackage(pkg);
                }
            }

            if (handler == null)
                throw new EventHandlerException("Unable to create event handler for class: " + clsname);

            // Content based (XPATH) external event handler request - Need to authorize
            if (!EventHandlerCache.serviceHandler.equals(eventHandler))
                isAuthorized(handler, metaInfo);  // Throws exception if not authorized

            requestDoc.setContent(request);
            Response response = handler.handleEventMessage(requestDoc, msgdoc, metaInfo);

            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                Object obj = monitor.onResponse(response, metaInfo);
                if (obj != null) {
                    if (obj instanceof Response)
                        altResponse = (Response)obj;
                    else
                        altResponse = new Response((String)obj);

                    response = altResponse;
                }
            }

            if (response.getStatusCode() == null)
                response.setStatusCode(getResponseCode(metaInfo));
            response.setPath(metaInfo.get(Listener.METAINFO_REQUEST_PATH));
            if (metaInfo.containsKey(Listener.METAINFO_DOCUMENT_ID)) {
                metaInfo.put(Listener.METAINFO_MDW_REQUEST_ID, metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
                metaInfo.remove(Listener.METAINFO_DOCUMENT_ID);
            }


            if (persistMessage(metaInfo) && !StringHelper.isEmpty(response.getContent())) {
                Long ownerId = createResponseDocument(response, eeid);
                if (persistMeta(metaInfo))
                    response.setMeta(createResponseMeta(metaInfo, reqMetaInfo, ownerId, requestTime));
            }
            return response.getContent();
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            Response response = createErrorResponse(request, metaInfo, ex);
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, eeid), requestTime);
            }
            catch (Throwable e) {
                logger.severeException("Failed to persist response", e);
            }
            return response.getContent();
        }
        catch (Exception e) {
            logger.severeException("Exception in ListenerHelper.processEvent()", e);
            Response response = null;
            for (ServiceMonitor monitor : MonitorRegistry.getInstance().getServiceMonitors()) {
                Object obj = monitor.onError(e, metaInfo);
                if (obj != null) {
                    if (obj instanceof Response)
                        altResponse = (Response)obj;
                    else
                        altResponse = new Response((String)obj);

                    response = altResponse;
                }
            }

            if (response == null) {
                response = createErrorResponse(request, metaInfo,
                        new ServiceException(ServiceException.INTERNAL_ERROR, e.getMessage()));
            }
            if (response.getStatusCode() == null)
                response.setStatusCode(getResponseCode(metaInfo));
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, eeid), requestTime);
            }
            catch (Throwable ex) {
                logger.severeException("Failed to persist response", ex);
            }
            return response.getContent();
        }
        finally {
            if (Thread.currentThread().getContextClassLoader() instanceof CloudClassLoader)
                ApplicationContext.resetContextClassLoader();
        }
    }

    private int getResponseCode(Map<String,String> metainfo) {
        try {
            if (metainfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null && !metainfo.get(Listener.METAINFO_HTTP_STATUS_CODE).equals("0"))  // Allow services to populate code via metaInfo, same as Rest servlet
                return Integer.parseInt(metainfo.get(Listener.METAINFO_HTTP_STATUS_CODE));
            else   // Return 200 for non-error responses, which is what Tomcat returns in HTTP header if not overridden above
                return RestService.HTTP_200_OK;
        }
        catch (NumberFormatException e) {
            return RestService.HTTP_200_OK;
        }
    }

    private boolean persistMessage(Map<String,String> metaInfo) {
        return !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_PERSISTENCE))
                && !Listener.CONTENT_TYPE_DOWNLOAD.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));
    }

    private boolean persistMeta(Map<String,String> metaInfo) {
        return !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_META_PERSISTENCE))
                && !Listener.CONTENT_TYPE_DOWNLOAD.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));
    }

    private Long createRequestDocument(String request, Long handlerId, String path) throws EventHandlerException {
        String docType = request == null || request.isEmpty() || isJson(request) ? JSONObject.class.getName() : XmlObject.class.getName();
        return createDocument(docType, request, null, OwnerType.LISTENER_REQUEST, handlerId, path).getDocumentId();
    }

    private Long createResponseDocument(Response response, Long ownerId) throws EventHandlerException {
        String docType = String.class.getName();
        return createDocument(docType, response, OwnerType.LISTENER_RESPONSE, ownerId).getDocumentId();
    }

    private JSONObject createRequestMetaDocument(Map<String,String> metaInfo, Set<String> reqMetaInfo, Long ownerId) throws EventHandlerException, JSONException{
        JSONObject meta = new JsonObject();
        JSONObject headers = new JsonObject();

        for (String key : metaInfo.keySet()) {
            if (Listener.AUTHENTICATED_USER_HEADER.equals(key) ||
                Listener.METAINFO_HTTP_STATUS_CODE.equals(key) ||
                Listener.METAINFO_PROTOCOL.equals(key) ||
                Listener.METAINFO_SERVICE_CLASS.equals(key) ||
                Listener.METAINFO_REQUEST_URL.equals(key) ||
                Listener.METAINFO_HTTP_METHOD.equals(key) ||
                Listener.METAINFO_REMOTE_HOST.equals(key) ||
                Listener.METAINFO_REMOTE_ADDR.equals(key) ||
                Listener.METAINFO_REMOTE_PORT.equals(key) ||
                Listener.METAINFO_REQUEST_PATH.equals(key) ||
                Listener.METAINFO_REQUEST_QUERY_STRING.equals(key) ||
                Listener.METAINFO_CONTENT_TYPE.equals(key) ||
                !reqMetaInfo.contains(key))
                meta.put(key, metaInfo.get(key));
            else {
                headers.put(key, metaInfo.get(key));
            }
        }

        meta.put("headers", headers);

        createDocument(JSONObject.class.getName(), meta, null, OwnerType.LISTENER_REQUEST_META, ownerId, metaInfo.get(Listener.METAINFO_REQUEST_PATH));
        return meta;
    }

    /**
     * Inserts the response meta DOCUMENT, as well as INSTANCE_TIMING.
     */
    private JSONObject createResponseMeta(Map<String,String> metaInfo, Set<String> reqMetaInfo, Long ownerId, long requestTime)
            throws EventHandlerException, JSONException, ServiceException {
        JSONObject meta = new JsonObject();
        JSONObject headers = new JsonObject();

        for (String key : metaInfo.keySet()) {
            if (!Listener.AUTHENTICATED_USER_HEADER.equals(key)
                    && !Listener.METAINFO_HTTP_STATUS_CODE.equals(key)
                    && !Listener.METAINFO_ACCEPT.equals(key)
                    && !Listener.METAINFO_DOWNLOAD_FORMAT.equals(key)
                    && !Listener.METAINFO_MDW_REQUEST_ID.equals(key)
                    && !reqMetaInfo.contains(key)) {
                headers.put(key, metaInfo.get(key));
            }
        }

        // these always get populated if present
        if (metaInfo.get(Listener.METAINFO_REQUEST_ID) != null)
            headers.put(Listener.METAINFO_REQUEST_ID, metaInfo.get(Listener.METAINFO_REQUEST_ID));
        if (metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID) != null && !metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID).equals("0"))
            headers.put(Listener.METAINFO_MDW_REQUEST_ID, metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID));
        if (metaInfo.get(Listener.METAINFO_CORRELATION_ID) != null)
            headers.put(Listener.METAINFO_CORRELATION_ID, metaInfo.get(Listener.METAINFO_CORRELATION_ID));
        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) != null)
            headers.put(Listener.METAINFO_CONTENT_TYPE, metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (!headers.has(Listener.METAINFO_CONTENT_TYPE))
            headers.put(Listener.METAINFO_CONTENT_TYPE, "application/json");

        meta.put("headers", headers);

        createDocument(JSONObject.class.getName(), meta, OwnerType.LISTENER_RESPONSE_META, ownerId);


        ServiceLocator.getRequestServices().setElapsedTime(OwnerType.LISTENER_RESPONSE, ownerId,
                System.currentTimeMillis() - requestTime);

        return meta;
    }

    /**
     * Create a default error message.  To customized this response use a ServiceMonitor.
     */
    public Response createErrorResponse(String req, Map<String,String> metaInfo, ServiceException ex) {
        Request request = new Request(0L);
        request.setContent(req);

        Response response = new Response();
        String contentType = metaInfo.get(Listener.METAINFO_CONTENT_TYPE);
        if (contentType == null && req != null && !req.isEmpty())
            contentType = req.trim().startsWith("{") ? Listener.CONTENT_TYPE_JSON : Listener.CONTENT_TYPE_XML;
        if (contentType == null)
            contentType = Listener.CONTENT_TYPE_XML; // compatibility

        metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getCode()));

        StatusMessage statusMsg = new StatusMessage();
        statusMsg.setCode(ex.getCode());
        statusMsg.setMessage(ex.getMessage());
        response.setStatusCode(statusMsg.getCode());
        response.setStatusMessage(statusMsg.getMessage());
        if (contentType.equals(Listener.CONTENT_TYPE_JSON)) {
            response.setContent(statusMsg.getJsonString());
        }
        else {
            response.setContent(statusMsg.getXml());
        }
        return response;
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

    private Object getParsedMessage(String request, Map<String,String> metaInfo) {
        if (request == null || request.isEmpty())
            return null;
        boolean jsonMessage = request.charAt(0) == '{';
        Object msgdoc = null;
        if (jsonMessage) {
            try {
                msgdoc = new JsonObject(request);
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
     */
    public DocumentReference createDocument(String docType, Object document, Package pkg,
                                            String ownerType, Long ownerId) throws EventHandlerException {
        return createDocument(docType, document, pkg, ownerType, ownerId, null);
    }

    public DocumentReference createDocument(String docType, Object document, Package pkg,
            String ownerType, Long ownerId, String path) throws EventHandlerException {
        try {
            Long docId = ServiceLocator.getEventServices().createDocument(docType, ownerType, ownerId, document, pkg, path);
            return new DocumentReference(docId);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new EventHandlerException(0, ex.getMessage(), ex);
        }
    }

    public void updateDocumentContent(DocumentReference docref, Object doc, String type, Package pkg)
            throws ActivityException {
        try {
            EventServices eventMgr = ServiceLocator.getEventServices();
            eventMgr.updateDocumentContent(docref.getDocumentId(), doc, type, pkg);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public ServiceHandler getServiceHandler(String request, Map<String, String> metaInfo)
            throws EventException {
        EventServices eventMgr = ServiceLocator.getEventServices();
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
        Process proc = ProcessCache.getProcess(procname, 0);
        if (proc == null)
            throw new DataAccessException(0, "Cannot find process with name "
                    + procname + ", version 0");

        return proc.getId();
    }

    public static boolean isJson(String message) {
        return message.charAt(0) == '{';
    }
}
