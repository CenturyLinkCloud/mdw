/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events;

import java.util.Date;

import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.web.ui.list.ListItem;

public class AuditEventItem extends ListItem
{
  private UserActionVO userAction;
  public UserActionVO getUserAction() { return userAction; }

  public AuditEventItem(UserActionVO userAction)
  {
    this.userAction = userAction;
  }
  
  public Long getId()
  {
    return userAction.getId();
  }
  
  public String getAction()
  {
    if (userAction.getAction().equals(Action.Other))
      return userAction.getExtendedAction();
    else
      return userAction.getAction().toString();
  }
  
  public String getEntity()
  {
    return userAction.getEntity().toString();
  }
  
  public Long getEntityId()
  {
    return userAction.getEntityId();
  }
  
  public String getUser()
  {
    return userAction.getUser();
  }
  
  public Date getDate()
  {
    return userAction.getDate();
  }
  
  public String getSource()
  {
    return userAction.getSource();
  }
  
  public String getDescription()
  {
    return userAction.getDescription();
  }
  
  

}
