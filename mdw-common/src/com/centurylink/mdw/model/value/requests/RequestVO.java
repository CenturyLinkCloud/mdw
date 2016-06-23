/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.requests;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkStatuses;

public class RequestVO implements Serializable, Jsonable {
    public static final String REQUEST_JSON_NAME = "Requests";
    public static final String LISTENER_REQUEST = "LISTENER_REQUEST";
    public static final String LISTENER_RESPONSE = "LISTENER_RESPONSE";

    private Long requestId;
    private Long ownerId;
    private Long processInstanceId;
    private Long processId;
    private Integer processInstStatusCode;
    private Date receivedDate;
    private String processName;
    private String orderId;
    private String searchKey1;
    private String searchKey2;
    private String content;
    private String ownerType;

    public Long getRequestId() {
        return requestId;
    }
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public Long getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    public String getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }
    public Date getReceivedDate() {
        return receivedDate;
    }
    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getSearchKey1() {
        return searchKey1;
    }
    public void setSearchKey1(String searchKey1) {
        this.searchKey1 = searchKey1;
    }
    public String getSearchKey2() {
        return searchKey2;
    }
    public void setSearchKey2(String searchKey2) {
        this.searchKey2 = searchKey2;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getProcessName() {
        return processName;
    }
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    public Long getProcessInstanceId() {
        return processInstanceId;
    }
    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    public Long getProcessId() {
        return processId;
    }
    public void setProcessId(Long processId) {
        this.processId = processId;
    }
    public Integer getProcessInstStatusCode() {
        return processInstStatusCode;
    }
    public void setProcessInstStatusCode(Integer processInstStatusCode) {
        this.processInstStatusCode = processInstStatusCode;
    }
    public String getProcessInstanceStatus() {
        return WorkStatuses.getWorkStatuses().get(processInstStatusCode);
    }

    public void setProcessInstanceStatus(String processInstanceStatus) {
        for (int i = 0; i < WorkStatus.allStatusNames.length; i++) {
            if (WorkStatus.allStatusNames[i].equals(processInstanceStatus)) {
                processInstStatusCode = WorkStatus.allStatusCodes[i];
                break;
            }
        }
    }

    public RequestVO() {
    }

    public RequestVO(Long requestId, Long ownerId, Date receivedDate,
            String searchKey1, String searchKey2, String content, String ownerType) {
        super();
        this.requestId = requestId;
        this.ownerId = ownerId;
        this.receivedDate = receivedDate;
        this.searchKey1 = searchKey1;
        this.searchKey2 = searchKey2;
        this.content = content;
        this.ownerType = ownerType;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject requestJson = new JSONObject();
        requestJson.put("requestId", requestId);
        requestJson.put("orderId", orderId);
        requestJson.put("process", processInstanceId);
        requestJson.put("status", getProcessInstanceStatus());
        requestJson.put("receivedDate", receivedDate);
        requestJson.put("searchKey1", searchKey1);
        requestJson.put("searchKey2", searchKey2);
        requestJson.put("searchKey2", searchKey2);
        return requestJson;
    }

    @Override
    public String getJsonName() {
        return REQUEST_JSON_NAME;
    }
}
