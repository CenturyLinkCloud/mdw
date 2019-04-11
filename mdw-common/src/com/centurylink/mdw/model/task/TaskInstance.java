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
package com.centurylink.mdw.model.task;

import static com.centurylink.mdw.constant.TaskAttributeConstant.TASK_INSTANCE_JSONNAME;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.Instance;
import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Value object corresponding to a TaskInstance persistable.  Includes
 * population of some values from related tables for the convenience of
 * the TM GUI and other clients.
 */
@ApiModel(value="Task", description="MDW task instance")
public class TaskInstance implements Serializable, Jsonable, Instance {

    private String masterRequestId;
    private Long taskInstanceId;
    private Long templateId;
    private String taskName;
    private Integer statusCode;
    private Integer stateCode;
    private String comments;
    private String description;
    private Long assigneeId;
    private String assigneeCuid;
    private String categoryCode;
    private String ownerType;
    private Long ownerId;
    private String secondaryOwnerType;
    private Long secondaryOwnerId;
    private List<String> groups;
    private Integer priority;

    private Instant due;
    public Instant getDue() { return due; }
    public void setDue(Instant due) { this.due = due; }

    private Instant start;
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }

    private Instant end;
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }

    private String template;
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    private String title;
    /**
     * Title allows dynamic override of display label.
     */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TaskInstance() {
    }

    public TaskInstance(JSONObject jsonObj) throws JSONException {
        if (jsonObj.has("id"))
            taskInstanceId = jsonObj.getLong("id");
        if (jsonObj.has("taskId"))
            templateId = jsonObj.getLong("taskId");
        if (jsonObj.has("name"))
            taskName = jsonObj.getString("name");
        if (jsonObj.has("start"))
            start = Instant.parse(jsonObj.getString("start"));
        if (jsonObj.has("end"))
            end = Instant.parse(jsonObj.getString("end"));
        if (jsonObj.has("due"))
            due = Instant.parse(jsonObj.getString("due"));
        if (jsonObj.has("status"))
            setStatus(jsonObj.getString("status"));
        if (jsonObj.has("advisory"))
            setAdvisory(jsonObj.getString("advisory"));
        if (jsonObj.has("masterRequestId"))
            masterRequestId = jsonObj.getString("masterRequestId");
        if (jsonObj.has("assignee"))
            assigneeCuid = jsonObj.getString("assignee");
        if (jsonObj.has("instanceUrl"))
            taskInstanceUrl = jsonObj.getString("instanceUrl");
        if (jsonObj.has("category"))
            category = jsonObj.getString("category");
        if (jsonObj.has("priority"))
            priority = jsonObj.getInt("priority");
        if (jsonObj.has("description"))
            description = jsonObj.getString("description");
        if (jsonObj.has("comments"))
            comments = jsonObj.getString("comments");
        if (jsonObj.has("ownerId"))
            ownerId = jsonObj.getLong("ownerId");
        if (jsonObj.has("ownerType"))
            ownerType = jsonObj.getString("ownerType");
        if (jsonObj.has("secondaryOwnerId"))
            secondaryOwnerId = jsonObj.getLong("secondaryOwnerId");
        if (jsonObj.has("secondaryOwnerType"))
            secondaryOwnerType = jsonObj.getString("secondaryOwnerType");
        if (jsonObj.has("workgroups")) {
            JSONArray workGrpsJsonArr = jsonObj.getJSONArray("workgroups");
            if (workGrpsJsonArr != null) {
                groups = new ArrayList<String>();
                for (int i = 0; i < workGrpsJsonArr.length(); i++) {
                    groups.add(workGrpsJsonArr.getString(i));
                }
            }
        }
        if (jsonObj.has("assigneeId"))
            assigneeCuid = jsonObj.getString("assigneeId");
        if (jsonObj.has("assignee"))
            assignee = jsonObj.getString("assignee");
        if (jsonObj.has("activityInstanceId"))
            activityInstanceId = jsonObj.getLong("activityInstanceId");
        if (jsonObj.has("template"))
            template = jsonObj.getString("template");
        if (jsonObj.has("title"))
            title = jsonObj.getString("title");
        if (jsonObj.has("processName"))
            processName = jsonObj.getString("processName");
        if (jsonObj.has("packageName"))
            packageName = jsonObj.getString("packageName");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("id", taskInstanceId);
        if (templateId != null)
            json.put("taskId", templateId);
        json.put("name", taskName);
        json.put("start", start.toString());
        if (due != null)
            json.put("due", due.toString());
        if (end != null)
            json.put("end", end.toString());
        json.put("status", getStatus());
        String advisory = getAdvisory();
        if (advisory != null)
            json.put("advisory", getAdvisory());
        json.put("masterRequestId", masterRequestId);
        if (taskInstanceUrl != null)
            json.put("instanceUrl", taskInstanceUrl);
        json.put("category", getCategory());
        json.put("priority", priority);
        json.put("description", description);
        json.put("comments", comments);
        json.put("ownerId", ownerId);
        json.put("ownerType", ownerType);
        json.put("secondaryOwnerId", secondaryOwnerId);
        json.put("secondaryOwnerType", secondaryOwnerType);
        if (groups != null && !groups.isEmpty()) {
            JSONArray workGroupsJson = new JSONArray();
            for (String group : groups) {
                workGroupsJson.put(group);
            }
            json.put("workgroups", workGroupsJson);
        }
        if (assigneeCuid != null)
            json.put("assigneeId", assigneeCuid);
        if (assignee != null)
            json.put("assignee", assignee);
        if (activityInstanceId != null)
            json.put("activityInstanceId", activityInstanceId);
        if (template != null)
            json.put("template", template);
        if (title != null)
            json.put("title", title);
        if (processName != null)
            json.put("processName", processName);
        if (packageName != null)
            json.put("packageName", packageName);
        return json;
    }

    public String getJsonName() {
        return TASK_INSTANCE_JSONNAME;
    }

    public boolean equals(Object pTaskInstanceVO) {
        if (this == pTaskInstanceVO)
            return true;
        TaskInstance myTIVO = (TaskInstance) pTaskInstanceVO;
        if( myTIVO!=null)
            return this.taskInstanceId.longValue() == myTIVO.taskInstanceId.longValue();
           else
            return false;
    }

    @ApiModelProperty(hidden=true)
    public Long getTaskInstanceId() {
       return taskInstanceId ;
    }
    public void setTaskInstanceId(Long taskInstanceId) {
        this.taskInstanceId = taskInstanceId ;
    }

    public String getId() {
        return String.valueOf(taskInstanceId);
    }
    public void setId(String instanceId) {
        setTaskInstanceId(Long.parseLong(instanceId));
    }

    public String getMasterRequestId() {
        return masterRequestId;
    }
    public void setMasterRequestId(String id) {
        this.masterRequestId = id;
    }

    @ApiModelProperty(hidden=true)
    public Integer getStateCode() {
        return stateCode;
    }
    public void setStateCode(Integer stateCode) {
        this.stateCode = stateCode;
    }
    @ApiModelProperty(hidden=true)
    public String getAdvisory() {
        if (TaskState.STATE_ALERT.equals(stateCode))
            return TaskStates.getTaskStates().get(TaskState.STATE_ALERT);
        else if (TaskState.STATE_JEOPARDY.equals(stateCode))
            return TaskStates.getTaskStates().get(TaskState.STATE_JEOPARDY);
        else
            return null;
    }
    public void setAdvisory(String advisory) {
        if (TaskState.getTaskStateName(TaskState.STATE_ALERT).equals(advisory))
            this.stateCode = TaskState.STATE_ALERT;
        else if (TaskState.getTaskStateName(TaskState.STATE_JEOPARDY).equals(advisory))
            this.stateCode = TaskState.STATE_JEOPARDY;
        else if (TaskState.getTaskStateName(TaskState.STATE_INVALID).equals(advisory))
            this.stateCode = TaskState.STATE_INVALID;
    }

    @ApiModelProperty(hidden=true)
    public Integer getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    public String getStatus() {
        return TaskStatuses.getTaskStatuses().get(statusCode);
    }
    public void setStatus(String status) {
        for (int i = 0; i < TaskStatus.allStatusNames.length; i++) {
            if (TaskStatus.allStatusNames[i].equals(status)) {
                statusCode = TaskStatus.allStatusCodes[i];
                break;
            }
        }
    }

    @ApiModelProperty(hidden=true)
    public Long getTaskId() {
        return templateId;
    }
    public void setTaskId(Long taskId) {
        this.templateId = taskId;
    }

    public String getComments() {
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return taskName;
    }
    public void setName(String name) { this.taskName = name; }

    @ApiModelProperty(hidden=true)
    public String getTaskName(){
        return taskName;
    }
    public void setTaskName(String pName){
        this.taskName = pName;
    }

    public String getAssigneeCuid() {
        return assigneeCuid;
    }
    public void setAssigneeCuid(String cuid) {
        assigneeCuid = cuid;
    }

    public void setAssigneeId(Long id){
        this.assigneeId = id;
    }
    @ApiModelProperty(hidden=true)
    public Long getAssigneeId() {
        return assigneeId;
    }

    private String assignee; // assignee name
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public Long activityInstanceId;
    public Long getActivityInstanceId() { return this.activityInstanceId; }
    public void setActivityInstanceId(Long id) { this.activityInstanceId = id; }

    @ApiModelProperty(hidden=true)
    public String getCategoryCode(){
        return categoryCode;
    }
    public void setCategoryCode(String pCategoryCode){
        this.categoryCode = pCategoryCode;
    }
    private String category;
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
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

    @ApiModelProperty(hidden=true)
    public boolean isProcessOwned() {
        return OwnerType.PROCESS_INSTANCE.equals(getOwnerType());
    }
    @ApiModelProperty(hidden=true)
    public String getSecondaryOwnerType() {
        return secondaryOwnerType;
    }
    public void setSecondaryOwnerType(String secondaryOwnerType) {
        this.secondaryOwnerType = secondaryOwnerType;
    }

    @ApiModelProperty(hidden=true)
    public Long getSecondaryOwnerId() {
        return secondaryOwnerId;
    }
    public void setSecondaryOwnerId(Long secondaryOwnerId) {
        this.secondaryOwnerId = secondaryOwnerId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public List<String> getWorkgroups() {
        return getGroups();
    }
    @ApiModelProperty(hidden=true)
    public List<String> getGroups() {
        return groups;
    }

    public void setWorkgroups(List<String> workgroups) {
        setGroups(workgroups);
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    @ApiModelProperty(hidden=true)
    public String getWorkgroupsString() {
        List<String> wrkGrps = getWorkgroups();
        if (null == wrkGrps)
            return "";
        StringBuilder workGroupList = new StringBuilder();
        for (String workgroup : wrkGrps) {
            workGroupList.append(workgroup);
            workGroupList.append(",");
        }
        return workGroupList.length() > 0 ? workGroupList.substring(0, workGroupList.length() - 1) : "";
    }

    public void setWorkgroupsFromString(String newGroups) {
        List<String> toSet = null;
        if (newGroups != null) {
            toSet = new ArrayList<String>();
            for (String newGroup : newGroups.split(","))
                toSet.add(newGroup);
        }
        groups = toSet;
    }

    private String taskInstanceUrl;
    @ApiModelProperty(hidden=true)
    public String getTaskInstanceUrl() {
        return taskInstanceUrl;
    }
    public void setTaskInstanceUrl(String url) {
        this.taskInstanceUrl = url;
    }
    public String getInstanceUrl() {
        return taskInstanceUrl;
    }

    // this is only used for templated e-mail notifications with one-click functionality
    // to identify the recipient for tracking who performed the one-click action on a task instance
    private String messageIdentifier;
    @ApiModelProperty(hidden=true)
    public String getUserIdentifier() {
        return messageIdentifier;
    }
    public void setUserIdentifier(String userIdentifier) {
        this.messageIdentifier = userIdentifier;
    }

    // currently only used for templated e-mail notifications
    private Map<String,Object> variables;
    /**
     * Currently only populated for templated e-mail notifications
     * and variable values specified by $-prefixed column attrs in MDWTaskView.xml.
     * @return map containing the task instance variables
     */
    @ApiModelProperty(hidden=true)
    public Map<String,Object> getVariables() {
        return variables;
    }
    public void setVariables(Map<String,Object> vars) {
        this.variables = vars;
    }

    @ApiModelProperty(hidden=true)
    public boolean isGeneralTask() {
        return OwnerType.DOCUMENT.equals(secondaryOwnerType);
    }

    @ApiModelProperty(hidden=true)
    public boolean isShallow() {
        return groups==null && categoryCode==null;
    }

    // only correct when additional information is loaded
    @ApiModelProperty(hidden=true)
    public boolean isTemplateBased() {
        return groups!=null;
    }

    @ApiModelProperty(hidden=true)
    public Boolean isInFinalStatus() {
        return TaskStatus.STATUS_COMPLETED.equals(statusCode) || TaskStatus.STATUS_CANCELLED.equals(statusCode);
    }

    @ApiModelProperty(hidden=true)
    public boolean isSubTask() {
        return OwnerType.TASK_INSTANCE.equals(getSecondaryOwnerType());
    }

    public Long getMasterTaskInstanceId() {
        if (!isSubTask())
            return null;
        else
            return getSecondaryOwnerId();
    }

    /**
     * Transient marker for flagging as invalid (no template) prior to updating in db.
     */
    private boolean invalid;
    @ApiModelProperty(hidden=true)
    public boolean isInvalid() { return invalid; }
    public void setInvalid(boolean invalid) { this.invalid = invalid; }

}
