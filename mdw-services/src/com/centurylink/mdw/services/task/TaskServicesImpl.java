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
package com.centurylink.mdw.services.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetHeader;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.observer.task.TaskValuesProvider;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

/**
 * Services related to manual tasks.
 */
public class TaskServicesImpl implements TaskServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskDataAccess getTaskDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new TaskDataAccess(db);
    }

    protected AggregateDataAccessVcs getAggregateDataAccess() throws DataAccessException {
        return new AggregateDataAccessVcs();
    }

    public TaskInstance createTask(Long taskId, String masterRequestId, Long procInstId,
            String secOwner, Long secOwnerId, String title, String comments) throws ServiceException, DataAccessException {
        return TaskWorkflowHelper.createTaskInstance(taskId, masterRequestId, procInstId, secOwner, secOwnerId, title, comments);
    }

    public TaskInstance createTask(String logicalId, String userCuid, String title, String comments, Instant due) throws ServiceException {
        TaskTemplate template = TaskTemplateCache.getTaskTemplate(logicalId);
        if (template == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Task Template '" + logicalId + "' not found");
        User user = UserGroupCache.getUser(userCuid);
        if (user == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "User '" + userCuid + "' not found");
        try {
            return TaskWorkflowHelper.createTaskInstance(template.getId(), null, title, comments, due, user.getId(), 0L);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public TaskList getTasks(Query query) throws ServiceException {
        return getTasks(query, null);
    }

    /**
     * Retrieve tasks.
     */
    public TaskList getTasks(Query query, String cuid) throws ServiceException {
        try {
            String workgroups = query.getFilter("workgroups");
            if ("[My Workgroups]".equals(workgroups)) {
                User user = UserGroupCache.getUser(cuid);
                if (user == null)
                    throw new DataAccessException("Unknown user: " + cuid);
                query.setArrayFilter("workgroups", user.getGroupNames());
            }
            if ("[My Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().put("assignee", cuid);
            else if ("[Everyone's Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().remove("assignee");

            // processInstanceId
            long processInstanceId = query.getLongFilter("processInstanceId");
            if (processInstanceId > 0) {
                List<String> processInstanceIds = new ArrayList<String>();
                processInstanceIds.add(String.valueOf(processInstanceId));
                // implies embedded subprocess instances also
                ProcessInstance processInstance = ServiceLocator.getWorkflowServices().getProcess(processInstanceId, true);
                if (processInstance.getSubprocessInstances() != null) {
                    for (ProcessInstance subproc : processInstance.getSubprocessInstances()) {
                        processInstanceIds.add(String.valueOf(subproc.getId()));
                    }
                }
                query.setArrayFilter("processInstanceIds", processInstanceIds.toArray(new String[]{}));

                // activityInstanceId -- only honored if processInstanceId is also specified
                Long[] activityInstanceIds = query.getLongArrayFilter("activityInstanceIds");
                if (activityInstanceIds != null && activityInstanceIds.length > 0) {
                    // tasks for activity instance -- special logic applied after retrieving
                    TaskList taskList = getTaskDAO().getTaskInstances(query);
                    for (TaskInstance taskInstance : taskList.getTasks()) {
                        TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
                        taskInstance.setTaskInstanceUrl(helper.getTaskInstanceUrl());
                    }
                    return filterForActivityInstance(taskList, processInstance, activityInstanceIds);
                }
            }

            TaskList taskList = getTaskDAO().getTaskInstances(query);
            for (TaskInstance taskInstance : taskList.getTasks()) {
                TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
                taskInstance.setTaskInstanceUrl(helper.getTaskInstanceUrl());
            }
            return taskList;
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Ugly logic for determining task instances for activity instances.
     */
    private TaskList filterForActivityInstance(TaskList taskList, ProcessInstance processInstance, Long[] activityInstanceIds)
    throws DataAccessException {
        TaskList filteredList = new TaskList();
        List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
        for (TaskInstance taskInstance : taskList.getItems()) {
            Process process = ProcessCache.getProcess(processInstance.getProcessId());
            for (Long activityInstanceId : activityInstanceIds) {
                ActivityInstance activityInstance = processInstance.getActivity(activityInstanceId);
                if (activityInstance == null && processInstance.getSubprocessInstances() != null) {
                    for (ProcessInstance subproc : processInstance.getSubprocessInstances()) {
                        activityInstance = subproc.getActivity(activityInstanceId);
                        if (activityInstance != null)
                            break;
                    }
                }
                if (activityInstance != null) {
                    Long activityId = activityInstance.getActivityId();
                    Long workTransInstId = taskInstance.getSecondaryOwnerId();
                    for (TransitionInstance transitionInstance : processInstance.getTransitions()) {
                        if (transitionInstance.getTransitionInstanceID().equals(workTransInstId)) {
                            Long transitionId = transitionInstance.getTransitionID();
                            Transition workTrans = process.getTransition(transitionId);
                            if (workTrans == null && process.getSubprocesses() != null) {
                                for (Process subproc : process.getSubprocesses()) {
                                    workTrans = subproc.getTransition(transitionId);
                                    if (workTrans != null)
                                        break;
                                }
                            }
                            if (workTrans.getToId().equals(activityId))
                                taskInstances.add(taskInstance);
                        }
                    }
                }
            }
        }

        filteredList.setTasks(taskInstances);
        filteredList.setCount(taskInstances.size());
        filteredList.setTotal(taskInstances.size());
        return filteredList;
    };

    public void saveTaskTemplate(TaskTemplate taskVO) {

    }

    public Map<String,String> getIndexes(Long taskInstanceId) throws ServiceException {
        try {
            return getTaskDAO().getIndexes(taskInstanceId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
            throws ServiceException, DataAccessException {
        TaskTemplate subTaskVo = TaskTemplateCache.getTaskTemplate(subtaskLogicalId);
        if (subTaskVo == null)
            throw new ServiceException("Task Template '" + subtaskLogicalId + "' does not exist");

        TaskInstance masterTaskInstance = getInstance(masterTaskInstanceId);
        TaskRuntimeContext masterTaskContext = getContext(masterTaskInstance);
        TaskInstance subTaskInstance = TaskWorkflowHelper.createTaskInstance(subTaskVo.getTaskId(), masterTaskContext.getMasterRequestId(),
                masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId(), null, null);
        logger.info("SubTask instance created - ID: " + subTaskInstance.getTaskInstanceId());
    }

    public TaskInstance getInstance(Long id) throws ServiceException {
        try {
            return TaskWorkflowHelper.getTaskInstance(id);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot retrieve task instance: " + id, ex);
        }
    }

    public TaskRuntimeContext getContext(Long instanceId) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
        return getContext(taskInstance);
    }

    public TaskRuntimeContext getContext(TaskInstance taskInstance) throws ServiceException {
        return new TaskWorkflowHelper(taskInstance).getContext();
    }

    public Map<String,Value> getValues(Long instanceId) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null) {
            throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
        }
        TaskRuntimeContext runtimeContext = getContext(taskInstance);
        if (runtimeContext.getTaskTemplate().isAutoformTask()) {
            return new AutoFormTaskValuesProvider().collect(runtimeContext);
        }
        else {
            // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
            return new HashMap<String,Value>();
        }
    }

    /**
     * Update task values.
     */
    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException {
        try {
            // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
            TaskRuntimeContext runtimeContext = getContext(instanceId);
            TaskValuesProvider valuesProvider;
            if (runtimeContext.getTaskTemplate().isAutoformTask())
                valuesProvider = new AutoFormTaskValuesProvider();
            else
                valuesProvider = new CustomTaskValuesProvider();
            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
            valuesProvider.apply(runtimeContext, values);
            Map<String,Object> newValues = new HashMap<String,Object>();
            for (String name : values.keySet()) {
                if (TaskRuntimeContext.isExpression(name)) {
                    String rootVar;
                    if (name.indexOf('.') > 0)
                      rootVar = name.substring(2, name.indexOf('.'));
                    else
                      rootVar = name.substring(2, name.indexOf('}'));
                    newValues.put(rootVar, runtimeContext.evaluate("#{" + rootVar + "}"));
                }
                else {
                    newValues.put(name, runtimeContext.getVariables().get(name));
                }
            }
            for (String name : newValues.keySet()) {
                Object newValue = newValues.get(name);
                workflowServices.setVariable(runtimeContext, name, newValue);
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
    }

    public TaskInstance performAction(Long taskInstanceId, String action, String userCuid, String assigneeCuid, String comment,
            String destination, boolean notifyEngine) throws ServiceException {
        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstanceId);
            if (helper.getTaskInstance() == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + taskInstanceId);
            User user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + userCuid);
            Long assigneeId = null;
            if (assigneeCuid != null) {
                User assignee = UserGroupCache.getUser(assigneeCuid);
                if (assignee == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Assignee not found: " + assigneeCuid);
                assigneeId = assignee.getId();
            }
            helper.performAction(action, user.getId(), assigneeId, comment, destination, notifyEngine, true);
            return helper.getTaskInstance();
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error doing " + action + " on task " + taskInstanceId, ex);
        }
    }

    public void performTaskAction(UserTaskAction taskAction) throws ServiceException {
        String action = taskAction.getTaskAction();
        String userCuid = taskAction.getUser();
        try {
            User user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + userCuid);
            Long assigneeId = null;
            if (com.centurylink.mdw.model.task.TaskAction.ASSIGN.equals(action)
                    || com.centurylink.mdw.model.task.TaskAction.CLAIM.equals(action)) {
                String assignee = taskAction.getAssignee();
                if (assignee == null) {
                    assignee = userCuid;
                }
                User assigneeUser = UserGroupCache.getUser(assignee);
                if (assigneeUser == null)
                    throw new ServiceException("Assignee user not found: " + assignee);
                assigneeId = assigneeUser.getId();
            }
            String destination = taskAction.getDestination();
            String comment = taskAction.getComment();

            List<Long> taskInstanceIds;
            Long taskInstanceId = taskAction.getTaskInstanceId();
            if (taskInstanceId != null) {
                taskInstanceIds = new ArrayList<Long>();
                taskInstanceIds.add(taskInstanceId);
            }
            else {
                taskInstanceIds = taskAction.getTaskInstanceIds();
                if (taskInstanceIds == null || taskInstanceIds.isEmpty())
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Missing TaskAction field: 'taskInstanceId' or 'taskInstanceIds'");
            }

            for (Long instanceId : taskInstanceIds) {
                TaskInstance taskInst = getInstance(instanceId);
                if (taskInst == null)
                    throw new TaskValidationException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);

                TaskRuntimeContext runtimeContext = getContext(taskInst);
                TaskActionValidator validator = new TaskActionValidator(runtimeContext);
                validator.validateAction(taskAction);

                TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInst);
                helper.performAction(action, user.getId(), assigneeId, comment, destination, true, false);

                if (logger.isDebugEnabled())
                    logger.debug("Performed action: " + action + " on task instance: " + instanceId);
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            String msg = "Error performing action: " + action + " on task instance(s)";
            throw new ServiceException(ServiceException.INTERNAL_ERROR, msg, ex);
        }

    }

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException {
        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(masterTaskInstanceId);
            List<TaskInstance> subtasks = helper.getSubtasks(masterTaskInstanceId);
            for (TaskInstance subtask : subtasks) {
                TaskWorkflowHelper subtaskHelper = new TaskWorkflowHelper(subtask);
                subtask.setTaskInstanceUrl(subtaskHelper.getTaskInstanceUrl());
            }
            return new TaskList("subtasks", subtasks);
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Problem getting subtasks for: " + masterTaskInstanceId, ex);
        }
    }

    public List<TaskCount> getTopThroughputTasks(String aggregateBy, Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            if ("task".equals(aggregateBy)) {
                List<TaskCount> list = getAggregateDataAccess().getTopTasks(query);
                timer.logTimingAndContinue("AggregateDataAccessVcs.getTopTasks()");
                list = populate(list);
                timer.stopAndLogTiming("TaskServicesImpl.populate()");
                return list;
            }
            else if ("workgroup".equals(aggregateBy)) {
                List<TaskCount> list = getAggregateDataAccess().getTopTaskWorkgroups(query);
                timer.stopAndLogTiming("AggregateDataAccessVcs.getTopTaskWorkgroups()");
                return list;
            }
            else if ("assignee".equals(aggregateBy)) {
                List<TaskCount> list = getAggregateDataAccess().getTopTaskAssignees(query);
                timer.stopAndLogTiming("AggregateDataAccessVcs.getTopTaskAssignees()");
                return list;
            }
            else {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported aggregation: " + aggregateBy);
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error retrieving top throughput processes: query=" + query, ex);
        }
    }

    public Map<Date,List<TaskCount>> getTaskInstanceBreakdown(Query query) throws ServiceException {
        try {
            Map<Date,List<TaskCount>> map = getAggregateDataAccess().getTaskInstanceBreakdown(query);
            if (query.getFilters().get("taskIds") != null) {
                for (Date date : map.keySet())
                    populate(map.get(date));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving task instance breakdown: query=" + query, ex);
        }
    }

    public List<TaskTemplate> getTaskTemplates(Query query) throws ServiceException {
        List<TaskTemplate> templates;
        String find = query.getFind();
        if (find == null) {
            templates = TaskTemplateCache.getTaskTemplates();
        }
        else {
            templates = new ArrayList<TaskTemplate>();
            String findLower = find.toLowerCase();
            for (TaskTemplate taskVO : TaskTemplateCache.getTaskTemplates()) {
                if (taskVO.getTaskName() != null && taskVO.getTaskName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
                else if (find.indexOf(".") > 0 && taskVO.getPackageName() != null && taskVO.getPackageName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
            }
            return templates;
        }
        Collections.sort(templates, new Comparator<TaskTemplate>() {
            public int compare(TaskTemplate t1, TaskTemplate t2) {
                return t1.getName().compareToIgnoreCase(t2.getName());
            }
        });
        return templates;
    }

    public Map<String, List<TaskTemplate>> getTaskTemplatesByPackage(Query query)
            throws ServiceException {
        List<TaskTemplate> taskVOs = getTaskTemplates(query);
        Map<String, List<TaskTemplate>> templates = new HashMap<>();
        for (TaskTemplate taskVO : taskVOs) {
            List<TaskTemplate> templateList = templates.get(taskVO.getPackageName());
            if (templateList == null) {
                templateList = new ArrayList<>();
                templates.put(taskVO.getPackageName(), templateList);
            }
            templateList.add(taskVO);
        }
        return templates;
    }

    public void updateTask(String userCuid, TaskInstance taskInstance) throws ServiceException {
        try {
            Long instanceId = taskInstance.getTaskInstanceId();
            TaskInstance oldTaskInstance = getInstance(instanceId);
            if (oldTaskInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
            if (!userCuid.equals(oldTaskInstance.getAssigneeCuid()))
                throw new ServiceException(ServiceException.FORBIDDEN, "Task instance " + instanceId + " not assigned to " + userCuid);
            if (oldTaskInstance.isInFinalStatus())
                throw new ServiceException(ServiceException.FORBIDDEN, "Updates not allowed for task " + instanceId + " with status " + oldTaskInstance.getStatus());

            TaskWorkflowHelper helper = new TaskWorkflowHelper(oldTaskInstance);

            // due date
            if (taskInstance.getDue() == null) {
                if (oldTaskInstance.getDue() != null)
                    helper.updateDue(null, userCuid, null);
            }
            else if (!taskInstance.getDue().equals(oldTaskInstance.getDue())) {
                if (Date.from(taskInstance.getDue()).compareTo(DatabaseAccess.getDbDate()) < 0)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Cannot set due date in the past for task instance " + instanceId);
                helper.updateDue(taskInstance.getDue(), userCuid, null);
            }

            // priority
            if (taskInstance.getPriority() == null) {
                if (oldTaskInstance.getPriority() != null && oldTaskInstance.getPriority().intValue() != 0)
                    helper.updatePriority(0);
            }
            else if (!taskInstance.getPriority().equals(oldTaskInstance.getPriority()))
                helper.updatePriority(taskInstance.getPriority());

            // comments
            if (taskInstance.getComments() == null) {
                if (oldTaskInstance.getComments() != null)
                    helper.updateComments(null);
            }
            else if (!taskInstance.getComments().equals(oldTaskInstance.getComments())) {
                helper.updateComments(taskInstance.getComments());
            }

            // workgroups
            if (taskInstance.getWorkgroups() == null || taskInstance.getWorkgroups().isEmpty()) {
                if (oldTaskInstance.getWorkgroups() != null && !oldTaskInstance.getWorkgroups().isEmpty())
                    helper.updateWorkgroups(new ArrayList<String>());
            }
            else if (!taskInstance.getWorkgroupsString().equals(oldTaskInstance.getWorkgroupsString())) {
                helper.updateWorkgroups(taskInstance.getWorkgroups());
            }

            helper.notifyTaskAction(TaskAction.SAVE, null, null);
            // audit log
            UserAction userAction = new UserAction(userCuid, Action.Change, Entity.TaskInstance, instanceId, "summary");
            userAction.setSource("Task Services");
            ServiceLocator.getEventServices().createAuditLog(userAction);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    /**
     * Fills in task header info, consulting latest instance comment if necessary.
     */
    protected List<TaskCount> populate(List<TaskCount> taskCounts) throws DataAccessException {
        AggregateDataAccessVcs dataAccess = null;
        for (TaskCount tc : taskCounts) {
            TaskTemplate taskTemplate = TaskTemplateCache.getTaskTemplate(tc.getId());
            if (taskTemplate == null) {
                logger.severe("Missing definition for task id: " + tc.getId());
                tc.setDefinitionMissing(true);
                // may have been deleted -- infer from comments
                if (dataAccess == null)
                    dataAccess = getAggregateDataAccess();
                CodeTimer timer = new CodeTimer(true);
                String comments = dataAccess.getLatestTaskInstanceComments(tc.getId());
                timer.stopAndLogTiming("getLatestTaskInstanceComments()");
                if (comments != null) {
                    AssetHeader assetHeader = new AssetHeader(comments);
                    tc.setName(assetHeader.getName());
                    tc.setVersion(assetHeader.getVersion());
                    tc.setPackageName(assetHeader.getPackageName());
                }
                else {
                    logger.severe("Unable to infer task name for: " + tc.getId());
                    tc.setName("Unknown (" + tc.getId() + ")");
                }
            }
            else {
                tc.setName(taskTemplate.getName());
                tc.setVersion(Asset.formatVersion(taskTemplate.getVersion()));
                tc.setPackageName(taskTemplate.getPackageName());
            }
        }
        return taskCounts;
    }

    @Override
    public List<EventLog> getHistory(Long taskInstanceId) throws ServiceException {
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            return eventManager.getEventLogs(null, null, "TaskInstance", taskInstanceId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public void updateIndexes(Long taskInstanceId, Map<String,String> indexes)
            throws ServiceException {
        try {
            new TaskDataAccess().setTaskInstanceIndices(taskInstanceId, indexes);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR,
                    "Index error on task " + taskInstanceId + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void cancelTaskInstancesForProcess(Long processInstanceId)
            throws ServiceException, DataAccessException {
        CodeTimer timer = new CodeTimer("cancelTaskInstancesForProcess()", true);
        List<TaskInstance> instances = getTaskInstancesForProcess(processInstanceId);
        if (instances == null || instances.size() == 0) {
            timer.stopAndLogTiming("NoTaskInstances");
            return;
        }
        TaskDataAccess dataAccess = new TaskDataAccess();
        for (TaskInstance instance : instances) {
            instance.setComments("Task has been cancelled by ProcessInstance.");
            String instantStatus = instance.getStatus();
            if (instantStatus == null) {
                TaskStatus taskStatus = DataAccess.getBaselineData().getTaskStatuses().get(instance.getStatusCode());
                if (taskStatus != null)
                    instantStatus = taskStatus.getDescription();
            }
            if (!instance.isInFinalStatus()) {
                Integer prevStatus = instance.getStatusCode();
                Integer prevState = instance.getStateCode();

                instance.setStateCode(TaskState.STATE_CLOSED);
                instance.setStatusCode(TaskStatus.STATUS_CANCELLED);
                Map<String,Object> changes = new HashMap<String,Object>();
                changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_CANCELLED);
                changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
                dataAccess.updateTaskInstance(instance.getTaskInstanceId(), changes, true);
                new TaskWorkflowHelper(instance).notifyTaskAction(TaskAction.CANCEL, prevStatus, prevState);
            }
        }
        timer.stopAndLogTiming("");
    }

    public List<TaskInstance> getTaskInstancesForProcess(Long processInstanceId)
    throws ServiceException, DataAccessException {
        CodeTimer timer = new CodeTimer("getTaskInstancesForProcess()", true);
        List<TaskInstance> daoResults = getTaskDAO().getTaskInstancesForProcessInstance(processInstanceId);
        timer.stopAndLogTiming("");
        return daoResults;
    }

    public void cancelTaskForActivity(Long activityInstanceId) throws ServiceException, DataAccessException {
        TaskInstance taskInstance = getTaskInstanceForActivity(activityInstanceId);
        if (taskInstance == null)
            throw new ServiceException("Cannot find the task instance for the activity instance: " + activityInstanceId);
        if (taskInstance.getStatusCode().equals(TaskStatus.STATUS_ASSIGNED)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_IN_PROGRESS)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_OPEN)) {
            new TaskWorkflowHelper(taskInstance).cancel();
        }
    }

    public TaskInstance getTaskInstanceForActivity(Long activityInstanceId)
            throws ServiceException, DataAccessException {
        return getTaskDAO().getTaskInstanceByActivityInstanceId(activityInstanceId);
    }

    public List<String> getGroupsForTaskInstance(TaskInstance taskInstance)
            throws DataAccessException, ServiceException {
        if (taskInstance.isShallow())
            new TaskWorkflowHelper(taskInstance).getTaskInstanceAdditionalInfo();
        return taskInstance.getGroups();
    }

    public List<TaskAction> getActions(Long instanceId, String userCuid, Query query) throws ServiceException {
        TaskInstance taskInstance = getInstance(instanceId);
        if (taskInstance == null) {
            throw new ServiceException(ServiceException.NOT_FOUND,
                    "Unable to load runtime context for task instance: " + instanceId);
        }

        try {
            TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
            if (query.getBooleanFilter("custom"))
                return helper.getCustomActions();
            else
                return AllowableTaskActions.getTaskDetailActions(userCuid, helper.getContext());
        }
        catch (Exception ex) {
            throw new ServiceException("Failed to get actions for task instance: " + instanceId, ex);
        }
    }

    public void updateTaskInstanceState(Long taskInstId, boolean isAlert)
            throws DataAccessException, ServiceException {
        TaskInstance taskInstance = getInstance(taskInstId);
        TaskWorkflowHelper helper = new TaskWorkflowHelper(taskInstance);
        helper.updateState(isAlert);
    }

}
