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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.model.value.process.PackageVO;

public class AccessControlFilter extends SecurityFilter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public static final String OSGI_SECURITY_FILTER = "com.centurylink.mdw.web.servlet.filter.OsgiSecurityFilter";
  public static final String DEFAULT_SECURITY_FILTER = "com.centurylink.mdw.web.servlet.filter.DefaultSecurityFilter";
  public static final String WEBLOGIC_SECURITY_FILTER = "com.qwest.appsec.WeblogicSecurityFilter";

  private Filter filter;

  public void init(FilterConfig filterConfig) throws ServletException
  {
    super.init(filterConfig);
    String appSecContainerFilter = WEBLOGIC_SECURITY_FILTER;
    try
    {
      String container = ApplicationContext.getContainerName();

      if (NamingProvider.OSGI.equals(container))
        appSecContainerFilter = OSGI_SECURITY_FILTER;
      else
        appSecContainerFilter = DEFAULT_SECURITY_FILTER;

      Class<? extends Filter> filterClass = Class.forName(appSecContainerFilter).asSubclass(Filter.class);
      filter = filterClass.newInstance();
      logger.info("Using access control filter: " + filter.getClass().getName());
      filter.init(filterConfig);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new ServletException(ex.getMessage(), ex);
    }
  }

  public void destroy()
  {
    filter.destroy();
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
  {
    // check whether accessing custom webapp welcome page
    if (request instanceof HttpServletRequest)
    {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      if ("GET".equals(httpRequest.getMethod())
          && ( ("/" + ApplicationContext.getTaskManagerContextRoot()).equals(httpRequest.getContextPath())
                  && !("/" + ApplicationContext.getMdwHubContextRoot()).equals(httpRequest.getContextPath()) )
          && httpRequest.getPathInfo() == null)
      {
        String servletPath = httpRequest.getServletPath();
        if (servletPath != null && !servletPath.startsWith("/facelets"))
        {
          String pkgPath = servletPath.substring(1);
          if (pkgPath.endsWith("/"))
            pkgPath = pkgPath.substring(0, pkgPath.length());
          if (pkgPath.indexOf('/') == -1)  // request for one subpath with no file
          {
            try
            {
              PackageVO pkg = PackageVOCache.getPackageVO(pkgPath);
              if (pkg != null)
              {
                String welcomePage = pkg.getProperty(PropertyNames.MDW_WELCOME_PAGE);
                if (welcomePage == null)
                  welcomePage = "tasks/myTasks.jsf";
                StringBuffer redirect = httpRequest.getRequestURL();
                if (!redirect.toString().endsWith("/"))
                  redirect.append('/');
                redirect.append(welcomePage);
                if (logger.isDebugEnabled())
                  logger.debug("Redirecting to package welcome page: " + welcomePage);
                ((HttpServletResponse)response).sendRedirect(redirect.toString());
                return;
              }
            }
            catch (Exception ex)
            {
              logger.severeException(ex.getMessage(), ex);
            }
          }
        }
      }

      // IE-9 fixes
      String userAgent = httpRequest.getHeader("User-Agent");
      String servletPath = httpRequest.getServletPath();
      if (userAgent != null && (userAgent.contains("MSIE")))
      {
        // append */* to faces css requests due to RichFaces issue
        if ("text/css".equals(httpRequest.getHeader("Accept"))
                && (servletPath != null && (servletPath.startsWith("/faces") || servletPath.endsWith(".jsf"))))
        {
          filter.doFilter(new HttpServletRequestWrapper(httpRequest)
          {
            public String getHeader(String name)
            {
              if ("Accept".equals(name))
                return super.getHeader(name) + ",*/*";
              else
                return super.getHeader(name);
            }
          }, response, chain);
          return;
        }
      }

      if (servletPath != null && ((servletPath.startsWith("/filepanel") || servletPath.startsWith("/configManager") || servletPath.startsWith("/property"))
          || (servletPath.startsWith("/system/filepanel") || servletPath.startsWith("/system/configManager") || servletPath.startsWith("/system/property"))))
      {
        if  ((servletPath.equals("/system/filepanel") || servletPath.equals("/system/filepanel/"))
            && httpRequest.getContextPath().equals("/" + ApplicationContext.getMdwHubContextRoot()))
        {
          // hub filepanel welcome redirect
          ((HttpServletResponse)response).sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/system/filepanel/index.jsf");
          return;
        }
        if  ((servletPath.equals("/system/configManager") || servletPath.equals("/system/configManager/")
              || servletPath.equals("/configManager") || servletPath.equals("/configManager/") || servletPath.equals("/configManager/index.jsf"))
            && (httpRequest.getContextPath().equals("/" + ApplicationContext.getMdwHubContextRoot()) || httpRequest.getContextPath().equals("/MDWWeb")))
        {
          // hub (and MDWWeb) configManager welcome redirect
          ((HttpServletResponse)response).sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/system/configManager/index.jsf");
          return;
        }
        else
        {
          // IE8 compatibility mode for filepanel, configManager, property
          ((HttpServletResponse)response).setHeader("X-UA-Compatible", "IE=8");
        }
      }
      else if (httpRequest.getContextPath().equals("/" + ApplicationContext.getTaskManagerContextRoot()) && servletPath != null && (servletPath.startsWith("/admin")))
      {
        // IE9 compatibility mode for the Admin tab
        ((HttpServletResponse)response).setHeader("X-UA-Compatible", "IE=9");
      }
      else  // honor X-UA-Compatible in config
      {
        if (getIeCompatibilityHeader() != null)
          ((HttpServletResponse)response).setHeader("X-UA-Compatible", getIeCompatibilityHeader());
        else
          ((HttpServletResponse)response).setHeader("X-UA-Compatible", "IE=Edge");  // default to latest
      }
      // cross-origin access for mdw-hub business live view
      // Note PaaS handles this through a Tomcat web.xml configuration as part of the mdw-buildpack 
      if (("/" + ApplicationContext.getServicesContextRoot()).equals(httpRequest.getContextPath()) && !ApplicationContext.isPaaS())
        ((HttpServletResponse)response).setHeader("Access-Control-Allow-Origin", "*");

      // AdminUI JSON service requests to MDWHub may be redirected to login.jsf by ClearTrust.
      if ("/authentication/login.jsf".equals(httpRequest.getServletPath()) &&
          "application/json".equals(httpRequest.getHeader("Accept")) &&
          ("/" + ApplicationContext.getMdwHubContextRoot()).equals(httpRequest.getContextPath()))
      {
        request.getRequestDispatcher("/noAuth" + httpRequest.getServletPath()).forward(request, response);
        return;
      }
    }

    // chain the request to the appropriate appsec filter
    filter.doFilter(request, response, chain);
  }

}