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
package com.centurylink.mdw.workflow.activity.event;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.CompletionCode;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

@Tracked(LogLevel.TRACE)
public class EventCheckActivity extends EventWaitActivity {

    private Integer exitStatus;
    public static final String RECEIVED_MESSAGE_DOC_VAR = "rcvdMsgDocVar";


    /**
     * Method that executes the logic based on the work
     */
    public void execute() throws ActivityException {
        EventWaitInstance received = registerWaitEvents(false, true);
        if (received!=null) {
            setReturnCodeAndExitStatus(received.getCompletionCode());
            processMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
            boolean toFinish = handleCompletionCode();
            if (toFinish && exitStatus==null)
                exitStatus = WorkStatus.STATUS_COMPLETED;
        } else {
            exitStatus = WorkStatus.STATUS_COMPLETED;
            setReturnCode(null);
        }
    }


    public boolean needSuspend() {
        return !WorkStatus.STATUS_COMPLETED.equals(exitStatus);
    }


    protected void setReturnCodeAndExitStatus(String code) {
        CompletionCode compcode = new CompletionCode();
        compcode.parse(code);
        exitStatus = compcode.getActivityInstanceStatus();
        if (compcode.getEventType().equals(EventType.FINISH)) {
            setReturnCode(compcode.getCompletionCode());
        } else {
            setReturnCode(compcode.getEventTypeName() + ":" +
                    (compcode.getCompletionCode()==null?"":compcode.getCompletionCode()));
        }
    }

    protected boolean handleCompletionCode() throws ActivityException {
        String compCode = this.getReturnCode();
        if (compCode!=null && (compCode.length()==0||compCode.equals(EventType.EVENTNAME_FINISH)))
            compCode = null;
        String actInstStatusName;
        if (exitStatus==null) actInstStatusName = null;
        else if (exitStatus.equals(WorkStatus.STATUS_CANCELLED)) actInstStatusName = WorkStatus.STATUSNAME_CANCELLED;
        else if (exitStatus.equals(WorkStatus.STATUS_WAITING)) actInstStatusName = WorkStatus.STATUSNAME_WAITING;
        else if (exitStatus.equals(WorkStatus.STATUS_HOLD)) actInstStatusName = WorkStatus.STATUSNAME_HOLD;
        else actInstStatusName = null;
        if (actInstStatusName!=null) {
            if (compCode==null) compCode = actInstStatusName + "::";
            else compCode = actInstStatusName + "::" + compCode;
        }
        setReturnCode(compCode);
        if (WorkStatus.STATUS_WAITING.equals(exitStatus)) {
            this.registerWaitEvents(true, false);
            if (compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_CORRECT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ABORT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ERROR))
                return true;
            else return false;
        } else return true;
    }

    /**
     * Typically you will not override this method
     */
    public boolean resumeWaiting(InternalEvent eventMessageDoc) throws ActivityException {
        if (WorkStatus.STATUSNAME_COMPLETED.equalsIgnoreCase(eventMessageDoc.getCompletionCode())){
            setReturnCode(null);
            return true;
        }
        EventWaitInstance received = registerWaitEvents(true, true);
        if (received!=null) {
            setReturnCodeAndExitStatus(received.getCompletionCode());
            processMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
            return handleCompletionCode();
        } else {
            setReturnCode(null);
            return true;
        }
    }


}
