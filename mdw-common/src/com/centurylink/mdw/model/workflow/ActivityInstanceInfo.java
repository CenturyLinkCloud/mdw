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

@ApiModel(value="Activity", description="MDW runtime activity instance")
public class ActivityInstanceInfo implements Jsonable, Serializable {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String definitionId;
    public String getDefinitionId() { return definitionId;}
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }

    private Date startDate;
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    private Date endDate;
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

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

    private Long activityInstanceId;
    public Long getactivityInstanceId() { return activityInstanceId;}
    public void setActivityInstanceId(Long activityInstanceId) { this.activityInstanceId = activityInstanceId; }

    private boolean definitionMissing;
    public boolean isDefinitionMissing() { return definitionMissing; }
    public void setDefinitionMissing(boolean defMissing) { this.definitionMissing = defMissing; }

    private String processVersion = "0";
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String processVersion) { this.processVersion = processVersion; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    public ActivityInstanceInfo() {
    }

    public ActivityInstanceInfo(JSONObject jsonObj) throws JSONException {
        if (jsonObj.has("id"))
            id = jsonObj.getLong("id");
        if (jsonObj.has("name"))
            name = jsonObj.getString("name");
        if (jsonObj.has("definitionId"))
            definitionId = jsonObj.getString("definitionId");
        if (jsonObj.has("startDate"))
            startDate = StringHelper.stringToDate(jsonObj.getString("startDate"));
        if (jsonObj.has("endDate"))
            endDate = StringHelper.stringToDate(jsonObj.getString("endDate"));
        if (jsonObj.has("status"))
            status = jsonObj.getString("status");
        if (jsonObj.has("result"))
            result = jsonObj.getString("result");
        if (jsonObj.has("message"))
            message = jsonObj.getString("message");
        if (jsonObj.has("masterRequestId"))
            masterRequestId = jsonObj.getString("masterRequestId");
        if (jsonObj.has("processName"))
            processName = jsonObj.getString("processName");
        if (jsonObj.has("processVersion"))
            processVersion = jsonObj.getString("processVersion");
        if (jsonObj.has("packageName"))
            packageName = jsonObj.getString("packageName");
    }

    public String getJsonName() {
        return "ActivityInstance";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null)
            json.put("id", id);
        if (processInstanceId != null)
            json.put("processInstanceId", processInstanceId);
        if (activityInstanceId != null)
            json.put("activityInstanceId", activityInstanceId);
        if (name != null)
            json.put("name", name);
        if (definitionId != null)
            json.put("definitionId", definitionId);
        if (startDate != null)
            json.put("startDate", StringHelper.dateToString(startDate));
        if (endDate != null)
            json.put("endDate", StringHelper.dateToString(endDate));
        if (status != null)
            json.put("status", status);
        if (result != null)
            json.put("result", result);
        if (message != null)
            json.put("message", message);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (processName != null)
            json.put("processName", processName);
        if (processVersion != null)
            json.put("processVersion", processVersion);
        if (packageName != null)
            json.put("packageName", packageName);
        return json;
    }
}
