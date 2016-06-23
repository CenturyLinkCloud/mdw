/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListActionController;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskListController extends TaskListActionController {

    @Override
    public String performAction(String action, ListItem listItem) throws UIException {
        FullTaskInstance taskInstance = (FullTaskInstance) listItem;
        DetailManager detailManager = DetailManager.getInstance();
        if (!taskInstance.isSummaryOnly() && taskInstance.isHasCustomPage()) {
            detailManager.setTaskDetail(taskInstance.getInstanceId().toString());
            taskInstance = detailManager.getTaskDetail().getFullTaskInstance();
        }

        if (action.equals("orderDetail")) {
            // update the orderDetail from the list item
            detailManager.setOrderDetail(taskInstance.getOrderId());
            return "go_orderDetail";
        }
        else if (action.equals("processDetail")) {
            // update the processDetail from the list item
            detailManager.setProcessDetail(String.valueOf(taskInstance.getProcessInstanceId()));
            return "go_processDetail";
        }
        else {
            return super.performAction(action, listItem);
        }
    }

    @Override
    protected CustomPageDetailHandler getCustomPageDetailHandler(FullTaskInstance taskInstance) {
        return new CustomPageDetailHandler(taskInstance);
    }

}
