/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import com.centurylink.mdw.designer.display.TextNote;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

public class Note extends WorkflowElement
{
  private TextNote textNote;
  public TextNote getTextNote() { return textNote; }
  public void setTextNote(TextNote textNote) { this.textNote = textNote; }

  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  public Note(TextNote textNote, WorkflowProcess processVersion)
  {
    this.textNote = textNote;
    this.process = processVersion;
    setProject(processVersion.getProject());
  }

  public String getTitle()
  {
    return "Note";
  }

  public Long getId()
  {
    return textNote.getId();
  }

  public String getName()
  {
    return textNote.getName();
  }
  public void setName(String name)
  {
    textNote.setName(name);
  }

  @Override
  public String getFullPathLabel()
  {
    return getPath() + (getProcess() == null ? "Note " : getProcess().getName() + "/Note ") + getLabel();
  }

  public String getText()
  {
    return textNote.vo.getContent();
  }
  public void setText(String text)
  {
    textNote.setText(text);
  }

  public void adjustSize()
  {
    textNote.adjustSize();
  }

  public String getIcon()
  {
    return "doc.gif";
  }

  public boolean isReadOnly()
  {
    return process.isReadOnly();
  }

  public boolean hasInstanceInfo()
  {
    return false;
  }

  public Entity getActionEntity()
  {
    return Entity.Note;
  }
}
