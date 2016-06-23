/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.web.util.RemoteLocator;


public class TaskCategories implements Lister {

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  static
  {
    refresh();
  }

  private static TaskCategory[] mTaskCategories;
  public static TaskCategory[] getTaskCategories() { return mTaskCategories; }

  /**
   * Retrieve the categories.
  */
  public static void refresh()
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
        selectItems.add(new SelectItem(category.getId().toString(), category.getCode() + " - "
            + category.getDescription()));
      }
    }

    return selectItems;
  }

  public List<SelectItem> getTaskCategorySelectItems()
  {
    return list();
  }

  public List<SelectItem> list()
  {
    return getTaskCategorySelectItems(getTaskCategories(), "");
  }

}
