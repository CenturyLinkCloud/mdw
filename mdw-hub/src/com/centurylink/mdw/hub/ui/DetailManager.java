/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.hub.ui.order.OrderDetail;
import com.centurylink.mdw.hub.ui.task.CustomPageDetailHandler;
import com.centurylink.mdw.hub.ui.task.TaskDetail;
import com.centurylink.mdw.taskmgr.ui.detail.Detail;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class DetailManager extends com.centurylink.mdw.taskmgr.ui.detail.DetailManager {

    public DetailManager() {
        super();
    }

    @Override
    public Detail createDetail(String detailId) throws UIException {
        if (detailId.equals("taskDetail"))
            return new TaskDetail();
        else if (detailId.equals("orderDetail"))
            return new OrderDetail();
        else
            return super.createDetail(detailId);
    }

    @Override
    public OrderDetail getOrderDetail() throws UIException {
        // handle case where orderId is request param
        String orderId = (String) FacesVariableUtil.getRequestParamValue("orderId");
        OrderDetail orderDetail = (OrderDetail) getDetail("orderDetail");
        if (!StringHelper.isEmpty(orderId) && !orderId.equals(orderDetail.getOrderId())) {
            setOrderDetail(orderId);
        }

        return (OrderDetail) getDetail("orderDetail");
    }

    @Override
    protected CustomPageDetailHandler getCustomPageDetailHandler(FullTaskInstance taskInstance) {
        return new CustomPageDetailHandler(taskInstance);
    }
}
