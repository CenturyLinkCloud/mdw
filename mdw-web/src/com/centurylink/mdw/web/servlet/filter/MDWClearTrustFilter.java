/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ajax4jsf.webapp.BaseFilter;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.util.RemoteLocator;


/**
 * Servlet filter for populating the session's "authenticatedUser" with
 * the values passed from ClearTrust in HTTP Headers.
 */
public class MDWClearTrustFilter extends SecurityFilter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
  throws IOException, ServletException
  {
    logger.mdwDebug("Request received from remote host: " + req.getRemoteHost() + "\n");

    if (!(req instanceof HttpServletRequest))
      return;

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    SafeResponseWrapper safeResponse = new SafeResponseWrapper(response);
    HttpSession session = request.getSession();

    if (session.isNew())
    {
    	logger.debug("-- new HTTP session from " + request.getRemoteHost());
		try {
			String v = PropertyManager.getProperty(PropertyNames.MDW_WEB_SESSION_TIMEOUT);
			if (v!=null) {
				int sessionTimeout = Integer.parseInt(v);
				session.setMaxInactiveInterval(sessionTimeout);
			} else {
				int sessionTimeout = session.getMaxInactiveInterval();
				// override 20 seconds set in MDWWeb's weblogic.xml
				if (sessionTimeout==20) session.setMaxInactiveInterval(3600);
			}
		} catch (NumberFormatException e) {
			logger.warn("Property needs to have numeric value: " + PropertyNames.MDW_WEB_SESSION_TIMEOUT);
		}
    }

    // don't interfere with error processing
    if (request.getServletPath().endsWith("error.jsf"))
    {
      chain.doFilter(req, safeResponse);
      return;
    }

    // hack to avoid RichFaces interference with JSON (non-XML) requests for FilePanel
    String servletPath = request.getServletPath();
    if (servletPath.startsWith("/filepanel") || servletPath.startsWith("/property")
        || servletPath.startsWith("/configManager") || servletPath.startsWith("/system"))
    {
      request.setAttribute(BaseFilter.FILTER_PERFORMED, Boolean.TRUE);
    }

    String runtimeEnv = System.getProperty("runtimeEnv");
    if (runtimeEnv == null)
    {
      showError(request, response, "Missing Java VM parameter 'runtimeEnv'.");
    }

    AuthenticatedUser user = (AuthenticatedUser) session.getAttribute("authenticatedUser");

    if ((user == null || user.getCuid() == null) || differentUser(session, request))
    {
      String cuid = null;
      if (request.getRemoteUser() != null)
      {
        cuid = request.getRemoteUser();
      }
      else if (ApplicationContext.isDevelopment())
      {
        // special handling for dev environment
        String devUser = ApplicationContext.getDevUser();
        if (devUser != null && devUser.length() > 0)
          cuid = devUser;
      }

      if (cuid != null)
      {
        try
        {
          user = RemoteLocator.getUserManager().loadUser(cuid);
          if (user == null)
          {
            if (isAllowAnyAuthenticatedUser())
            {
              user = new AuthenticatedUser(cuid);
            }
            else
            {
              disallow(request, response);
            }
          }
          session.setAttribute(MiscConstants.authenticatedUser, user);
          logger.info("Authenticated User: " + user);
        }
        catch (Exception ex)
        {
          showException(request, response, ex);
        }
      }
      else
      {
        if (!getAuthExclusionPatterns().match(getUrlPath(request)))
        {
          disallow(request,response);
        }
      }
    }

    chain.doFilter(req, safeResponse);
  }
}