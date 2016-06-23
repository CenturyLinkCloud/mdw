/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

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
import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

/**
 * Servlet filter for populating the session's "authenticatedUser" with the values passed from
 * ClearTrust in HTTP Headers.
 */
public class CommonSecurityFilter implements Filter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Filter filter;
  private boolean timed;
  private boolean showHeaders;
  private boolean showParameters;

  public void init(FilterConfig filterConfig) throws ServletException
  {
    if (AuthConstants.getOAuthTokenLocation() != null || AuthConstants.isMdwLdapAuth())
    {
      // does not use CTAPPFilter.config
      filter = new MdwAuthFilter();
      timed = Boolean.parseBoolean(System.getProperty("mdw.hub.timed.responses"));
      showHeaders = Boolean.parseBoolean(System.getProperty("mdw.hub.show.headers"));
      showParameters = Boolean.parseBoolean(System.getProperty("mdw.hub.show.parameters"));
    }
    else
    {
      String ctConfigFile = filterConfig.getInitParameter("CtAppConfigFile");
      InputStream inStream = null;
      Properties properties = new Properties();
      try
      {
        inStream = FileHelper.openConfigurationFile(ctConfigFile, this.getClass().getClassLoader());
        properties.load(inStream);
        String useLdap = properties.getProperty("useLdapFilter");
        if ("true".equalsIgnoreCase(useLdap))
          filter = new MdwAuthFilter();
        else
          filter = new MDWClearTrustFilter();

        timed = Boolean.parseBoolean(properties.getProperty("timedResponses"));
        showHeaders = Boolean.parseBoolean(properties.getProperty("showHeaders"));
        showParameters = Boolean.parseBoolean(properties.getProperty("showParameters"));
      }
      catch (Exception ex)
      {
        logger.severeException("Cannot instantiate security filter class - use Clear Trust", ex);
        filter = new MDWClearTrustFilter();
      }
      finally
      {
        if (inStream != null)
        {
          try
          {
            inStream.close();
          }
          catch (IOException e)
          {
            logger.severeException(e.getMessage(), e);
          }
        }
      }
    }

    logger.info("Using security filter " + filter.getClass().getName());
    filter.init(filterConfig);
  }

  public void destroy()
  {
    filter.destroy();
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
  {
    long start = 0;
    HttpServletRequest httpReq = (HttpServletRequest) req;
    HttpServletResponse httpResp = (HttpServletResponse) resp;

    if (logger.isInfoEnabled())
    {
      if (showHeaders && logger.isInfoEnabled())
      {
        logger.info("HTTP Request Headers (" + httpReq.getRequestURL() + "):");
        Enumeration<?> e = httpReq.getHeaderNames();
        while (e.hasMoreElements())
        {
          String name = e.nextElement().toString();
          logger.info("  " + name + "=" + httpReq.getHeader(name));
        }
      }
      if (showParameters && logger.isInfoEnabled())
      {
        logger.info("HTTP Request Parameters:");
        Enumeration<?> e = httpReq.getParameterNames();
        while (e.hasMoreElements())
        {
          String name = e.nextElement().toString();
          logger.info("  " + name + "=" + httpReq.getParameter(name));
        }
      }
    }

    if (timed)
      start = System.currentTimeMillis();


    if ("/tasks".equals(httpReq.getServletPath()))
      httpResp.sendRedirect(ApplicationContext.getTaskManagerUrl());
    else if ("/tools".equals(httpReq.getServletPath()))
      httpResp.sendRedirect(ApplicationContext.getMdwWebUrl());
    else
    {
      if (showHeaders && logger.isInfoEnabled())
      {
        filter.doFilter(httpReq, new HttpServletResponseWrapper(httpResp)
        {
          public void addHeader(String name, String value)
          {
            logger.info("HTTP Response Header: " + name + "=" + value);
            super.addHeader(name, value);
          }
          public void addDateHeader(String name, long value)
          {
            logger.info("HTTP Response Date Header: " + name + "=" + new Date(value));
            super.addDateHeader(name, value);
          }
          public void addIntHeader(String name, int value)
          {
            logger.info("HTTP Response Int Header: " + name + "=" + value);
            super.addIntHeader(name, value);
          }
          public void setHeader(String name, String value)
          {
            logger.info("HTTP Response Header: " + name + "=" + value);
            super.setHeader(name, value);
          }
          public void setDateHeader(String name, long value)
          {
            logger.info("HTTP Response Date Header: " + name + "=" + new Date(value));
            super.setDateHeader(name, value);
          }
          public void setIntHeader(String name, int value)
          {
            logger.info("HTTP Response Int Header: " + name + "=" + value);
            super.setIntHeader(name, value);
          }
        }, chain);
      }
      else
      {
        filter.doFilter(req, resp, chain);
      }
    }

    if (timed && logger.isInfoEnabled())
    {
      String relUrl = httpReq.getServletPath() + (httpReq.getPathInfo() == null ? "" : httpReq.getPathInfo()) + (httpReq.getQueryString() == null ? "" : "?" + httpReq.getQueryString());
      if (relUrl.endsWith("_.jsf") || relUrl.endsWith("gif.jsf") || relUrl.endsWith("js.jsf"))  // richfaces resources
      {
        if (logger.isDebugEnabled())
          logger.debug(relUrl + ": " + (System.currentTimeMillis() - start) + " ms");
      }
      else
      {
        logger.info(relUrl + ": " + (System.currentTimeMillis() - start) + " ms");
      }
    }
  }

}