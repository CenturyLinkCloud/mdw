/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.FormConstants;
import com.centurylink.mdw.constant.MiscConstants;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.note.InstanceNote;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskStatuses;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.observer.task.ParameterizedStrategy;
import com.centurylink.mdw.observer.task.PrioritizationStrategy;
import com.centurylink.mdw.observer.task.RoutingStrategy;
import com.centurylink.mdw.observer.task.SubTaskStrategy;
import com.centurylink.mdw.observer.task.TaskIndexProvider;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.task.TaskDataException;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.data.user.UserDataAccess;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.task.factory.TaskInstanceNotifierFactory;
import com.centurylink.mdw.services.task.factory.TaskInstanceStrategyFactory;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;
import com.centurylink.mdw.task.types.TaskServiceRegistry;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.qwest.mbeng.MbengException;

/**
 * Repackaged to separate business interface from implementation.
 */
public class TaskManagerBean implements TaskManager {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskDataAccess getTaskDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new TaskDataAccess(db);
    }

    /**
     * Returns the claimed tasks for a given user
     *
     * @param userId
     * @param criteria
     * @return Array of TaskInstanceVOs
     */
    public TaskInstance[] getAssignedTasks(Long userId, Map<String,String> criteria)
    throws TaskException, DataAccessException {
        return getAssignedTasks(userId, criteria, null, null);
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
    public TaskInstance[] getAssignedTasks(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria)
    throws TaskException, DataAccessException {
        return getAssignedTasks(userId, criteria, variables, variablesCriteria, null, null);
    }

   /**
    *
    */
    @Override
    public TaskInstance[] getAssignedTasks(Long userId, Map<String, String> criteria, List<String> variables, Map<String, String> variablesCriteria,
            List<String> indexKeys, Map<String, String> indexCriteria) throws TaskException, DataAccessException {
        TaskInstance[] retInstances = null;
        CodeTimer timer = new CodeTimer("TaskManager.getClaimedTaskInstanceVOs()", true);

        criteria.put("taskClaimUserId", " = " + userId);
        if (!(criteria.containsKey("statusCode"))) {
            criteria.put("statusCode", " in (" + TaskStatus.STATUS_ASSIGNED.toString() + ", " + TaskStatus.STATUS_IN_PROGRESS.toString() + ")");
        }

        List<TaskInstance> taskInstances = getTaskDAO().queryTaskInstances(criteria, variables, variablesCriteria, indexKeys, indexCriteria);

        // update instances flagged as invalid (missing template)
        for (TaskInstance taskInstance : taskInstances) {
            if (taskInstance.isInvalid()) {
                taskInstance.setStateCode(TaskState.STATE_INVALID);
            }
        }
        retInstances = taskInstances.toArray(new TaskInstance[taskInstances.size()]);
        timer.stopAndLogTiming("");
        return retInstances;
    }

    /**
     * Returns a task instance VO
     *
     * @param pTaskInstId
     * @return the taskInst and associated data
     * @throws TaskDataException
     * @throws DataAccessException
     */
    public TaskInstance getTaskInstanceVO(Long pTaskInstId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getTaskInstanceVO()", true);
        TaskInstance retVO = this.getTaskDAO().getTaskInstanceAllInfo(pTaskInstId);
        retVO.setTaskInstanceUrl(getTaskInstanceUrl(retVO));
        timer.stopAndLogTiming("");
        return retVO;
    }

    /**
     * Returns tasks instance identified by the primary key
     *
     * @param taskInstanceId
     */
    public TaskInstance getTaskInstance(Long taskInstanceId)
    throws DataAccessException {
        TaskInstance taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);
        if (taskInstance == null)
            return null;
        if (taskInstance.getAssigneeId() != null && taskInstance.getAssigneeId() != 0 && taskInstance.getAssigneeCuid() == null) {
            try {
                User user = UserGroupCache.getUser(taskInstance.getAssigneeId());
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
    public void getTaskInstanceAdditionalInfo(TaskInstance taskInst)
    throws DataAccessException, TaskException {
        getTaskDAO().getTaskInstanceAdditionalInfoGeneral(taskInst);
        taskInst.setTaskInstanceUrl(getTaskInstanceUrl(taskInst));
    }

    private TaskInstance createTaskInstance0(Long taskId, String owner, Long ownerId,
            String secondaryOwner, Long secondaryOwnerId, String message,
            String label, Date dueDate, String masterRequestId, int priority)
    throws TaskException, DataAccessException {

        TaskInstance ti = new TaskInstance();
        ti.setTaskId(taskId);
        ti.setOwnerType(owner);
        ti.setOwnerId(ownerId);
        ti.setSecondaryOwnerType(secondaryOwner);
        ti.setSecondaryOwnerId(secondaryOwnerId);
        ti.setStatusCode(TaskStatus.STATUS_OPEN);
        ti.setStateCode(TaskState.STATE_OPEN);
        ti.setComments(message);
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
    public TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId)
    throws TaskException, DataAccessException {
        return createTaskInstance(taskId, masterRequestId, procInstId, secOwner, secOwnerId, null);
    }

    /**
     * Convenience method for below.
     */
    public TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId, Map<String,String> indices)
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
    public TaskInstance createTaskInstance(Long taskId, Long procInstId,
                String secondaryOwner, Long secondaryOwnerId, String comment,
                String ownerApp, Long asgnTaskInstId, String taskName, int dueInSeconds,
                Map<String,String> indices, String assignee, String masterRequestId)
        throws TaskException, DataAccessException {
        TaskTemplate task = TaskTemplateCache.getTaskTemplate(taskId);
        String label = task.getLabel();
        Package taskPkg = PackageCache.getTaskTemplatePackage(taskId);
        if (taskPkg != null && !taskPkg.isDefaultPackage())
            label = taskPkg.getLabel() + "/" + label;
        Date dueDate = this.findDueDate(dueInSeconds, task);
        int pri = 0;
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

        TaskInstance ti = createTaskInstance0(taskId, (procInstId == null || procInstId == 0) ? OwnerType.EXTERNAL : OwnerType.PROCESS_INSTANCE,
                procInstId, secondaryOwner, secondaryOwnerId, comment, label, dueDate, masterRequestId, pri);
        ti.setTaskName(task.getTaskName()); // Reset task name back (without package name pre-pended)
        if (dueDate!=null) {
            int alertInterval = 0; //initialize
            String alertIntervalString = ""; //initialize

            alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            scheduleTaskSlaEvent(ti.getTaskInstanceId(), dueDate, alertInterval, false);
        }

        // create instance indices for template based general tasks (MDW 5.1) and all template based tasks (MDW 5.2)
        if (indices!=null && !indices.isEmpty()) {
            getTaskDAO().setTaskInstanceIndices(ti.getTaskInstanceId(), indices);
        }
        // create instance groups for template based tasks
        List<String> groups = determineWorkgroups(task, ti, indices);
        if (groups != null && groups.size() >0) {
            getTaskDAO().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));
            ti.setWorkgroups(groups);
        }

        if (assignee!=null) {
            try
            {
               User user =  UserGroupCache.getUser(assignee);
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
    public TaskInstance createTaskInstance(Long taskId, String masterOwnerId,
            String comment, Date dueDate, Long userId, Long documentId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.createTaskInstance()", true);
        TaskTemplate task = TaskTemplateCache.getTaskTemplate(taskId);
        int pri = 0; //AK added
        TaskInstance ti = createTaskInstance0(taskId,  OwnerType.USER, userId,
                (documentId != null?OwnerType.DOCUMENT : null), documentId, comment,
                task.getTaskName(), dueDate, masterOwnerId, pri);
        if (dueDate!=null) {
            String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            scheduleTaskSlaEvent(ti.getTaskInstanceId(), dueDate, alertInterval, false);
        }
        // create instance groups for template based tasks
        List<String> groups = determineWorkgroups(task, ti, null);
        if (groups != null && groups.size() >0)
            getTaskDAO().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));
        this.notifyTaskAction(ti, TaskAction.CREATE, null, null);   // notification/observer/auto-assign
        this.auditLogActionPerformed("Create", "MDW", Entity.TaskInstance, ti.getTaskInstanceId(),
                null, TaskTemplateCache.getTaskTemplate(taskId).getTaskName());
        timer.stopAndLogTiming("");
        return ti;
    }

    private Date findDueDate(int dueInSeconds, TaskTemplate task)
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
    private void assignTaskInstance(TaskInstance ti, Long userId)
    throws TaskException, DataAccessException {
        if (this.isTaskInstanceAssignable(ti)) {
            ti.setStatusCode(TaskStatus.STATUS_ASSIGNED);
            ti.setTaskClaimUserId(userId);
            Map<String,Object> changes = new HashMap<String,Object>();
            changes.put("TASK_CLAIM_USER_ID", userId);
            changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_ASSIGNED);
            getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);

            // update process variable with assignee - only for local tasks
            TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
            if (taskVO != null) {
                String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
                if (!StringHelper.isEmpty(assigneeVarSpec)) {
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
                                Variable doc = runtimeContext.getProcess().getVariable(rootVar);
                                VariableInstance varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                                String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getVariableType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                                if (varInst == null) {
                                    Long procInstId = runtimeContext.getProcessInstanceId();
                                    Long docId = eventMgr.createDocument(doc.getVariableType(), OwnerType.PROCESS_INSTANCE, procInstId, stringValue);
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
    private void releaseTaskInstance(TaskInstance ti)
    throws DataAccessException {
        ti.setStatusCode(TaskStatus.STATUS_OPEN);
        ti.setTaskClaimUserId(null);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_CLAIM_USER_ID", null);
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_OPEN);
        getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);

        // clear assignee process variable value
        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
        if (taskVO != null) { // not for invalid tasks
            String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
            if (!StringHelper.isEmpty(assigneeVarSpec)) {
                try {
                    TaskRuntimeContext runtimeContext = getTaskRuntimeContext(ti);
                    EventManager eventMgr = ServiceLocator.getEventManager();
                    if (runtimeContext.isExpression(assigneeVarSpec)) {
                        // create or update document variable referenced by expression
                        runtimeContext.set(assigneeVarSpec, null);
                        String rootVar = assigneeVarSpec.substring(2, assigneeVarSpec.indexOf('.'));
                        Variable doc = runtimeContext.getProcess().getVariable(rootVar);
                        VariableInstance varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                        String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getVariableType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                        if (varInst == null) {
                            Long procInstId = runtimeContext.getProcessInstanceId();
                            Long docId = eventMgr.createDocument(doc.getVariableType(), OwnerType.PROCESS_INSTANCE, procInstId, stringValue);
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
    private void workTaskInstance(TaskInstance ti)
    throws DataAccessException {
        ti.setStatusCode(TaskStatus.STATUS_IN_PROGRESS);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_IN_PROGRESS);
        getTaskDAO().updateTaskInstance(ti.getTaskInstanceId(), changes, false);
    }

    /**
     * Returns the list of task instances associated with the process instance
     *
     * @param pProcessInstanceId
     * @return Array of TaskInstance
     */
    private TaskInstance[] getAllTaskInstancesForProcessInstance(Long pProcessInstanceId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.getAllTaskInstancesForProcessInstance()", true);
        List<TaskInstance> daoResults = this.getTaskDAO()
                .getTaskInstancesForProcessInstance(pProcessInstanceId);
        timer.stopAndLogTiming("");
        return daoResults.toArray(new TaskInstance[daoResults.size()]);
    }

    /**
     * Checks if the Task Instance is open
     *
     * @param pTaskInstance
     * @return boolean status
     * @throws TaskException
     * @throws DataAccessException
     */
    private boolean isTaskInstanceOpen(TaskInstance pTaskInstance)
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
    private boolean isTaskInstanceAssignable(TaskInstance pTaskInstance)
    throws TaskException, DataAccessException {
        return isTaskInstanceOpen(pTaskInstance);
    }

    public void cancelTaskInstance(TaskInstance taskInst)
    throws TaskException, DataAccessException {
        if (taskInst.isInFinalStatus()) {
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
    public void cancelTasksForProcessInstance(Long pProcessInstId)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.cancelTaskInstancesForProcessInstance()", true);
        TaskInstance[] instances = this.getAllTaskInstancesForProcessInstance(pProcessInstId);
        if (instances == null || instances.length == 0) {
            timer.stopAndLogTiming("NoTaskInstances");
            return;
        }
        for (int i = 0; i < instances.length; i++) {
            instances[i].setComments("Task has been cancelled by ProcessInstance.");
            String instantStatus = instances[i].getStatus();
            if (instantStatus == null && ApplicationContext.isFileBasedAssetPersist()) {
                TaskStatus taskStatus = DataAccess.getBaselineData().getTaskStatuses().get(instances[i].getStatusCode());
                if (taskStatus != null)
                    instantStatus = taskStatus.getDescription();
            }
            if (!instances[i].isInFinalStatus()) {
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
     * Method that checks whether a task instance was created by an
     * embedded exception handler subprocess
     *
     * @param taskInstance
     * @return true if exceptionHandler created
     */
    private boolean isExceptionHandlerTaskInstance(TaskInstance taskInstance) {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            ProcessInstance pi = eventManager.getProcessInstance(taskInstance.getOwnerId());
            if (pi.isEmbedded())
                return true;
            Process processVO = ProcessCache.getProcess(pi.getProcessId());
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
    private void forwardTaskInstance(TaskInstance taskInst, String destination, String comment)
    throws TaskException, DataAccessException {
        List<String> prevWorkgroups = taskInst.getWorkgroups();
        if (prevWorkgroups == null || prevWorkgroups.isEmpty()) {
            TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
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
            sendNotification(taskInst, TaskAction.FORWARD, TaskAction.FORWARD);

            AutoAssignStrategy strategy = getAutoAssignStrategy(taskInst);
            if (strategy != null) {
                User assignee = strategy.selectAssignee(taskInst);
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

    public TaskInstance performActionOnTaskInstance(String action, Long taskInstanceId,
            Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine)
        throws TaskException, DataAccessException {
        return performActionOnTaskInstance(action, taskInstanceId, userId, assigneeId, comment, destination, notifyEngine, true);
    }

    public TaskInstance performActionOnTaskInstance(String action, Long taskInstanceId,
            Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine, boolean allowResumeEndpoint)
        throws TaskException, DataAccessException {

        if (logger.isInfoEnabled())
            logger.info("task action '" + action + "' on instance " + taskInstanceId);
        TaskInstance ti = getTaskDAO().getTaskInstanceAllInfo(taskInstanceId);
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
            TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
            if (taskVO != null)
                label = taskVO.getTaskName();
        }
        auditLogActionPerformed(action, userId, Entity.TaskInstance, taskInstanceId, assigneeCuid, label);

        if (isComplete) {
            // in case subTask
            if (ti.isSubTask()) {
                Long masterTaskInstId = ti.getMasterTaskInstanceId();
                boolean allCompleted = true;
                for (TaskInstance subTask : getSubTaskInstances(masterTaskInstId)) {
                    if (subTask.isInFinalStatus()) {
                        allCompleted = false;
                        break;
                    }
                }
                if (allCompleted) {
                    TaskInstance masterTaskInst = getTaskInstance(masterTaskInstId);
                    TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(masterTaskInst.getTaskId());
                    if ("true".equalsIgnoreCase(taskVO.getAttribute(TaskAttributeConstant.SUBTASKS_COMPLETE_MASTER)))
                        performActionOnTaskInstance(TaskAction.COMPLETE, masterTaskInstId, userId, null, null, null, notifyEngine, false);
                }
            }

            // in case master task
            for (TaskInstance subTask : getSubTaskInstances(ti.getTaskInstanceId())) {
                if (!subTask.isInFinalStatus())
                    cancelTaskInstance(subTask);
            }
        }


        return ti;
    }

    public List<SubTask> getSubTaskList(TaskRuntimeContext runtimeContext) throws TaskException {
        SubTaskPlan subTaskPlan = getSubTaskPlan(runtimeContext);
        if (subTaskPlan != null) {
            return subTaskPlan.getSubTaskList();
        }
        return null;
    }

    public List<TaskInstance> getSubTaskInstances(Long masterTaskInstanceId) throws DataAccessException {
        return getTaskDAO().getSubTaskInstances(masterTaskInstanceId);
    }

    private void resumeAutoFormTaskInstance(String taskAction, TaskInstance ti) throws TaskException {
        try {
            String eventName = FormConstants.TASK_CORRELATION_ID_PREFIX + ti.getTaskInstanceId().toString();
            FormDataDocument datadoc = new FormDataDocument();
            String formAction; // FIXME Aufoform
            if (taskAction.equals(TaskAction.CANCEL))
                formAction = "@CANCEL_TASK";
            else if (taskAction.equals(TaskAction.COMPLETE))
                formAction = "@COMPLETE_TASK";
            else {
                formAction = "@COMPLETE_TASK";
                datadoc.setMetaValue(FormConstants.URLARG_COMPLETION_CODE, taskAction);
            }
            datadoc.setAttribute(FormDataDocument.ATTR_ACTION, formAction);
            String message = datadoc.format();
            EventManager eventManager = ServiceLocator.getEventManager();
            Long docid = eventManager.createDocument(FormDataDocument.class.getName(),
                    OwnerType.TASK_INSTANCE, ti.getTaskInstanceId(), message);
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

    private void resumeCustomTaskInstance(String action, TaskInstance ti) throws TaskException {
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
            Long docid = eventManager.createDocument(XmlObject.class.getName(),
                    OwnerType.TASK_INSTANCE, ti.getTaskInstanceId(), message);
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

    private void notifyProcess(TaskInstance taskInstance, String eventName, Long eventInstId,
            String message, int delay) throws DataAccessException, EventException {
        EventManager eventManager = ServiceLocator.getEventManager();
        eventManager.notifyProcess(eventName, eventInstId, message, delay);
    }

    public void resumeThroughService(String taskResumeEndpoint, Long taskInstanceId, String action, Long userId, String comment)
        throws TaskException {
        UserTaskAction taskAction = new UserTaskAction();
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

    public void closeTaskInstance(TaskInstance ti, String action, String comment)
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
            UserDataAccess uda = new UserDataAccess(new DatabaseAccess(null));
            User user = uda.getUser(userId);
            userCuid = user.getCuid();
        }

        auditLogActionPerformed(action, userCuid, entity, entityId, destination, comments);
    }

    private void auditLogActionPerformed(String action, String user, Entity entity, Long entityId, String destination, String comments)
    throws TaskException, DataAccessException {
        try {
            if (user == null)
                user = "N/A";

            UserAction userAction = new UserAction(user, UserAction.getAction(action), entity, entityId, comments);
            userAction.setSource("Task Manager");
            userAction.setDestination(destination);
            if (userAction.getAction().equals(UserAction.Action.Other)) {
                if (action != null && action.startsWith(TaskAction.CANCEL + "::")) {
                    userAction.setAction(UserAction.Action.Cancel);
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
     * Creates a task instance note.
     */
    public Long addNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.addNote()", true);
        Long id = getTaskDAO().createInstanceNote(owner, ownerId, noteName, noteDetails, user);
        auditLogActionPerformed(UserAction.Action.Create.toString(), user, Entity.Note, id, null, noteName);
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
        auditLogActionPerformed(UserAction.Action.Change.toString(), user, Entity.Note, noteId, null, noteName);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates a note based on ownerId.
     */
    public void updateNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
    throws DataAccessException, TaskException {
        CodeTimer timer = new CodeTimer("TaskManager.updateNote()", true);
        getTaskDAO().updateInstanceNote(owner, ownerId, noteName, noteDetails, user);
        auditLogActionPerformed(UserAction.Action.Change.toString(), user, Entity.Note, ownerId, null, noteName);
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
        auditLogActionPerformed(UserAction.Action.Delete.toString(), userId, Entity.Note, noteId, null, null);
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
        auditLogActionPerformed(UserAction.Action.Create.toString(), user, Entity.Attachment, id, null, attachName);

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
        auditLogActionPerformed(UserAction.Action.Delete.toString(), userId, Entity.Attachment, pAttachId, null, null);
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
    public void notifyTaskAction(TaskInstance taskInstance, String action, Integer previousStatus, Integer previousState)
    throws TaskException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.notifyStatusChange()", true);

        try {
            // new-style notifiers
            String outcome = TaskStatuses.getTaskStatuses().get(taskInstance.getStatusCode());
            //if (!action.equals(TaskAction.CLAIM) && !action.equals(TaskAction.RELEASE))  // avoid nuisance notice to claimer and releaser
            sendNotification(taskInstance, action, outcome);

            // new-style auto-assign
            if (TaskStatus.STATUS_OPEN.intValue() == taskInstance.getStatusCode().intValue()
                    && !action.equals(TaskAction.RELEASE)) {
                AutoAssignStrategy strategy = getAutoAssignStrategy(taskInstance);
                if (strategy != null) {
                    User assignee = strategy.selectAssignee(taskInstance);
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

    public AutoAssignStrategy getAutoAssignStrategy(TaskInstance taskInstance) throws StrategyException, TaskException {
      TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
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
    private List<String> determineWorkgroups(TaskTemplate taskTemplate, TaskInstance taskInstance, Map<String,String> indices)
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
                TaskInstance taskInstance = runtimeContext.getTaskInstanceVO();
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

    private PrioritizationStrategy getPrioritizationStrategy(TaskTemplate taskTemplate, Long processInstanceId, Long formDataDocId, Map<String,String> indices)
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

    private void populateStrategyParams(ParameterizedStrategy strategy, TaskTemplate taskTemplate, Long processInstId, Long formDataDocId, Map<String,String> indices)
    throws DataAccessException, MbengException {
        Package pkg = null;
        for (Attribute attr : taskTemplate.getAttributes()) {
            strategy.setParameter(attr.getAttributeName(), attr.getAttributeValue());
        }
        EventManager eventManager = ServiceLocator.getEventManager();
        try {
            Process procdef = eventManager.findProcessByProcessInstanceId(processInstId);
            pkg = PackageCache.getProcessPackage(procdef.getProcessId());
        } catch (Exception ex) {
            logger.severeException("Failed to get the process Package information : "+processInstId, ex);
        }

        if (taskTemplate.isAutoformTask() && formDataDocId != null) {
            Document docvo = eventManager.getDocumentVO(formDataDocId);
            FormDataDocument datadoc = new FormDataDocument();
            datadoc.load(docvo.getContent());
            List<VariableInstance> varInsts = new ArrayList<VariableInstance>(); // FIXME Autoform
            for (VariableInstance varInst : varInsts) {
                Object varVal = varInst.getData();
                if (varInst.isDocument()) {
                    varVal = varInst.getData(); // FIXME AutoForm
                }
                strategy.setParameter(varInst.getName(), varVal);
            }
        }
        else {
            for (VariableInstance varInst : getProcessInstanceVariables(processInstId)) {
                Object varVal = varInst.getData();
                if (varInst.isDocument())
                    varVal = eventManager.getDocumentVO(((DocumentReference)varVal).getDocumentId()).getObject(varInst.getType(), pkg);
                strategy.setParameter(varInst.getName(), varVal);
            }
        }
    }

    /**
     * get the task instance for the activity instance.
     * For general tasks, the process instance ID is needed
     * to loop through all task instances of the process.
     *
     * @param activityInstId activity instance ID
     * @param pProcessInstId process instance ID for general tasks; null for classic tasks
     */
    public TaskInstance getTaskInstanceByActivityInstanceId(Long activityInstanceId,
            Long procInstId)
    throws TaskException, DataAccessException {
        TaskInstance taskInst;
        if (procInstId==null) {
            taskInst = getTaskDAO().getTaskInstanceByActivityInstanceId(activityInstanceId);
        } else {
            List<TaskInstance> taskInstList = getTaskDAO().getTaskInstancesForProcessInstance(procInstId);
            taskInst = null;
            for (TaskInstance one : taskInstList) {
                if (!one.getSecondaryOwnerType().equals(OwnerType.DOCUMENT)) continue;
                Document doc = getTaskInstanceData(one);
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
        EventInstance event = eventManager.getEventInstance(ScheduledEvent.SPECIAL_EVENT_PREFIX + "TaskDueDate." + pTaskInstanceId.toString());
        boolean isEventExist = event == null ? false : true;
        hasOldSlaInstance = !isEventExist;
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("DUE_DATE", pDueDate);
        changes.put("COMMENTS", comment);
        getTaskDAO().updateTaskInstance(pTaskInstanceId, changes, false);
        if (pDueDate==null) {
            this.unscheduleTaskSlaEvent(pTaskInstanceId);
        } else {
            TaskInstance taskInst = getTaskDAO().getTaskInstance(pTaskInstanceId);
            TaskTemplate task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
            String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            this.scheduleTaskSlaEvent(pTaskInstanceId, pDueDate, alertInterval, !hasOldSlaInstance);
        }
        auditLogActionPerformed(UserAction.Action.Change.toString(), cuid,
                Entity.TaskInstance, pTaskInstanceId, null, "change due date / comments");
    }

    public void updateTaskInstanceComments(Long taskInstanceId, String comments)
    throws TaskException, DataAccessException {
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("COMMENTS", comments);
        getTaskDAO().updateTaskInstance(taskInstanceId, changes, false);
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
        TaskInstance taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);

        try {
            Long activityInstanceId = getActivityInstanceId(taskInstance, true);

            if (activityInstanceId != null) {
                EventManager eventManager = ServiceLocator.getEventManager();
                ActivityInstance activityInstance = eventManager.getActivityInstance(activityInstanceId);
                Long processInstanceId = activityInstance.getOwnerId();
                ProcessInstance processInstance = eventManager.getProcessInstance(processInstanceId);

                Process processVO = ProcessCache.getProcess(processInstance.getProcessId());
                if (processInstance.isEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<Transition> outgoingWorkTransVOs = processVO.getAllWorkTransitions(activityInstance.getDefinitionId());
                for (Transition workTransVO : outgoingWorkTransVOs) {
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
        TaskInstance taskInstance = getTaskDAO().getTaskInstance(taskInstanceId);

        try {
            Long activityInstanceId = getActivityInstanceId(taskInstance, true);

            if (activityInstanceId != null) {
                EventManager eventManager = ServiceLocator.getEventManager();
                ActivityInstance activityInstance = eventManager.getActivityInstance(activityInstanceId);
                Long processInstanceId = activityInstance.getOwnerId();
                ProcessInstance processInstance = eventManager.getProcessInstance(processInstanceId);

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

                Process processVO = ProcessCache.getProcess(processInstance.getProcessId());
                if (processInstance.isEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<Transition> outgoingWorkTransVOs = processVO.getAllWorkTransitions(activityInstance.getDefinitionId());
                boolean foundNullResultCode = false;
                for (Transition workTransVO : outgoingWorkTransVOs) {
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
    public Long getActivityInstanceId(TaskInstance taskInstance, boolean sourceActInst)
    {
      Long activityInstanceId = null;
      TaskInstance taskInst = taskInstance;
      try
      {
        if (!OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
            return null;
        EventManager eventManager = ServiceLocator.getEventManager();
        if (sourceActInst && isExceptionHandlerTaskInstance(taskInstance)) {
            ProcessInstance subProcessInstance = eventManager.getProcessInstance(taskInstance.getOwnerId());
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
                TransitionInstance workTransInst = eventManager.getWorkTransitionInstance(workTransInstId);
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

    public Document getTaskInstanceData(TaskInstance taskInst) throws DataAccessException {
        EventManager eventManager = ServiceLocator.getEventManager();
        return eventManager.getDocumentVO(taskInst.getSecondaryOwnerId());
    }

    public VariableInstance[] getProcessInstanceVariables(Long procInstId)
    throws DataAccessException {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            ProcessInstance procInst = eventManager.getProcessInstance(procInstId);
            List<VariableInstance> vars;
            if (procInst.isEmbedded())
                vars = eventManager.getProcessInstanceVariables(procInst.getOwnerId());
            else vars = eventManager.getProcessInstanceVariables(procInstId);
            VariableInstance[] retVars = new VariableInstance[vars.size()];
            for (int i=0; i<vars.size(); i++) {
                VariableInstance v = vars.get(i);
                VariableInstance retv = new VariableInstance();
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

    public List<String> getGroupsForTaskInstance(TaskInstance taskInst) throws DataAccessException, TaskException {
        if (taskInst.isShallow()) this.getTaskInstanceAdditionalInfo(taskInst);
        if (taskInst.isTemplateBased()) return taskInst.getGroups();
        else return getTaskDAO().getGroupsForTask(taskInst.getTaskId());
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


  public TaskRuntimeContext getTaskRuntimeContext(TaskInstance taskInstanceVO) {
      EventManager eventMgr = null;
      TaskRuntimeContext taskRunTime = null;

      try {
          eventMgr = ServiceLocator.getEventManager();
          TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskInstanceVO.getTaskId());
          Map<String,Object> vars = new HashMap<String,Object>();

          if(OwnerType.PROCESS_INSTANCE.equals(taskInstanceVO.getOwnerType())) {
              ProcessInstance procInstVO = eventMgr.getProcessInstance(taskInstanceVO.getOwnerId(), true);
                if (procInstVO.isEmbedded()) {
                    Long procInstanceId = procInstVO.getOwnerId();
                    procInstVO = eventMgr.getProcessInstance(procInstanceId, true);
                }
                Long processId = procInstVO.getProcessId();
              Process processVO = ProcessCache.getProcess(processId);
              Package packageVO = PackageCache.getProcessPackage(processId);

              if (procInstVO.getVariables() != null) {
                  for (VariableInstance var : procInstVO.getVariables()) {
                      Object value = var.getData();
                      if (value instanceof DocumentReference) {
                          try {
                              Document docVO = eventMgr.getDocument((DocumentReference)value, false);
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

   public void cancelTasksOfActivityInstance(Long actInstId, Long procInstId)
            throws NamingException, MDWException {
        TaskInstance taskInstance = getTaskInstanceByActivityInstanceId(actInstId,
                procInstId == null ? null : new Long(procInstId));
        if (taskInstance == null)
            throw new TaskException("Cannot find the task instance for the activity instance");
        if (taskInstance.getStatusCode().equals(TaskStatus.STATUS_ASSIGNED)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_IN_PROGRESS)
                || taskInstance.getStatusCode().equals(TaskStatus.STATUS_OPEN)) {
            cancelTaskInstance(taskInstance);
        }
    }

    public void cancelTasksForProcessInstances(List<Long> procInstIds)
            throws TaskException, DataAccessException {
        for (Long procInstId : procInstIds) {
            cancelTasksForProcessInstance(procInstId);
        }
    }

    public String getTaskInstanceUrl(TaskInstance taskInst) throws TaskException {
        String baseUrl = ApplicationContext.getMdwHubUrl();
        if (!baseUrl.endsWith("/"))
          baseUrl += "/";
        return baseUrl + "#/tasks/" + taskInst.getTaskInstanceId();
    }

    private void sendNotification(TaskInstance taskInst, String action, String outcome) {
        try {
            TaskInstanceNotifierFactory notifierFactory = TaskInstanceNotifierFactory.getInstance();
            List<String> notifierSpecs = new ArrayList<String>();
            Long processInstId = OwnerType.PROCESS_INSTANCE.equals(taskInst.getOwnerType()) ? taskInst.getOwnerId() : null;
            notifierSpecs = notifierFactory.getNotifierSpecs(taskInst.getTaskId(), processInstId, outcome);
            if (notifierSpecs == null || notifierSpecs.isEmpty()) return;
            getTaskInstanceAdditionalInfo(taskInst);
            TaskRuntimeContext taskRuntime = getTaskRuntimeContext(taskInst);
            for (String notifierSpec : notifierSpecs) {
                try {
                    TaskNotifier notifier = notifierFactory.getNotifier(notifierSpec, processInstId);
                    if (notifier != null) {
                        notifier.sendNotice(taskRuntime, action, outcome);
                    }
                }
                catch (Exception ex) {
                    // don't let one notifier failure prevent others from processing
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        } catch (Exception e) {
            logger.severeException("Failed to send email notification for task instance "
                    + taskInst.getTaskInstanceId(), e);
        }
    }

}

