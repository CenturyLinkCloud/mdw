/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.net.URL;
import java.util.Map;

/**
 * Registered service interface for routing MDW service requests.
 */
public interface RequestRoutingStrategy extends RegisteredService {

    /**
     * Returns the rerouted destination URL, or null if request is not to be rerouted.
     * For HTTP protocol, this can be simply http://<host>:<port> if the request path
     * should be the same on the target server.  Or a full destination URL can be returned.
     */
    public URL getDestination(Object request, Map<String,String> headers);

    /**
     * Return the set priority for the particular routing strategy.
     * A Lower numbered strategy gets priority over a higher numbered strategy
     */
    public int getPriority();
}
