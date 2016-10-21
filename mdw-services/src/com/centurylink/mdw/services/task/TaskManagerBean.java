/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.AUTO_ASSIGNEE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.DUE_DATE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.GROUPS;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.PRIORITY;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyGroups;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.task.TaskServiceRegistry;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringArrayHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.model.data.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStates;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.data.task.TaskStatuses;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.EventInstanceVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.observer.task.ParameterizedStrategy;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;
import com.centurylink.mdw.observer.task.RoutingStrategy;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.observer.task.TaskIndexProvider;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.TaskDAO;
import com.centurylink.mdw.services.dao.task.TaskDAOException;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.UserDAO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.task.factory.TaskInstanceStrategyFactory;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;
import com.qwest.mbeng.MbengException;

/**
 * Repackaged to separate business interface from implementation.
 */
public class TaskManagerBean implements TaskManager {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static String finalStatuses[] = null;

    private TaskDAO getTaskDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new TaskDAO(db);
    }

    public TaskAction[] getTaskActionVOs() throws TaskException, DataAccessException {
        List<TaskAction> taskActions = getTaskDAO().getAllTaskActions();
        try {
            UserManager userMgr = ServiceLocator.getUserManager();
            for (TaskAction action : taskActions) {
                UserRoleVO[] userRoles = userMgr.getUserRolesForTaskAction(action.getTaskActionId());
                action.setUserRoles(userRoles);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new TaskException(ex.getMessage(), ex);
        }

        return taskActions.toArray(new TaskAction[taskActions.size()]);
    }

    /**
     * Retrieves all tasks associated with a particular workgroup.
     * @return array of Task objects
     */
    public List<TaskVO> getTasksForWorkgroup(String groupName)
    throws DataAccessException {
        return getTaskDAO().getTasksForGroup(groupName);
    }

    /**
     * Returns all TaskVOs in their shallow form.  Sorted by latest first.
     *
     * @return Array of TaskVO
     */
    public TaskVO[] getShallowTaskVOs()
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getShallowTaskVOs()", true);
        List<TaskVO> shallowTaskVOs;
        if (ApplicationContext.isFileBasedAssetPersist()) {
            // sorted by latest first
            shallowTaskVOs = DataAccess.getProcessLoader().getTaskTemplates(); // actually not shallow
            shallowTaskVOs.addAll(getTaskDAO().getAllTasks()); // compatibility for pre-existing db tasks
        }
        else {
            shallowTaskVOs = getTaskDAO().getAllTasks();
            Collections.sort(shallowTaskVOs);
        }
        timer.stopAndLogTiming("");
        return shallowTaskVOs.toArray(new TaskVO[0]);
    }

    /**
     * Returns the fully populated TaskVO.
     */
    public TaskVO getTaskVO(Long taskId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getTaskVO()", true);
        TaskVO task = null;
        try {
            task = getTaskDAO().getTask(taskId);
            List<AttributeVO> attributes = getTaskAttributes(taskId);
            task.setAttributes(attributes);
            if (task.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES) != null) {
                task.setVariablesFromAttribute();
            }
            if (!task.isTemplate() && task.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA)==null) {
                // for MDW 3 and early versions of MDW 4 tasks
                ServiceLevelAgreement sla = getTaskDAO().getServiceLevelAgreement(taskId);
                if (sla != null)
                    task.setSlaSeconds(Math.round(sla.getSLAInHours().floatValue() * 3600));
            }
            if (null != task.getAttribute(TaskActivity.ATTRIBUTE_TASK_DESC)) {
              task.setComment(task.getAttribute(TaskActivity.ATTRIBUTE_TASK_DESC));
            }
            task.setShallow(false);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new TaskException(ex.getMessage());
        }
        timer.stopAndLogTiming("");
        return task;
    }

    /**
     * Method that returns the collection of attributes for the given task
     *
     * @param pTaskId
     * @return List of AttributeVOs
     */
    public List<AttributeVO> getTaskAttributes(Long pTaskId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getTaskAttributes()", true);
        List<AttributeVO> attributeVOs = getTaskDAO().getTaskAttributes(pTaskId);
        timer.stopAndLogTiming("");
        return attributeVOs;
    }


    /**
     * Returns the unclaimed tasks for the specified set of workgroups
     *
     * @param workgroups
     * @param queryRequest
     * @return PaginatedResponse
     */
    @Override
    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest)
    throws TaskException, DataAccessException {
        return getUnClaimedTaskInstanceVOs(workgroups, queryRequest, null, null, null, null, null, null);
    }

    /**
     * Returns the unclaimed tasks for the specified set of workgroups, including variables
     *
     * @param workgroups
     * @param queryRequest
     * @param variables variable values to retrieve
     * @param variablesCriteria
     * @param indexKeys
     * @param indexCriteria
     * @return PaginatedResponse
     */
    @Override
    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest, List<String> variables, Map<String,String> variablesCriteria,
            List<String> indexKeys, Map<String,String> indexCriteria)
    throws TaskException, DataAccessException {
        return getUnClaimedTaskInstanceVOs(workgroups, queryRequest, variables, variablesCriteria, indexKeys, indexCriteria, null, null);
    }

    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest,
            List<String> variables, Map<String, String> variablesCriteria, List<String> indexKeys, Map<String,String> indexCriteria, List<String> searchColumns, Object searchKey)
            throws TaskException, DataAccessException {
        TaskInstanceVO[] instArr = null;
        Map<String,String> criteria = queryRequest.getRestrictions();
        CodeTimer timer = new CodeTimer("TaskManager.getUnClaimedTaskInstanceVOs()", true);

        List<TaskInstanceVO> taskInstances = null;
        int totalRowsCount = 0;
        if (queryRequest.getPageSize() == QueryRequest.ALL_ROWS) {
            int maxRows = queryRequest.getShowAllDisplayRows();
            if (maxRows == 0) // unlimited
                taskInstances = getTaskDAO().queryTaskInstances(criteria, variables,variablesCriteria, indexKeys, indexCriteria, searchColumns, searchKey,
                        workgroups, queryRequest.getOrderBy(), queryRequest.isAscendingOrder());
            else
                taskInstances = getTaskDAO().queryTaskInstances(criteria, variables, variablesCriteria, indexKeys, indexCriteria, searchColumns, searchKey,
                        workgroups, queryRequest.getOrderBy(), queryRequest.isAscendingOrder(), 0, maxRows);
        }
        else {
            int startIndex = ((queryRequest.getPageIndex()) * queryRequest.getPageSize());
            int endIndex = startIndex + queryRequest.getPageSize();
            taskInstances = getTaskDAO().queryTaskInstances(criteria, variables, variablesCriteria,indexKeys, indexCriteria, searchColumns, searchKey, workgroups,
                    queryRequest.getOrderBy(), queryRequest.isAscendingOrder(), startIndex, endIndex);
        }
        Map<String,String> countRestrictions = new HashMap<String,String>(criteria);
        totalRowsCount = getTaskDAO().queryTaskInstancesCount(countRestrictions, variablesCriteria, indexCriteria, searchColumns, searchKey, workgroups);

        // update instances flagged as invalid (missing template)
        for (TaskInstanceVO taskInstance : taskInstances) {
            if (taskInstance.isInvalid())
                taskInstance.setStateCode(TaskState.STATE_INVALID);
        }

        instArr = taskInstances.toArray(new TaskInstanceVO[taskInstances.size()]);
        int returnedRows = instArr.length;

        PaginatedResponse response = new PaginatedResponse(instArr, totalRowsCount, returnedRows,
                queryRequest.getPageSize(), queryRequest.getPageIndex(), queryRequest.getShowAllDisplayRows());
        timer.stopAndLogTiming("");
        return response;
    }

    /**
     * Returns the claimed tasks for a given user
     *
     * @param userId
     * @param criteria
     * @return Array of TaskInstanceVOs
     */
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String,String> criteria)
    throws TaskException, DataAccessException {
        return getClaimedTaskInstanceVOs(userId, criteria, null, null);
    }

    /**
     * Returns the claimed tasks for a given user, including specified variable values
     *
     * @param userId
     * @param criteria
     * @param variables
     * @param variablesCriteria
     * @return Array of TaskInstanceVOs
     */
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria)
    throws TaskException, DataAccessException {
        return getClaimedTaskInstanceVOs(userId, criteria, variables, variablesCriteria, null, null);
    }

   /**
    *
    */
    @Override
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String, String> criteria, List<String> variables, Map<String, String> variablesCriteria,
            List<String> indexKeys, Map<String, String> indexCriteria) throws TaskException, DataAccessException {
        TaskInstanceVO[] retInstances = null;
        CodeTimer timer = new CodeTimer("TaskManager.getClaimedTaskInstanceVOs()", true);

        criteria.put("taskClaimUserId", " = " + userId);
        if (!(criteria.containsKey("statusCode"))) {
            criteria.put("statusCode", " in (" + TaskStatus.STATUS_ASSIGNED.toString() + ", " + TaskStatus.STATUS_IN_PROGRESS.toString() + ")");
        }

        List<TaskInstanceVO> taskInstances = getTaskDAO().queryTaskInstances(criteria, variables, variablesCriteria, indexKeys, indexCriteria);

        // update instances flagged as invalid (missing template)
        for (TaskInstanceVO taskInstance : taskInstances) {
            if (taskInstance.isInvalid()) {
                taskInstance.setStateCode(TaskState.STATE_INVALID);
            }
        }
        retInstances = taskInstances.toArray(new TaskInstanceVO[taskInstances.size()]);
        timer.stopAndLogTiming("");
        return retInstances;
    }

    /**
     * Returns a task instance VO
     *
     * @param pTaskInstId
     * @return the taskInst and associated data
     * @throws TaskDAOException
     * @throws DataAccessException
     */
    public TaskInstanceVO getTaskInstanceVO(Long pTaskInstId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getTaskInstanceVO()", true);
        TaskInstanceVO retVO = this.getTaskDAO().getTaskInstanceAllInfo(pTaskInstId);
        retVO.setTaskInstanceUrl(TaskManagerAccess.getInstance().getTaskInstanceUrl(retVO));
        timer.stopAndLogTiming("");
        return retVO;
    }

    /**
     * Returns tasks instance identified by the primary key
     *
     * @param taskInstanceId
     */
    public TaskInstanceVO getTaskInstance(Long taskInstanceId)
    throws DataAccessException {
        TaskInstanceVO taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);
        if (taskInstance == null)
            return null;
        if (taskInstance.getAssigneeId() != null && taskInstance.getAssigneeId() != 0 && taskInstance.getAssigneeCuid() == null) {
            try {
                UserVO user = UserGroupCache.getUser(taskInstance.getAssigneeId());
                if (user != null) {
                    taskInstance.setAssigneeCuid(user.getCuid());
                    taskInstance.setAssignee(user.getName());
                }
            }
            catch (CachingException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
        Long activityInstanceId = getActivityInstanceId(taskInstance, false);
        if (activityInstanceId != null) {
            taskInstance.setActivityInstanceId(activityInstanceId);
        }
        return taskInstance;
    }

    /**
     * Retrieve additional info for task instance, including
     * assignee user name, due date, groups, master request id, etc.
     */
    public void getTaskInstanceAdditionalInfo(TaskInstanceVO taskInst)
    throws DataAccessException, TaskException {
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
        if (taskVO.isTemplate())
            getTaskDAO().getTaskInstanceAdditionalInfoGeneral(taskInst);
        else {
            getTaskDAO().getTaskInstanceAdditionalInfoClassic(taskInst, taskVO.isTemplate());
        }
        taskInst.setTaskInstanceUrl(TaskManagerAccess.getInstance().getTaskInstanceUrl(taskInst));
    }

    public String getTaskInstanceUrl(Long taskInstanceId) throws DataAccessException, TaskException {
        return TaskManagerAccess.getInstance().getTaskInstanceUrl(getTaskInstance(taskInstanceId));
    }


    private TaskInstanceVO createTaskInstance0(Long taskId, String owner, Long ownerId,
            String secondaryOwner, Long secondaryOwnerId, String message,
            String pOwnerAppName, Long pAssTaskInstId, String label, Date dueDate, String masterRequestId, int priority) //AK..added priority argument
    throws TaskException, DataAccessException {

        TaskInstanceVO ti = new TaskInstanceVO();
        ti.setTaskId(taskId);
        ti.setOwnerType(owner);
        ti.setOwnerId(ownerId);
        ti.setSecondaryOwnerType(secondaryOwner);
        ti.setSecondaryOwnerId(secondaryOwnerId);
        ti.setStatusCode(TaskStatus.STATUS_OPEN);
        ti.setStateCode(TaskState.STATE_OPEN);
        ti.setComments(message);
        ti.setOwnerApplicationName(pOwnerAppName);
        ti.setAssociatedTaskInstanceId(pAssTaskInstId);
        ti.setTaskName(label);
        ti.setMasterRequestId(masterRequestId);
        ti.setPriority(priority); //AK..changed from ti.setPriority(0);
        ti.setDueDate(dueDate);
        Long id = getTaskDAO().createTaskInstance(ti, dueDate);
        ti.setTaskInstanceId(id);
        return ti;
    }

    /**
     * Convenience method for below.
     */
    public TaskInstanceVO createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId)
    throws TaskException, DataAccessException {
        return createTaskInstance(taskId, masterRequestId, procInstId, secOwner, secOwnerId, null);
    }

    /**
     * Convenience method for below.
     */
    public TaskInstanceVO createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId, Map<String,String> indices)
    throws TaskException, DataAccessException {
        return createTaskInstance(taskId, procInstId, secOwner, secOwnerId, null, null, null, null, 0, indices, null, masterRequestId);
    }

    /**
     * Creates a task instance. This is the main version. There is another version
     * for creating independent task instance directly from task manager,
     * and a third version for creating detail-only task instances.
     *
     * The method does the following:
     * - create task instance entry in database
     * - create SLA if passed in or specified in template
     * - create groups for new TEMPLATE based tasks
     * - create indices for new TEMPLATE based general tasks
     * - send notification if specified in template
     * - invoke old-style observer if specified in template
     * - auto-assign if specified in template
     * - record in audit log
     *
     * @param taskId    task template ID
     * @param procInstId    process instance ID
     * @param secondaryOwner    can be DOCUMENT (general task), TASK_INSTANCE (subtask) or WORK_TRANSITION_INSTANCE (for classic task)
     * @param secondaryOwnerId  document ID or transition instance ID
     * @param comment   return message (typically stacktrace for fallout tasks) of the activity for classic task
     * @param ownerApp  owner system MAL, optionally followed by ":" and engine URL.
     *                  The field is only populated when the task manager is remote
     * @param asgnTaskInstId when the task manager is remote and for summary only, this field
     *                  is populated with the task instance ID of the local task manager
     * @param taskName  task name. Taken from task template when not populated (for VCS assets is task template label)
     * @param dueInSeconds  SLA. When it is 0, check if template has specified SLA
     * @param indices   indices for general task based on templates
     * @param assignee  assignee CUID if this is to be auto-assigned by process variable
     * @return TaskInstance
     */
    public TaskInstanceVO createTaskInstance(Long taskId, Long procInstId,
                String secondaryOwner, Long secondaryOwnerId, String comment,
                String ownerApp, Long asgnTaskInstId, String taskName, int dueInSeconds,
                Map<String,String> indices, String assignee, String masterRequestId)
        throws TaskException, DataAccessException {
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskId);
        String label = task.getLabel();
        PackageVO taskPkg = PackageVOCache.getTaskTemplatePackage(taskId);
        if (taskPkg != null && !taskPkg.isDefaultPackage())
            label = taskPkg.getLabel() + "/" + label;
        Date dueDate = this.findDueDate(dueInSeconds, task);
        int pri = 0;
        if (task.isTemplate()) {
            // use the prioritization strategy if one is defined for the task
            try {
                PrioritizationStrategy prioritizationStrategy = getPrioritizationStrategy(task, procInstId, OwnerType.DOCUMENT.equals(secondaryOwner) ? secondaryOwnerId : null,indices);
                if (prioritizationStrategy != null) {
                    Date calcDueDate = prioritizationStrategy.determineDueDate(task);
                    if (calcDueDate != null)
                        dueDate = calcDueDate;
                    pri = prioritizationStrategy.determinePriority(task, dueDate);
                }
            }
            catch (Exception ex) {
                throw new TaskException(ex.getMessage(), ex);
            }
        }

        TaskInstanceVO ti = createTaskInstance0(taskId, (procInstId == null || procInstId == 0) ? OwnerType.EXTERNAL : OwnerType.PROCESS_INSTANCE,
                procInstId, secondaryOwner, secondaryOwnerId, comment,
                ownerApp, asgnTaskInstId, label, dueDate, masterRequestId, pri);
        ti.setTaskName(task.getTaskName()); // Reset task name back (without package name pre-pended)
        if (dueDate!=null) {
            int alertInterval = 0; //initialize
            String alertIntervalString = ""; //initialize

            alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            scheduleTaskSlaEvent(ti.getTaskInstanceId(), dueDate, alertInterval, false);
        }

        // create instance indices for template based general tasks (MDW 5.1) and all template based tasks (MDW 5.2)
        if (task.isUsingIndices()) {
            if (indices!=null && !indices.isEmpty()) {
                getTaskDAO().setTaskInstanceIndices(ti.getTaskInstanceId(), indices);
            }
        }
        // create instance groups for template based tasks
        if (task.isTemplate()) {
            List<String> groups = determineWorkgroups(task, ti, indices);
            if (groups != null && groups.size() >0) {
                getTaskDAO().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));
                ti.setWorkgroups(groups);
            }
        }

        if (assignee!=null) {
            try
            {
               UserVO user =  UserGroupCache.getUser(assignee);
               if(user == null)
                 throw new TaskException("Assignee user not found to perform auto-assign : " + assignee);
               assignTaskInstance(ti, user.getId()); // Performing auto assign on summary
            }
            catch (CachingException e)
            {
              logger.severeException(e.getMessage(), e);
            }
        }

        this.notifyTaskAction(ti, TaskAction.CREATE, null, null);   // notification/observer/auto-assign
        this.auditLogActionPerformed("Create", "MDW", Entity.TaskInstance, ti.getTaskInstanceId(),
                null, TaskTemplateCache.getTaskTemplate(taskId).getTaskName());

        return ti;
    }

    /**
     * This version is used to create detail-only task instance where
     * the summary instance is hosted in remote task manager.
     * The method only creates task instance entry in database.
     * It does *not* create SLA, groups, indices, notifications, observers,
     * auto-assignment, or audit log.
     *
     * @param taskId    task template ID
     * @param procInstId    process instance ID
     * @param secondaryOwner    can be DOCUMENT (general task) or WORK_TRANSITION_INSTANCE (for classic task)
     * @param secondaryOwnerId  document ID or transition instance ID
     * @param message   return message (typically stacktrace for fallout tasks) of the activity for classic task
     * @param taskName  task name. Taken from task template when not populated
     * @return TaskInstance
     *
     */
    public TaskInstanceVO createTaskInstance(Long taskId, Long procInstId,
            String secondaryOwner, Long secondaryOwnerId, String message, String taskName, String masterRequestId)
        throws TaskException, DataAccessException {
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskId);
        int pri = 0; //AK added
        return createTaskInstance0(taskId, OwnerType.PROCESS_INSTANCE, procInstId,
                secondaryOwner, secondaryOwnerId, message,
                null, null, taskName!=null?taskName:task.getTaskName(), null, masterRequestId, pri); //AK..added pri argument
        // detail only copy does not store due date, as it will not raise jeopardy here
    }

    /**
     * This version is used by the task manager to create a task instance
     * not associated with a process instance.
     *
     * @param taskId
     * @param masterOwnerId
     * @param comment optional
     * @param dueDate optional
     * @param userId
     * @param documentId secondary owner, optional
     * @return TaskInstance
     */
    public TaskInstanceVO createTaskInstance(Long taskId, String masterOwnerId,
            String comment, Date dueDate, Long userId, Long documentId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.createTaskInstance()", true);
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskId);
        int pri = 0; //AK added
        TaskInstanceVO ti = createTaskInstance0(taskId,  OwnerType.USER, userId,
                (documentId!=null?OwnerType.DOCUMENT:null), documentId, comment,
                null, null, task.getTaskName(), dueDate, masterOwnerId, pri); //AK added pri
        if (dueDate!=null) {
            String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            scheduleTaskSlaEvent(ti.getTaskInstanceId(), dueDate, alertInterval, false);
        }
        // create instance indices for template based general tasks
        if (task.isUsingIndices()) {
//          if (masterOwnerId!=null) {
//              Map<String,String> indices = new HashMap<String,String>();
//              indices.put(FormDataDocument.META_MASTER_REQUEST_ID, masterOwnerId);
//              setTaskInstanceIndices(ti.getTaskInstanceId(), indices);
//          }
        }
        // create instance groups for template based tasks
        if (task.isTemplate()) {
            List<String> groups = determineWorkgroups(task, ti, null);
            if (groups != null && groups.size() >0)
                getTaskDAO().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));
        }
        this.notifyTaskAction(ti, TaskAction.CREATE, null, null);   // notification/observer/auto-assign
        this.auditLogActionPerformed("Create", "MDW", Entity.TaskInstance, ti.getTaskInstanceId(),
                null, TaskTemplateCache.getTaskTemplate(taskId).getTaskName());
        timer.stopAndLogTiming("");
        return ti;
    }

    public TaskInstanceVO createSubTaskInstance(Long masterTaskInstanceId, String subTaskName)
    throws TaskException, DataAccessException {
        TaskInstanceVO masterTask = getTaskInstance(masterTaskInstanceId);

        String masterRequestId = masterTask.getMasterRequestId();
        TaskVO subTaskVo = TaskTemplateCache.getTaskTemplate(subTaskName);
        if (subTaskVo == null)
            throw new TaskException("SubTask not found: '" + subTaskName + "'");
        Long processInstanceId = masterTask.getOwnerId();

        return createTaskInstance(subTaskVo.getTaskId(), masterRequestId, processInstanceId,
                 OwnerType.TASK_INSTANCE, masterTaskInstanceId, null);
    }

    private Date findDueDate(int dueInSeconds, TaskVO task)
    throws DataAccessException {
        Date dueDate;
        if (dueInSeconds<=0) dueInSeconds = task.getSlaSeconds();
        if (dueInSeconds>0) {
            long now = DatabaseAccess.getCurrentTime();
            // Need to create a long value
            // since adding a long to an int caused wrong dates
            // to be created
            long dueInSecondsLong =new Long(dueInSeconds).longValue();
            dueDate = new Date(now+dueInSecondsLong*1000);
        } else dueDate = null;
        return dueDate;
    }

    private void scheduleTaskSlaEvent(Long taskInstId, Date dueDate,
            int alertInterval, boolean isReschedule) {
        ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
        String eventName = ScheduledEvent.SPECIAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstId.toString();
        boolean isAlert;
        Date eventDate;
        if (alertInterval>0) {
            long eventTime = dueDate.getTime()-alertInterval*1000;
            if (eventTime<DatabaseAccess.getCurrentTime()) {
                eventDate = dueDate;
                isAlert = false;
            } else {
                eventDate = new Date(eventTime);
                isAlert = true;
            }
        } else {
            isAlert = false;
            eventDate = dueDate;
        }
        String message = "<_mdw_task_sla><task_instance_id>" + taskInstId.toString()
            + "</task_instance_id><is_alert>"
            + (isAlert?"true":"false") + "</is_alert></_mdw_task_sla>";
        if (isReschedule) {
            queue.rescheduleExternalEvent(eventName, eventDate, message);
        }
        else {
            queue.scheduleExternalEvent(eventName, eventDate, message, null);
        }
    }

    private void unscheduleTaskSlaEvent(Long taskInstId) throws TaskException {
        ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
        String eventName = ScheduledEvent.SPECIAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstId.toString();
        String backwardCompatibleEventName = ScheduledEvent.EXTERNAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstId.toString();
        try {
            queue.unscheduleEvent(eventName);   // this broadcasts
            queue.unscheduleEvent(backwardCompatibleEventName); // this broadcasts
        } catch (Exception e) {
            throw new TaskException("Failed to unschedule task SLA", e);
        }
    }

    /**
     * Returns the available task statuses from reference data.
     *
     * @return the statuses
     */
    public Collection<TaskStatus> getTaskStatuses()
    throws DataAccessException {
        return getTaskDAO().getAllTaskStatuses();
    }

    /**
     * Returns the available task statuses from reference data.
     *
     * @return the statuses
     */
    public Collection<TaskState> getTaskStates()
    throws DataAccessException {
        return getTaskDAO().getAllTaskStates();
    }

    /**
     * Updates the appropriate taskInstanceData based on the passed info.
     *
     * @param pVarInstId
     * @param pName
     * @param pValue
     */
    public void updateTaskInstanceData(VariableInstanceVO variableInstanceVO, Serializable value, Long userId)
    throws TaskException, DataAccessException {

        CodeTimer timer = new CodeTimer("TaskManager.updateTaskInstanceData()", true);

        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            if (variableInstanceVO.getData() instanceof DocumentReference) {
                DocumentReference docref = (DocumentReference) variableInstanceVO.getData();
                eventManager.updateDocumentContent(docref.getDocumentId(), value, variableInstanceVO.getType());
            }
            else {
                eventManager.updateVariableInstance(variableInstanceVO.getInstanceId(), value);
            }
            auditLogActionPerformed(UserActionVO.Action.Change.toString(), userId, Entity.VariableInstance, variableInstanceVO.getInstanceId(), null, variableInstanceVO.getName());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new TaskException(ex.getMessage(), ex);
        }
        timer.stopAndLogTiming("");
    }

    /**
     * Returns the available notes for the given id
     *
     * @param instanceId
     * @return Notes
     */
    public Collection<InstanceNote> getNotes(String owner, Long id)
    throws DataAccessException {
        return getTaskDAO().getInstanceNotes(owner, id);
    }

    /**
     * Makes a user the assignee for a task instance.
     *
     * @param ti the task instance
     * @param userId the assignee
     * @return the updated task instance
     */
    private void assignTaskInstance(TaskInstanceVO ti, Long userId)
    throws TaskException, DataAccessException {
        if (this.isTaskInstanceAssignable(ti)) {
            ti.setStatusCode(TaskStatus.STATUS_ASSIGNED);
            ti.setTaskClaimUserId(userId);
            Map<String,Object> changes = new HashMap<String,Object>();
            changes.put("TASK_CLAIM_USER_ID", userId);
            changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_ASSIGNED);
            getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);

            // update process variable with assignee - only for local tasks
            TaskVO taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
            if (taskVO != null) {
                String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
                if (ti.isLocal()&&!StringHelper.isEmpty(assigneeVarSpec)) {
                    try {
                        String cuid = UserGroupCache.getUser(userId).getCuid();
                        if (cuid == null)
                            throw new DataAccessException("User not found for id: " + userId);
                        TaskRuntimeContext runtimeContext = getTaskRuntimeContext(ti);
                        String prevCuid;
                        if (runtimeContext.isExpression(assigneeVarSpec))
                            prevCuid = runtimeContext.evaluateToString(assigneeVarSpec);
                        else {
                            Object varVal = runtimeContext.getVariables().get(assigneeVarSpec);
                            prevCuid = varVal == null ? null : varVal.toString();
                        }

                        if (!cuid.equals(prevCuid)) {
                            // need to update variable to match new assignee
                            EventManager eventMgr = ServiceLocator.getEventManager();
                            if (runtimeContext.isExpression(assigneeVarSpec)) {
                                // create or update document variable referenced by expression
                                runtimeContext.set(assigneeVarSpec, cuid);
                                String rootVar = assigneeVarSpec.substring(2, assigneeVarSpec.indexOf('.'));
                                VariableVO doc = runtimeContext.getProcess().getVariable(rootVar);
                                VariableInstanceInfo varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                                String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getVariableType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                                if (varInst == null) {
                                    Long procInstId = runtimeContext.getProcessInstanceId();
                                    Long docId = eventMgr.createDocument(doc.getVariableType(), procInstId, OwnerType.PROCESS_INSTANCE, procInstId, null, null, stringValue);
                                    eventMgr.setVariableInstance(procInstId, rootVar, new DocumentReference(docId));
                                }
                                else {
                                    DocumentReference docRef = (DocumentReference) varInst.getData();
                                    eventMgr.updateDocumentContent(docRef.getDocumentId(), stringValue, doc.getVariableType());
                                }
                            }
                            else {
                                eventMgr.setVariableInstance(ti.getOwnerId(), assigneeVarSpec, cuid);
                            }
                        }
                    }
                    catch (Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }
            }
        }
        else {
            logger.warn("TaskInstance is not assignable.  ID = " + ti.getTaskInstanceId());
        }
    }

    /**
     * Releases the task instance from being assigned
     * @param ti task instance
     * @return the updated task instance
     */
    private void releaseTaskInstance(TaskInstanceVO ti)
    throws DataAccessException {
        ti.setStatusCode(TaskStatus.STATUS_OPEN);
        ti.setTaskClaimUserId(null);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_CLAIM_USER_ID", null);
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_OPEN);
        getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);

        // clear assignee process variable value - only when task is local
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
        if (taskVO != null) { // not for invalid tasks
            String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
            if (ti.isLocal() && !StringHelper.isEmpty(assigneeVarSpec)) {
                try {
                    TaskRuntimeContext runtimeContext = getTaskRuntimeContext(ti);
                    EventManager eventMgr = ServiceLocator.getEventManager();
                    if (runtimeContext.isExpression(assigneeVarSpec)) {
                        // create or update document variable referenced by expression
                        runtimeContext.set(assigneeVarSpec, null);
                        String rootVar = assigneeVarSpec.substring(2, assigneeVarSpec.indexOf('.'));
                        VariableVO doc = runtimeContext.getProcess().getVariable(rootVar);
                        VariableInstanceInfo varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                        String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getVariableType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                        if (varInst == null) {
                            Long procInstId = runtimeContext.getProcessInstanceId();
                            Long docId = eventMgr.createDocument(doc.getVariableType(), procInstId, OwnerType.PROCESS_INSTANCE, procInstId, null, null, stringValue);
                            eventMgr.setVariableInstance(procInstId, rootVar, new DocumentReference(docId));
                        }
                        else {
                            DocumentReference docRef = (DocumentReference) varInst.getData();
                            eventMgr.updateDocumentContent(docRef.getDocumentId(), stringValue, doc.getVariableType());
                        }
                    }
                    else {
                        eventMgr.setVariableInstance(ti.getOwnerId(), assigneeVarSpec, null);
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Changes the task instance status to "In Progress"
     * @param ti task instance
     * @return the updated task instance
     */
    private void workTaskInstance(TaskInstanceVO ti)
    throws DataAccessException {
        ti.setStatusCode(TaskStatus.STATUS_IN_PROGRESS);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_IN_PROGRESS);
        getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);
    }

    /**
     * Changes the task instance status to "Failed"
     * @param ti task instance
     * @return the updated task instance
     */
//    private void abortTaskInstance(TaskInstanceVO ti)
//    throws TaskException, DataAccessException {
//        updateTaskInstance(ti.getTaskInstanceId(), TaskStatus.STATUS_FAILED, TaskState.STATE_CLOSED, "Task aborted");
//        try {
//            ProcessInstanceVO procInst = processManager.getProcessInstance(ti.getOwnerId());
//            ProcessVO processVO = ProcessVOCache.getProcessVO(procInst.getProcessId());
//            if (processVO.isEmbeddedExceptionHandler()) {
//                procInst = processManager.getProcessInstance(procInst.getOwnerId());
//            }
//            if (procInst == null) {
//                throw new TaskException("Failed to locate the ProcessInstance for TaskInstanceId:" + ti.getTaskInstanceId());
//            }
//            processManager.updateProcessInstanceStatus(procInst.getId(),WorkStatus.STATUS_FAILED);
//
//            // task instance secondary owner is work transition instance
//            Long workTransInstId = ti.getSecondaryOwnerId();
//            WorkTransitionInstanceVO workTransInst = processManager.getWorkTransitionInstance(workTransInstId);
//            Long activityInstanceId = workTransInst.getDestinationID();
//            if (activityInstanceId != null)
//                processManager.updateActivityInstanceStatus(activityInstanceId, WorkStatus.STATUS_FAILED, "Manually aborted.");
//        }
//        catch (Exception ex) {
//            logger.severeException(ex.getMessage(), ex);
//            throw new TaskException(-1, ex.getMessage(), ex);
//        }
//    }

    /**
     * Creates and returns the list of tasks associated with an order
     *
     * @param pOwnerId
     * @return Collection
     */
    public TaskInstanceVO[] getTaskInstanceVOsForMasterOwner(String pMasterOwnerId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getTaskInstanceVOsForMasterOwner()", true);
        List<TaskInstanceVO> daoResults = getTaskDAO().queryTaskInstances(pMasterOwnerId);
        timer.stopAndLogTiming("");
        return daoResults.toArray(new TaskInstanceVO[daoResults.size()]);
    }

    /**
     * Returns the list of task instances associated with the proces instance
     *
     * @param pProcessInstanceId
     * @return Array of TaskInstance
     */
    private TaskInstanceVO[] getAllTaskInstancesForProcessInstance(Long pProcessInstanceId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getAllTaskInstancesForProcessInstance()", true);
        List<TaskInstanceVO> daoResults = this.getTaskDAO()
                .getTaskInstancesForProcessInstance(pProcessInstanceId);
        timer.stopAndLogTiming("");
        return daoResults.toArray(new TaskInstanceVO[daoResults.size()]);
    }

    /**
     * Checks if the Task Instance is open
     *
     * @param pTaskInstance
     * @return boolean status
     * @throws TaskException
     * @throws DataAccessException
     */
    private boolean isTaskInstanceOpen(TaskInstanceVO pTaskInstance)
    throws TaskException, DataAccessException {
        if (pTaskInstance.getStatusCode().intValue() == TaskStatus.STATUS_COMPLETED.intValue()) {
            return false;
        }
        if (pTaskInstance.getStatusCode().intValue() == TaskStatus.STATUS_CANCELLED.intValue()) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the Task Instance is Assignable
     *
     * @param pTaskInstance
     * @return boolean status
     */
    private boolean isTaskInstanceAssignable(TaskInstanceVO pTaskInstance)
    throws TaskException, DataAccessException {
        return isTaskInstanceOpen(pTaskInstance);
    }

    public void cancelTaskInstance(TaskInstanceVO taskInst)
    throws TaskException, DataAccessException {
        if (isInFinalStatus(taskInst)) {
            logger.info("Cannot change the state of the TaskInstance to Cancel.");
            return;
        }
        Integer prevStatus = taskInst.getStatusCode();
        Integer prevState = taskInst.getStateCode();
        taskInst.setStateCode(TaskState.STATE_CLOSED);
        taskInst.setStatusCode(TaskStatus.STATUS_CANCELLED);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_CANCELLED);
        changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
        getTaskDAO().updateTaskInstance(taskInst.getTaskInstanceId(), changes, true);
        notifyTaskAction(taskInst, TaskAction.CANCEL, prevStatus, prevState);
    }

    /**
     * Cancels the taskInstances for the process instance
     *
     * @param pProcessInstance
     */
    public void cancelTaskInstancesForProcessInstance(Long pProcessInstId, String ownerApplName)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.cancelTaskInstancesForProcessInstance()", true);
        if (ownerApplName==null || ownerApplName.equals(TaskInstanceVO.DETAILONLY)) ownerApplName = "";
        else {
            int k = ownerApplName.indexOf('@');
            if (k>0) ownerApplName = ownerApplName.substring(0,k);
        }
        TaskInstanceVO[] instances = this.getAllTaskInstancesForProcessInstance(pProcessInstId);
        if (instances == null || instances.length == 0) {
            timer.stopAndLogTiming("NoTaskInstances");
            return;
        }
        for (int i = 0; i < instances.length; i++) {
            instances[i].setComments("Task has been cancelled by ProcessInstance.");
            String owner = instances[i].getOwnerApplicationName();
            if (owner==null || owner.equals(TaskInstanceVO.DETAILONLY)) owner = "";
            else {
                int k = owner.indexOf('@');
                if (k>0) owner = owner.substring(0,k);
            }
            if (!owner.equals(ownerApplName))
                continue;
            String instantStatus = instances[i].getStatus();
            if (instantStatus == null && ApplicationContext.isFileBasedAssetPersist()) {
                TaskStatus taskStatus = DataAccess.getBaselineData().getTaskStatuses().get(instances[i].getStatusCode());
                if (taskStatus != null)
                    instantStatus = taskStatus.getDescription();
            }
            if (!isInFinalStatus(instances[i])) {
                Integer prevStatus = instances[i].getStatusCode();
                Integer prevState = instances[i].getStateCode();

                instances[i].setStateCode(TaskState.STATE_CLOSED);
                instances[i].setStatusCode(TaskStatus.STATUS_CANCELLED);
                Map<String,Object> changes = new HashMap<String,Object>();
                changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_CANCELLED);
                changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
                getTaskDAO().updateTaskInstance(instances[i].getTaskInstanceId(), changes, true);
                notifyTaskAction(instances[i], TaskAction.CANCEL, prevStatus, prevState);
            }
        }
        timer.stopAndLogTiming("");
    }

    /**
     * Checks whether a Task Instance is in a final status
     *
     * @param statusCode
     * @return boolean
     */

    public boolean isInFinalStatus(TaskInstanceVO pTaskInstance) {
        if (pTaskInstance.getStatusCode().intValue() == TaskStatus.STATUS_COMPLETED.intValue()) {
            pTaskInstance.setInFinalStatus(true);
            return true;
        }
        if (pTaskInstance.getStatusCode().intValue() == TaskStatus.STATUS_CANCELLED.intValue()) {
            pTaskInstance.setInFinalStatus(true);
            return true;
        }
        if (finalStatuses == null){
            String finalTaskStatusesString = PropertyUtil.getInstance().getPropertyManager().getStringProperty(PropertyNames.FINAL_TASK_STATUSES);
            finalStatuses = StringArrayHelper.covertToArray(finalTaskStatusesString, ",");
        }
        if (finalStatuses != null && finalStatuses.length > 0) {
            if (Arrays.asList(finalStatuses).indexOf(pTaskInstance.getStatusCode().toString()) >= 0) {
                pTaskInstance.setInFinalStatus(true);
                return true;
            }
        }
        pTaskInstance.setInFinalStatus(false);
        return false;
    }
    /**
     * Method that checks whether a task instance was created by an
     * embedded exception handler subprocess
     *
     * @param taskInstance
     * @return true if exceptionHandler created
     */
    private boolean isExceptionHandlerTaskInstance(TaskInstanceVO taskInstance) {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            ProcessInstanceVO pi = eventManager.getProcessInstance(taskInstance.getOwnerId());
            if (pi.isNewEmbedded()) return true;
            ProcessVO processVO = ProcessVOCache.getProcessVO(pi.getProcessId());
            return processVO.isEmbeddedExceptionHandler();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Forwards the Task Instance
     *
     * @param taskInst the task instance
     * @param destination the logical destination = the new workgroup name
     */
    private void forwardTaskInstance(TaskInstanceVO taskInst, String destination, String comment)
    throws TaskException, DataAccessException {
        List<String> prevWorkgroups = taskInst.getWorkgroups();
        if (prevWorkgroups == null || prevWorkgroups.isEmpty()) {
            TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
            prevWorkgroups = taskVO.getWorkgroups();
        }

        // change the task instance to be associated with the specified group
        releaseTaskInstance(taskInst);

        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_OPEN);
        changes.put("TASK_CLAIM_USER_ID", null);
        if (comment!=null) changes.put("COMMENTS", comment);
        getTaskDAO().updateTaskInstance(taskInst.getTaskInstanceId(), changes, false);
        String[] destWorkgroups = destination.split(",");
        try {
            for(String groupName:destWorkgroups) {
                if (UserGroupCache.getWorkgroup(groupName) == null) {
                    throw new TaskException( "Invalid Workgroup: " + groupName);
                }
            }
        } catch (CachingException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        getTaskDAO().setTaskInstanceGroups(taskInst.getTaskInstanceId(),destWorkgroups );
        taskInst.setWorkgroups(Arrays.asList(destWorkgroups));

        try {
            // new-style notifiers (registered on destination task)
            (new EngineAccess()).sendNotification(taskInst, TaskAction.FORWARD, TaskAction.FORWARD);

            AutoAssignStrategy strategy = getAutoAssignStrategy(taskInst);
            if (strategy != null) {
                UserVO assignee = strategy.selectAssignee(taskInst);
                if (assignee == null)
                    logger.severe("No users found for auto-assignment of task instance ID: " + taskInst.getTaskInstanceId());
                else
                    performActionOnTaskInstance(TaskAction.ASSIGN, taskInst.getTaskInstanceId(), assignee.getId(), assignee.getId(), "Auto-assigned", null, false, false);
            }
        }
        catch (ObserverException ex) {
            // do not rethrow; observer problems should not prevent task actions
            logger.severeException(ex.getMessage(), ex);
        }
        catch (StrategyException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    public TaskInstanceVO performActionOnTaskInstance(String action, Long taskInstanceId,
            Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine)
        throws TaskException, DataAccessException {
        return performActionOnTaskInstance(action, taskInstanceId, userId, assigneeId, comment, destination, notifyEngine, true);
    }

    public TaskInstanceVO performActionOnTaskInstance(String action, Long taskInstanceId,
            Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine, boolean allowResumeEndpoint)
        throws TaskException, DataAccessException {

        if (logger.isInfoEnabled())
            logger.info("task action '" + action + "' on instance " + taskInstanceId);
        TaskInstanceVO ti = getTaskDAO().getTaskInstanceAllInfo(taskInstanceId);
        // verifyPermission(ti, action, userId);
        Integer prevStatus = ti.getStatusCode();
        Integer prevState = ti.getStateCode();
        String assigneeCuid = null;

        boolean isComplete = false;

        // special behavior for some types of actions
        if (action.equalsIgnoreCase(TaskAction.ASSIGN) || action.equalsIgnoreCase(TaskAction.CLAIM)) {
            if (assigneeId == null || assigneeId.longValue() == 0) {
                assigneeId = userId;
            }
            assignTaskInstance(ti, assigneeId);
            try {
                assigneeCuid = UserGroupCache.getUser(assigneeId).getCuid();

            }
            catch (CachingException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        else if (action.equalsIgnoreCase(TaskAction.RELEASE)) {
            releaseTaskInstance(ti);
        }
        else if (action.equalsIgnoreCase(TaskAction.WORK)) {
            workTaskInstance(ti);
        }
//        else if (action.equalsIgnoreCase(TaskAction.ABORT)) {
        // now handled by ProcessControllerMDBHelper - by falling through the else case
//            abortTaskInstance(ti);
//        }
        else if (action.equalsIgnoreCase(TaskAction.FORWARD)) {
            forwardTaskInstance(ti, destination, comment);
            auditLogActionPerformed(action, userId, Entity.TaskInstance, taskInstanceId, destination, TaskTemplateCache.getTaskTemplate(ti.getTaskId()).getTaskName());
            return ti;  // forward notifications are handled in forwardTaskInstance()
        }
        else {
            isComplete = true;
            TaskRuntimeContext runtimeContext = getTaskRuntimeContext(ti);
            // update the indexes
            setIndexes(runtimeContext);
            // option to notify through service (eg: to offload to workflow server instance)
            String taskResumeEndpoint = null;
            if (ti.isProcessOwned() && allowResumeEndpoint)
                taskResumeEndpoint = runtimeContext.getProperty(PropertyNames.TASK_RESUME_NOTIFY_ENDPOINT);
            if (taskResumeEndpoint == null) // otherwise it will be closed via service
                closeTaskInstance(ti, action, comment);
            if (notifyEngine && !ti.isSubTask()) {
                if (taskResumeEndpoint == null) {
                    // resume through engine
                    if (ti.isGeneralTask())
                        resumeAutoFormTaskInstance(action, ti);
                    else
                        resumeCustomTaskInstance(action, ti);
                }
                else {
                    // resume through service
                    resumeThroughService(taskResumeEndpoint, taskInstanceId, action, userId, comment);
                    return ti; // notify and audit log at endpoint destination
                }
            }
        }

        notifyTaskAction(ti, action, prevStatus, prevState);
        String label = ti.getTaskName();
        if (label == null) {
            label = "Unknown";
            TaskVO taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
            if (taskVO != null)
                label = taskVO.getTaskName();
        }
        auditLogActionPerformed(action, userId, Entity.TaskInstance, taskInstanceId, assigneeCuid, label);

        if (isComplete) {
            // in case subTask
            if (ti.isSubTask()) {
                Long masterTaskInstId = ti.getMasterTaskInstanceId();
                boolean allCompleted = true;
                for (TaskInstanceVO subTask : getSubTaskInstances(masterTaskInstId)) {
                    if (!isInFinalStatus(subTask)) {
                        allCompleted = false;
                        break;
                    }
                }
                if (allCompleted) {
                    TaskInstanceVO masterTaskInst = getTaskInstance(masterTaskInstId);
                    TaskVO taskVO = TaskTemplateCache.getTaskTemplate(masterTaskInst.getTaskId());
                    if ("true".equalsIgnoreCase(taskVO.getAttribute(TaskAttributeConstant.SUBTASKS_COMPLETE_MASTER)))
                        performActionOnTaskInstance(TaskAction.COMPLETE, masterTaskInstId, userId, null, null, null, notifyEngine, false);
                }
            }

            // in case master task
            for (TaskInstanceVO subTask : getSubTaskInstances(ti.getTaskInstanceId())) {
                if (!isInFinalStatus(subTask))
                    cancelTaskInstance(subTask);
            }
        }


        return ti;
    }

    public List<SubTask> getSubTaskList(TaskRuntimeContext runtimeContext) throws TaskException {
        TaskVO task = TaskTemplateCache.getTaskTemplate(runtimeContext.getTaskId());
        if (task.isTemplate()) {
            SubTaskPlan subTaskPlan = getSubTaskPlan(runtimeContext);
            if (subTaskPlan != null) {
                return subTaskPlan.getSubTaskList();
            }
        }
        return null;
    }

    public List<TaskInstanceVO> getSubTaskInstances(Long masterTaskInstanceId) throws DataAccessException {
        return getTaskDAO().getSubTaskInstances(masterTaskInstanceId);
    }

    private void resumeAutoFormTaskInstance(String taskAction, TaskInstanceVO ti) throws TaskException {
        try {
            String eventName = FormConstants.TASK_CORRELATION_ID_PREFIX + ti.getTaskInstanceId().toString();
            FormDataDocument datadoc = new FormDataDocument();
            String formAction;
            if (taskAction.equals(TaskAction.CANCEL))
                formAction = FormConstants.ACTION_CANCEL_TASK;
            else if (taskAction.equals(TaskAction.COMPLETE))
                formAction = FormConstants.ACTION_COMPLETE_TASK;
            else {
                formAction = FormConstants.ACTION_COMPLETE_TASK;
                datadoc.setMetaValue(FormConstants.URLARG_COMPLETION_CODE, taskAction);
            }
            datadoc.setAttribute(FormDataDocument.ATTR_ACTION, formAction);
            String message = datadoc.format();
            EventManager eventManager = ServiceLocator.getEventManager();
            Long docid = eventManager.createDocument(FormDataDocument.class.getName(),
                    ti.getOwnerId(), OwnerType.TASK_INSTANCE, ti.getTaskInstanceId(), null, null, message);
            String av = PropertyManager.getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
            int delay = 2;
            if (av!=null) {
                // delay some seconds to avoid race condition
                try {
                    delay = Integer.parseInt(av);
                    if (delay<0) delay = 0;
                    else if (delay>300) delay = 300;
                } catch (Exception e) {
                }
            }
            notifyProcess(ti, eventName, docid, message, delay);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new TaskException(ex.getMessage(), ex);
        }
    }

    private void resumeCustomTaskInstance(String action, TaskInstanceVO ti) throws TaskException {
        try {
            Long actInstId = getActivityInstanceId(ti, false);

            String correlationId = "TaskAction-" + actInstId.toString();
            ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
            ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
            Action actionItem = actionRequest.addNewAction();
            actionItem.setName("TaskAction");
            Parameter param = actionItem.addNewParameter();
            param.setName("Action");
            param.setStringValue(action);

            String message = actionRequestDoc.xmlText();
            EventManager eventManager = ServiceLocator.getEventManager();
            Long docid = eventManager.createDocument(XmlObject.class.getName(), ti.getOwnerId(),
                    OwnerType.TASK_INSTANCE, ti.getTaskInstanceId(), null, null, message);
            int delay = 2;
            String av = PropertyManager.getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
            if (av!=null) {
                try {
                    delay = Integer.parseInt(av);
                    if (delay<0) delay = 0;
                    else if (delay>300) delay = 300;
                } catch (Exception e) {
                    logger.warn("activity resume delay spec is not an integer");
                }
            }
            notifyProcess(ti, correlationId, docid, message, delay);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new TaskException(ex.getMessage(), ex);
        }
    }

    private void notifyProcess(TaskInstanceVO taskInstance, String eventName, Long eventInstId,
            String message, int delay) throws DataAccessException, EventException {
        EventManager eventManager = ServiceLocator.getEventManager();
        eventManager.notifyProcess(eventName, eventInstId, message, delay);
    }

    public void resumeThroughService(String taskResumeEndpoint, Long taskInstanceId, String action, Long userId, String comment)
        throws TaskException {
        TaskActionVO taskAction = new TaskActionVO();
        taskAction.setTaskInstanceId(taskInstanceId);
        taskAction.setAction(action);
        try {
            taskAction.setUser(UserGroupCache.getUser(userId).getCuid());
            taskAction.setComment(comment);
            // Send request to new endpoint, while preventing infinte loop with new Query parameter
            HttpHelper helper = new HttpHelper(new URL(taskResumeEndpoint + "/Services/Tasks/" + taskInstanceId + "/" + action + "?disableEndpoint=true"));
            String response = helper.post(taskAction.getJson().toString(2));
            StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
            if (statusMessage.getCode() != 0)
                throw new ServiceException("Failure response resuming task instance " + taskInstanceId + " at " +
                       taskResumeEndpoint + ": " + statusMessage.getMessage());
        }
        catch (Exception ex) {
            throw new TaskException("Failed to resume task instance: " + taskInstanceId, ex);
        }
    }

    public void closeTaskInstance(TaskInstanceVO ti, String action, String comment)
            throws TaskException, DataAccessException {
        // set the new task instance status appropriately
        Integer newStatus = TaskStatus.STATUS_COMPLETED;
        if (action.equals(TaskAction.CANCEL) || action.equals(TaskAction.ABORT) || action.startsWith(TaskAction.CANCEL + "::"))
            newStatus = TaskStatus.STATUS_CANCELLED;
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", newStatus);
        changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
        if (comment != null) changes.put("COMMENTS", comment);
        getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, true);
        ti.setStatusCode(newStatus);
    }


    private void auditLogActionPerformed(String action, Long userId, Entity entity, Long entityId, String destination, String comments)
    throws TaskException, DataAccessException {
        String userCuid = null;
        if (userId != null) {
            UserDAO uda = new UserDAO(new DatabaseAccess(null));
            UserVO user = uda.getUser(userId);
            userCuid = user.getCuid();
        }

        auditLogActionPerformed(action, userCuid, entity, entityId, destination, comments);
    }

    private void auditLogActionPerformed(String action, String user, Entity entity, Long entityId, String destination, String comments)
    throws TaskException, DataAccessException {
        try {
            if (user == null)
                user = "N/A";

            UserActionVO userAction = new UserActionVO(user, UserActionVO.getAction(action), entity, entityId, comments);
            userAction.setSource("Task Manager");
            userAction.setDestination(destination);
            if (userAction.getAction().equals(UserActionVO.Action.Other)) {
                if (action != null && action.startsWith(TaskAction.CANCEL + "::")) {
                    userAction.setAction(UserActionVO.Action.Cancel);
                }
                else {
                userAction.setExtendedAction(action);
                }
            }
            EventManager eventManager = ServiceLocator.getEventManager();
            eventManager.createAuditLog(userAction);
        } catch (Exception e) {
            throw new TaskException("Failed to create audit log", e);
        }
    }


    /**
     * Retrieves all task reference data.
     *
     * @return Collection of Tasks
     */
    public Collection<TaskVO> getTasks()
    throws DataAccessException {
        return getTaskDAO().getAllTasks();
    }

    /**
     * Retrieves all task reference data.
     *
     * @param categoryId
     * @return Collection of Tasks
     */
    public TaskVO[] getTasks(Long categoryId)
    throws DataAccessException {
        List<Long> ids = getTaskDAO().getTaskIdsForCategory(categoryId);
        TaskVO[] tasks = new TaskVO[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            tasks[i] = TaskTemplateCache.getTaskTemplate(ids.get(i));
        }
        return tasks;
    }

    /**
     * Returns all the Task categories.
     *
     * @return Collection of TaskCategory objects
     */
    public TaskCategory[] getTaskCategories()
    throws DataAccessException {
        if (ApplicationContext.isFileBasedAssetPersist()) {
            return DataAccess.getBaselineData().getTaskCategories().values().toArray(new TaskCategory[0]);
        }
        else {
            Collection<TaskCategory> coll = getTaskDAO().getAllTaskCategories();
            return coll.toArray(new TaskCategory[coll.size()]);
        }
    }

    /**
     * Creates and returns a task category for the given params
     *
     * @param pCode
     * @param pDesc
     * @return TaskCategory
     */
    public TaskCategory createTaskCategory(String pCode, String pDesc)
    throws TaskException, DataAccessException {
        TaskCategory cat = this.getTaskDAO().createTaskCategory(pCode, pDesc);
        // refresh the cache
        new CacheRegistration().refreshCache("TaskCategoryCache");
        return cat;
    }

    /**
     * Updates the Task Category
     *
     * @param pId
     * @param pCode
     * @param pDesc
     * @return TaskCategory
     */
    public TaskCategory updateTaskCategory(Long pId, String pCode, String pDesc)
    throws TaskException, DataAccessException {
        TaskCategory cat = this.getTaskDAO().updateTaskCategory(pId, pCode, pDesc);
        // refresh the cache
        new CacheRegistration().refreshCache("TaskCategoryCache");
        return cat;
    }

    /**
     * Deletes the Task Category
     *
     * @param pId
     */
    public void deleteTaskCategory(Long pId)
    throws TaskException, DataAccessException {
        this.getTaskDAO().deleteTaskCategory(pId);
        // refresh the cache
        new CacheRegistration().refreshCache("TaskCategoryCache");
    }

    /**
     * Creates a task instance note.
     */
    public Long addNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.addNote()", true);
        Long id = getTaskDAO().createInstanceNote(owner, ownerId, noteName, noteDetails, user);
        auditLogActionPerformed(UserActionVO.Action.Create.toString(), user, Entity.Note, id, null, noteName);
        timer.stopAndLogTiming("");
        return id;
    }

    /**
     * Updates a note.
     */
    public void updateNote(Long noteId, String noteName, String noteDetails, String user)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.updateNote()", true);
        getTaskDAO().updateInstanceNote(noteId, noteName, noteDetails, user);
        auditLogActionPerformed(UserActionVO.Action.Change.toString(), user, Entity.Note, noteId, null, noteName);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates a note based on ownerId.
     */
    public void updateNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.updateNote()", true);
        getTaskDAO().updateInstanceNote(owner, ownerId, noteName, noteDetails, user);
        auditLogActionPerformed(UserActionVO.Action.Change.toString(), user, Entity.Note, ownerId, null, noteName);
        timer.stopAndLogTiming("");
    }

    /**
     * Deletes the passed in TaskInstanceNote
     *
     * @param pTaskNote
     */
    public void deleteNote(Long noteId, Long userId)
    throws TaskException, DataAccessException {
        this.getTaskDAO().deleteInstanceNote(noteId);
        auditLogActionPerformed(UserActionVO.Action.Delete.toString(), userId, Entity.Note, noteId, null, null);
    }

    /**
     * Deletes the task action user role mapping for the passid in user role
     *
     * @param pTaskUserGroupMap
     */
    public void updateTaskUserGroupMappings(Long pTaskId, Long[] pUserGroupIds)
    throws TaskException, DataAccessException {
        getTaskDAO().updateGroupsForTask(pTaskId, pUserGroupIds);
    }

    /**
     * Returns the TaskInstanceReport based on the requested report type
     *
     * @param pRportType
     */
    /**
     * Updates the Task state as jeopardy
     *
     */
    public int updateTaskInstanceStateAsJeopardy()
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.updateTaskInstanceStateAsJeopardy()", true);
        int count = 0;
        int batchSize = PropertyManager.getIntegerProperty(PropertyGroups.APPLICATION_DETAILS+"/BatchSize", 31);
        Collection<TaskInstanceVO> instances = getTaskDAO().getTaskInstancesPastDueDate(batchSize);
        for (TaskInstanceVO inst : instances) {
            updateTaskInstanceState(inst, TaskState.STATE_JEOPARDY);
        }
        count = instances.size();
        timer.stopAndLogTiming("Instances updated: " + instances.size());
        return count;
    }

    public void updateTaskInstanceState(Long taskInstId, boolean isAlert)
    throws TaskException, DataAccessException {
        TaskInstanceVO taskInst = getTaskDAO().getTaskInstance(taskInstId);
        if (isAlert) {
            if (taskInst.getStateCode().equals(TaskState.STATE_OPEN)) {
                updateTaskInstanceState(taskInst, TaskState.STATE_ALERT);
                if (taskInst.getDueDate()!=null) {
                    scheduleTaskSlaEvent(taskInst.getTaskInstanceId(),
                            taskInst.getDueDate(), 0, false);
                }
            }
        } else {
            if (taskInst.getStateCode().equals(TaskState.STATE_OPEN)
                    || taskInst.getStateCode().equals(TaskState.STATE_ALERT)) {
                updateTaskInstanceState(taskInst, TaskState.STATE_JEOPARDY);
            }
        }
    }

    private void updateTaskInstanceState(TaskInstanceVO inst, Integer newState)
            throws TaskException, DataAccessException {
        if (TaskState.STATE_INVALID != newState)
            notifyStateChange(inst, newState.intValue());
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATE", newState);
        getTaskDAO().updateTaskInstance(inst.getTaskInstanceId(), changes, false);
    }

    /**
     * Updates a batch of task instances to Alert state as appropriate.
     */
    public int updateTaskInstanceStateAsAlert()
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.updateTaskInstanceStateAsAlert()", true);
        int count = 0;
        int batchSize = PropertyManager.getIntegerProperty(PropertyGroups.APPLICATION_DETAILS+"/BatchSize", 31);
        Collection<TaskInstanceVO> taskInstances = getTaskDAO().getTaskInstancesApproachingDueDate(batchSize);
        for (TaskInstanceVO inst : taskInstances) {
            updateTaskInstanceState(inst, TaskState.STATE_ALERT);
        }
        count = taskInstances.size();
        timer.stopAndLogTiming("Instances updated: " + taskInstances.size());
        return count;
    }

    public void updateAssociatedTaskInstance(Long taskInstId, String ownerApplName, Long associatedTaskInstId)
    throws TaskException, DataAccessException {
        getTaskDAO().updateAssociatedTaskInstance(taskInstId, ownerApplName, associatedTaskInstId);
    }

    /**
     * Creates and adds a task attachment
     */
    public Long addAttachment(String attachName,
            String attachLoc, String contentType, String user, String owner, Long ownerId)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.addTaskInstanceAttachment()", true);
        if (! attachLoc.startsWith(MiscConstants.ATTACHMENT_LOCATION_PREFIX)
             && ! attachLoc.endsWith("/")) {
            attachLoc += "/";
        }
        Attachment att = getAttachment(attachName,attachLoc);
        if (att != null) {
            getTaskDAO().updateAttachment(att.getId(), attachLoc, user);
            timer.stopAndLogTiming("UpdatedExistingOne");
            return att.getId();
        }


        Long id = this.getTaskDAO().createAttachment(owner, ownerId,
                Attachment.STATUS_ATTACHED, attachName, attachLoc, contentType, user);
        auditLogActionPerformed(UserActionVO.Action.Create.toString(), user, Entity.Attachment, id, null, attachName);

        timer.stopAndLogTiming("");
        return id;
    }

    /**
     * Removes the attachment from the task instance
     *
     * @param pTaskInstId
     * @param pAttachName
     * @param pAttachLocation
     * @return Attachment
     */
    public void removeAttachment(Long pAttachId, Long userId)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.removeAttachment()", true);
        this.getTaskDAO().deleteAttachment(pAttachId);
        timer.stopAndLogTiming("");
        auditLogActionPerformed(UserActionVO.Action.Delete.toString(), userId, Entity.Attachment, pAttachId, null, null);
    }

    /**
     * Returns the collection of attachments
     *
     * @param pTaskInstId
     * @return Collection of Attachment
     */
    public Collection<Attachment> getAttachments(String pAttachName,String attachmentLocation)
    throws DataAccessException {
        Collection<Attachment> attColl = null;
        CodeTimer timer = new CodeTimer("TaskManager.getTaskInstanceAttachments()", true);
        attColl = this.getTaskDAO().getAttachments(pAttachName,attachmentLocation);
        timer.stopAndLogTiming("");
        return attColl;

    }

    /**
     * Method provides attachment based on attachment location
     */
    public Attachment getAttachment(String pAttachName,
            String attachmentLocation) throws DataAccessException {
        Collection<Attachment> attColl = null;
        Attachment att = null;
        CodeTimer timer = new CodeTimer("TaskManager.getTaskInstanceAttachment()", true);
        attColl = getAttachments(pAttachName,attachmentLocation);
        for (Attachment attachment : attColl) {
            att = attachment;
            break;
        }
        timer.stopAndLogTiming("");
        return att;
    }

   /**
    * Notifies the Observer about the Task State
    *
    * @param taskInstance
    */
    // TODO: handle non-standard status changes
    public void notifyTaskAction(TaskInstanceVO taskInstance, String action, Integer previousStatus, Integer previousState)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.notifyStatusChange()", true);

        try {
            // new-style notifiers
            String outcome = TaskStatuses.getTaskStatuses().get(taskInstance.getStatusCode());
            //if (!action.equals(TaskAction.CLAIM) && !action.equals(TaskAction.RELEASE))  // avoid nuisance notice to claimer and releaser
            (new EngineAccess()).sendNotification(taskInstance, action, outcome);

            // new-style auto-assign
            if (TaskStatus.STATUS_OPEN.intValue() == taskInstance.getStatusCode().intValue()
                    && !action.equals(TaskAction.RELEASE)) {
                AutoAssignStrategy strategy = getAutoAssignStrategy(taskInstance);
                if (strategy != null) {
                    UserVO assignee = strategy.selectAssignee(taskInstance);
                    if (assignee == null)
                        logger.severe("No users found for auto-assignment of task instance ID: " + taskInstance.getTaskInstanceId());
                    else
                    {
                      taskInstance.setTaskClaimUserId(assignee.getId());
                      taskInstance.setTaskClaimUserCuid(assignee.getCuid());
                      performActionOnTaskInstance(TaskAction.ASSIGN, taskInstance.getTaskInstanceId(),
                          assignee.getId(), assignee.getId(), "Auto-assigned", null, false, false);
                    }
                }
            }
        }
        catch (ObserverException ex) {
            // do not rethrow; observer problems should not prevent task actions
            logger.severeException(ex.getMessage(), ex);
        }
        catch (StrategyException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        timer.stopAndLogTiming("");
    }

    public AutoAssignStrategy getAutoAssignStrategy(TaskInstanceVO taskInstance) throws StrategyException, TaskException {
      TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
      String autoAssignAttr = taskVO.getAttribute(TaskAttributeConstant.AUTO_ASSIGN);
      AutoAssignStrategy strategy = null;
      if (StringHelper.isEmpty(autoAssignAttr))
          return strategy;
      else{
        try {
          strategy = TaskInstanceStrategyFactory.getAutoAssignStrategy(autoAssignAttr,
                  OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
          if (strategy instanceof ParameterizedStrategy) {
            // need to check how to pass the indices
            populateStrategyParams((ParameterizedStrategy)strategy, TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId()),
                taskInstance.getOwnerId(),
                OwnerType.DOCUMENT.equals(taskInstance.getSecondaryOwnerType()) ? taskInstance.getSecondaryOwnerId() : null, null);
          }
        }
        catch (Exception ex) {
            throw new TaskException(ex.getMessage(), ex);
        }
      }
      return strategy;
  }

    /**
     * Determines a task instance's workgroups based on the defined strategy.  If no strategy exists,
     * default to the workgroups defined in the task template.
     *
     * By default this method propagates StrategyException as TaskException.  If users wish to continue
     * processing they can override the default strategy implementation to catch StrategyExceptions.
     */
    private List<String> determineWorkgroups(TaskVO taskTemplate, TaskInstanceVO taskInstance, Map<String,String> indices)
    throws TaskException {
        String routingStrategyAttr = taskTemplate.getAttribute(TaskAttributeConstant.ROUTING_STRATEGY);
        if (StringHelper.isEmpty(routingStrategyAttr)) {
            return taskTemplate.getWorkgroups();
        }
        else {
            try {
                RoutingStrategy strategy = TaskInstanceStrategyFactory.getRoutingStrategy(routingStrategyAttr,
                        OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
                if (strategy instanceof ParameterizedStrategy) {
                    populateStrategyParams((ParameterizedStrategy)strategy, taskTemplate,
                            taskInstance.getOwnerId(),
                            OwnerType.DOCUMENT.equals(taskInstance.getSecondaryOwnerType()) ? taskInstance.getSecondaryOwnerId() : null, indices);
                }
                return strategy.determineWorkgroups(taskTemplate, taskInstance);
            }
            catch (Exception ex) {
                throw new TaskException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Returns the subTaskPlan for a master task instance
     *
     * By default this method propagates StrategyException as TaskException.  If users wish to continue
     * processing they can override the default strategy implementation to catch StrategyExceptions.
     */
    public SubTaskPlan getSubTaskPlan(TaskRuntimeContext runtimeContext)
    throws TaskException {
        String subTaskStrategyAttr = runtimeContext.getTaskAttribute(TaskAttributeConstant.SUBTASK_STRATEGY);
        if (StringHelper.isEmpty(subTaskStrategyAttr)) {
            return null;
        }
        else {
            try {
                TaskInstanceVO taskInstance = runtimeContext.getTaskInstanceVO();
                SubTaskStrategy strategy = TaskInstanceStrategyFactory.getSubTaskStrategy(subTaskStrategyAttr,
                        OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
                if (strategy instanceof ParameterizedStrategy) {
                    populateStrategyParams((ParameterizedStrategy)strategy, runtimeContext.getTaskTemplate(), taskInstance.getOwnerId(),
                            OwnerType.DOCUMENT.equals(taskInstance.getSecondaryOwnerType()) ? taskInstance.getSecondaryOwnerId() : null, null);
                }
                XmlOptions xmlOpts = Compatibility.namespaceOptions().setDocumentType(SubTaskPlanDocument.type);
                SubTaskPlanDocument subTaskPlanDoc = SubTaskPlanDocument.Factory.parse(strategy.getSubTaskPlan(runtimeContext), xmlOpts);
                return subTaskPlanDoc.getSubTaskPlan();
            }
            catch (Exception ex) {
                throw new TaskException(ex.getMessage(), ex);
            }
        }
    }

    private PrioritizationStrategy getPrioritizationStrategy(TaskVO taskTemplate, Long processInstanceId, Long formDataDocId, Map<String,String> indices)
    throws DataAccessException, MbengException, StrategyException {
        String priorityStrategyAttr = taskTemplate.getAttribute(TaskAttributeConstant.PRIORITY_STRATEGY);
        if (StringHelper.isEmpty(priorityStrategyAttr)) {
            return null;
        }
        else {
            PrioritizationStrategy strategy = TaskInstanceStrategyFactory.getPrioritizationStrategy(priorityStrategyAttr, processInstanceId);
            if (strategy instanceof ParameterizedStrategy) {
                populateStrategyParams((ParameterizedStrategy)strategy, taskTemplate, processInstanceId, formDataDocId, indices);
            }
            return strategy;
        }
    }

    private void populateStrategyParams(ParameterizedStrategy strategy, TaskVO taskTemplate, Long processInstId, Long formDataDocId, Map<String,String> indices)
    throws DataAccessException, MbengException {
        PackageVO pkg = null;
        for (AttributeVO attr : taskTemplate.getAttributes()) {
            strategy.setParameter(attr.getAttributeName(), attr.getAttributeValue());
        }
        EventManager eventManager = ServiceLocator.getEventManager();
        try {
            ProcessVO procdef = eventManager.findProcessByProcessInstanceId(processInstId);
            pkg = PackageVOCache.getProcessPackage(procdef.getProcessId());
        } catch (Exception ex) {
            logger.severeException("Failed to get the process Package information : "+processInstId, ex);
        }

        if (taskTemplate.isAutoformTask() && formDataDocId != null) {
            DocumentVO docvo = eventManager.getDocumentVO(formDataDocId);
            FormDataDocument datadoc = new FormDataDocument();
            datadoc.load(docvo.getContent());
            List<VariableInstanceVO> varInsts = constructVariableInstancesFromFormDataDocument(taskTemplate, processInstId, datadoc);
            for (VariableInstanceVO varInst : varInsts) {
                Object varVal = varInst.getData();
                if (varInst.isDocument()) {
                    varVal = varInst.getRealData();
                }
                strategy.setParameter(varInst.getName(), varVal);
            }
        }
        else {
            for (VariableInstanceVO varInst : getProcessInstanceVariables(processInstId)) {
                Object varVal = varInst.getData();
                if (varInst.isDocument())
                    varVal = eventManager.getDocumentVO(((DocumentReference)varVal).getDocumentId()).getObject(varInst.getType(), pkg);
                strategy.setParameter(varInst.getName(), varVal);
            }
        }
    }

    /**
     * Notifies the Observer about the Task State
     *
     * @param pTaskInst
     */
    private void notifyStateChange(TaskInstanceVO pTaskInst, int pNewState) throws TaskException,
            DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.notifyStateChange()", true);

        try {
            // new-style notifiers
            String outcome = TaskStates.getTaskStates().get(pNewState);
            (new EngineAccess()).sendNotification(pTaskInst, null, outcome);
        }
        catch (Exception ex) {
            // do not rethrow to avoid killing monitoring timer
            logger.severeException(ex.getMessage(), ex);
        }
        timer.stopAndLogTiming("");
    }

    /**
     * get the task instance for the activity instance.
     * For general tasks, the process instance ID is needed
     * to loop through all task instances of the process.
     *
     * @param activityInstId activity instance ID
     * @param pProcessInstId process instance ID for general tasks; null for classic tasks
     */
    public TaskInstanceVO getTaskInstanceByActivityInstanceId(Long activityInstanceId,
            Long procInstId)
    throws TaskException, DataAccessException {
        TaskInstanceVO taskInst;
        if (procInstId==null) {
            taskInst = getTaskDAO().getTaskInstanceByActivityInstanceId(activityInstanceId);
        } else {
            List<TaskInstanceVO> taskInstList = getTaskDAO().getTaskInstancesForProcessInstance(procInstId);
            taskInst = null;
            for (TaskInstanceVO one : taskInstList) {
                if (!one.getSecondaryOwnerType().equals(OwnerType.DOCUMENT)) continue;
                DocumentVO doc = getTaskInstanceData(one);
                FormDataDocument formDataDoc = new FormDataDocument();
                try {
                    formDataDoc.load(doc.getContent());
                    if (!activityInstanceId.equals(formDataDoc.getActivityInstanceId())) continue;
                    taskInst = one;
                    break;
                } catch (MbengException e) {
                    logger.warnException("Failed to load document", e);
                }
            }
        }
        return taskInst;
    }


    /**
     * Creates and returns a Task.
     *
     * @param pTaskName
     * @param pTaskType
     * @param pTaskCategory
     * @param pTaskDesc
     * @param pSla
     * @return task ID
     */
    public Long createTask(TaskVO task, boolean saveAttributes)
    throws DataAccessException {
        task.setTaskId(null);
        Long taskId = getTaskDAO().saveTask(task, saveAttributes);
        // refresh the task and sla caches
        CacheRegistration cacheRegistry = new CacheRegistration();
        cacheRegistry.refreshCache("TaskCache");
//        cacheRegistry.refreshCache("ServiceLevelAgreementCache");
        return taskId;
    }

    /**
     * updates the task
     *
     * @param pTaskId
     * @param pTaskName
     * @param pTaskType
     * @param pTaskCategory
     * @param pTaskDesc
     * @param pSla
     * @return Task
     */
    public void updateTask(TaskVO task, boolean saveAttributes)
    throws DataAccessException {
        getTaskDAO().saveTask(task, saveAttributes);
        // refresh the task and sla caches
        CacheRegistration cacheRegistry = new CacheRegistration();
        cacheRegistry.refreshCache("TaskCache");
//        cacheRegistry.refreshCache("ServiceLevelAgreementCache");
    }

    /**
     * deletes the task
     *
     * @param pTaskId
     */
    public void deleteTask(Long pTaskId)
    throws DataAccessException {
        getTaskDAO().deleteTask(pTaskId);
        // refresh the task and sla caches
        CacheRegistration cacheRegistry = new CacheRegistration();
        cacheRegistry.refreshCache("TaskCache");
//        cacheRegistry.refreshCache("ServiceLevelAgreementCache");
    }

    /**
     * Creates and returns a Task Attribute
     *
     * @param pName
     * @param pValue
     * @return Attribute
     */
    public void addTaskAttribute(Long pTaskId, String pName, String pValue, Integer pType)
    throws DataAccessException {
        getTaskDAO().setTaskAttribute(pTaskId, pName, pValue);
    }

    /**
     * Updates the passed in attribute for task
     *
     * @param pAttribId
     * @param pName
     * @param pValue
     * @return Attribute
     */
    public void updateTaskAttribute(Long pAttribId, String pName, String pValue, Integer pType)
    throws DataAccessException {
        getTaskDAO().updateAttribute(pAttribId, pName, pValue);
    }

    /**
     * Deletes the passed in attribute for task
     *
     * @param pAttribId
     */
    public void deleteTaskAttribute(Long pAttribId)
    throws TaskException, DataAccessException {
        getTaskDAO().updateAttribute(pAttribId, null, null);
    }

    /**
     * Updates the due date for a task instance.
     * The method should only be called in summary (or summary-and-detail) task manager.
     *
     * @param pTaskInstanceId
     * @param pDueDate
     */
    public void updateTaskInstanceDueDate(Long pTaskInstanceId, Date pDueDate, String cuid, String comment)
    throws TaskException, DataAccessException {
        boolean hasOldSlaInstance;
        EventManager eventManager = ServiceLocator.getEventManager();
        EventInstanceVO event = eventManager.getEventInstance(ScheduledEvent.SPECIAL_EVENT_PREFIX + "TaskDueDate." + pTaskInstanceId.toString());
        boolean isEventExist = event == null ? false : true;
        hasOldSlaInstance = !isEventExist;
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("DUE_DATE", pDueDate);
        changes.put("COMMENTS", comment);
        getTaskDAO().updateTaskInstance(pTaskInstanceId, changes, false);
        if (pDueDate==null) {
            this.unscheduleTaskSlaEvent(pTaskInstanceId);
        } else {
            TaskInstanceVO taskInst = getTaskDAO().getTaskInstance(pTaskInstanceId);
            TaskVO task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
            String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            this.scheduleTaskSlaEvent(pTaskInstanceId, pDueDate, alertInterval, !hasOldSlaInstance);
        }
        auditLogActionPerformed(UserActionVO.Action.Change.toString(), cuid,
                Entity.TaskInstance, pTaskInstanceId, null, "change due date / comments");
    }

    public void updateTaskInstanceComments(Long taskInstanceId, String comments)
    throws TaskException, DataAccessException {
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("COMMENTS", comments);
        getTaskDAO().updateTaskInstance(taskInstanceId, changes, false);
    }

    public TaskAction getTaskAction(String action)
    throws DataAccessException {
        return getTaskDAO().getTaskAction(action);
    }

    public TaskAction addTaskAction(String action, String description)
    throws DataAccessException {
        return getTaskDAO().createTaskAction(action, description);
    }


    /**
     * Gets the dynamic task actions associated with a task instance as determined by
     * the result codes for the possible outgoing work transitions from the associated
     * activity.
     *
     * @param taskInstanceId
     * @return the list of task actions
     */
    public List<TaskAction> getDynamicTaskActions(Long taskInstanceId)
    throws TaskException, DataAccessException {

        CodeTimer timer = new CodeTimer("TaskManager.getDynamicTaskActions()", true);

        List<TaskAction> dynamicTaskActions = new ArrayList<TaskAction>();
        TaskInstanceVO taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);

        try {
            Long activityInstanceId = getActivityInstanceId(taskInstance, true);

            if (activityInstanceId != null) {
                EventManager eventManager = ServiceLocator.getEventManager();
                ActivityInstanceVO activityInstance = eventManager.getActivityInstance(activityInstanceId);
                Long processInstanceId = activityInstance.getOwnerId();
                ProcessInstanceVO processInstance = eventManager.getProcessInstance(processInstanceId);

                ProcessVO processVO = ProcessVOCache.getProcessVO(processInstance.getProcessId());
                if (processInstance.isNewEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<WorkTransitionVO> outgoingWorkTransVOs = processVO.getAllWorkTransitions(activityInstance.getDefinitionId());
                for (WorkTransitionVO workTransVO : outgoingWorkTransVOs) {
                    String resultCode = workTransVO.getCompletionCode();
                    if (resultCode != null) {
                        Integer eventType = workTransVO.getEventType();
                        if (eventType.equals(EventType.FINISH) || eventType.equals(EventType.RESUME) || TaskAction.FORWARD.equals(resultCode)) {
//                            TaskAction taskAction = getTaskDAO().getTaskAction(resultCode);
                            TaskAction taskAction = new TaskAction();
                            taskAction.setTaskActionName(resultCode);
                            taskAction.setDynamic(true);
                            dynamicTaskActions.add(taskAction);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        timer.stopAndLogTiming("");
        return dynamicTaskActions;
    }

    /**
     * Gets the standard task actions, filtered according to what's applicable
     * depending on the context of the task activity instance.
     *
     * @param taskInstanceId
     * @param standardTaskActions unfiltered list
     * @return the list of task actions
     */
    public List<TaskAction> filterStandardTaskActions(Long taskInstanceId, List<TaskAction> standardTaskActions)
    throws TaskException, DataAccessException {

        CodeTimer timer = new CodeTimer("TaskManager.filterStandardTaskActions()", true);

        List<TaskAction> filteredTaskActions = standardTaskActions;
        TaskInstanceVO taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);

        try {
            Long activityInstanceId = getActivityInstanceId(taskInstance, true);

            if (activityInstanceId != null) {
                EventManager eventManager = ServiceLocator.getEventManager();
                ActivityInstanceVO activityInstance = eventManager.getActivityInstance(activityInstanceId);
                Long processInstanceId = activityInstance.getOwnerId();
                ProcessInstanceVO processInstance = eventManager.getProcessInstance(processInstanceId);

                if (!isExceptionHandlerTaskInstance(taskInstance)) {
                    // remove RETRY since no default behavior is defined for inline tasks
                    TaskAction retryAction = null;
                    for (TaskAction taskAction : standardTaskActions) {
                        if (taskAction.getTaskActionName().equalsIgnoreCase("Retry"))
                            retryAction = taskAction;
                        }
                    if (retryAction != null)
                        standardTaskActions.remove(retryAction);
                }

                ProcessVO processVO = ProcessVOCache.getProcessVO(processInstance.getProcessId());
                if (processInstance.isNewEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<WorkTransitionVO> outgoingWorkTransVOs = processVO.getAllWorkTransitions(activityInstance.getDefinitionId());
                boolean foundNullResultCode = false;
                for (WorkTransitionVO workTransVO : outgoingWorkTransVOs) {
                    Integer eventType = workTransVO.getEventType();
                    if ((eventType.equals(EventType.FINISH) || eventType.equals(EventType.RESUME))
                        && workTransVO.getCompletionCode() == null) {
                      foundNullResultCode = true;
                      break;
                    }
                }
                if (!foundNullResultCode) {
                    TaskAction cancelAction = null;
                    TaskAction completeAction = null;
                    for (TaskAction taskAction : standardTaskActions) {
                        if (taskAction.getTaskActionName().equalsIgnoreCase("Cancel"))
                            cancelAction = taskAction;
                        if (taskAction.getTaskActionName().equalsIgnoreCase("Complete"))
                            completeAction = taskAction;
                    }
                    if (cancelAction != null)
                        standardTaskActions.remove(cancelAction);
                    if (completeAction != null)
                      standardTaskActions.remove(completeAction);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        timer.stopAndLogTiming("");
        return filteredTaskActions;
    }

    @Override
    public Long getActivityInstanceId(TaskInstanceVO taskInstance, boolean sourceActInst)
    {
      Long activityInstanceId = null;
      TaskInstanceVO taskInst = taskInstance;
      try
      {
        if (!OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
            return null;
        EventManager eventManager = ServiceLocator.getEventManager();
        if (sourceActInst && isExceptionHandlerTaskInstance(taskInstance)) {
            ProcessInstanceVO subProcessInstance = eventManager.getProcessInstance(taskInstance.getOwnerId());
            // subprocess secondary owner is activity instance
            activityInstanceId = subProcessInstance.getSecondaryOwnerId();
        }
        else {
            // Master/Sub task
            if (OwnerType.TASK_INSTANCE.equals(taskInstance.getSecondaryOwnerType())) {
                taskInst = getTaskDAO().getTaskInstance(taskInstance.getSecondaryOwnerId()); //secondary owner id refers to master task instance id
            }
            if (OwnerType.DOCUMENT.equals(taskInst.getSecondaryOwnerType())) {
                String formDataString = getTaskInstanceData(taskInst).getContent();
                FormDataDocument formDataDoc = new FormDataDocument();
                formDataDoc.load(formDataString);
                activityInstanceId = formDataDoc.getActivityInstanceId();
            }
            else {
                // task instance secondary owner is work transition instance
                Long workTransInstId = taskInst.getSecondaryOwnerId();
                WorkTransitionInstanceVO workTransInst = eventManager.getWorkTransitionInstance(workTransInstId);
                activityInstanceId = workTransInst.getDestinationID();
            }
        }
      }
      catch (Exception ex) {
        logger.severeException(ex.getMessage(), ex);
      }
      return activityInstanceId;
    }

    /**
     * Gets the event log entries for a task instance.
     *
     * @param taskInstanceId
     * @return Collection of EventLog objects
     */
    public List<EventLog> getEventLogs(Long taskInstanceId)
    throws TaskException, DataAccessException {
        EventManager eventManager = ServiceLocator.getEventManager();
        return eventManager.getEventLogs(null, null, "TaskInstance", taskInstanceId);
    }

    public DocumentVO getTaskInstanceData(TaskInstanceVO taskInst) throws DataAccessException {
        EventManager eventManager = ServiceLocator.getEventManager();
        return eventManager.getDocumentVO(taskInst.getSecondaryOwnerId());
    }

    public VariableInstanceVO[] getProcessInstanceVariables(Long procInstId)
    throws DataAccessException {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            ProcessInstanceVO procInst = eventManager.getProcessInstance(procInstId);
            List<VariableInstanceInfo> vars;
            ProcessVO procdef = ProcessVOCache.getProcessVO(procInst.getProcessId());
            if (procdef.isEmbeddedProcess() || procInst.isNewEmbedded())
                vars = eventManager.getProcessInstanceVariables(procInst.getOwnerId());
            else vars = eventManager.getProcessInstanceVariables(procInstId);
            VariableInstanceVO[] retVars = new VariableInstanceVO[vars.size()];
            for (int i=0; i<vars.size(); i++) {
                VariableInstanceInfo v = vars.get(i);
                VariableInstanceVO retv = new VariableInstanceVO();
                retVars[i] = retv;
                retv.setStringValue(v.getStringValue());
                retv.setName(v.getName());
                retv.setType(v.getType());
                retv.setInstanceId(v.getInstanceId());
                retv.setVariableId(v.getVariableId());
            }
            return retVars;
        } catch (ProcessException e) {
            throw new DataAccessException(-1, "failed to get task variable instances", e);
        }
    }

    // see TaskManager.java for javadoc
    public int countTaskInstances(String fromWhereClause)
    throws TaskException, DataAccessException {
        return getTaskDAO().countTaskInstancesNew(fromWhereClause);
    }

    // see TaskManager.java for javadoc
    public List<TaskInstanceVO> queryTaskInstances(String fromWhereClause,
            int startIndex, int endIndex, String sortOn, boolean loadIndices)
    throws TaskException, DataAccessException {
        return getTaskDAO().queryTaskInstancesNew(fromWhereClause, startIndex, endIndex, sortOn, loadIndices);
    }

    // see TaskManager.java for javadoc
    public int countTasks(String whereCondition) throws DataAccessException {
        return getTaskDAO().countTasks(whereCondition);
    }

    // see TaskManager.java for javadoc
    public List<TaskVO> queryTasks(String whereCondition, int startIndex, int endIndex, String sortOn) throws DataAccessException {
        return getTaskDAO().queryTasks(whereCondition, startIndex, endIndex, sortOn);
    }

    public List<String> getGroupsForTask(Long taskId) throws DataAccessException {
        return getTaskDAO().getGroupsForTask(taskId);
    }

    public List<String> getGroupsForTaskInstance(TaskInstanceVO taskInst) throws DataAccessException, TaskException {
        if (taskInst.isShallow()) this.getTaskInstanceAdditionalInfo(taskInst);
        if (taskInst.isTemplateBased()) return taskInst.getGroups();
        else return getTaskDAO().getGroupsForTask(taskInst.getTaskId());
    }

    public Long saveResource(RuleSetVO resource)
        throws DataAccessException {
        DatabaseAccess db = new DatabaseAccess(null);
        ProcessPersister dao = DataAccess.getProcessPersister(DataAccess.currentSchemaVersion,
                DataAccess.supportedSchemaVersion, db, null);
        Long id = resource.getId();
        if (id<=0L) id = dao.createRuleSet(resource);
        else dao.updateRuleSet(resource);
            return id;
    }

    public Long saveAsset(String workflowPackage, RuleSetVO asset, String user)
            throws DataAccessException, TaskException {
                PackageVO pkg = PackageVOCache.getPackage(workflowPackage);
                if (pkg == null)
                    throw new DataAccessException("Workflow package not found: " + workflowPackage);
                DatabaseAccess db = new DatabaseAccess(null);
                ProcessPersister dao = DataAccess.getProcessPersister(DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion, db, null);
                Long id = dao.createRuleSet(asset);
                RuleSetVO oldMapped = pkg.getRuleSet(asset.getName());
                if (oldMapped != null)
                    dao.removeRuleSetFromPackage(oldMapped.getId(), pkg.getId());
                dao.addRuleSetToPackage(id, pkg.getId());

                auditLogActionPerformed("Create", user, Entity.RuleSet, id, null, workflowPackage + "/" + asset.getLabel());

                return id;
     }

    public List<VariableInstanceVO> constructVariableInstancesFromFormDataDocument(TaskVO taskVO, Long processInstanceId, FormDataDocument datadoc) throws DataAccessException {
        return constructVariableInstancesFromFormDataDocument(taskVO, processInstanceId, datadoc, null);
    }

    public List<VariableInstanceVO> constructVariableInstancesFromFormDataDocument(TaskVO taskVO, Long processInstanceId, FormDataDocument datadoc, Long taskInstId) throws DataAccessException {
        List<VariableInstanceVO> varinstList = new ArrayList<VariableInstanceVO>();
        TaskRuntimeContext runtimeContext = null;
        if (taskVO.getVariables() != null) {
            for (VariableVO vardef : taskVO.getVariables()) {
                String name = vardef.getName();
                VariableInstanceVO varinst = null;
                if (!name.startsWith("#{") && !name.startsWith("${")) {
                    varinst = new VariableInstanceVO();
                    String varvalue = datadoc.getValue(name);
                    if (VariableTranslator.isDocumentReferenceVariable(null, vardef.getVariableType())) {
                        varinst.setRealStringValue(varvalue);
                    }
                    else {
                        varinst.setStringValue(varvalue);
                    }
                }
                else if (taskInstId != null && taskInstId != 0L) {
                    if (runtimeContext == null)
                        runtimeContext = getTaskRuntimeContext(getTaskInstance(taskInstId));

                    varinst = new VariableInstanceVO();
                    varinst.setStringValue(runtimeContext.evaluateToString(name));
                }

                if (varinst != null) {
                    varinst.setVariableReferredName(vardef.getVariableReferredAs());
                    varinst.setName(vardef.getName());
                    varinst.setEditable(vardef.getDisplayMode().equals(VariableVO.DATA_OPTIONAL)
                            || vardef.getDisplayMode().equals(VariableVO.DATA_REQUIRED));
                    varinst.setRequired(vardef.getDisplayMode().equals(VariableVO.DATA_REQUIRED));
                    varinst.setProcessInstanceId(processInstanceId);
                    varinst.setType(vardef.getVariableType());
                    varinstList.add(varinst);
                }
            }
        }
        return varinstList;
    }

    @Override
    public Attachment getAttachment(Long pAttachmentId)
    throws DataAccessException {
         Attachment attachment= null;
         CodeTimer timer = new CodeTimer("TaskManager.getAttachment()", true);
         attachment = this.getTaskDAO().getAttachment(pAttachmentId);
         timer.stopAndLogTiming("");
         return attachment;
    }

    public void updateTaskIndices(Long taskInstanceId, Map<String,String> indices)
    throws DataAccessException {
        getTaskDAO().setTaskInstanceIndices(taskInstanceId, indices);
    }

    public void updateTaskInstanceWorkgroups(Long taskInstanceId, List<String> groups)
    throws DataAccessException {
        getTaskDAO().setTaskInstanceGroups(taskInstanceId, groups.toArray(new String[0]));
    }

    public void updateTaskInstancePriority(Long taskInstanceId, Integer priority)
    throws DataAccessException {
        getTaskDAO().setTaskInstancePriority(taskInstanceId, priority);
    }

  @Override
  public void updateTaskInstanceData(Map<String,Object> changes, List<String> workGroups, Long autoAssignee, TaskInstanceVO taskInst, String cuid)
      throws DataAccessException, TaskException
  {
    boolean hasOldSlaInstance;
    Long taskInstId = taskInst.getTaskInstanceId();
    Date taskDueDate = (Date) changes.get("DUE_DATE");

    EventManager eventManager = ServiceLocator.getEventManager();
    EventInstanceVO event = eventManager.getEventInstance(ScheduledEvent.SPECIAL_EVENT_PREFIX + "TaskDueDate." + taskInstId.toString());
    boolean isEventExist = event == null ? false : true;

    // ReSchedule the event if event is already exist otherwise schedule the event
    hasOldSlaInstance = !isEventExist;

    getTaskDAO().updateTaskInstance(taskInstId, changes, false); // update task instance with changes(duedate,priority,comments)

    if(workGroups != null && !workGroups.isEmpty())
    {
      getTaskDAO().setTaskInstanceGroups(taskInst.getTaskInstanceId(), StringHelper.toStringArray(workGroups)); // update task instance work groups
    }

    if(autoAssignee != null)
    {
      assignTaskInstance(taskInst, autoAssignee); // handle auto-assign
    }

    if (taskDueDate == null)
    {
      this.unscheduleTaskSlaEvent(taskInstId);
    }
    else
    {
      TaskVO task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
      String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
      int alertInterval = StringHelper.isEmpty(alertIntervalString) ? 0 : Integer.parseInt(alertIntervalString);
      this.scheduleTaskSlaEvent(taskInstId, taskDueDate, alertInterval, !hasOldSlaInstance);
    }
    if (!taskInst.isSummaryOnly())
      this.notifyTaskAction(taskInst, TaskAction.SAVE, null, null);
    auditLogActionPerformed(UserActionVO.Action.Change.toString(), cuid, Entity.TaskInstance, taskInstId, null, "change due date / priority / comments");
  }

  @Override
  public Map<String, Object> getChangesAfterApplyStrategy(Map<String, Object> changesMap, TaskInstanceVO taskInst) throws DataAccessException, TaskException
  {
    Date taskDueDate = (Date) changesMap.get(DUE_DATE);
    TaskVO task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());

    Map<String, String> indices = null;

    if(task.isUsingIndices())
    {
      indices = getTaskDAO().getTaskInstIndices(taskInst.getTaskInstanceId()); // TODO : verify Is it required call at detail side?
    }

    // apply prioritization strategy only when due date change
    if (taskDueDate != null && changesMap.get(PRIORITY) == null)
    {
      if (task.isTemplate())
      {
        try
        {
          PrioritizationStrategy prioritizationStrategy = getPrioritizationStrategy(task, taskInst.getOwnerId(),
              OwnerType.DOCUMENT.equals(taskInst.getSecondaryOwnerType()) ? taskInst.getSecondaryOwnerId() : null, indices);
          if (prioritizationStrategy != null)
          {
            int priority = prioritizationStrategy.determinePriority(task, taskDueDate);
            changesMap.put(PRIORITY, priority);
          }
        }
        catch (Exception ex)
        {
          throw new TaskException(ex.getMessage(), ex);
        }
      }
    }
    if (changesMap.containsKey(DUE_DATE) || changesMap.containsKey(PRIORITY))
    { // recalculate routing strategy
      if (task.isTemplate())
      {
        List<String> groups = determineWorkgroups(task, taskInst, indices);

        if (groups != null && groups.size() > 0)
        {
          if (!groups.equals(taskInst.getWorkgroups()))
          {
            changesMap.put(GROUPS, groups);
            taskInst.setWorkgroups(groups);
            try
            {
              AutoAssignStrategy strategy = getAutoAssignStrategy(taskInst);
              if (strategy != null)
              {
                UserVO assignee = strategy.selectAssignee(taskInst);
                if (assignee == null)
                  logger.severe("No users found for auto-assignment of task instance ID: " + taskInst.getTaskInstanceId());
                else
                  changesMap.put(AUTO_ASSIGNEE, assignee);
              }
            }
            catch (StrategyException ex)
            {
              logger.severeException(ex.getMessage(), ex);
            }
            catch (ObserverException ex)
            {
              logger.severeException(ex.getMessage(), ex);
            }
          }
        }
      }
    }
    return changesMap;
  }

  public TaskRuntimeContext getTaskRuntimeContext(TaskInstanceVO taskInstanceVO) {
      EventManager eventMgr = null;
      TaskRuntimeContext taskRunTime = null;

      try {
          eventMgr = ServiceLocator.getEventManager();
          TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstanceVO.getTaskId());
          Map<String,Object> vars = new HashMap<String,Object>();

          if(OwnerType.PROCESS_INSTANCE.equals(taskInstanceVO.getOwnerType())) {
              ProcessInstanceVO procInstVO = eventMgr.getProcessInstance(taskInstanceVO.getOwnerId(), true);
              if (procInstVO.isNewEmbedded())
              {
                  Long procInstanceId = procInstVO.getOwnerId();
                  procInstVO = eventMgr.getProcessInstance(procInstanceId, true);
              }
              Long processId = procInstVO.getProcessId();
              ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
              PackageVO packageVO = PackageVOCache.getProcessPackage(processId);

              if (procInstVO.getVariables() != null) {
                  for (VariableInstanceInfo var : procInstVO.getVariables()) {
                      Object value = var.getData();
                      if (value instanceof DocumentReference) {
                          try {
                              DocumentVO docVO = eventMgr.getDocument((DocumentReference)value, false);
                              value = docVO == null ? null : docVO.getObject(var.getType(), packageVO);
                          }
                          catch (DataAccessException ex) {
                              logger.severeException(ex.getMessage(), ex);
                          }
                      }
                      vars.put(var.getName(), value);
                  }
              }
              taskRunTime = new TaskRuntimeContext(packageVO, processVO, procInstVO, taskVO, taskInstanceVO, vars);
          } else {
              taskRunTime = new TaskRuntimeContext(null, null, null, taskVO, taskInstanceVO, vars);
          }


      } catch (Exception e) {
          logger.severeException("Failed to get Task Runtime context" + e.getMessage(), e);
      }
      return taskRunTime;
  }

  public void setIndexes(TaskRuntimeContext runtimeContext) throws DataAccessException {
      TaskIndexProvider indexProvider = getIndexProvider(runtimeContext);
      if (indexProvider != null) {
          Map<String,String> indexes = indexProvider.collect(runtimeContext);
          if (indexes != null)
              getTaskDAO().setTaskInstanceIndices(runtimeContext.getTaskInstanceId(), indexes);
      }
  }

  private TaskIndexProvider getIndexProvider(TaskRuntimeContext runtimeContext) throws DataAccessException {
      String indexProviderClass = runtimeContext.getTaskAttribute(TaskAttributeConstant.INDEX_PROVIDER);
      if (indexProviderClass == null) {
          return TaskTemplateCache.getTaskTemplate(runtimeContext.getTaskId()).isAutoformTask() ?
                  new AutoFormTaskIndexProvider() : new CustomTaskIndexProvider();
      }
      else {
          if (indexProviderClass.equals(AutoFormTaskIndexProvider.class.getName()))
              return new AutoFormTaskIndexProvider();
          else if (indexProviderClass.equals(CustomTaskIndexProvider.class.getName()))
              return new CustomTaskIndexProvider();

          TaskIndexProvider provider = TaskServiceRegistry.getInstance().getIndexProvider(runtimeContext.getPackage(), indexProviderClass);
          if (provider == null)
              logger.severe("ERROR: cannot create TaskIndexProvider: " + indexProviderClass);
          return provider;
      }
  }

  public Map<String,String> collectIndices(Long taskId, Long processInstanceId, FormDataDocument formdatadoc) throws DataAccessException {
      TaskVO task = TaskTemplateCache.getTaskTemplate(taskId);

      EventManager eventManager = ServiceLocator.getEventManager();
      Long procInstId = processInstanceId;
      if (task.isUsingIndices()) {
          try {
              if (processInstanceId != null && processInstanceId != 0L) {
                  ProcessInstanceVO prcInst = eventManager.getProcessInstance(processInstanceId);
                  ProcessVO procdef = ProcessVOCache.getProcessVO(prcInst.getProcessId());
                  procInstId = (prcInst.isNewEmbedded() || procdef.isEmbeddedProcess()) ? prcInst.getOwnerId() : prcInst.getId();
              }
              else
                  procInstId = null;
          }
          catch (Exception ex) {
             throw new DataAccessException("Failed to collect Task Indices: "+ex.getMessage());
          }

          Map<String,String> indices = new HashMap<String, String>();
          String v;
          if (task.isAutoformTask()) {
              String varstring = task.getAttribute(TaskAttributeConstant.VARIABLES);
              List<String[]> parsed = StringHelper.parseTable(varstring, ',', ';', 5);
              for (String[] one : parsed) {
                  String varname = one[0];
                  String displayOption = one[2];
                  String index_key = one[4];
                  if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED))
                      continue;
                  if (StringHelper.isEmpty(index_key))
                      continue;
                  String data = null;
                  if (formdatadoc != null) {
                      data = formdatadoc.getValue(varname);
                  } else if (procInstId != null) {
                      VariableInstanceInfo varInst = eventManager.getVariableInstance(procInstId, varname);
                      if (varInst.isDocument())
                          throw new DataAccessException("Document variable type not supported for indices: " + varname);
                      data = varInst.getStringValue();
                  }
                  if (!StringHelper.isEmpty(data)) {
                      indices.put(index_key, data);
                  }
              }
          }
          else {
              v = task.getAttribute(TaskAttributeConstant.INDICES);
              if (v != null && v.length() > 0) {
                  List<String[]> rows = StringHelper.parseTable(v, ',', ';', 2);
                  for (String[] row : rows) {
                      if (row[0] != null && row[1] != null
                              && row[0].length() > 0 && row[1].length() > 3) {
                          if (procInstId != null) {
                              VariableInstanceInfo varInst = eventManager.getVariableInstance(procInstId, row[1].substring(2, row[1].length() - 1));
                              if (varInst != null && !varInst.getStringValue().isEmpty()) {
                                  indices.put(row[0], varInst.getStringValue());
                              }
                          }
                      }
                  }
              }
          }
          return indices;
      }
      else
          return null;
  }
}

