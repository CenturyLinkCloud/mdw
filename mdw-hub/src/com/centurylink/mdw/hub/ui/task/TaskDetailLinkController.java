/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.hub.ui.DetailManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;


public class TaskDetailLinkController {

    public Object goOrderDetail() throws UIException {
        DetailManager detailManager = (DetailManager) DetailManager.getInstance();
        TaskDetail taskDetail = (TaskDetail) detailManager.getTaskDetail();
        detailManager.setOrderDetail(taskDetail.getMasterRequestId());
        return "go_orderDetail";
    }

    public Object goProcessDetail() throws UIException {
        DetailManager detailManager = (DetailManager) DetailManager.getInstance();
        TaskDetail taskDetail = (TaskDetail) detailManager.getTaskDetail();
        detailManager.setProcessDetail(String.valueOf(taskDetail.getProcessInstanceId()));
        return "go_processDetail";
    }

    public Object goTaskTemplate() throws UIException, IOException {
        DetailManager detailManager = (DetailManager) DetailManager.getInstance();
        TaskDetail taskDetail = (TaskDetail) detailManager.getTaskDetail();
        String taskLogicalId = taskDetail.getTaskTemplate().getLogicalId();
        String url = ApplicationContext.getTaskManagerUrl() + "/admin/groups.jsf?taskLogicalId=" + taskLogicalId;
        FacesVariableUtil.navigate(new URL(url));
        return null;
    }

}
