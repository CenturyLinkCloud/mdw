/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.process;

import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.process.ProcessInstanceActionController;
import com.centurylink.mdw.taskmgr.ui.process.ProcessInstanceItem;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Process instance list controller.
 */
public class ProcessListController extends ProcessInstanceActionController {
    public String performAction(String action, ListItem listItem) throws UIException {
        ProcessInstanceItem processListItem = (ProcessInstanceItem) listItem;
        DetailManager detailManager = DetailManager.getInstance();

        if (action.equals("processDetail")) {
            // update the processDetail from the list item
            detailManager.setProcessDetail(String.valueOf(processListItem.getInstanceId()));
            return "go_processDetail";
        }
        else {
            return super.performAction(action, listItem);
        }
    }
}
