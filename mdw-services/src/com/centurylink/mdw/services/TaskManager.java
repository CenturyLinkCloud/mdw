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
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;

public interface TaskManager {

    /**
     * Returns task instance basic info (only from task_instance table)
     *
     * @param pId
     */
    public TaskInstance getTaskInstance(Long pId)
    throws DataAccessException;

    /**
     * Convenience method for below.
     */
    public TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId)
    throws ServiceException, DataAccessException;

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
     * @param taskId  task template ID
     * @param procInstId  process instance ID
     * @param secondaryOwner  can be DOCUMENT (general task) or WORK_TRANSITION_INSTANCE (for classic task)
     * @param secondaryOwnerId  document ID or transition instance ID
     * @param message return message (typically stacktrace for fallout tasks) of the activity for classic task
     * @param ownerApp  owner system MAL, optionally followed by ":" and engine URL.
     *          The field is only populated when the task manager is remote
     * @param assTaskInstId when the task manager is remote and for summary only, this field
     *          is populated with the task instance ID of the local task manager
     * @param taskName  task name. Taken from task template when not populated (for VCS assets is task template label)
     * @param dueInSeconds  SLA. When it is 0, check if template has specified SLA
     * @param indices indices for general task based on templates
     * @param assignee  assignee CUID if this is to be auto-assigned by process variable
     * @return TaskInstance
     */
   public TaskInstance createTaskInstance(Long taskId, Long procInstId,
           String secondaryOwner, Long secondaryOwnerId, String message,
           String pOwnerAppName, Long pAssTaskInstId, String taskName,
           int dueInSeconds, Map<String,String> indices, String assignee, String masterRequestId)
   throws ServiceException, DataAccessException;

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
   throws ServiceException, DataAccessException;

   /**
    * Cancel the task
    * @param taskInst the task instance object
    * @throws ServiceException
    * @throws DataAccessException
    */
   public void cancelTaskInstance(TaskInstance taskInst)
   throws ServiceException, DataAccessException;

   /**
    * Cancels all tasks of the given process instance if they are not complete.
    *
    * @param pProcessInstance
    */
   public void cancelTasksForProcessInstance(Long pProcessInstId)
   throws ServiceException, DataAccessException;

   public void cancelTasksForProcessInstances(List<Long> procInstIds)
   throws ServiceException, DataAccessException;

   public void cancelTasksOfActivityInstance(Long actInstId)
   throws NamingException, MdwException;

   public TaskInstance performActionOnTaskInstance(String action, Long taskInstanceId,
           Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine)
   throws ServiceException, DataAccessException;

   /**
    * Performs an action on a task instance.  Updates the task instance to the appropriate status
    * and state based on the action being performed.
    *
    * @param action the action to be performed
    * @param taskInstanceId the task instance to act on
    * @param userId the user performing the action
    * @param assignee the assignee if the action is "claim" or "assignee"
    * @param comment comment to be associated with the task
    * @param destination the destination outcome selected for the action
    * @param notifyEngine when true, notify engine for task completion/cancellation
    * @param allowResumeEndpoint consults the service endpoint for resuming workflow (prevent
    *        infinite recursion when this is called from the service endpoint itself)
    * @return the updated task instance
    */
   public TaskInstance performActionOnTaskInstance(String action, Long taskInstanceId,
           Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine, boolean allowResumeEndpoint)
   throws ServiceException, DataAccessException;

   /**
    * Used by detail-only task manager in place of performActionOnTaskInstance
    * @param ti
    * @param action
    * @param comment
    * @throws ServiceException
    * @throws DataAccessException
    */
   public void closeTaskInstance(TaskInstance ti, String action, String comment)
   throws ServiceException, DataAccessException;

   /**
    * get the task instance for the activity instance.
    *
    * @param activityInstId activity instance ID
    */
   public TaskInstance getTaskInstanceByActivityInstanceId(Long activityInstanceId)
   throws ServiceException, DataAccessException;

   /**
    * Updates the due date for a task instance.
    *
    * @param pTaskInstanceId
    * @param pDueDate
    */
   public void updateTaskInstanceDueDate(Long pTaskInstanceId, Date pDueDate, String cuid, String comment)
   throws ServiceException, DataAccessException;

   /**
    * Update comments of a task instance
    * @param taskInstanceId
    * @param comments
    * @throws ServiceException
    * @throws DataAccessException
    */
   public void updateTaskInstanceComments(Long taskInstanceId, String comments)
   throws ServiceException, DataAccessException;

   /**
    * Gets the dynamic task actions associated with a task instance as determined by
    * the result codes for the possible outgoing work transitions from the associated
    * activity.
    *
    * @param taskInstanceId
    * @return the list of task actions
    */
   public List<TaskAction> getDynamicTaskActions(Long taskInstanceId)
   throws ServiceException, DataAccessException;

   /**
    * Gets the standard task actions, filtered according to what's applicable
    * depending on the context of the task activity instance.
    *
    * @param taskInstanceId
    * @param standardTaskActions unfiltered list
    * @return the list of task actions
    */
   public List<TaskAction> filterStandardTaskActions(Long taskInstanceId, List<TaskAction> standardTaskActions)
   throws ServiceException, DataAccessException;

   /**
    * Gets the event log entries for a task instance.
    *
    * @param taskInstanceId
    * @return Collection of EventLog objects
    */
   public List<EventLog> getEventLogs(Long taskInstanceId)
   throws ServiceException, DataAccessException;

   /**
    * Get the list of group names to which the given task instance is associated with
    * @param taskInst
    * @return
    * @throws DataAccessException
    */
   public List<String> getGroupsForTaskInstance(TaskInstance taskInst)
   throws DataAccessException, ServiceException;

   public List<SubTask> getSubTaskList(TaskRuntimeContext runtimeContext) throws ServiceException;

  /**
   * Update task index values for an instance.
   */
  public void updateTaskIndices(Long taskInstanceId, Map<String,String> indices)
  throws DataAccessException;

  /**
   * Update workgroups for a task instance.
   */
  public void updateTaskInstanceWorkgroups(Long taskInstanceId, List<String> groups)
  throws DataAccessException;

  /**
   * Update priority for a task instance.
   */
  public void updateTaskInstancePriority(Long taskInstanceId, Integer priority)
  throws DataAccessException;

  /**
   * Get the Subtask plan for a specified taskInstance
   */
  public SubTaskPlan getSubTaskPlan(TaskRuntimeContext runtimeContext) throws ServiceException;

  public void setIndexes(TaskRuntimeContext runtimeContext) throws DataAccessException;

  public Long getActivityInstanceId(TaskInstance taskInstance, boolean sourceActInst);

  public void notifyTaskAction(TaskInstance taskInstance, String action, Integer previousStatus, Integer previousState)
  throws ServiceException, DataAccessException;

}