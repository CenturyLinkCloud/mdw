/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.routing;

import java.net.URL;
import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.RequestRoutingStrategy;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(ServiceMonitor.class)
public class RoutingServiceMonitor implements ServiceMonitor {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public Object onRequest(Object request, Map<String,String> headers) {
        return null;
    }

    @Override
    public Object onHandle(Object request, Map<String,String> headers) {
        // First check if routing is enabled
        if (!isRoutingEnabled())
            return null;

        // Ignore intra-MDW requests
        if (Listener.METAINFO_PROTOCOL_JMS.equalsIgnoreCase(headers.get(Listener.METAINFO_PROTOCOL)) &&
                JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE.equals(headers.get(Listener.METAINFO_REQUEST_PATH)))
            return null;

        // If GET request, check exclusion_list property of routing package
        if ("GET".equalsIgnoreCase(headers.get(Listener.METAINFO_HTTP_METHOD))) {
            Package pkg = PackageCache.getPackage(this.getClass().getName().substring(0,this.getClass().getName().lastIndexOf('.')));
            String[] exclusions = pkg.getProperty("ExclusionRoutingList").split(",");
            for (String path : exclusions) {
                if (headers.get(Listener.METAINFO_REQUEST_PATH).toLowerCase().contains(path.toLowerCase()))
                    return null;
            }
        }

        // Do not route requests for cache refreshes so ALL instances perform the refresh
        if ("WorkflowCache".equalsIgnoreCase(headers.get(Listener.METAINFO_REQUEST_PATH)) ||
            "GitVcs".equalsIgnoreCase(headers.get(Listener.METAINFO_REQUEST_PATH)) ||
            (request.toString().length() < 200 && request.toString().contains("ActionRequest") &&
            (request.toString().contains("RefreshCache") || request.toString().contains("RefreshProcessCache"))) ) {
            return null;
        }

        Exception caughtException = null;
        URL prevDestination = null;
        // In case of failure (i.e. host not responsive, etc) re-try by routing to a different URL
        for (int i=0; i<ApplicationContext.getManagedServerList().size(); i++) {
            URL destination = getRoutingStrategyDestination(request, headers);
            if (destination == null)
                return null;
            else if (prevDestination == null || !destination.toString().equals(prevDestination.toString())) {
                prevDestination = destination;

                // currently, routed requests go through HTTP/HTTPS(if enabled) regardless of incoming protocol
                if (!"HTTP".equalsIgnoreCase(destination.getProtocol()) && !"HTTPS".equalsIgnoreCase(destination.getProtocol())) {
                    throw new IllegalArgumentException("Invalid destination URL: " + destination);
                }
                else if ("HTTPS".equalsIgnoreCase(destination.getProtocol()) && !PropertyManager.getBooleanProperty(PropertyNames.MDW_ROUTING_REQUESTS_HTTPS_ENABLED, false)) {
                    try {
                        destination = new URL(destination.toString().replace("https:", "http:"));
                    }
                    catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid destination URL: " + destination);
                    }
                }

                // Reroute request and return the response
                try {
                    int timeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_ROUTING_REQUEST_TIMEOUT, 300) * 1000;
                    logger.debug("Routing request from: " + headers.get(Listener.METAINFO_REQUEST_PATH) + " to: " + destination);
                    HttpHelper httpHelper = new HttpHelper(destination);
                    httpHelper.setHeaders(headers);
                    httpHelper.setConnectTimeout(timeout);   // Timeout after 5 minutes - Host not responsive
                    httpHelper.setReadTimeout(timeout);
                    if ("GET".equalsIgnoreCase(headers.get(Listener.METAINFO_HTTP_METHOD)))
                        return httpHelper.get();
                    else
                        return httpHelper.post(request.toString());
                }
                catch (Exception e) {
                    logger.severeException("Error routing request to " + destination.toString() + ": " + e.getMessage(), e);
                    caughtException = e;
                }
            }
        }
        return caughtException != null ? caughtException.toString() : null;
    }

    @Override
    public Object onResponse(Object response, Map<String,String> headers) {
        return null;
    }

    @Override
    public Object onError(Throwable t, Map<String,String> headers) {
        return null;
    }

    /**
     * Returns an instance of the first applicable routing strategy found based on priority of each strategy, or null if none return a URL.
     */
    protected URL getRoutingStrategyDestination(Object request, Map<String,String> headers) {
        for (RequestRoutingStrategy routingStrategy : MdwServiceRegistry.getInstance().getRequestRoutingStrategies()) {
            URL destination = routingStrategy.getDestination(request, headers);
            if (destination != null)
                return destination;
        }
        return null;
    }

    protected boolean isRoutingEnabled() {
        return PropertyManager.getBooleanProperty(PropertyNames.MDW_ROUTING_REQUESTS_ENABLED, false) &&
               !ApplicationContext.getRoutingServerList().isEmpty() &&
               ApplicationContext.getRoutingServerList().contains(ApplicationContext.getServerHostPort()) &&
               !ApplicationContext.getManagedServerList().isEmpty();
    }
}
