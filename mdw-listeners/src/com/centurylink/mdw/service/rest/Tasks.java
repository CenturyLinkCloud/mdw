/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.COMMENTS;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.DUE_DATE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.LOGICAL_ID;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.PRIORITY;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Exportable;
import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.JsonListMap;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.common.utilities.ResourceFormatter.Format;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskStatus;
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
import com.centurylink.mdw.observer.task.RemoteNotifier;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.task.TaskActionValidator;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.services.task.TaskValidationException;
import com.centurylink.mdw.services.task.factory.TaskInstanceNotifierFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Tasks")
@Api("Task instances")
public class Tasks extends JsonRestService implements Exportable {
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
     * Retrieve a task or list of tasks.
     */
    @Override
    @Path("/{taskInstanceId}/{subData}")
    @ApiOperation(value="Retrieve a task instance or a page of task instances",
        notes="If taskInstanceId is not present, returns a page of task instances. " +
          "If subData is not present, returns task summary info. " +
          "Options for subData: 'values', 'indexes', 'history', 'actions', 'subtasks', 'topThroughput'")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        TaskServices taskServices = ServiceLocator.getTaskServices();
        try {
            String segOne = getSegment(path, 1);
            if (segOne == null) {
                // task list query
                String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
                TaskList tasks = taskServices.getWorkgroupTasks(userId, getQuery(path, headers));
                tasks.setName("tasks");
                return tasks.getJson();
            }
            else {
                if (segOne.equals("topThroughput")) {
                    Query query = getQuery(path, headers);
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
                    Query query = getQuery(path, headers);
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
                            return taskServices.getInstance(instanceId).getJson();
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
                            //TODO Fill this in with real valid actions
                            JSONObject json = new JSONObject();
                            List<String> validActions = new ArrayList<String>();
                            validActions.add("Assign");
                            validActions.add("Claim");
                            json.put("validActions", validActions);
                            return json;
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
     * For creating a new task.
     */
    @Override
    @Path("/{taskInstanceId}/{action}")
    @ApiOperation(value="Create a task instance or perform an action on an existing instance",
        notes="If {action} is present, then the body contains a TaskAction; otherwise it contains a Task.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Task", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskInstanceVO"),
        @ApiImplicitParam(name="TaskAction", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskActionVO")})
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String instanceId = getSegment(path, 1);
        try {
            if (instanceId == null) {
                // /Tasks
                // Create a new task
                TaskInstanceVO taskInstanceIn = new TaskInstanceVO(content);
                String taskLogicalId = content.getString(LOGICAL_ID);
                if (content.has("masterTaskInstanceId")) {
                    // subtask instance
                    Long masterTaskInstId = content.getLong("masterTaskInstanceId");
                    TaskServices taskServices = ServiceLocator.getTaskServices();
                    taskServices.createSubTask(taskLogicalId, masterTaskInstId);
                    return null;
                }
                else {
                    // top-level task instance
                    String ownerAppName = content.getString("appName");
                    Long procInstId = content.getLong(TaskAttributeConstant.PROCESS_INST_ID);
                    return createTaskInstance(ownerAppName, taskLogicalId, procInstId, taskInstanceIn, Format.json);
                }
            }
            else {
                // e.g Tasks/12/Claim
                String action = getSegment(path, 2);

                // Perform task action
                // Tasks/id/action
                if (StringUtils.isEmpty(action)) {
                    throw new ServiceException(ServiceException.BAD_REQUEST,
                            "Missing TaskAction on URL request, should be e.g /TaskActions/id/Action , where 'Action' is one of "
                                    + com.centurylink.mdw.model.data.task.TaskAction.STANDARD_ACTIONS);
                }
                // Simple validation
                validateTaskInstanceId(path, instanceId, content.getLong("taskInstanceId"));

                TaskActionVO taskAction = new TaskActionVO(content, action);

                return performActionOnTask(taskAction);
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

    /**
     * For update of a task.
     * <p>
     * /mdw/Services/Tasks/{taskInstanceId}
     * e.g /mdw/Services/Tasks/200065
     * </p>
     * Also for update of a task's indexes.
     * <p>
     * /mdw/Services/Tasks/{taskInstanceId}/indexes
     * e.g /mdw/Services/Tasks/200065/indexes
     * </p>
     *
     */
    @Override
    @Path("/{taskInstanceId}/{subData}")
    @ApiOperation(value="Update a task instance, or update an instance's index values", response=StatusMessage.class,
        notes="If indexes is present, body is TaskIndexes; otherwise body is a Task." +
          "If subData is not present, returns task summary info. Options for subData: 'values', 'indexes'")
    @ApiImplicitParams({
        @ApiImplicitParam(name="Task", paramType="body", dataType="com.centurylink.mdw.model.value.task.TaskInstanceVO"),
        @ApiImplicitParam(name="SubData", paramType="body", dataType="java.util.Map")})
    public JSONObject put(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException("Missing path segment: {taskInstanceId}");
        Long instanceId = Long.parseLong(id);
        String extra = getSegment(path, 2);
        try {
            if (extra == null) {
                String taskInstanceId = path;
                // Update a task
                TaskInstanceVO taskInstJson = new TaskInstanceVO(content);
                // Small validation
                validateTaskInstanceId(path, taskInstanceId, content.getLong("id"));

                String user = content.getString("user");
                boolean clearDueDate = content.has("clearDueDate") ? content.getBoolean("clearDueDate") : false;
                return updateTask(user, taskInstJson, clearDueDate);
            }
            else if (extra.equals("values")) {
                Map<String,String> values = JsonUtil.getMap(content);
                TaskServices taskServices = ServiceLocator.getTaskServices();
                taskServices.applyValues(instanceId, values);
            }
            else if (extra.equals("indexes")) {
                String taskInstanceId = getSegment(path, 2);
                // Update task indexes
                TaskIndexes taskIndexes = new TaskIndexes(content);
                validateTaskInstanceId(path, taskInstanceId, content.getLong("taskInstanceId"));
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
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return null;
    }

    // Utility functions are below

    /**
     * @param path
     * @param taskInstanceId
     * @param contentTaskInstanceId
     * @throws ServiceException
     */
    private void validateTaskInstanceId(String path, String taskInstanceId,
            long contentTaskInstanceId) throws ServiceException {
        if (!(Long.valueOf(taskInstanceId).longValue() == contentTaskInstanceId)) {
            throw new ServiceException(
                    "Url " + path + " contains a different taskInstanceId from the content "
                            + contentTaskInstanceId);
        }

    }

    private JSONObject createTaskInstance(String ownerAppName, String logicalId, Long procInstId,
            TaskInstanceVO taskInstanceIn, Format format) throws ServiceException {
        TaskInstanceVO taskInstance = null;
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskVO task = TaskTemplateCache.getTaskTemplate(ownerAppName, logicalId);
            if (task == null)
                throw new ServiceException("Task template '"
                        + (ownerAppName == null ? logicalId : ownerAppName + ":" + logicalId)
                        + "' is not defined");

            int dueInSeconds = taskInstanceIn.getDueDate() != null
                    ? (int) ((taskInstanceIn.getDueDate().getTime()
                            - DatabaseAccess.getCurrentTime()) / 1000)
                    : 0;
            Long proccessInstanceId = procInstId == null ? new Long(0L) : procInstId;
            // TODO : collect indices
            if (ownerAppName == null) {
                taskInstance = taskMgr.createTaskInstance(task.getTaskId(), proccessInstanceId,
                        taskInstanceIn.getSecondaryOwnerType(),
                        taskInstanceIn.getSecondaryOwnerId(), taskInstanceIn.getComments(),
                        ownerAppName, null, task.getTaskName(), dueInSeconds, null,
                        taskInstanceIn.getTaskClaimUserCuid(), taskInstanceIn.getOrderId());
                TaskRuntimeContext taskRuntime = null;
                List<TaskNotifier> taskNotifiers = TaskInstanceNotifierFactory.getInstance()
                        .getNotifiers(task.getTaskId(),
                                OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType())
                                        ? proccessInstanceId : null,
                                TaskStatus.STATUSNAME_OPEN);
                for (TaskNotifier notifier : taskNotifiers) {
                    if (notifier instanceof RemoteNotifier) {
                        if (taskRuntime == null)
                            taskRuntime = taskMgr.getTaskRuntimeContext(taskInstance);
                        notifier.sendNotice(taskRuntime, TaskAction.CREATE,
                                TaskStatus.STATUSNAME_OPEN);
                    }
                }
            }
            else {
                int detailDueInSeconds = dueInSeconds;
                int summaryDueInSecs = task.getSlaSeconds();
                // json request sets associated taskInstanceId as taskInstanceId
                Long associatedTaskInstId = taskInstanceIn.getTaskInstanceId() == null
                        ? taskInstanceIn.getAssociatedTaskInstanceId()
                        : taskInstanceIn.getTaskInstanceId();
                taskInstance = taskMgr.createTaskInstance(task.getTaskId(), proccessInstanceId,
                        taskInstanceIn.getSecondaryOwnerType(),
                        taskInstanceIn.getSecondaryOwnerId(), taskInstanceIn.getComments(),
                        ownerAppName, associatedTaskInstId, task.getTaskName(),
                        summaryDueInSecs > 0 ? summaryDueInSecs : detailDueInSeconds, null,
                        taskInstanceIn.getTaskClaimUserCuid(), taskInstanceIn.getOrderId());

                if (summaryDueInSecs != 0 && summaryDueInSecs != detailDueInSeconds) {
                    Date summaryDueDate = taskInstance.getDueDate();
                    if (summaryDueDate != null) {
                        Date detailDueDate = new Date(
                                DatabaseAccess.getCurrentTime() + detailDueInSeconds * 1000);
                        StringBuffer msg = new StringBuffer();
                        msg.append("Warning: Task Instance ")
                                .append(taskInstance.getTaskInstanceId())
                                .append(" has due date from detail (").append(detailDueDate)
                                .append(") that is being overridden by template SLA in summary (")
                                .append(summaryDueDate).append(")");
                        logger.warnException(msg.toString(), new Exception(msg.toString()));
                    }
                }

                Integer detailPriority = taskInstanceIn.getPriority() == null ? null
                        : taskInstanceIn.getPriority();
                if (detailPriority != null) {
                    int summaryPriority = taskInstance.getPriority();
                    if (detailPriority != summaryPriority) {
                        taskMgr.updateTaskInstancePriority(taskInstance.getTaskInstanceId(),
                                detailPriority);
                        taskInstance.setPriority(detailPriority);
                    }
                }

                List<String> detailWorkGroups = taskInstanceIn.getGroups();
                if (detailWorkGroups != null
                        && !detailWorkGroups.equals(taskInstance.getWorkgroups())) {
                    List<String> summaryWorkGroups = taskInstance.getWorkgroups();
                    if (summaryWorkGroups == null || summaryWorkGroups.isEmpty()) {
                        taskMgr.updateTaskInstanceWorkgroups(taskInstance.getTaskInstanceId(),
                                detailWorkGroups);
                        taskInstance.setWorkgroups(detailWorkGroups);
                    }
                    else {
                        // we'll not override groups configured in summary
                        // template -- just warn
                        StringBuffer msg = new StringBuffer();
                        msg.append("Warning: Task Instance ")
                                .append(taskInstance.getTaskInstanceId())
                                .append(" has workgroup routing from detail (")
                                .append(detailWorkGroups).append(") ")
                                .append("that is being overridden by template group assignments in summary (")
                                .append(summaryWorkGroups).append(")");
                        logger.warnException(msg.toString(), new Exception(msg.toString()));
                    }
                }
            }
            if (logger.isInfoEnabled())
                logger.info(TaskActivity.TASK_CREATE_RESPONSE_ID_PREFIX
                        + taskInstance.getTaskInstanceId());
            return null;
        }
        catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private JSONObject updateTask(String user, TaskInstanceVO taskInstIn, boolean clearDueDate)
            throws ServiceException {
        try {
            Map<String, Object> changes = new HashMap<String, Object>();
            Long autoAssigneeUserId = null;

            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstanceVO taskInst = taskMgr.getTaskInstance(taskInstIn.getTaskInstanceId());

            String autoAssigneeCuid = taskInstIn.getTaskClaimUserCuid();
            if (autoAssigneeCuid != null) {
                UserVO assigneeUser = ServiceLocator.getUserManager().getUser(autoAssigneeCuid);
                if (assigneeUser == null) {
                    throw new ServiceException("Assignee user not found: " + autoAssigneeCuid);
                }
                autoAssigneeUserId = assigneeUser.getId();
            }
            if (taskInstIn.getDueDate() != null || clearDueDate)
                changes.put(DUE_DATE, taskInstIn.getDueDate());
            if (taskInstIn.getPriority() != null)
                changes.put(PRIORITY, taskInstIn.getPriority().intValue());
            if (taskInstIn.getComments() != null)
                changes.put(COMMENTS, taskInstIn.getComments());

            if (changes.isEmpty()) {
                throw new ServiceException("No changes found to update in UpdateTask request");
            }

            if (!taskInst.isSummaryOnly()) {
                TaskManagerAccess.getInstance().processTaskInstanceUpdate(changes, taskInst, user);
            }
            else {
                taskMgr.updateTaskInstanceData(changes, taskInstIn.getGroups(), autoAssigneeUserId,
                        taskInst, user);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return null;
    }

    private JSONObject performActionOnTask(TaskActionVO taskAction)
            throws DataAccessException, ServiceException, TaskException {
        String action = taskAction.getTaskAction();
        Long taskInstanceId = taskAction.getTaskInstanceId();
        String user = taskAction.getUser();
        UserVO userVO = ServiceLocator.getUserManager().getUser(user);
        if (userVO == null)
            throw new ServiceException("User not found: " + user);
        Long userId = userVO.getId();
        Long assigneeId = null;
        if (com.centurylink.mdw.model.data.task.TaskAction.ASSIGN.equals(action)
                || com.centurylink.mdw.model.data.task.TaskAction.CLAIM.equals(action)) {
            String assignee = taskAction.getAssignee();
            if (assignee == null) {
                assignee = user;
            }
            UserVO assigneeUser = ServiceLocator.getUserManager().getUser(assignee);
            if (assigneeUser == null)
                throw new ServiceException("Assignee user not found: " + assignee);
            assigneeId = assigneeUser.getId();
        }
        String destination = taskAction.getDestination();
        String comment = taskAction.getComment();

        TaskManager taskMgr = ServiceLocator.getTaskManager();
        TaskInstanceVO taskInst = taskMgr.getTaskInstance(taskInstanceId);
        if (taskInst == null)
            throw new TaskValidationException(ServiceException.NOT_FOUND, "Task instance not found: " + taskInstanceId);

        TaskActionValidator validator = new TaskActionValidator(taskInst);
        validator.validateAction(userVO, action);

        taskMgr.performActionOnTaskInstance(action, taskInstanceId, userId, assigneeId, comment,
                destination, OwnerType.PROCESS_INSTANCE.equals(taskInst.getOwnerType())
                        && !TaskManagerAccess.getInstance().isRemoteDetail(), false);

        if (logger.isDebugEnabled())
            logger.debug("Action handler service " + getClass().getSimpleName() + ": " + action
                    + " on task instance: " + taskInstanceId);

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