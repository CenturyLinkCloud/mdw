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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.listener.Listener;

public class ExternalNotifyWaitingActivityEventHandler extends ExternalEventHandlerBase {

    public String handleEventMessage(
            String message, Object msgdoc, Map<String,String> metainfo)
            throws EventHandlerException {
        String response;
        try {
            String eventName = metainfo.get(Listener.METAINFO_EVENT_NAME);
            String eventMessage = metainfo.get(Listener.METAINFO_EVENT_MESSAGE);
            Long eventInstId = new Long(metainfo.get(Listener.METAINFO_DOCUMENT_ID));
            eventName = placeHolderTranslation(eventName, metainfo, (XmlObject)msgdoc);
            eventMessage = StringEscapeUtils.unescapeXml(placeHolderTranslation(eventMessage, metainfo, (XmlObject)msgdoc));
            int delay = 2;
            String av = PropertyManager.getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
            if (av!=null) {
                try {
                    delay = Integer.parseInt(av);
                    if (delay<0) delay = 0;
                    else if (delay>300) delay = 300;
                } catch (Exception e) {
                    logger.warn("activity resume delay spec is not an integer");
                }
            }
            // a custom implementation of this handler may populate parameters here
            Integer status = notifyProcesses(eventName, eventInstId, eventMessage, delay);

            response = createResponseMessage(null, "NOTIFICATION STATUS CODE " + status, msgdoc, metainfo);

        }
        catch (Exception e) {
            response = createResponseMessage(e, null, msgdoc, metainfo);
        }
        return response;
    }

}
