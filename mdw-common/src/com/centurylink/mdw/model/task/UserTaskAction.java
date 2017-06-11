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

import static com.centurylink.mdw.constant.TaskAttributeConstant.TASK_ACTION_JSONNAME;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.common.service.types.TaskAction;
import com.centurylink.mdw.model.user.UserAction;

public class UserTaskAction extends UserAction implements Jsonable {

    // allow other possibilities besides UserAction.Action enum
    private String taskAction;
    public String getTaskAction() { return taskAction; }
    public void setTaskAction(String taskAction) { this.taskAction = taskAction; }

    public UserTaskAction() {

    }

    public UserTaskAction(JSONObject json) throws JSONException {
         this(json, json.getString("action"));
    }

    /**
     * This supports the new /mdw/Services/TaskAction/Action service
     * where the action is part of the url instead of the json content
     * @param json
     * @param action
     * @throws JSONException
     */
    public UserTaskAction(JSONObject json, String action) throws JSONException {
        super.fromJson(json);
        setTaskAction(action);
        if (action.equals(Action.Assign.toString()))
            setAssignee(json.getString("assignee"));
        if (json.has("taskInstanceId")) {
            setTaskInstanceId(json.getLong("taskInstanceId"));
        }
        else if (json.has("taskInstanceIds")) {
            List<Long> instanceIds = new ArrayList<Long>();
            JSONArray idsArray = json.getJSONArray("taskInstanceIds");
            for (int i = 0; i < idsArray.length(); i++) {
                instanceIds.add(idsArray.getLong(i));
            }
            setTaskInstanceIds(instanceIds);
        }
        if (json.has("comment"))
            setComment(json.getString("comment"));
    }

    public UserTaskAction(TaskAction taskAction) {
        setTaskInstanceId(taskAction.getInstanceId());
        setTaskAction(taskAction.getAction());
        setUser(taskAction.getUser());
        setAssignee(taskAction.getAssignee());
        setDestination(taskAction.getDestination());
        setComment(taskAction.getComments());
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (getId() != null)
            return super.getJson();  // should be treated as UserActionVO (auditing, etc)

        String action = getTaskAction();
        json.put("action", action);
        json.put("user", getUser());
        if (getTaskInstanceId() != null) {
            json.put("taskInstanceId", getTaskInstanceId());
        }
        else if (getTaskInstanceIds() != null) {
            JSONArray idsArray = new JSONArray();
            for (Long instanceId : getTaskInstanceIds()) {
                idsArray.put(instanceId);
            }
            json.put("taskInstanceIds", idsArray);
        }

        if (action.equals(Action.Assign.toString()))
            json.put("assignee", getAssignee());
        else if (getDestination() != null)
            json.put("destination", getDestination());

        if (getComment() != null)
            json.put("comment", getComment());

        return json;
    }

    public Long getTaskInstanceId() {
        return getEntityId();
    }

    public void setTaskInstanceId(Long instanceId) {
        setEntityId(instanceId);
    }

    private List<Long> taskInstanceIds;
    public List<Long> getTaskInstanceIds() { return taskInstanceIds; }
    public void setTaskInstanceIds(List<Long> instanceIds) { this.taskInstanceIds = instanceIds; }

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
