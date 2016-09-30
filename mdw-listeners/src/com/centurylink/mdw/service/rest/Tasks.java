/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.LOGICAL_ID;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonExportable;
import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.JsonListMap;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskCount;
import com.centurylink.mdw.model.value.task.TaskIndexes;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.task.AllowableTaskActions;

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
        roles.add(UserRoleVO.TASK_EXECUTION);
        return roles;
    }

    /**
     * @see com.centurylink.mdw.service.action.ActionService#getAuthorizedWorkGroups()
     */
    @Override
    protected List<UserGroupVO> getRequiredWorkgroups(JSONObject content)
            throws JSONException, DataAccessException {
        List<UserGroupVO> groups = null;
        if (content.has("workgroups")) {
            UserServices userServices = ServiceLocator.getUserServices();
            JSONArray workGrpsJsonArr = content.getJSONArray("workgroups");
            if (workGrpsJsonArr != null) {
                groups = new ArrayList<UserGroupVO>();
                for (int i = 0; i < workGrpsJsonArr.length(); i++) {
                    groups.add(userServices.getWorkgroup(workGrpsJsonArr.getString(i)));
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
                TaskList tasks = taskServices.getWorkgroupTasks(userCuid, query);
                return tasks.getJson();
            }
            else {
                if (segOne.equals("templates")) {
                    List<TaskVO> taskVOs = taskServices.getTaskTemplates(query);
                    JSONArray jsonTasks = new JSONArray();
                    for (TaskVO taskVO : taskVOs) {
                        JSONObject jsonTask = new JSONObject();
                        jsonTask.put("packageName", taskVO.getPackageName());
                        jsonTask.put("taskId", taskVO.getId());
                        jsonTask.put("name", taskVO.getName());
                        jsonTask.put("version", taskVO.getVersionString());
                        jsonTask.put("logicalId", taskVO.getLogicalId());
                        jsonTasks.put(jsonTask);
                    }
                    return new JsonArray(jsonTasks).getJson();
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
                    UserVO user = UserGroupCache.getUser(userCuid);
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
                            TaskInstanceVO taskInstance = taskServices.getInstance(instanceId);
                            if (taskInstance == null)
                                throw new ServiceException(HTTP_404_NOT_FOUND, "Task instance not found: " + instanceId);
                            return taskInstance.getJson();
                        }
                        else if (extra.equals("values")) {
                            Map<String,Value> values = taskServices.getValues(instanceId);
                            JSONObject valuesJson = new JSONObject();
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
                            TaskManager taskMgr = ServiceLocator.getTaskManager();
                            Collection<EventLog> eventLogs = taskMgr.getEventLogs(instanceId);
                            JSONObject json = new JSONObject();
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
                            TaskRuntimeContext runtimeContext = taskServices.getRuntimeContext(instanceId);
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
        @ApiImplicitParam(name="Task", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskInstanceVO"),
        @ApiImplicitParam(name="TaskAction", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskActionVO")})
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        Query query = getQuery(path, headers);
        String segOne = getSegment(path, 1);
        try {
            TaskServices taskServices = ServiceLocator.getTaskServices();
            if (segOne == null || segOne.equalsIgnoreCase("create")) {
                // Create a new task
                if (!content.has(LOGICAL_ID))
                    throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing content field: " + LOGICAL_ID);
                String taskLogicalId = content.getString(LOGICAL_ID);
                if (content.has("masterTaskInstanceId")) {
                    // subtask instance
                    Long masterTaskInstId = content.getLong("masterTaskInstanceId");
                    taskServices.createSubTask(taskLogicalId, masterTaskInstId);
                    return null;
                }
                else {
                    // top-level task instance
                    Long taskInstanceId = taskServices.createTask(headers.get(Listener.AUTHENTICATED_USER_HEADER), taskLogicalId);
                    JSONObject json = new JSONObject();
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
                        TaskActionVO taskAction = new TaskActionVO(content, segTwo);
                        if (taskAction.getTaskInstanceId() == null || taskInstanceId != taskAction.getTaskInstanceId())
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (instanceId): " + taskAction.getTaskInstanceId() + " is not: " + taskInstanceId);
                        taskServices.performTaskAction(taskAction, query);
                        return null;
                    }
                    catch (IllegalArgumentException ex2) {
                        throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid task action: '" + segTwo + "'", ex2);
                    }
                }
                catch (NumberFormatException ex) {
                    // segOne must be the action
                    try {
                        TaskActionVO taskAction = new TaskActionVO(content, segOne);
                        if (!segOne.equals(taskAction.getAction().toString()))
                            throw new ServiceException(HTTP_400_BAD_REQUEST, "Content/path mismatch (action): '" + taskAction.getAction() + "' is not: '" + segOne + "'");
                        taskServices.performTaskAction(taskAction, query);
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
    @ApiOperation(value="Update a task instance, or update an instance's index values", response=StatusMessage.class,
        notes="If indexes is present, body is TaskIndexes; otherwise body is a Task." +
          "If subData is not present, returns task summary info. Options for subData: 'values', 'indexes'")
    @ApiImplicitParams({
        @ApiImplicitParam(name="Task", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskInstanceVO"),
        @ApiImplicitParam(name="SubData", paramType="body", dataType="java.util.Map")})
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
                TaskInstanceVO taskInstJson = new TaskInstanceVO(content);
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
                TaskManager taskMgr = ServiceLocator.getTaskManager();
                taskMgr.updateTaskIndices(taskIndexes.getTaskInstanceId(), taskIndexes.getIndexes());

                if (logger.isDebugEnabled())
                    logger.debug("Updated task indexes for instance ID: " + taskIndexes.getTaskInstanceId());

                return null;
            }
            else if (extra.equals("regEvent")) {
                String eventName = getSegment(path, 3);
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                workflowServices.registerTaskWaitEvent(instanceId, eventName);
                if (logger.isDebugEnabled())
                    logger.debug("Registered Event : [" + eventName + "]Task Instance Id = " + instanceId);

                return null;
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