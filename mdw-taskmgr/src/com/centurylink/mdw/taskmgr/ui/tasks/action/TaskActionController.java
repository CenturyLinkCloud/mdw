/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.naming.NamingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.status.GlobalApplicationStatus;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceDataItem;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.GeneralTaskDetailHandler;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskInstanceActionController;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskInstanceDataItem;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.qwest.mbeng.MbengException;

/**
 * UI controller for task action events.
 */
public class TaskActionController extends EditableItemActionController implements ActionListener
{
  public static final String PERFORM_TASK_ACTION = "PerformTaskAction";
  public static final String PERFORM_TASK_ACTIONS = "PerformTaskActions";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String navigationOutcome;
  private List<String> errors = new ArrayList<String>();
  public List<String> getErrors() { return errors; }
  public boolean hasErrors()
  {
    return (errors != null && errors.size() > 0);
  }
  public void addError(String errorMessage)
  {
    errors.add(errorMessage);
  }

  private TaskAction taskAction;
  public TaskAction getTaskAction()
  {
    return taskAction;
  }
  public void setTaskAction(TaskAction taskAction)
  {
    this.taskAction = taskAction;
  }

  /**
   * @see javax.faces.event.ActionListener#processAction(javax.faces.event.ActionEvent)
   */
  @SuppressWarnings("unchecked")
  public void processAction(ActionEvent event) throws AbortProcessingException
  {
    errors = new ArrayList<String>(); // clear any errors

    UIComponent taskActionComponent = getContainingTaskAction(event.getComponent());
    Object applyTo = taskActionComponent.getAttributes().get("applyTo");

    // comments dialog
    if ("submitCommentsHiddenButton".equals(event.getComponent().getId()))
    {
      TaskAction taskAction = ((TaskAction)FacesVariableUtil.getValue("taskAction"));
      UIComponent parent = event.getComponent().getParent();
      for (UIComponent child : parent.getChildren())
      {
        if (child instanceof HtmlInputHidden)
        {
          HtmlInputHidden hidden = (HtmlInputHidden)child;
          if (hidden.getId().equals("submitCommentsHiddenAction"))
            taskAction.setAction(String.valueOf(hidden.getValue()));
          else if (hidden.getId().equals("submitCommentsHiddenComment"))
            taskAction.setComment(hidden.getValue() == null ? "" : hidden.getValue().toString());
          else if (hidden.getId().equals("submitCommentsHiddenItem"))
          {
            if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.ASSIGN))
              taskAction.setUserId(hidden.getValue() == null ? null : hidden.getValue().toString());
            else if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.FORWARD))
              taskAction.setDestination(hidden.getValue() == null ? null : hidden.getValue().toString());
          }
        }
      }
    }
    // handle selection from richfaces menu
    if (event.getSource() instanceof HtmlMenuItem)
    {
      HtmlMenuItem item = (HtmlMenuItem)event.getSource();
      TaskAction taskAction = ((TaskAction)FacesVariableUtil.getValue("taskAction"));
      if (item.getParent() instanceof HtmlMenuGroup)
      {
        // submenu selection
        HtmlMenuGroup group = (HtmlMenuGroup)item.getParent();
        Object nonAlias = group.getAttributes().get("nonAlias");
        taskAction.setAction(nonAlias != null ? nonAlias.toString() : group.getValue().toString());
        nonAlias = item.getData();  // item level
        if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.ASSIGN))
          taskAction.setUserId(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
        else if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.FORWARD))
          taskAction.setDestination(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
      }
      else
      {
        Object nonAlias = item.getData();
        taskAction.setAction(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
      }

      if (applyTo instanceof List)
      {
        taskAction.setCommentRequired(TaskActions.isBulkCommentRequired(taskAction.getAction()));
      }
      else if (applyTo instanceof TaskDetail)
      {
        TaskDetail taskDetail = (TaskDetail) applyTo;
        taskAction.setCommentRequired(TaskActions.isCommentRequired(taskAction.getAction(), taskDetail.getStatus()));
        // determine dynamicism
        taskAction.setDynamic(true);
        for (com.centurylink.mdw.model.data.task.TaskAction stdTaskAction : taskDetail.getStandardTaskActions())
        {
          if (stdTaskAction.getLabel().equals(taskAction.getAction()))
          {
            taskAction.setDynamic(false);
            break;
          }
        }
      }
    }

    if (applyTo == null)
    {
      throw new IllegalArgumentException("Missing applyTo object for TaskAction.");
    }
    else if (applyTo instanceof TaskDetail)
    {
      preProcessTaskDetailAction(event, (TaskDetail)applyTo);
    }
    else if (applyTo instanceof List)
    {
      preProcessTaskListAction(event, (List<FullTaskInstance>)applyTo);
    }

    processAction(applyTo);

    if (errors != null && errors.size() > 0)
    {
      for (int i = 0; i < errors.size(); i++)
      {
        String error = errors.get(i);
        logger.severe("Task Action ERROR: " + error);
        FacesVariableUtil.addMessage(error);
      }
      throw new AbortProcessingException("Errors in performing task action.");
    }
  }

  /**
   * Process the selected task action.
   *
   * @param applyTo the entity to apply the task action to
   */
  @SuppressWarnings("unchecked")
  protected void processAction(Object applyTo)
  {
    FacesVariableUtil.setValue("taskActionController", this);

    TaskAction taskAction = (TaskAction) FacesVariableUtil.getValue("taskAction");
    if (taskAction == null || taskAction.getAction() == null)
    {
      throw new IllegalArgumentException("Missing or invalid 'taskAction' faces variable: " + taskAction);
    }

    // taskAction values are set in selectOneMenu tag in taskAction.xhtml
    String action = taskAction.getAction();
    if (action == null || action.trim().length() == 0 || action.equals("Select an Action"))
    {
      errors.add("Please select an action to perform.");
      return;
    }

    if (action.equals("Cancel"))
    {
      if (((applyTo instanceof List && TaskActions.isBulkCommentRequired(taskAction.getAction()))
          || (applyTo instanceof TaskDetail && TaskActions.isCommentRequired(taskAction.getAction(), ((TaskDetail)applyTo).getStatus())))
        && taskAction.isCommentEmpty())
      {
        errors.add("Please enter a comment for the task(s).");
        return;
      }
    }

    if (action.equals("Assign"))
    {
      if (taskAction.isUserEmpty())
      {
        errors.add("Please select a user to receive assigned task(s).");
        return;
      }
    }

    if (action.equals("Forward"))
    {
      if (taskAction.isDestinationEmpty())
      {
        errors.add("Please select a destination for forwarding the task(s).");
        return;
      }
    }

    Long userId = new Long(FacesVariableUtil.getCurrentUser().getUserId());
    Long assigneeId = new Long(taskAction.getUserId());
    String comment = taskAction.getComment();
    String destination = taskAction.getDestination();

    if (taskAction.getAction().equals("Claim"))
    {
      assigneeId = userId;
    }

    try
    {
      // actions can apply to either a list or a task detail
      if (applyTo instanceof List)
      {
        List<FullTaskInstance> taskList = (List<FullTaskInstance>) applyTo;
        navigationOutcome = TaskActions.getBulkNavigationOutcome(action);
        performTaskListValidations(action, taskList);
        if (hasErrors())
          return;
        performActionOnTaskInstances(taskList, action, taskAction.isDynamic(), comment, userId, assigneeId, destination);
      }
      else if (applyTo instanceof TaskDetail)
      {
        TaskDetail taskDetail = (TaskDetail) applyTo;
        navigationOutcome = TaskActions.getTaskStatusNavigationOutcome(action, taskDetail.getStatus());
        if (navigationOutcome != null && navigationOutcome.equals("go_myTaskDetail")
        		&& taskDetail.getFullTaskInstance().isGeneralTask()
        		&& !taskDetail.getFullTaskInstance().isAutoformTask())
            navigationOutcome = new GeneralTaskDetailHandler(taskDetail.getFullTaskInstance()).getMyTaskDetailNavOutcome();
        else if (navigationOutcome == null && taskAction.isDynamic())
            navigationOutcome = TaskActions.convertNavigationOutcome("tasks");

        performTaskDetailValidations(action, taskDetail);
        if (hasErrors())
          return;
        performActionOnTaskInstance((FullTaskInstance)taskDetail.getModelWrapper(), action, taskAction.isDynamic(), comment, userId, assigneeId, destination);
        // refresh the task detail data after performing action
        String navOutcome = taskDetail.getNavOutcome();
        DetailManager.getInstance().setTaskDetail(taskDetail.getInstanceId());
        if (navOutcome != null)
          DetailManager.getInstance().getTaskDetail().setNavOutcome(navOutcome);
      }
      else
      {
        throw new IllegalArgumentException("Bad applyTo object for TaskAction: " + applyTo);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      errors.add(ex.getMessage());
      return;
    }
  }

  /**
   * Performs custom validation for actions on taskDetail.  Calls addError() to
   * associate an error message with the taskDetail.
   * @param action the action to be performed
   * @param taskDetail the taskDetail instance
   */
  protected void performTaskDetailValidations(String action, TaskDetail taskDetail)
  {
    boolean isAutosave = TaskActions.isAutosaveEnabled(action, taskDetail.getStatus());
    if (isAutosave)
    {
      // validate that required data has been entered
      checkRequiredFieldsEntered(taskDetail);
    }
  }

  /**
   * Performs custom validation for actions on taskList.  Calls addError() to
   * associate an error message with the taskList.
   * @param action the action to be performed
   * @param taskList the taskList instance
   */
  protected void performTaskListValidations(String action, List<FullTaskInstance> taskList)
  {
    boolean isAutosave = TaskActions.isBulkAutosaveEnabled(action);
    if (isAutosave)
    {
      checkRequiredFieldsEntered(taskList);
    }
  }

  /**
   * Perform an action on a single task instance.
   */
  protected void performActionOnTaskInstance(FullTaskInstance task, String action, boolean dynamic, String comment, Long userId, Long assigneeId, String destination)
  throws TaskException, DataAccessException, CachingException, MbengException, NamingException, MDWException, UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      // retrieve the instance in case status has changed
      task.setTaskInstance(taskMgr.getTaskInstanceVO(task.getTaskInstance().getTaskInstanceId()));

      String currentStatus = task.getStatus();

      AllowableAction allowableAction = TaskActions.getTaskStatusAllowableAction(action, currentStatus);
      // standard allowable actions are config-driven; dynamic are unvalidated except final status
      if ((!dynamic && allowableAction == null) || (dynamic && taskMgr.isInFinalStatus(task.getTaskInstance()))) {
        DetailManager.getInstance().setTaskDetail(task.getWrappedId()); // force refresh since status may have changed
        throw new TaskException("Task: " + task.getWrappedId() + " Action=" + action + " not allowed for Status=" + currentStatus);
      }

      // handle forced outcome
      if (!dynamic && allowableAction.getOutcome() != null)
        action += "::" + allowableAction.getOutcome();

      if (TaskAction.DEFAULT_COMMENT.equals(comment))
        comment = null;

      if (logger.isDebugEnabled())
      {
        logger.debug("\n performActionOnTaskInstance:\n"
            + "   task instance id = " + task.getWrappedId() + "   "
            + "action = " + action + "   "
            + "user = " + FacesVariableUtil.getCurrentUser().getCuid() + "   "
            + "comment = " + comment + "   "
            + "assigneeId = " + assigneeId + "   "
            + "destination = " + destination);
      }

      if (task.isDetailOnly())
      {
        TaskActionVO notifyAction = new TaskActionVO();
        notifyAction.setTaskAction(dynamic ? com.centurylink.mdw.model.data.task.TaskAction.COMPLETE : action);
        notifyAction.setTaskInstanceId(new Long(task.getAssociatedInstanceId()));
        notifyAction.setUser(UserGroupCache.getUser(userId).getCuid());
        if (com.centurylink.mdw.model.data.task.TaskAction.ASSIGN.equals(action))
          notifyAction.setAssignee(UserGroupCache.getUser(assigneeId).getCuid());
        if (com.centurylink.mdw.model.data.task.TaskAction.FORWARD.equals(action))
          notifyAction.setWorkgroup(destination);
        if (comment != null)
          notifyAction.setComment(comment);
        if (GlobalApplicationStatus.getInstance().getSystemStatusMap().isEmpty()
            || GlobalApplicationStatus.ONLINE.equals(GlobalApplicationStatus.getInstance().getSystemStatusMap().get(GlobalApplicationStatus.SUMMARY_TASK_APPNAME)))
        {
          StatusMessage response = TaskManagerAccess.getInstance().notifySummaryTaskManager(PERFORM_TASK_ACTION,notifyAction);
          if (!response.isSuccess())
            throw new TaskException(response.getMessage());
        }
        else
        {
          addError("The summary task manager application is not reachable. Hence unable to process the request");
          logger.warn("The summary task manager application is not reachable. Hence unable to process the request. " + task.getWrappedId());
        }
      }
      if(hasErrors()){
        return;
      }
      taskMgr.performActionOnTaskInstance(action, new Long(task.getWrappedId()), userId, assigneeId, comment, destination,
              OwnerType.PROCESS_INSTANCE.equals(task.getOwnerType())); // notifyEngine only when owner is process
    }
    catch (Exception ex)
    {
      addError(ex.toString());
      logger.severeException(ex.getMessage(), ex);
    }
  }

  protected void performActionOnTaskInstances(List<FullTaskInstance> taskList, String action, boolean dynamic, String comment, Long userId, Long assigneeId)
  throws TaskException, DataAccessException
  {
     performActionOnTaskInstances(taskList, action, dynamic, comment, userId, assigneeId, null);
  }

  /**
   * Perform an action on a list of task instances.
   */
  protected void performActionOnTaskInstances(List<FullTaskInstance> taskList, String action, boolean dynamic, String comment, Long userId, Long assigneeId, String destination)
  throws TaskException, DataAccessException
  {
    if (taskList.size() == 0)
    {
      errors.add("Please select task(s) to perform action on.");
      return;
    }

    boolean notifyEngine = !TaskManagerAccess.getInstance().isRemoteDetail();
    List<Long> taskInstanceIdList = new ArrayList<Long>();
    Map<Long,Boolean> notifyEngineMap = new HashMap<Long,Boolean>();

    if (TaskAction.DEFAULT_COMMENT.equals(comment))
      comment = null;

    TaskManager taskMgr = RemoteLocator.getTaskManager();

    for (int i = 0; i < taskList.size(); i++)
    {
      FullTaskInstance task = (FullTaskInstance) taskList.get(i);
      taskInstanceIdList.add(new Long(task.getWrappedId()));

      // retrieve the instance in case status has changed
      task.setTaskInstance(taskMgr.getTaskInstanceVO(task.getTaskInstance().getTaskInstanceId()));
      notifyEngineMap.put(task.getInstanceId(), OwnerType.PROCESS_INSTANCE.equals(task.getOwnerType()) && notifyEngine);
      String currentStatus = task.getStatus();

      AllowableAction allowableAction = TaskActions.getTaskStatusAllowableAction(action, currentStatus);
      if (!dynamic && allowableAction == null || (dynamic && taskMgr.isInFinalStatus(task.getTaskInstance())))
        errors.add("Task: " + task.getWrappedId() + "  Action=" + action + " not allowed for Status=" + currentStatus);

      if (!isUserAuthorizedToPerformTaskAction(task))
      {
        errors.add("Not authorized to perform action on Task: "+ task.getWrappedId());
      }
    }
    if (errors != null && errors.size() > 0)
    {
      return;
    }

    AllowableAction bulkAction = TaskActions.getAllowableBulkAction(action);
    if (bulkAction.getOutcome() != null)
      action += "::" + bulkAction.getOutcome();

    if (logger.isDebugEnabled())
    {
      logger.debug("\n performActionOnTaskInstanceCollection:\n"
          + "   task instances = " + taskInstanceIdList + "   "
          + "action = " + action + "   "
          + "user = " + FacesVariableUtil.getCurrentUser().getCuid() + "   "
          + "comment = " + comment + "   "
          + "assigneeId = " + assigneeId);
    }

    if (TaskManagerAccess.getInstance().isRemoteDetail())
    {
      notifyDetailTaskManagersNPerformAction(taskList, action, comment, userId, assigneeId, destination, taskMgr, notifyEngineMap);
    }
    else
    {
      for (Long taskInstanceId : taskInstanceIdList)
      {
        taskMgr.performActionOnTaskInstance(action, taskInstanceId, userId, assigneeId, comment, null, notifyEngineMap.get(taskInstanceId));
      }
    }
  }

  /**
   * @param task
   * @return
   */
  private boolean isUserAuthorizedToPerformTaskAction(FullTaskInstance taskInstance)
  {
    return isTaskAssignedToCurrentUser(taskInstance) || isApplicableForCurrentUsersWorkgroups(taskInstance);
  }

  public boolean isTaskAssignedToCurrentUser(FullTaskInstance taskInstance)
  {
    if (!"Assigned".equals(taskInstance.getStatus())
        && !"In Progress".equals(taskInstance.getStatus()))
    {
      return false;
    }
    else
    {
      return FacesVariableUtil.getCurrentUser().getCuid().equals(taskInstance.getAssignee());
    }
  }

  public boolean isApplicableForCurrentUsersWorkgroups(FullTaskInstance taskInstance)
  {
    List<String> taskGroups = taskInstance.getTaskInstance().getGroups();
    if (taskGroups != null && taskGroups.size() > 0)
    {
      String[] userGroups = FacesVariableUtil.getCurrentUser().getWorkgroupNames();
      for (String g : userGroups)
      {
        if (taskGroups.contains(g))
          return true;
      }
      return false;
    }
    else
    {
      try
      {
        TaskVO[] tasks = ((Tasks) FacesVariableUtil.getValue("tasks")).getTasksForUserWorkgroups();
        for (int i = 0; i < tasks.length; i++)
        {
          if (tasks[i].getTaskId().longValue() == taskInstance.getTaskId())
            return true;
        }
      }
      catch (UIException ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
      return false;
    }
  }

  private void notifyDetailTaskManagersNPerformAction(List<FullTaskInstance> taskList, String action, String comment, Long userId, Long assigneeId, String destination, TaskManager taskMgr, Map<Long, Boolean> notifyEngineMap)
          throws TaskException, DataAccessException
  {
    final Map<String, List<String>> assocTaskAppToIds = new HashMap<String, List<String>>();
    List<Long> processedTaskInsIds = new ArrayList<Long>();
    List<Long> unprocessedTaskInsIds = new ArrayList<Long>();
    Map<String, Long> taskInstRelMap = new HashMap<String, Long>(); // associatedtaskInstanceId,taskInstanceID
    for (FullTaskInstance taskInst : taskList)
    {
      if (taskInst.isSummaryOnly())
      {
        List<String> assocTaskInstIds = assocTaskAppToIds.get(taskInst.getOwnerApplicationName());
        if (assocTaskInstIds == null)
          assocTaskInstIds = new ArrayList<String>();
        assocTaskInstIds.add(taskInst.getAssociatedInstanceId());
        assocTaskAppToIds.put(taskInst.getOwnerApplicationName(), assocTaskInstIds);
        taskInstRelMap.put(taskInst.getAssociatedInstanceId(), new Long(taskInst.getWrappedId()));
      }
    }
    if (assocTaskAppToIds.size() > 0)
    {
      try
      {
        TaskActionVO notifyAction = new TaskActionVO();
        notifyAction.setTaskAction(action);
        notifyAction.setUser(UserGroupCache.getUser(userId).getCuid());
        if (com.centurylink.mdw.model.data.task.TaskAction.ASSIGN.equals(action))
          notifyAction.setAssignee(UserGroupCache.getUser(assigneeId).getCuid());
        if (com.centurylink.mdw.model.data.task.TaskAction.FORWARD.equals(action))
          notifyAction.setWorkgroup(destination);
        if (comment != null)
          notifyAction.setComment(comment);

        for (final String ownerApp : assocTaskAppToIds.keySet())
        {
          Map<String, String> applicationStatus = GlobalApplicationStatus.getInstance().getSystemStatusMap();
          List<Long> taskInstList = getTaskInstIds(taskInstRelMap, assocTaskAppToIds.get(ownerApp),null);
          if (applicationStatus.isEmpty() || GlobalApplicationStatus.ONLINE.equals(applicationStatus.get(ownerApp)))
          {
            TaskActionVO detailNotifyAction = new TaskActionVO(notifyAction.getJson())
            {
              @Override
              public JSONObject getJson() throws JSONException
              {
                JSONObject json = super.getJson();
                JSONArray taskInstsJson = new JSONArray();
                for (String assocTaskInstId : assocTaskAppToIds.get(ownerApp))
                  taskInstsJson.put(new Long(assocTaskInstId).longValue());
                json.put("taskInstanceIds", taskInstsJson);
                return json;
              }
            };
            StatusMessage response = TaskManagerAccess.getInstance().notifyDetailTaskManager(PERFORM_TASK_ACTIONS, detailNotifyAction, ownerApp);
            if (!response.isSuccess())
            {
              logger.severeException("Unable to notify remote detail", new TaskException(response.getMessage()));
              List<Long> unProTaskInstList = getTaskInstIds(taskInstRelMap,assocTaskAppToIds.get(ownerApp), response.getMessage()); // To get only unprocessed taskInstIds
              addError("Unable to process task(s) :" + unProTaskInstList);
              unprocessedTaskInsIds.addAll(unProTaskInstList);
              taskInstList.removeAll(unProTaskInstList);
              processedTaskInsIds.addAll(taskInstList);
            }
            else
            {
              processedTaskInsIds.addAll(taskInstList);
            }
          }
          else
          {
            addError("The remote detail application is not reachable :" + ownerApp+ " hence unable to process task(s) :" + taskInstList);
            unprocessedTaskInsIds.addAll(taskInstList);
          }
        }
      }
      catch (Exception ex)
      {
        StringBuffer msg = new StringBuffer();
        msg.append("Unable to process task(s) : ").append(ex.getMessage());
        if (!processedTaskInsIds.isEmpty())
        {
          msg = new StringBuffer();
          msg.append("Able to process only : ").append(processedTaskInsIds).append(". Unable to process other task(s) : ").append(ex.getMessage());
        }
        throw new TaskException(msg.toString(), ex);
      }
      finally
      {
        for (Long taskInstanceId : processedTaskInsIds)
        {
          taskMgr.performActionOnTaskInstance(action, taskInstanceId, userId, assigneeId, comment, null, notifyEngineMap.get(taskInstanceId));
        }
      }
    }
  }
  private List<Long> getTaskInstIds(Map<String,Long> taskInstRelMap, List<String> assTaskInstanceIds, String message)
  {
    List<Long> taskInstIds = new ArrayList<Long>();
    if (null != message && message.contains("UnProcessedTaskInstIds:"))
    {
      String[] errorMessage = message.split(":");
      String[] assTaskInstIds = errorMessage[(errorMessage.length - 1)].split(",");
      for (String assTaskInstId : assTaskInstIds)
      {
        taskInstIds.add(taskInstRelMap.get(assTaskInstId));
      }
    }
    else
    {
      for (String assTaskInstId : assTaskInstanceIds)
      {
        taskInstIds.add(taskInstRelMap.get(assTaskInstId));
      }
    }
    return taskInstIds;
  }
  /**
   * Allows custom pre-processing of TaskDetail before the task action is performed.
   * @param event the JSF action event
   */
  protected void preProcessTaskDetailAction(ActionEvent event, TaskDetail taskDetail)
  {
    String action = ((TaskAction)FacesVariableUtil.getValue("taskAction")).getAction();
    boolean isAutosave = TaskActions.isAutosaveEnabled(action, taskDetail.getStatus());
    if (isAutosave)
    {
      // save the input data values
      TaskInstanceActionController taskInstanceActionController = new TaskInstanceActionController();
      taskInstanceActionController.processAction(event);
    }
    if (taskDetail.getFullTaskInstance().isGeneralTask()
        && com.centurylink.mdw.model.data.task.TaskAction.COMPLETE.equals(action))
    {
      // clear the formDataDoc task instance ID
      try
      {
        FormDataDocument formDataDocument = (FormDataDocument) FacesVariableUtil.getValue("formDataDocument");
        formDataDocument.setAttribute(FormDataDocument.ATTR_ID, null);
        String varName = formDataDocument.getMetaValue(FormDataDocument.META_FORM_DATA_VARIABLE_NAME);
        MDWProcessInstance processInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        DocumentReference docRef = (DocumentReference) processInstance.getVariables().get(varName);
        if (docRef != null)
        {
          UIDocument uiDoc = processInstance.getDocument(docRef);
          EventManager eventMgr = RemoteLocator.getEventManager();
          eventMgr.updateDocumentContent(docRef.getDocumentId(), uiDoc.getDocumentVO().getContent(), uiDoc.getType());
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
    }
  }

  @Deprecated
  protected void preProcessTaskListAction(ActionEvent event, List<FullTaskInstance> taskList)
  {
  }

  /**
   * Allows custom pre-processing of Task List before the task action is performed.
   * @param event the JSF action event
   */
  protected void preProcessTaskListAction(SortableList taskList)
  {
  }

  /**
   * Default behavior is for AutoForm and classic tasks (checks the required variables
   * specified in the task attribute or variable mappings, respectively).
   */
  protected void checkRequiredFieldsEntered(TaskDetail taskDetail)
  {
    List<InstanceDataItem> instanceDataList = taskDetail.getInstanceDataItems();
    for (int i = 0; i < instanceDataList.size(); i++)
    {
      TaskInstanceDataItem tidi = (TaskInstanceDataItem) instanceDataList.get(i);
      if (tidi.isValueEditable() && tidi.isValueRequired())
      {
        Object instanceObj = tidi.getTaskInstanceData().getData();
        if (instanceObj == null)
        {
          addError("Required field data missing: '" + tidi.getDataKeyName() + "'");
        }
        else if (instanceObj instanceof String)
        {
          if (StringHelper.isEmpty((String)instanceObj))
          {
            addError("Required field data missing: '" + tidi.getDataKeyName() + "'");
          }
        }
      }
    }
  }

  /**
   * Default behavior is for AutoForm and classic tasks (checks the required variables
   * specified in the task attribute or variable mappings, respectively).
   */
  protected void checkRequiredFieldsEntered(List<FullTaskInstance> taskList)
  {
    try
    {
      TaskManager varMgr = RemoteLocator.getTaskManager();

      for (int i = 0; i < taskList.size(); i++)
      {
        FullTaskInstance taskInst = taskList.get(i);
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
        if (taskVO.isAutoformTask() || (!taskVO.isGeneralTask() && !taskVO.isNeoClassicTask()))
        {
          Long taskInstanceId = taskInst.getInstanceId();

          List<VariableInstanceVO> variableInstanceArr = varMgr.getVariableInstanceVOsForTaskInstance(taskInstanceId);
          for (VariableInstanceVO var : variableInstanceArr)
          {
            if (var.isRequired() && var.isEditable() &&
                (var.getData() == null || var.getData().equals("")))
            {
              addError("Required fields for task: " + taskInstanceId.intValue()+" must be entered.");
              break; // prevent multiple message for same taskInstanceId
            }
          }
        }
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      addError(ex.getMessage());
      return;
    }
  }

  protected com.centurylink.mdw.taskmgr.jsf.components.TaskAction getContainingTaskAction(UIComponent component)
  {
    if (component.getParent() == null)
    {
      throw new IllegalArgumentException("Containing TaskAction component not found.");
    }
    if (component.getParent() instanceof com.centurylink.mdw.taskmgr.jsf.components.TaskAction)
    {
      return (com.centurylink.mdw.taskmgr.jsf.components.TaskAction) component.getParent();
    }
    else
    {
      return getContainingTaskAction(component.getParent());
    }
  }

  public String refresh() throws UIException
  {
    errors = new ArrayList<String>();
    taskAction = new TaskAction();

    // handle remote task manager navigation
    if ("go_workgroupTasks".equals(navigationOutcome))
      return TaskListScopeActionController.getInstance().goWorkgroupTasks();
    else if ("go_myTasks".equals(navigationOutcome))
      return TaskListScopeActionController.getInstance().goMyTasks();

    return navigationOutcome;
  }

  public void completeTask(ActionEvent event) throws UIException, AbortProcessingException
  {
    TaskAction taskAction = new TaskAction();
    taskAction.setAction(com.centurylink.mdw.model.data.task.TaskAction.COMPLETE);
    taskAction.setUserId(String.valueOf(FacesVariableUtil.getCurrentUser().getUserId()));
    FacesVariableUtil.setValue("taskAction", taskAction);
    Object applyTo = DetailManager.getInstance().getTaskDetail();
    preProcessTaskDetailAction(event, (TaskDetail)applyTo);
    processAction(applyTo);

    if (errors != null && errors.size() > 0)
    {
      for (int i = 0; i < errors.size(); i++)
      {
        FacesVariableUtil.addMessage((String)errors.get(i));
      }
      throw new AbortProcessingException("Errors in completing task.");
    }
  }
}
