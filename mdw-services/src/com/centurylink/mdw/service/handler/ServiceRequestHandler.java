/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.handler;

import java.util.Map;

import org.json.JSONException;

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
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.PackageAware;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.ResourceFormatter.Format;

public abstract class ServiceRequestHandler implements ExternalEventHandler, PackageAware {

    protected static final String REST_SERVICE_PROVIDER_PACKAGE = "com.centurylink.mdw.service.rest";

    protected String requestId;

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

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
        if (requestId != null)
            statusMessage.setRequestId(requestId);
        if (format == Format.xml)
            return statusMessage.getXml();
        else if (format == Format.json) {
            try {
                return statusMessage.getJson().toString(2);
            }
            catch (JSONException ex) {
                throw new EventHandlerException(code, ex.getMessage(), ex);
            }
        }
        else {
            return "ERROR " + (code == 0 ? "" : code) + ": " + message;
        }
    }

    /**
     * Returns the service instance, consulting the service registry if necessary.
     */
    protected TextService getServiceInstance(String defaultImplPackage, String logicalName, Map<String,String> headers) throws ServiceException {
        try {
            String format = headers.get(Listener.METAINFO_FORMAT);
            String serviceClassName;
            if (logicalName.indexOf('.') > 0)
                serviceClassName = logicalName;  // old-style full-package-path custom
            else
                serviceClassName = defaultImplPackage + "." + logicalName.replaceAll(" ", "").replaceAll("-", "");
            int lastDot = serviceClassName.lastIndexOf('.');
            String packageName = serviceClassName.substring(0, lastDot);
            Package packageVO = PackageCache.getPackageVO(packageName);
            if (packageVO == null)
                packageVO = PackageCache.getPackage(logicalName); // new jax-rs style pathing

            Class<? extends TextService> serviceClass;

            if (packageVO == null) {
                // normal classloader
                serviceClass = Class.forName(serviceClassName).asSubclass(TextService.class);
            }
            else {
                // Not handling Format.text as TextService is not a Registered Service
                Class<? extends RegisteredService> serviceType = Format.json.toString().equals(format) ? JsonService.class : XmlService.class;
                MdwServiceRegistry registry = MdwServiceRegistry.getInstance();
                TextService service = (TextService) registry.getDynamicService(packageVO, serviceType, serviceClassName);
                if (service == null) {
                    // new-style jax-rs based on annotations eg: MyServices/Employees/dxoakes
                    String requestPath = headers.get(Listener.METAINFO_REQUEST_PATH);
                    if (requestPath != null && requestPath.startsWith(logicalName + "/") && requestPath.length() > logicalName.length() + 1) {
                        String subPath = requestPath.substring(logicalName.length() + 1);
                        String resPath = subPath;
                        int slash = resPath.indexOf('/');
                        if (slash > 0)
                            resPath = subPath.substring(0, slash);

                        // try resPath with and without leading slash
                        service = (TextService) registry.getDynamicServiceForPath(packageVO, serviceType, "/" + resPath);
                        if (service == null)
                            service = (TextService) registry.getDynamicServiceForPath(packageVO, serviceType, resPath);
                        if (service == null && Format.xml.toString().equals(format)) { // forgive missing header for browser requests
                            service = (TextService) registry.getDynamicServiceForPath(packageVO, JsonService.class, "/" + resPath);
                            if (service == null)
                                service = (TextService) registry.getDynamicServiceForPath(packageVO, JsonService.class, resPath);
                        }
                        if (service == null)
                            throw new ServiceException("Dynamic Java " + format + " service not found for path: " + resPath);
                    }
                }
                if (service == null)
                    throw new ServiceException("Dynamic Java " + format + " service not found: " + serviceClassName);
                return service;
            }
            return serviceClass.newInstance();
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
