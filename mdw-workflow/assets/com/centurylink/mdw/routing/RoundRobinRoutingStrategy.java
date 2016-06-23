/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.routing;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.AbstractRoutingStrategy;
import com.centurylink.mdw.common.service.RequestRoutingStrategy;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.listener.Listener;

/**
 * Routing strategy that determines the appropriate destination based on the round-robin algorithm.
 */
@RegisteredService(RequestRoutingStrategy.class)
public class RoundRobinRoutingStrategy extends AbstractRoutingStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile String lastInstance = null;
    private static final Object lock = new Object();
    private static final int priority = 999;

    public RoundRobinRoutingStrategy() {
    }

    public int getPriority() {
        return priority;
    }

    public URL getDestination(Object request, Map<String,String> headers) {
        if (!StringHelper.isEmpty(PropertyManager.getProperty(PropertyNames.MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY)) && !this.getClass().getName().equalsIgnoreCase(PropertyManager.getProperty(PropertyNames.MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY)))
            return null;

        String instance = null;
        String firstInstance = null;
        boolean nextInstance = false;

        synchronized (lock) {
            try{
                for(String server : super.getActiveWorkerInstances()) {
                    if (instance == null) {
                        if (firstInstance == null)
                            firstInstance = server;

                        // First request since server start-up
                        if (lastInstance == null) {
                            instance = lastInstance = server;
                        }
                        // Found server last request was sent to - route request to next server in list
                        else if (server.equals(lastInstance))
                            nextInstance = true;
                        // This is the next server in list
                        else if (nextInstance) {
                            instance = lastInstance = server;
                            nextInstance = false;
                        }
                    }
                }

                // Last request was sent to last server in list, so circle back to beginning of list
                if (instance == null) {
                    instance = lastInstance = firstInstance;
                }

                // If instance is still null, then there are no active servers to route to
                return instance == null ? null : buildURL(headers, instance);
            }
            catch (IOException ex) {
                logger.severeException("Cannot get routing destination for request path: " + headers.get(Listener.METAINFO_REQUEST_PATH), ex);
                return null;
            }
        }
    }

}
