/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.notes;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.notes.NotesActionController;
import com.centurylink.mdw.web.ui.UIException;

public class OrderNotesActionController extends NotesActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public String getManagedBeanName()
  {
    return "orderNotesItem";
  }

  public String getNavigationOutcome()
  {
    return "go_orderNotes";
  }

  public Long getOwnerId()
  {
    long instanceId = 0;
    try
    {
      instanceId = Long.parseLong(DetailManager.getInstance().getOrderDetail().getOrderId());
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return new Long(instanceId);
  }

  public String getActionControllerName()
  {
    return "orderNotesActionController";
  }


}
