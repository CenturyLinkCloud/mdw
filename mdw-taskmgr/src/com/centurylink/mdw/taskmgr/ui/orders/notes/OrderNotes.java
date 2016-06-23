/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.notes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class OrderNotes extends SortableList
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public OrderNotes(ListUI listUI)
  {
    super(listUI);
  }

  private long orderId;
  public long getOrderId() { return orderId; }
  public void setOrderId(long id) { orderId = id; }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {

    OrderDetail orderDetail = DetailManager.getInstance().getOrderDetail();
    try
    {
      setOrderId(Long.parseLong(orderDetail.getOrderId()));
    }
    catch (NumberFormatException ex)
    {
      // can't have notes associated with a non-numeric owner_id
      setOrderId(0L);
    }

    // retrieve the notes for this task instance from the workflow
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<InstanceNote> notes = taskMgr.getNotes("ORDER", new Long(orderId));
      return new ListDataModel<ListItem>(convertNotes(notes));
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Notes for instance " + orderId + ".";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }

  protected List<ListItem> convertNotes(Collection<InstanceNote> notesData)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (InstanceNote notes : notesData) {

      OrderNotesItem item = new OrderNotesItem(notes);
      rowList.add(item);
    }
    return rowList;
  }
}
