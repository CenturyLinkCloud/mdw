/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.util.UrlPatterns;

public abstract class SecurityFilter implements Filter
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private static final String DEFAULT_FILTER_CONFIG_FILE = "CTAPPFilter.config";
  private static final String DEFAULT_EXCLUDE_URLS_PROP = "excludedURLs";
  private static final String IE_COMPATIBILITY_HEADER = "X-UA-Compatible";

  private FilterConfig filterConfig = null;

  private UrlPatterns authExclusionPatterns = null;
  public UrlPatterns getAuthExclusionPatterns() { return authExclusionPatterns; }

  private boolean allowAnyAuthenticatedUser;
  public boolean isAllowAnyAuthenticatedUser() { return allowAnyAuthenticatedUser; }

  private boolean useMdwAuthFilter;
  public boolean isUseMdwAuthFilter() { return useMdwAuthFilter; }

  private String loginPage;
  public String getLoginPage() { return loginPage; }

  private String ieCompatibilityHeader;
  public String getIeCompatibilityHeader()
  {
    return ieCompatibilityHeader;
  }

  public void init(FilterConfig filterConfig) throws ServletException
  {
    this.filterConfig = filterConfig;
    if (AuthConstants.getOAuthTokenLocation() != null || AuthConstants.isMdwLdapAuth() || ApplicationContext.isDevelopment())
    {
      // does not use CTAPPFilter.config
      useMdwAuthFilter = true;
      loginPage = "/login";
      String authExclusions = AuthConstants.getAuthExclusionPatterns();
      authExclusionPatterns = new UrlPatterns(authExclusions);
      allowAnyAuthenticatedUser = AuthConstants.isAllowAnyAuthenticatedUser();
    }
    else
    {
      String ctConfigFile = this.filterConfig.getInitParameter("CtAppConfigFile");
      if (ctConfigFile == null)
        ctConfigFile = DEFAULT_FILTER_CONFIG_FILE;
      InputStream inStream = null;
      Properties properties = new Properties();
      String excludeUrlsProp = this.filterConfig.getInitParameter("CtExcludeUrlsProperty");
      if (excludeUrlsProp == null)
        excludeUrlsProp = DEFAULT_EXCLUDE_URLS_PROP;

      String authExclusionUrlPatterns = "";
      try
      {
        inStream = FileHelper.openConfigurationFile(ctConfigFile, this.getClass().getClassLoader());
        properties.load(inStream);
        authExclusionUrlPatterns = properties.getProperty(excludeUrlsProp);
      }
      catch (IOException ex)
      {
        logger.severeException(ex.getMessage(), ex);
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

      authExclusionPatterns = new UrlPatterns(authExclusionUrlPatterns);

      useMdwAuthFilter = Boolean.valueOf(properties.getProperty("useLdapFilter"));

      ieCompatibilityHeader = properties.getProperty(IE_COMPATIBILITY_HEADER);

      loginPage = properties.getProperty(MiscConstants.LoginPage);
      if (loginPage == null)
      {
        if (("/" + ApplicationContext.getTaskManagerContextRoot()).equals(filterConfig.getServletContext().getContextPath()))
          loginPage = "/facelets/authentication/ldapLogin.jsf";
        else if (("/" + ApplicationContext.getMdwHubContextRoot()).equals(filterConfig.getServletContext().getContextPath()))
          loginPage = "/login";
        else
          loginPage = "/authentication/ldapLogin.jsf";
      }

      String allowAnyUser = properties.getProperty(MiscConstants.AllowAnyAuthenticatedUser);
      if (allowAnyUser == null)
          allowAnyUser = filterConfig.getInitParameter(MiscConstants.AllowAnyAuthenticatedUser);
      if (allowAnyUser != null && Boolean.parseBoolean(allowAnyUser))
          allowAnyAuthenticatedUser = true;
    }
  }

  public void destroy()
  {
    filterConfig = null;
  }

  protected void showError(HttpServletRequest request, HttpServletResponse response, String message)
  throws IOException, ServletException
  {
    if (!request.getServletPath().endsWith("error.jsf"))
    {
      UIError error = new UIError(message);
      request.getSession().setAttribute("error", error);
      RequestDispatcher dispatcher = request.getRequestDispatcher("/error.jsf");
      dispatcher.forward(request, response);
    }
  }

  protected String getUrlPath(HttpServletRequest request)
  {
    String url = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (url.startsWith(contextPath))
      return url.substring(contextPath.length());
    logger.warn("URL " + url + " does not start with context path " + contextPath);
    return url;
  }

  /**
   * Checks whether the user identified in the HTTP headers differs from that
   * stored in the session.  Currently only compares CUIDs.  Returns false if the
   * header value for loginId is not populated.
   *
   * @param session
   * @param request
   * @return whether the user is different
   */
  protected boolean differentUser(HttpSession session, HttpServletRequest request)
  {
    AuthenticatedUser sessionUser = (AuthenticatedUser) session.getAttribute("authenticatedUser");
    String sessionCuid = sessionUser.getCuid();
    String authCuid = request.getRemoteUser();
    if (authCuid == null || authCuid.trim().length() == 0)
      return false;
    return !authCuid.equals(sessionCuid);
  }

  protected void disallow(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    if (!authExclusionPatterns.match(getUrlPath(request)))
    {
      if (logger.isDebugEnabled())
      {
        Exception ex = new Exception("Not authorized");
        logger.debugException(ex.getMessage(), ex);
      }
      showError(request, response, "Sorry, you are not authorized to use this MDW App.  Please contact an administrator");
    }
  }

  protected void showException(HttpServletRequest request, HttpServletResponse response, Exception ex)
  throws IOException, ServletException
  {
    if (!request.getServletPath().endsWith("error.jsf"))
    {
      logger.severeException(ex.getMessage(), ex);
      UIError error = new UIError(ex);
      if (ex.getCause() != null && ex.getCause().toString().startsWith("java.sql.SQLException")) // database error
      {
        error.setMessage("A database error has occurred.  Please contact your administrator for assistance.");
        error.setException(null);
      }
      request.getSession().setAttribute("error", error);
      RequestDispatcher dispatcher = request.getRequestDispatcher("/error.jsf");
      dispatcher.forward(request, response);
    }
  }

  protected void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    if (!request.getServletPath().endsWith(loginPage))
      response.sendRedirect(request.getContextPath() + loginPage);
  }

  protected class SafeResponseWrapper extends HttpServletResponseWrapper
  {
    private boolean gotWriter;
    private boolean gotOutputStream;

    public SafeResponseWrapper(HttpServletResponse httpServletResponse)
    {
      super(httpServletResponse);
    }

    public void setContentType(String contentType)
    {
      if (isCommitted() || gotWriter || gotOutputStream)
      {
        // skipping response.setContentType(..);");
      }
      else
      {
        super.setContentType(contentType);
      }
    }

    public PrintWriter getWriter() throws IOException
    {
      gotWriter = true;
      return super.getWriter();
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
      gotOutputStream = true;
      return super.getOutputStream();
    }
  }
}
