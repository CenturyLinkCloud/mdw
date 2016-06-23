/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.history;

import java.util.Date;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskHistoryItem extends ListItem
{
  private EventLog eventLog;
  public EventLog getEventLog() { return eventLog; }
  
  public TaskHistoryItem(EventLog eventLog)
  {
    this.eventLog = eventLog;
  }

  public Date getCreateDate()
  {
      String ds = eventLog.getCreateDate();
      if (ds==null) return null;
      return StringHelper.stringToDate(ds);
  }
  
  public String getEventName()
  {
    return eventLog.getEventName();
  }
  
  /**
   * Actually user, but misnamed here for backward compatibility in TaskView.xml.
   */
  public String getEventSource()
  {
    return eventLog.getCreateUser();
  }
  
  public String getComments()
  {
    return eventLog.getComment();
  }
  
  public String getCategory()
  {
    return eventLog.getCategory();
  }
  
  public String getSubCategory()
  {
    return eventLog.getSubCategory();
  }
  
  public Long getEventLogId()
  {
    return eventLog.getId();
  }

}