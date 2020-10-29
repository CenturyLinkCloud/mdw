package com.centurylink.mdw.listener;

import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.file.Packages;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.asset.AssetPath;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.HandlerSpec;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.pkg.PackageClassLoader;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.service.data.ServicePaths;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.ServiceHandler;
import com.centurylink.mdw.services.request.ErrorResponse;
import com.centurylink.mdw.services.request.FallbackRequestHandler;
import com.centurylink.mdw.services.request.HandlerCache;
import com.centurylink.mdw.services.request.ServiceRequestHandler;
import com.centurylink.mdw.services.rest.RestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.centurylink.mdw.xml.XmlPath;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private HandlerSpec findEventHandler(String request, Object requestDoc, Map<String,String> metaInfo)
    throws XmlException, JSONException {
        XmlObject xmlBean = null;
        String rootname = null;
        // Handle MDW internal legacy messages first
        if (requestDoc instanceof XmlObject) {
            xmlBean = (XmlObject) requestDoc;
            rootname = XmlPath.getRootNodeName(xmlBean);
            if (rootname.startsWith("_mdw_"))
                return null; // internal request
        }
        // Check for path/topic based routing - Takes precendence over content based routing
        List<HandlerSpec> bucket = null;
        boolean isTopic = metaInfo.get(Listener.METAINFO_TOPIC) != null;   // Preliminary test (cannot be Topic if REST or SOAP)
        if (metaInfo.get(Listener.METAINFO_REQUEST_PATH) != null &&   // Means no path after context and servlet (e.g. /mdw/services, etc)
            (Listener.METAINFO_PROTOCOL_REST.equals(metaInfo.get(Listener.METAINFO_PROTOCOL)) || Listener.METAINFO_PROTOCOL_SOAP.equals(metaInfo.get(Listener.METAINFO_PROTOCOL)))) {
            bucket = HandlerCache.getPathHandlers(metaInfo.get(Listener.METAINFO_REQUEST_PATH));
            isTopic = false;  // In case it was set to true above
        }
        else if (isTopic) {
            bucket = HandlerCache.getPathHandlers(metaInfo.get(Listener.METAINFO_TOPIC));
        }

        if (bucket != null) {
            for (HandlerSpec handlerSpec : bucket) {
                if (isTopic) {
                    if (metaInfo.get(Listener.METAINFO_TOPIC).equals(handlerSpec.getPath())) {
                        if (bucket.size() > 1)
                            logger.warn("Multiple external event handlers matched incoming request for topic " + metaInfo.get(Listener.METAINFO_TOPIC));
                        return handlerSpec;
                    }
                }
                else if (metaInfo.get(Listener.METAINFO_REQUEST_PATH).startsWith(handlerSpec.getPath())) {
                    if (bucket.size() > 1)
                        logger.warn("Multiple external event handlers matched incoming request for path " + metaInfo.get(Listener.METAINFO_REQUEST_PATH));
                    return handlerSpec;
                }
            }
        }

        // Check legacy content based routing (XML only)  TODO: JSONPath
        if (xmlBean != null) {
            bucket = HandlerCache.getContentHandlers(rootname);
            if (bucket != null) {
                for (HandlerSpec e : bucket) {
                        String v = e.getXpath().evaluate(xmlBean);
                        if (v != null)
                            return e;
                }
            }
            bucket = HandlerCache.getContentHandlers("*");
            if (bucket != null) {
                for (HandlerSpec e : bucket) {
                    String v = XmlPath.evaluate(xmlBean, e.getPath());
                    if (v != null)
                        return e;
                }
            }
        }

        if (isForFallbackHandler(request, metaInfo))
            return HandlerCache.fallbackHandler;
        else
            return HandlerCache.serviceHandler;
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
    private boolean isAuthorized(RequestHandler handler, Map<String,String> headers)
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
     * @deprecated use @{link #processRequest(String, Map)}
     */
    @Deprecated
    public String processEvent(String request, Map<String,String> metaInfo) {
        return processRequest(request, metaInfo);
    }

    /**
     * This method is provided to listeners to handle external messages in a
     * protocol-independent way. The method performs the following things:
     * <ul>
     * <li>It parses the request message into generic XML bean</li>
     * <li>It determines an external event handler based on their configuration
     * and the content of the message. If a matching external event handler is
     * not found, {@link com.centurylink.mdw.services.request.FallbackRequestHandler} will be used.</li>
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
     * @see com.centurylink.mdw.services.request.FallbackRequestHandler
     */
    public String processRequest(String request, Map<String,String> metaInfo) {
        Response altResponse = null;
        Long requestId = 0L;
        Request requestDoc = new Request(requestId);
        Set<String> reqMetaInfo = new HashSet<>(metaInfo.keySet());
        long requestTime = System.currentTimeMillis();

        List<ServiceMonitor> monitors = MonitorRegistry.getInstance().getServiceMonitors();
        try {
            for (ServiceMonitor monitor : monitors) {
                String altRequest = (String) monitor.onRequest(request, metaInfo);
                if (altRequest != null)
                    request = altRequest;
            }

            if (persistMessage(metaInfo)) {
                requestId = createRequestDocument(request, 0L, metaInfo.get(Listener.METAINFO_REQUEST_PATH));
                requestDoc.setId(requestId);
            }

            // persist meta even if no request doc
            if (persistMeta(metaInfo))
                requestDoc.setMeta(createRequestMetaDocument(metaInfo, reqMetaInfo, requestId));

            // log request-id so that it can easily be located
            if (logger.isInfoEnabled()) {
                StringBuilder reqMsg = new StringBuilder(">> ");
                String method = metaInfo.get(Listener.METAINFO_HTTP_METHOD);
                if (method != null)
                    reqMsg.append(method).append(" request ");
                else
                    reqMsg.append("Request ");
                if (requestId > 0)
                    reqMsg.append(requestId).append(" ");
                String protocol = metaInfo.get(Listener.METAINFO_PROTOCOL);
                if (protocol != null)
                    reqMsg.append("over ").append(protocol).append(" ");
                String path = metaInfo.get(Listener.METAINFO_REQUEST_PATH);
                if (path != null)
                    reqMsg.append("on path '").append(path).append("'");
                if (requestId > 0 && !"AppSummary".equals(path)) // don't log health/ping
                    logger.info("", reqMsg.toString());
                else
                    logger.mdwDebug(reqMsg.toString());
            }

            for (ServiceMonitor monitor : monitors) {
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
                    altResponse.setPath(ServicePaths.getInboundResponsePath(metaInfo));

                    if (persistMessage(metaInfo)) {
                        Long ownerId = createResponseDocument(altResponse, requestId);
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
                for (ServiceMonitor monitor : monitors) {
                    Object obj = monitor.onResponse(responseObj, metaInfo);
                    if (obj != null)
                        responseObj = obj;
                }

                Response response = responseObj == null ? null : responseObj instanceof Response ? (Response)responseObj : new Response(responseObj.toString());

                if (response.getStatusCode() == null)
                    response.setStatusCode(getResponseCode(metaInfo));
                response.setPath(ServicePaths.getInboundResponsePath(metaInfo));

                if (persistMessage(metaInfo) && !StringUtils.isBlank(response.getContent())) {
                    Long ownerId = createResponseDocument(response, requestId);
                    if (persistMeta(metaInfo)) {
                        response.setMeta(createResponseMeta(metaInfo, reqMetaInfo, ownerId, requestTime));
                    }
                }

                if (logger.isDebugEnabled() && requestId > 0 && !Listener.isHealthCheck(metaInfo)) {
                    logger.debug("", "<< Request " + requestId + " processed: " + (System.currentTimeMillis() - requestTime) + " ms");
                }

                return response.getContent();
            }
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            Response response = createErrorResponse(request, metaInfo, ex);
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, requestId), requestTime);
            }
            catch (Throwable e) {
                logger.error("Failed to persist response", e);
            }
            return response.getContent();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Response response = null;
            for (ServiceMonitor monitor : monitors) {
                Object obj = monitor.onError(ex, metaInfo);
                if (obj != null) {
                    if (obj instanceof Response)
                        altResponse = (Response)obj;
                    else
                        altResponse = new Response((String)obj);

                    response = altResponse;
                    if (response.getStatusCode() == null)
                        response.setStatusCode(getResponseCode(metaInfo));
                    response.setPath(ServicePaths.getInboundResponsePath(metaInfo));
                }
            }
            if (response == null)
                response = createErrorResponse(request, metaInfo, new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage()));

            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, requestId), requestTime);
            }
            catch (Throwable e) {
                logger.error("Failed to persist response", e);
            }
            return response.getContent();
        }
        finally {
            if (Thread.currentThread().getContextClassLoader() instanceof PackageClassLoader)
                ApplicationContext.resetContextClassLoader();
        }

        // Parse the incoming message
        Object msgdoc = getParsedMessage(request, metaInfo);
        RequestHandler handler = null;
        try {
            // find event handler specification
            HandlerSpec handlerSpec = findEventHandler(request, msgdoc, metaInfo);

            if (handlerSpec == null) {
                FallbackRequestHandler defaultHandler = new FallbackRequestHandler();
                if (isAuthorized(defaultHandler, metaInfo))  // Throws exception if not authorized
                    // Need authorization for incoming HTTP requests
                    return defaultHandler.handleSpecialEventMessage((XmlObject) msgdoc);
            }

            // parse handler specification - must before checking persistence flag
            String clsname = parseHandlerSpec(handlerSpec.getHandlerClass(), metaInfo);
            metaInfo.put(Listener.METAINFO_DOCUMENT_ID, requestId.toString());

            // invoke event handler
            if (clsname.equals(START_PROCESS_HANDLER)) {
                clsname = ProcessStartEventHandler.class.getName();  // compatibility
            }
            else if (clsname.equals(NOTIFY_PROCESS_HANDLER)) {
                clsname = NotifyWaitingActivityEventHandler.class.getName();  // compatibility
            }

            if (clsname.equals(FallbackRequestHandler.class.getName())) {
                handler = new FallbackRequestHandler();
            }
            else {
                String packageName = Packages.MDW_BASE;
                if (handlerSpec.getAssetPath() != null) {
                    packageName = new AssetPath(handlerSpec.getAssetPath()).pkg;
                }
                Package pkg = PackageCache.getPackage(packageName);
                if (pkg == null && ServiceRequestHandler.class.getName().equals(clsname)) {
                    // can happen during bootstrap scenario -- just try regular reflection
                    handler = Class.forName(clsname).asSubclass(RequestHandler.class).newInstance();
                }
                else {
                    handler = pkg.getRequestHandler(clsname);
                    if (handler instanceof ExternalEventHandlerBase)
                        ((ExternalEventHandlerBase)handler).setPackage(pkg);
                }
            }

            if (handler == null)
                throw new RequestHandlerException("Unable to create handler: " + clsname);

            // Content based (XPath, JSONPath) handler request - Need to authorize
            if (!HandlerCache.serviceHandler.equals(handlerSpec))
                isAuthorized(handler, metaInfo);  // throws exception if not authorized

            requestDoc.setContent(request);
            Response response = handler.handleRequest(requestDoc, msgdoc, metaInfo);

            for (ServiceMonitor monitor : monitors) {
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
            else
                metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, response.getStatusCode().toString());
            response.setPath(ServicePaths.getInboundResponsePath(metaInfo));
            if (metaInfo.containsKey(Listener.METAINFO_DOCUMENT_ID)) {
                metaInfo.put(Listener.METAINFO_MDW_REQUEST_ID, metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
                metaInfo.remove(Listener.METAINFO_DOCUMENT_ID);
            }

            if (persistMessage(metaInfo) && !StringUtils.isBlank(response.getContent())) {
                Long ownerId = createResponseDocument(response, requestId);
                if (persistMeta(metaInfo))
                    response.setMeta(createResponseMeta(metaInfo, reqMetaInfo, ownerId, requestTime));
            }

            if (logger.isDebugEnabled() && requestId > 0 && !Listener.isHealthCheck(metaInfo)) {
                logger.debug("", "<< Request " + requestId + " processed: " + (System.currentTimeMillis() - requestTime) + " ms");
            }

            return response.getContent();
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            Response response = createErrorResponse(request, metaInfo, ex);
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, requestId), requestTime);
            }
            catch (Throwable e) {
                logger.error("Failed to persist response", e);
            }
            return response.getContent();
        }
        catch (Exception e) {
            logger.error("Exception in ListenerHelper.processEvent()", e);
            Response response = null;
            for (ServiceMonitor monitor : monitors) {
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
            response.setPath(ServicePaths.getInboundResponsePath(metaInfo));
            try {
                createResponseMeta(metaInfo, reqMetaInfo, createResponseDocument(response, requestId), requestTime);
            }
            catch (Throwable ex) {
                logger.error("Failed to persist response", ex);
            }
            return response.getContent();
        }
        finally {
            if (Thread.currentThread().getContextClassLoader() instanceof PackageClassLoader)
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
                && !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_PERSISTENCE.toLowerCase()))
                && !Listener.CONTENT_TYPE_DOWNLOAD.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE))
                && !"AppSummary".equals(metaInfo.get(Listener.METAINFO_REQUEST_PATH))
                && (PropertyManager.getBooleanProperty("mdw.persist.hub.requests", false) ||
                   (!"mdw-admin".equals(metaInfo.get("app")) && !"mdw-hub".equals(metaInfo.get(Listener.METAINFO_MDW_APP_ID))));
    }

    private boolean persistMeta(Map<String,String> metaInfo) {
        return !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_META_PERSISTENCE))
                && !"true".equalsIgnoreCase(metaInfo.get(Listener.METAINFO_NO_META_PERSISTENCE.toLowerCase()))
                && !Listener.CONTENT_TYPE_DOWNLOAD.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE))
                && !"AppSummary".equals(metaInfo.get(Listener.METAINFO_REQUEST_PATH))
                && (PropertyManager.getBooleanProperty("mdw.persist.hub.requests", false) ||
                (!"mdw-admin".equals(metaInfo.get("app")) && !"mdw-hub".equals(metaInfo.get(Listener.METAINFO_MDW_APP_ID))));
    }

    private Long createRequestDocument(String request, Long handlerId, String path) throws RequestHandlerException {
        String varType = request == null || request.isEmpty() || isJson(request) ? JSONObject.class.getName() : XmlObject.class.getName();
        return createDocument(varType, request, null, OwnerType.LISTENER_REQUEST, handlerId, path).getDocumentId();
    }

    private Long createResponseDocument(Response response, Long ownerId) throws RequestHandlerException {
        String variableType = StringDocument.class.getName();
        return createDocument(variableType, response, OwnerType.LISTENER_RESPONSE, ownerId).getDocumentId();
    }

    private JSONObject createRequestMetaDocument(Map<String,String> metaInfo, Set<String> reqMetaInfo, Long ownerId)
            throws RequestHandlerException {
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
            throws RequestHandlerException, JSONException, ServiceException {
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
     * Create a default error message.  To customize this response use a ServiceMonitor.
     */
    public Response createErrorResponse(String req, Map<String,String> metaInfo, ServiceException ex) {
        return new ErrorResponse(req, metaInfo, ex);
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
                logger.error(e.getMessage(), e);
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
            XmlObject xmlBean = XmlObject.Factory.parse(request, new XmlOptions());
            timer.stopAndLogTiming("");
            return xmlBean;
        }
        catch (XmlException ex) {
            logger.error(ex.getMessage(), ex);
            timer.stopAndLogTiming("unparseable");
            return null;
        }
    }

    public DocumentReference createDocument(String variableType, Object document, String ownerType, Long ownerId)
                    throws RequestHandlerException {
        return createDocument(variableType, document, null, ownerType, ownerId);
    }

    /**
     * This method is used to create a document from external messages. The
     * document reference returned can be sent as parameters to start or inform
     * processes.
     *
     * @param variableType
     *            Variable type for serializing
     * @param docObj
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
    public DocumentReference createDocument(String variableType, Object docObj, Package pkg, String ownerType, Long ownerId)
            throws RequestHandlerException {
        return createDocument(variableType, docObj, pkg, ownerType, ownerId, null);
    }

    public DocumentReference createDocument(String variableType, Object docObj, Package pkg,
            String ownerType, Long ownerId, String path) throws RequestHandlerException {
        // TODO: proper package association (usually pkg is null)
        try {
            Long docId = ServiceLocator.getEventServices().createDocument(variableType, ownerType, ownerId, docObj, pkg, path);
            return new DocumentReference(docId);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RequestHandlerException(ex.getMessage(), ex);
        }
    }

    public ServiceHandler getServiceHandler(String request, Map<String,String> metaInfo)
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
     */
    public Long getProcessId(String procname) throws Exception {
        Process proc = ProcessCache.getProcess(procname);
        if (proc == null)
            throw new DataAccessException(0, "Cannot find process with name "
                    + procname + ", version 0");

        return proc.getId();
    }

    public static boolean isJson(String message) {
        return message.charAt(0) == '{';
    }
}
