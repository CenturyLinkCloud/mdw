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
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.CompletionCode;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractWait;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@Tracked(LogLevel.TRACE)
public class EventWaitActivity extends AbstractWait implements com.centurylink.mdw.activity.types.EventWaitActivity {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

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
        }
        else {
            try {
                // set timeouts
                int timeout = getTimeoutSeconds();
                if (timeout > 0) {
                    loginfo("set activity timeout as " + timeout + " seconds");
                    InternalEvent delayMsg = InternalEvent.createActivityDelayMessage(getActivityInstance(), getMasterRequestId());
                    getEngine().sendDelayedInternalEvent(delayMsg, timeout,
                            ScheduledEvent.INTERNAL_EVENT_PREFIX+getActivityInstanceId()+"timeout", false);
                }
            }
            catch (MdwException e) {
                throw new ActivityException(0, "Failed to set timeout", e);
            }
        }
    }

    protected boolean isEventRecurring(String completionCode) {
        for (String[] eventSpec : getWaitEventSpecs()) {
            if (completionCode.equals(eventSpec[1])
               || eventSpec[1] == null || eventSpec[1].equals(""))
                return Boolean.parseBoolean(eventSpec[2]);
        }
        return false;
    }

    protected int getTimeoutSeconds() throws PropertyException {
        String sla = getAttributeValueSmart(WorkAttributeConstant.SLA);
        if (sla == null || sla.length() == 0)
            return 0;
        String unit = getAttributeValue(WorkAttributeConstant.SLA_UNITS);
        if (StringUtils.isBlank(unit))
            unit = getAttributeValue(WorkAttributeConstant.SLA_UNIT);
        if (StringUtils.isBlank(unit))
            unit = ServiceLevelAgreement.INTERVAL_SECONDS;
        return ServiceLevelAgreement.unitsToSeconds(sla, unit);
    }

    /**
     * @return a table of event specifications
     */
    public List<String[]> getWaitEventSpecs() {
        String attVal = this.getAttributeValue(WorkAttributeConstant.WAIT_EVENT_NAMES);
        return Attribute.parseTable(attVal, ',', ';', 3);
    }

    protected void logMessage(String responseData) {
        try {
            // TODO this duplicates the data in document table. Any better solution?
            // this information is needed to display in designer only
            // same is true for GeneralManualTaskActivity
            this.createDocument(String.class.getName(), responseData,
                    OwnerType.ADAPTER_RESPONSE, getActivityInstanceId());
        } catch (Exception ex) {
            logger.severeException("Failed to log message", ex);
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

    /**
     * You cannot override this method. Override {@link processMessage(String)} instead.
     *
     * This method is called when the message is received after registration. It extracts the message,
     * records the message in ADAPTER_INSTANCE table, and invoke {@link processMessage(String)}.
     */
    public final boolean resume(InternalEvent eventMessageDoc) throws ActivityException {
        boolean toFinish;
        String secondaryOwnerType = eventMessageDoc.getSecondaryOwnerType();
        if (secondaryOwnerType!=null && secondaryOwnerType.equals(OwnerType.DOCUMENT)) {
            String responseData = super.getMessageFromEventMessage(eventMessageDoc);
            setReturnCodeAndExitStatus(eventMessageDoc.getCompletionCode());
            processMessage(responseData);
            toFinish = handleCompletionCode();
        } else {
            setReturnCodeAndExitStatus(eventMessageDoc.getCompletionCode());
            processMessage(null);
            toFinish = handleCompletionCode();
        }
        return toFinish;
    }

    /**
     * You should override this method to process event messages.
     *
     * The default method does nothing if Received Message Variable is not defined and returns WorkStatus.STATUS_COMPLETED.
     * If Received Message Variable is defined then
     * it would store the received message in that pre-defined document variable
     * Process Designer has to define a Document type variable in order to store the message received
     *
     * When the method returns WorkStatus.STATUS_COMPLETED, the engine will complete the activity and move on.
     * By default, the transition is determined based on null completion code and
     * FINISH event type. You can change the completion code by using the method
     * this.setReturnCode(). You can also change the event type from FINISH
     * to ABORT, CORRECT and DELAY, by prepending the completion code with
     * "ABORT:", "CORRECT:" and "DELAY:", respectively. Examples are
     * "ABORT:my-return-code" and "CORRECT:".
     *
     * You can leave the activity in a status other than WorkStatus.STATUS_COMPLETED by returning
     * a different status code.
     *
     * If WorkStatus.STATUS_WAITING is returned, your overridden method needs to re-register
     * the events to be waited on. The work flow does not transition away and the completion code
     * is ignored.
     *
     * You can also return WorkStatus.STATUS_HOLD or return WorkStatus.STATUS_CANCELLED.
     *
     * @return see above
     * @param message received message
     * @throws ActivityException ActivityException
     */
    protected void processMessage(String message) throws ActivityException {
        try {
            String rcvdMsgDocVar = getAttributeValueSmart(RECEIVED_MESSAGE_DOC_VAR);
            if (rcvdMsgDocVar != null && !rcvdMsgDocVar.isEmpty()) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(rcvdMsgDocVar);
                if (variableVO == null)
                    throw new ActivityException("Received Message Variable '" + rcvdMsgDocVar + "' is not defined or is not Document Type for process " + processVO.getQualifiedLabel());
                if (message != null) {
                    this.setParameterValueAsDocument(rcvdMsgDocVar, variableVO.getType(), message);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
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
            this.registerWaitEvents(true, true);
            return compCode != null && (compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_CORRECT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ABORT)
                    || compCode.startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ERROR));
        } else return true;
    }

    /**
     * Typically you will not override this method
     */
    public boolean resumeWaiting(InternalEvent eventMessageDoc) throws ActivityException {
        EventWaitInstance received = registerWaitEvents(true, true);
        if (received!=null) {
            setReturnCodeAndExitStatus(received.getCompletionCode());
            processMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
            return handleCompletionCode();
        } else return false;
    }

    protected final void setActivityWaitingOnExit() {
        this.exitStatus = WorkStatus.STATUS_WAITING;
    }

    protected final void setActivityHoldOnExit() {
        this.exitStatus = WorkStatus.STATUS_HOLD;
    }

    /**
     * Update SLA of this
     * @param seconds number of seconds from now as the new SLA
     * @throws ActivityException ActivityException
     */
    protected void updateSLA(int seconds) throws ActivityException {
        try {
            ProcessExecutor engine = this.getEngine();
            super.loginfo("Update activity timeout as " + seconds + " seconds");
            InternalEvent delayMsg = InternalEvent.createActivityDelayMessage(this.getActivityInstance(),
                    this.getMasterRequestId());
            String eventName = ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId() + "timeout";
            engine.sendDelayedInternalEvent(delayMsg, seconds, eventName, true);
        } catch (Exception e) {
            throw new ActivityException(-1, "Failed to update SLA for activity instance"
                    + this.getActivityInstanceId(), e);
        }
    }


}
