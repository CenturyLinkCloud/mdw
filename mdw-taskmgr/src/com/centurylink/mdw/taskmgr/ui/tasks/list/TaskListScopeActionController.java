/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.NavScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Keeps track of the current navigation scope, so that when a user clicks
 * on a detail nav link to return to the task list they get routed to the
 * correct list (user-level versus workgroup-level).  Also governs list
 * scope navigation for task action navigation outcomes.
 */
public class TaskListScopeActionController extends NavScopeActionController
{
  private TaskListScope scope = TaskListScope.getUserScope(); // represents the current scope
  public TaskListScope getTaskListScope() { return scope; }
  public void setTaskListScope(TaskListScope tls) { scope = tls; }

  private String remoteTaskRoot = "/facelets/";
  public String getRemoteTaskRoot() { return remoteTaskRoot; }
  public void setRemoteTaskRoot(String root) { this.remoteTaskRoot = root; }

  /**
   * Action handler which forwards to the in-scope task list.
   */
  public String goTaskList() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary(remoteTaskRoot + scope.getListPath());
      return null;
    }
    else if (isMdwHubRequest() || isCompatibilityMode())
    {
      return scope.getListNavigationOutcome();
    }
    else
    {
      redirectToMdwHub("/" + scope.getListPath());
      return null;
    }
  }

  public String goMyTasks() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary(remoteTaskRoot + TaskListScope.getUserScope().getListPath(), TaskListScope.getUserScope().getTabbedPage());
      return null;
    }
    else if (isMdwHubRequest() || isCompatibilityMode())
    {
      return TaskListScope.getUserScope().getListNavigationOutcome();
    }
    else
    {
      redirectToMdwHub("/" + TaskListScope.getUserScope().getListPath());
      return null;
    }
  }

  /**
   * for value expressions
   */
  public String getGoMyTasks()
  {
    return TaskListScope.getUserScope().getListNavigationOutcome();
  }

  public String goWorkgroupTasks() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary(remoteTaskRoot + TaskListScope.getWorkgroupScope().getListPath(), TaskListScope.getWorkgroupScope().getTabbedPage());
      return null;
    }
    else if (isMdwHubRequest() || isCompatibilityMode())
    {
      return TaskListScope.getWorkgroupScope().getListNavigationOutcome();
    }
    else
    {
      redirectToMdwHub("/" + TaskListScope.getWorkgroupScope().getListPath());
      return null;
    }
  }

  /**
   * for value expressions
   */
  public String getGoWorkgroupTasks()
  {
    return TaskListScope.getWorkgroupScope().getListNavigationOutcome();
  }

  /**
   * Determine appropriate navigation outcome.
   */
  public static String getTaskListNavigation()
  {
    return getInstance().getTaskListScope().getListNavigationOutcome();
  }

  public static TaskListScopeActionController getInstance()
  {
    TaskListScopeActionController instance = (TaskListScopeActionController)
        FacesVariableUtil.getValue("taskListScopeActionController");

    if (instance == null)
    {
      instance = new TaskListScopeActionController();
      FacesVariableUtil.setValue("taskListScopeActionController", instance);
    }

    return instance;
  }

}
