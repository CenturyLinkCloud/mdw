/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * This class represents process instances.
 * It is used for two purposes: a) represent memory image
 * of process instances in execution engine; b) represent
 * process instance runtime information for designer/task manager
 *
 * The following fields are used by designer only:
 *   - activity instances
 *   - transition instances
 *   - remote server
 *   - start and end dates
 *
 * Objects of this class can be created in two cases for designer:
 *   1) when displaying a list of process instances in the designer
 *   2) when returning a list of parent/ancestor process instances.
 * In the second case, the version is not returned (has value 0)
 *
 * For the engine, the objects can also be created in two cases:
 *   3) when starting a process, create the object from JMS message
 *      content
 *   4) when loading an existing process instance, where contents
 *      are loaded from database.
 *
 */
@ApiModel(value="Process", description="MDW workflow process instance")
public class ProcessInstance implements Serializable, Jsonable {

    /**
     * Creates a skeleton process instance VO (without an ID).
     */
    public ProcessInstance(Long processId, String processName) {
        setProcessName(processName);
        setProcessId(processId);
    }

    public ProcessInstance() {
    }

    /**
     * For ProcessLists containing only ids.
     */
    public ProcessInstance(Long id) {
        this.id = id;
    }

    public ProcessInstance(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public ProcessInstance(JSONObject jsonObj) throws JSONException {
        // summary info
        if (jsonObj.has("id"))
            this.id = jsonObj.getLong("id");
        if (jsonObj.has("masterRequestId"))
            masterRequestId = jsonObj.getString("masterRequestId");
        if (jsonObj.has("processId"))
            processId = jsonObj.getLong("processId");
        if (jsonObj.has("statusCode"))
            statusCode = jsonObj.getInt("statusCode");
        if (jsonObj.has("status"))
            status = jsonObj.getString("status");
        if (jsonObj.has("startDate"))
            startDate = jsonObj.getString("startDate");
        if (jsonObj.has("endDate"))
            endDate = jsonObj.getString("endDate");
        if (jsonObj.has("comments"))
            comment = jsonObj.getString("comments");
        if (jsonObj.has("completionCode"))
            completionCode = jsonObj.getString("completionCode");
        if (jsonObj.has("owner"))
            owner = jsonObj.getString("owner");
        if (jsonObj.has("ownerId"))
            ownerId = jsonObj.getLong("ownerId");
        if (jsonObj.has("secondaryOwner"))
            secondaryOwner = jsonObj.getString("secondaryOwner");
        if (jsonObj.has("secondaryOwnerId"))
            secondaryOwnerId = jsonObj.getLong("secondaryOwnerId");
        // detail info
        if (jsonObj.has("activities")) {
            JSONArray activitiesJson = jsonObj.getJSONArray("activities");
            activities = new ArrayList<ActivityInstance>();
            for (int i = 0; i < activitiesJson.length(); i++) {
                activities.add(new ActivityInstance(activitiesJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("transitions")) {
            JSONArray transitionsJson = jsonObj.getJSONArray("transitions");
            transitions = new ArrayList<TransitionInstance>();
            for (int i = 0; i < transitionsJson.length(); i++) {
                transitions.add(new TransitionInstance(transitionsJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("variables")) {
            JSONArray variablesJson = jsonObj.getJSONArray("variables");
            variables = new ArrayList<VariableInstance>();
            for (int i = 0; i < variablesJson.length(); i++) {
                variables.add(new VariableInstance(variablesJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("subprocesses")) {
            JSONArray subprocsJson = jsonObj.getJSONArray("subprocesses");
            subprocessInstances = new ArrayList<ProcessInstance>();
            for (int i = 0; i < subprocsJson.length(); i++)
                subprocessInstances.add(new ProcessInstance(subprocsJson.getJSONObject(i)));
        }
        if (jsonObj.has("processName"))
            processName = jsonObj.getString("processName");
        if (jsonObj.has("processVersion"))
            processVersion = jsonObj.getString("processVersion");
        if (jsonObj.has("packageName"))
            packageName = jsonObj.getString("packageName");
    }

    private Long id;
    public Long getId() { return id; }
    public void setId(Long l) { id = l; }

    private Long processId;
    public Long getProcessId() { return processId; }
    public void setProcessId(Long l) { processId = l; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String s) { processName = s; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    /**
     * Formatted.
     */
    private String processVersion;
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String s) { processVersion = s; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String s) { masterRequestId = s; }

    private String owner;
    public String getOwner() { return owner; }
    public void setOwner(String s) { owner = s; }

    private Long ownerId;
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long l) { ownerId = l; }

    private String secondaryOwner;
    public String getSecondaryOwner() { return secondaryOwner; }
    public void setSecondaryOwner(String s) { secondaryOwner = s; }

    private Long secondaryOwnerId;
    public Long getSecondaryOwnerId() { return secondaryOwnerId; }
    public long getSecondaryOwnerIdLongValue() {
        return secondaryOwnerId == null ? 0 : secondaryOwnerId.longValue();
    }
    public void setSecondaryOwnerId(Long l) { secondaryOwnerId = l; }

    private Integer statusCode;
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer i) { statusCode = i; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private String startDate;
    public String getStartDate() { return startDate; }
    public void setStartDate(String d) { startDate = d; }
    @ApiModelProperty(hidden=true)
    public void setStartDate(Date d) {
        startDate = StringHelper.dateToString(d);
    }

    private String completionCode;
    public String getCompletionCode() { return completionCode; }
    public void setCompletionCode(String s) { this.completionCode = s; }

    private List<VariableInstance> variables = null;
    public List<VariableInstance> getVariables() { return variables; }
    public void setVariables(List<VariableInstance> variables) { this.variables = variables; }

    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String d) { endDate = d; }
    @ApiModelProperty(hidden=true)
    public void setEndDate(Date d) {
        endDate = StringHelper.dateToString(d);
    }

    private String comment;
    public String getComment() { return comment; }
    public void setComment(String s) { comment = s; }

    // for run time information display only
    @ApiModelProperty(hidden=true)
    private String remoteServer;
    public String getRemoteServer() { return remoteServer; }
    public void setRemoteServer(String s) { remoteServer = s; }

    // for run time information display only
    @ApiModelProperty(hidden=true)
    private List<ActivityInstance> activities;
    public List<ActivityInstance> getActivities() { return activities; }
    public void setActivities(List<ActivityInstance> activities) { this.activities = activities; }
    public ActivityInstance getActivity(Long instanceId) {
        if (activities != null) {
            for (ActivityInstance activity : activities) {
                if (activity.getId().equals(instanceId))
                    return activity;
            }
        }
        return null;
    }

    // for run time information display only
    @ApiModelProperty(hidden=true)
    private List<TransitionInstance> transitions;
    public List<TransitionInstance> getTransitions() { return transitions; }
    public void setTransitions(List<TransitionInstance> t) { this.transitions = t; }

    // for run time information display only
    @ApiModelProperty(hidden=true)
    public boolean isRemote() {
        return remoteServer!=null;
    }

    // for designer run time information display only
    @ApiModelProperty(hidden=true)
    public VariableInstance getVariable(String name) {
        for (VariableInstance v : variables) {
            if (v.getName().equals(name)) return v;
        }
        return null;
    }

    // for tester expressions only
    @ApiModelProperty(hidden=true)
    private Map<String,Object> varMap;
    public Map<String,Object> getVariable() {
        if (varMap == null) {
            varMap = new HashMap<String,Object>();
            for (VariableInstance var : getVariables())
                varMap.put(var.getName(), var.getData());
        }
        return varMap;
    }

    // for designer run time information display only
    public void copyFrom(ProcessInstance copy) {
        this.activities = copy.activities;
        this.transitions = copy.transitions;
        this.variables = copy.variables;
    }

    // for designer run time information display only
    public List<TransitionInstance> getTransitionInstances(Long transId) {
        List<TransitionInstance> ret = new ArrayList<TransitionInstance>();
        for (TransitionInstance one : this.transitions) {
            if (one.getTransitionID().equals(transId)) ret.add(one);
        }
        return ret;
    }

    // for designer run time information display only
    public List<ActivityInstance> getActivityInstances(Long actId) {
        List<ActivityInstance> ret = new ArrayList<ActivityInstance>();
        for (ActivityInstance one : this.activities) {
            if (one.getDefinitionId().equals(actId)) ret.add(one);
        }
        return ret;
    }

    // for designer run time information display only
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProcessInstance) {
            ProcessInstance procInstVO = (ProcessInstance)obj;
            return procInstVO.id.equals(id)
               && (remoteServer==null&&procInstVO.remoteServer==null
                   ||remoteServer!=null&&remoteServer.equals(procInstVO.remoteServer));
        } else {
            return false;
        }
    }

    // for designer run time information display only
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isEmbedded() {
        return OwnerType.MAIN_PROCESS_INSTANCE.equals(owner);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        // summary info (for ProcessLists)
        json.put("id", this.id);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (processId != null)
            json.put("processId", processId);
        if (processName != null)
            json.put("processName", processName);
        if (processVersion != null)
            json.put("processVersion", processVersion);
        if (packageName != null)
            json.put("packageName", packageName);
        if (statusCode != null)
            json.put("statusCode", statusCode);
        if (status != null)
            json.put("status", status);
        if (startDate != null)
            json.put("startDate", startDate);
        if (endDate != null) {
            json.put("endDate", endDate);
            json.put("completionCode", completionCode == null ? "null" : completionCode);
        }
        if (comment != null)
            json.put("comments", comment);
        if (owner != null) {
            json.put("owner", owner);
            json.put("ownerId", ownerId);
        }
        if (secondaryOwnerId != null) {
            json.put("secondaryOwner", secondaryOwner);
            json.put("secondaryOwnerId", secondaryOwnerId);
        }
        // detail info (for ProcessInstanceVO retrieval)
        if (activities != null) {
            JSONArray activitiesJson = new JSONArray();
            for (ActivityInstance activity : activities) {
                activitiesJson.put(activity.getJson());
            }
            json.put("activities", activitiesJson);
        }
        if (transitions != null) {
            JSONArray transitionsJson = new JSONArray();
            for (TransitionInstance transition : transitions) {
                transitionsJson.put(transition.getJson());
            }
            json.put("transitions", transitionsJson);
        }
        if (variables != null) {
            JSONArray variablesJson = new JSONArray();
            for (VariableInstance variable : variables) {
                variablesJson.put(variable.getJson());
            }
            json.put("variables", variablesJson);
        }
        if (subprocessInstances != null) {
            JSONArray subprocsJson = new JSONArray();
            for (ProcessInstance subproc : subprocessInstances) {
                subprocsJson.put(subproc.getJson());
            }
            json.put("subprocesses", subprocsJson);
        }
        return json;
    }

    public JSONObject getJsonNew() throws JSONException {
        JSONObject json = new JSONObject();
        // summary info (for ProcessLists)
        json.put("id", this.id);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (processId != null)
            json.put("processId", processId);
        if (processName != null)
            json.put("processName", processName);
        if (processVersion != null)
            json.put("processVersion", processVersion);
        if (packageName != null)
            json.put("packageName", packageName);
        if (statusCode != null)
            json.put("statusCode", statusCode);
        if (status != null)
            json.put("status", status);
        if (startDate != null)
            json.put("startDate", startDate);
        if (endDate != null) {
            json.put("endDate", endDate);
            json.put("completionCode", completionCode == null ? "null" : completionCode);
        }
        if (comment != null)
            json.put("comments", comment);
        if (owner != null) {
            json.put("owner", owner);
            json.put("ownerId", ownerId);
        }
        if (secondaryOwnerId != null) {
            json.put("secondaryOwner", secondaryOwner);
            json.put("secondaryOwnerId", secondaryOwnerId);
        }
        // detail info (for ProcessInstanceVO retrieval)
        if (activities != null) {
            JSONArray activitiesJson = new JSONArray();
            for (ActivityInstance activity : activities) {
                activitiesJson.put(activity.getJson());
            }
            json.put("activities", activitiesJson);
        }
        if (transitions != null) {
            JSONArray transitionsJson = new JSONArray();
            for (TransitionInstance transition : transitions) {
                transitionsJson.put(transition.getJson());
            }
            json.put("transitions", transitionsJson);
        }
        if (variables != null) {
            JSONArray variablesJson = new JSONArray();
            for (VariableInstance variable : variables) {
                variablesJson.put(variable.getJson());
            }
            json.put("variables", variablesJson);
        }
        if (subprocessInstances != null) {
            JSONArray subprocsJson = new JSONArray();
            for (ProcessInstance subproc : subprocessInstances) {
                subprocsJson.put(subproc.getJson());
            }
            json.put("subprocesses", subprocsJson);
        }
        return json;
    }

    public String getJsonName() {
        return "ProcessInstance";
    }

    // embedded subprocesses only for new REST API
    private List<ProcessInstance> subprocessInstances;
    public List<ProcessInstance> getSubprocessInstances() { return subprocessInstances; }
    public void setSubprocessInstances(List<ProcessInstance> subinsts) { this.subprocessInstances = subinsts; }
}
