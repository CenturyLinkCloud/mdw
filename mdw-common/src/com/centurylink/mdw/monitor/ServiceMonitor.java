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
import com.centurylink.mdw.common.service.ServiceException;

/**
 * ServiceMonitors can be registered through @RegisteredService annotations to intercept
 * service calls handled by the MDW framework.  Must be threadsafe.
 * Normally all registered monitors are called in sequence.  However the first monitor whose onHandle()
 * returns non-null breaks the chain, and this non-null response is passed back to the consumer.
 */
public interface ServiceMonitor extends RegisteredService {

    /**
     * Called when the request comes in.  Can change the incoming request contents.
     * Currently must return null or a String.
     *
     * @param request Incoming request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers Incoming protocol header values.  May be modified by the monitor.
     * @return null, or an alternative request that should be passed on instead of the incoming request
     */
    public Object onRequest(Object request, Map<String,String> headers) throws ServiceException;

    /**
     * Called after onRequest(), but before the appropriate handler is determined.
     * Can be used to short-circuit the request.  Currently must return null or a String.
     * Returning non-null from here bypasses onResponse() and other monitors' onHandle().
     *
     * @param request Incoming request message content (String, XMLBean, JAXB Element, JSONObject, etc).
     * @param headers protocol headers
     * @return null, or an alternative response to send back to the service consumer
     */
    public Object onHandle(Object request, Map<String,String> headers) throws ServiceException;

    /**
     * Called before the response is sent out from the MDW event handler or camel route.
     * Currently must return null or a String.
     *
     * @param response Outgoing response message content (probably a String, XMLBean or JAXB Element)
     * @param headers Outbound protocol header values.  May be modified by the monitor.
     * @return null, or an alternative response to send back to the service consumer.
     */
    public Object onResponse(Object response, Map<String,String> headers) throws ServiceException;

    /**
     * @param t Throwable error that was encountered
     * @param headers service meta information
     * @return alternate response or null
     */
    public Object onError(Throwable t, Map<String,String> headers);

}
