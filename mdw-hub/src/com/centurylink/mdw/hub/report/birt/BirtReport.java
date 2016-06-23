/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report.birt;

import org.eclipse.birt.report.engine.api.IReportRunnable;

import com.centurylink.mdw.hub.report.Report;

public class BirtReport extends Report {
    public BirtReport() {
    }

    public BirtReport(String name) {
      super(name);
    }

    private IReportRunnable reportDesign;
    public IReportRunnable getReportDesign() { return reportDesign; }
    public void setReportDesign(IReportRunnable design) { this.reportDesign = design; }
}
