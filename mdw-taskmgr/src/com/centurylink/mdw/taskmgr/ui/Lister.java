/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import java.util.List;

import javax.faces.model.SelectItem;

public interface Lister
{
  /**
   * Returns a list to populate a dropdown.
   *
   * @return list of SelectItems
   */
  public List<SelectItem> list();

}
