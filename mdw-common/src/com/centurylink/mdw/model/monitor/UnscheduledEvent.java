/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.monitor;

import java.util.Date;

public class UnscheduledEvent extends ScheduledEvent {

    @Override
    public Date getScheduledTime() {
        return null;
    }

    public void setScheduledTime(Date scheduledTime) {
        throw new UnsupportedOperationException("Can't set scheduledTime for an unscheduled event");
    }

    @Override
    public int compareTo(ScheduledEvent o) {
        return getCreateTime().compareTo(o.getCreateTime());
    }
}
