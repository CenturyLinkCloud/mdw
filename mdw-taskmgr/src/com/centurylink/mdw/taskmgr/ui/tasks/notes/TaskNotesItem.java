/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.notes;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.notes.NotesItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskNotesItem extends NotesItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskNotesItem()
  {
  }

  public TaskNotesItem(InstanceNote note)
  {
    super(note);
  }

  public void add()
  {
    try
    {
      AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.addNote(OwnerType.TASK_INSTANCE, getOwnerId(), getSummary(), getDetail(), user.getCuid());
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
