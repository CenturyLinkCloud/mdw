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

import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.util.DateHelper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

@ApiModel(value="ProcessInstance", description="MDW workflow process instance")
public class ProcessInstance implements Jsonable, Linkable {

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
            activities = new ArrayList<>();
            for (int i = 0; i < activitiesJson.length(); i++) {
                activities.add(new ActivityInstance(activitiesJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("transitions")) {
            JSONArray transitionsJson = jsonObj.getJSONArray("transitions");
            transitions = new ArrayList<>();
            for (int i = 0; i < transitionsJson.length(); i++) {
                transitions.add(new TransitionInstance(transitionsJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("variables")) {
            JSONArray variablesJson = jsonObj.getJSONArray("variables");
            variables = new ArrayList<>();
            for (int i = 0; i < variablesJson.length(); i++) {
                variables.add(new VariableInstance(variablesJson.getJSONObject(i)));
            }
        }
        if (jsonObj.has("subprocesses")) {
            JSONArray subprocsJson = jsonObj.getJSONArray("subprocesses");
            subprocesses = new ArrayList<>();
            for (int i = 0; i < subprocsJson.length(); i++)
                subprocesses.add(new ProcessInstance(subprocsJson.getJSONObject(i)));
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
        startDate = DateHelper.dateToString(d);
    }

    private String completionCode;
    public String getCompletionCode() { return completionCode; }
    public void setCompletionCode(String s) { this.completionCode = s; }

    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String d) { endDate = d; }
    @ApiModelProperty(hidden=true)
    public void setEndDate(Date d) {
        endDate = DateHelper.dateToString(d);
    }

    private String comment;
    public String getComment() {
        if (comment != null && comment.indexOf("|HasInstanceDef|") >= 0) {
            int index = comment.indexOf("|HasInstanceDef|");
            String origComment = comment;
            if (index == 0)
                comment = null;
            else
                comment = origComment.split("\\|")[0];
            processInstanceDefId = Long.parseLong(origComment.split("\\|")[2]);
        }
        return comment;
    }
    public void setComment(String s) { comment = s; }

    private long processInstanceDefId = 0L;
    public long getProcessInstDefId() {
        if (processInstanceDefId == 0L)
            getComment();  // This parses the comment for instance definition
        return processInstanceDefId;
    }

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
    public List<ActivityInstance> getActivities(String logicalId) {
        List<ActivityInstance> instances = new ArrayList<>();
        if (activities != null) {
            for (ActivityInstance instance : activities) {
                if (("A" + instance.getActivityId()).equals(logicalId))
                    instances.add(instance);
            }
        }
        // latest first
        instances.sort(Comparator.comparingLong(ActivityInstance::getId).reversed());
        return instances;
    }

    private List<TransitionInstance> transitions;
    public List<TransitionInstance> getTransitions() { return transitions; }
    public void setTransitions(List<TransitionInstance> t) { this.transitions = t; }

    private List<ProcessInstance> subprocesses;
    public List<ProcessInstance> getSubprocesses() { return subprocesses; }
    public void setSubprocesses(List<ProcessInstance> subinsts) { this.subprocesses = subinsts; }

    private List<VariableInstance> variables = null;
    public List<VariableInstance> getVariables() { return variables; }
    public void setVariables(List<VariableInstance> variables) { this.variables = variables; }

    @ApiModelProperty(hidden=true)
    public VariableInstance getVariable(String name) {
        for (VariableInstance v : variables) {
            if (v.getName().equals(name)) return v;
        }
        return null;
    }

    // for tester expressions
    @ApiModelProperty(hidden=true)
    private Map<String,Object> varMap;
    public Map<String,Object> getVariable() {
        if (varMap == null) {
            varMap = new HashMap<>();
            for (VariableInstance var : getVariables())
                varMap.put(var.getName(), var.getData());
        }
        return varMap;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProcessInstance && ((ProcessInstance)other).id.equals(id);
    }

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
        if (getProcessInstDefId() > 0L)
            json.put("instanceDefinitionId", getProcessInstDefId());
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
        if (subprocesses != null) {
            JSONArray subprocsJson = new JSONArray();
            for (ProcessInstance subproc : subprocesses) {
                subprocsJson.put(subproc.getJson());
            }
            json.put("subprocesses", subprocsJson);
        }
        return json;
    }

    public JSONObject getSummaryJson() throws JSONException {
        JSONObject json = create();
        json.put("id", this.id);
        if (status != null)
            json.put("status", status);
        else if (statusCode > 0)
            json.put("status", WorkStatuses.getWorkStatuses().get(statusCode));
        if (completionCode != null)
            json.put("result", completionCode);
        if (startDate != null)
            json.put("start", new Date(DateHelper.stringToDate(startDate).getTime() + DatabaseAccess.getDbTimeDiff()).toInstant());
        if (endDate != null)
            json.put("end", new Date(DateHelper.stringToDate(endDate).getTime() + DatabaseAccess.getDbTimeDiff()).toInstant());
        return json;
    }

    @Override
    public JSONObject getSummaryJson(int detail) {
        JSONObject json = getSummaryJson();
        if (detail > 0) {
            // these are needed for process instance hierarchy
            if (processName != null)
                json.put("processName", processName);
            if (processVersion != null)
                json.put("processVersion", processVersion);
            if (packageName != null)
                json.put("packageName", packageName);
        }
        return json;
    }


    public String getJsonName() {
        return "ProcessInstance";
    }

    @Override
    public String getQualifiedLabel() {
        String label = getPackageName() == null ? getProcessName() : getPackageName() + "/" + getProcessName();
        label += ": " + getId();
        return label;
    }

    public Linked<ActivityInstance> getLinkedActivities(Process process) {
        List<ActivityInstance> starts = getActivities(process.getStartActivity().getLogicalId());
        if (starts.isEmpty())
            return null;
        else
            return getLinkedActivities(process, starts.get(0));
    }

    public Linked<ActivityInstance> getLinkedActivities(Process process, ActivityInstance start) {
        Linked<ActivityInstance> parent = new Linked<>(start);
        linkActivities(process, parent);
        return parent;
    }

    private void linkActivities(Process process, Linked<ActivityInstance> parent) {
        ActivityInstance parentInst = parent.get();
        parentInst.setProcessInstanceId(getId());
        parentInst.setProcessName(getProcessName());
        parentInst.setProcessId(getProcessId());
        parentInst.setProcessVersion(getProcessVersion());
        Activity activity = process.getActivity(parentInst.getActivityId());
        parentInst.setName(activity.getName());
        parentInst.setMilestoneName(activity.milestoneName());
        parentInst.setMilestoneGroup(activity.milestoneGroup());
        for (TransitionInstance ti : transitions) {
            for (Transition t : process.getAllTransitions(parentInst.getActivityId())) {
                if (t.getId().equals(ti.getTransitionID())) {
                    List<ActivityInstance> instances = getActivities("A" + t.getToId());
                    ActivityInstance instance = instances.get(0); // latest instance
                    if (parent.getTop().find(instance) == null) { // don't add same instance twice
                        Linked<ActivityInstance> child = new Linked<>(instance);
                        child.setParent(parent);
                        parent.getChildren().add(child);
                        if (!child.checkCircular()) {
                            linkActivities(process, child);
                        }
                    }
                }
            }
        }
    }

}
