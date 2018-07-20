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
package com.centurylink.mdw.model.workflow;

public interface WorkStatus {

    public static final Integer STATUS_PENDING_PROCESS = new Integer(1);    // only used by process
    public static final Integer STATUS_IN_PROGRESS = new Integer(2);
    public static final Integer STATUS_FAILED = new Integer(3);
    public static final Integer STATUS_COMPLETED = new Integer(4);
    public static final Integer STATUS_CANCELLED = new Integer(5);
    public static final Integer STATUS_HOLD = new Integer(6);
    public static final Integer STATUS_WAITING = new Integer(7);
    public static final Integer STATUS_PURGE = new Integer(32);        // only used by process at cleanup

    public static final String STATUSNAME_PENDING_PROCESS = "Pending Processing";
    public static final String STATUSNAME_PENDING = "Pending";
    public static final String STATUSNAME_IN_PROGRESS = "In Progress";
    public static final String STATUSNAME_FAILED = "Failed";
    public static final String STATUSNAME_COMPLETED = "Completed";
    public static final String STATUSNAME_CANCELLED = "Cancelled";
    public static final String STATUSNAME_CANCELED = "Canceled";
    public static final String STATUSNAME_HOLD = "Hold";
    public static final String STATUSNAME_WAITING = "Waiting";
    public static final String STATUSNAME_PURGE = "Purge";
    public static final String STATUSNAME_ACTIVE = "[Active]"; // pseudo status meaning not final

    public static final Integer[] allStatusCodes = { STATUS_PENDING_PROCESS, STATUS_IN_PROGRESS,
        STATUS_FAILED, STATUS_COMPLETED, STATUS_CANCELLED, STATUS_HOLD, STATUS_WAITING};
    public static final String[] allStatusNames = {STATUSNAME_PENDING_PROCESS, STATUSNAME_IN_PROGRESS,
        STATUSNAME_FAILED, STATUSNAME_COMPLETED, STATUSNAME_CANCELLED, STATUSNAME_HOLD, STATUSNAME_WAITING};

    public static final String LOGMSG_START = "Activity started";
    public static final String LOGMSG_EXECUTE = "Activity executing";
    public static final String LOGMSG_COMPLETE = "Activity completed";
    public static final String LOGMSG_SUSPEND = "Activity suspended";
    public static final String LOGMSG_CANCELLED = "Activity cancelled";
    public static final String LOGMSG_FAILED = "Activity failed";
    public static final String LOGMSG_HOLD = "Activity on hold";
    public static final String LOGMSG_PROC_START = "Process started";
    public static final String LOGMSG_PROC_COMPLETE = "Process completed";
    public static final String LOGMSG_PROC_CANCEL = "Process cancelled";

}