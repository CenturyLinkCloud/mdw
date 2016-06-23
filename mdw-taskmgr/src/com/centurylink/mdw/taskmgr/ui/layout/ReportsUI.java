/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.web.ui.UIException;

public class ReportsUI extends UI
{
  public ReportsUI() {}

  private List<ReportUI> reports = new ArrayList<ReportUI>();

  public List<ReportUI> getReports()
  {
    return reports;
  }

  public void setReports(List<ReportUI> reports)
  {
    this.reports = reports;
  }
  public void addReportUI(ReportUI reportui)
  {
    reports.add(reportui);
  }


  public void addReportsUI(String id)
  throws UIException
  {
    setId(id);

    ViewUI.getInstance().addReportsUI(id, this);
  }
}
