/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.notes;

import java.util.Date;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;

public abstract class NotesActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public static final String ACTION_VIEW = "viewNotes";
  public static final String ACTION_EDIT = "editNote";
  public static final String ACTION_DELETE = "deleteNote";
  public static final String ACTION_ADD = "addNote";
  public static final String ACTION_SAVE = "saveNote";
  public static final String ACTION_CONFIRM_DELETE = "confirmDeleteNote";
  public static final String ACTION_CANCEL = "cancel";

  private NotesItem mNotesItem;

  /**
   * @return name of the notes item instance
   */
  public abstract String getManagedBeanName();
  /**
   * @return the jsf navigation outcome
   */
  public abstract String getNavigationOutcome();
  /**
   * @return managed bean name of this action controller
   */
  public abstract String getActionControllerName();
  /**
   * @return the unique id of the owner
   */
  public abstract Long getOwnerId();
  
  private String _action = ACTION_VIEW;
  public String getAction() { return _action; }
  public void setAction(String s)
  {
    _action = s;
    if (_action == ACTION_VIEW)
    {
      mNotesItem = (NotesItem) FacesVariableUtil.getValue(getManagedBeanName());
      try
      {
        mNotesItem = mNotesItem.getClass().newInstance();
        FacesVariableUtil.setValue(getManagedBeanName(), mNotesItem);
        FacesVariableUtil.removeValue("dataTableCurrentRow_notesList");        
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
    }
  }
  public String getDefaultAction() { return ACTION_VIEW; }

  public String addNote()
  {
    mNotesItem.setOwnerId(getOwnerId());
    setAction(ACTION_ADD);
    return performAction();
  }
  
  public String saveNote()
  {
    setAction(ACTION_SAVE);
    return performAction();
  }
  
  public String cancel()
  {
    setAction(ACTION_CANCEL);
    return performAction();
  }
  
  public String confirmDeleteNote()
  {
    setAction(ACTION_CONFIRM_DELETE);
    return performAction();
  }
  
  /**
   * Called from command links in the notes list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem)
  {
    setAction(action);
    FacesVariableUtil.setValue(getManagedBeanName(), listItem);
    FacesVariableUtil.setValue(getActionControllerName(), this);
    return performAction();
  }

  /**
   * Called from the command buttons for editing a note.
   *
   * @return the nav destination
   */
  public String performAction()
  {
    mNotesItem = (NotesItem) FacesVariableUtil.getValue(getManagedBeanName());

    return performActionOnInstanceNote();
  }

  private String performActionOnInstanceNote()
  {
    if (mNotesItem.getSummary() == null || mNotesItem.getSummary().length() == 0)
      return getNavigationOutcome();

    String user = FacesVariableUtil.getCurrentUser().getCuid();
    mNotesItem.setModifiedBy(user);

    Date modDate = new Date();
    mNotesItem.setModifiedDate(modDate);

    if (logger.isDebugEnabled())
    {
      logger.debug("Notes Action: " + getAction());
      logger.debug("\nNote:\n"
        + "   ownerId: " + mNotesItem.getOwnerId() + "   "
        + "summary = " + mNotesItem.getSummary() + "   "
        + "detail = " + mNotesItem.getDetail() + "   "
        + "user = " + user + "   "
        + "mod date = " + modDate);
    }

    try
    {
      if (getAction().equals(ACTION_ADD))
      {
        mNotesItem.add();
        setAction(ACTION_VIEW);
      }
      else if (getAction().equals(ACTION_CANCEL))
      {
        setAction(ACTION_VIEW);
      }
      else if (getAction().equals(ACTION_CONFIRM_DELETE))
      {
        mNotesItem.delete();
        setAction(ACTION_VIEW);
      }
      else if (getAction().equals(ACTION_SAVE))
      {
        mNotesItem.save();
        setAction(ACTION_VIEW);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return "go_error";
    }

    return getNavigationOutcome();
  }

}
