/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.io.Serializable;

/**
 * Type-safe enum for TaskListScope.
 */
public class TaskListScope implements Serializable
{
  private static TaskListScope userScope;
  public static TaskListScope getUserScope()
  {
    if (userScope == null)
      userScope = new TaskListScope(0, "User", "go_myTasks", "go_myTaskDetail", "tasks/myTasks.jsf", "myTasksTab");
    return userScope;
  }

  private static TaskListScope workgroupScope;
  public static TaskListScope getWorkgroupScope()
  {
    if (workgroupScope == null)
      workgroupScope = new TaskListScope(1, "Workgroup", "go_workgroupTasks", "go_workgroupTaskDetail", "tasks/workgroupTasks.jsf", "workgroupTasksTab");
    return workgroupScope;
  }

  private int id;
  public int getId() { return id; }
  private String name;
  public String getName() { return name; }
  private String listNavigationOutcome;
  public String getListNavigationOutcome() { return listNavigationOutcome; }
  private String detailNavigationOutcome;
  public String getDetailNavigationOutcome() { return detailNavigationOutcome; }
  private String listPath;
  public String getListPath() { return listPath; }
  private String tabbedPage;
  public String getTabbedPage() { return tabbedPage; }

  /**
   * Private constructor.
   */
  private TaskListScope(int id, String name, String listNavigationOutcome, String detailNavigationOutcome, String listPath, String tabbedPage)
  {
    this.id = id;
    this.name = name;
    this.listNavigationOutcome = listNavigationOutcome;
    this.detailNavigationOutcome = detailNavigationOutcome;
    this.listPath = listPath;
    this.tabbedPage = tabbedPage;
  }

  public boolean isUser()
  {
    return this.equals(getUserScope());
  }

  public boolean isWorkgroup()
  {
    return this.equals(getWorkgroupScope());
  }

  public String toString()
  {
    return getName();
  }

  public boolean equals(Object o)
  {
    if (o == this)
    {
      return true;
    }
    if (!(o instanceof TaskListScope))
    {
      return false;
    }
    TaskListScope other = (TaskListScope) o;
    return getId() == other.getId();
  }

}
