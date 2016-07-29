/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.task;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.TASK_INSTANCE_JSONNAME;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.service.Instance;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.types.Task;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStates;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.data.task.TaskStatuses;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Value object corresponding to a TaskInstance persistable.  Includes
 * population of some values from related tables for the convenience of
 * the TM GUI and other clients.
 */
@ApiModel(value="Task", description="MDW task instance")
public class TaskInstanceVO implements Serializable, Jsonable, Instance {

    public static final String DETAILONLY = "@";
    private static Long db_time_diff = 0l;

	private String orderId;
	private Date dueDate;
    private Long taskInstanceId;
    private Long taskId;
    private String taskName;
    private Integer statusCode;
    private Integer stateCode;
    private String startDate;
    private String endDate;
    private String comments;
    private String description;
    private Long taskClaimUserId;
    private String taskClaimUserCuid;
    private String taskMessage;
    private String activityName;
    private String ownerApplicationName;
    private Long associatedTaskInstanceId;
    //R2 Changes
    private String categoryCode;
    //R4 changes
    private String ownerType;
    private Long ownerId;
    private String secondaryOwnerType;
    private Long secondaryOwnerId;
    private List<String> groups; // for MDW 5.1 template based task instances
    private Integer priority;
    private Boolean inFinalStatus;

    public TaskInstanceVO(Long pTaskInstId, Long pTaskId, String pTaskName, String pOrderId, Date pStartDate,
            Date pEndDate, Date pDueDate, Integer pStatusCd, Integer pStateCd, String pComments, String pClaimUserCuid,
            String pTaskMessage, String pActivityName, String pCategoryCd, String pOwnerAppName, Long pAssTaskInstId) {
        this.dueDate = pDueDate;
        this.endDate = StringHelper.dateToString(pEndDate);
        this.startDate = StringHelper.dateToString(pStartDate);
        this.stateCode = pStateCd;
        this.statusCode = pStatusCd;
        this.taskId = pTaskId;
        this.taskInstanceId = pTaskInstId;
        this.orderId = pOrderId;
        this.comments = pComments;
        this.taskName = pTaskName;
        this.taskClaimUserCuid = pClaimUserCuid;
        this.taskMessage = pTaskMessage;
        this.activityName = pActivityName;
        this.categoryCode = pCategoryCd;
        this.ownerApplicationName = pOwnerAppName;
        this.associatedTaskInstanceId = pAssTaskInstId;
    }

    public TaskInstanceVO(Task jaxbTask) {
        this.taskInstanceId = jaxbTask.getInstanceId();
        this.orderId = jaxbTask.getMasterRequestId();
        this.priority = jaxbTask.getPriority();
        if (jaxbTask.getDueInSeconds() != null) {
            this.dueDate = new Date(System.currentTimeMillis() + db_time_diff + jaxbTask.getDueInSeconds() * 1000);
        }
        this.taskClaimUserCuid = jaxbTask.getAssignee();
        if (jaxbTask.getWorkgroups() != null)
            this.groups = Arrays.asList(jaxbTask.getWorkgroups().split(","));
        this.comments = jaxbTask.getComments();
        this.ownerApplicationName = jaxbTask.getOwnerApplicationName();
        this.associatedTaskInstanceId = jaxbTask.getAssociatedTaskInstanceId();
    }

    public TaskInstanceVO() {
    }

    public TaskInstanceVO(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public TaskInstanceVO(JSONObject jsonObj) throws JSONException {
        if (jsonObj.has("id"))
            taskInstanceId = jsonObj.getLong("id");
        if (jsonObj.has("taskId"))
            taskId = jsonObj.getLong("taskId");
        if (jsonObj.has("name"))
            taskName = jsonObj.getString("name");
        if (jsonObj.has("startDate"))
            startDate = jsonObj.getString("startDate");
        if (jsonObj.has("status"))
            setStatus(jsonObj.getString("status"));
        if (jsonObj.has("advisory"))
            setAdvisory(jsonObj.getString("advisory"));
        if (jsonObj.has("masterRequestId"))
            orderId = jsonObj.getString("masterRequestId");
        if (jsonObj.has("dueDate"))
            dueDate = StringHelper.stringToDate(jsonObj.getString("dueDate"));
        if (jsonObj.has("assignee"))
            taskClaimUserCuid = jsonObj.getString("assignee");
        if (jsonObj.has("instanceUrl"))
            taskInstanceUrl = jsonObj.getString("instanceUrl");
        if (jsonObj.has("category"))
            category = jsonObj.getString("category");
        if (jsonObj.has("priority"))
            priority = jsonObj.getInt("priority");
        if (jsonObj.has("endDate"))
            endDate = jsonObj.getString("endDate");
        if (jsonObj.has("description"))
            description = jsonObj.getString("description");
        if (jsonObj.has("comments"))
            comments = jsonObj.getString("comments");
        if (jsonObj.has("message"))
            taskMessage = jsonObj.getString("message");
        if (jsonObj.has("ownerApp"))
            ownerApplicationName = jsonObj.getString("ownerApp");
        if (jsonObj.has("dueInSeconds"))
            dueDate = new Date(System.currentTimeMillis() + db_time_diff + jsonObj.getInt("dueInSeconds") * 1000);
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
            taskClaimUserCuid = jsonObj.getString("assigneeId");
        if (jsonObj.has("assignee"))
            assignee = jsonObj.getString("assignee");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", associatedTaskInstanceId == null || associatedTaskInstanceId == 0L ? taskInstanceId
                : associatedTaskInstanceId);
        if (taskId != null)
            json.put("taskId", taskId);
        json.put("name", taskName);
        json.put("startDate", startDate);
        json.put("status", getStatus());
        String advisory = getAdvisory();
        if (advisory != null)
            json.put("advisory", getAdvisory());
        json.put("masterRequestId", orderId);
        if (dueDate != null) {
            json.put("dueDate", dueDate);
            json.put("dueInSeconds", (dueDate.getTime() - (System.currentTimeMillis() + db_time_diff))/1000);
        }
        json.put("instanceUrl", taskInstanceUrl);
        json.put("category", getCategory());
        json.put("priority", priority);
        json.put("endDate", endDate);
        json.put("description", description);
        json.put("comments", comments);
        json.put("message", taskMessage);
        json.put("ownerApp", ownerApplicationName);
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
        if (taskClaimUserCuid != null)
            json.put("assigneeId", taskClaimUserCuid);
        if (assignee != null)
            json.put("assignee", assignee);
        return json;
    }

    public String getJsonName() {
        return TASK_INSTANCE_JSONNAME;
    }

    public boolean equals(Object pTaskInstanceVO) {
        if (this == pTaskInstanceVO)
            return true;
        TaskInstanceVO myTIVO = (TaskInstanceVO) pTaskInstanceVO;
        return this.taskInstanceId.longValue() == myTIVO.taskInstanceId.longValue();
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

	public Date getDueDate() {
		return dueDate;
	}
	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}
    @ApiModelProperty(hidden=true)
    public void setDueDate(String dueDateString) {
        this.dueDate = StringHelper.stringToDate(dueDateString);
    }
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getMasterRequestId() {
        return orderId;
    }
    public void setMasterRequestId(String id) {
        this.orderId = id;
    }
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
    @ApiModelProperty(hidden=true)
    public void setEndDate(Date d) {
        this.endDate = StringHelper.dateToString(d);
    }
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
    @ApiModelProperty(hidden=true)
	public void setStartDate(Date d) {
	    this.startDate = StringHelper.dateToString(d);
	}
    @ApiModelProperty(hidden=true)
	public Date getStartDateAsDate() {
	    if (startDate == null)
	        return null;
	    return StringHelper.stringToDate(startDate);
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
        return taskId;
    }
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getComments() {
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }

    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getComment() {
        return getComments();
    }
    @Deprecated
    public void setComment(String comment) {
        setComments(comment);
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

    @ApiModelProperty(hidden=true)
    public String getTaskClaimUserCuid(){
    	return taskClaimUserCuid;
    }
    public void setTaskClaimUserCuid(String pCuid){
    	this.taskClaimUserCuid = pCuid;
    }
    public String getAssigneeCuid() {
        return taskClaimUserCuid;
    }
    public void setAssigneeCuid(String cuid) {
        taskClaimUserCuid = cuid;
    }

    @ApiModelProperty(hidden=true)
    public Long getTaskClaimUserId(){
    	return taskClaimUserId;
    }
    public void setTaskClaimUserId(Long id){
    	this.taskClaimUserId = id;
    }
    @ApiModelProperty(hidden=true)
    public Long getAssigneeId() {
        return taskClaimUserId;
    }

    private String assignee; // assignee name
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getTaskMessage(){
    	return taskMessage;
    }
    public void setTaskMessage(String pTaskMessage){
    	this.taskMessage = pTaskMessage;
    }

    public String getActivityName(){
    	return activityName;
    }
    public void setActivityName(String pActivityName){
    	this.activityName = pActivityName;
    }

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

    @ApiModelProperty(hidden=true)
    public String getOwnerApplicationName(){
    	return this.ownerApplicationName;
    }
    public void setOwnerApplicationName(String pName){
    	this.ownerApplicationName = pName;
    }

    @ApiModelProperty(hidden=true)
    public Long getAssociatedTaskInstanceId(){
    	return associatedTaskInstanceId;
    }
    public void setAssociatedTaskInstanceId(Long pId){
    	this.associatedTaskInstanceId = pId;
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
    private String userIdentifier;
    @ApiModelProperty(hidden=true)
    public String getUserIdentifier() {
        return userIdentifier;
    }
    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getUserCuid() throws GeneralSecurityException {
        if (userIdentifier == null)
            return null;
        return CryptUtil.decrypt(userIdentifier);
    }
    @Deprecated
    public void setUserCuid(String cuid) throws GeneralSecurityException {
        userIdentifier = CryptUtil.encrypt(cuid);
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

    // only applicable to template based general task
    @ApiModelProperty(hidden=true)
    public boolean isDetailOnly() {
    	return DETAILONLY.equals(this.ownerApplicationName);
    }

    @ApiModelProperty(hidden=true)
    public boolean isSummaryOnly() {
        if (ownerApplicationName == null || ownerApplicationName.equals(DETAILONLY)) {
            return false;
        }
        else {
            String app = ownerApplicationName;
            int idx = ownerApplicationName.indexOf('@');
            if (idx > 0)
                app = ownerApplicationName.substring(0, idx);
            return !app.equals(ApplicationContext.getApplicationName());
        }
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
    public boolean isLocal() {
    	if (ownerApplicationName == null || ownerApplicationName.equals(DETAILONLY)) {
    	    return true;
    	}
    	else {
            String app = ownerApplicationName;
            int idx = ownerApplicationName.indexOf('@');
            if (idx > 0)
                app = ownerApplicationName.substring(0, idx);
            return app.equals(ApplicationContext.getApplicationName());
    	}

    }

    @ApiModelProperty(hidden=true)
    public Boolean isInFinalStatus() {
        if (inFinalStatus == null)
            return TaskStatus.STATUS_COMPLETED.equals(statusCode) || TaskStatus.STATUS_CANCELLED.equals(statusCode);
        else
            return inFinalStatus;
    }
    public void setInFinalStatus(boolean inFinalStatus) {
        this.inFinalStatus = inFinalStatus;
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

    public static void setDbTimeDiff(long timeDiff) {
        db_time_diff = timeDiff;
    }

    /**
     * Transient marker for flagging as invalid (no template) prior to updating in db.
     */
    private boolean invalid;
    @ApiModelProperty(hidden=true)
    public boolean isInvalid() { return invalid; }
    public void setInvalid(boolean invalid) { this.invalid = invalid; }

}
