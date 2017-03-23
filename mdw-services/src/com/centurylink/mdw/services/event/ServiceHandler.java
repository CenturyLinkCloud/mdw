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
package com.centurylink.mdw.services.event;

import java.util.Map;

/**
 * Abstraction of a Service processor so that other handlers such as Camel routes
 * can register to respond to MDW listener requests. (See EventManager.registerServiceHandler()).
 */
public interface ServiceHandler {
    
    /**
     * Performs the actions associated with the request and returns a response,
     * which currently must be a String.  Could be used to configure a threadpool
     * to handle the actual work.
     * @param request the content of the request
     * @param metaInfo name/value pairs including protocol info such as request headers
     * @return the response (currently must be a String or implement toString())
     */
    public Object invoke(String request, Map<String,String> metaInfo);
    
    /**
     * The protocol along with getPath() are used to identify which requests the
     * handler should respond to. 
     */
    public String getProtocol();
    
    /**
     * The path along with getProtocol() are used to identify which requests the
     * handler should respond to. 
     */
    public String getPath();

}
