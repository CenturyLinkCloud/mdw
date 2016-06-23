/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.notes;

import java.util.Date;

import com.centurylink.mdw.model.data.common.InstanceNote;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Wraps a model Notes instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public abstract class NotesItem extends ListItem
{
  InstanceNote mNote;

  /**
   * Add the note.
   */
  public abstract void add();
  /**
   * Delete the note.
   */
  public abstract void delete();
  /**
   * Update the note.
   */
  public abstract void save();

  public InstanceNote getInstanceNote()
  {
    return mNote;
  }
  /**
   * No-arg constructor for adding new notes.
   */
  public NotesItem()
  {
    mNote = new InstanceNote();
  }
  public NotesItem(InstanceNote note)
  {
    mNote = note;
  }

  public void clear()
  {
    mNote = new InstanceNote();
  }

  public Long getId()
  {
    return mNote.getId();
  }

  public String getSummary()
  {
    return mNote.getNoteName();
  }

  public void setSummary(String s)
  {
    mNote.setNoteName(s);
  }

  public Date getCreatedDate()
  {
    return mNote.getCreatedDate();
  }

  public void setCreatedDate(Date d)
  {
    mNote.setModifiedDate(d);
  }

  public String getCreatedBy()
  {
    return mNote.getCreatedBy();
  }

  public void setCreatedBy(String s)
  {
    mNote.setCreatedBy(s);
  }
  
  public Date getModifiedDate()
  {
    if (mNote.getModifiedDate() == null)
      return mNote.getCreatedDate();
    else
      return mNote.getModifiedDate();
  }

  public void setModifiedDate(Date d)
  {
    mNote.setModifiedDate(d);
  }

  public String getModifiedBy()
  {
    if (mNote.getModifiedBy() == null)
      return mNote.getCreatedBy();
    else
      return mNote.getModifiedBy();
  }

  public void setModifiedBy(String s)
  {
    mNote.setModifiedBy(s);
  }
  
  public String getDetail()
  {
    return mNote.getNoteDetails();
  }

  public void setDetail(String s)
  {
    mNote.setNoteDetails(s);
  }

  public Long getOwnerId()
  {
    return mNote.getOwnerId();
  }

  public void setOwnerId(Long instanceId)
  {
    mNote.setOwnerId(instanceId);
  }

}
