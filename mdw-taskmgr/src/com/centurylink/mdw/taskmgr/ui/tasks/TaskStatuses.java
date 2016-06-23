/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.Decoder;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Caches the refrence data for task statuses.
 *
 */
public class TaskStatuses implements Lister, Decoder
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  static
  {
    refresh();
  }

  private static List<TaskStatus> mStatuses;


  /**
   * Get the list of Statuses based on a user role.
   * @return list of Statuses
   */
  public static List<TaskStatus> getStatuses()
  {
    return mStatuses;
  }

  /**
   * Retrieve the list of TaskStatuses from the workflow.
   */
  public static void refresh()
  {
    mStatuses = new ArrayList<TaskStatus>();
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<?> statuses = taskMgr.getTaskStatuses();
      for (Iterator<?> iter = statuses.iterator(); iter.hasNext(); )
      {
        TaskStatus status = (TaskStatus) iter.next();
        mStatuses.add(status);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  /**
   * Get a list of SelectItems populated from the statuses.
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public List<SelectItem> getStatusSelectItems(String firstItem)
  {
    List<TaskStatus> statuses = getStatuses(); // use the cached list

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
    {
      selectItems.add(new SelectItem("0", firstItem));
    }
    for (int i = 0; i < statuses.size(); i++)
    {
      TaskStatus status = (TaskStatus) statuses.get(i);
      // TODO: check user role if supplied
      selectItems.add(new SelectItem(status.getId().toString(), status.getDescription()));
    }
    return selectItems;
  }

  public String decode(Long id)
  {
    for (int i = 0; i < getStatuses().size(); i++)
    {
      TaskStatus status = (TaskStatus) getStatuses().get(i);
      if (status.getId().equals(id))
        return status.getDescription();
    }
    return null;
  }

  public List<SelectItem> list()
  {
    List<SelectItem> list = new ArrayList<SelectItem>();
    list.add(new SelectItem("-1", "[All Active]"));
    list.addAll(getStatusSelectItems(""));
    return list;
  }

}
