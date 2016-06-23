/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * UI list for displaying the audit log history of a task instance.
 */
public class TaskHistory extends SortableList
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskHistory(ListUI listUI)
  {
    super(listUI);
  }

  private long taskInstanceId;
  public long getTaskInstanceId() { return taskInstanceId; }
  public void setTaskInstanceId(long id) { taskInstanceId = id; }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {
    TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
    setTaskInstanceId(Long.parseLong(taskDetail.getInstanceId()));

    // retrieve the history for this task instance
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<?> eventLogs = taskMgr.getEventLogs(new Long(taskInstanceId));
      return new ListDataModel<ListItem>(convertEventLogs(eventLogs));
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Task Notes for instance " + taskDetail.getInstanceId() + ".";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }

  /**
   * Converts a collection of task instance eventLogs into TaskHistoryItem objects.
   *
   * @param taskInstanceEventLogs - collection retrieved from the workflow
   * @return list of ui data model items
   */
  protected List<ListItem> convertEventLogs(Collection<?> taskInstanceEventLogs)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (Iterator<?> i = taskInstanceEventLogs.iterator(); i.hasNext(); )
    {
      EventLog eventLog = (EventLog) i.next();
      TaskHistoryItem item = new TaskHistoryItem(eventLog);
      rowList.add(item);
    }
    return rowList;
  }
}
