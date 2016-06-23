/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;

import java.net.URL;

import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.taskmgr.ui.events.AuditEventItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;

public class AuditEventsListActionController implements ListActionController
{

  @Override
  public String performAction(String action, ListItem listItem) throws UIException
  {
    AuditEventItem selected = (AuditEventItem) listItem;
    try
    {
      if (action.equals("entitySelect"))
      {
        String url = ServiceLocator.getTaskManager().getTaskInstanceUrl(selected.getEntityId());
        FacesVariableUtil.navigate(new URL(url));
      }
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }

    return null;
  }

}
