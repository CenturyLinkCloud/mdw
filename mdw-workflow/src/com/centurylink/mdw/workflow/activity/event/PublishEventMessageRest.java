/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
