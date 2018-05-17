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

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
        this(new JsonObject(json));
    }

    public ProcessInstance(JSONObject jsonObj) throws JSONException {
        // summary info
        if (jsonObj.has("id"))
            this.id = jsonObj.getLong("id");
        if (jsonObj.has("solutionId"))
            solutionId = jsonObj.getString("solutionId");
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
        if (jsonObj.has("template"))
            template = jsonObj.getString("template");
        if (jsonObj.has("templatePackage"))
            templatePackage = jsonObj.getString("templatePackage");
        if (jsonObj.has("templateVersion"))
            templateVersion = jsonObj.getString("templateVersion");
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

    private Long completionTime;
    public Long getCompletionTime(){return completionTime; }
    public void setCompletionTime(Long completionTime){ this.completionTime=completionTime;}

    /**
     * Formatted.
     */
    private String processVersion;
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String s) { processVersion = s; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String s) { masterRequestId = s; }

    private String solutionId;
    public String getSolutionId() {
        return solutionId;
    }
    public void setSolutionId(String solutionId) {
        this.solutionId = solutionId;
    }

    private String template;
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    private String templatePackage;
    public String getTemplatePackage() { return templatePackage; }
    public void setTemplatePackage(String templatePackage) { this.templatePackage = templatePackage; }

    private String templateVersion;
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String version) { this.templateVersion = version; }

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
            if (one.getActivityId().equals(actId)) ret.add(one);
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
        JSONObject json = create();
        // summary info (for ProcessLists)
        json.put("id", this.id);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (solutionId != null)
            json.put("solutionId", solutionId);
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
        if (template != null)
            json.put("template", template);
        if (templatePackage != null)
            json.put("templatePackage", templatePackage);
        if (templateVersion != null)
            json.put("templateVersion", templateVersion);
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
