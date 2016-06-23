/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import com.centurylink.mdw.taskmgr.ui.detail.DataItem;
import com.centurylink.mdw.taskmgr.ui.detail.DataItemActionController;
import com.centurylink.mdw.taskmgr.ui.process.ProcessDesignerView;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Action handler for link click events on the task detail.
 */
public class DetailLinkActionController implements DataItemActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  public String performAction(String action, DataItem dataItem) throws UIException
  {
    if (action.equals("orderDetail"))
    {
      // default behavior does nothing
      return null;
    }
    if (action.equals("processInstance"))
    {
      ProcessDesignerView designerView = (ProcessDesignerView) FacesVariableUtil.getValue("processDesignerView");
      designerView.launchDesigner();
      return null;
    }
    else
    {
      logger.severe("Unknown task detail link action: " + action);
      UIError error = new UIError("Unknown task detail link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

}
