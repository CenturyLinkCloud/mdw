/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.task;

import com.centurylink.mdw.model.data.common.StatusCode;

public class TaskState extends StatusCode {

	public static final Integer STATE_OPEN = new Integer(1);	// means not in jeopardy
    public static final Integer STATE_ALERT = new Integer(2);
    public static final Integer STATE_JEOPARDY = new Integer(3);
    public static final Integer STATE_CLOSED = new Integer(4);
    public static final Integer STATE_INVALID = new Integer(5);

    public static final String STATE_NOT_INVALID = "[Not Invalid]";

    public static final Integer[] allTaskStateCodes
     = {STATE_OPEN, STATE_ALERT, STATE_JEOPARDY, STATE_CLOSED, STATE_INVALID };

    public static final String[] allTaskStateNames
      = {"Open", "Alert", "Jeopardy", "Closed", "Invalid" };


    public static String getTaskStateName(Integer code) {
        return allTaskStateNames[code - 1];
    }
    public static Integer getStatusForNameContains(String name) {
        if ("ALERT".contains(name)) {
            return STATE_ALERT;
        }
        if ("JEOPARDY".contains(name)) {
            return STATE_JEOPARDY;
        }
        if ("INVALID".contains(name)) {
            return STATE_INVALID;
        }
        return null;
    }

    public TaskState() {
    }

    public TaskState(Long id, String description) {
        setId(id);
        setDescription(description);
    }

}