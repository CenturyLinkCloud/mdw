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

    Integer STATUS_PENDING_PROCESS = new Integer(1);    // only used by process
    Integer STATUS_IN_PROGRESS = new Integer(2);
    Integer STATUS_FAILED = new Integer(3);
    Integer STATUS_COMPLETED = new Integer(4);
    Integer STATUS_CANCELLED = new Integer(5);
    Integer STATUS_HOLD = new Integer(6);
    Integer STATUS_WAITING = new Integer(7);
    Integer STATUS_PURGE = new Integer(32);        // only used by process at cleanup

    String STATUSNAME_PENDING_PROCESS = "Pending Processing";
    String STATUSNAME_PENDING = "Pending";
    String STATUSNAME_IN_PROGRESS = "In Progress";
    String STATUSNAME_FAILED = "Failed";
    String STATUSNAME_COMPLETED = "Completed";
    String STATUSNAME_CANCELLED = "Cancelled";
    String STATUSNAME_CANCELED = "Canceled";
    String STATUSNAME_HOLD = "Hold";
    String STATUSNAME_WAITING = "Waiting";
    String STATUSNAME_ACTIVE = "[Active]"; // pseudo status meaning not final

    Integer[] allStatusCodes = { STATUS_PENDING_PROCESS, STATUS_IN_PROGRESS,
        STATUS_FAILED, STATUS_COMPLETED, STATUS_CANCELLED, STATUS_HOLD, STATUS_WAITING};
    String[] allStatusNames = {STATUSNAME_PENDING_PROCESS, STATUSNAME_IN_PROGRESS,
        STATUSNAME_FAILED, STATUSNAME_COMPLETED, STATUSNAME_CANCELLED, STATUSNAME_HOLD, STATUSNAME_WAITING};

    enum InternalLogMessage {

        ACTIVITY_START("Activity started", STATUS_IN_PROGRESS, STATUSNAME_IN_PROGRESS),
        ACTIVITY_EXECUTE("Activity executing", STATUS_IN_PROGRESS, STATUSNAME_IN_PROGRESS),
        ACTIVITY_COMPLETE("Activity completed", STATUS_COMPLETED, STATUSNAME_COMPLETED),
        ACTIVITY_SUSPEND("Activity suspended", STATUS_WAITING, STATUSNAME_WAITING),
        ACTIVITY_CANCEL("Activity cancelled", STATUS_CANCELLED, STATUSNAME_CANCELLED),
        ACTIVITY_FAIL("Activity failed", STATUS_FAILED, STATUSNAME_FAILED),
        ACTIVITY_HOLD("Activity on hold", STATUS_HOLD, STATUSNAME_HOLD),
        PROCESS_START("Process started", STATUS_IN_PROGRESS, STATUSNAME_IN_PROGRESS),
        PROCESS_COMPLETE("Process completed", STATUS_COMPLETED, STATUSNAME_COMPLETED),
        PROCESS_CANCEL("Process cancelled", STATUS_CANCELLED, STATUSNAME_CANCELLED),
        PROCESS_ERROR("Process errored", STATUS_FAILED, STATUSNAME_FAILED),
        TRANSITION_INIT("Transition initiated", STATUS_IN_PROGRESS, STATUSNAME_IN_PROGRESS);

        public final String message;
        public final Integer status;
        public final String statusName;

        InternalLogMessage(String message, Integer status, String statusName) {
            this.message = message;
            this.status = status;
            this.statusName = statusName;
        }

        public String toString() {
            return message;
        }

        public static boolean isInternalMessage(String message) {
            for (InternalLogMessage internalLogMessage : InternalLogMessage.values()) {
                if (message.startsWith(internalLogMessage.message)) {
                    return true;
                }
            }
            return false;
        }

        public static InternalLogMessage match(String message) {
            for (InternalLogMessage internalMessage : InternalLogMessage.values()) {
                if (message.startsWith(internalMessage.message)) {
                    return internalMessage;
                }
            }
            return null;
        }
    }
}