/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.filter;

import java.util.Map;

import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.taskmgr.ui.filter.Filter;
import com.centurylink.mdw.taskmgr.ui.layout.FilterUI;

/**
 * Specialized filter for handling Tasks.
 */
public class TaskFilter extends Filter
{
  public TaskFilter()
  {
    super();
  }
  public TaskFilter(FilterUI filterUI)
  {
    super(filterUI);
  }

  private String userName;
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.filter.Filter#buildCriteriaMap()
   */
  public Map<String,String> buildCriteriaMap()
  {
    Map<String,String> map = super.buildCriteriaMap();

    if (map.containsKey("taskClaimUserId") && map.get("taskClaimUserId").toString().equals(" = '-1'"))
    {
      // assignee criteria value is [Unassigned]
      map.put("taskClaimUserId", " IS NULL ");
    }
    if (map.containsKey("stateCode") && map.get("stateCode").toString().equals(" = '-1'"))
    {
      // advisory is [Not Invalid]
      map.put("stateCode", " != " + TaskState.STATE_INVALID);
    }
    if (map.containsKey("statusCode") && map.get("statusCode").toString().equals(" = '-1'"))
    {
      // status is [Active]
      map.put("statusCode", " != " + TaskStatus.STATUS_COMPLETED + " and task_instance_status != " + TaskStatus.STATUS_CANCELLED);
    }

    return map;
  }

}
