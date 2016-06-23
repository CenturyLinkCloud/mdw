/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import java.util.Iterator;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.ListSearch;

/**
 * JSF phase listener for repopulating the lists cached by ListManager.
 */
public class ListRefreshPhaseListener implements PhaseListener
{
  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }

  /**
   * Force list refresh before the Restore View phase for GET requests.
   */
  public void beforePhase(PhaseEvent event)
  {
    if (event.getPhaseId().equals(PhaseId.RESTORE_VIEW))
    {
      FacesContext facesContext = event.getFacesContext();
      Object request = facesContext.getExternalContext().getRequest();
      if (((HttpServletRequest)request).getMethod().equalsIgnoreCase("get"))
      {
        clearListManager();
        clearListSearch();
        try
        {
          WorkgroupTree workgroupTree = WorkgroupTree.getInstance();
          workgroupTree.clearSelection();
          workgroupTree.clearDisplayList();
        }
        catch (NoClassDefFoundError er)
        {
          // happens when taskmgr jar used with RichFaces 4.0
        }
        if ("true".equalsIgnoreCase(facesContext.getExternalContext().getRequestParameterMap().get("mdwReloadUi")))
        {
          // TODO: does not require JSF so could be invoked through servlet standard filter
          // (used this way for convenience since error.jsf is already excluded from CT auth for all apps)
          ViewUI.clear();
          TaskActions.clear();
        }
      }
    }
    else if (event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION))
    {
      Map<String,String> requestParams = event.getFacesContext().getExternalContext().getRequestParameterMap();
      if (requestParams.get("refreshUserTasks") != null || requestParams.get("refreshWorkgroupTasks") != null)
        clearListManager();
    }
  }

  /**
   * Force the lists to refresh after the Invoke Application phase
   * or The Apply Request Values Phase if sorting has occurred.
   */
  public void afterPhase(PhaseEvent event)
  {
    if (event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION))
    {
      // don't clear lists for ajax requests or when there are list action validation messages
      Map<String,String> requestParams = event.getFacesContext().getExternalContext().getRequestParameterMap();
      if (!event.getFacesContext().getMessages().hasNext())
      {
        if (requestParams.get("AJAXREQUEST") == null && !"true".equals(requestParams.get("javax.faces.partial.ajax")))
        {
          // not an ajax request
          clearListManager();
        }
        else
        {
          for (String requestParam : requestParams.keySet())
          {
            if (requestParam.endsWith("listRefreshButton") && requestParam.equals(requestParams.get(requestParam)))
              clearListManager();
          }
        }
      }
    }
    else if (event.getPhaseId().equals(PhaseId.APPLY_REQUEST_VALUES))
    {
      ExternalContext externalContext = event.getFacesContext().getExternalContext();
      boolean clearSearch = false;
      String searchValue = null;
      for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
      {
        String paramName = iter.next();
        String value = externalContext.getRequestParameterMap().get(paramName);
        if (value != null)
        {
          // sort headers do not trigger invoke application phase
          if (value.endsWith("sortHeader"))
            clearListManager();
          else if (paramName.endsWith(ListSearch.LIST_SEARCH_CLEAR))
            clearSearch = true;
          else if (paramName.endsWith(ListSearch.LIST_SEARCH_INPUT))
            searchValue = externalContext.getRequestParameterMap().get(paramName);
        }
      }
      Map<String,String> requestParams = event.getFacesContext().getExternalContext().getRequestParameterMap();
      if (clearSearch)
      {
        clearListSearch();
        if (requestParams.get("AJAXREQUEST") != null || "true".equals(requestParams.get("javax.faces.partial.ajax")))
        {
          clearListManager();
        }
      }
      else if (searchValue != null && !searchValue.isEmpty())
      {
        ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
        listSearch.setSearch(searchValue);
        if (requestParams.get("AJAXREQUEST") != null || "true".equals(requestParams.get("javax.faces.partial.ajax")))
        {
          clearListManager();
        }
      }
    }
  }

  private void clearListManager()
  {
    ListManager listManager = ListManager.getInstance();
    listManager.invalidate();
  }

  private void clearListSearch()
  {
    ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
    listSearch.clear();
  }
}
