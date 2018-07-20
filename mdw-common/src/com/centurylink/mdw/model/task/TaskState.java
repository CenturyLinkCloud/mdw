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
package com.centurylink.mdw.model.task;

import com.centurylink.mdw.model.StatusCode;

public class TaskState extends StatusCode {

    public static final Integer STATE_OPEN = new Integer(1);    // means not in jeopardy
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