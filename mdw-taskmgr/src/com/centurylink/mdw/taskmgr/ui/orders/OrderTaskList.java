/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a list of tasks associated with an order.
 */
public class OrderTaskList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public OrderTaskList(ListUI listUI)
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
      OrderDetail orderDetail = getOrderDetail();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskInstanceVO[] taskInstances
        = taskMgr.getTaskInstanceVOsForMasterOwner(orderDetail.getMasterRequestId());
      return new ListDataModel<ListItem>(convertTaskInstanceVOs(taskInstances));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public OrderDetail getOrderDetail() throws UIException
  {
    return DetailManager.getInstance().getOrderDetail();
  }

  /**
   * Converts a collection of Task Instance VOs retrieved from the workflow.
   *
   * @param taskInstances - collection retrieved from the workflow
   * @return list of ui data model items
   */
  protected List<ListItem> convertTaskInstanceVOs(TaskInstanceVO[] taskInstances )
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (int i = 0; i < taskInstances.length; i++)
    {
      TaskInstanceVO taskInstance = taskInstances[i];
      FullTaskInstance instance = new FullTaskInstance(taskInstance);
      rowList.add(instance);
    }

    return rowList;
  }
}