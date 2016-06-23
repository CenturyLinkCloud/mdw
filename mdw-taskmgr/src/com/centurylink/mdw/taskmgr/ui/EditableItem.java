/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import com.centurylink.mdw.web.ui.UIException;


/**
 * Functionality common to items with id, name and comment attributes
 * that can be persisted via the workflow.
 */
public interface EditableItem
{
  public Long getId();
  public String getName();
  public void setName(String name);
  public String getComment();
  public void setComment(String comment);


  public void add()
    throws UIException;

  public void delete()
    throws UIException;

  public void save()
    throws UIException;
  
  public boolean isEditableByCurrentUser();
}
