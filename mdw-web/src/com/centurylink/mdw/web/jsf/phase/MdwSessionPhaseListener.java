/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;

public class MdwSessionPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }

  public void beforePhase(PhaseEvent phaseEvent)
  {
    if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
    {
      Object mdwObj = FacesVariableUtil.getValue("mdw");
      if (mdwObj instanceof MDWBase) // i.e. not MDWHub
      {
        MDWBase mdw = (MDWBase) mdwObj;
        mdw.beforeAllPhases(phaseEvent.getFacesContext());
      }
      FacesContext facesContext = phaseEvent.getFacesContext();
      ExternalContext externalContext = facesContext.getExternalContext();
      if ("true".equalsIgnoreCase(facesContext.getExternalContext().getRequestParameterMap().get("mdwResetSessionParam")))
      {
        Object session = externalContext.getSession(false);
        if (session instanceof HttpSession)
        {
          AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
          HttpSession httpSession = (HttpSession) session;
          httpSession.invalidate();
          FacesVariableUtil.setValue("authenticatedUser", authUser);
          HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
          response.setHeader("Cache-Control", "no-cache,no-store");
          response.setHeader("Pragma", "no-cache");
        }
      }
    }
    if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
    {
      Object mdwObj = FacesVariableUtil.getValue("mdw");
      if (mdwObj instanceof MDWBase) // i.e. not MDWHub
      {
        MDWBase mdw = (MDWBase) mdwObj;
        mdw.beforeRenderResponse(phaseEvent.getFacesContext());
      }

      // handle session logout
      FacesContext facesContext = phaseEvent.getFacesContext();
      UIViewRoot viewRoot = facesContext.getViewRoot();
      if ((viewRoot != null && viewRoot.getViewId() != null && viewRoot.getViewId().endsWith("/logout.xhtml")))
      {
        ExternalContext externalContext = facesContext.getExternalContext();
        Object session = externalContext.getSession(false);
        if (session instanceof HttpSession)
        {
          HttpSession httpSession = (HttpSession) session;
          httpSession.invalidate();

          HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
          response.setHeader("Cache-Control", "no-cache,no-store");
          response.setHeader("Pragma", "no-cache");

          Map<String,String> params = externalContext.getRequestParameterMap();
          if ("true".equalsIgnoreCase(params.get("removeSessionCookie")))
          {
            // FROM APPSEC TEAM EXAMPLE LOGOUT JSP:
            // If you wish, you can add code to immediately delete the cookie for
            // the HttpSession object (generally, JSESSIONID), by setting it to
            // a date in the past. E.g., for JSESSIONID ...
            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            Cookie[] cookies = request.getCookies();
            if (request.isRequestedSessionIdFromCookie() && cookies != null)
            {
              for (Cookie cookie : cookies)
              {
                if ("JSESSIONID".equals(cookie.getName()))
                {
                  cookie.setMaxAge(0); // Delete JSESSIONID cookie
                  response.addCookie(cookie);
                  break;
                }
              }
            }
          }

          // send client redirect so that ClearTrust will recognize that it needs to terminate the session
          if (params.get("logoutLocation") != null)
          {
            try
            {
              response.sendRedirect(params.get("logoutLocation"));
              facesContext.responseComplete();
            }
            catch (IOException ex)
            {
              logger.severeException(ex.getMessage(), ex);
            }
          }
        }
      }
    }
  }

  public void afterPhase(PhaseEvent phaseEvent)
  {
    if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
    {
      Object mdwObj = FacesVariableUtil.getValue("mdw");
      if (mdwObj instanceof MDWBase) // i.e. not MDWHub
      {
        MDWBase mdw = (MDWBase) mdwObj;
        mdw.afterAllPhases();
      }
    }
  }

}
