/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.list;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.util.RemoteLocator;

public class ExternalEventsDataModel extends PagedListDataModel
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public ExternalEventsDataModel(ListUI listUI, Filter filter)
  {
    super(listUI, filter);
  }

  public PaginatedResponse fetchPage(int pageIndex, Filter filter)
  {
    try
    {
      List<QueryRequest.Restriction> restrictions = filter == null ? new ArrayList<QueryRequest.Restriction>() : ((com.centurylink.mdw.taskmgr.ui.filter.Filter)filter).buildRestrictions();

      QueryRequest queryRequest = new QueryRequest();
      queryRequest.setPageIndex(pageIndex);
      queryRequest.setPageSize(getPageSize());
      queryRequest.setRestrictionList(restrictions);
      queryRequest.setOrderBy(getSortColumn());
      queryRequest.setIsAscendingOrder(isSortAscending());

      EventManager eventMgr = RemoteLocator.getEventManager();

      return eventMgr.getExternalEventInstanceVOs(queryRequest);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }
}
