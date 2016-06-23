/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
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

/**
 * Not used in MDW proper.  Temporary redirect impl for ConfigManager access through old URLs.
 * Do not add dependencies on any other MDW classes.
 */
public class DiscoveryRedirectFilter implements Filter
{

  public void init(FilterConfig filterConfig) throws ServletException
  {
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
  {

    if (req instanceof HttpServletRequest)
    {
      HttpServletRequest request = (HttpServletRequest) req;
      String servletPath = request.getServletPath();
      if ("/configManager".equals(servletPath) || "/configManager/".equals(servletPath) || "/configManager/index.jsf".equals(servletPath))
      {
        // hub (and MDWWeb) configManager welcome redirect
        ((HttpServletResponse)resp).sendRedirect("/mdw/system/configManager/index.jsf");
        return;
      }
    }

    chain.doFilter(req, resp);
  }

  public void destroy()
  {
  }

}
