/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;


import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.event.ExternalEventInstanceVO;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.data.RowConverter;
import com.centurylink.mdw.taskmgr.ui.events.ExternalEventItem;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Handles paginated responses for external events.
 */
public class ExternalEventsList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public ExternalEventsList(ListUI listUI)
  {
    super(listUI);
  }

  public DataModel<ListItem> retrieveItems() throws UIException
  {
    try
    {
      PagedListDataModel pagedDataModel = new ExternalEventsDataModel(getListUI(), getFilter());

      // set the converter for ExternalEventItems
      pagedDataModel.setRowConverter(new RowConverter()
        {
          public Object convertRow(Object o)
          {
            ExternalEventInstanceVO externalEventInstance = (ExternalEventInstanceVO) o;
            ExternalEventItem item = new ExternalEventItem(externalEventInstance);
            return item;
          }
        });
      return pagedDataModel;
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving External Events.";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }
}
