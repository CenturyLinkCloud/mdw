/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;

import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.data.RowConverter;
import com.centurylink.mdw.taskmgr.ui.events.AuditEventItem;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class AuditEventsList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public AuditEventsList(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {
    try
    {
      PagedListDataModel pagedDataModel = new AuditEventsDataModel(getListUI(), getFilter(), getUserPreferredDbColumns());

      // set the converter for FullTaskInstances
      pagedDataModel.setRowConverter(new RowConverter()
        {
          public Object convertRow(Object o)
          {
            UserActionVO userAction = (UserActionVO) o;
            AuditEventItem item = new AuditEventItem(userAction);
            return item;
          }
        });

      return pagedDataModel;
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Audit Events.";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }
}
