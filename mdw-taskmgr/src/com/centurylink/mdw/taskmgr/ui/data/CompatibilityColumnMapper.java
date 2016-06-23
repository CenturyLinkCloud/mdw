/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.data;

import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.ui.list.ListColumnMapper;

public class CompatibilityColumnMapper implements ListColumnMapper
{
  private ListUI listUI;
  public CompatibilityColumnMapper(ListUI listUI)
  {
    this.listUI = listUI;
  }

  public String getDatabaseColumn(String listColumn)
  {
    ItemUI column = listUI.getColumn(listColumn);

    // TODO: what does this do?
    if (column.getAttribute().startsWith("$"))
      return column.getAttribute().substring(1);

    if (column.getDbColumn() == null)
      return listColumn;
    else
      return column.getDbColumn();
  }

}
