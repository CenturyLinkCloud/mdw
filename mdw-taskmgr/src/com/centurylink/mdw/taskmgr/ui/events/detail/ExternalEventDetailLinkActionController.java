/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.detail;

import com.centurylink.mdw.taskmgr.ui.detail.DataItem;
import com.centurylink.mdw.taskmgr.ui.detail.DataItemActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

public class ExternalEventDetailLinkActionController implements DataItemActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  public String performAction(String action, DataItem dataItem) throws UIException
  {
    DetailManager detailManager = DetailManager.getInstance();
    String dataItemValue = dataItem.getDataValue().toString();

    if (action.equals("orderDetail"))
    {
      detailManager.setOrderDetail(dataItemValue);
      return "go_orderDetail";
    }
    else if (action.equals("processInstance"))
    {
      detailManager.setOrderDetail(dataItemValue);
      return "go_orderWorkflow";
    }
    else
    {
      logger.severe("Unknown detail link action: " + action);
      UIError error = new UIError("Unknown detail link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

}