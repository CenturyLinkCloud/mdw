/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.data;

public interface RowConverter
{
  /**
   * Converts a data row for use by the PagedListDataModel
   * @param o the data to convert
   * @return the row in the form returned by PagedListDataModel.getRowData()
   */
  public Object convertRow(Object o);
}
