/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.event;

import java.io.Serializable;
import java.util.Date;

public class ExternalEventInstance implements Serializable{

    private String eventName;
    private Long externalEventInstanceId;
    private Long eventId;
    private Date createdDate;
    private String eventData;
    private Long processInstanceId;
    private Long processId;
    private String processName;
    private Integer processInstanceStatus;
    private String masterRequestId;

    public ExternalEventInstance(){
    }

    public ExternalEventInstance(Long pInstId, Date pCreateDt, Long pEventId, String pEventName, String pEventData, Long pProcessInstId,
         Integer pProcessInstStatus, Long pProcessId, String pProcessName, String pMasterReqId){
        this.eventName = pEventName;
        this.eventId = pEventId;
        this.eventData = pEventData;
        this.createdDate = pCreateDt;
        this.externalEventInstanceId = pInstId;
        this.processId = pProcessId;
        this.processInstanceId = pProcessInstId;
        this.processInstanceStatus = pProcessInstStatus;
        this.masterRequestId = pMasterReqId;
        this.processName = pProcessName;
    }

    // PUBLIC AND PROTECTED METHODS -----------------------------------
    /**
     * @return the createdDate
     */
    public Date getCreatedDate() {
        return createdDate;
    }
    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    /**
     * @return the eventData
     */
    public String getEventData() {
        return eventData;
    }
    /**
     * @param eventData the eventData to set
     */
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    /**
     * @return the eventId
     */
    public Long getEventId() {
        return eventId;
    }
    /**
     * @param eventId the eventId to set
     */
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
    /**
     * @return the eventName
     */
    public String getEventName() {
        return eventName;
    }
    /**
     * @param eventName the eventName to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    /**
     * @return the externalEventInstanceId
     */
    public Long getExternalEventInstanceId() {
        return externalEventInstanceId;
    }
    /**
     * @param externalEventInstanceId the externalEventInstanceId to set
     */
    public void setExternalEventInstanceId(Long externalEventInstanceId) {
        this.externalEventInstanceId = externalEventInstanceId;
    }
    /**
     * @return the masterRequestId
     */
    public String getMasterRequestId() {
        return masterRequestId;
    }
    /**
     * @param masterRequestId the masterRequestId to set
     */
    public void setMasterRequestId(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }
    /**
     * @return the processInstanceId
     */
    public Long getProcessInstanceId() {
        return processInstanceId;
    }
    /**
     * @param processInstanceId the processInstanceId to set
     */
    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * @return the processId
     */
    public Long getProcessId() {
        return processId;
    }
    /**
     * @param processId the processId to set
     */
    public void setProcessId(Long processId) {
        this.processId = processId;
    }
    /**
     * @return the processInstanceStatus
     */
    public Integer getProcessInstanceStatus() {
        return processInstanceStatus;
    }
    /**
     * @param processInstanceStatus the processInstanceStatus to set
     */
    public void setProcessInstanceStatus(Integer processInstanceStatus) {
        this.processInstanceStatus = processInstanceStatus;
    }
    /**
     * @return the processName
     */
    public String getProcessName() {
        return processName;
    }
    /**
     * @param processName the processName to set
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }
}
