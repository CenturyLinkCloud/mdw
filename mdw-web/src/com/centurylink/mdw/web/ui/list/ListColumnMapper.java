/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;


public interface ListColumnMapper
{
  /**
   * Map listUI column names to database column names
   */
  public String getDatabaseColumn(String listColumn);
}
