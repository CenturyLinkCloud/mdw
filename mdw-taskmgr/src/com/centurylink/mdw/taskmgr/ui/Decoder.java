/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

public interface Decoder
{
  /**
   * Looks up the user display value based on a system ID.
   * @param id
   * @return the value to display, or null if not found
   */
  public String decode(Long id);
}
