/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.StringHelper;


/**
 * Base class for all the Wait Activities
 * This class will be extended by the custom activity
 * that do not want a completion event raised.
 * This will act as a stopper for the workflow
 * In cases where in case of an error or a maual task creation,
 * we can use this activity to pause the flow and the flow can be
 * re started from an external event.
 */
public abstract class AbstractWait extends DefaultActivityImpl implements SuspendibleActivity {

    private List<String[]> getWaitEventSpecs() {
        String attVal = this.getAttributeValue(WorkAttributeConstant.WAIT_EVENT_NAMES);
        if (attVal==null) return new ArrayList<String[]>();
        return StringHelper.parseTable(attVal, ',', ';', 3);
    }

    protected EventWaitInstance registerWaitEvents(boolean reregister, boolean check_if_arrvied)
    throws ActivityException {
        List<String[]> eventSpecs = this.getWaitEventSpecs();
        if (eventSpecs.isEmpty()) return null;
        String[] eventNames = new String[eventSpecs.size()];
        String[] eventCompletionCodes = new String[eventSpecs.size()];
        boolean[] eventOccurances = new boolean[eventSpecs.size()];
        for (int i=0; i<eventNames.length; i++) {
            eventNames[i] = translatePlaceHolder(eventSpecs.get(i)[0]);
            eventCompletionCodes[i] = eventSpecs.get(i)[1];
            if (eventSpecs.get(i)[1]==null) {
                eventCompletionCodes[i] = EventType.EVENTNAME_FINISH;
            } else {
                eventCompletionCodes[i] = eventSpecs.get(i)[1].trim();
                if (eventCompletionCodes[i].length()==0)
                    eventCompletionCodes[i] = EventType.EVENTNAME_FINISH;
            }
            String eventOccur = eventSpecs.get(i)[2];
            eventOccurances[i] = (eventOccur==null || eventOccur.length()==0
                    || eventOccur.equalsIgnoreCase("true"));
        }
        try {
            EventWaitInstance received = getEngine().createEventWaitInstances(
                    this.getActivityInstanceId(),
                    eventNames,
                    eventCompletionCodes,
                    eventOccurances, !check_if_arrvied);
            return received;
        } catch (Exception ex) {
            super.logexception(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage());
        }
    }

    protected EventWaitInstance registerWaitEvent(String eventName, String completionCode,
            boolean recurring, boolean check_if_arrvied)
        throws ServiceLocatorException, DataAccessException, ProcessException {
        if (StringHelper.isEmpty(completionCode))
            completionCode = EventType.EVENTNAME_FINISH;
        EventWaitInstance received = getEngine().createEventWaitInstance(
                this.getActivityInstanceId(),
                eventName,
                completionCode, recurring, !check_if_arrvied);
        return received;
    }

    protected final void deregisterEvents() throws ActivityException {
        List<String[]> eventSpecs = this.getWaitEventSpecs();
        if (eventSpecs.isEmpty()) return;
        try {
            getEngine().removeEventWaitForActivityInstance(getActivityInstanceId(),
                    "activity completed w/o event");
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
            this.registerWaitEvents(true, false);
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
