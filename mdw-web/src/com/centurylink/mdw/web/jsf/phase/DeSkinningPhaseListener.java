/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.ajax4jsf.application.AjaxViewHandler;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;

/**
 * Remove the RichFaces skinning from the login page so that
 * ClearTrust does not redirect to skinning resource URLs after login.
 */
public class DeSkinningPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void beforePhase(PhaseEvent event)
  {
    Object mdwObj = FacesVariableUtil.getValue("mdw");
    if (mdwObj instanceof MDWBase) // i.e. not MDWHub
    {
      MDWBase mdw = (MDWBase) mdwObj;
      ExternalContext externalContext = event.getFacesContext().getExternalContext();
      String path = externalContext.getRequestServletPath();

      // TODO: allow user-specified login pages
      if (path != null && (path.endsWith("login.jsf") || path.endsWith("ldapLogin.jsf") || path.endsWith("loginError.jsf") || path.endsWith("mdwLogin.xhtml") || path.endsWith("/login") || path.endsWith("/loginError")))
      {
        mdw.setSkin("plain");
        Map<String,Object> requestMap = externalContext.getRequestMap();
        // let RichFaces think the resources have already been processed
        requestMap.put(AjaxViewHandler.RESOURCES_PROCESSED, Boolean.TRUE);
      }
      else if ("plain".equals(mdw.getSkin()))
      {
        mdw.setSkin(null); // defer to default prop-driven setting
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
  }
}
