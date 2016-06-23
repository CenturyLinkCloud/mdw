/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.reports;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.taskmgr.ui.user.UserReport;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class ReportMetrics {
	
	List<Report> reportList = new ArrayList<Report>();
	String typeOfReport;
	String reportTitle;
	Date startDate;
	Date endDate;
	
	public List<Report> getReportList() {
		return reportList;
	}

	public void setReportList(List<Report> reportList) {
		this.reportList = reportList;
	}

	public ReportMetrics()  {
		Set<String> metricsRules = RuleSetCache.getRuleNames("(Metrics).*");
		for(String metricsRule : metricsRules){
			reportList.add(new Report(metricsRule));
		}
	}
	
	public String getReportTitle() {
		return reportTitle;
	}

	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}

	public String getTypeOfReport() {
		return typeOfReport;
	}

	public void setTypeOfReport(String typeOfReport) {
		this.typeOfReport = typeOfReport;
		generateReport();
	}
	
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@SuppressWarnings("deprecation")
    public void generateReport() {
		// TODO Auto-generated method stub
		UserReport userReport = (UserReport) FacesVariableUtil.getValue("userReport");
	    userReport.setName(typeOfReport);
	    Map<String,String> params = new HashMap<String,String>();
	    String startDate;
	    String endDate;
	    String startDateDisplay="";
	    String endDateDisplay="";
	    SimpleDateFormat sfDate = new SimpleDateFormat("yyyy-MM-dd");
	    SimpleDateFormat displayformat = new SimpleDateFormat("MMM d, yyyy");
	    if(this.startDate !=null && this.endDate!=null){
	    	startDate = sfDate.format(this.startDate);
	    	endDate = sfDate.format(this.endDate);
	    	startDateDisplay = displayformat.format(this.startDate);
		    endDateDisplay = displayformat.format(this.endDate);
	    }else{
	    	startDate = "2009-01-01";
	    	endDate = sfDate.format(new Date());
	    	Date currentDate = new Date();
	    	endDateDisplay = displayformat.format(currentDate);
	    	currentDate.setYear(currentDate.getYear() - 3);
	    	startDateDisplay = displayformat.format(currentDate);
	    }
	    setReportTitle(typeOfReport.substring(7).replace('_',' ')+"(" + startDateDisplay + " To " + endDateDisplay + ")");
	    params.put("startDate", startDate);
	    params.put("endDate", endDate);
	    userReport.setParams(params);
	}
	
	
	

	
	
}
