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
package com.centurylink.mdw.workflow.activity.event;

import org.apache.commons.lang.StringEscapeUtils;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.rest.MultiRestServiceAdapter;

@Tracked(LogLevel.TRACE)
public class PublishEventMessageRest extends MultiRestServiceAdapter {

    public static final String ATTRIBUTE_EVENT_NAME = "Event Name";
    public static final String ATTRIBUTE_MESSAGE = "Message";

    public static final String beginMsg = "<MDWEventNotification><Name>";
    public static final String middleMsg = "</Name><Message>";
    public static final String endMsg = "</Message></MDWEventNotification>";

    @Override
    protected String getRequestData() throws ActivityException {
        String eventName = getEventName();

        if (eventName == null)
            throw new ActivityException("Event Name attribute is missing");

        String request = beginMsg + eventName + middleMsg + StringEscapeUtils.escapeXml(getEventMessage()) + endMsg;
        return request;
    }

    @Override
    protected String getHttpMethod() throws ActivityException {
        return "POST";
    }

    protected String getEventName() {
        String eventName = getAttributeValue(ATTRIBUTE_EVENT_NAME);
        return translatePlaceHolder(eventName);
    }

    protected String getEventMessage() {
        String message = this.getAttributeValue(ATTRIBUTE_MESSAGE);
        if (message == null)
              message = "Empty";
        return translatePlaceHolder(message);
    }
}
