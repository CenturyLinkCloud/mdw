/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class SubTaskList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public SubTaskList(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    try
    {
      TaskDetail taskDetail = getTaskDetail();
      FullTaskInstance fullTaskInstance = taskDetail.getFullTaskInstance();
      List<ListItem> items = new ArrayList<ListItem>();
      List<FullTaskInstance> subTaskInsts = new ArrayList<FullTaskInstance>();
      for (TaskInstanceVO subTaskInst : RemoteLocator.getTaskManager().getSubTaskInstances(fullTaskInstance.getId()))
          subTaskInsts.add(new FullTaskInstance(subTaskInst));
      fullTaskInstance.setSubTaskInstances(subTaskInsts);
      items.addAll(fullTaskInstance.getSubTaskInstances());
      return new ListDataModel<ListItem>(items);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public TaskDetail getTaskDetail() throws UIException
  {
    return DetailManager.getInstance().getTaskDetail();
  }
}