/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.work;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class WorkTransitionInstanceVO implements Jsonable, Serializable {

    private Long transitionInstanceID;
    private Long transitionID;
    private Long processInstanceID;
    private Long destinationID;     // to-activity ID at initialization; to-activity instance ID at completion
    private int statusCode;
    private String startDate;
    private String endDate;

    public WorkTransitionInstanceVO(){
        this.processInstanceID = null;
        this.statusCode = 0;
        this.startDate = null;
        this.endDate = null;
    }

    public WorkTransitionInstanceVO(JSONObject json) throws JSONException {
        if (json.has("id"))
            transitionInstanceID = json.getLong("id");
        if (json.has("transitionId"))
            transitionID = json.getLong("transitionId");
        if (json.has("processInstanceId"))
            processInstanceID = json.getLong("processInstanceId");
        if (json.has("destinationId"))
            destinationID = json.getLong("destinationId");
        if (json.has("statusCode"))
            statusCode = json.getInt("statusCode");
        if (json.has("startDate"))
            startDate = json.getString("startDate");
        if (json.has("endDate"))
            endDate = json.getString("endDate");
    }

    public Long getProcessInstanceID() {
        return processInstanceID;
    }

    public void setProcessInstanceID(Long pID) {
        this.processInstanceID = pID;
    }

     public Long getTransitionInstanceID() {
        return transitionInstanceID;
    }

    public void setTransitionInstanceID(Long pID) {
        this.transitionInstanceID = pID;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int stCD) {
        this.statusCode = stCD;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Long getTransitionID() {
        return transitionID;
    }

    public void setTransitionID(Long transitionID) {
        this.transitionID = transitionID;
    }

    public Long getDestinationID() {
        return destinationID;
    }

    public void setDestinationID(Long destinationID) {
        this.destinationID = destinationID;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (transitionInstanceID != null)
            json.put("id", transitionInstanceID);
        if (transitionID != null)
            json.put("transitionId", transitionID);
        if (processInstanceID != null)
            json.put("processInstanceId", processInstanceID);
        if (destinationID != null)
            json.put("destinationId", destinationID);
        if (statusCode > 0)
            json.put("statusCode", statusCode);
        if (startDate != null)
            json.put("startDate", startDate);
        if (endDate != null)
            json.put("endDate", endDate);
        return json;
    }

    public String getJsonName() {
        return "TransitionInstance";
    }
}
