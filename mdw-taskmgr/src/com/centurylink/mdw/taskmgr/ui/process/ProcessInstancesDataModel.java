/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.filter.Filter;

/**
 * Data model for retrieving paginated lists of processes based on name.
 */
public class ProcessInstancesDataModel extends PagedListDataModel
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public ProcessInstancesDataModel(ListUI listUI, Filter filter)
  {
    super(listUI, filter);
  }

  // TODO : Implement search functionality if required.
  public PaginatedResponse fetchPage(int pageIndex, Filter filter)
  {
    try
    {
      Map<String,String> criteriaMap = filter == null ? new HashMap<String,String>() : ((com.centurylink.mdw.taskmgr.ui.filter.Filter)filter).buildCriteriaMap();
      Map<String,String> specialCriteria = filter == null ? null : filter.getSpecialCriteria();

      // must have processId in the criteria
      String processIdStr = criteriaMap.get("processId");
      String processIdListStr = criteriaMap.get("processIdList");
      if (processIdStr == null && processIdListStr == null);
      //  return new PaginatedResponse(new ProcessInstanceVO[0], 0, 0, 0, 0);

      List<ProcessVO> processVOs = new ArrayList<ProcessVO>();
      if (processIdStr != null)
      {
        Long processId = new Long(processIdStr.replaceAll("['\\= ]", ""));
        processVOs.add(ProcessVOCache.getProcessVO(processId));
      }
      else if (processIdListStr != null)
      {
        StringTokenizer st = new StringTokenizer(processIdListStr, "(), ");
        while (st.hasMoreTokens())
        {
          Long processId = new Long(st.nextToken());
          processVOs.add(ProcessVOCache.getProcessVO(processId));
        }
      }

      MDWDataAccess dataAccess = (MDWDataAccess)FacesVariableUtil.getValue("dataAccess");
      RuntimeDataAccess runtimeDataAccess = dataAccess.getRuntimeDataAccess();

      int pageSize = getPageSize() == QueryRequest.ALL_ROWS ? QueryRequest.ALL_ROWS : getListUI().getDisplayRows();
      String orderBy = getOrderBy(processVOs);

      ProcessList procList = runtimeDataAccess.getProcessInstanceList(criteriaMap, getSpecialColumns(), specialCriteria, pageIndex + 1, pageSize, orderBy);
      Long count = procList.getTotal();
      List<ProcessInstanceVO> processInstances = procList.getProcesses();

      return new PaginatedResponse(processInstances.toArray(new ProcessInstanceVO[0]), count.intValue(), processInstances.size(), getPageSize(), pageIndex);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  protected String getOrderBy(List<ProcessVO> processVOs)
  {
    String sortCol = getSortColumn();
    if (sortCol == null)
      return null;

    String orderBy = "    order by ";

    VariableVO varVO = null;
    for (ProcessVO processVO : processVOs)
    {
      varVO = processVO.getVariable(sortCol);
      if (varVO != null)
        break;
    }

    if (varVO != null)
    {
      String varType = varVO.getVariableType();

      if (varType.equals(Integer.class.getName()) || varType.equals(Long.class.getName()))
        orderBy += "to_number(" + sortCol + ")";
      else if (varType.equals(Date.class.getName()))
        orderBy += "to_date(substr(" + sortCol + ", 5, 7) || substr(" + sortCol + ", 25), 'MON DD YYYY')";
      else
        orderBy += sortCol;
    }
    else
    {
      orderBy += sortCol;
    }

    if (!isSortAscending())
      orderBy += " desc";

    return orderBy;
  }
}
