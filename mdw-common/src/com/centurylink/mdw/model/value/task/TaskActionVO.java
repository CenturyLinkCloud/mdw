/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.task;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.TASK_ACTION_JSONNAME;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.types.TaskAction;
import com.centurylink.mdw.model.value.user.UserActionVO;

public class TaskActionVO extends UserActionVO implements Jsonable {

    public TaskActionVO() {

    }

    public TaskActionVO(JSONObject json) throws JSONException {
         this(json, json.getString("action"));
    }

    /**
     * This supports the new /mdw/Services/TaskAction/Action service
     * where the action is part of the url instead of the json content
     * @param json
     * @param action
     * @throws JSONException
     */
    public TaskActionVO(JSONObject json, String action) throws JSONException {
        super.fromJson(json);
        setTaskAction(action);
        if (action.equals(Action.Assign.toString()))
            setAssignee(json.getString("assignee"));
        if (json.has("taskInstanceId"))
            setTaskInstanceId(json.getLong("taskInstanceId"));
        if (json.has("comment"))
            setComment(json.getString("comment"));
    }

    public TaskActionVO(TaskAction taskAction) {
        setTaskInstanceId(taskAction.getInstanceId());
        setTaskAction(taskAction.getAction());
        setUser(taskAction.getUser());
        setAssignee(taskAction.getAssignee());
        setDestination(taskAction.getDestination());
        setComment(taskAction.getComments());
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (getId() != null)
            return super.getJson();  // should be treated as UserActionVO (auditing, etc)

        String action = getTaskAction();
        json.put("action", action);
        json.put("user", getUser());
        if (getTaskInstanceId() != null)
            json.put("taskInstanceId", getTaskInstanceId());

        if (action.equals(Action.Assign.toString()))
            json.put("assignee", getAssignee());
        else if (getDestination() != null)
            json.put("destination", getDestination());

        if (getComment() != null)
            json.put("comment", getComment());

        return json;
    }

    public String getTaskAction() {
        return getAction().toString();
    }

    public void setTaskAction(String action) {
        setAction(Action.valueOf(action));
    }

    public Long getTaskInstanceId() {
        return getEntityId();
    }

    public void setTaskInstanceId(Long instanceId) {
        setEntityId(instanceId);
    }

    /**
     * destination is assignee for ASSIGN
     */
    public String getAssignee() {
        return getDestination();
    }
    public void setAssignee(String assignee) {
        setDestination(assignee);
    }

    /**
     * destination is workgroup for FORWARD
     */
    public String getWorkgroup() {
        return getDestination();
    }
    public void setWorkgroup(String group) {
        setDestination(group);
    }

    public String getComment() {
        return getDescription();
    }

    public void setComment(String comment) {
        setDescription(comment);
    }

    @Override
    public String getJsonName() {
        return TASK_ACTION_JSONNAME;
    }


}
