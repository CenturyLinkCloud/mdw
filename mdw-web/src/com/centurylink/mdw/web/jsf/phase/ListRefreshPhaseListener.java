/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.util.Iterator;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.SortableList;

public class ListRefreshPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;

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
      Object request = event.getFacesContext().getExternalContext().getRequest();
      if (request instanceof HttpServletRequest)
      {
        if (((HttpServletRequest)request).getMethod().equalsIgnoreCase("get"))
        {
          refreshLists(event.getFacesContext());
        }
      }
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
      if (requestParams.get("AJAXREQUEST") == null
          && !event.getFacesContext().getMessages().hasNext())
      {
        refreshLists(event.getFacesContext());
      }
    }
    else if (event.getPhaseId().equals(PhaseId.APPLY_REQUEST_VALUES))
    {
      ExternalContext externalContext = event.getFacesContext().getExternalContext();
      for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
      {
        String paramName = iter.next();
        String value = externalContext.getRequestParameterMap().get(paramName);
        if (value != null)
        {
          // sort headers do not trigger invoke application phase
          if (value.endsWith("sortHeader") || value.endsWith("listPrefsResetButton"))
            refreshLists(event.getFacesContext());
          else if (paramName.endsWith("_scroller") && value.equals("all"))
          {
            refreshLists(event.getFacesContext());
            int idx = paramName.lastIndexOf(':');
            if (idx == -1)
              idx = 0;
            String listId = paramName.substring(idx + 1, paramName.length() - 9);
            Object val = FacesVariableUtil.getValue(listId);
            if (val instanceof SortableList)
              ((SortableList)val).setShowAll(true);
          }          
        }
      }
    }    
  }
  
  private void refreshLists(FacesContext facesContext)
  {
    Map<String,Object> sessionMap = facesContext.getExternalContext().getSessionMap();
    for (String sessionKey : sessionMap.keySet())
    {
      Object sessionObj = sessionMap.get(sessionKey);
      if (sessionObj instanceof SortableList)
        ((SortableList)sessionObj).clear();
    }
    clearCurrentRows();
  }
  
  public void clearCurrentRows()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    
    // clear existing sessionMap values 
    Map<String, Object> sessionMap = externalContext.getSessionMap();
    for (Iterator<String> iter = sessionMap.keySet().iterator(); iter.hasNext(); )
    {
      String key = iter.next().toString();
      if (key.startsWith("dataTableCurrentRow"))
        sessionMap.remove(key);
    }    

    // preserve specifically-requested values
    Map<String, Object> requestMap = externalContext.getRequestMap();
    for (Iterator<String> iter = requestMap.keySet().iterator(); iter.hasNext(); )
    {
      String key = iter.next().toString();
      if (key.startsWith("dataTableCurrentRow"))
        sessionMap.put(key, requestMap.get(key));
    }    
  }
}
