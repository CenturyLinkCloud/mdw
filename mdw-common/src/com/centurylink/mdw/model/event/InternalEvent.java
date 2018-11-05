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
package com.centurylink.mdw.model.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.EventMessageDocument;
import com.centurylink.mdw.bpm.EventMessageDocument.EventMessage;
import com.centurylink.mdw.bpm.EventParametersDocument.EventParameters;
import com.centurylink.mdw.bpm.EventTypeDocument;
import com.centurylink.mdw.bpm.ParameterDocument.Parameter;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.bpm.WorkTypeDocument;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;

public class InternalEvent {

    private boolean isProcess;
    private Long workId;
    private Long transitionInstanceId;
    private Integer eventType;
    private String ownerType;
    private Long ownerId;
    private String masterRequestId;
    private String completionCode;
    private Long workInstanceId;

    private String secondaryOwnerType;
    private Long secondaryOwnerId;
    private Map<String,String> parameters;
    private int messageDelay;
    private String statusMessage;
    private int deliveryCount;

    private String messageId;

    private InternalEvent() {
        isProcess = false;
        messageDelay = 0;
        statusMessage = null;
        parameters = null;
        secondaryOwnerType = null;
        deliveryCount = 0;
        setMessageId(null);
    }

    public InternalEvent(String xmlMessage) throws XmlException {
        this();
        EventMessageDocument messageEventDoc = EventMessageDocument.Factory.parse(xmlMessage, Compatibility.namespaceOptions());
        fromEventMessageDocument(messageEventDoc);
    }

    private void fromEventMessageDocument(EventMessageDocument msgDoc) {
        EventMessage aMsg = msgDoc.getEventMessage();
        isProcess = aMsg.getWorkType().equals(WorkTypeDocument.WorkType.PROCESS);
        eventType = EventType.getEventTypeFromName(aMsg.getEventType().toString());
        workId = aMsg.getWorkId();
        transitionInstanceId = aMsg.getWorkTransitionInstanceId();
        if (transitionInstanceId.longValue()==0L) transitionInstanceId = null;
        ownerId = aMsg.getWorkOwnerId();
        ownerType = aMsg.getWorkOwnerType();
        masterRequestId = aMsg.getMasterRequestId();
        workInstanceId = aMsg.getWorkInstanceId();
        if (workInstanceId.longValue()==0L) workInstanceId = null;
        secondaryOwnerType = aMsg.getSecondaryWorkOwnerType();
        if (secondaryOwnerType!=null) secondaryOwnerId = aMsg.getSecondaryWorkOwnerId();
        completionCode = aMsg.getWorkCompletionCode();
        EventParameters params = aMsg.getEventParameters();
        if (params!=null && !params.isNil()) {
            parameters = new HashMap<String,String>();
            for (Parameter param : params.getParameterList()) {
                parameters.put(param.getName(), param.getStringValue());
            }
        }
        messageDelay = aMsg.getMessageDelay();
        statusMessage = aMsg.getStatusMessage();
        deliveryCount = aMsg.getDeliveryCount();
    }

    /**
     * Method that creates the event params based on the passed in Map
     * @param pParams
     * @return EventParameters
     */
    private EventParameters createEventParameters(Map<String,String> pParams){
        EventParameters evParams = EventParameters.Factory.newInstance();
        for (String name : pParams.keySet()) {
             String val = pParams.get(name);
             if(val == null){
                 continue;
             }
             Parameter evParam = evParams.addNewParameter();
             evParam.setName(name);
             evParam.setStringValue(val);
         }
         return evParams;

    }

    // procInstId is populated when the process instance is already created. o/w null
    public static InternalEvent createProcessStartMessage(
            Long processId, String parentType, Long parentId,
            String masterRequestId, Long procInstId,
            String secondaryOwnerType, Long secondaryOwnerId) {
        InternalEvent event = new InternalEvent();
        event.isProcess = true;
        event.workId = processId;
        event.eventType = EventType.START;
        event.ownerType = parentType;
        event.ownerId = parentId;
        event.masterRequestId = masterRequestId;
        event.workInstanceId = procInstId;
//        event.parameters = params;
        event.secondaryOwnerType = secondaryOwnerType;
        event.secondaryOwnerId = secondaryOwnerId;
        return event;
    }

    public static InternalEvent createProcessFinishMessage(ProcessInstance processInst){
        InternalEvent event = new InternalEvent();
        event.isProcess = true;
        event.workId = processInst.getOwner().equals(OwnerType.MAIN_PROCESS_INSTANCE) ?
                new Long(processInst.getComment()) : processInst.getProcessId();
        event.transitionInstanceId = null;
        event.eventType = EventType.FINISH;
        event.ownerType = processInst.getOwner();
        event.ownerId = processInst.getOwnerId();
        event.workInstanceId = processInst.getId();
        event.masterRequestId = processInst.getMasterRequestId();
        return event;
    }

    public static InternalEvent createProcessAbortMessage(ProcessInstance processInst){
        InternalEvent event = new InternalEvent();
        event.isProcess = true;
        event.workInstanceId = processInst.getId();
        return event;
    }

    public static InternalEvent createProcessDelayMessage(ProcessInstance procInst) {
        InternalEvent event = new InternalEvent();
        event.isProcess = true;
        event.eventType = EventType.DELAY;
        event.workId = procInst.getProcessId();
        event.workInstanceId = procInst.getId();
        event.ownerType = OwnerType.SLA;
        event.ownerId = 0L;
        return event;
    }

    public static InternalEvent createActivityDelayMessage(ActivityInstance actInst,
            String masterRequestId) {
        InternalEvent event = new InternalEvent();
        event.workId = actInst.getActivityId();
        event.transitionInstanceId = null;
        event.eventType = EventType.DELAY;
        event.ownerType = OwnerType.PROCESS_INSTANCE;
        event.ownerId = actInst.getProcessInstanceId();
        event.workInstanceId = actInst.getId();
        event.masterRequestId = masterRequestId;
        event.secondaryOwnerType = OwnerType.SLA;    // just an indicator for new style
        event.secondaryOwnerId = 1L;
        return event;
    }

    public static InternalEvent createActivityStartMessage(
            Long actId, Long procInstId, Long transInstId, String masterRequestId, String compCode){
        InternalEvent event = new InternalEvent();
        event.workId = actId;
        event.transitionInstanceId = transInstId;
        event.eventType = EventType.START;
        event.ownerType = OwnerType.PROCESS_INSTANCE;
        event.ownerId = procInstId;
        event.masterRequestId = masterRequestId;
        if (compCode!=null) event.completionCode = compCode;
        return event;
    }

    public static InternalEvent createActivityErrorMessage(
            Long actId, Long actInstId, Long procInstId, String compCode,
            String masterRequestId, String statusMessage, Long docId){
        InternalEvent event = new InternalEvent();
        event.workId = actId;
        event.transitionInstanceId = null;
        event.eventType = EventType.ERROR;
        event.ownerType = OwnerType.PROCESS_INSTANCE;
        event.ownerId = procInstId;
        event.masterRequestId = masterRequestId;
        event.workInstanceId = actInstId;
        event.completionCode = compCode;
        event.statusMessage = statusMessage;
        event.secondaryOwnerId = docId;
        return event;
    }

    /**
     * create activity FINISH, ABORT, RESUME, CORRECT, ERROR and any event type that can be specified in
     * designer configuration for events.
     *
     * @param ai
     * @param eventType
     * @param masterRequestId
     * @param compCode
     * @return
     */
    public static InternalEvent createActivityNotifyMessage(ActivityInstance ai,
            Integer eventType, String masterRequestId, String compCode) {
        InternalEvent event = new InternalEvent();
        event.workId = ai.getActivityId();
        event.transitionInstanceId = null;
        event.eventType = eventType;
        event.ownerType = OwnerType.PROCESS_INSTANCE;
        event.ownerId = ai.getProcessInstanceId();
        event.masterRequestId = masterRequestId;
        event.workInstanceId = ai.getId();
        event.completionCode = compCode;
        return event;
    }

    public Long getWorkId() {
        return workId;
    }

    public void setWorkId(Long workId) {
        this.workId = workId;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getMasterRequestId() {
        return masterRequestId;
    }

    public void setMasterRequestId(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    public String getCompletionCode() {
        return completionCode;
    }

    public void setCompletionCode(String completionCode) {
        this.completionCode = completionCode;
    }

    public String getSecondaryOwnerType() {
        return secondaryOwnerType;
    }

    public void setSecondaryOwnerType(String secondaryOwnerType) {
        this.secondaryOwnerType = secondaryOwnerType;
    }

    public Long getSecondaryOwnerId() {
        return secondaryOwnerId;
    }

    public void setSecondaryOwnerId(Long secondaryOwnerId) {
        this.secondaryOwnerId = secondaryOwnerId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String name, String value) {
        if (parameters==null) parameters = new HashMap<String,String>();
        parameters.put(name, value);
    }

    public int getMessageDelay() {
        return messageDelay;
    }

    public void setMessageDelay(int messageDelay) {
        this.messageDelay = messageDelay;
    }

    public Long getTransitionInstanceId() {
        return transitionInstanceId;
    }

    public void setTransitionInstanceId(Long transitionInstanceId) {
        this.transitionInstanceId = transitionInstanceId;
    }

    public Long getWorkInstanceId() {
        return workInstanceId;
    }

    public void setWorkInstanceId(Long workInstanceId) {
        this.workInstanceId = workInstanceId;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(int deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public boolean isProcess() {
        return isProcess;
    }

    public void setProcess(boolean isProcess) {
        this.isProcess = isProcess;
    }

    private EventMessageDocument toEventMessageDocument() {

        EventMessageDocument msgDoc = EventMessageDocument.Factory.newInstance();
        EventMessage aMsg = msgDoc.addNewEventMessage();
        String eventTypeString = EventType.getEventTypes().get(eventType);
        aMsg.setEventType(EventTypeDocument.EventType.Enum.forString(eventTypeString));
        aMsg.setWorkId(workId.longValue());
        aMsg.setWorkTransitionInstanceId(transitionInstanceId!=null?transitionInstanceId.longValue():0L);
        aMsg.setWorkOwnerId(ownerId.longValue());
        aMsg.setWorkOwnerType(ownerType);
        aMsg.setMasterRequestId(masterRequestId);
        aMsg.setWorkType(isProcess?WorkTypeDocument.WorkType.PROCESS:WorkTypeDocument.WorkType.ACTIVITY);
        aMsg.setWorkInstanceId(workInstanceId!=null?workInstanceId.longValue():0L);
        if (secondaryOwnerType!=null && secondaryOwnerId!=null) {
            aMsg.setSecondaryWorkOwnerId(secondaryOwnerId.longValue());
            aMsg.setSecondaryWorkOwnerType(secondaryOwnerType);
        }
        if (completionCode!=null) aMsg.setWorkCompletionCode(completionCode);
        if (parameters!=null && !parameters.isEmpty()) {
            EventParameters params = createEventParameters(parameters);
            aMsg.setEventParameters(params);
        }
        if (messageDelay!=0) aMsg.setMessageDelay(messageDelay);
        if (statusMessage!=null) aMsg.setStatusMessage(statusMessage);
        if (deliveryCount!=0) aMsg.setDeliveryCount(deliveryCount);
        return msgDoc;
    }

    public String toXml() {
        EventMessageDocument msgDoc = this.toEventMessageDocument();
        return msgDoc.xmlText((new XmlOptions()).setUseDefaultNamespace());
    }

    @Override
    public String toString() {
        return toXml();
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

}


