/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

// TODO: redundant values: definitionId/activityId, status/statusCode, result/completionCode
@ApiModel(value="Activity", description="MDW runtime activity instance")
public class ActivityInstance implements Jsonable, Serializable {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String definitionId;
    public String getDefinitionId() { return definitionId;}
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }

    private Long activityId;
    @ApiModelProperty(hidden=true)
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private Date startDate;
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    private Date endDate;
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private int statusCode;
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private String completionCode;
    public String getCompletionCode() { return completionCode; }
    public void setCompletionCode(String completionCode) { this.completionCode = completionCode; }

    private String result;
    public String getResult() { return result;}
    public void setResult(String result) { this.result = result; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private Long processId;
    public Long getProcessId() { return processId;}
    public void setProcessId(Long processId) { this.processId = processId; }

    private Long processInstanceId;
    public Long getProcessInstanceId() { return processInstanceId;}
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }

    private boolean definitionMissing;
    public boolean isDefinitionMissing() { return definitionMissing; }
    public void setDefinitionMissing(boolean defMissing) { this.definitionMissing = defMissing; }

    private String processVersion = "0";
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String processVersion) { this.processVersion = processVersion; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    public ActivityInstance() {
    }

    public ActivityInstance(JSONObject json) throws JSONException {
        if (json.has("id"))
            id = json.getLong("id");
        if (json.has("activityId"))
            activityId = json.getLong("activityId");
        if (json.has("definitionId"))
            definitionId = json.getString("definitionId");
        if (json.has("name"))
            name = json.getString("name");
        if (json.has("startDate"))
            startDate = StringHelper.stringToDate(json.getString("startDate"));
        if (json.has("endDate"))
            endDate = StringHelper.stringToDate(json.getString("endDate"));
        if (json.has("statusCode"))
            statusCode = json.getInt("statusCode");
        if (json.has("status"))
            status = json.getString("status");
        if (json.has("message"))
            message = json.getString("message");
        if (json.has("completionCode"))
            completionCode = json.getString("completionCode");
        if (json.has("result"))
            result = json.getString("result");
        if (json.has("masterRequestId"))
            masterRequestId = json.getString("masterRequestId");
        if (json.has("processName"))
            processName = json.getString("processName");
        if (json.has("processVersion"))
            processVersion = json.getString("processVersion");
        if (json.has("packageName"))
            packageName = json.getString("packageName");
        if (json.has("processInstanceId"))
            processInstanceId = json.getLong("processInstanceId");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null)
            json.put("id", id);
        if (activityId != null)
            json.put("activityId", activityId);
        if (definitionId != null)
            json.put("definitionId", definitionId);
        if (name != null)
            json.put("name", name);
        if (startDate != null)
            json.put("startDate", StringHelper.dateToString(startDate));
        if (endDate != null)
            json.put("endDate", StringHelper.dateToString(endDate));
        if (statusCode > 0)
            json.put("statusCode", statusCode);
        if (status != null)
            json.put("status", status);
        if (message != null)
            json.put("message", message);
        if (status != null)
            json.put("status", status);
        if (completionCode != null)
            json.put("completionCode", completionCode);
        if (result != null)
            json.put("result", result);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (processName != null)
            json.put("processName", processName);
        if (processVersion != null)
            json.put("processVersion", processVersion);
        if (packageName != null)
            json.put("packageName", packageName);
        if (processInstanceId != null)
            json.put("processInstanceId", processInstanceId);
        return json;
    }

    public String getJsonName() {
        return "ActivityInstance";
    }


}
