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
