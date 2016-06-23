/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.Task;
import com.centurylink.mdw.common.service.types.TaskAction;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.asset.AssetHeader;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskCount;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.task.TaskDAO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.task.SubTask;

/**
 * TODO Implement replacements for old-style services createTask, updateTask and performActionOnTask.
 *
 * TODO add TaskNotes services based on InstanceNotes
 */
public class TaskServicesImpl implements TaskServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskDAO getTaskDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new TaskDAO(db);
    }

    protected AggregateDataAccessVcs getAggregateDataAccess() throws DataAccessException {
        return new AggregateDataAccessVcs();
    }

    /**
     * Create a manual task instance that points to a remote task.
     * The base URL for accessing the remote task is included as the "appUrl" parameter.
     *
     * @param task
     * @param parameters
     */
    public void createTask(Task task, Map<String,Object> parameters) throws ServiceException {
        throw new UnsupportedOperationException("TODO: implement for AdminUI");
    }

    /**
     * Update a task instance.
     *
     * @param task
     * @param parameters
     */
    public void updateTask(Task task, Map<String,Object> parameters) throws ServiceException {
        throw new UnsupportedOperationException("TODO: implement for AdminUI");
    }

    /**
     * Perform the designated action on a task instance.
     *
     * @param taskAction
     * @param parameters
     */
    public void performActionOnTask(TaskAction taskAction, Map<String,Object> parameters) throws ServiceException {
        throw new UnsupportedOperationException("TODO: implement for AdminUI");
    }

    /**
     * Returns all the tasks assigned to a particular user.
     */
    public TaskList getUserTasks(String cuid) throws TaskException, DataAccessException {
        UserVO user = ServiceLocator.getUserManager().getUser(cuid);
        if (user == null)
            throw new DataAccessException("Unknown user: " + cuid);
        Map<String,String> criteria = new HashMap<String,String>();
        criteria.put("taskClaimUserId", " = " + user.getId());
        criteria.put("statusCode", " in (" + TaskStatus.STATUS_ASSIGNED.toString() + ", " + TaskStatus.STATUS_IN_PROGRESS.toString() + ")");

        TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));
        List<TaskInstanceVO> tasks = taskDao.queryTaskInstances(criteria, null, null, null, null);
        Collections.sort(tasks, new Comparator<TaskInstanceVO>() {
            public int compare(TaskInstanceVO ti1, TaskInstanceVO ti2) {
                return ti2.getId().compareTo(ti1.getId());  // descending instanceId
            }
        });
        for (TaskInstanceVO task : tasks) {
            // TODO refactor this and handle remote task manager urls
            task.setTaskInstanceUrl(TaskManagerAccess.getInstance().getTaskInstanceUrl(task));
        }
        TaskList taskList = new TaskList(TaskList.USER_TASKS, tasks);
        taskList.setRetrieveDate(new Date()); // TODO db time
        taskList.setCount(tasks.size());
        return taskList;
    }

    /**
     * Returns tasks associated with the specified user's workgroups.
     * TODO: filtering, pagination and sorting
     */
    public TaskList getWorkgroupTasks(String cuid, Query query) throws TaskException, UserException, DataAccessException {
        UserManager userMgr = ServiceLocator.getUserManager();
        UserVO user = userMgr.getUser(cuid);
        if (user == null)
            throw new DataAccessException("Unknown user: " + cuid);

        Map<String,String> criteria = query.getFilters();
        if (criteria == null)
            criteria = new HashMap<String,String>();

        String status = criteria.get("status");
        if (status == null)
            status = TaskStatus.STATUSNAME_ACTIVE;

        if (status.equals(TaskStatus.STATUSNAME_ACTIVE))
            criteria.put("statusCode", " != " + TaskStatus.STATUS_COMPLETED + " and task_instance_status != " + TaskStatus.STATUS_CANCELLED);
        else if (status.equals(TaskStatus.STATUSNAME_CLOSED))
            criteria.put("statusCode", " = " + TaskStatus.STATUS_COMPLETED + " or task_instance_status = " + TaskStatus.STATUS_CANCELLED);
        else if (!status.equals(TaskStatus.STATUSNAME_ALL))
            criteria.put("statusCode", String.valueOf(TaskStatus.getStatusCodeForName(status)));

        criteria.remove("status");

        UserGroupVO[] userGroups = userMgr.getGroupsForUser(cuid);
        String[] workgroups = new String[userGroups.length];
        for (int i = 0; i < userGroups.length; i++) {
            workgroups[i] = userGroups[i].getName();
        }

        String orderBy = query.getSort();
        if (orderBy == null)
            orderBy = "TASK_INSTANCE_ID";
        TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));

        int start = query.getStart();
        int end = query.getMax() - start;
        // TODO: variables and indexes (or better way for AdminUI?)
        List<TaskInstanceVO> tasks = taskDao.queryTaskInstances(criteria, null, null, null, null, null, null, workgroups, orderBy, !query.isDescending(), start, end);
        TaskList taskList = new TaskList(TaskList.WORKGROUP_TASKS, tasks);
        taskList.setRetrieveDate(new Date()); // TODO db time
        taskList.setCount(tasks.size());
        return taskList;
    }

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException {
        TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));
        List<TaskInstanceVO> tasks = taskDao.getTaskInstancesForProcessInstance(processInstanceId, true);
        TaskList taskList = new TaskList(TaskList.PROCESS_TASKS, tasks);
        taskList.setRetrieveDate(new Date()); // TODO db time
        taskList.setCount(tasks.size());
        return taskList;
    }

    public void saveTaskTemplate(TaskVO taskVO) {

    }

    public TaskInstanceVO createCustomTaskInstance(String logicalId, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId) throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(null, logicalId);
        if (taskVO == null)
            throw new DataAccessException("Task template '" + logicalId + "' is not defined");

        TaskInstanceVO instance = taskManager.createTaskInstance(taskVO.getTaskId(), masterRequestId, processInstanceId,
                OwnerType.WORK_TRANSITION_INSTANCE, transitionId);

        TaskRuntimeContext runtimeContext = taskManager.getTaskRuntimeContext(instance);

        taskManager.setIndexes(runtimeContext);

        List<SubTask> subTaskList = taskManager.getSubTaskList(runtimeContext);
        if (subTaskList != null && !subTaskList.isEmpty())
            createSubTasks(subTaskList, runtimeContext);

        logger.info("Created task instance " + instance.getId() + " (" + taskVO.getTaskName() + ")");

        return instance;
    }

    public TaskInstanceVO createAutoFormTaskInstance(String logicalId, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, FormDataDocument formDoc) throws TaskException, DataAccessException {

        TaskManager taskManager = ServiceLocator.getTaskManager();
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(null, logicalId);
        if (taskVO == null)
            throw new DataAccessException("Task template '" + logicalId + "' is not defined");

        EventManager eventManager = ServiceLocator.getEventManager();
        Long documentId = eventManager.createDocument(FormDataDocument.class.getName(), 0L, OwnerType.LISTENER_REQUEST, // ugh
                1L, null, null, formDoc.format());

        TaskInstanceVO instance = taskManager.createTaskInstance(taskVO.getTaskId(), masterRequestId, processInstanceId,
                OwnerType.DOCUMENT, documentId);

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
        TaskVO subTaskVo = TaskTemplateCache.getTaskTemplate(null, subtaskLogicalId);
        if (subTaskVo == null)
            throw new TaskException("Task Template '" + subtaskLogicalId + "' does not exist");

        TaskInstanceVO masterTaskInstance = taskManager.getTaskInstance(masterTaskInstanceId);
        TaskRuntimeContext masterTaskContext = taskManager.getTaskRuntimeContext(masterTaskInstance);
        TaskInstanceVO subTaskInstance = taskManager.createTaskInstance(subTaskVo.getTaskId(), masterTaskContext.getMasterRequestId(),
                masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId());
        logger.info("SubTask instance created - ID: " + subTaskInstance.getTaskInstanceId());
    }

    public void createSubTasks(List<SubTask> subTaskList, TaskRuntimeContext masterTaskContext)
            throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        for (SubTask subTask : subTaskList) {
            TaskVO subTaskVo = TaskTemplateCache.getTaskTemplate(null, subTask.getLogicalId());
            if (subTaskVo == null)
                throw new TaskException("Task Template '" + subTask.getLogicalId() + "' does not exist");

            TaskInstanceVO subTaskInstance = taskManager.createTaskInstance(subTaskVo.getTaskId(), masterTaskContext.getMasterRequestId(),
                    masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId());
            logger.info("SubTask instance created - ID " + subTaskInstance.getTaskInstanceId());
            // TODO recursive subtask creation
        }
    }

    public TaskInstanceVO getInstance(Long instanceId) throws DataAccessException {
        TaskManager taskMgr = ServiceLocator.getTaskManager();
        return taskMgr.getTaskInstance(instanceId);
    }

    public Map<String,Value> getValues(Long instanceId) throws ServiceException {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskMgr.getTaskInstance(instanceId));
            if (runtimeContext.getTaskTemplate().isAutoformTask()) {
                return new AutoFormTaskValuesProvider().collect(runtimeContext);
            }
            else {
                // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
                return new HashMap<String,Value>();
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error getting values for task instance: " + instanceId, ex);
        }
    }

    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskMgr.getTaskInstance(instanceId));
            if (runtimeContext.getTaskTemplate().isAutoformTask()) {
                EventManager eventMgr = ServiceLocator.getEventManager();
                new AutoFormTaskValuesProvider().apply(runtimeContext, values);
                Map<String,Object> newValues = new HashMap<String,Object>();
                for (String name : values.keySet()) {
                    if (runtimeContext.isExpression(name)) {
                        // TODO: more robust
                        String rootVar = name.substring(2, name.indexOf('.'));
                        newValues.put(rootVar, runtimeContext.evaluate("#{" + rootVar + "}"));
                    }
                    else {
                        newValues.put(name, runtimeContext.getVariables().get(name));
                    }
                }
                for (String name : newValues.keySet()) {
                    Object newValue = newValues.get(name);
                    VariableVO var = runtimeContext.getProcess().getVariable(name);
                    String type = var.getVariableType();
                    if (VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), type)) {
                        String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), type, newValue);
                        VariableInstanceInfo varInst = runtimeContext.getProcessInstance().getVariable(name);
                        if (varInst == null) {
                            Long procInstId = runtimeContext.getProcessInstanceId();
                            Long docId = eventMgr.createDocument(type, procInstId, OwnerType.PROCESS_INSTANCE, procInstId, null, null, stringValue);
                            eventMgr.setVariableInstance(procInstId, name, new DocumentReference(docId, null));
                        }
                        else {
                            DocumentReference docRef = (DocumentReference) varInst.getData();
                            eventMgr.updateDocumentContent(docRef.getDocumentId(), stringValue, type);
                        }
                    }
                    else {
                        eventMgr.setVariableInstance(runtimeContext.getProcessInstanceId(), name, newValue);
                    }
                }
            }
            else {
                // TODO: implement CustomTaskValuesProvider, and also make provider configurable in Designer (like TaskIndexProvider)
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Error setting values for task instance: " + instanceId, ex);
        }
    }

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException {
        try {
            List<TaskInstanceVO> subtasks = getTaskDAO().getSubTaskInstances(masterTaskInstanceId);
            TaskList subtaskList = new TaskList("subtasks", subtasks);
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

    /**
     * Fills in task header info, consulting latest instance comment if necessary.
     */
    protected List<TaskCount> populate(List<TaskCount> taskCounts) throws DataAccessException {
        AggregateDataAccessVcs dataAccess = null;
        for (TaskCount tc : taskCounts) {
            TaskVO taskTemplate = TaskTemplateCache.getTaskTemplate(tc.getId());
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
                tc.setVersion(RuleSetVO.formatVersion(taskTemplate.getVersion()));
                tc.setPackageName(taskTemplate.getPackageName());
            }
        }
        return taskCounts;
    }


}
