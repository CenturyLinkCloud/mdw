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
package com.centurylink.mdw.listener;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;

public class NotifySynchronousAdapterHandler extends ExternalEventHandlerBase {

    public String handleEventMessage(
            String message, Object msgdoc, Map<String, String> metainfo)
            throws EventHandlerException {
        String response;
        try {
            String eventName = metainfo.get(Listener.METAINFO_EVENT_NAME);
            eventName = placeHolderTranslation(eventName, metainfo, (XmlObject)msgdoc);
            notifyWaitingThread(eventName, message);
            response = createResponseMessage(null, null, msgdoc, metainfo);
        } catch (Exception e) {
            response = createResponseMessage(e, null, msgdoc, metainfo);
        }
        return response;
    }

    public void notifyWaitingThread(String eventName, String message)
            throws JSONException, ProcessException {
        JSONObject json = new JsonObject();
        json.put("ACTION", "NOTIFY");
        json.put("CORRELATION_ID", eventName);
        json.put("MESSAGE", message);
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }

}
