/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.filter;

import java.util.Date;

import com.centurylink.mdw.taskmgr.ui.filter.Filter;
import com.centurylink.mdw.taskmgr.ui.layout.FilterUI;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.input.SelectInput;

public class EventsFilter extends Filter
{
  public EventsFilter(FilterUI filterUI)
  {
    super(filterUI);
  }
  
  public String getEventName()
  {
    SelectInput eventNameCrit = (SelectInput) getInput("eventName");
    if (eventNameCrit == null || eventNameCrit.getValue() == null)
      return null;
    
    return eventNameCrit.getValue().toString();
  }

  public String getEventSource()
  {
    SelectInput eventSourceCrit = (SelectInput) getInput("eventSource");
    if (eventSourceCrit == null || eventSourceCrit.getValue() == null)
      return null;
    
    return eventSourceCrit.getValue().toString();
  }
  
  public Date getStartDate()
  {
    DateRangeInput drCrit = (DateRangeInput) getInput("eventDate");
    if (drCrit == null)
      return null;
    String formattedStartDate = drCrit.getFormattedFromDateString();
    if (formattedStartDate.length() == 0)
      return new Date(0);
    else
      return drCrit.getFromDate();
  }
  
  public Date getEndDate()
  {
    DateRangeInput drCrit = (DateRangeInput) getInput("eventDate");
    if (drCrit == null)
      return null;
    String formattedEndDate = drCrit.getFormattedToDateString();
    if (formattedEndDate.length() == 0)
      return new Date();
    else
      return drCrit.getToDate();
  }
  
  public String toString()
  {
    return "startDate: " + getStartDate() + "\n"
      + "endDate: " + getEndDate() + "\n"
      + "eventName: " + getEventName() + "\n"
      + "eventSource: " + getEventSource() + "\n";
  }
  
}
