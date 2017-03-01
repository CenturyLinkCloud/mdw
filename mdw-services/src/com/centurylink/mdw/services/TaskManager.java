/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.note.InstanceNote;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.service.data.task.TaskDataException;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;

public interface TaskManager {

    /**
     * Return the claimed tasks for a given user, including specified variable values and index keys
     * @param userId
     * @param criteria
     * @param variables
     * @param variablesCriteria
     * @param indexKeys
     * @param indexCriteria
     * @return
     * @throws TaskException
     * @throws DataAccessException
     */
    public TaskInstance[] getAssignedTasks(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria, List<String> indexKeys, Map<String,String> indexCriteria)
            throws TaskException, DataAccessException;
    /**
     * Returns the claimed tasks for a given user, including specified variable values
     *
     * @param userId
     * @param criteria
     * @param variables
     * @return Array of TaskInstanceVOs
     */
    public TaskInstance[] getAssignedTasks(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria)
    throws TaskException, DataAccessException;


    /**
     * Returns the claimed tasks for a given user
     *
     * @param userId
     * @param criteria
     * @return Array of TaskInstanceVOs
     */
    public TaskInstance[] getAssignedTasks(Long userId, Map<String,String> criteria)
    throws TaskException, DataAccessException;

    /**
     * Returns a task with all associated information
     *
     * @param pTaskInstId
     * @return the taskInst and associated data
     * @throws TaskDataException
     * @throws DataAccessException
     */
    public TaskInstance getTaskInstanceVO(Long pTaskInstId)
    throws TaskException, DataAccessException;

    /**
     * Returns task instance basic info (only from task_instance table)
     *
     * @param pId
     */
    public TaskInstance getTaskInstance(Long pId)
    throws DataAccessException;

    /**
     * Get additional information about the task instance (data
     * not stored in TASK_INSTANCE table), which include:
     * - task name (classic task or non-template task)
     * - due date
     * - assignee cuid
     * - task category (classic task or non-template task)
     * - master request id (classic task or non-template task)
     * - activity message (classic task or non-template task)
     * - activity name (classic task or non-template task)
     * - indices (general template task)
     * - groups (template task)
     *
     * @param taskInstId
     * @param allInfo
     * @return
     * @throws DataAccessException
     */
    public void getTaskInstanceAdditionalInfo(TaskInstance taskInst)
    throws DataAccessException, TaskException;

    /**
     * Convenience method for below.
     */
    public TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId)
    throws TaskException, DataAccessException;

    /**
     * Convenience method for below.
     */
    public TaskInstance createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId, Map<String,String> indices)
    throws TaskException, DataAccessException;

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
   throws TaskException, DataAccessException;

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
   throws TaskException, DataAccessException;

   /**
    * Cancel the task
    * @param taskInst the task instance object
    * @throws TaskException
    * @throws DataAccessException
    */
   public void cancelTaskInstance(TaskInstance taskInst)
   throws TaskException, DataAccessException;

   /**
    * Cancels all tasks of the given process instance if they are not complete.
    *
    * @param pProcessInstance
    */
   public void cancelTasksForProcessInstance(Long pProcessInstId)
   throws TaskException, DataAccessException;

   public void cancelTasksForProcessInstances(List<Long> procInstIds)
   throws TaskException, DataAccessException;

   public void cancelTasksOfActivityInstance(Long actInstId)
   throws NamingException, MdwException;

   public TaskInstance performActionOnTaskInstance(String action, Long taskInstanceId,
           Long userId, Long assigneeId, String comment, String destination, boolean notifyEngine)
   throws TaskException, DataAccessException;

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
   throws TaskException, DataAccessException;

   /**
    * Used by detail-only task manager in place of performActionOnTaskInstance
    * @param ti
    * @param action
    * @param comment
    * @throws TaskException
    * @throws DataAccessException
    */
   public void closeTaskInstance(TaskInstance ti, String action, String comment)
   throws TaskException, DataAccessException;

   /**
    * Returns the available notes for the given owner type and ID
    *
    * @param owner the owner type
    * @param id the owner id
    * @return collection of notes for the given owner
    */
   public Collection<InstanceNote> getNotes(String owner, Long instanceId)
   throws DataAccessException;

   /**
    * Creates a instance note.
    */
   public Long addNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
   throws DataAccessException, TaskException;

   /**
    * Updates a task instance note.
    */
   public void updateNote(Long noteId, String noteName, String noteDetails, String user)
   throws DataAccessException, TaskException;

   /**
    * Updates a note based on ownerId.
    */
   public void updateNote(String owner, Long ownerId, String noteName, String noteDetails, String user)
   throws DataAccessException, TaskException;

   /**
    * Deletes the passed in TaskInstanceNote
    *
    * @param pTaskNote
    */
   public void deleteNote(Long noteId, Long userId)
   throws TaskException, DataAccessException;

   /**
    * Returns the collection of attachments
    *
    * @param pTaskInstId
    * @return Collection of Attachment
    */
   public Collection<Attachment> getAttachments(String attachName,String attachmentLocation)
   throws DataAccessException;

   /**
    * Creates and adds a task attachment
    */
   public Long addAttachment(String attachName,
           String attachLoc, String contentType, String user,String owner,Long ownerId)
   throws DataAccessException, TaskException;

   /**
    * Removes the attachment from the task instance
    *
    * @param pTaskInstId
    * @param pAttachName
    * @param pAttachLocation
    * @return Attachment
    */
   public void removeAttachment(Long pAttachId, Long userId)
   throws DataAccessException, TaskException;

   /**
    * Returns the attachment
    *
    * @param pTaskInstId
    * @param pAttachName
    * @return Attachment
    */
   public Attachment getAttachment(String attachName,String pAttachLocation)
   throws DataAccessException;

   /**
    * Return the attachment by its ID
    * @param pAttachmentId
    * @return
    * @throws DataAccessException
    */
   public Attachment getAttachment(Long pAttachmentId) throws DataAccessException;

   /**
    * get the task instance for the activity instance.
    *
    * @param activityInstId activity instance ID
    */
   public TaskInstance getTaskInstanceByActivityInstanceId(Long activityInstanceId)
   throws TaskException, DataAccessException;

   /**
    * Updates the due date for a task instance.
    *
    * @param pTaskInstanceId
    * @param pDueDate
    */
   public void updateTaskInstanceDueDate(Long pTaskInstanceId, Date pDueDate, String cuid, String comment)
   throws TaskException, DataAccessException;

   /**
    * Update comments of a task instance
    * @param taskInstanceId
    * @param comments
    * @throws TaskException
    * @throws DataAccessException
    */
   public void updateTaskInstanceComments(Long taskInstanceId, String comments)
   throws TaskException, DataAccessException;

   /**
    * Gets the dynamic task actions associated with a task instance as determined by
    * the result codes for the possible outgoing work transitions from the associated
    * activity.
    *
    * @param taskInstanceId
    * @return the list of task actions
    */
   public List<TaskAction> getDynamicTaskActions(Long taskInstanceId)
   throws TaskException, DataAccessException;

   /**
    * Gets the standard task actions, filtered according to what's applicable
    * depending on the context of the task activity instance.
    *
    * @param taskInstanceId
    * @param standardTaskActions unfiltered list
    * @return the list of task actions
    */
   public List<TaskAction> filterStandardTaskActions(Long taskInstanceId, List<TaskAction> standardTaskActions)
   throws TaskException, DataAccessException;

   /**
    * Gets the event log entries for a task instance.
    *
    * @param taskInstanceId
    * @return Collection of EventLog objects
    */
   public List<EventLog> getEventLogs(Long taskInstanceId)
   throws TaskException, DataAccessException;

   /**
    * Return the data associated with the task instance
    * @param taskInst
    * @return the form data document containing the task instance data
    * @throws DataAccessException
    */
   public Document getTaskInstanceData(TaskInstance taskInst)
   throws DataAccessException;

   public VariableInstance[] getProcessInstanceVariables(Long procInstId)
   throws DataAccessException;

   /**
    * Get the list of group names to which the given task instance is associated with
    * @param taskInst
    * @return
    * @throws DataAccessException
    */
   public List<String> getGroupsForTaskInstance(TaskInstance taskInst)
   throws DataAccessException, TaskException;

   public List<SubTask> getSubTaskList(TaskRuntimeContext runtimeContext) throws TaskException;

   /**
   * Get all subtask instances for a specified master task instance id.
   */
  public List<TaskInstance> getSubTaskInstances(Long masterTaskInstanceId)
  throws DataAccessException;

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
   * Get the runtime context for a taskInstance.
   */
  public TaskRuntimeContext getTaskRuntimeContext(TaskInstance taskInstanceVO)
      throws DataAccessException;

  /**
   * Get the Subtask plan for a specified taskInstance
   */
  public SubTaskPlan getSubTaskPlan(TaskRuntimeContext runtimeContext) throws TaskException;

  public void setIndexes(TaskRuntimeContext runtimeContext) throws DataAccessException;

  public Long getActivityInstanceId(TaskInstance taskInstance, boolean sourceActInst);

  public void notifyTaskAction(TaskInstance taskInstance, String action, Integer previousStatus, Integer previousState)
  throws TaskException, DataAccessException;

}