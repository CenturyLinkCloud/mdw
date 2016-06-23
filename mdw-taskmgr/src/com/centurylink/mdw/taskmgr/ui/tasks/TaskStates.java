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
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.Decoder;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskStates implements Lister, Decoder
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  static
  {
    refresh();
  }

  private static List<TaskState> mStates;


  /**
   * Get the list of possible Task States.
   * @param role null if all statuses desired
   * @return list of Statuses
   */
  public static List<TaskState> getStates()
  {
    return mStates;
  }

  /**
   * Retrieve the list of TaskStates from the workflow.
   */
  public static void refresh()
  {
    mStates = new ArrayList<TaskState>();
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<?> states = taskMgr.getTaskStates();
      for (Iterator<?> iter = states.iterator(); iter.hasNext(); )
      {
        TaskState state = (TaskState) iter.next();
        mStates.add(state);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  /**
   * Get a list of SelectItems populated from the states.
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public List<SelectItem> getStateSelectItems(String firstItem)
  {
    List<TaskState> states = getStates(); // use the cached list

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
    {
      selectItems.add(new SelectItem("0", firstItem));
    }

    for (int i = 0; i < states.size(); i++)
    {
      TaskState state = (TaskState) states.get(i);
      selectItems.add(new SelectItem(state.getId().toString(), state.getDescription()));
    }
    return selectItems;
  }

  public String decode(Long id)
  {
    for (int i = 0; i < getStates().size(); i++)
    {
      TaskState state = (TaskState) getStates().get(i);
      if (state.getId().equals(id))
      {
        return state.getDescription();
      }
    }

    return null;
  }

  /**
   * Ignores open and closed states.
   */
  public String decodeForAdvisory(Long id)
  {
    // special logic to suppress non-interesting task states
    String description = decode(id);
    if (id.equals(new Long(TaskState.STATE_OPEN.longValue()))
        || id.equals(new Long(TaskState.STATE_CLOSED.longValue())))
    {
      description = "";
    }

    return description;
  }

  public List<SelectItem> list()
  {
    List<SelectItem> list = new ArrayList<SelectItem>();
    list.add(new SelectItem("-1", "[Not Closed]"));
    list.addAll(getStateSelectItems(""));
    return list;
  }

}
