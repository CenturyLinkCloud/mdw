/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.list;

import java.util.List;

import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;

public interface ColumnSpecifier
{
  public List<ItemUI> getColumns(ListUI listUi);
}
