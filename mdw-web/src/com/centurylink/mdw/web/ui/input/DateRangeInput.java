/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.input;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.centurylink.mdw.common.utilities.StringHelper;

public class DateRangeInput extends Input
{
  private Date _fromDate;
  public Date getFromDate() { return _fromDate; }
  public void setFromDate(Date d) { _fromDate = d; }

  private Date _toDate;
  public Date getToDate() { return _toDate; }
  public void setToDate(Date d) { _toDate = d; }

  public DateRangeInput(String attribute, String label, int defaultStartDay, int defaultEndDay)
  {
    super(attribute, label);

    if (defaultStartDay != 0)
    {
      Calendar from = Calendar.getInstance();
      from.add(Calendar.DATE, defaultStartDay);
      _fromDate = from.getTime();
    }
    if (defaultEndDay != 0)
    {
      Calendar to = Calendar.getInstance();
      to.add(Calendar.DATE, defaultEndDay);
      _toDate = to.getTime();
    }
  }

  public DateRangeInput(String attribute, String label, String defaultStartDay, String defaultEndDay)
  {
    super(attribute, label);
    
    try {
		if(!(StringHelper.isEmpty(defaultStartDay))){
			int startDay = Integer.parseInt(defaultStartDay);
		    Calendar from = Calendar.getInstance();
		    from.add(Calendar.DATE, startDay);
		    _fromDate = from.getTime();
		 }

		if(!(StringHelper.isEmpty(defaultEndDay))){
			int endDay = Integer.parseInt(defaultEndDay);
		    Calendar to = Calendar.getInstance();
		    to.add(Calendar.DATE, endDay);
		    _toDate = to.getTime(); 
		}
	} catch (NumberFormatException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("Unexpected Input for Date Range found in TaskView.xml",e);		
	}
  }

  public String getFormattedFromDateString()
  {
    if (_fromDate == null)
      return null;
    
    return formattedDateString(_fromDate);
  }

  public String getFormattedToDateString()
  {
    if (_toDate == null)
      return null;
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(_toDate);
        
    // add 24 hours to move date to midnight so that query returns results matching this day
    cal.add(Calendar.DATE, 1);
    // now subtract one millisecond to avoid including pure-date (non-time-containing)
    // matches which fall exactly on midnight of the day after the end-date
    cal.add(Calendar.MILLISECOND, -1);
    return formattedDateTimeString(cal.getTime());
  }

  private String formattedDateString(Date d)
  {
    if (d == null)
      return "";
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    StringBuffer formatted = new StringBuffer(256);
    formatted.append("to_date('");
    formatted.append(sdf.format(d));
    formatted.append("', 'mm/dd/yyyy')");
    return formatted.toString();
  }

  private String formattedDateTimeString(Date d)
  {
    if (d == null)
      return "";
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    StringBuffer formatted = new StringBuffer(256);
    formatted.append("to_date('");
    formatted.append(sdf.format(d));
    formatted.append("', 'mm/dd/yyyy hh24:mi:ss')");
    return formatted.toString();
  }

  public boolean isValueEmpty()
  {
    return getFromDate() == null && getToDate() == null;
  }

  public String getDefaultSpecFromValues()
  {
    // eg: -1/14
    if (isValueEmpty())
      return "";
    
    long nowMs = new Date().getTime();
    
    String spec = "";
    
    if (_fromDate != null)
    {
      long fromMs = _fromDate.getTime();
      int fromDiff = (int)(Math.floor((fromMs - nowMs)/(1000*3600*24f)) + 1);
      spec += fromDiff;
    }
    
    spec += "/";
    
    if (_toDate != null)
    {
      long toMs = _toDate.getTime();
      int toDiff = (int)(Math.floor((toMs - nowMs)/(1000*3600*24f)) + 1);
      spec += toDiff;
    }
    
    return spec;
  }
}
