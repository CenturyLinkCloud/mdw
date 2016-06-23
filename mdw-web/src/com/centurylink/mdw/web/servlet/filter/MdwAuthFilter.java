/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ajax4jsf.webapp.BaseFilter;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.qwest.appsec.actrl.AccessControlInfo;

/**
 * Servlet filter for ensuring that users have been authenticated.
 * Currently used for MDW LDAP Auth as well as OAuth (not used for ClearTrust auth -- see MDWClearTrustFilter).
 */
public class MdwAuthFilter extends SecurityFilter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void init(FilterConfig filterConfig) throws ServletException
  {
    super.init(filterConfig);
    try
    {
      logger.info("Access Control: " + AccessControlInfo.class.getName());
    }
    catch (Throwable t)
    {
      logger.severeException(t.getMessage(), t);
    }
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException
  {
    logger.mdwDebug("MdwAuthFilter: Request received from remote host: " + req.getRemoteHost() + "\n");

    if (!(req instanceof HttpServletRequest))
      return;

    HttpServletRequest request = (HttpServletRequest) req;
    if (request.getServletPath().equals("/javax.faces.resource/jsf.js.jsf"))
    {
      // avoid creating a new session for jsf js requests
      chain.doFilter(req, resp);
      return;
    }

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
        if (sessionTimeout==20) session.setMaxInactiveInterval(1800);
      }
    } catch (NumberFormatException e) {
      logger.warn("Property needs to have numeric value: " + PropertyNames.MDW_WEB_SESSION_TIMEOUT);
    }
    }

    // don't interfere with error or login processing
    if (request.getServletPath().endsWith("error.jsf")
        || request.getServletPath().endsWith("ldapLogin.jsf")
        || request.getServletPath().endsWith("mdwLogin.jsf")
        || request.getServletPath().endsWith("mdwLoginError.jsf")
        || request.getServletPath().endsWith("/login")
        || request.getServletPath().endsWith("/loginError")
        || request.getServletPath().endsWith("MDWHTTPListener/login"))
    {
      chain.doFilter(request, safeResponse);
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

    if ((user == null || user.getCuid() == null))
    {
      if (ApplicationContext.isDevelopment())
      {
        // special handling for dev environment
        String devUser = ApplicationContext.getDevUser();
        if (devUser != null && devUser.length()>0)
        {
          try
          {
            user = RemoteLocator.getUserManager().loadUser(devUser);
            session.setAttribute("authenticatedUser", user);
            if (user == null)
              throw new MDWException("User not authorized: " + devUser);
            logger.info("Authenticated User: " + user.getCuid());
            logger.mdwDebug("Auth User Details:\n" + user);
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            showException(request, response, ex);
          }
        }
      }

      if (user == null || user.getCuid() == null)
      {
        if (!getAuthExclusionPatterns().match(getUrlPath(request)))
        {
          // disallow
            redirectToLogin(request, response);
            return;
        }
      }
    }

    chain.doFilter(request, safeResponse);
  }
}