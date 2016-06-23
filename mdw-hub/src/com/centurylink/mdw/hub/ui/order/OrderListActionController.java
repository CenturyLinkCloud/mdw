/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.order;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.process.ProcessInstanceItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Action handler for link click events on the task lists.
 */
public class OrderListActionController implements ListActionController {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String performAction(String action, ListItem listItem) throws UIException {
        ProcessInstanceItem processInstance = (ProcessInstanceItem) listItem;
        DetailManager detailManager = DetailManager.getInstance();

        if (action.equals("blv")) {
            try {
                String url = ApplicationContext.getMdwHubUrl();
                FacesVariableUtil.navigate(new URL(url + "/process/blv.jsf?MasterRequestId="
                        + processInstance.getMasterRequestId()));
            }
            catch (IOException ex) {
                throw new UIException(ex.getMessage(), ex);
            }
            return null;
        }
        else if (action.equals("orderDetail"))
        {
          // update the orderDetail from the list item
          detailManager.setOrderDetail(processInstance.getMasterRequestId());
          return "go_orderDetail";
        }
        else {
            logger.severe("Unknown Order list link action: " + action);
            UIError error = new UIError("Unknown Order list link action: " + action);
            FacesVariableUtil.setValue("error", error);
            return "go_error";
        }
    }
}
