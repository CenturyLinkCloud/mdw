/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Caches the task reference data, as well as implementing the SortableList
 * API to populate the task list for maintaining ref data.  This class and its
 * collections are cached at the session level via the JSF managed bean.
 */
public class Tasks extends SortableList implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private TaskVO[] tasksForUserWorkgroups;

  public Tasks(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * Needs a no-arg constructor since Tasks is a dropdown lister
   * in addition to being a SortableList implementation.
   */
  public Tasks() throws UIException
  {
    super(ViewUI.getInstance().getListUI("taskTemplateList"));
  }

  /**
   * Returns all the tasks associated with a workgroup.
   *
   * @param groupName the name of the workgroup
   * @return array of model Task objects
   */
  public static TaskVO[] getTasksForWorkgroup(String groupName) throws UIException
  {
    try
    {
      TaskManager taskManager = RemoteLocator.getTaskManager();
      return taskManager.getTasksForWorkgroup(groupName).toArray(new TaskVO[0]);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * Returns the tasks associated with the current user's workgroups.
   *
   * @return array of Tasks
   */
  public TaskVO[] getTasksForUserWorkgroups() throws UIException
  {
    if (tasksForUserWorkgroups == null)
    {
      loadTasksForUserWorkgroups();
    }

    return tasksForUserWorkgroups;
  }

  public void loadTasksForUserWorkgroups() throws UIException
  {
    List<TaskVO> uniqueTasks = new ArrayList<TaskVO>();
    if (FacesVariableUtil.getCurrentUser().isUserBelongsToAdminGrp())
    {
      TaskVO[] tasksForSiteAdmnGrp = getTasksForWorkgroup(UserGroupVO.SITE_ADMIN_GROUP);
      uniqueTasks = Arrays.asList(tasksForSiteAdmnGrp);
    }
    else
    {
      String[] userWorkgroups = FacesVariableUtil.getCurrentUser().getWorkgroupNames();

      for (int i = 0; i < userWorkgroups.length; i++)
      {
        TaskVO[] tasksForWorkgroup = getTasksForWorkgroup(userWorkgroups[i]);
        for (int j = 0; j < tasksForWorkgroup.length; j++)
        {
          TaskVO task = tasksForWorkgroup[j];
          boolean taskAlreadyInList = false;
          for (int k = 0; k < uniqueTasks.size(); k++)
          {
            if (task.getTaskId().equals(uniqueTasks.get(k).getTaskId()))
            {
              taskAlreadyInList = true;
              break;
            }
          }
          if (!taskAlreadyInList)
            uniqueTasks.add(task);
        }
      }
    }

    Collections.sort(uniqueTasks, new Comparator<TaskVO>()
        {
          public int compare(TaskVO task1, TaskVO task2)
          {
            return task1.getTaskName().compareTo(task2.getTaskName());
          }
        });

    tasksForUserWorkgroups = uniqueTasks.toArray(new TaskVO[uniqueTasks.size()]);
  }

  /**
   * Get a list of SelectItems populated from an array of task names.
   *
   * @param tasks the tasks to include in the list
   * @return list of SelectItems
   */
  public List<SelectItem> getTaskSelectItems(TaskVO[] tasks)
  {
    return getTaskSelectItems(tasks, "");
  }

  /**
   * Get a list of SelectItems populated from an array of task names.
   *
   * @param tasks the task VOs to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getTaskSelectItems(TaskVO[] tasks, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
      selectItems.add(new SelectItem("0", firstItem));
    for (int i = 0; i < tasks.length; i++)
    {
      TaskVO task = tasks[i];
      selectItems.add(new SelectItem(task.getTaskId().toString(), task.getTaskName()));
    }
    sortSelectItems(selectItems);

    return selectItems;
  }

  /**
   * Get a list of SelectItems populated from an array of task names.
   *
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems with a default message
   */
  public static List<SelectItem> getTaskSelectItems(String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();

    Collection<TaskVO> taskVOs = getTasks();
    if (taskVOs.size() > 0)
    {
      if (firstItem != null)
      {
        selectItems.add(new SelectItem("0", firstItem));
      }
      for (TaskVO taskVO : taskVOs)
      {
        selectItems.add(new SelectItem(taskVO.getTaskId().toString(), taskVO.getTaskName()));
      }
    }

    return selectItems;
  }

  /**
   * Get a list of SelectItems based on the task category id.
   *
   * @param categoryId task category id
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems for the category
   */
  public static List<SelectItem> getTaskSelectItemsForCategory(Long categoryId, String firstItem)
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskVO[] tasks = taskMgr.getTasks(categoryId);

      List<SelectItem> selectItems = new ArrayList<SelectItem>();

      if (tasks.length > 0)
      {
        if (firstItem != null)
        {
          selectItems.add(new SelectItem("0", firstItem));
        }
        for (int i = 0; i < tasks.length; i++)
        {
          TaskVO taskForCategory = tasks[i];
          selectItems.add(new SelectItem(taskForCategory.getTaskId().toString(), taskForCategory.getTaskName()));
        }
      }

      return selectItems;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Gets the Due date for a task from its Service Level Agreement.
   *
   * @param taskId
   * @return dueDate defined for this task.
   */

  public static Date getDueDateForTask(long taskId)
  {
    int sla_seconds = 0;
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskVO task = taskMgr.getTaskVO(taskId);
      sla_seconds = task.getSlaSeconds();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }

    Date dueDate;
    if (sla_seconds>0)
    {
      Calendar dt = Calendar.getInstance();
      dt.add(Calendar.SECOND, sla_seconds);
      dueDate = dt.getTime();
    }
    else
    {
      Calendar dt = Calendar.getInstance();
      dt.add(Calendar.DATE, 7);
      dueDate = dt.getTime();
    }
    return dueDate;
  }

  /**
   * Find a task in the list based on its ID.
   *
   * @param taskId
   * @return the task, or null if not found
   */
  public static TaskVO getTask(Long taskId)
  {
    return TaskTemplateCache.getTaskTemplate(taskId);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Decoder#decode(java.lang.Long)
   */
  public String decode(Long id)
  {
    return TaskTemplateCache.getTaskTemplate(id).getTaskName();
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Lister#list(java.lang.String)
   */
  public List<SelectItem> list()
  {
    try
    {
      return getTaskSelectItems(getTasksForUserWorkgroups());
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return new ArrayList<SelectItem>();
    }
  }

  public boolean isShowAll()
  {
    boolean sessVal = Boolean.parseBoolean((String)FacesVariableUtil.getValue("taskList_showAll"));
    if (sessVal)
      return true;  // remembered for session

    boolean showAll = super.isShowAll();
    if (showAll)
      FacesVariableUtil.setValue("taskList_showAll", "true");

    return showAll;
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    List<ListItem> taskItems = new ArrayList<ListItem>();
    Map<String,TaskVO> latestTaskVOs = new HashMap<String,TaskVO>();
    for (TaskVO taskVO : getTasks())
    {
      if (taskVO.isTemplate())
      {
        TaskVO latest = latestTaskVOs.get(taskVO.getTaskName());
        if (latest == null || latest.getTaskId() < taskVO.getTaskId())
          latestTaskVOs.put(taskVO.getTaskName(), taskVO);
      }
    }
    for (String taskName : latestTaskVOs.keySet())
      taskItems.add(new TaskItem(latestTaskVOs.get(taskName)));

    return new ListDataModel<ListItem>(taskItems);
  }

  public static Collection<TaskVO> getTasks()
  {
    return TaskTemplateCache.getTaskTemplates();
  }

  /**
   * Get a list of SelectItems populated from List of user group names.
   *
   * @param groups - the UserGroup names to include
   * @return list of SelectItems
   */
  public static List<SelectItem> getUserGroupSelectItems(List<String> groups)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    for (String group : groups) {
        if (!group.equals(UserGroupVO.COMMON_GROUP))
            selectItems.add(new SelectItem(group, group));
    }
    return selectItems;
  }

  // the following methods are implemented for RichFaces table functionality

  @Override
  public void saveCurrentItem() throws UIException
  {
    TaskItem taskItem = (TaskItem) getCurrentItem();
    taskItem.save();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void deleteCurrentItem() throws UIException
  {
    TaskItem taskItem = (TaskItem) getCurrentItem();
    taskItem.delete();
    ((List<ListItem>)this.getItems().getWrappedData()).remove(taskItem);
  }

  @Override
  public boolean isSortable()
  {
    return getTasks() != null && !getTasks().isEmpty();
  }
}
