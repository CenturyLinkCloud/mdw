/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.events.ExternalEventItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

public class ExternalEventsListActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  public String performAction(String action, ListItem listItem) throws UIException
  {
    ExternalEventItem eventItem = (ExternalEventItem) listItem;
    DetailManager detailManager = DetailManager.getInstance();

    if (action.equals("eventDetail"))
    {
      // update the eventDetail from the list item
      detailManager.setExternalEventDetail(OwnerType.DOCUMENT, eventItem.getInstanceId().toString());
      return "go_eventDetail";
    }
    else if (action.equals("orderDetail"))
    {
      detailManager.setOrderDetail(eventItem.getMasterRequestId());
      return "go_orderDetail";
    }
    else if (action.equals("processInstance"))
    {
      detailManager.setOrderDetail(eventItem.getMasterRequestId());
      return "go_orderWorkflow";
    }
    else
    {
      logger.severe("Unknown event list link action: " + action);
      UIError error = new UIError("Unknown event list link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

}
