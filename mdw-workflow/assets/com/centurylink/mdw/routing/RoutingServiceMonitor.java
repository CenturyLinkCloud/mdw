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
package com.centurylink.mdw.routing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
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
        String pkgName = this.getClass().getName().substring(0,this.getClass().getName().lastIndexOf('.'));
        if ("GET".equalsIgnoreCase(headers.get(Listener.METAINFO_HTTP_METHOD))) {
            try {
                Package pkg = PackageCache.getPackage(pkgName);
                String[] exclusions = pkg.getProperty("ExclusionRoutingList").split(",");
                for (String path : exclusions) {
                    if (headers.get(Listener.METAINFO_REQUEST_PATH).toLowerCase().contains(path.toLowerCase()))
                        return null;
                }
            }
            catch (CachingException ex) {
                logger.severeException("Error getting routing package: " + pkgName, ex);
            }
        }

        // Do not route requests for cache refreshes so ALL instances perform the refresh
        if ("WorkflowCache".equalsIgnoreCase(headers.get(Listener.METAINFO_REQUEST_PATH)) ||
            "GitVcs".equalsIgnoreCase(headers.get(Listener.METAINFO_REQUEST_PATH)) ||
            (request != null && (request.toString().length() < 200 && request.toString().contains("ActionRequest") &&
            request.toString().contains("RefreshCache"))) ) {
            return null;
        }

        Exception caughtException = null;
        URL prevDestination = null;
        // In case of failure (i.e. host not responsive, etc) re-try by routing to a different URL
        for (int i=0; i<ApplicationContext.getServerList().getServers().size(); i++) {
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
            if (destination != null) {
                // Only return a destination if it is not itself (identical call) - Avoid infinite loop
         //       int queryIdx = destination.toString().indexOf("?");
                URL origRequestUrl = null;
                try {
                    origRequestUrl = new URL(headers.get(Listener.METAINFO_REQUEST_URL));
                }
                catch (MalformedURLException e) {
                    logger.severeException("Malformed original RequestURL", e);
                }
                String origHost = origRequestUrl.getHost().indexOf(".") > 0 ? origRequestUrl.getHost().substring(0, origRequestUrl.getHost().indexOf(".")) : origRequestUrl.getHost();
                int origPort = origRequestUrl.getPort() == 80 || origRequestUrl.getPort() == 443 ? -1 : origRequestUrl.getPort();
                String origQuery = headers.get(Listener.METAINFO_REQUEST_QUERY_STRING) == null ? "" : headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
                String newHost = destination.getHost().indexOf(".") > 0 ? destination.getHost().substring(0, destination.getHost().indexOf(".")) : destination.getHost();
                int newPort = destination.getPort() == 80 || destination.getPort() == 443 ? -1 : destination.getPort();
                String newQuery = destination.getQuery() == null ? "" : destination.getQuery();

                if (!newHost.equalsIgnoreCase(origHost) || !(newPort == origPort) || !newQuery.equalsIgnoreCase(origQuery) || !origRequestUrl.getPath().equals(destination.getPath()))
                    return destination;
            }
        }
        return null;
    }

    protected boolean isRoutingEnabled() {
        return PropertyManager.getBooleanProperty(PropertyNames.MDW_ROUTING_REQUESTS_ENABLED, false);
    }
}
