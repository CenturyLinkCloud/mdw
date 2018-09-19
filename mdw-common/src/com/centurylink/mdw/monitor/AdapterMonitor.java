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
package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

/**
 * AdapterMonitors can be registered through @Monitor annotations to intercept
 * external service calls originating from MDW workflow adapter activities.  Must be threadsafe.
 * Normally all registered monitors are called in sequence.  However the first monitor whose onInvoke()
 * returns non-null breaks the chain, and this non-null response is passed back to the MDW runtime.
 */
public interface AdapterMonitor extends RegisteredService, Monitor {

    /**
     * Called when the request is created.  Can change the request contents.
     * Currently must return null or a String.
     *
     * @param runtimeContext The workflow runtime context of the adapter activity
     * @param request The request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Protocol header values.  May be modified by the monitor.
     * @return null, or an alternative request that should be sent to the called service
     */
    default public Object onRequest(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        return null;
    }

    /**
     * Called after onRequest(), but before the service is actually invoked.
     * Can be used to short-circuit the service.  Currently must return null or a String.
     * Returning non-null from here bypasses onResponse() and other chained monitors' onInvoke().
     *
     * @param runtimeContext The workflow runtime context of the adapter activity
     * @param request Outgoing request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers protocol headers
     * @return null, or an alternative response to send back to the MDW runtime
     */
    default public Object onInvoke(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        return null;
    }

    /**
     * Called when the response is received.
     *
     * @param runtimeContext The workflow runtime context of the adapter activity
     * @param response The response that was received from the external system. (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Protocol headers for the incoming response.  New headers can be injected by the monitor.
     * @return Null unless the monitor wants to change the response contents.  In that case, return the alternative response.
     */
    default public Object onResponse(ActivityRuntimeContext runtimeContext, Object response, Map<String,String> headers) {
        return null;
    }


    /**
     * Called when an error is encountered.
     * @param runtimeContext adapter workflow context
     * @param ex error that was encountered
     * @return null, or activity result code
     */
    default public String onError(ActivityRuntimeContext runtimeContext, Throwable t) {
        return null;
    }
}
