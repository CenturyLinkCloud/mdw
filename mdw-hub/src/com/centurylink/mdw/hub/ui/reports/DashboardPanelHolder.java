/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.reports;

import java.io.Serializable;

import com.centurylink.mdw.hub.report.ReportBean;
/**
 * Only store light stuff it here
 *
 * @author aa70413
 *
 */
public class DashboardPanelHolder implements Serializable {
    private String panelId;
    private boolean visible;
    private ReportBean reportItem;

    public DashboardPanelHolder() {

    }
    public DashboardPanelHolder(String panelId, boolean visible, ReportBean reportItem) {
        this.setPanelId(panelId);
        this.setVisible(visible);
        this.setReportItem(reportItem);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public String getPanelId() {
        return panelId;
    }
    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }
    public ReportBean getReportItem() {
        return reportItem;
    }
    public void setReportItem(ReportBean reportItem) {
        this.reportItem = reportItem;
    }
}
