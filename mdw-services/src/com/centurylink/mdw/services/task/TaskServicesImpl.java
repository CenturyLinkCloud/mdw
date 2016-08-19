/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.asset.AssetHeader;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskCount;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.dao.task.TaskDAO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.task.SubTask;

/**
 * Services related to manual tasks.
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

    public Long createTask(String userCuid, String logicalId) throws ServiceException {

        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            TaskVO template = TaskTemplateCache.getTaskTemplate(null, logicalId);
            if (template == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Task Template '" + logicalId + "' not found");

            UserVO user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User '" + userCuid + "' not found");
            TaskInstanceVO instance = taskManager.createTaskInstance(template.getId(), null, null, null, user.getId(), (Long)null);

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
     * Returns tasks associated with the specified user's workgroups.
     */
    public TaskList getWorkgroupTasks(String cuid, Query query) throws DataAccessException {
        try {
            UserVO user = UserGroupCache.getUser(cuid);
            if (user == null)
                throw new DataAccessException("Unknown user: " + cuid);

            String workgroups = query.getFilter("workgroups");
            if (workgroups == null || workgroups.equals("[My Workgroups]"))
              query.setArrayFilter("workgroups", user.getGroupNames());
            if ("[My Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().put("assignee", cuid);
            else if ("[Everyone's Tasks]".equals(query.getFilter("assignee")))
                query.getFilters().remove("assignee");
            TaskList taskList = getTaskDAO().getTaskInstances(query);
            return taskList;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (CachingException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException {
        TaskDAO taskDao = new TaskDAO(new DatabaseAccess(null));
        List<TaskInstanceVO> tasks = taskDao.getTaskInstancesForProcessInstance(processInstanceId, true);
        TaskList taskList = new TaskList(TaskList.PROCESS_TASKS, tasks);
        taskList.setRetrieveDate(DatabaseAccess.getDbDate());
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
        TaskInstanceVO taskInstance = taskMgr.getTaskInstance(instanceId);
        taskInstance.setRetrieveDate(DatabaseAccess.getDbDate());
        return taskInstance;
    }

    public TaskRuntimeContext getRuntimeContext(Long instanceId) throws ServiceException {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstanceVO taskInstance = taskMgr.getTaskInstance(instanceId);
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
            TaskInstanceVO taskInstance = taskMgr.getTaskInstance(instanceId);
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
                    VariableVO var = runtimeContext.getProcess().getVariable(name);
                    if (var == null)
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Process Variable not found: " + name);
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
        catch (ServiceException ex) {
            throw ex;
        }
    }

    public void performTaskAction(TaskActionVO taskAction) throws ServiceException {
        String action = taskAction.getTaskAction();
        String userCuid = taskAction.getUser();
        try {
            UserVO user = UserGroupCache.getUser(userCuid);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + userCuid);
            Long assigneeId = null;
            if (com.centurylink.mdw.model.data.task.TaskAction.ASSIGN.equals(action)
                    || com.centurylink.mdw.model.data.task.TaskAction.CLAIM.equals(action)) {
                String assignee = taskAction.getAssignee();
                if (assignee == null) {
                    assignee = userCuid;
                }
                UserVO assigneeUser = UserGroupCache.getUser(assignee);
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
                TaskInstanceVO taskInst = taskMgr.getTaskInstance(instanceId);
                if (taskInst == null)
                    throw new TaskValidationException(ServiceException.NOT_FOUND, "Task instance not found: " + instanceId);

                TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskInst);
                TaskActionValidator validator = new TaskActionValidator(runtimeContext);
                validator.validateAction(taskAction);

                taskMgr.performActionOnTaskInstance(action, instanceId, user.getId(), assigneeId, comment,
                        destination, OwnerType.PROCESS_INSTANCE.equals(taskInst.getOwnerType())
                                && !TaskManagerAccess.getInstance().isRemoteDetail(), false);

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

    public List<TaskVO> getTaskTemplates(Query query) throws ServiceException {
        List<TaskVO> templates;
        String find = query.getFind();
        if (find == null) {
            templates = TaskTemplateCache.getTaskTemplates();
        }
        else {
            templates = new ArrayList<TaskVO>();
            String findLower = find.toLowerCase();
            for (TaskVO taskVO : TaskTemplateCache.getTaskTemplates()) {
                if (taskVO.getName() != null && taskVO.getName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
                else if (find.indexOf(".") > 0 && taskVO.getPackageName() != null && taskVO.getPackageName().toLowerCase().startsWith(findLower))
                    templates.add(taskVO);
            }
            return templates;
        }
        Collections.sort(templates, new Comparator<TaskVO>() {
            public int compare(TaskVO t1, TaskVO t2) {
                return t1.getName().compareToIgnoreCase(t2.getName());
            }
        });
        return templates;
    }

    public void updateTask(String userCuid, TaskInstanceVO taskInstance) throws ServiceException {
        try {
            Long instanceId = taskInstance.getTaskInstanceId();
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstanceVO oldTaskInstance = taskMgr.getTaskInstance(instanceId);
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
            UserActionVO userAction = new UserActionVO(userCuid, Action.Change, Entity.TaskInstance, instanceId, "summary");
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
