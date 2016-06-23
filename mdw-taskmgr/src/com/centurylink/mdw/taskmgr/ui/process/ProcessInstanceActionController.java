/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

public class ProcessInstanceActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  public String performAction(String action, ListItem listItem) throws UIException
  {
    ProcessInstanceItem processListItem = (ProcessInstanceItem) listItem;
    DetailManager detailManager = DetailManager.getInstance();

    if (action.equals("processInstance"))
    {
      // open designer
      ProcessDesignerView designerView = (ProcessDesignerView) FacesVariableUtil
          .getValue("processDesignerView");
      return designerView.launchDesigner(processListItem.getInstanceInfo().getId());
    }
    else if (action.equals("orderDetail"))
    {
      // update the orderDetail from the list item
      detailManager.setOrderDetail(processListItem.getMasterRequestId());
      return "go_orderDetail";
    }
    else
    {
      logger.severe("Unknown process list link action: " + action);
      UIError error = new UIError("Unknown process list link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }
}
