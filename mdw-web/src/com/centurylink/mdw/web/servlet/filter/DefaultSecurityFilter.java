/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.util.RemoteLocator;

public class DefaultSecurityFilter extends SecurityFilter
{
  public static final String ACCESS_CONTROL_FILTER = "com.qwest.appsec.TomcatSecurityFilter";
  public static final String AUTH_USER_HEADER = "ct-remote-user";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Filter accessControlFilter;

  public void init(FilterConfig filterConfig) throws ServletException
  {
    super.init(filterConfig);
    try
    {
      if (!isUseMdwAuthFilter())
      {
        // use the AppSec access control filter
        Class<? extends Filter> filterClass = Class.forName(ACCESS_CONTROL_FILTER).asSubclass(Filter.class);
        accessControlFilter = filterClass.newInstance();
        logger.info("Using Web Agent access control: " + accessControlFilter.getClass().getName());
        accessControlFilter.init(filterConfig);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
  {
    if (!(request instanceof HttpServletRequest))
    {
      chain.doFilter(request, response);
      return;
    }
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    if (httpRequest.getServletPath().equals("/") || httpRequest.getServletPath().equals("/facelets/index.xhtml"))
    {
      // handle welcome page forwarding since jetty doesn't seem to
      if (httpRequest.getContextPath().equals("/" + ApplicationContext.getMdwWebContextRoot()))
      {
        httpResponse.sendRedirect("/" + ApplicationContext.getMdwWebContextRoot() + ApplicationContext.getMdwWebWelcomePath());
      }
      else if (httpRequest.getContextPath().equals("/" + ApplicationContext.getTaskManagerContextRoot())
          && ! httpRequest.getContextPath().equals("/" + ApplicationContext.getMdwHubContextRoot()))
      {
        httpResponse.sendRedirect("/" + ApplicationContext.getTaskManagerContextRoot() + ApplicationContext.getTaskWelcomePath());
      }
      else if (httpRequest.getContextPath().equals("/" + ApplicationContext.getReportsContextRoot()))
      {
        httpResponse.sendRedirect("/" + ApplicationContext.getReportsContextRoot() + "/reportsList.jsf");
      }
      else if (httpRequest.getContextPath().equals("/mdw-taskmgr"))
      {
        // standard TaskManager in the context of a custom TaskManager
        httpResponse.sendRedirect("/MDWTaskManagerWeb/facelets/tasks/myTasks.jsf");
      }
    }
    else if (httpRequest.getServletPath().equals("/filepanel")
             && httpRequest.getContextPath().equals("/" + ApplicationContext.getMdwWebContextRoot()))
    {
      httpResponse.sendRedirect("/" + ApplicationContext.getMdwWebContextRoot() + "/filepanel/index.jsf");
    }

    // prevent appending jsession id on URLs in initial request
    // TODO: does this do anything useful?
    HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(httpResponse)
    {
      public String encodeRedirectUrl(String url) { return url; }
      public String encodeRedirectURL(String url) { return url; }
      public String encodeUrl(String url) { return url; }
      public String encodeURL(String url) { return url; }
    };

    if (accessControlFilter != null)
    {
      AuthenticatedUser user = (AuthenticatedUser) httpRequest.getSession().getAttribute(MiscConstants.authenticatedUser);
      String userId = httpRequest.getHeader(AUTH_USER_HEADER);
      if (user == null || user.getCuid() == null || (userId != null && !user.getCuid().equals(userId)))
      {
        if (userId != null)
        {
          try
          {
            user = RemoteLocator.getUserManager().loadUser(userId);
            if (user == null)
            {
              if (isAllowAnyAuthenticatedUser())
              {
                user = new AuthenticatedUser(userId);
              }
              else
              {
                disallow(httpRequest, httpResponse);
              }
            }
            httpRequest.getSession().setAttribute(MiscConstants.authenticatedUser, user);
            logger.info("Authenticated User: " + user);
          }
          catch (Exception ex)
          {
            showException(httpRequest, httpResponse, ex);
          }
        }
      }
      accessControlFilter.doFilter(request, wrappedResponse, chain);
    }
    else
    {
      chain.doFilter(request, wrappedResponse);
    }
  }

  public void destroy()
  {
  }
}
