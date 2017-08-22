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
package com.centurylink.mdw.service.rest;

import static com.centurylink.mdw.constant.TaskAttributeConstant.LOGICAL_ID;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonExportable;
import com.centurylink.mdw.model.JsonListMap;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskIndexes;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.task.AllowableTaskActions;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * TODO: Clean up references to TaskManager and instead go through TaskServices.
 */
@Path("/Tasks")
@Api("Task instances")
public class Tasks extends JsonRestService implements JsonExportable {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.TASK_EXECUTION);
        return roles;
    }

    /**
     * @see com.centurylink.mdw.service.action.ActionService#getAuthorizedWorkGroups()
     */
    @Override
    protected List<Workgroup> getRequiredWorkgroups(JSONObject content)
            throws JSONException, DataAccessException {
        List<Workgroup> groups = null;
        if (content.has("workgroups")) {
            UserServices userServices = ServiceLocator.getUserServices();
            JSONArray workGrpsJsonArr = content.getJSONArray("workgroups");
            if (workGrpsJsonArr != null) {
                groups = new ArrayList<Workgroup>();
                for (int i = 0; i < workGrpsJsonArr.length(); i++) {
                    Workgroup workgroup = userServices.getWorkgroup(workGrpsJsonArr.getString(i));
                    if (workgroup == null)
                        throw new DataAccessException("Workgroup not found: " + workGrpsJsonArr.getString(i));
                    groups.add(workgroup);
                }
            }
        }
        return groups;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Task;
    }

    /**
     * Retrieve a task or list of tasks, or subData for a task instance.
     */
    @Override
    @Path("/{taskInstanceId}/{subData}")
    @ApiOperation(value="Retrieve a task instance or a page of task instances",
        notes="If taskInstanceId is not present, returns a page of task instances. " +
          "If subData is not present, returns task summary info. " +
          "Options for subData: 'values', 'indexes', 'history', 'actions', 'subtasks', 'topThroughput'",
          response=Value.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        TaskServices taskServices = ServiceLocator.getTaskServices();
        try {
            Query query = getQuery(path, headers);
            String userCuid = headers.get(Listener.AUTHENTICATED_USER_HEADER);
            String segOne = getSegment(path, 1);
            if (segOne == null) {
                // task list query
                TaskList tasks = taskServices.getTasks(query, userCuid);
                return tasks.getJson();
            }
            else {
                if (segOne.equals("templates")) {
                    List<TaskTemplate> taskVOs = taskServices.getTaskTemplates(query);
                    JSONArray jsonTasks = new JSONArray();
                    for (TaskTemplate taskVO : taskVOs) {
                        JSONObject jsonTask = new JsonObject();
                        jsonTask.put("packageName", taskVO.getPackageName());
                        jsonTask.put("taskId", taskVO.getId());
                        jsonTask.put("name", taskVO.getName());
                        jsonTask.put("version", taskVO.getVersionString());
                        jsonTask.put("logicalId", taskVO.getLogicalId());
                        jsonTasks.put(jsonTask);
                    }
                    return new JsonArray(jsonTasks).getJson();
                }
                else if (segOne.equals("categories")) {
                    Map<Integer,TaskCategory> categories = DataAccess.getBaselineData().getTaskCategories();
                    List<TaskCategory> list = new ArrayList<>();
                    list.addAll(categories.values());
                    Collections.sort(list);
                    return JsonUtil.getJsonArray(list).getJson();
                }
                else if (segOne.equals("bulkActions")) {
                    boolean myTasks = query.getBooleanFilter("myTasks");
                    List<TaskAction> taskActions;
                    if (myTasks)
                        taskActions = AllowableTaskActions.getMyTasksBulkActions();
                    else
                        taskActions = AllowableTaskActions.getWorkgroupTasksBulkActions();
                    JSONArray jsonTaskActions = new JSONArray();
                    for (TaskAction taskAction : taskActions) {
                        jsonTaskActions.put(taskAction.getJson());
                    }
                    return new JsonArray(jsonTaskActions).getJson();
                }
                else if (segOne.equals("assignees")) {
                    // return potential assignees for all this user's workgroups
                    User user = UserGroupCache.getUser(userCuid);
                    return ServiceLocator.getUserServices().findWorkgroupUsers(user.getGroupNames(), query.getFind()).getJson();
                }
                else if (segOne.equals("topThroughput")) {
                    // dashboard top throughput query
                    String breakdown = getSegment(path, 2);
                    List<TaskCount> list = taskServices.getTopThroughputTasks(breakdown, query);
                    JSONArray taskArr = new JSONArray();
                    int ct = 0;
                    TaskCount other = null;
                    long otherTot = 0;
                    for (TaskCount taskCount : list) {
                        if (ct >= query.getMax()) {
                            if (other == null) {
                                other = new TaskCount(0);
                                other.setName("Other");
                            }
                            otherTot += taskCount.getCount();
                        }
                        else {
                            taskArr.put(taskCount.getJson());
                        }
                        ct++;
                    }
                    if (other != null) {
                        other.setCount(otherTot);
                        taskArr.put(other.getJson());
                    }
                    return new JsonArray(taskArr).getJson();
                }
                else if (segOne.equals("instanceCounts")) {
                    Map<Date,List<TaskCount>> dateMap = taskServices.getTaskInstanceBreakdown(query);
                    boolean isTotals = query.getFilters().get("taskIds") == null
                            && query.getFilters().get("workgroups") == null
                            && query.getFilters().get("users") == null
                            && query.getFilters().get("statuses") == null;

                    Map<String,List<TaskCount>> listMap = new HashMap<String,List<TaskCount>>();
                    for (Date date : dateMap.keySet()) {
                        List<TaskCount> taskCounts = dateMap.get(date);
                        if (isTotals) {
                            for (TaskCount taskCount : taskCounts)
                                taskCount.setName("Total");
                        }
                        listMap.put(Query.getString(date), taskCounts);
                    }
                    return new JsonListMap<TaskCount>(listMap).getJson();
                }
                else {
                    // must be instance id
                    try {
                        Long instanceId = Long.parseLong(segOne);
                        String extra = getSegment(path, 2);
                        if (extra == null) {
                            TaskInstance taskInstance = taskServices.getInstance(instanceId);
                            if (taskInstance == null)
                                throw new ServiceException(HTTP_404_NOT_FOUND, "Task instance not found: " + instanceId);
                            if (taskInstance.isProcessOwned()) {
                                ProcessInstance procInst = ServiceLocator.getWorkflowServices().getProcess(taskInstance.getOwnerId());
                                taskInstance.setProcessName(procInst.getProcessName());
                                taskInstance.setPackageName(procInst.getPackageName());
                            }
                            return taskInstance.getJson();
                        }
                        else if (extra.equals("values")) {
                            Map<String,Value> values = taskServices.getValues(instanceId);
                            JSONObject valuesJson = new JsonObject();
                            for (String name : values.keySet()) {
                                valuesJson.put(name, values.get(name).getJson());
                            }
                            return valuesJson;
                        }
                        else if (extra.equals("indexes")) {
                            Map<String,String> indexes = taskServices.getIndexes(instanceId);
                            return JsonUtil.getJson(indexes);
                        }
                        else if (extra.equals("history")) {
                            List<EventLog> eventLogs = taskServices.getHistory(instanceId);
                            JSONObject json = new JsonObject();
                            if (eventLogs !=null && eventLogs.size() >0) {
                                JSONArray historyJson = new JSONArray();

                                for (EventLog log : eventLogs) {
                                    historyJson.put(log.getJson());
                                }
                                json.put("taskHistory", historyJson);
                            }
                            return json;
                        }
                        else if (extra.equals("actions")) {
                            // actions for an individual task based on its status and custom outcomes
                            TaskRuntimeContext runtimeContext = taskServices.getContext(instanceId);
                            if (runtimeContext == null)
                                throw new ServiceException(HTTP_404_NOT_FOUND, "Unable to load runtime context for task instance: " + instanceId);
                            List<TaskAction> taskActions = AllowableTaskActions.getTaskDetailActions(userCuid, runtimeContext);
                            JSONArray jsonTaskActions = new JSONArray();
                            for (TaskAction taskAction : taskActions) {
                                jsonTaskActions.put(taskAction.getJson());
                            }
                            return new JsonArray(jsonTaskActions).getJson();
                        }
                        else if (extra.equals("subtasks")) {
                            TaskList subtasks = taskServices.getSubtasks(instanceId);
                            return subtasks.getJson();
                        }
                        else {
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid subpath: " + extra);
                        }
                    }
                    catch (NumberFormatException ex) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task instance id: " + segOne);
                    }
                }
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * For creating a new task or performing task action(s).
     * When performing actions: old way = path='Tasks/{instanceId}/{action}', new way = path='Tasks/{action}'.
     * Where {action} is an actual valid task action like 'Claim'.
     */
    @Override
    @Path("/{action}")
    @ApiOperation(value="Create a task instance or perform an action on existing instance(s)",
        notes="If {action} is 'Create', then the body contains a task template logical Id; otherwise it contains a TaskAction to be performed.",
        response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="TaskAction", paramType="body", dataType="com.centurylink.mdw.model.task.UserTaskAction")})
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String segOne = getSegment(path, 1);
        try {
            TaskServices taskServices = ServiceLocator.getTaskServices();
            if (segOne == null || segOne.equalsIgnoreCase("create")) {
                // Create a new task
                if (!content.has(LOGICAL_ID))
                    throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing content field: " + LOGICAL_ID);
                String taskLogicalId = content.getString(LOGICAL_ID);
                // TODO: title, comments, dueDate
                String title = null;
                String comments = null;
                Instant due = null;
                if (content.has("masterTaskInstanceId")) {
                    // subtask instance
                    Long masterTaskInstId = content.getLong("masterTaskInstanceId");
                    taskServices.createSubTask(taskLogicalId, masterTaskInstId);
                    return null;
                }
                else {
                    // top-level task instance
                    Long taskInstanceId = taskServices
                            .createTask(taskLogicalId, headers.get(Listener.AUTHENTICATED_USER_HEADER),
                                     title, comments, due).getTaskInstanceId();
                    JSONObject json = new JsonObject();
                    json.put("taskInstanceId", taskInstanceId);
                    return json;
                }
            }
            else {
                try {
                    // handle taskInstanceId as segOne and actual action as segTwo
                    long taskInstanceId = Long.parseLong(segOne);
                    String segTwo = getSegment(path, 2);
                    if (segTwo == null)
                        throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing {action} on request path, should be eg: /Tasks/12345/Claim");
                    try {
                        if (content.has("taskAction") && !content.getString("taskAction").equals(segTwo))
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (action): '" + content.getString("taskAction") + "' is not: '" + segTwo + "'");
                        UserTaskAction taskAction = new UserTaskAction(content, segTwo);
                        if (taskAction.getTaskInstanceId() == null || taskInstanceId != taskAction.getTaskInstanceId())
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (instanceId): " + taskAction.getTaskInstanceId() + " is not: " + taskInstanceId);
                        taskServices.performTaskAction(taskAction);
                        return null;
                    }
                    catch (IllegalArgumentException ex2) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task action: '" + segTwo + "'", ex2);
                    }
                }
                catch (NumberFormatException ex) {
                    // segOne must be the action
                    try {
                        UserTaskAction taskAction = new UserTaskAction(content, segOne);
                        if (!segOne.equals(taskAction.getTaskAction().toString()))
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (action): '" + taskAction.getTaskAction() + "' is not: '" + segOne + "'");
                        taskServices.performTaskAction(taskAction);
                        return null;
                    }
                    catch (IllegalArgumentException ex2) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task action: '" + segOne + "'", ex2);
                    }
                }
            }
        }
        catch (JSONException e) {
            logger.severeException(e.getMessage(), e);
            throw new ServiceException(e.getMessage(), e);
        }
        catch (DataAccessException e) {
            logger.severeException(e.getMessage(), e);
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    @Path("/{taskInstanceId}/{subData}")
    @ApiOperation(value="Update a task instance, update an instance's index values, or register to wait for an event", response=StatusMessage.class,
        notes="If indexes is present, body is TaskIndexes; if regEvent is present, body is Event; otherwise body is a Task." +
          "If subData is not present, returns task summary info. Options for subData: values, indexes, regEvent")
    @ApiImplicitParams({
        @ApiImplicitParam(name="Task", paramType="body", dataType="com.centurylink.mdw.model.task.TaskInstance", value="When no subData is specified"),
        @ApiImplicitParam(name="Indexes", paramType="body", dataType="com.centurylink.mdw.model.task.TaskIndexes", value="When {subData}=indexes"),
        @ApiImplicitParam(name="Event", paramType="body", dataType="com.centurylink.mdw.model.event.Event", value="When {subData}=regEvent.  Only the id (event name) field is mandatory in Event object.  Optionally, a completionCode can specified - Default is FINISHED"),
        @ApiImplicitParam(name="Values", paramType="body", dataType="java.lang.Object", value="When {subData}=values. JSON object parseable into a key/value Map.")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {taskInstanceId}");
        try {
            Long instanceId = Long.parseLong(id);
            String extra = getSegment(path, 2);
            if (extra == null) {
                // update task summary info
                TaskInstance taskInstJson = new TaskInstance(content);
                if (!content.has("id"))
                    throw new ServiceException(HTTP_400_BAD_REQUEST, "Content is missing required field: id");
                long contentInstanceId = content.getLong("id");
                if (instanceId != contentInstanceId)
                    throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (instanceId): " + contentInstanceId + " is not: " + instanceId);

                ServiceLocator.getTaskServices().updateTask(getAuthUser(headers), taskInstJson);
                return null;
            }
            else if (extra.equals("values")) {
                Map<String,String> values = JsonUtil.getMap(content);
                TaskServices taskServices = ServiceLocator.getTaskServices();
                taskServices.applyValues(instanceId, values);
            }
            else if (extra.equals("indexes")) {
                // update task indexes
                TaskIndexes taskIndexes = new TaskIndexes(content);
                if (instanceId != taskIndexes.getTaskInstanceId())
                    throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (instanceId): " + taskIndexes.getTaskInstanceId() + " is not: " + instanceId);
                ServiceLocator.getTaskServices().updateIndexes(taskIndexes.getTaskInstanceId(), taskIndexes.getIndexes());

                if (logger.isDebugEnabled())
                    logger.debug("Updated task indexes for instance ID: " + taskIndexes.getTaskInstanceId());

                return null;
            }
            else if (extra.equals("regEvent")) {
                Event event = new Event(content);
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                workflowServices.registerTaskWaitEvent(instanceId, event);
                if (logger.isDebugEnabled())
                    logger.debug("Registered Event : [" + event.getId() + "]Task Instance Id = " + instanceId);
            }
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task instance id: " + id);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return null;
    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has(TaskList.TASKS))
                return new TaskList(TaskList.TASKS, json);
            else if ("Tasks/instanceCounts".equals(query.getPath()))
                return new JsonListMap<TaskCount>(json, TaskCount.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }

    }
}