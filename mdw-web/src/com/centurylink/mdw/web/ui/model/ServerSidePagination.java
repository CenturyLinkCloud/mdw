/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.model;

import com.centurylink.mdw.common.query.PaginatedResponse;


/**
 * Indicates that paginated results are expected.
 */
public interface ServerSidePagination
{
  public int getPageSize();
  public PaginatedResponse getPage();
  public boolean isAllRowsMode();
  public void setAllRowsMode(boolean allRows);
}
