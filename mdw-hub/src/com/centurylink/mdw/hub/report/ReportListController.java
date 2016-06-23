/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report;

import com.centurylink.mdw.hub.jsf.FacesUtil;
import com.centurylink.mdw.hub.report.birt.BirtReportFactory;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class ReportListController implements ListActionController {

    private static final String OPEN_REPORT = "openReport";

    public String performAction(String action, ListItem listItem) throws UIException {
        ReportItem reportItem = (ReportItem) listItem;
        if (action.equals(OPEN_REPORT)) {
            Report birtReport = BirtReportFactory.loadReport(reportItem.getName(), null, reportItem.getVersionInt());
            FacesUtil.setValue("mdwReport", birtReport);
            return "go_report";
        }
        else {
            throw new UIException("Unsupported ReportList action: " + action);
        }
    }

}
