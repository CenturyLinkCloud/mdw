/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.reports;

public class Report {
	
	String reportName;
	String linkDisplayValue;
	
	public Report(String reportName) {
		super();
		this.reportName = reportName;
		this.linkDisplayValue = reportName.substring(7).replace('_', ' ');
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getLinkDisplayValue() {
		return linkDisplayValue;
	}

	public void setLinkDisplayValue(String linkDisplayValue) {
		this.linkDisplayValue = linkDisplayValue;
	}
	
	
}
