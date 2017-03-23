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