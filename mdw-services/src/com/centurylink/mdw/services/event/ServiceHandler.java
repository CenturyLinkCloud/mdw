/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
