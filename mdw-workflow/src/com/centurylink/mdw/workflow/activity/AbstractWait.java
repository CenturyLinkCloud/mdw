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
package com.centurylink.mdw.workflow.activity;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendableActivity;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.ServiceLocatorException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Base class for all the Wait Activities
 * This class will be extended by the custom activity
 * that do not want a completion event raised.
 * This will act as a stopper for the workflow
 * In cases where in case of an error or a manual task creation,
 * we can use this activity to pause the flow and the flow can be
 * restarted from an event.
 */
public abstract class AbstractWait extends DefaultActivityImpl implements SuspendableActivity {

    public List<String[]> getWaitEventSpecs() {
        String attVal = getAttributeValue(WorkAttributeConstant.WAIT_EVENT_NAMES);
        if (attVal == null)
            return new ArrayList<>();
        return Attribute.parseTable(attVal, ',', ';', 3);
    }

    /**
     * Register activity wait event names.
     */
    protected EventWaitInstance registerWaitEvents(boolean reregister)
            throws ActivityException {
        return registerWaitEvents(reregister, true);
    }

    @Deprecated
    /**
     * Deprecated Pre-arrival check is handled separately.
     * Use {@link #registerWaitEvents(boolean) registerWaitEvents}.
     */
    protected EventWaitInstance registerWaitEvents(boolean reregister, boolean suppressNotify)
    throws ActivityException {
        List<String[]> eventSpecs = this.getWaitEventSpecs();
        if (eventSpecs.isEmpty())
            return null;
        String[] eventNames = new String[eventSpecs.size()];
        String[] eventCompletionCodes = new String[eventSpecs.size()];
        boolean[] eventOccurances = new boolean[eventSpecs.size()];
        for (int i = 0; i < eventNames.length; i++) {
            eventNames[i] = translatePlaceHolder(eventSpecs.get(i)[0]);
            eventCompletionCodes[i] = eventSpecs.get(i)[1];
            if (eventSpecs.get(i)[1] == null) {
                eventCompletionCodes[i] = EventType.EVENTNAME_FINISH;
            } else {
                eventCompletionCodes[i] = eventSpecs.get(i)[1].trim();
                if (eventCompletionCodes[i].length() == 0)
                    eventCompletionCodes[i] = EventType.EVENTNAME_FINISH;
            }
            String eventOccur = eventSpecs.get(i)[2];
            eventOccurances[i] = (eventOccur != null && eventOccur.equalsIgnoreCase("true"));
        }
        try {
            EventWaitInstance received = getEngine().createEventWaitInstances(
                    this.getProcessInstanceId(),
                    this.getActivityInstanceId(),
                    eventNames,
                    eventCompletionCodes,
                    eventOccurances, !suppressNotify, reregister);
            return received;
        } catch (Exception ex) {
            super.logexception(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage());
        }
    }

    protected final void deregisterEvents() throws ActivityException {
        List<String[]> eventSpecs = this.getWaitEventSpecs();
        if (eventSpecs.isEmpty()) return;
        try {
            getEngine().removeEventWaitForActivityInstance(getActivityInstanceId(), "activity completed w/o event");
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    protected final Integer handleEventCompletionCode() throws ActivityException {
        Integer actInstStatus;
        String compCode = this.getReturnCode();
        if (compCode!=null && (compCode.length()==0||compCode.equals(EventType.FINISH.toString())))
            compCode = null;
        String actInstStatusName = this.getAttributeValue(WorkAttributeConstant.STATUS_AFTER_EVENT);
        if (actInstStatusName!=null) {
            if (actInstStatusName.equals(WorkStatus.STATUSNAME_CANCELLED)) actInstStatus = WorkStatus.STATUS_CANCELLED;
            else if (actInstStatusName.equals(WorkStatus.STATUSNAME_WAITING)) actInstStatus = WorkStatus.STATUS_WAITING;
            else {
                actInstStatusName = WorkStatus.STATUSNAME_HOLD;
                actInstStatus = WorkStatus.STATUS_HOLD;
            }
        } else {
            actInstStatusName = WorkStatus.STATUSNAME_HOLD;
            actInstStatus = WorkStatus.STATUS_HOLD;
        }
        if (compCode==null) compCode = actInstStatusName + "::";
        else compCode = actInstStatusName + "::" + compCode;
        this.setReturnCode(compCode);
        if (actInstStatus.equals(WorkStatus.STATUS_WAITING)) {
            this.registerWaitEvents(true);
        }
        return actInstStatus;
    }

    protected String getMessageFromEventMessage(InternalEvent eventMessageDoc)
        throws ActivityException {
        if (eventMessageDoc.getParameters()!=null) {
            String msg = eventMessageDoc.getParameters().get("ExternalEventMessage");
            if (msg!=null) return msg;
        }
        Long extEventInstId = eventMessageDoc.getSecondaryOwnerId();
        return this.getExternalEventInstanceDetails(extEventInstId);
    }

    public boolean needSuspend() {
        return true;
    }

    public boolean resume(InternalEvent eventMessageDoc)
    throws ActivityException {
        return true;
    }

}
