/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

public class ListSearch
{
  public static final String LIST_SEARCH = "listSearch";
  public static final String LIST_SEARCH_INPUT = "listSearchInput";
  public static final String LIST_SEARCH_APPLY = "listSearchApplyButton";
  public static final String LIST_SEARCH_CLEAR = "listSearchClearButton";

  private String search;
  public String getSearch() { return search; }
  public void setSearch(String search) { this.search = search; }

  public void clear()
  {
    search = null;
  }

  public boolean isValueEmpty()
  {
    if (search == null || search.toString().trim().length() == 0)
      return true;
    return false;
  }

}
