/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class ActivityInstance implements Jsonable, Serializable {

    private Long id;
    private Long definitionId;
    private String startDate;
    private String endDate;
    private int statusCode;
    private String status;
    private String statusMessage;
    private String completionCode;
    private Long ownerId;

    public ActivityInstance() {
    }

    public ActivityInstance(JSONObject json) throws JSONException {
        if (json.has("id"))
            id = json.getLong("id");
        if (json.has("activityId"))
            definitionId = json.getLong("activityId");
        if (json.has("startDate"))
            startDate = json.getString("startDate");
        if (json.has("endDate"))
            endDate = json.getString("endDate");
        if (json.has("statusCode"))
            statusCode = json.getInt("statusCode");
        if (json.has("statusMessage"))
            statusMessage = json.getString("statusMessage");
        if (json.has("completionCode"))
            completionCode = json.getString("completionCode");
        if (json.has("ownerId"))
            ownerId = json.getLong("ownerId");
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getDefinitionId() {
        return definitionId;
    }
    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
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
    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    public String getCompletionCode() {
        return completionCode;
    }
    public void setCompletionCode(String completionCode) {
        this.completionCode = completionCode;
    }
    public Long getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getJsonName() {
        return "ActivityInstance";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null)
            json.put("id", id);
        if (definitionId != null)
            json.put("activityId", definitionId);
        if (startDate != null)
            json.put("startDate", startDate);
        if (endDate != null)
            json.put("endDate", endDate);
        if (statusCode > 0)
            json.put("statusCode", statusCode);
        if (statusMessage != null)
            json.put("statusMessage", statusMessage);
        if (status != null)
            json.put("status", status);
        if (completionCode != null)
            json.put("completionCode", completionCode);
        if (ownerId != null)
            json.put("ownerId", ownerId);
        return json;
    }
}
