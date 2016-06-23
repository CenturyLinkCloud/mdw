/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events;

import java.util.Date;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Wraps an EventLog model object and provides list behavior.
 */
public class EventItem extends ListItem
{
  public EventLog mEventLog;
  public EventLog getEventLog() { return mEventLog; }
  
  public EventItem(EventLog eventLog)
  {
    mEventLog = eventLog;
  }
  
  public String getName()
  {
    return mEventLog.getEventName();
  }
  
  public String getSource()
  {
    return mEventLog.getSource();
  }
  
  public Date getCreateDate()
  {
    String ds = mEventLog.getCreateDate();
    if (ds==null) return null;
    return StringHelper.stringToDate(ds);
  }
  
  public String getComment()
  {
    return mEventLog.getComment();
  }
  
}
