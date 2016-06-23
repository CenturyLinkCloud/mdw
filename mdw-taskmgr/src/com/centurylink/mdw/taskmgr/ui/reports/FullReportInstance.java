/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.reports;

import com.centurylink.mdw.taskmgr.ui.tasks.TaskStates;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * UI model object representing a full task instance, with both the task data
 * and the instance data.  Also can contain the list of columns to be included
 * in the task's list display.
 */
public class FullReportInstance extends ListItem
{
  private String entityName ;
  private Integer itemState ;
  private Integer itemCount ;

  public FullReportInstance( String entityName )
  {
    this.entityName = entityName;
  }

  public String getEntityName()
  {
    return entityName;
  }

  public void setEntityName(String entityName)
  {
    this.entityName = entityName;
  }

  public Integer getItemCount()
  {
    return itemCount;
  }

  public void setItemCount(Integer itemCount)
  {
    this.itemCount = itemCount;
  }

  public String getItemState()
  {
    return new TaskStates().decode(new Long( itemState.longValue() ));
  }

  public void setItemState(Integer itemState)
  {
    this.itemState = itemState;
  }
}