/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;


import com.centurylink.mdw.hub.report.Reports;
import com.centurylink.mdw.hub.ui.order.Orders;
import com.centurylink.mdw.web.ui.UIException;


/**
 * ListManager class for MDW Hub.
 */
public class ListManager extends com.centurylink.mdw.taskmgr.ui.list.ListManager {

    public Orders getOrderList() throws UIException {
        return (Orders) getList("orderList");
    }

    public Reports getReportsList() throws UIException {
        return (Reports) getList("reportsList");
    }
}
