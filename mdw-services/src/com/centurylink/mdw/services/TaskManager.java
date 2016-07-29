/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.common.Attachment;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskInstanceReportVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.dao.task.TaskDAOException;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;

public interface TaskManager {
    public TaskAction[] getTaskActionVOs() throws TaskException, DataAccessException;

    /**
     * Retrieves all task templates associated with a particular work group.
     * @return array of task template objects
     */
    public List<TaskVO> getTasksForWorkgroup(String groupName)
    throws DataAccessException;

    /**
     * Returns all TaskVOs (task templates) in their shallow form.
     *
     * @return Array of TaskVO
     */
    public TaskVO[] getShallowTaskVOs()
    throws TaskException, DataAccessException;

    /**
     * Returns the fully populated TaskVO (task template)
     */
    public TaskVO getTaskVO(Long taskId)
    throws TaskException, DataAccessException;

    /**
     * Method that returns the collection of attributes for the given task template
     *
     * @param pTaskId
     * @return List of AttributeVOs
     */
    public List<AttributeVO> getTaskAttributes(Long pTaskId)
    throws TaskException, DataAccessException;

    /**
     * Returns the unclaimed tasks for the specified set of work groups
     *
     * @param workgroups
     * @param queryRequest
     * @return PaginatedResponse
     */
    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest)
    throws TaskException, DataAccessException;

    /**
     * Returns the unclaimed tasks for the specified set of work groups, including variables
     *
     * @param workgroups
     * @param queryRequest
     * @param variables variable values to retrieve
     * @param variablesCriteria
     * @param indexKeys
     * @param indexCriteria
     * @return PaginatedResponse
     */
    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest, List<String> variables, Map<String,String> variablesCriteria,
            List<String> indexKeys, Map<String,String> indexCriteria)
    throws TaskException, DataAccessException;

    /**
     * Returns the unclaimed tasks for the specified set of work groups based on search key, including variables
     * @param workgroups
     * @param queryRequest
     * @param variables
     * @param variablesCriteria
     * @param indexKeys
     * @param indexCriteria
     * @param searchColumns
     * @param searchKey
     * @return
     * @throws TaskException
     * @throws DataAccessException
     */
    public PaginatedResponse getUnClaimedTaskInstanceVOs(String[] workgroups, QueryRequest queryRequest, List<String> variables, Map<String,String> variablesCriteria,
             List<String> indexKeys, Map<String,String> indexCriteria, List<String> searchColumns, Object searchKey)
    throws TaskException, DataAccessException;

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
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria, List<String> indexKeys, Map<String,String> indexCriteria)
            throws TaskException, DataAccessException;
    /**
     * Returns the claimed tasks for a given user, including specified variable values
     *
     * @param userId
     * @param criteria
     * @param variables
     * @return Array of TaskInstanceVOs
     */
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria)
    throws TaskException, DataAccessException;


    /**
     * Returns the claimed tasks for a given user
     *
     * @param userId
     * @param criteria
     * @return Array of TaskInstanceVOs
     */
    public TaskInstanceVO[] getClaimedTaskInstanceVOs(Long userId, Map<String,String> criteria)
    throws TaskException, DataAccessException;

    /**
     * Returns a task with all associated information
     *
     * @param pTaskInstId
     * @return the taskInst and associated data
     * @throws TaskDAOException
     * @throws DataAccessException
     */
    public TaskInstanceVO getTaskInstanceVO(Long pTaskInstId)
    throws TaskException, DataAccessException;

    /**
     * Returns task instance basic info (only from task_instance table)
     *
     * @param pId
     */
    public TaskInstanceVO getTaskInstance(Long pId)
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
    public void getTaskInstanceAdditionalInfo(TaskInstanceVO taskInst)
    throws DataAccessException, TaskException;

    public String getTaskInstanceUrl(Long taskInstanceId)
    throws DataAccessException, TaskException;

    /**
     * Convenience method for below.
     */
    public TaskInstanceVO createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId)
    throws TaskException, DataAccessException;

    /**
     * Convenience method for below.
     */
    public TaskInstanceVO createTaskInstance(Long taskId, String masterRequestId, Long procInstId, String secOwner, Long secOwnerId, Map<String,String> indices)
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
   public TaskInstanceVO createTaskInstance(Long taskId, Long procInstId,
           String secondaryOwner, Long secondaryOwnerId, String message,
           String pOwnerAppName, Long pAssTaskInstId, String taskName,
           int dueInSeconds, Map<String,String> indices, String assignee, String masterRequestId)
   throws TaskException, DataAccessException;

   /**
    * This version is used to create detail-only task instance where
    * the summary instance is hosted in remote task manager.
    * The method only creates task instance entry in database.
    * It does *not* create SLA, groups, indices, notifications, observers,
    * auto-assignment, or audit log.
    *
    * @param taskId task template ID
    * @param procInstId process instance ID
    * @param secondaryOwner can be DOCUMENT (general task) or WORK_TRANSITION_INSTANCE (for classic task)
    * @param secondaryOwnerId document ID or transition instance ID
    * @param message  return message (typically stacktrace for fallout tasks) of the activity for classic task
    * @param taskName task name. Taken from task template when not populated
    * @return TaskInstance
    */
   public TaskInstanceVO createTaskInstance(Long taskId, Long procInstId,
           String secondaryOwner, Long secondaryOwnerId, String message, String taskName, String masterRequestId)
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
   public TaskInstanceVO createTaskInstance(Long taskId, String masterOwnerId,
      String comment, Date dueDate, Long userId, Long documentId)
   throws TaskException, DataAccessException;

   public TaskInstanceVO createSubTaskInstance(Long masterTaskInstanceId, String subTaskName)
   throws TaskException, DataAccessException;

   /**
    * Returns the available task statuses from reference data.
    *
    * @return the statuses
    */
   public Collection<TaskStatus> getTaskStatuses()
   throws DataAccessException;

   /**
    * Returns the available task statuses from reference data.
    *
    * @return the statuses
    */
   public Collection<TaskState> getTaskStates()
   throws DataAccessException;

   /**
    * Updates the appropriate taskInstanceData based on the passed info.
    *
    * @param variableInstanceVO
    * @param value
    * @param userId
    */
   public void updateTaskInstanceData(VariableInstanceVO variableInstanceVO, Serializable value, Long userId)
   throws TaskException, DataAccessException;

   /**
    * Creates the Task Instance Data
    *
    * @param taskInstanceId
    * @param variableInstanceVO
    * @param value
    * @param userId
    */
   public VariableInstanceInfo createTaskInstanceData(Long taskInstanceId, VariableInstanceVO variableInstanceVO, Serializable value, Long userId)
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
    * Creates and returns the list of tasks associated with an order
    *
    * @param pOwnerId
    * @return Collection
    */
   public TaskInstanceVO[] getTaskInstanceVOsForMasterOwner(String pMasterOwnerId)
   throws TaskException, DataAccessException;

   /**
    * Cancel the task
    * @param taskInst the task instance object
    * @throws TaskException
    * @throws DataAccessException
    */
   public void cancelTaskInstance(TaskInstanceVO taskInst)
   throws TaskException, DataAccessException;

   /**
    * Cancels all tasks of the given process instance if they are not complete.
    *
    * @param pProcessInstance
    */
   public void cancelTaskInstancesForProcessInstance(Long pProcessInstId, String ownerApplName)
   throws TaskException, DataAccessException;

   public TaskInstanceVO performActionOnTaskInstance(String action, Long taskInstanceId,
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
   public TaskInstanceVO performActionOnTaskInstance(String action, Long taskInstanceId,
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
   public void closeTaskInstance(TaskInstanceVO ti, String action, String comment)
   throws TaskException, DataAccessException;

   /**
    * Retrieves all task templates.
    *
    * @return Collection of Tasks
    */
   public Collection<TaskVO> getTasks()
   throws DataAccessException;

   /**
    * Retrieves all task templates with given category
    *
    * @param categoryId
    * @return Array of Tasks
    */
   public TaskVO[] getTasks(Long categoryId)
   throws DataAccessException;

   /**
    * Returns all the Task categories.
    *
    * @return Collection of TaskCategory objects
    */
   public TaskCategory[] getTaskCategories()
   throws DataAccessException;

   /**
    * Creates and returns a task category for the given params
    *
    * @param pCode
    * @param pDesc
    * @return TaskCategory
    */
   public TaskCategory createTaskCategory(String pCode, String pDesc)
   throws TaskException, DataAccessException;

   /**
    * Updates the Task Category
    *
    * @param pId
    * @param pCode
    * @param pDesc
    * @return TaskCategory
    */
   public TaskCategory updateTaskCategory(Long pId, String pCode, String pDesc)
   throws TaskException, DataAccessException;

   /**
    * Deletes the Task Category
    *
    * @param pId task category ID
    */
   public void deleteTaskCategory(Long pId)
   throws TaskException, DataAccessException;

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
    * Deletes the task action user role mapping for the passid in user role
    * This is to be retired after MDW 5.2
    *
    * @param pTaskUserGroupMap
    */
   @Deprecated
   public void updateTaskUserGroupMappings(Long pTaskId, Long[] pUserGroupIds)
   throws TaskException, DataAccessException;

   /**
    * Returns the TaskInstanceReport based on the requested report type
    *
    * @param pRportType
    */
   public TaskInstanceReportVO[] getTaskInstanceReportVOs(String pReportType)
   throws TaskException, DataAccessException;

   /**
    * Updates a bache of task instances to Jeopardy state as appropriate.
    *
    */
   public int updateTaskInstanceStateAsJeopardy()
   throws TaskException, DataAccessException;

   /**
    * Updates a batch of task instances to Alert state as appropriate.
    */
   public int updateTaskInstanceStateAsAlert()
   throws TaskException, DataAccessException;

   /**
    * Update a single task instance to alert or jeopardy state.
    * When updating to alert state, also schedule an event so that it will later
    * update to jeopardy state.
    * @param taskInstId
    * @param isAlert true to update it to alert state; o/w to jeopardy state
    * @throws TaskException
    * @throws DataAccessException
    */
   public void updateTaskInstanceState(Long taskInstId, boolean isAlert)
   throws TaskException, DataAccessException;

   /**
    * Used to set cross references between task instances in summary and detail task manager
    * @param taskInstId  the ID of the task instance to be updated (in the current database,
    *     which may be detail or summary task manager).
    * @param ownerApplName the remote task manager logical name
    * @param associatedTaskInstId the corresponding task instance ID in the remote task manager
    * @throws TaskException
    * @throws DataAccessException
    */
   public void updateAssociatedTaskInstance(Long taskInstId, String ownerApplName, Long associatedTaskInstId)
   throws TaskException, DataAccessException;

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
    * Returns the collection of attachments
    *
    * @param pTaskInstId
    * @return Collection of Attachment
    */
   public Collection<Attachment> getAttachments(String attachName,String attachmentLocation)
   throws DataAccessException;

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
    * Return the attachment document
    * @param attachment
    * @return
    * @throws DataAccessException
    */
   public DocumentVO getAttachmentDocument(Attachment attachment)
  throws DataAccessException;

   /**
    * get the task instance for the activity instance.
    * For general tasks, the process instance ID is needed
    * to loop through all task instances of the process.
    *
    * @param activityInstId activity instance ID
    * @param pProcessInstId process instance ID for general tasks; null for classic tasks
    */
   public TaskInstanceVO getTaskInstanceByActivityInstanceId(Long activityInstanceId, Long procInstId)
   throws TaskException, DataAccessException;

   /**
    * Creates and returns a task template
    *
    * @param task the task template object to be created
    * @param saveAttributes when it is true, persist task template attributes as well
    * @return task template ID
    */
   public Long createTask(TaskVO task, boolean saveAttributes)
   throws DataAccessException;

   /**
    * updates the task template
    *
    * @param task the task template object to updated
    * @param saveAttributes when it is true, persist task template attributes as well
    */
   public void updateTask(TaskVO task, boolean saveAttributes)
   throws TaskException, DataAccessException;

   /**
    * deletes the task template
    *
    * @param pTaskId task template ID
    */
   public void deleteTask(Long pTaskId)
   throws DataAccessException;

   /**
    * Creates a task template attribute
    *
    * @param pTaskId task template ID
    * @param pName
    * @param pValue
    * @return Attribute
    */
   public void addTaskAttribute(Long pTaskId, String pName, String pValue, Integer pType)
   throws DataAccessException;

   /**
    * Updates the passed in attribute for task
    *
    * @param pAttribId
    * @param pName
    * @param pValue
    * @return Attribute
    */
   public void updateTaskAttribute(Long pAttribId, String pName, String pValue, Integer pType)
   throws DataAccessException;

   /**
    * Deletes the passed in attribute for task
    *
    * @param pAttribId
    */
   public void deleteTaskAttribute(Long pAttribId)
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

   public TaskAction getTaskAction(String action)
   throws DataAccessException;

   public TaskAction addTaskAction(String action, String description)
   throws DataAccessException;

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
   public DocumentVO getTaskInstanceData(TaskInstanceVO taskInst)
   throws DataAccessException;

   /**
    * Method that returns all the variables that are mapped to a given task
    * @param pTaskId
    * @return VariableVO
    */
   public List<VariableVO> getVariablesForTask(Long pTaskId)
   throws DataAccessException;

   /**
    * Returns the array of variable Instance VO
    *
    * @param pTaskInstId
    * @return Array of VariableInstanceVO
    */
   public List<VariableInstanceVO> getVariableInstanceVOsForTaskInstance(Long pTaskInstId)
   throws DataAccessException;

   /**
    * Method that creates the VariableMapping identified by the passed in
    * MappingOwnerID, MappingOwnerName, variableID and variableRefAs
    *
    * @param pMappingOwnerID
    * @param pMappingOwnerName
    * @param variableID
    * @param variableRefAs
    * @param pOptInd
    * @param pSrc
    * @param pSeq
    * @return VariableMapping
    */
   public void createVariableMapping(Long pMappingOwnerID, String pMappingOwnerName,
       Long pVariableId, String pVariableRefAs, Integer pDisplayMode, Integer pSeq)
   throws DataAccessException;

   /**
    * Method that deletes the variable mapping
    *
    * @param pMappingOwnerID
    * @param pMappingOwnerName
    * @param variableID
    * @return VariableMapping
    */
   public void deleteVariableMapping(Long pMappingOwnerID, String pMappingOwnerName, Long pVariableId)
   throws DataAccessException;

   /**
    * Method that updates the variable mapping
    *
    * @param pMappingOwnerID
    * @param pMappingOwnerName
    * @param variableID
    * @param pVarRefAs
    * @param pOpt
    * @param pSrc
    * @param pSeq
    * @return VariableMapping
    */
   public void updateVariableMapping(Long pMappingOwnerID, String pMappingOwnerName,
       Long pVariableId, String pVarRefAs, Integer pDisplayMode, Integer pSeq)
   throws DataAccessException;

   /**
    * Used by ESOWF task manager only - not used by MDW
    * @param procInstId
    * @return
    * @throws DataAccessException
    */
   public VariableInstanceVO[] getProcessInstanceVariables(Long procInstId)
   throws DataAccessException;

   /**
    * Get the list of group names to which the given task template is associated with
    * @param taskId
    * @return
    * @throws DataAccessException
    */
   public List<String> getGroupsForTask(Long taskId)
   throws DataAccessException;

   /**
    * Get the list of group names to which the given task instance is associated with
    * @param taskInst
    * @return
    * @throws DataAccessException
    */
   public List<String> getGroupsForTaskInstance(TaskInstanceVO taskInst)
   throws DataAccessException, TaskException;

   /**
    * Save a resource into database. This is used to import resources
    * @param resource
    * @return
    * @throws DataAccessException
    */
   public Long saveResource(RuleSetVO resource)
   throws DataAccessException;

   /**
    * Saves a workflow asset and its package association. Always saves a new version.
    * @param workflowPackage
    * @param asset
    * @param user making the change
    * @return id
    */
   public Long saveAsset(String workflowPackage, RuleSetVO asset, String user)
   throws DataAccessException, TaskException;

   /**
    * This method is used to build a where clause
    * for the given search criteria when querying for task instances.
    * The criteria a hash map containing potentially multiple
    *     conditions (with conjunction semantics).
    * Each condition is a key-value pair that will be translated
    *     into SQL where clause condition as key=value.
    * The keys can be:
    * <ul>
    *   <li>A column name in TASK_INSTANCE table prefixed by <code>ti_</code></li>
    *   <li>"<code>cuid</code>", and the value is the CUID of a user</li>
    *   <li>"<code>groups</code>", and the value must be a list of group names
    *       delimited by commas</li>
    *   <li>A task index key name (values in the INDEX_KEY column of the table TASK_INST_INEX)
    *     prefixed by <code>ix_</code></li>
    * </ul>
    * @param criteria see above
    * @return
    */
   public String buildFromWhereClause(Map<String,String> criteria);

   /**
    * Count the total number of task instances satisfying the
    * from-where clause constructed by the method buildFromWhereClause.
    * @param fromWhereClause from-where clause string constructed
    *     from query criteria by the method buildFromWhereClause
    * @return number of task instances satisfying the query criteria.
    * @throws TaskException
    * @throws DataAccessException
    */
   public int countTaskInstances(String fromWhereClause)
   throws TaskException, DataAccessException;

   /**
    * Load a page of task instances satisfying the
    * from-where clause constructed by the method buildFromWhereClause.
    * @param fromWhereClause
    * @param startIndex the index of the start row on the page. The first row has index 0
    * @param endIndex the index of the end row on the page plus one
    * @param sortOn the column name on which the task instances are sorted.
    *     If this is null, the sorting is based on task instance ID.
    *     The default sorting order is ascending, and if a descending order
    *     is desired, a "-" should be prefixed to the column name.
    * @param loadIndices when it is true, the query also loads
    *     indices for each task instance as stored in the TASK_INST_INDEX table.
    *     The indices can be accessed through TaskInstanceVO.getVariables()
    * @return a page of task instance list
    * @throws TaskException
    * @throws DataAccessException
    */
   public List<TaskInstanceVO> queryTaskInstances(String fromWhereClause,
      int startIndex, int endIndex, String sortOn, boolean loadIndices)
   throws TaskException, DataAccessException;

   /**
    * Count the total number of task templates satisfying the
    * where clause, which must be a valid SQL where clause composed
    * of field names in the TASK table.
    * @param whereCondition the where clause
    * @return
    * @throws DataAccessException
    */
   public int countTasks(String whereCondition) throws DataAccessException;

   /**
    * Retrieve a page of task templates satisfying the where clause,
    * which must be a valid SQL where clause composed
    * of field names in the TASK table.
    * @param whereCondition
    * @param startIndex the index of the start row on the page. The first row has index 0
    * @param endIndex the index of the end row on the page plus one
    * @param sortOn the column name on which the task templates are sorted.
    *     The default sorting order is ascending, and if a descending order
    *     is desired, a "-" should be prefixed to the column name.
    * @return
    * @throws DataAccessException
    */
   public List<TaskVO> queryTasks(String whereCondition, int startIndex, int endIndex, String sortOn)
   throws DataAccessException;

   public List<VariableInstanceVO> constructVariableInstancesFromFormDataDocument(TaskVO taskVO, Long processInstanceId, FormDataDocument datadoc) throws DataAccessException;

   public List<SubTask> getSubTaskList(TaskRuntimeContext runtimeContext) throws TaskException;

   /**
   * Get all subtask instances for a specified master task instance id.
   */
  public List<TaskInstanceVO> getSubTaskInstances(Long masterTaskInstanceId)
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
   * Update task instance data
   */
  public void updateTaskInstanceData(Map<String, Object> changes, List<String> workGroups, Long autoAssignee, TaskInstanceVO taskInst, String cuid)
      throws DataAccessException, TaskException;

  /**
   * Get the duedate,priority,workgroups and auo-assignee after applying strategy
   */
  public Map<String, Object> getChangesAfterApplyStrategy(Map<String, Object> changesMap, TaskInstanceVO taskInst)
      throws DataAccessException, TaskException;

  /**
   * Get the runtime context for a taskInstance.
   */
  public TaskRuntimeContext getTaskRuntimeContext(TaskInstanceVO taskInstanceVO)
      throws DataAccessException;

  /**
   * Get the Subtask plan for a specified taskInstance
   */
  public SubTaskPlan getSubTaskPlan(TaskRuntimeContext runtimeContext) throws TaskException;

  /**
   * Use setIndexes(TaskRuntimeContext).
   */
  @Deprecated
  public Map<String,String> collectIndices(Long taskId, Long processInstanceId, FormDataDocument formdatadoc) throws DataAccessException;

  public void setIndexes(TaskRuntimeContext runtimeContext) throws DataAccessException;

  public boolean isInFinalStatus(TaskInstanceVO pTaskInstance);

  public Long getActivityInstanceId(TaskInstanceVO taskInstance, boolean sourceActInst);

}