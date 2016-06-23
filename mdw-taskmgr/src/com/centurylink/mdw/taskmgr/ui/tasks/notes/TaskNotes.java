/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.notes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a collection of NotesItem objects in a sortable list.
 */
public class TaskNotes extends SortableList
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskNotes(ListUI listUI)
  {
    super(listUI);
  }

  private long mTaskInstanceId;
  public long getTaskInstanceId() { return mTaskInstanceId; }
  public void setTaskInstanceId(long id) { mTaskInstanceId = id; }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {
    TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
    setTaskInstanceId(Long.parseLong(taskDetail.getInstanceId()));

    // retrieve the notes for this task instance from the workflow
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<InstanceNote> notes = taskMgr.getNotes(OwnerType.TASK_INSTANCE, new Long(mTaskInstanceId));
      return new ListDataModel<ListItem>(convertTaskNotes(notes));
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Task Notes for instance " + taskDetail.getInstanceId() + ".";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }

  /**
   * Converts a collection of task instance notes retrieved from the workflow.
   *
   * @param taskInstanceNotes - collection retrieved from the workflow
   * @return list of ui data model items
   */
  protected List<ListItem> convertTaskNotes(Collection<InstanceNote> taskInstanceNotesData)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (InstanceNote taskInstanceNotes : taskInstanceNotesData) {

      TaskNotesItem item = new TaskNotesItem(taskInstanceNotes);
      rowList.add(item);
    }
    return rowList;
  }
}
