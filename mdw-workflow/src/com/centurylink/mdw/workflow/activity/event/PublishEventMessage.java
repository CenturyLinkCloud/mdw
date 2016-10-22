/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.event;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

@Tracked(LogLevel.TRACE)
public class PublishEventMessage extends DefaultActivityImpl {

	public static final String ATTRIBUTE_EVENT_NAME = "Event Name";
	public static final String ATTRIBUTE_MESSAGE = "Message";

	private static StandardLogger logger = LoggerUtil.getStandardLogger();

	@Override
	public void execute() throws ActivityException {
		try {
			signal(getEventName(), getEventMessage(), getEventDelay());
		}
		catch (Exception ex) {
		    logger.severeException(ex.getMessage(), ex);
			throw new ActivityException(-1, "Failed to publish event message", ex);
		}
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

	protected int getEventDelay() {
        int delay = 2;
        String av = getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
        if (av!=null) {
            try {
                delay = Integer.parseInt(av);
                if (delay<0) delay = 0;
                else if (delay>300) delay = 300;
            } catch (Exception e) {
                logwarn("activity resume delay spec is not an integer");
            }
        }
        return delay;
	}

	protected final void signal(String eventName, String eventMessage, int delay) throws Exception {
        DocumentReference docref = this.createDocument(String.class.getName(),
        		eventMessage, OwnerType.INTERNAL_EVENT, this.getActivityInstanceId());
        super.loginfo("Publish message, event=" + eventName +
        		", id=" + docref.getDocumentId() + ", message=" + eventMessage);
        getEngine().notifyProcess(eventName, docref.getDocumentId(), eventMessage, delay);
	}

}
