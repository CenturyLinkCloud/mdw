/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.Map;

/**
 * The base External Event Handler that can be orchestrated
 * The implementors of this handler will implement this
 *
 */
public interface ExternalEventHandler {

      /**
      * Handles the external event message
      * The passed in message
      * @param msg message from external system in string format
      * @param msgobj message from external system in parsed form, such as generic XML bean, JSON object.
      * 		It is passed in as null if the message cannot be parsed
      *         or does not have an object representation (such as USO orders)
      * @param metainfo meta information around the request message, such as protocol,
      *     reply to address, request id, event name, event id, event instance id,
      *     arguments specified for handler, etc. See constants in Listener.java
      *     for typical ones
      * @throws  EventHandlerException
      */
     public String handleEventMessage(String msg, Object msgobj, Map<String,String> metainfo)
     throws EventHandlerException;
}
