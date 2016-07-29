/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.ResourceFormatter.Format;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.Resource;
import com.centurylink.mdw.service.ResourceRequestDocument;
import com.centurylink.mdw.service.ResourceRequestDocument.ResourceRequest;
import com.centurylink.mdw.service.handler.ServiceRequestHandler;
import com.centurylink.mdw.services.rest.JsonRestService;

public class ResourceRequestHandler extends ServiceRequestHandler {

    protected static final String SERVICE_PROVIDER_IMPL_PACKAGE = "com.centurylink.mdw.service.resource";

    public static final String APP_SUMMARY = "AppSummary";
    public static final String CONFIG_ELEMENT = "ConfigElement";
    public static final String WORKFLOW_ASSET = "WorkflowAsset";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String handleEventMessage(String msg, Object xmlBean, Map<String,String> metaInfo)
    throws EventHandlerException {
        ResourceRequestDocument resourceRequestDoc = (ResourceRequestDocument)
                ((XmlObject)xmlBean).changeType(ResourceRequestDocument.type);
        ResourceRequest resourceRequest = resourceRequestDoc.getResourceRequest();
        Resource resource = resourceRequest.getResource();
        Map<String,Object> parameters = new HashMap<String,Object>();
        for (Parameter parameter : resource.getParameterList()) {
            parameters.put(parameter.getName(), parameter.getStringValue());
        }
        String requestPath = metaInfo.get(Listener.METAINFO_REQUEST_PATH);
        if (requestPath != null) {
            int slash = requestPath.indexOf('/');
            if (slash > 0 && slash < requestPath.length() - 1)
                metaInfo.put(Listener.METAINFO_RESOURCE_SUBPATH, requestPath.substring(slash + 1));
        }
        Format format = Format.xml;
        try {
            // compatibility for individually-registered handlers
            if (resource.getName().equals("GetAppSummary")) {
                resource.setName(APP_SUMMARY);
            }
            else if (resource.getName().equals("ConfigFile") || resource.getName().equals("GetConfigFile")) {
                resource.setName(CONFIG_ELEMENT);
            }
            else if (resource.getName().equals("RuleSet") || resource.getName().equals("GetRuleSet")) {
                resource.setName(WORKFLOW_ASSET);
            }
            else if (resource.getName().equals("User")) {
                resource.setName("Users");
            }

            String formatParam = (String)parameters.get("format");
            if (formatParam != null) {
                if (formatParam.equals("json"))
                    format = Format.json;
                else if (formatParam.equals("text"))
                    format = Format.text;
            }
            else if (Listener.CONTENT_TYPE_JSON.equals(metaInfo.get("accept")) || Listener.CONTENT_TYPE_JSON.equals(metaInfo.get("Accept"))) {
                format = Format.json;
            }
            else if (Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get("accept")) || Listener.CONTENT_TYPE_TEXT.equals(metaInfo.get("Accept"))) {
                format = Format.text;
            }

            if (format != null)
                metaInfo.put(Listener.METAINFO_FORMAT, format.toString());
            TextService resourceService = getResourceServiceInstance(resource.getName(), metaInfo);
            if (resourceService == null) {
                return createErrorResponse("Unable to handle resource request for Resource: " + resource.getName(), Format.xml);
            }
            else {
                if (format == Format.json || (resourceService instanceof JsonService && !(resourceService instanceof XmlService))) {
                    JsonService jsonService = (JsonService) resourceService;
                    if (Listener.CONTENT_TYPE_EXCEL.equals(metaInfo.get(Listener.METAINFO_DOWNLOAD_FORMAT))) {
                        if (!(jsonService instanceof JsonRestService))
                            throw new ServiceException(ServiceException.BAD_REQUEST, "Export not supported for " + jsonService.getClass());
                        return ((JsonRestService)jsonService).getExcel(parameters, metaInfo);
                    }
                    else {
                        metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_JSON);
                        return jsonService.getJson(parameters, metaInfo);
                    }
                }
                else if (format == Format.xml) {
                    metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_XML);
                    XmlService xmlService = (XmlService) resourceService;
                    return xmlService.getXml(parameters, metaInfo);
                }
                else {
                    metaInfo.put(Listener.METAINFO_CONTENT_TYPE, Listener.CONTENT_TYPE_TEXT);
                    return resourceService.getText(parameters, metaInfo);
                }
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            if (ex.getErrorCode() >= 400)
                metaInfo.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(ex.getErrorCode()));
            return createResponse(ex.getErrorCode(), ex.getMessage(), format);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(ex.getMessage(), format);
        }
    }

    private static final List<String> OLD_STYLE_SERVICE_CONFLICTS = Arrays.asList(new String[]{"Attributes", "Packages", "Processes"});

    protected TextService getResourceServiceInstance(String resource, Map<String,String> headers) throws ServiceException {
        if (Listener.METAINFO_PROTOCOL_REST.equals(headers.get(Listener.METAINFO_PROTOCOL))) {
            // try new-style services first (except for certain non-json requests from Designer)
            if (!OLD_STYLE_SERVICE_CONFLICTS.contains(resource) || "application/json".equals(headers.get("accept")) || headers.containsKey("DownloadFormat")) {
                try {
                    return getServiceInstance(REST_SERVICE_PROVIDER_PACKAGE, resource, headers);
                }
                catch (ServiceException ex) {
                    if (!(ex.getCause() instanceof ClassNotFoundException))
                        throw ex;  // otherwise fall back to old packaging below
                }
            }
        }
        return getServiceInstance(SERVICE_PROVIDER_IMPL_PACKAGE, resource, headers);
    }

}