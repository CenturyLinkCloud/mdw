/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.handler;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.listener.RegressionTestEventHandler;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageAware;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.action.InstanceLevelActionHandler;
import com.centurylink.mdw.service.resource.AppSummary;
import com.centurylink.mdw.service.rest.Users;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.ResourceFormatter.Format;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;

public class ServiceRequestHandler implements ExternalEventHandler, PackageAware {

    protected static final String MDW_REST_SERVICE_PROVIDER_PACKAGE = "com.centurylink.mdw.service.rest";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

    @Override
    public String handleEventMessage(String request, Object requestObj, Map<String,String> metaInfo)
    throws EventHandlerException {

        TextService service = null;
        String path = metaInfo.get(Listener.METAINFO_REQUEST_PATH);

        Format format = getFormat(metaInfo);

        try {
            // compatibility - START
            if ("GetAppSummary".equals(path)) {
                service = new AppSummary();
            }
            else if ("User".equals(path)) {
                service = new Users();
            }

            if (requestObj instanceof XmlObject) {
                // XML request
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, "text/xml");
                if ("ActionRequest".equals(XmlPath.getRootNodeName((XmlObject)requestObj))) {
                    ActionRequestDocument actionRequestDoc = (ActionRequestDocument) ((XmlObject)requestObj).changeType(ActionRequestDocument.type);
                    ActionRequest actionRequest = actionRequestDoc.getActionRequest();

                    Action action = actionRequest.getAction();
                    if (action == null || action.getName() == null)
                        throw new EventHandlerException("Missing Action in request");
                    // compatibility for regression test handler
                    if (action.getName().equals("RegressionTest")) {
                        RegressionTestEventHandler handler = new RegressionTestEventHandler();
                        return handler.handleEventMessage(request, requestObj, metaInfo);
                    }
                    // compatibility for instance level handler
                    if (action.getName().equals("PerformInstanceLevelAction")) {
                        InstanceLevelActionHandler handler = new InstanceLevelActionHandler();
                        return handler.handleEventMessage(request, requestObj, metaInfo);
                    }

                    for (Parameter param : actionRequest.getAction().getParameterList())
                        metaInfo.put(param.getName(), param.getStringValue());
                    requestObj = actionRequest.getContent();

                    metaInfo.put(Listener.METAINFO_REQUEST_PATH, action.getName());
                }
            }
            else if (requestObj instanceof JSONObject) {
                // JSON request
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, "application/json");
                JSONObject jsonObj = (JSONObject) requestObj;
                String action = null;
                if ((jsonObj.has("Action") && jsonObj.get("Action") instanceof JSONObject) ||
                        (jsonObj.has("action") && jsonObj.get("action") instanceof JSONObject)) {
                    JSONObject actionObj = jsonObj.has("Action") ? jsonObj.getJSONObject("Action") : jsonObj.getJSONObject("action");
                    if (actionObj.has("name")) {
                        action = actionObj.getString("name");
                        if (actionObj.has("parameters")) {
                            Object paramsObj = actionObj.get("parameters");
                            if (paramsObj instanceof JSONArray) {
                                JSONArray params = (JSONArray) paramsObj;
                                // TODO: does this ever really work?
                                for (int i = 0; i < params.length(); i++) {
                                    JSONObject param = params.getJSONObject(i);
                                    String paramName = JSONObject.getNames(param)[0];
                                    String value = param.getString(paramName);
                                    metaInfo.put(paramName, value);
                                }
                            }
                            else {
                                // params is a JSONObject
                                JSONObject params = (JSONObject) paramsObj;
                                String[] names = JSONObject.getNames(params);
                                if (names != null) {
                                    for (String name : names)
                                        metaInfo.put(name, params.getString(name));
                                }
                            }
                        }
                        // top level entity
                        String[] jsonNames = JSONObject.getNames(jsonObj);
                        if (jsonNames != null) {
                            if (jsonNames.length > 1) {
                                for (int i = 0; i < jsonNames.length; i++) {
                                    String paramName = jsonNames[i];
                                    if (!paramName.equals("Action") && !paramName.equals("action")) {
                                        JSONObject value = jsonObj.getJSONObject(paramName);
                                        requestObj = value;
                                    }
                                }
                            }
                        }
                    }
                    if (action != null)
                        metaInfo.put(Listener.METAINFO_REQUEST_PATH, action);
                }
            }
            // compatibility - END

            if (service == null) {
                service = getServiceInstance(metaInfo);
                if (service == null)
                    return createErrorResponse("Unable to handle service request: " + path, format);
            }

            if (format == Format.json || (service instanceof JsonService && !(service instanceof XmlService))) {
                JsonService jsonService = (JsonService) service;
                String downloadFormat = metaInfo.get(Listener.METAINFO_DOWNLOAD_FORMAT);
                if (downloadFormat != null && !(downloadFormat.equals(Listener.DOWNLOAD_FORMAT_JSON)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_XML) || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_TEXT))) {
                    // binary download format requires export
                    if (!(jsonService instanceof JsonRestService))
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Export not supported for " + jsonService.getClass());
                    return ((JsonRestService)jsonService).export(downloadFormat, metaInfo);
                }
                else {
                    metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_JSON);
                    String json = jsonService.getJson((JSONObject)requestObj, metaInfo);
                    if (json == null)
                        return createSuccessResponse(Format.json);
                    else
                        return json;
                }
            }
            else if (format == Format.xml) {
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_XML);
                XmlService xmlService = (XmlService) service;
                String xml = xmlService.getXml((XmlObject)requestObj, metaInfo);
                if (xml == null)
                    return createSuccessResponse(Format.xml);
                else
                    return xml;
            }
            else {
                metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_TEXT);
                String text = service.getText(requestObj, metaInfo);
                if (text == null)
                    return createSuccessResponse(Format.text);
                else
                    return text;
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
            return createResponse(ex.getErrorCode(), ex.getMessage(), format);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(ex, format);
        }
    }

    protected String createErrorResponse(int code, String message, Format format) throws EventHandlerException {
        return createResponse(code, message, format);
    }

    protected String createErrorResponse(Throwable t, Format f) throws EventHandlerException {
        if (t instanceof ServiceException && ((ServiceException)t).getErrorCode() >= 400)
            return createResponse(((ServiceException)t).getErrorCode(), t.getMessage(), f);
        Throwable cause = t;
        while (cause.getCause() != null)
          cause = cause.getCause();
        return createResponse(-1, cause.toString(), f);
    }

    protected String createErrorResponse(String message, Format format) throws EventHandlerException {
        return createResponse(-1, message, format);
    }

    protected String createSuccessResponse(String message, Format format) throws EventHandlerException {
        return createResponse(0, message, format);
    }

    protected String createSuccessResponse(Format format) throws EventHandlerException {
        return createResponse(0, "Success", format);
    }

    protected String createResponse(int code, String message, Format format) throws EventHandlerException {
        StatusMessage statusMessage = new StatusMessage();
        statusMessage.setCode(code);
        if (message != null)
            statusMessage.setMessage(message);
        if (format == Format.xml)
            return statusMessage.getXml();
        else if (format == Format.json)
            return statusMessage.getJsonString();
        else
            return "ERROR " + (code == 0 ? "" : code) + ": " + message;
    }

    /**
     * Returns the service instance, consulting the service registry if necessary.
     */
    protected TextService getServiceInstance(Map<String,String> headers) throws ServiceException {
        try {
            String[] pathSegments = headers.get(Listener.METAINFO_REQUEST_PATH).split("/");
            String contentType = headers.get(Listener.METAINFO_CONTENT_TYPE);
            String serviceClassName = MDW_REST_SERVICE_PROVIDER_PACKAGE + "." + pathSegments[0];
            try {
                // normal classloader -- built-in service
                Class<? extends TextService> serviceClass = Class.forName(serviceClassName).asSubclass(TextService.class);
                return serviceClass.newInstance();
            }
            catch (ClassNotFoundException ex) {
                // try dynamic based on annotations eg: MyServices/Employees/dxoakes
                Class<? extends RegisteredService> serviceType = Listener.CONTENT_TYPE_JSON.equals(contentType) ? JsonService.class : XmlService.class;
                MdwServiceRegistry registry = MdwServiceRegistry.getInstance();
                String pkgName = null;
                for (int i = 0; i < pathSegments.length - 1; i++) {
                    String pathSegment = pathSegments[i];
                    if (i == 0)
                        pkgName = pathSegment;
                    else
                        pkgName += "." + pathSegment;
                    Package pkg = PackageCache.getPackage(pkgName);
                    if (pkg != null) {
                        // give it a shot
                        TextService service = (TextService)registry.getDynamicServiceForPath(pkg, serviceType, "/" + pathSegments[i + 1]);
                        if (service != null)
                            return service;
                    }
                }
                if (pathSegments.length == 1) {
                    // fall back to old-style REST handlers
                    String cl = pathSegments[0];
                    try {
                        Class<? extends TextService> serviceClass = Class.forName("com.centurylink.mdw.service.resource." + cl).asSubclass(TextService.class);
                        return serviceClass.newInstance();
                    }
                    catch (ClassNotFoundException cnfe) {
                        Class<? extends TextService> serviceClass = Class.forName("com.centurylink.mdw.service.action." + cl).asSubclass(TextService.class);
                        return serviceClass.newInstance();
                    }
                }
                return null;
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    protected Format getFormat(Map<String,String> metaInfo) {
        Format format = Format.xml;
        String formatParam = (String) metaInfo.get("format");
        if (formatParam != null) {
            if (formatParam.equals("json"))
                format = Format.json;
            else if (formatParam.equals("text"))
                format = Format.text;
        }
        else {
            if (Listener.CONTENT_TYPE_JSON.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE)) || Listener.CONTENT_TYPE_JSON.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE.toLowerCase()))) {
                format = Format.json;
            }
            else if (Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE)) || Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE.toLowerCase()))) {
                format = Format.text;
            }
        }
        return format;
    }
}
