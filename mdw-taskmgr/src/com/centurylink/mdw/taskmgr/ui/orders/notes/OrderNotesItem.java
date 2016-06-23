/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders.notes;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.notes.NotesItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class OrderNotesItem extends NotesItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public OrderNotesItem()
  {
  }

  public OrderNotesItem(InstanceNote note)
  {
    super(note);
  }

  public void add()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.addNote("ORDER", getOwnerId(), getSummary(), getDetail(), user.getCuid());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void delete()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.deleteNote(getId(), user.getUserId());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void save()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.updateNote(getId(), getSummary(), getDetail(), user.getCuid());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }
}
