/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.categories;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.Decoder;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskCategories extends SortableList implements Lister, Decoder
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  static
  {
    refreshList();
  }

  private static TaskCategory[] mTaskCategories;

  public static TaskCategory[] getTaskCategories()
  {
    return mTaskCategories;
  }

  public TaskCategories(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * Needs a no-arg constructor since categories is a dropdown lister
   * in addition to being a SortableList implementation.
   */
  public TaskCategories() throws UIException
  {
    super(ViewUI.getInstance().getListUI("taskCategoryList"));
  }

  /**
   * Retrieve the categories.
   */
  public static void refreshList()
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      mTaskCategories = taskMgr.getTaskCategories();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  /**
   * Get a list of SelectItems populated from an array of task categories.
   *
   * @param categories the categories VOs to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getTaskCategorySelectItems(TaskCategory[] categories, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (categories.length > 0)
    {
      if (firstItem != null)
      {
        selectItems.add(new SelectItem("0", firstItem));
      }
      for (int i = 0; i < categories.length; i++)
      {
        TaskCategory category = categories[i];
        selectItems.add(new SelectItem(category.getId().toString(), category.getCode() + " - " + category.getDescription()));
      }
    }

    return selectItems;
  }

  public List<SelectItem> getTaskCategorySelectItems()
  {
    return list();
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Lister#list(java.lang.String)
   */
  public List<SelectItem> list()
  {
    return getTaskCategorySelectItems(getTaskCategories(), "");
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    try
    {
      refreshList();
      List<ListItem> l = new ArrayList<ListItem>();
      if (mTaskCategories == null || mTaskCategories.length == 0)
      {

      }
      else
      {
        for (int i = 0; i < mTaskCategories.length; i++)
        {
          TaskCategoryItem item = new TaskCategoryItem(mTaskCategories[i]);
          l.add(item);
        }
      }
      ListDataModel<ListItem> retModel = new ListDataModel<ListItem>(l);
      return retModel;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public String decode(Long id)
  {
    for (int i = 0; i < getTaskCategories().length; i++)
    {
      TaskCategory taskCategory = getTaskCategories()[i];
      if (taskCategory.getId().equals(id))
        return taskCategory.getCode() + " - " + taskCategory.getDescription();
    }
    return null;
  }

}
