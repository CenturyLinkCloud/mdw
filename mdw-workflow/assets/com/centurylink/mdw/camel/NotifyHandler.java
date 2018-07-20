/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import org.apache.camel.Message;

import com.centurylink.mdw.camel.EventHandler;

public interface NotifyHandler extends EventHandler {

    /**
     * Return the eventId correlating with the process instance(s) to notify.
     * @param request the request message
     */
    public String getEventId(Message request);

    /**
     * @return the notification delay in seconds
     */
    public int getDelay();

    /**
     * Performs the notification.
     * @param eventId correlates with the process instance(s) to notify
     * @param docId the request document id
     * @param request the message whose body contents are to be sent to the notify receiver(s)
     * @param delay in seconds
     * @return the response document
     */
    public Object notify(String eventId, Long docId, Message request, int delay);

}
