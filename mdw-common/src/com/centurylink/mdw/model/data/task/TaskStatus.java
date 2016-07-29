/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.task;

import com.centurylink.mdw.model.data.common.StatusCode;

public class TaskStatus extends StatusCode {

	// logical status ACTIVE means not COMPLETED or CANCELLED
	public static final Integer STATUS_ACTIVE = new Integer(-1);
	// logical status CLOSED means COMPLETED or CANCELLED
	public static final Integer STATUS_CLOSED = new Integer(-2);

	public static final Integer STATUS_OPEN = new Integer(1);
    public static final Integer STATUS_ASSIGNED = new Integer(2);
    // public static final Integer STATUS_FAILED = new Integer(3);
    public static final Integer STATUS_COMPLETED = new Integer(4);
    public static final Integer STATUS_CANCELLED = new Integer(5);
    public static final Integer STATUS_IN_PROGRESS = new Integer(6);

    public static final String STATUSNAME_ACTIVE = "[Active]";
    public static final String STATUSNAME_CLOSED = "[Closed]";

    public static final String STATUSNAME_OPEN = "Open";
    public static final String STATUSNAME_ASSIGNED = "Assigned";
    public static final String STATUSNAME_COMPLETED = "Completed";
    public static final String STATUSNAME_CANCELLED = "Cancelled";
    public static final String STATUSNAME_CANCELED = "Canceled";
    public static final String STATUSNAME_IN_PROGRESS = "In Progress";

    // does not include logical statuses
    public static final Integer[] allStatusCodes = { STATUS_OPEN, STATUS_ASSIGNED,
        STATUS_COMPLETED, STATUS_CANCELLED, STATUS_IN_PROGRESS};
    public static final String[] allStatusNames = { STATUSNAME_OPEN, STATUSNAME_ASSIGNED,
        STATUSNAME_COMPLETED, STATUSNAME_CANCELLED, STATUSNAME_IN_PROGRESS};

    public static Integer getStatusCodeForName(String name) {
        for (int i = 0; i < allStatusNames.length; i++) {
            if (name.equals(allStatusNames[i]))
                return allStatusCodes[i];
        }
        return null;
    }
    public static Integer getStatusCodeForNameContains(String name) {
        for (int i = 0; i < allStatusNames.length; i++) {
            if (allStatusNames[i].toUpperCase().contains(name))
                return allStatusCodes[i];
        }
        return null;
    }

    public TaskStatus() {
    }

    public TaskStatus(Long id, String description) {
        setId(id);
        setDescription(description);
    }

}