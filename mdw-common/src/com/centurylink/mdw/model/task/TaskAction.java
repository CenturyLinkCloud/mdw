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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.user.Role;

/**
 * Task Action model object.  Task actions are no longer constrained to a constant
 * set of values declared here.  Non-standard actions are governed by the result codes
 * of the outgoing transitions from the task creation activity.
 * When treated as a Jsonable, this is the DEFINITION for a task action (from mdw-hub-actions.xml);
 *
 */
public class TaskAction implements Serializable, Jsonable, Comparable<TaskAction> {

    // standard task actions
    public static final String CREATE = "Create";
    public static final String ASSIGN = "Assign";
    public static final String CLAIM = "Claim";
    public static final String RELEASE = "Release";
    public static final String CANCEL = "Cancel";
    public static final String COMPLETE = "Complete";
    public static final String RETRY = "Retry";
    public static final String FORWARD = "Forward";
    public static final String ABORT = "Abort";
    public static final String WORK = "Work";
    public static final String SAVE = "Save";
    public static final String CALLBACK = "Callback";    // for making a request/response call to the engine

    public static final String[] STANDARD_ACTIONS = { CREATE, ASSIGN, CLAIM, RELEASE, CANCEL, COMPLETE, RETRY, FORWARD, ABORT, WORK, SAVE };

    private boolean dynamic;
    private String taskActionName;
    private Long taskActionId;
    private Role[] userRoles;
    private String alias;
    private boolean requireComment;
    private String outcome;
    private boolean autoSave;
    private List<ForTask> forTasks;

    public String getAlias() { return alias; }
    public void setAlias(String s) { alias = s; }

    public boolean isRequireComment() { return requireComment; }
    public void setRequireComment(boolean b) { this.requireComment = b; }

    public boolean isAutoSave() { return autoSave; }
    public void setAutoSave(boolean b) { this.autoSave = b; }

    /**
     * Override standard action outcome
     */
    public String getOutcome() { return outcome; }
    public void setOutcome(String s) { this.outcome = s; }

    public boolean isDynamic() { return dynamic; }
    public void setDynamic(boolean b) { dynamic = b; }

    public List<ForTask> getForTasks() { return forTasks; }
    public void setForTasks(List<ForTask> forTasks) { this.forTasks = forTasks; }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof TaskAction))
            return false;
        TaskAction taskAction = (TaskAction) o;
        if (taskAction.getTaskActionName() == null)
            return getTaskActionName() == null;
        return (taskAction.getTaskActionName().equals(getTaskActionName()));
    }

    public int compareTo(TaskAction other)
    {
      return this.getLabel().compareTo(other.getLabel());
    }

    /**
     * Returns the TaskActionName of  the user
     * @return String
     *
     */
    public String getTaskActionName() {
        return taskActionName;
    }

    public String getLabel() {
        return alias == null ? taskActionName : alias;
    }

    /**
     * Sets the TaskActionName of the USer
     * @param pTaskActionName
     */
    public void setTaskActionName(String pTaskActionName){
        this.taskActionName = pTaskActionName;
    }
    public Long getTaskActionId() {
        return taskActionId;
    }
    public void setTaskActionId(Long taskActionId) {
        this.taskActionId = taskActionId;
    }
    public Role[] getUserRoles() {
        return userRoles;
    }
    public void setUserRoles(Role[] userRoles) {
        this.userRoles = userRoles;
    }

    public static String fixCase(String action) {
        if (action == null)
            return null;
        if (action.equalsIgnoreCase(TaskAction.CANCEL) || action.equalsIgnoreCase(TaskAction.RETRY) || action.equalsIgnoreCase(TaskAction.FORWARD))
            return action.substring(0, 1).toUpperCase() + action.substring(1).toLowerCase();
        else
            return action;
    }

    public static boolean isStandardAction(String action) {
        for (String stdAction : STANDARD_ACTIONS) {
            if (stdAction.equals(action))
                return true;
        }
        return false;
    }

    /**
     * TODO: Drive this from rules in MDWTaskActions.xml (requires major refactoring -- use JAXB).
     */
    public static boolean isCompleting(String action) {
        return COMPLETE.equals(action) || !isStandardAction(action);
    }

    /**
     * Checked if the passed in RoleId is mapped to this TaskAction
     * @param pRoleId
     * @return boolean results
     */
    public boolean isRoleMapped(Long pRoleId){
       if(userRoles == null || userRoles.length == 0){
            return false;
        }
        for(int i=0; i<userRoles.length; i++){
            if(userRoles[i].getId().longValue() == pRoleId.longValue()){
                return true;
            }
        }
        return false;
    }

    /**
         *
         * @return
         * @author
         */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("TaskActionVO[");
        buffer.append("taskActionId = ").append(taskActionId);
        buffer.append(" taskActionName = ").append(taskActionName);
        if (userRoles == null) {
            buffer.append(" userRoles = ").append("null");
        } else {
            buffer.append(" userRoles = ").append(
                    Arrays.asList(userRoles).toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("action", taskActionName);
        if (alias != null)
            json.put("alias", alias);
        if (dynamic)
            json.put("dynamic", true);
        if (outcome != null)
            json.put("outcome", outcome);
        if (requireComment)
            json.put("requireComment", true);
        if (autoSave)
            json.put("autosave", true);
        if (forTasks != null) {
            JSONObject forTasksJson = create();
            for (ForTask forTask : forTasks) {
                forTasksJson.put(forTask.getJsonName(), forTask.getJson());
            }
            json.put("forTasks", forTasksJson);
        }
        return json;
    }

    @Override
    public String getJsonName() {
        return "taskAction";
    }

    /**
     * Limit action to specified tasks
     */
    public class ForTask implements Jsonable {
        private String taskId;
        /**
         * Logical ID or task name (for compatibility)
         */
        public String getTaskId() { return taskId; }

        private List<String> destinations; // workgroups
        public List<String> getDestinations() { return destinations; }

        public ForTask(String taskId, List<String> destinations) {
            this.taskId = taskId;
            this.destinations = destinations;
        }

        public String getJsonName() {
            return taskId;
        }

        public JSONObject getJson() throws JSONException {
            JSONObject json = create();
            if (destinations != null) {
              JSONArray destArray = new JSONArray();
              for (String destination : destinations) {
                  destArray.put(destination);
              }
              json.put("destinations", destArray);
            }
            return json;
        }

    }
}
