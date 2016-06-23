/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.Map;

/**
 * <p>
 * The use of this interface would be in the following scenario:
 * <ul>
 * <li>You already have a user-developed External Event handler</li>
 * <li>You wish to throw an exception in your Event Handler (based on some error in your processing)</li>
 * <li>You also wish to control which error response is sent back based on your thrown exception</li>
 * </ul>
 * </p>
 * <p>
 * <b>Please note</b>
 * <br/>
 * Currently we are only supporting this interface for <b>SOAP messages</b>
 * </p>
 * @author aa70413
 *
 */
public interface ExternalEventHandlerErrorResponse {

    /**
     * <p>
     * This method should be implemented in your own event handler to control what error/fault
     * message would be sent back in the case of an exception thrown from your event handler.
     * This gives the developer full control of the response sent back in the case of exception.
     * <b>2 things to note:</b>
     * <ul>
     * <li>Normally the exception is an EventHandlerException type</li>
     * <li>This callback mechanism is currently only supported for SOAP messages</li>
     * </ul>
     * </p>
     * @param request This is the original request received by an MDW servlet
     * @param metaInfo This contains metaInfo populated by the ListenerHelper
     * @see com.qwest.mdw.listener.ListenerHelper
     * @param eventHandlerException This is the exception thrown from eventhandler
     * @return The string xml of the error response
     */
    public String createErrorResponse(String request, Map<String,String> metaInfo, Throwable eventHandlerException);

}
