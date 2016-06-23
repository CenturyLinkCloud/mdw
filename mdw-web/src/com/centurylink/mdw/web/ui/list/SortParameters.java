/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

public class SortParameters
{
  public SortParameters(String sort, boolean ascending)
  {
    _sort = sort;
    _ascending = ascending;
  }

  private String _sort;
  public String getSort() { return _sort; }
  public void setSort(String s) { _sort = s; }

  private boolean _ascending = true;
  public boolean isAscending() { return _ascending; }
  public void setAscending(boolean b) { _ascending = b; }

  public String toString()
  {
    return "Sort: " + getSort() + "   Ascending: " + isAscending();
  }

}
