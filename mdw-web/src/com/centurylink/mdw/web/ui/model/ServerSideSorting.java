/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.model;

/**
 * Indicates that sorting is done on the server.
 */
public interface ServerSideSorting
{
  public void setSortColumn(String sortColumn);
  public String getSortColumn();

  public void setSortAscending(boolean isAscending);
  public boolean isSortAscending();

  public void sort();
}
