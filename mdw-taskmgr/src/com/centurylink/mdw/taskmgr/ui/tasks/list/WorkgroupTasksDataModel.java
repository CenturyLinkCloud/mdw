/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.ui.list.ListSearch;
import com.centurylink.mdw.web.util.RemoteLocator;

public class WorkgroupTasksDataModel extends PagedListDataModel
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();
  private List<String> userPrefrdDBClmns = new ArrayList<String>();

  public WorkgroupTasksDataModel(ListUI listUI, Filter filter,List<String> dbClmns)
  {
    super(listUI, filter);
    this.userPrefrdDBClmns = dbClmns;
  }

  public PaginatedResponse fetchPage(int pageIndex, Filter filter)
  {
    try
    {
      Map<String,String> criteriaMap = filter == null ? new HashMap<String,String>() : ((com.centurylink.mdw.taskmgr.ui.filter.Filter)filter).buildCriteriaMap();
      Map<String,String> specialCriteria = filter == null ? null : filter.getSpecialCriteria();
      Map<String,String> indexCriteria = filter == null ? null : filter.getIndexCriteria();

      String[] groups = FacesVariableUtil.getCurrentUser().getWorkgroupNames();

      // User is not authorized to view WorkGroup Tasks page when he belongs only to Common group.
      if (groups == null || !FacesVariableUtil.getCurrentUser().isHasWorkgroupsOtherThanCommon())
      {
        return null;
      }

      TaskManager taskMgr = RemoteLocator.getTaskManager();

      if (getListUI().isSearchable())
      {
        ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
        if (listSearch != null && !listSearch.isValueEmpty())
        {
          QueryRequest searchQueryRequest = new QueryRequest();
          searchQueryRequest.setRestrictions(new HashMap<String,String>());
          searchQueryRequest.setOrderBy(getSortColumn());
          searchQueryRequest.setIsAscendingOrder(isSortAscending());
          searchQueryRequest.setPageIndex(pageIndex);
          searchQueryRequest.setPageSize(getPageSize());
          searchQueryRequest.setShowAllDisplayRows(getListUI().getShowAllDisplayRows());
          return taskMgr.getUnClaimedTaskInstanceVOs(groups, searchQueryRequest, getSpecialColumns(), null, getIndexColumns(), null, this.userPrefrdDBClmns, listSearch.getSearch());
        }
      }

      QueryRequest queryRequest = new QueryRequest();
      queryRequest.setRestrictions(criteriaMap);
      queryRequest.setOrderBy(getSortColumn());
      queryRequest.setIsAscendingOrder(isSortAscending());
      queryRequest.setPageIndex(pageIndex);
      queryRequest.setPageSize(getPageSize());
      queryRequest.setShowAllDisplayRows(getListUI().getShowAllDisplayRows());
      return taskMgr.getUnClaimedTaskInstanceVOs(groups, queryRequest, getSpecialColumns(), specialCriteria, getIndexColumns(), indexCriteria);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }
}
