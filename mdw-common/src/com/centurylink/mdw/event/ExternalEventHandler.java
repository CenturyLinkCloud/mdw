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
package com.centurylink.mdw.event;

import java.util.Map;

import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.request.Request;

/**
 * The base External Event Handler that can be orchestrated
 * The implementors of this handler will implement this
 *
 */
public interface ExternalEventHandler extends EventHandler {

      /**
      * Handles the external event message
      * The passed in message
      * @param msg message from external system in string format
      * @param msgobj message from external system in parsed form, such as generic XML bean, JSON object.
      *         It is passed in as null if the message cannot be parsed
      *         or does not have an object representation (such as USO orders)
      * @param metainfo meta information around the request message, such as protocol,
      *     reply to address, request id, event name, event id, event instance id,
      *     arguments specified for handler, etc. See constants in Listener.java
      *     for typical ones
      * @throws  EventHandlerException
      */
     public String handleEventMessage(String msg, Object msgobj, Map<String,String> metainfo)
     throws EventHandlerException;

     public default Response handleEventMessage(Request msg, Object msgobj, Map<String,String> metainfo)
     throws EventHandlerException {
         return new Response(handleEventMessage(msg.getContent(), msgobj, metainfo));
     }
}
