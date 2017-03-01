/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetHeader;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
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
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.task.SubTask;
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

    public Long createTask(String userCuid, String logicalId) throws ServiceException {

        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            TaskTemplate template = TaskTemplateCache.getTaskTemplate(logicalId);
            if (template == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task Template '" + logicalId + "' not found");

            User user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User '" + userCuid + "' not found");
            TaskInstance instance = taskManager.createTaskInstance(template.getId(), null, null, null, user.getId(), (Long)null);

            logger.info("Task instance created: " + instance.getTaskInstanceId());
            return instance.getTaskInstanceId();
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
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
                    return filterForActivityInstance(taskList, processInstance, activityInstanceIds);
                }
            }

            TaskList taskList = getTaskDAO().getTaskInstances(query);
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
                            Transition workTrans = process.getWorkTransition(transitionId);
                            if (workTrans == null && process.getSubProcesses() != null) {
                                for (Process subproc : process.getSubProcesses()) {
                                    workTrans = subproc.getWorkTransition(transitionId);
                                    if (workTrans != null)
                                        break;
                                }
                            }
                            if (workTrans.getToWorkId().equals(activityId))
                                taskInstances.add(taskInstance);
                        }
                    }
                }
            }
        }

        filteredList.setTasks(taskInstances);
        filteredList.setCount(taskInstances.size());
        filteredList.setTotal(taskInstances.size());
        filteredList.setRetrieveDate(taskList.getRetrieveDate());
        return filteredList;
    };

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException {
        TaskDataAccess taskDao = new TaskDataAccess(new DatabaseAccess(null));
        List<TaskInstance> tasks = taskDao.getTaskInstancesForProcessInstance(processInstanceId, true);
        TaskList taskList = new TaskList(TaskList.PROCESS_TASKS, tasks);
        taskList.setRetrieveDate(DatabaseAccess.getDbDate());
        taskList.setCount(tasks.size());
        return taskList;
    }

    public void saveTaskTemplate(TaskTemplate taskVO) {

    }

    public TaskInstance createTaskInstance(AssetVersionSpec spec, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId) throws TaskException, DataAccessException, CachingException {

        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(spec);
        if (taskVO == null)
            throw new DataAccessException("Task template not found: " + spec);

        TaskManager taskManager = ServiceLocator.getTaskManager();
        TaskInstance instance = taskManager.createTaskInstance(taskVO.getTaskId(), masterRequestId, processInstanceId,
                OwnerType.WORK_TRANSITION_INSTANCE, transitionId);

        TaskRuntimeContext runtimeContext = taskManager.getTaskRuntimeContext(instance);

        taskManager.setIndexes(runtimeContext);

        List<SubTask> subTaskList = taskManager.getSubTaskList(runtimeContext);
        if (subTaskList != null && !subTaskList.isEmpty())
            createSubTasks(subTaskList, runtimeContext);

        logger.info("Created task instance " + instance.getId() + " (" + taskVO.getTaskName() + ")");

        return instance;
    }

    public Map<String,String> getIndexes(Long taskInstanceId) throws DataAccessException {
        return getTaskDAO().getTaskInstIndices(taskInstanceId);
    }

    public void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
            throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        TaskTemplate subTaskVo = TaskTemplateCache.getTaskTemplate(subtaskLogicalId);
        if (subTaskVo == null)
            throw new TaskException("Task Template '" + subtaskLogicalId + "' does not exist");

        TaskInstance masterTaskInstance = taskManager.getTaskInstance(masterTaskInstanceId);
        TaskRuntimeContext masterTaskContext = taskManager.getTaskRuntimeContext(masterTaskInstance);
        TaskInstance subTaskInstance = taskManager.createTaskInstance(subTaskVo.getTaskId(), masterTaskContext.getMasterRequestId(),
                masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId());
        logger.info("SubTask instance created - ID: " + subTaskInstance.getTaskInstanceId());
    }

    public void createSubTasks(List<SubTask> subTaskList, TaskRuntimeContext masterTaskContext)
            throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        for (SubTask subTask : subTaskList) {
            TaskTemplate subTaskVo = TaskTemplateCache.getTaskTemplate(subTask.getLogicalId());
            if (subTaskVo == null)
                throw new TaskException("Task Template '" + subTask.getLogicalId() + "' does not exist");

            TaskInstance subTaskInstance = taskManager.createTaskInstance(subTaskVo.getTaskId(), masterTaskContext.getMasterRequestId(),
                    masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId());
            logger.info("SubTask instance created - ID " + subTaskInstance.getTaskInstanceId());
            // TODO recursive subtask creation
        }
    }

    public TaskInstance getInstance(Long instanceId) throws DataAccessException {
        TaskManager taskMgr = ServiceLocator.getTaskManager();
        TaskInstance taskInstance = taskMgr.getTaskInstance(instanceId);
        if (taskInstance == null)
            return null;
        taskInstance.setRetrieveDate(DatabaseAccess.getDbDate());
        return taskInstance;
    }

    public TaskRuntimeContext getRuntimeContext(Long instanceId) throws ServiceException {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstance taskInstance = taskMgr.getTaskInstance(instanceId);
            if (taskInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
            return taskMgr.getTaskRuntimeContext(taskInstance);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot get runtime context for task instance: " + instanceId, ex);
        }
    }

    public Map<String,Value> getValues(Long instanceId) throws ServiceException {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstance taskInstance = taskMgr.getTaskInstance(instanceId);
            if (taskInstance == null) {
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
            }
            TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskInstance);
            if (runtimeContext.getTaskTemplate().isAutoformTask()) {
                return new AutoFormTaskValuesProvider().collect(runtimeContext);
            }
            else {
                // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
                return new HashMap<String,Value>();
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error getting values for task instance: " + instanceId, ex);
        }
    }

    /**
     * Update task values.
     */
    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException {
        try {
            // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskMgr.getTaskInstance(instanceId));
            TaskValuesProvider valuesProvider;
            if (runtimeContext.getTaskTemplate().isAutoformTask())
                valuesProvider = new AutoFormTaskValuesProvider();
            else
                valuesProvider = new CustomTaskValuesProvider();
            if (runtimeContext.getTaskTemplate().isAutoformTask()) {
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                valuesProvider.apply(runtimeContext, values);
                Map<String,Object> newValues = new HashMap<String,Object>();
                for (String name : values.keySet()) {
                    if (runtimeContext.isExpression(name)) {
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
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error setting values for task instance: " + instanceId, ex);
        }
        catch (ServiceException ex) {
            throw ex;
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

            TaskManager taskMgr = ServiceLocator.getTaskManager();
            for (Long instanceId : taskInstanceIds) {
                TaskInstance taskInst = taskMgr.getTaskInstance(instanceId);
                if (taskInst == null)
                    throw new TaskValidationException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);

                TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskInst);
                TaskActionValidator validator = new TaskActionValidator(runtimeContext);
                validator.validateAction(taskAction);

                taskMgr.performActionOnTaskInstance(action, instanceId, user.getId(), assigneeId, comment,
                        destination, true, false);

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
            List<TaskInstance> subtasks = getTaskDAO().getSubTaskInstances(masterTaskInstanceId);
            TaskList subtaskList = new TaskList("subtasks", subtasks);
            subtaskList.setRetrieveDate(DatabaseAccess.getDbDate());
            return subtaskList;
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
                if (taskVO.getName() != null && taskVO.getName().toLowerCase().startsWith(findLower))
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

    public void updateTask(String userCuid, TaskInstance taskInstance) throws ServiceException {
        try {
            Long instanceId = taskInstance.getTaskInstanceId();
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstance oldTaskInstance = taskMgr.getTaskInstance(instanceId);
            if (oldTaskInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);
            if (!userCuid.equals(oldTaskInstance.getAssigneeCuid()))
                throw new ServiceException(ServiceException.FORBIDDEN, "Task instance " + instanceId + " not assigned to " + userCuid);
            if (oldTaskInstance.isInFinalStatus())
                throw new ServiceException(ServiceException.FORBIDDEN, "Updates not allowed for task " + instanceId + " with status " + oldTaskInstance.getStatus());


            // due date
            if (taskInstance.getDueDate() == null) {
                if (oldTaskInstance.getDueDate() != null)
                    taskMgr.updateTaskInstanceDueDate(instanceId, null, userCuid, null);
            }
            else if (!taskInstance.getDueDate().equals(oldTaskInstance.getDueDate())) {
                if (taskInstance.getDueDate().compareTo(DatabaseAccess.getDbDate()) < 0)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Cannot set due date in the past for task instance " + instanceId);
                taskMgr.updateTaskInstanceDueDate(instanceId, taskInstance.getDueDate(), userCuid, null);
            }

            // priority
            if (taskInstance.getPriority() == null) {
                if (oldTaskInstance.getPriority() != null && oldTaskInstance.getPriority().intValue() != 0)
                    taskMgr.updateTaskInstancePriority(instanceId, 0);
            }
            else if (!taskInstance.getPriority().equals(oldTaskInstance.getPriority()))
                taskMgr.updateTaskInstancePriority(instanceId, taskInstance.getPriority());

            // comments
            if (taskInstance.getComments() == null) {
                if (oldTaskInstance.getComments() != null)
                    taskMgr.updateTaskInstanceComments(instanceId, null);
            }
            else if (!taskInstance.getComments().equals(oldTaskInstance.getComments())) {
                taskMgr.updateTaskInstanceComments(instanceId, taskInstance.getComments());
            }

            // workgroups
            if (taskInstance.getWorkgroups() == null || taskInstance.getWorkgroups().isEmpty()) {
                if (oldTaskInstance.getWorkgroups() != null && !oldTaskInstance.getWorkgroups().isEmpty())
                    taskMgr.updateTaskInstanceWorkgroups(instanceId, new ArrayList<String>());
            }
            else if (!taskInstance.getWorkgroupsString().equals(oldTaskInstance.getWorkgroupsString())) {
                taskMgr.updateTaskInstanceWorkgroups(instanceId, taskInstance.getWorkgroups());
            }

            taskMgr.notifyTaskAction(taskInstance, TaskAction.SAVE, null, null);
            // audit log
            UserAction userAction = new UserAction(userCuid, Action.Change, Entity.TaskInstance, instanceId, "summary");
            userAction.setSource("Task Services");
            ServiceLocator.getEventManager().createAuditLog(userAction);
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
}
