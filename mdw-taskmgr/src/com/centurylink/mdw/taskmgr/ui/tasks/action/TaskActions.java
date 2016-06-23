/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Utility class for accessing allowable task action information for
 * the current user (see TaskActions.xml).  Also provides the caching
 * of task action reference data from the workflow.
 */
public class TaskActions
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String uiTaskActionsFile;

  public boolean allowableTaskActionsLoaded;

  private static TaskActions taskMgrInstance;
  private static TaskActions hubInstance;
  public static TaskActions getInstance()
  {
    return getInstance(TaskListScopeActionController.getInstance().isMdwHubRequest());
  }
  public static TaskActions getInstance(boolean isMdwHub)
  {
    if (isMdwHub)
    {
      if (hubInstance == null)
      {
        hubInstance = new TaskActions();
        hubInstance.uiTaskActionsFile = PropertyManager.getProperty(PropertyNames.MDW_HUB_ACTION_DEF);
      }
      return hubInstance;
    }
    else
    {
      if (taskMgrInstance == null)
      {
        taskMgrInstance = new TaskActions();
        taskMgrInstance.uiTaskActionsFile = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_ACTIONS_FILE);
        if (taskMgrInstance.uiTaskActionsFile == null)
          taskMgrInstance.uiTaskActionsFile = "MDWTaskActions.xml";
      }
      return taskMgrInstance;
    }
  }

  private static List<TaskStatusAllowableActions> taskStatusAllowableActions;
  /**
   * @return list of AllowableActions that are allowed depending on the
   * current task status (used when an action is performed on a single task
   * instance -- ie: from the taskDetail view).
   */
  public static List<TaskStatusAllowableActions> getTaskStatusAllowableActions()
  {
    TaskActions taskActions = getInstance();
    if (!taskActions.allowableTaskActionsLoaded)
      taskActions.loadAllowableTaskActions();
    return taskStatusAllowableActions;
  }

  private List<AllowableAction> allowableBulkActions;
  /**
   * @return list of AllowableActions that are allowed when multiple tasks
   * are selected from among UNCLAIMED tasks
   */
  public static List<AllowableAction> getAllowableBulkActions()
  {
    TaskActions taskActions = getInstance();
    if (!taskActions.allowableTaskActionsLoaded)
      taskActions.loadAllowableTaskActions();
    return taskActions.allowableBulkActions;
  }

  public static AllowableAction getAllowableBulkAction(String action)
  {
    for (AllowableAction bulkAction : getAssignedBulkActions())
    {
      if (bulkAction.getName().equals(action))
        return bulkAction;
    }
    for (AllowableAction bulkAction : getAllowableBulkActions())
    {
      if (bulkAction.getName().equals(action))
        return bulkAction;
    }
    return null;
  }

  private List<AllowableAction> assignedBulkActions;
  /**
   * @return list of AllowableActions that are allowed when multiple tasks
   * are selected from among CLAIMED or ASSIGNED or IN_PROGRESS tasks
   */
  public static List<AllowableAction> getAssignedBulkActions()
  {
    TaskActions taskActions = getInstance();
    if (!taskActions.allowableTaskActionsLoaded)
      taskActions.loadAllowableTaskActions();
    return taskActions.assignedBulkActions;
  }

  private static TaskAction[] allTaskActions;  // holds all task actions for admin gui
  public static TaskAction[] getAllTaskActions()
  {
    if (allTaskActions == null)
      refreshAll();
    return allTaskActions;
  }

  /**
   * Builds a list of task action select items from an array of VOs.
   *
   * @param taskActions task actions
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public List<SelectItem> buildSelectItemList(TaskAction[] taskActions, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
      selectItems.add(new SelectItem("0", firstItem));
    for (int i = 0; i < taskActions.length; i++)
    {
      TaskAction taskAction = taskActions[i];
      selectItems.add(new SelectItem(taskAction.getTaskActionId().toString(), taskAction.getTaskActionName()));
    }
    return selectItems;
  }

  /**
   * Preference order for loading action definition XML:
   * MDWHub: (TODO honor in-scope PackageVO for custom webapps)
   *   - Use global value in MDW properties file:
   *       - Plain filename resolves per rules below for TaskMgr
   *       - Path like MyPack/MyActions.xml loads from designated workflow asset
   *   - Otherwise use asset com.centurylink.mdw.hub/mdw-hub-actions.xml
   *   - Otherwise error
   * MDWTaskManagerWeb:
   *   - Otherwise load from filename designated in cfg via MDWFramework.TaskManagerWeb-ui.task.actions.file
   *       - Use the file resolution rules defined in FileHelper.openConfigurationFile().
   */
  public void loadAllowableTaskActions()
  {
    try
    {
      parseAllowableTaskActions();
      // add configuration-defined task actions to the database
      for (TaskStatusAllowableActions tsAllowableActions : taskStatusAllowableActions)
      {
        addConfigDrivenTaskActions(tsAllowableActions.getAllowableActions());
      }
      addConfigDrivenTaskActions(allowableBulkActions);
      addConfigDrivenTaskActions(assignedBulkActions);
      allowableTaskActionsLoaded = true;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  private void addConfigDrivenTaskActions(List<AllowableAction> allowableActions)
  {
    try
    {
      TaskManager taskManager = RemoteLocator.getTaskManager();
      for (AllowableAction allowableAction : allowableActions)
      {
        if (taskManager.getTaskAction(allowableAction.getName()) == null)
        {
          taskManager.addTaskAction(allowableAction.getName(), "Configuration driven task action");
        }
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  private void parseAllowableTaskActions()
  throws FileNotFoundException, IOException, SAXException
  {
    taskStatusAllowableActions = new ArrayList<TaskStatusAllowableActions>();
    assignedBulkActions = new ArrayList<AllowableAction>();
    allowableBulkActions = new ArrayList<AllowableAction>();

    Digester d = new Digester();
    d.push(this);

    d.addObjectCreate("taskActions/bulkTaskActions/allowableBulkActions/allowableAction", AllowableAction.class);
    d.addSetNext("taskActions/bulkTaskActions/allowableBulkActions/allowableAction", "addAllowableBulkAction");
    d.addCallMethod("taskActions/bulkTaskActions/allowableBulkActions/allowableAction", "setName", 0);
    d.addSetProperties("taskActions/bulkTaskActions/allowableBulkActions/allowableAction");
    d.addCallMethod("taskActions/bulkTaskActions/allowableBulkActions/allowableAction/navigationOutcome", "setNavigationOutcome", 0);
    d.addCallMethod("taskActions/bulkTaskActions/allowableBulkActions/allowableAction/autosave", "setAutosave", 0);

    d.addObjectCreate("taskActions/bulkTaskActions/assignedBulkActions/allowableAction", AllowableAction.class);
    d.addSetNext("taskActions/bulkTaskActions/assignedBulkActions/allowableAction", "addAssignedBulkAction");
    d.addCallMethod("taskActions/bulkTaskActions/assignedBulkActions/allowableAction", "setName", 0);
    d.addSetProperties("taskActions/bulkTaskActions/assignedBulkActions/allowableAction");
    d.addCallMethod("taskActions/bulkTaskActions/assignedBulkActions/allowableAction/navigationOutcome", "setNavigationOutcome", 0);
    d.addCallMethod("taskActions/bulkTaskActions/assignedBulkActions/allowableAction/autosave", "setAutosave", 0);

    d.addObjectCreate("taskActions/taskDetailActions/taskStatus", TaskStatusAllowableActions.class);
    d.addSetProperties("taskActions/taskDetailActions/taskStatus");
    d.addSetNext("taskActions/taskDetailActions/taskStatus", "addTaskStatusAllowableActions");
    d.addObjectCreate("taskActions/taskDetailActions/taskStatus/allowableAction", AllowableAction.class);
    d.addSetNext("taskActions/taskDetailActions/taskStatus/allowableAction", "addAllowableAction");
    d.addSetProperties("taskActions/taskDetailActions/taskStatus/allowableAction");
    d.addCallMethod("taskActions/taskDetailActions/taskStatus/allowableAction", "setName", 0);
    d.addCallMethod("taskActions/taskDetailActions/taskStatus/allowableAction/navigationOutcome", "setNavigationOutcome", 0);
    d.addCallMethod("taskActions/taskDetailActions/taskStatus/allowableAction/autosave", "setAutosave", 0);

    d.addObjectCreate("taskActions/taskDetailActions/taskStatus/allowableAction/forTask", ForTask.class);
    d.addSetNext("taskActions/taskDetailActions/taskStatus/allowableAction/forTask", "addForTask");
    d.addCallMethod("taskActions/taskDetailActions/taskStatus/allowableAction/forTask", "setTaskName", 0);
    d.addObjectCreate("taskActions/taskDetailActions/taskStatus/allowableAction/forTask/destination", TaskDestination.class);
    d.addSetNext("taskActions/taskDetailActions/taskStatus/allowableAction/forTask/destination", "addDestination");
    d.addCallMethod("taskActions/taskDetailActions/taskStatus/allowableAction/forTask/destination", "setName", 0);
    d.addSetProperties("taskActions/taskDetailActions/taskStatus/allowableAction/forTask/destination");

    InputStream is = null;
    try
    {
      RuleSetVO actionsAsset = null;
      boolean isMdwHub = TaskListScopeActionController.getInstance().isMdwHubRequest();
      if (isMdwHub && uiTaskActionsFile == null)
      {
        PackageVO packageVO = PackageVOCache.getPackage(PackageVO.MDW_HUB);
        if (packageVO == null)
        {
          // initialization scenario before hub package imported
          uiTaskActionsFile = "mdw-hub-actions.xml";
        }
        else
        {
          String actionsDefDocName = packageVO == null ? "mdw-hub-actions.xml" : packageVO.getProperty(PropertyNames.MDW_HUB_ACTION_DEF);
          if (actionsDefDocName != null)
          {
            actionsAsset = RuleSetCache.getRuleSet(actionsDefDocName);
          }
          else
          {
            // but override from user-controlled pkg if present
            String userHubPkgName = ApplicationContext.getHubOverridePackage();
            PackageVO userHubPkg = PackageVOCache.getPackage(userHubPkgName);
            if (userHubPkg != null && userHubPkg.getRuleSet("mdw-hub-actions.xml") != null)
              packageVO = userHubPkg;
            if (packageVO.getRuleSets() != null)
            {
              for (RuleSetVO ruleSetVO : packageVO.getRuleSets())
              {
                if ((ruleSetVO.getLanguage().equals(RuleSetVO.CONFIG) || ruleSetVO.getLanguage().equals(RuleSetVO.XML))
                    && (ruleSetVO.getName().endsWith("HubActions.xml") || ruleSetVO.getName().endsWith("hub-actions.xml")))
                {
                  actionsAsset = RuleSetCache.getRuleSet(ruleSetVO.getId());
                }
              }
            }
          }
        }
      }

      if (actionsAsset != null)  // custom actions defined for package
        is = new ByteArrayInputStream(actionsAsset.getRuleSet().getBytes());
      else
        is = FileHelper.openConfigurationFile(uiTaskActionsFile, Thread.currentThread().getContextClassLoader());

      d.parse(is);
    }
    finally
    {
      if (is != null)
      {
        is.close();
      }
    }
  }

  /**
   * Add a TaskStatusAllowableActions object to the reference collection.
   */
  public void addTaskStatusAllowableActions(TaskStatusAllowableActions tsaa)
  {
    taskStatusAllowableActions.add(tsaa);
  }

  /**
   * Add an allowable bulk task to the possibilities.
   */
  public void addAllowableBulkAction(AllowableAction taskAction)
  {
    allowableBulkActions.add(taskAction);
  }

  /**
   * Add an assigned bulk task to the possibilities.
   */
  public void addAssignedBulkAction(AllowableAction taskAction)
  {
    assignedBulkActions.add(taskAction);
  }

  /**
   * Returns all the task actions associated with a user role.
   * @param roleId the unique identifier of the role
   * @return array of model TaskActionVO objects
   */
  public static TaskAction[] getTaskActionsInRole(Long roleId)
  {
    List<TaskAction> taskActionsInRole = new ArrayList<TaskAction>();
    for (int i = 0; i < allTaskActions.length; i++)
    {
      if (allTaskActions[i].isRoleMapped(roleId))
        taskActionsInRole.add(allTaskActions[i]);
    }

    Collections.sort(taskActionsInRole, new TaskActionComparator());
    return (TaskAction[]) taskActionsInRole.toArray(new TaskAction[0]);
  }

  /**
   * Returns all the task actions NOT currently associated with a role.
   * @param roleId the unique identifier of the role
   * @return array of model TaskActionVO objects
   */
  public static TaskAction[] getTaskActionsNotInRole(Long roleId)
  {
    List<TaskAction> taskActionsNotInRole = new ArrayList<TaskAction>();
    TaskAction[] allTaskActions = getAllTaskActions();

    for (int i = 0; i < allTaskActions.length; i++)
    {
      if (!allTaskActions[i].isRoleMapped(roleId))
        taskActionsNotInRole.add(allTaskActions[i]);
    }

    Collections.sort(taskActionsNotInRole, new TaskActionComparator());
    return (TaskAction[]) taskActionsNotInRole.toArray(new TaskAction[0]);
  }

  public static void refreshAll()
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      allTaskActions = taskMgr.getTaskActionVOs();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public static boolean isTaskActionAllowedForStatus(String action, String status)
  {
    for (int i = 0; i < taskStatusAllowableActions.size(); i++)
    {
      TaskStatusAllowableActions tsaa = (TaskStatusAllowableActions) taskStatusAllowableActions.get(i);
      if (tsaa.getStatus().equals(status) && tsaa.containsAction(action))
      {
        return true;
      }
    }

    return false;
  }

  public static AllowableAction getTaskStatusAllowableAction(String action, String status)
  {
    for (int i = 0; i < taskStatusAllowableActions.size(); i++)
    {
      TaskStatusAllowableActions tsaa = (TaskStatusAllowableActions) taskStatusAllowableActions.get(i);
      if (tsaa.getStatus().equals(status) && tsaa.containsAction(action))
      {
        for (AllowableAction aa : tsaa.getAllowableActions())
        {
          if (aa.getName().equals(action))
            return aa;
        }
      }
    }
    return null;
  }

  static class TaskActionComparator implements Comparator<TaskAction>
  {
    public int compare(TaskAction taskAction1, TaskAction taskAction2)
    {
      return taskAction1.getTaskActionName().compareTo(taskAction2.getTaskActionName());
    }
  }

  /**
   * Determines the navigation outcome for a bulk task action.
   * @param action
   * @return the JSF navigation outcome
   */
  public static String getBulkNavigationOutcome(String action)
  {
    List<AllowableAction> allowableActions = null;
    if (TaskListScopeActionController.getInstance().getTaskListScope().isUser())
    {
      allowableActions = getAssignedBulkActions();
    }
    else if (TaskListScopeActionController.getInstance().getTaskListScope().isWorkgroup())
    {
      allowableActions = getAllowableBulkActions();
    }
    else
    {
      return null;
    }

    for (int i = 0; i < allowableActions.size(); i++)
    {
      AllowableAction allowableAction = (AllowableAction) allowableActions.get(i);
      if (allowableAction.getName().equals(action))
      {
        return convertNavigationOutcome(allowableAction.getNavigationOutcome());
      }
    }
    return null; // not found
  }

  public static boolean isBulkCommentRequired(String action)
  {
    List<AllowableAction> allowableActions = null;
    if (TaskListScopeActionController.getInstance().getTaskListScope().isUser())
    {
      allowableActions = getAssignedBulkActions();
    }
    else if (TaskListScopeActionController.getInstance().getTaskListScope().isWorkgroup())
    {
      allowableActions = getAllowableBulkActions();
    }
    else
    {
      return false;
    }

    for (int i = 0; i < allowableActions.size(); i++)
    {
      AllowableAction allowableAction = (AllowableAction) allowableActions.get(i);
      if (allowableAction.getName().equals(action))
      {
        return allowableAction.isRequireComment();
      }
    }
    return false; // not found
  }

  /**
   * Determines whether bulk list autosave is enabled for an action.
   * @param action
   * @return true if autosave is enabled
   */
  public static boolean isBulkAutosaveEnabled(String action)
  {
    List<AllowableAction> allowableActions = null;
    if (TaskListScopeActionController.getInstance().getTaskListScope().isUser())
    {
      allowableActions = getAssignedBulkActions();
    }
    else if (TaskListScopeActionController.getInstance().getTaskListScope().isWorkgroup())
    {
      allowableActions = getAllowableBulkActions();
    }
    else
    {
      return false;
    }

    for (int i = 0; i < allowableActions.size(); i++)
    {
      AllowableAction allowableAction = (AllowableAction) allowableActions.get(i);
      if (allowableAction.getName().equals(action))
      {
        return allowableAction.isAutosave();
      }
    }
    return false; // not found
  }

  /**
   * Determines the navigation outcome for an action on a TaskDetail.
   * @param action
   * @param status
   * @return the JSF navigation outcome
   */
  public static String getTaskStatusNavigationOutcome(String action, String status)
  {
    List<TaskStatusAllowableActions> taskStatusAllowableActionsList = getTaskStatusAllowableActions();

    for (int i = 0; i < taskStatusAllowableActionsList.size(); i++)
    {
      TaskStatusAllowableActions tsAllowableActions = (TaskStatusAllowableActions) taskStatusAllowableActionsList.get(i);
      if (tsAllowableActions.getStatus().equals(status))
      {
        List<AllowableAction> allowableActions = tsAllowableActions.getAllowableActions();
        for (int j = 0; j < allowableActions.size(); j++)
        {
          AllowableAction allowableAction = (AllowableAction) allowableActions.get(j);
          if (allowableAction.getName().equals(action))
          {
            return convertNavigationOutcome(allowableAction.getNavigationOutcome());
          }
        }
      }
    }

    return null; // not found
  }

  /**
   * Indicates whether autosave is enabled for a task action from the detail page.
   * @param action
   * @param status
   * @return true if autosave is enabled
   */
  public static boolean isAutosaveEnabled(String action, String status)
  {
    List<TaskStatusAllowableActions> taskStatusAllowableActionsList = getTaskStatusAllowableActions();

    for (int i = 0; i < taskStatusAllowableActionsList.size(); i++)
    {
      TaskStatusAllowableActions tsAllowableActions = (TaskStatusAllowableActions) taskStatusAllowableActionsList.get(i);
      if (tsAllowableActions.getStatus().equals(status))
      {
        List<AllowableAction> allowableActions = tsAllowableActions.getAllowableActions();
        for (int j = 0; j < allowableActions.size(); j++)
        {
          AllowableAction allowableAction = (AllowableAction) allowableActions.get(j);
          if (allowableAction.getName().equals(action))
          {
            return allowableAction.isAutosave();
          }
        }
      }
    }

//    return false; // disabled by default
    return true;	// changed by Jiyang to allow custom action to auto save - check with Don
  }

  public static boolean isCommentRequired(String action, String status)
  {
    List<TaskStatusAllowableActions> taskStatusAllowableActionsList = getTaskStatusAllowableActions();

    for (int i = 0; i < taskStatusAllowableActionsList.size(); i++)
    {
      TaskStatusAllowableActions tsAllowableActions = (TaskStatusAllowableActions) taskStatusAllowableActionsList.get(i);
      if (tsAllowableActions.getStatus().equals(status))
      {
        List<AllowableAction> allowableActions = tsAllowableActions.getAllowableActions();
        for (int j = 0; j < allowableActions.size(); j++)
        {
          AllowableAction allowableAction = (AllowableAction) allowableActions.get(j);
          if (allowableAction.getName().equals(action))
          {
            return allowableAction.isRequireComment();
          }
        }
      }
    }
    return false;
  }

  public static String convertNavigationOutcome(String outcome)
  {
    if (outcome == null)
      return null;
    else if (outcome.equals("tasks"))
      return TaskListScopeActionController.getTaskListNavigation();
    else if (outcome.startsWith("/"))
      return outcome;
    else
      return "go_" + outcome;
  }

  public static void clear()
  {
    logger.info("Task Action cache cleared");
    getInstance(true).allowableTaskActionsLoaded = false;
    getInstance(false).allowableTaskActionsLoaded = false;
  }

  public void reloadAllowableTaskActions()
  {
    try
    {
      loadAllowableTaskActions();
      FacesVariableUtil.addMessage("Task actions successfully reloaded from " + uiTaskActionsFile);

      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, Action.Refresh, Entity.File, new Long(0), uiTaskActionsFile);
      userAction.setSource("Task Manager");
      EventManager eventMgr = RemoteLocator.getEventManager();
      eventMgr.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      logger.severeException("Failed to reload " + uiTaskActionsFile + ": ", ex);
    }
  }

  public static List<ForTask> getForTasks(String action)
  {
    TaskActions taskActions = getInstance();
    if (!taskActions.allowableTaskActionsLoaded)
      taskActions.loadAllowableTaskActions();

    for (int i = 0; i < taskStatusAllowableActions.size(); i++)
    {
      TaskStatusAllowableActions tsAllowableActions = (TaskStatusAllowableActions) taskStatusAllowableActions.get(i);
      AllowableAction allowableAction = tsAllowableActions.getAllowableAction(action);
      if (allowableAction != null)
      {
        return allowableAction.getForTasks();
      }
    }
    return null; // not found
  }

  public static List<SelectItem> buildSelectItemList(List<TaskAction> taskActions)
  {
      List<SelectItem> selectItems = new ArrayList<SelectItem>();
      selectItems.add(new SelectItem("Select an Action"));
      for (TaskAction taskAction : taskActions)
      {
        selectItems.add(new SelectItem(taskAction, taskAction.getLabel()));
      }

    return selectItems;
  }

  public static List<TaskAction> getStandardTaskActionsForStatus(String taskName, String status)
  {
    List<TaskAction> validTaskActions = new ArrayList<TaskAction>();

    for (TaskStatusAllowableActions tsaa : TaskActions.getTaskStatusAllowableActions())
    {
      if (tsaa.getStatus().equals(status))
      {
        for (AllowableAction aa : tsaa.getAllowableActions())
        {
          TaskAction taskAction = new TaskAction();
          taskAction.setTaskActionName(aa.getName());
          taskAction.setAlias(aa.getAlias());
          taskAction.setRequireComment(aa.isRequireComment());
          taskAction.setOutcome(aa.getOutcome());

          if (!validTaskActions.contains(taskAction))
          {
            List<ForTask> forTasks = TaskActions.getForTasks(aa.getName());
            if (forTasks == null || forTasks.size() == 0)
            {
              validTaskActions.add(taskAction);
            }
            else
            {
              // forTasks are specified, so only add the action if task name matches
              for (ForTask forTask : forTasks)
              {
                if (forTask.getTaskName().equals(taskName))
                {
                  validTaskActions.add(taskAction);
                }
              }
            }
          }
        }
      }
    }
    return validTaskActions;
  }
}
