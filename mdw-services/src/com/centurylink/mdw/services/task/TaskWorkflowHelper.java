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

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
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
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
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
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.data.user.UserDataAccess;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.asset.CustomPageLookup;
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

public class TaskWorkflowHelper {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskInstance taskInstance;
    TaskInstance getTaskInstance() { return taskInstance; }

    TaskWorkflowHelper(TaskInstance taskInstance) {
        this.taskInstance = taskInstance;
    }

    TaskWorkflowHelper(Long taskInstanceId) throws DataAccessException {
        this.taskInstance = getTaskInstance(taskInstanceId);
    }

    /**
     * Convenience method for below.
     */
    static TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId,
            String secOwner, Long secOwnerId, String title, String comments) throws ServiceException, DataAccessException {
        return createTaskInstance(taskId, masterRequestId, procInstId, secOwner, secOwnerId, title, comments, 0, null, null);
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
     * @param masterRequestId
     * @param processInstanceId    process instance ID
     * @param secondaryOwner    can be DOCUMENT (general task), TASK_INSTANCE (subtask) or WORK_TRANSITION_INSTANCE (for classic task)
     * @param secondaryOwnerId  document ID or transition instance ID
     * @param title  Taken from task template name when not populated
     * @param comments  message (typically stacktrace for fallout tasks) of the activity for classic task
     * @param dueInSeconds  SLA. When it is 0, check if template has specified SLA
     * @param indexes   indices for general task based on templates
     * @param assignee  assignee CUID if this is to be auto-assigned by process variable
     * @return TaskInstance
     */
    static TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long processInstanceId,
                String secondaryOwner, Long secondaryOwnerId, String title, String comments,
                int dueInSeconds, Map<String,String> indexes, String assignee)
        throws ServiceException, DataAccessException {
        TaskTemplate task = TaskTemplateCache.getTaskTemplate(taskId);
        String label = task.getLabel();
        Package taskPkg = PackageCache.getTaskTemplatePackage(taskId);
        if (taskPkg != null && !taskPkg.isDefaultPackage())
            label = taskPkg.getLabel() + "/" + label;
        Instant due = null;
        dueInSeconds = dueInSeconds > 0 ? dueInSeconds : task.getSlaSeconds();
        if (dueInSeconds > 0)
            due = Instant.now().plusSeconds(dueInSeconds);
        int pri = 0;
        // use the prioritization strategy if one is defined for the task
        try {
            PrioritizationStrategy prioritizationStrategy = getPrioritizationStrategy(task, processInstanceId, indexes);
            if (prioritizationStrategy != null) {
                Date calcDueDate = prioritizationStrategy.determineDueDate(task);
                if (calcDueDate != null)
                    due = calcDueDate.toInstant();
                pri = prioritizationStrategy.determinePriority(task, due == null ? null : Date.from(due));
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }

        TaskInstance ti = createTaskInstance(taskId, masterRequestId, OwnerType.PROCESS_INSTANCE,
                processInstanceId, secondaryOwner, secondaryOwnerId, label, title, comments, due, pri);
        ti.setTaskName(task.getTaskName()); // Reset task name back (without package name pre-pended)
        TaskWorkflowHelper helper = new TaskWorkflowHelper(ti);
        if (due != null) {
            int alertInterval = 0; //initialize
            String alertIntervalString = ""; //initialize

            alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            helper.scheduleTaskSlaEvent(Date.from(due), alertInterval, false);
        }

        // create instance indices for template based general tasks (MDW 5.1) and all template based tasks (MDW 5.2)
        if (indexes!=null && !indexes.isEmpty()) {
            new TaskDataAccess().setTaskInstanceIndices(helper.getTaskInstance().getTaskInstanceId(), indexes);
        }
        // create instance groups for template based tasks
        List<String> groups = helper.determineWorkgroups(indexes);
        if (groups != null && groups.size() >0) {
            new TaskDataAccess().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));
            helper.getTaskInstance().setWorkgroups(groups);
        }

        if (assignee != null) {
            try
            {
               User user =  UserGroupCache.getUser(assignee);
               if(user == null)
                 throw new ServiceException("Assignee user not found to perform auto-assign : " + assignee);
               helper.assign(user.getId()); // Performing auto assign on summary
            }
            catch (CachingException e)
            {
              logger.severeException(e.getMessage(), e);
            }
        }

        helper.getTaskInstance().setTaskInstanceUrl(helper.getTaskInstanceUrl());
        helper.notifyTaskAction(TaskAction.CREATE, null, null);   // notification/observer/auto-assign
        helper.auditLog("Create", "MDW");

        TaskRuntimeContext runtimeContext = helper.getContext();
        helper.setIndexes(runtimeContext);
        List<SubTask> subtasks = helper.getSubtaskList(runtimeContext);
        if (subtasks != null && !subtasks.isEmpty())
            helper.createSubtasks(subtasks, runtimeContext);

        return ti;
    }

    /**
     * This version is used by the task manager to create a task instance
     * not associated with a process instance.
     *
     * @param taskId
     * @param masterOwnerId
     * @param comment optional
     * @param due optional
     * @param userId
     * @param secondaryOwner (optional)
     * @return TaskInstance
     */
    static TaskInstance createTaskInstance(Long taskId, String masterOwnerId,
            String title, String comment, Instant due, Long userId, Long secondaryOwner)
    throws ServiceException, DataAccessException {
        CodeTimer timer = new CodeTimer("createTaskInstance()", true);
        TaskTemplate task = TaskTemplateCache.getTaskTemplate(taskId);
        TaskInstance ti = createTaskInstance(taskId,  masterOwnerId, OwnerType.USER, userId,
                (secondaryOwner != null ? OwnerType.DOCUMENT : null), secondaryOwner,
                task.getTaskName(), title, comment, due, 0);

        TaskWorkflowHelper helper = new TaskWorkflowHelper(ti);

        if (due!=null) {
            String alertIntervalString = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString) ? 0 : Integer.parseInt(alertIntervalString);
            helper.scheduleTaskSlaEvent(Date.from(due), alertInterval, false);
        }
        // create instance groups for template based tasks
        List<String> groups = helper.determineWorkgroups(null);
        if (groups != null && groups.size() > 0)
            new TaskDataAccess().setTaskInstanceGroups(ti.getTaskInstanceId(), StringHelper.toStringArray(groups));

        helper.notifyTaskAction(TaskAction.CREATE, null, null);   // notification/observer/auto-assign
        helper.auditLog("Create", "MDW");
        timer.stopAndLogTiming("");
        return ti;
    }

    static TaskInstance createTaskInstance(Long taskId, String masterRequestId, String owner, Long ownerId,
            String secondaryOwner, Long secondaryOwnerId, String label, String title, String message, Instant due, int priority)
    throws ServiceException, DataAccessException {

        TaskInstance ti = new TaskInstance();
        ti.setTaskId(taskId);
        ti.setOwnerType(owner);
        ti.setOwnerId(ownerId);
        ti.setSecondaryOwnerType(secondaryOwner);
        ti.setSecondaryOwnerId(secondaryOwnerId);
        ti.setStatusCode(TaskStatus.STATUS_OPEN);
        ti.setStateCode(TaskState.STATE_OPEN);
        ti.setComments(message);
        ti.setTaskName(label); // actually populates referred_as
        ti.setTitle(title);
        ti.setMasterRequestId(masterRequestId);
        ti.setPriority(priority);
        ti.setDue(due);
        Long id = new TaskDataAccess().createTaskInstance(ti, due == null ? null : Date.from(due));
        ti.setTaskInstanceId(id);
        return ti;
    }

    static PrioritizationStrategy getPrioritizationStrategy(TaskTemplate taskTemplate, Long processInstanceId, Map<String,String> indexes)
    throws DataAccessException, StrategyException, ServiceException {
        String priorityStrategyAttr = taskTemplate.getAttribute(TaskAttributeConstant.PRIORITY_STRATEGY);
        if (StringHelper.isEmpty(priorityStrategyAttr)) {
            return null;
        }
        else {
            PrioritizationStrategy strategy = TaskInstanceStrategyFactory.getPrioritizationStrategy(priorityStrategyAttr, processInstanceId);
            if (strategy instanceof ParameterizedStrategy) {
                populateStrategyParams((ParameterizedStrategy)strategy, taskTemplate, processInstanceId, indexes);
            }
            return strategy;
        }
    }


    static TaskInstance getTaskInstance(Long taskInstanceId) throws DataAccessException {
        TaskInstance taskInstance = new TaskDataAccess().getTaskInstance(taskInstanceId);
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
        Long activityInstanceId = new TaskWorkflowHelper(taskInstance).getActivityInstanceId(false);
        if (activityInstanceId != null) {
            taskInstance.setActivityInstanceId(activityInstanceId);
        }
        TaskTemplate template = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
        if (template != null)
            taskInstance.setTemplate(template.getPackageName() + "/" + template.getName());
        return taskInstance;
    }

    void setIndexes(TaskRuntimeContext runtimeContext) throws DataAccessException, ServiceException {
        TaskIndexProvider indexProvider = getIndexProvider(runtimeContext);
        if (indexProvider != null) {
            Map<String,String> indexes = indexProvider.collect(runtimeContext);
            if (indexes != null)
                new TaskDataAccess().setTaskInstanceIndices(runtimeContext.getTaskInstanceId(), indexes);
        }
    }

    /**
     * Updates the due date for a task instance.
     * The method should only be called in summary (or summary-and-detail) task manager.
     */
    void updateDue(Instant due, String cuid, String comment)
    throws ServiceException, DataAccessException {
        boolean hasOldSlaInstance;
        EventServices eventManager = ServiceLocator.getEventServices();
        EventInstance event = eventManager.getEventInstance(ScheduledEvent.SPECIAL_EVENT_PREFIX + "TaskDueDate." + taskInstance.getId());
        boolean isEventExist = event == null ? false : true;
        hasOldSlaInstance = !isEventExist;
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("DUE_DATE", Date.from(due));
        changes.put("COMMENTS", comment);
        TaskDataAccess dataAccess = new TaskDataAccess();
        dataAccess.updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);
        if (due == null) {
            unscheduleTaskSlaEvent();
        }
        else {
            String alertIntervalString = getTemplate().getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
            int alertInterval = StringHelper.isEmpty(alertIntervalString)?0:Integer.parseInt(alertIntervalString);
            scheduleTaskSlaEvent(Date.from(due), alertInterval, !hasOldSlaInstance);
        }
        auditLog(UserAction.Action.Change.toString(), cuid, null, "change due date / comments");
    }

    void updateComments(String comments)
    throws ServiceException, DataAccessException {
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("COMMENTS", comments);
        new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);
    }

    public void updateWorkgroups(List<String> groups) throws DataAccessException {
        new TaskDataAccess().setTaskInstanceGroups(taskInstance.getTaskInstanceId(), groups.toArray(new String[0]));
    }

    public void updatePriority(Integer priority) throws DataAccessException {
        new TaskDataAccess().setTaskInstancePriority(taskInstance.getTaskInstanceId(), priority);
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


    void performAction(String action, Long userId, Long assigneeId, String comment,
            String destination, boolean notifyEngine) throws ServiceException, DataAccessException {
        performAction(action, userId, assigneeId, comment, destination, notifyEngine, true);
    }

    void performAction(String action, Long userId, Long assigneeId, String comment,
            String destination, boolean notifyEngine, boolean allowResumeEndpoint)
            throws ServiceException, DataAccessException {

        if (logger.isInfoEnabled())
            logger.info("task action '" + action + "' on instance " + taskInstance.getId());

        if (taskInstance.isShallow())
            getTaskInstanceAdditionalInfo();
        // verifyPermission(ti, action, userId);
        Integer prevStatus = taskInstance.getStatusCode();
        Integer prevState = taskInstance.getStateCode();
        String assigneeCuid = null;

        boolean isComplete = false;

        // special behavior for some types of actions
        if (action.equalsIgnoreCase(TaskAction.ASSIGN) || action.equalsIgnoreCase(TaskAction.CLAIM)) {
            if (assigneeId == null || assigneeId.longValue() == 0) {
                assigneeId = userId;
            }
            assign(assigneeId);
            try {
                User user = UserGroupCache.getUser(assigneeId);
                if (user != null)
                    assigneeCuid = user.getCuid();
            }
            catch (CachingException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        else if (action.equalsIgnoreCase(TaskAction.RELEASE)) {
            release();
        }
        else if (action.equalsIgnoreCase(TaskAction.WORK)) {
            work();
        }
        else if (action.equalsIgnoreCase(TaskAction.FORWARD)) {
            forward(destination, comment);
            auditLog(action, userId, destination, comment);
            return;  // forward notifications are handled in forwardTaskInstance()
        }
        else {
            isComplete = true;
            TaskRuntimeContext runtimeContext = getContext();
            // update the indexes
            setIndexes(runtimeContext);
            // option to notify through service (eg: to offload to workflow server instance)
            String taskResumeEndpoint = null;
            if (taskInstance.isProcessOwned() && allowResumeEndpoint)
                taskResumeEndpoint = runtimeContext.getProperty(PropertyNames.TASK_RESUME_NOTIFY_ENDPOINT);
            if (taskResumeEndpoint == null) // otherwise it will be closed via service
                close(action, comment);
            if (notifyEngine && !taskInstance.isSubTask()) {
                if (taskResumeEndpoint == null) {
                    resume(action);
                }
                else {
                    // resume through service
                    resumeThroughService(taskResumeEndpoint, action, userId, comment);
                    return; // notify and audit log at endpoint destination
                }
            }
        }

        notifyTaskAction(action, prevStatus, prevState);
        auditLog(action, userId, assigneeCuid, comment);

        if (isComplete) {
            // in case subtask
            if (taskInstance.isSubTask()) {
                Long masterTaskInstId = taskInstance.getMasterTaskInstanceId();
                boolean allCompleted = true;
                for (TaskInstance subtask : getSubtasks(masterTaskInstId)) {
                    if (!subtask.isInFinalStatus()) {
                        allCompleted = false;
                        break;
                    }
                }
                if (allCompleted) {
                    TaskInstance masterTaskInst = ServiceLocator.getTaskServices().getInstance(masterTaskInstId);
                    TaskWorkflowHelper masterHelper = new TaskWorkflowHelper(masterTaskInst);
                    if ("true".equalsIgnoreCase(masterHelper.getTemplate().getAttribute(TaskAttributeConstant.SUBTASKS_COMPLETE_MASTER)))
                        masterHelper.performAction(TaskAction.COMPLETE, userId, null, null, null, notifyEngine, false);
                }
            }

            // in case master task
            for (TaskInstance subtask : getSubtasks(taskInstance.getTaskInstanceId())) {
                if (!subtask.isInFinalStatus())
                    new TaskWorkflowHelper(subtask).cancel();
            }
        }
    }

    List<SubTask> getSubtaskList(TaskRuntimeContext runtimeContext) throws ServiceException {
        String subtaskStrategyAttr = getTemplate().getAttribute(TaskAttributeConstant.SUBTASK_STRATEGY);
        if (StringHelper.isEmpty(subtaskStrategyAttr)) {
            return null;
        }
        else {
            try {
                SubTaskStrategy strategy = TaskInstanceStrategyFactory.getSubTaskStrategy(subtaskStrategyAttr,
                        OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
                if (strategy instanceof ParameterizedStrategy) {
                    populateStrategyParams((ParameterizedStrategy)strategy, getTemplate(), taskInstance.getOwnerId(), null);
                }
                XmlOptions xmlOpts = Compatibility.namespaceOptions().setDocumentType(SubTaskPlanDocument.type);
                SubTaskPlanDocument subTaskPlanDoc = SubTaskPlanDocument.Factory.parse(strategy.getSubTaskPlan(runtimeContext), xmlOpts);
                SubTaskPlan plan = subTaskPlanDoc.getSubTaskPlan();
                return plan.getSubTaskList();
            }
            catch (Exception ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
    }

    void createSubtasks(List<SubTask> subtasks, TaskRuntimeContext masterTaskContext)
            throws ServiceException, DataAccessException {
        for (SubTask subtask : subtasks) {
            TaskTemplate subtaskTemplate = TaskTemplateCache.getTaskTemplate(subtask.getLogicalId());
            if (subtaskTemplate == null)
                throw new ServiceException("Task Template '" + subtask.getLogicalId() + "' does not exist");

            TaskInstance subtaskInstance = createTaskInstance(subtaskTemplate.getTaskId(), masterTaskContext.getMasterRequestId(),
                    masterTaskContext.getProcessInstanceId(), OwnerType.TASK_INSTANCE, masterTaskContext.getTaskInstanceId(), null, null);
            logger.info("Subtask instance created - ID " + subtaskInstance.getTaskInstanceId());
            // TODO recursive subtask creation
        }
    }

    List<TaskInstance> getSubtasks(Long masterTaskInstanceId) throws DataAccessException {
        return new TaskDataAccess().getSubtaskInstances(masterTaskInstanceId);
    }

    void resume(String action) throws ServiceException {
        // resume through engine
        if (getTemplate().isAutoformTask())
            resumeAutoForm(action);
        else
            resumeCustom(action);
    }

    public void resumeThroughService(String taskResumeEndpoint, String action, Long userId,
            String comment) throws ServiceException {
        UserTaskAction taskAction = new UserTaskAction();
        taskAction.setTaskInstanceId(taskInstance.getTaskInstanceId());
        taskAction.setTaskAction(action);
        try {
            taskAction.setUser(UserGroupCache.getUser(userId).getCuid());
            taskAction.setComment(comment);
            // Send request to new endpoint, while preventing infinte loop with
            // new Query parameter
            HttpHelper helper = new HttpHelper(new URL(taskResumeEndpoint + "/Services/Tasks/"
                    + taskInstance.getId() + "/" + action + "?disableEndpoint=true"));
            String response = helper.post(taskAction.getJson().toString(2));
            StatusMessage statusMessage = new StatusMessage(new JsonObject(response));
            if (statusMessage.getCode() != 0)
                throw new ServiceException(
                        "Failure response resuming task instance " + taskInstance.getId() + " at "
                                + taskResumeEndpoint + ": " + statusMessage.getMessage());
        }
        catch (Exception ex) {
            throw new ServiceException("Failed to resume task instance: " + taskInstance.getId(), ex);
        }
    }

    private void resumeAutoForm(String taskAction) throws ServiceException {
        try {
            String eventName = TaskAttributeConstant.TASK_CORRELATION_ID_PREFIX + taskInstance.getId();
            JSONObject jsonMsg = new JsonObject();
            String formAction;
            if (taskAction.equals(TaskAction.CANCEL))
                formAction = "@CANCEL_TASK";
            else if (taskAction.equals(TaskAction.COMPLETE))
                formAction = "@COMPLETE_TASK";
            else {
                formAction = "@COMPLETE_TASK";
                jsonMsg.put(TaskAttributeConstant.URLARG_COMPLETION_CODE, taskAction);
            }
            jsonMsg.put(TaskAttributeConstant.TASK_ACTION, formAction);
            JSONObject jsonMeta = new JsonObject().put("META", jsonMsg);
            Long docId = createDocument(JSONObject.class.getName(), jsonMeta);
            String av = PropertyManager.getProperty(PropertyNames.ACTIVITY_RESUME_DELAY);
            int delay = 2;
            if (av!=null) {
                // delay some seconds to avoid race condition
                try {
                    delay = Integer.parseInt(av);
                    if (delay < 0)
                        delay = 0;
                    else if (delay > 300)
                        delay = 300;
                } catch (Exception e) {
                }
            }
            notifyProcess(eventName, docId, jsonMeta.toString(2), delay);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private void resumeCustom(String action) throws ServiceException {
        try {
            Long actInstId = getActivityInstanceId(false);

            String correlationId = "TaskAction-" + actInstId.toString();
            ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
            ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
            Action actionItem = actionRequest.addNewAction();
            actionItem.setName("TaskAction");
            Parameter param = actionItem.addNewParameter();
            param.setName("Action");
            param.setStringValue(action);

            Long docId = createDocument(XmlObject.class.getName(), actionRequestDoc);
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
            notifyProcess(correlationId, docId, actionRequestDoc.xmlText(), delay);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private void notifyProcess(String eventName, Long eventInstId,
            String message, int delay) throws DataAccessException, EventException {
        EventServices eventManager = ServiceLocator.getEventServices();
        eventManager.notifyProcess(eventName, eventInstId, message, delay);
    }

    private Long createDocument(String type, Object value) throws DataAccessException, ServiceException {
        if (!OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
            throw new DataAccessException("Invalid owner for creating task doc: "
                    + taskInstance.getOwnerType() + " (" + taskInstance.getId() + ")");

        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        ProcessInstance proc = workflowServices.getProcess(taskInstance.getOwnerId());
        Package pkg = PackageCache.getProcessPackage(proc.getProcessId());
        EventServices eventMgr = ServiceLocator.getEventServices();
        return eventMgr.createDocument(type, OwnerType.TASK_INSTANCE, taskInstance.getTaskInstanceId(), value, pkg);
    }

    public Long getActivityInstanceId(boolean sourceActInst)
    {
      Long activityInstanceId = null;
      TaskInstance taskInst = taskInstance;
      try
      {
        if (!OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
            return null;
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        ProcessInstance procInst = workflowServices.getProcess(taskInstance.getOwnerId());
        if (sourceActInst && procInst.isEmbedded()) {
            // subprocess secondary owner is activity instance
            activityInstanceId = procInst.getSecondaryOwnerId();
        }
        else {
            // Master/Sub task
            if (OwnerType.TASK_INSTANCE.equals(taskInstance.getSecondaryOwnerType())) {
                // secondary owner id refers to master task instance id
                taskInst = new TaskDataAccess().getTaskInstance(taskInstance.getSecondaryOwnerId());
            }
            // task instance secondary owner is work transition instance
            Long workTransInstId = taskInst.getSecondaryOwnerId();
            TransitionInstance workTransInst = ServiceLocator.getEventServices().getWorkTransitionInstance(workTransInstId);
            activityInstanceId = workTransInst.getDestinationID();
        }
      }
      catch (Exception ex) {
        logger.severeException(ex.getMessage(), ex);
      }
      return activityInstanceId;
    }

    ActivityInstance getActivityInstance(boolean sourceActInst) throws DataAccessException, ServiceException {
        try {
            Long activityInstanceId = getActivityInstanceId(sourceActInst);
            if (activityInstanceId == null)
                return null;
            return ServiceLocator.getEventServices().getActivityInstance(activityInstanceId);
        }
        catch (ProcessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    void scheduleTaskSlaEvent(Date dueDate, int alertInterval, boolean isReschedule) {
        ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
        String eventName = ScheduledEvent.SPECIAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstance.getId();
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
        String message = "<_mdw_task_sla><task_instance_id>" + taskInstance.getId()
            + "</task_instance_id><is_alert>"
            + (isAlert?"true":"false") + "</is_alert></_mdw_task_sla>";
        if (isReschedule) {
            queue.rescheduleExternalEvent(eventName, eventDate, message);
        }
        else {
            queue.scheduleExternalEvent(eventName, eventDate, message, null);
        }
    }

    void unscheduleTaskSlaEvent() throws ServiceException {
        ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
        String eventName = ScheduledEvent.SPECIAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstance.getId();
        String backwardCompatibleEventName = ScheduledEvent.EXTERNAL_EVENT_PREFIX
            + "TaskDueDate." + taskInstance.getId();
        try {
            queue.unscheduleEvent(eventName);   // this broadcasts
            queue.unscheduleEvent(backwardCompatibleEventName); // this broadcasts
        } catch (Exception e) {
            throw new ServiceException("Failed to unschedule task SLA", e);
        }
    }

    /**
     * Determines a task instance's workgroups based on the defined strategy.  If no strategy exists,
     * default to the workgroups defined in the task template.
     *
     * By default this method propagates StrategyException as ServiceException.  If users wish to continue
     * processing they can override the default strategy implementation to catch StrategyExceptions.
     */
    List<String> determineWorkgroups(Map<String,String> indexes)
    throws ServiceException {
        TaskTemplate taskTemplate = getTemplate();
        String routingStrategyAttr = taskTemplate.getAttribute(TaskAttributeConstant.ROUTING_STRATEGY);
        if (StringHelper.isEmpty(routingStrategyAttr)) {
            return taskTemplate.getWorkgroups();
        }
        else {
            try {
                RoutingStrategy strategy = TaskInstanceStrategyFactory.getRoutingStrategy(routingStrategyAttr,
                        OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
                if (strategy instanceof ParameterizedStrategy) {
                    populateStrategyParams((ParameterizedStrategy)strategy, getTemplate(), taskInstance.getOwnerId(), indexes);
                }
                return strategy.determineWorkgroups(taskTemplate, taskInstance);
            }
            catch (Exception ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
    }

    public TaskTemplate getTemplate() {
        return TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
    }

    private static void populateStrategyParams(ParameterizedStrategy strategy,
            TaskTemplate template, Long processInstanceId, Map<String, String> indexes) throws ServiceException {
        for (Attribute attr : template.getAttributes()) {
            strategy.setParameter(attr.getAttributeName(), attr.getAttributeValue());
        }
        ProcessRuntimeContext context = ServiceLocator.getWorkflowServices().getContext(processInstanceId);
        for (String name : context.getVariables().keySet()) {
            strategy.setParameter(name, context.getVariables().get(name));
        }
    }

    // TODO: handle non-standard status changes
    public void notifyTaskAction(String action, Integer previousStatus, Integer previousState)
    throws ServiceException, DataAccessException {
        CodeTimer timer = new CodeTimer("TaskManager.notifyStatusChange()", true);

        try {
            // new-style notifiers
            String outcome = TaskStatuses.getTaskStatuses().get(taskInstance.getStatusCode());
            //if (!action.equals(TaskAction.CLAIM) && !action.equals(TaskAction.RELEASE))  // avoid nuisance notice to claimer and releaser
            sendNotification(action, outcome);

            // new-style auto-assign
            if (TaskStatus.STATUS_OPEN.intValue() == taskInstance.getStatusCode().intValue()
                    && !action.equals(TaskAction.RELEASE)) {
                AutoAssignStrategy strategy = getAutoAssignStrategy();
                if (strategy != null) {
                    User assignee = strategy.selectAssignee(taskInstance);
                    if (assignee == null)
                        logger.severe("No users found for auto-assignment of task instance ID: " + taskInstance.getTaskInstanceId());
                    else
                    {
                      taskInstance.setAssigneeId(assignee.getId());
                      taskInstance.setAssigneeCuid(assignee.getCuid());
                      performAction(TaskAction.ASSIGN, assignee.getId(), assignee.getId(), "Auto-assigned", null, false, false);
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

    private void sendNotification(String action, String outcome) {
        try {
            TaskInstanceNotifierFactory notifierFactory = TaskInstanceNotifierFactory.getInstance();
            List<String> notifierSpecs = new ArrayList<String>();
            Long processInstId = OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null;
            notifierSpecs = notifierFactory.getNotifierSpecs(taskInstance.getTaskId(), processInstId, outcome);
            if (notifierSpecs == null || notifierSpecs.isEmpty()) return;
            if (taskInstance.isShallow())
                getTaskInstanceAdditionalInfo();
            TaskRuntimeContext taskRuntime = getContext();
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
        } catch (Exception ex) {
            logger.severeException("Failed to send email notification for task instance "
                    + taskInstance.getTaskInstanceId(), ex);
        }
    }

    /**
     * Retrieve additional info for task instance, including
     * assignee user name, due date, groups, master request id, etc.
     */
    void getTaskInstanceAdditionalInfo()
    throws DataAccessException, ServiceException {
        new TaskDataAccess().getTaskInstanceAdditionalInfoGeneral(taskInstance);
        taskInstance.setTaskInstanceUrl(getTaskInstanceUrl());
    }

    public String getTaskInstanceUrl() throws ServiceException {
        // check for custom page
        TaskTemplate template = getTemplate();
        if (template != null && template.isHasCustomPage()) {
            String assetSpec = template.getCustomPage();
            if (assetSpec.endsWith(".jsx")) {
                if (template.getCustomPageAssetVersion() != null)
                    assetSpec += " v" + template.getCustomPageAssetVersion();
                AssetVersionSpec customPage = AssetVersionSpec.parse(assetSpec);
                try {
                    CustomPageLookup pageLookup = new CustomPageLookup(customPage, taskInstance.getTaskInstanceId());
                    return pageLookup.getUrl();
                }
                catch (Exception ex) {
                    throw new ServiceException("Cannot determine custom page URL for task: " + template.getLabel(), ex);
                }
            }
        }

        // default url
        String baseUrl = ApplicationContext.getMdwHubUrl();
        if (!baseUrl.endsWith("/"))
          baseUrl += "/";
        return baseUrl + "#/tasks/" + taskInstance.getTaskInstanceId();
    }

    public TaskRuntimeContext getContext() throws ServiceException {
        User assignee = null;
        if (taskInstance.getAssigneeCuid() != null) {
            try {
                assignee = ServiceLocator.getUserServices().getUser(taskInstance.getAssigneeCuid());
                if (assignee == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + taskInstance.getAssigneeCuid());
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }

        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();

        ProcessRuntimeContext processContext = null;
        if (OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()))
            processContext = workflowServices.getContext(taskInstance.getOwnerId());

        if (processContext != null) {
            return new TaskRuntimeContext(processContext, getTemplate(), taskInstance, assignee);
        }
        else {
            return new TaskRuntimeContext(null, null, null, new HashMap<>(), getTemplate(), taskInstance, assignee);
        }
    }

    public AutoAssignStrategy getAutoAssignStrategy() throws StrategyException, ServiceException {
        String autoAssignAttr = getTemplate().getAttribute(TaskAttributeConstant.AUTO_ASSIGN);
        AutoAssignStrategy strategy = null;
        if (StringHelper.isEmpty(autoAssignAttr))
            return strategy;
        else{
          try {
            strategy = TaskInstanceStrategyFactory.getAutoAssignStrategy(autoAssignAttr,
                    OwnerType.PROCESS_INSTANCE.equals(taskInstance.getOwnerType()) ? taskInstance.getOwnerId() : null);
            if (strategy instanceof ParameterizedStrategy) {
              // need to check how to pass the indices
              populateStrategyParams((ParameterizedStrategy)strategy, getTemplate(), taskInstance.getOwnerId(), null);
            }
          }
          catch (Exception ex) {
              throw new ServiceException(ex.getMessage(), ex);
          }
        }
        return strategy;
    }

    void assign(Long userId)
    throws ServiceException, DataAccessException {
        if (isAssignable()) {
            taskInstance.setStatusCode(TaskStatus.STATUS_ASSIGNED);
            taskInstance.setAssigneeId(userId);
            Map<String,Object> changes = new HashMap<String,Object>();
            changes.put("TASK_CLAIM_USER_ID", userId);
            changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_ASSIGNED);
            new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);

            // update process variable with assignee - only for local tasks
            TaskTemplate taskVO = getTemplate();
            if (taskVO != null) {
                String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
                if (!StringHelper.isEmpty(assigneeVarSpec)) {
                    try {
                        User user = UserGroupCache.getUser(userId);
                        String cuid = user == null ? null : user.getCuid();
                        if (cuid == null)
                            throw new DataAccessException("User not found for id: " + userId);
                        TaskRuntimeContext runtimeContext = getContext();
                        String prevCuid;
                        if (TaskRuntimeContext.isExpression(assigneeVarSpec))
                            prevCuid = runtimeContext.evaluateToString(assigneeVarSpec);
                        else {
                            Object varVal = runtimeContext.getVariables().get(assigneeVarSpec);
                            prevCuid = varVal == null ? null : varVal.toString();
                        }

                        if (!cuid.equals(prevCuid)) {
                            // need to update variable to match new assignee
                            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                            if (TaskRuntimeContext.isExpression(assigneeVarSpec)) {
                                // create or update document variable referenced by expression
                                runtimeContext.set(assigneeVarSpec, cuid);
                                String rootVar = assigneeVarSpec.substring(2, assigneeVarSpec.indexOf('.'));
                                Variable doc = runtimeContext.getProcess().getVariable(rootVar);
                                VariableInstance varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                                String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                                if (varInst == null) {
                                    workflowServices.createDocument(runtimeContext, rootVar, stringValue);
                                }
                                else {
                                    workflowServices.updateDocument(runtimeContext, rootVar, stringValue);
                                }
                            }
                            else {
                                workflowServices.setVariable(runtimeContext, assigneeVarSpec, cuid);
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
            logger.warn("TaskInstance is not assignable.  ID = " + taskInstance.getTaskInstanceId());
        }
    }

    void release()
    throws DataAccessException {
        taskInstance.setStatusCode(TaskStatus.STATUS_OPEN);
        taskInstance.setAssigneeId(null);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_CLAIM_USER_ID", null);
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_OPEN);
        new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);

        // clear assignee process variable value
        TaskTemplate taskVO = getTemplate();
        if (taskVO != null) { // not for invalid tasks
            String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
            if (!StringHelper.isEmpty(assigneeVarSpec)) {
                try {
                    TaskRuntimeContext runtimeContext = getContext();
                    WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                    if (TaskRuntimeContext.isExpression(assigneeVarSpec)) {
                        // create or update document variable referenced by expression
                        runtimeContext.set(assigneeVarSpec, null);
                        String rootVar = assigneeVarSpec.substring(2, assigneeVarSpec.indexOf('.'));
                        Variable doc = runtimeContext.getProcess().getVariable(rootVar);
                        VariableInstance varInst = runtimeContext.getProcessInstance().getVariable(rootVar);
                        String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getType(), runtimeContext.evaluate("#{" + rootVar + "}"));
                        if (varInst == null) {
                            workflowServices.createDocument(runtimeContext, rootVar, stringValue);
                        }
                        else {
                            workflowServices.updateDocument(runtimeContext, rootVar, stringValue);
                        }
                    }
                    else {
                        workflowServices.setVariable(runtimeContext, assigneeVarSpec, null);
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
    }

    void work() throws ServiceException, DataAccessException {
        taskInstance.setStatusCode(TaskStatus.STATUS_IN_PROGRESS);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_IN_PROGRESS);
        new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);
    }

    private void forward(String destination, String comment)
    throws ServiceException, DataAccessException {
        List<String> prevWorkgroups = taskInstance.getWorkgroups();
        if (prevWorkgroups == null || prevWorkgroups.isEmpty()) {
            prevWorkgroups = getTemplate().getWorkgroups();
        }

        // change the task instance to be associated with the specified group
        release();

        TaskDataAccess dataAccess = new TaskDataAccess();

        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_OPEN);
        changes.put("TASK_CLAIM_USER_ID", null);
        if (comment != null)
            changes.put("COMMENTS", comment);
        dataAccess.updateTaskInstance(taskInstance.getTaskInstanceId(), changes, false);
        String[] destWorkgroups = destination.split(",");
        try {
            for(String groupName:destWorkgroups) {
                if (UserGroupCache.getWorkgroup(groupName) == null) {
                    throw new ServiceException( "Invalid Workgroup: " + groupName);
                }
            }
        } catch (CachingException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        dataAccess.setTaskInstanceGroups(taskInstance.getTaskInstanceId(),destWorkgroups );
        taskInstance.setWorkgroups(Arrays.asList(destWorkgroups));

        try {
            // new-style notifiers (registered on destination task)
            sendNotification(TaskAction.FORWARD, TaskAction.FORWARD);

            AutoAssignStrategy strategy = getAutoAssignStrategy();
            if (strategy != null) {
                User assignee = strategy.selectAssignee(taskInstance);
                if (assignee == null)
                    logger.severe("No users found for auto-assignment of task instance ID: " + taskInstance.getId());
                else
                    performAction(TaskAction.ASSIGN, assignee.getId(), assignee.getId(), "Auto-assigned", null, false, false);
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

    void close(String action, String comment) throws ServiceException, DataAccessException {
        Integer newStatus = TaskStatus.STATUS_COMPLETED;
        if (action.equals(TaskAction.CANCEL) || action.equals(TaskAction.ABORT) || action.startsWith(TaskAction.CANCEL + "::"))
            newStatus = TaskStatus.STATUS_CANCELLED;
        Map<String,Object> changes = new HashMap<>();
        changes.put("TASK_INSTANCE_STATUS", newStatus);
        changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
        new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, true);
        taskInstance.setStatusCode(newStatus);
    }

    void cancel()
    throws ServiceException, DataAccessException {
        if (taskInstance.isInFinalStatus()) {
            logger.info("Cannot change the state of the TaskInstance to Cancel.");
            return;
        }
        Integer prevStatus = taskInstance.getStatusCode();
        Integer prevState = taskInstance.getStateCode();
        taskInstance.setStateCode(TaskState.STATE_CLOSED);
        taskInstance.setStatusCode(TaskStatus.STATUS_CANCELLED);
        Map<String,Object> changes = new HashMap<String,Object>();
        changes.put("TASK_INSTANCE_STATUS", TaskStatus.STATUS_CANCELLED);
        changes.put("TASK_INSTANCE_STATE", TaskState.STATE_CLOSED);
        new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes, true);
        notifyTaskAction(TaskAction.CANCEL, prevStatus, prevState);
    }


    private boolean isAssignable() {
        return isOpen();
    }

    private boolean isOpen() {
        if (taskInstance.getStatusCode().intValue() == TaskStatus.STATUS_COMPLETED.intValue()) {
            return false;
        }
        if (taskInstance.getStatusCode().intValue() == TaskStatus.STATUS_CANCELLED.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * Filters according to what's applicable
     * depending on the context of the task activity instance.
     *
     * @param standardTaskActions unfiltered list
     * @return the list of task actions
     */
    public List<TaskAction> filterStandardActions(List<TaskAction> standardTaskActions)
    throws ServiceException, DataAccessException {

        CodeTimer timer = new CodeTimer("TaskManager.filterStandardTaskActions()", true);

        List<TaskAction> filteredTaskActions = standardTaskActions;
        try {
            ActivityInstance activityInstance = getActivityInstance(true);

            if (activityInstance != null) {
                Long processInstanceId = activityInstance.getProcessInstanceId();
                ProcessInstance processInstance = ServiceLocator.getWorkflowServices().getProcess(processInstanceId);

                if (processInstance.isEmbedded()) {
                    // remove RETRY since no default behavior is defined for inline tasks
                    TaskAction retryAction = null;
                    for (TaskAction taskAction : standardTaskActions) {
                        if (taskAction.getTaskActionName().equalsIgnoreCase("Retry"))
                            retryAction = taskAction;
                        }
                    if (retryAction != null)
                        standardTaskActions.remove(retryAction);
                }

                Process processVO = null;
                if (processInstance.getProcessInstDefId() > 0L)
                    processVO = ProcessCache.getProcessInstanceDefiniton(processInstance.getProcessId(), processInstance.getProcessInstDefId());
                if (processVO == null)
                    processVO = ProcessCache.getProcess(processInstance.getProcessId());
                if (processInstance.isEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<Transition> outgoingWorkTransVOs = processVO.getAllTransitions(activityInstance.getActivityId());
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

    /**
     * Gets the custom task actions associated with a task instance as determined by
     * the result codes for the possible outgoing work transitions from the associated
     * activity.
     *
     * @return the list of task actions
     */
    public List<TaskAction> getCustomActions()
    throws ServiceException, DataAccessException {

        CodeTimer timer = new CodeTimer("getCustomActions()", true);
        List<TaskAction> dynamicTaskActions = new ArrayList<TaskAction>();

        try {
            ActivityInstance activityInstance = getActivityInstance(true);

            if (activityInstance != null) {
                Long processInstanceId = activityInstance.getProcessInstanceId();
                ProcessInstance processInstance = ServiceLocator.getWorkflowServices().getProcess(processInstanceId);
                Process processVO = null;
                if (processInstance.getProcessInstDefId() > 0L)
                    processVO = ProcessCache.getProcessInstanceDefiniton(processInstance.getProcessId(), processInstance.getProcessInstDefId());
                if (processVO == null)
                    processVO = ProcessCache.getProcess(processInstance.getProcessId());
                if (processInstance.isEmbedded())
                    processVO = processVO.getSubProcessVO(new Long(processInstance.getComment()));
                List<Transition> outgoingWorkTransVOs = processVO.getAllTransitions(activityInstance.getActivityId());
                for (Transition workTransVO : outgoingWorkTransVOs) {
                    String resultCode = workTransVO.getCompletionCode();
                    if (resultCode != null) {
                        Integer eventType = workTransVO.getEventType();
                        if (eventType.equals(EventType.FINISH) || eventType.equals(EventType.RESUME) || TaskAction.FORWARD.equals(resultCode)) {
                            TaskAction taskAction = new TaskAction();
                            taskAction.setTaskActionName(resultCode);
                            taskAction.setCustom(true);
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

    void auditLog(String action, Long userId, String destination, String comments)
    throws ServiceException, DataAccessException {
        String userCuid = null;
        if (userId != null) {
            UserDataAccess uda = new UserDataAccess(new DatabaseAccess(null));
            User user = uda.getUser(userId);
            userCuid = user.getCuid();
        }
        auditLog(action, userCuid, destination, comments);
    }

    void auditLog(String action, String user) throws ServiceException {
        auditLog(action, user, null, null);
    }

    void auditLog(String action, String user, String destination, String comments)
    throws ServiceException {
        if (user == null)
            user = "N/A";

        Entity entity = Entity.TaskInstance;
        Long entityId = taskInstance.getTaskInstanceId();

        UserAction userAction = new UserAction(user, UserAction.getAction(action), entity, entityId, comments);
        userAction.setSource("Task Services");
        userAction.setDestination(destination);
        if (userAction.getAction().equals(UserAction.Action.Other)) {
            if (action != null && action.startsWith(TaskAction.CANCEL + "::")) {
                userAction.setAction(UserAction.Action.Cancel);
            }
            else {
            userAction.setExtendedAction(action);
            }
        }
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            eventManager.createAuditLog(userAction);
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Failed to create audit log: " + userAction, ex);
        }
    }

    public void updateState(boolean isAlert) throws DataAccessException {
        if (isAlert) {
            if (taskInstance.getStateCode().equals(TaskState.STATE_OPEN)) {
                Map<String, Object> changes = new HashMap<>();
                changes.put("TASK_INSTANCE_STATE", TaskState.STATE_ALERT);
                new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes,
                        false);
                if (taskInstance.getDue() != null) {
                    scheduleTaskSlaEvent(Date.from(taskInstance.getDue()), 0, false);
                }
                sendNotification("UPDATE", TaskState.getTaskStateName(TaskState.STATE_ALERT));
            }
        }
        else {
            if (taskInstance.getStateCode().equals(TaskState.STATE_OPEN)
                    || taskInstance.getStateCode().equals(TaskState.STATE_ALERT)) {
                Map<String, Object> changes = new HashMap<>();
                changes.put("TASK_INSTANCE_STATE", TaskState.STATE_JEOPARDY);
                new TaskDataAccess().updateTaskInstance(taskInstance.getTaskInstanceId(), changes,
                        false);
                sendNotification("UPDATE", TaskState.getTaskStateName(TaskState.STATE_JEOPARDY));
            }
        }
    }
}
