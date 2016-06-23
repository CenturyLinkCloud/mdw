/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.ui.list.ListSearch;
import com.centurylink.mdw.web.util.RemoteLocator;

public class AuditEventsDataModel extends PagedListDataModel
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();
  public static int MAX_ROWS_IN_ALL_ROWS_MODE = 200;
  private List<String> userPrefrdDBClmns = new ArrayList<String>();

  public AuditEventsDataModel(ListUI listUI, Filter filter, List<String> userPrefrdDBColumns)
  {
    super(listUI, filter);
    this.userPrefrdDBClmns = userPrefrdDBColumns;
  }

  public PaginatedResponse fetchPage(int pageIndex, Filter filter)
  {
    try
    {
      Map<String,String> criteriaMap = filter == null ? new HashMap<String,String>() : ((com.centurylink.mdw.taskmgr.ui.filter.Filter)filter).buildCriteriaMap();
      EventManager eventMgr = RemoteLocator.getEventManager();

      if (getListUI().isSearchable())
      {
        ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
        if (listSearch != null && !listSearch.isValueEmpty() && !userPrefrdDBClmns.isEmpty())
        {
          QueryRequest searchQueryRequest = new QueryRequest();
          searchQueryRequest.setRestrictions(new HashMap<String,String>());
          searchQueryRequest.setOrderBy(getSortColumn());
          searchQueryRequest.setIsAscendingOrder(isSortAscending());
          searchQueryRequest.setPageIndex(pageIndex);
          searchQueryRequest.setPageSize(getPageSize());
          searchQueryRequest.setShowAllDisplayRows(getListUI().getShowAllDisplayRows());
          return eventMgr.getAuditLogs(searchQueryRequest, userPrefrdDBClmns, listSearch.getSearch());
        }
      }

      QueryRequest queryRequest = new QueryRequest();
      queryRequest.setRestrictions(criteriaMap);
      queryRequest.setOrderBy(getSortColumn());
      queryRequest.setIsAscendingOrder(isSortAscending());
      queryRequest.setPageIndex(pageIndex);
      queryRequest.setPageSize(getPageSize());
      queryRequest.setShowAllDisplayRows(getListUI().getShowAllDisplayRows());
      return eventMgr.getAuditLogs(queryRequest);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }
}
