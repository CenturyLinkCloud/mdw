/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.ArrayDataModel;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.constant.PropertyGroups;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.system.SystemUtil;
import com.centurylink.mdw.common.utilities.ClasspathUtil;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.log4j.Log4JStandardLoggerImpl;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugins.CommonThreadPool;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.script.GroovyExecutor;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.status.GlobalApplicationStatus;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Utility methods for supporting web applications.
 */
public class WebUtil implements Serializable
{
  private static final long serialVersionUID = 1L;

  private static StandardLogger logger = isStandAloneWebApp() ? new Log4JStandardLoggerImpl() : LoggerUtil.getStandardLogger();

  public static final String BUILD_VERSION_FILE = "buildversion.properties";

  public String getAppVersion()
  {
    return getAppName() + " Version: " + ApplicationContext.getApplicationVersion();
  }

  public String getMdwVersion()
  {
    return "MDW " + ApplicationContext.getMdwVersion();
  }

  public String getPlainMdwVersion()
  {
    return ApplicationContext.getMdwVersion();
  }

  public String getAppName()
  {
    if (isStandAloneWebApp())
      return "MDW";
    else
      return ApplicationContext.getApplicationName();
  }

  public static String showRequestParameters(ServletRequest request)
  {
    StringBuffer sb = new StringBuffer();
    Enumeration<?> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements())
    {
      String paramName = (String) paramNames.nextElement();
      String paramValue = request.getParameter(paramName);
      sb.append("  paramName: '" + paramName + "'  paramValue: '" + paramValue + "'\n");
    }
    return sb.toString();
  }

  public static String showRequestHeaders(HttpServletRequest request)
  {
    StringBuffer sb = new StringBuffer();
    Enumeration<?> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements())
    {
      String headerName = (String) headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      sb.append("  headerName: '" + headerName + "'  headerValue: '" + headerValue + "'\n");
    }
    return sb.toString();
  }

  public static String showRequestAttributes(HttpServletRequest request)
  {
    StringBuffer sb = new StringBuffer();
    Enumeration<?> attributes = request.getAttributeNames();
    while (attributes.hasMoreElements())
    {
      String attrName = (String) attributes.nextElement();
      String attrValue = request.getAttribute(attrName).toString();
      sb.append("  attrName: '" + attrName + "'  attrValue: '" + attrValue + "'\n");
    }
    return sb.toString();
  }

  public static String showRequestDetails(HttpServletRequest request)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("HttpServletRequest Information:\n-------------------------------------\n");
    sb.append("Method: " + request.getMethod() + "\n");
    sb.append("URL: " + request.getRequestURL() + "\n");
    sb.append("Protocol: " + request.getProtocol() + "\n");
    sb.append("Servlet path: " + request.getServletPath() + "\n");
    sb.append("Context path: " + request.getContextPath() + "\n");
    sb.append("Path info: " + request.getPathInfo() + "\n");
    sb.append("Path translated: " + request.getPathTranslated() + "\n");
    sb.append("Query string: " + request.getQueryString() + "\n");
    sb.append("Content length: " + request.getContentLength() + "\n");
    sb.append("Content type: " + request.getContentType() + "\n");
    sb.append("Server name: " + request.getServerName() + "\n");
    sb.append("Server port: " + request.getServerPort() + "\n");
    sb.append("Remote user: " + request.getRemoteUser() + "\n");
    sb.append("Remote address: " + request.getRemoteAddr() + "\n");
    sb.append("Remote host: " + request.getRemoteHost() + "\n");
    sb.append("Authorization scheme: " + request.getAuthType() + "\n");
    sb.append("Locale: " + request.getLocale() + "\n");

    sb.append("Request Parameters:\n");
    sb.append(showRequestParameters(request));

    sb.append("Request Headers:\n");
    sb.append(showRequestHeaders(request));

    sb.append("Request Attributes:\n");
    sb.append(showRequestAttributes(request));
    Principal principal = request.getUserPrincipal();
    sb.append("User Principal: " + (principal==null?"null":principal.getName()) + "\n");

    return sb.toString();
  }

  public static String getSessionDetails(HttpSession session)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("HttpSession Information:\n-----------------------------\n");
    sb.append("ID: " + session.getId() + "\n");
    sb.append("New: " + session.isNew() + "\n");
    sb.append("Creation time: " + session.getCreationTime() + "\n");
    sb.append("Last accessed time: " + session.getLastAccessedTime() + "\n");
    sb.append("Max inactive interval: " + session.getMaxInactiveInterval() + "\n");

    sb.append("Session Attributes:\n");
    Enumeration<?> attributes = session.getAttributeNames();
    while (attributes.hasMoreElements())
    {
      String attrName = (String) attributes.nextElement();
      if (!attrName.equals("facelets.ui.DebugOutput"))
      {
        String attrValue = session.getAttribute(attrName).toString();
        sb.append("  attrName: '" + attrName + "'  attrValue: '" + attrValue + "'\n");
      }
    }

    return sb.toString();
  }

  public static String showSystemInformation(ServletContext context)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("System Information:\n------------------------\n");

    sb.append("Server host: " + ApplicationContext.getServerHost() + "\n");
    try {
        sb.append("Server hostname: " + InetAddress.getLocalHost().getHostName() + "\n");
    }
    catch (UnknownHostException ex) {
        sb.append("Server hostname: " + ex);
    }
    sb.append("Server port: " + ApplicationContext.getServerPort() + "\n");
    sb.append("Server name: " + ApplicationContext.getServerHostPort() + "\n");
    sb.append("Runtime env: " + System.getProperty("runtimeEnv") + "\n");
    sb.append("Startup dir: " + System.getProperty("user.dir") + "\n");
    sb.append("App user: " + System.getProperty("user.name") + "\n");
    sb.append("Root path: " + context.getRealPath("/") + "\n");
    sb.append("System time: " + new Date(System.currentTimeMillis()) + "\n");
    sb.append("App startup time: " + ApplicationContext.getStartupTime() + "\n");
    sb.append("Java Version: " + System.getProperty("java.version") + "\n");
    sb.append("Java VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + "\n");
    sb.append("Servlet container: " + context.getServerInfo() + "\n");
    sb.append("Servlet version: " + context.getMajorVersion() + "." + context.getMinorVersion() + "\n");
    sb.append("OS: " + System.getProperty("os.name") + "\n");
    sb.append("OS Version: " + System.getProperty("os.version") + "\n");
    sb.append("OS Arch: " + System.getProperty("os.arch") + "\n");
    Runtime runtime = Runtime.getRuntime();
    sb.append("Max memory: " + runtime.maxMemory()/1024/1024 + " MB\n");
    sb.append("Free memory: " + runtime.freeMemory()/1024/1024 + "MB\n");
    sb.append("Total memory: " + runtime.totalMemory()/1024/1024 + "MB\n");
    sb.append("Available processors: " + runtime.availableProcessors() + "\n");

    sb.append(showPathInformation());

    return sb.toString();
  }

  public static String showDatabaseDetails(DatabaseMetaData metaData) throws SQLException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("MDWDataSource Information:\n-------------------------\n");
    sb.append("DB Product: " + metaData.getDatabaseProductName() + "\n");
    sb.append("DB Version: " + metaData.getDatabaseProductVersion() + "\n");
    sb.append("JDBC Driver: " + metaData.getDriverName() + "\n");
    sb.append("JDBC Driver Version: " + metaData.getDriverVersion() + "\n");
    sb.append("JDBC URL: " + metaData.getURL() + "\n");
    sb.append("DB User Name: " + metaData.getUserName() + "\n");
    return sb.toString();
  }

  public static String showSystemProperties()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("Java VM System Properties:\n--------------------------\n");
    Properties properties = System.getProperties();
    List<String> propStrings = new ArrayList<String>();
    for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext(); )
    {
      String key = (String) iter.next();
      String value = properties.getProperty(key);
      propStrings.add("'" + key + "' = '" + value + "'");
    }
    Collections.sort(propStrings);
    for (String line : propStrings)
      sb.append(line + "\n");

    return sb.toString();
  }

  public static String showEnvironmentVariables()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("Environment Variables:\n----------------------\n");

    List<String> envStrings = new ArrayList<String>();
    for (String envKey : System.getenv().keySet())
      envStrings.add("'" + envKey + "' = '" + System.getenv().get(envKey) + "'");

    Collections.sort(envStrings);
    for (String line : envStrings)
      sb.append(line + "\n");

    return sb.toString();
  }

  public static String showMdwProperties()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("MDW Properties:\n---------------\n");
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      Properties properties = propMgr.getAllProperties();
      List<String> propStrings = new ArrayList<String>();
      for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext(); )
      {
        String key = (String) iter.next();
        if (key.toLowerCase().indexOf("password") == -1)
        {
          String value = properties.getProperty(key);
          String propSource;
          if (propMgr instanceof PropertyManagerDatabase)
            propSource = ((PropertyManagerDatabase)propMgr).getPropertySource(key);
          else
            propSource = propMgr.getClass().getName();

          String propString = "'" + key + "' = '" + value + "'";
          if (PropertyManager.DATABASE.equals(propSource))
            propString += " (" + propSource + ")";

          propStrings.add(propString);
        }
      }
      Collections.sort(propStrings);
      for (String line : propStrings)
        sb.append(line + "\n");
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }

    return sb.toString();
  }

  public static String showOsgiInfo()
  {
    StringBuffer sb = new StringBuffer();
    if (ApplicationContext.isOsgi())
    {
      sb.append("MDW Bundle Activation Times:\n-------------\n");
      try
      {
        BundleContext bundleContext = ApplicationContext.getOsgiBundleContext();
        if (bundleContext != null)
        {
          Map<String,Date> activationTimes = ApplicationContext.getMdwBundleActivationTimes();
          for (String mdwBundle : activationTimes.keySet())
            sb.append(mdwBundle).append(": ").append(StringHelper.formatDate(activationTimes.get(mdwBundle))).append("\n");
          sb.append("\n");
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }

      sb.append("OSGi Bundles:\n-------------\n");
      try
      {
        BundleContext bundleContext = ApplicationContext.getOsgiBundleContext();
        if (bundleContext != null)
        {
          Bundle[] bundles = bundleContext.getBundles();
          if (bundles != null)
          {
            for (Bundle bundle : bundles)
            {
              sb.append(bundle.getSymbolicName());
              sb.append("/").append(bundle.getVersion());
              sb.append(" (").append(bundle.getBundleId()).append(")\n");
              sb.append("   Location: ").append(bundle.getLocation()).append("\n");
              sb.append("   State: ").append(getBundleState(bundle.getState())).append("\n");
            }
          }
        }
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
    }

    return sb.toString();
  }

  private static String getBundleState(int state)
  {
    if (state == Bundle.ACTIVE)
      return "Active";
    else if (state == Bundle.INSTALLED)
      return "Installed";
    else if (state == Bundle.RESOLVED)
      return "Resolved";
    else if (state == Bundle.STARTING)
      return "Starting";
    else if (state == Bundle.STOPPING)
      return "Stopping";
    else if (state == Bundle.UNINSTALLED)
      return "Uninstalled";
    else
      return String.valueOf(state);
  }

  /**
   * Generates a formatted string containing system classpath information.
   *
   * @return the classpath info
   */
  public static String showPathInformation()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("System Classpath:\n");
    String[] cps = ClasspathUtil.parseSystemClasspath();
    for (int i = 0; i < cps.length; i++)
    {
      sb.append("   " + cps[i] + "\n");
    }
    sb.append("Nonexistent Classpath Entries:\n");
    String[] badCps = ClasspathUtil.validateSystemClasspath();
    for (int i = 0; i < badCps.length; i++)
    {
      sb.append("   " + badCps[i] + "\n");
    }
    sb.append("PATH Environment Variable:\n");
    String[] ps = ClasspathUtil.parseSystemPath();
    for (int i = 0; i < ps.length; i++)
    {
      sb.append("   " + ps[i] + "\n");
    }
    return sb.toString();
  }
  /**
   * Generates a formatted string containing all the remote system statuses
   * @return
   */
  private String showRemoteSystemStatus()
  {
    StringBuffer sb = new StringBuffer();
    Map<String, String> statusMap = GlobalApplicationStatus.getInstance().getSystemStatusMap();
    if (!statusMap.isEmpty())
    {
      sb.append("Global Applications Status:\n-------------------------\n");
      for (String app : statusMap.keySet())
      {
        sb.append(app);
        if (GlobalApplicationStatus.ONLINE.equals(statusMap.get(app)))
        {
          sb.append(" is up and running");
        }
        else
        {
          sb.append(" is not reachable");
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  public static String getLocalHost()
  {
    try
    {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException ex)
    {
      ex.printStackTrace();
      return "unknown";
    }
  }

  public String getLocalHostName()
  {
    return getLocalHost();
  }


  public String getJsfRequestDetails()
  {
    try
    {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      Object request = facesContext.getExternalContext().getRequest();
      // no portlet support as yet
      if (request instanceof HttpServletRequest)
        return showRequestDetails((HttpServletRequest)request);
      else
        return "Request is no HttpServletRequest: " + request.getClass().getName();
    }
    catch (Exception ex)
    {
      // don't let runtime exceptions prevent page display
      logger.severeException(ex.getMessage(), ex);
      return ex.getMessage();
    }
  }

  public String getJsfSystemDetails()
  {
    try
    {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      Object context = facesContext.getExternalContext().getContext();
      // no portlet support as yet
      if (context instanceof ServletContext)
        return showSystemInformation(((ServletContext)context));
      else
        return "Context is no ServletContext: " + context.getClass().getName();
    }
    catch (Exception ex)
    {
      // don't let runtime exceptions prevent page display
      logger.severeException(ex.getMessage(), ex);
      return ex.getMessage();
    }
  }

  public String getJsfSessionDetails()
  {
    try
    {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      Object session = facesContext.getExternalContext().getSession(false);
      if (session instanceof HttpSession)
        return getSessionDetails((HttpSession)session);
      else
        return "Session is no HttpSession: " + session.getClass().getName();
    }
    catch (Exception ex)
    {
      // don't let runtime exceptions prevent page display
      logger.severeException(ex.getMessage(), ex);
      return ex.getMessage();
    }
  }

  public String getDatabaseDetails()
  {
    DatabaseAccess dbAccess = null;
    try
    {
      dbAccess = new DatabaseAccess(null);
      Connection conn = dbAccess.openConnection();
      StringBuffer sb = new StringBuffer(showDatabaseDetails(conn.getMetaData()));
      sb.append("Database Time: ").append(new Date(dbAccess.getDatabaseTime())).append("\n");
      return sb.toString();
    }
    catch (Exception ex)
    {
      // don't let runtime exceptions prevent page display
      logger.severeException(ex.getMessage(), ex);
      return ex.getMessage();
    }
    finally
    {
      if (dbAccess != null)
      {
        dbAccess.closeConnection();
      }
    }
  }

  public String getSystemProperties()
  {
    return showSystemProperties();
  }

  public String getEnvironmentVariables()
  {
    return showEnvironmentVariables();
  }

  public String getMdwProperties()
  {
    return showMdwProperties();
  }

  public String getOsgiInfo()
  {
    return showOsgiInfo();
  }

  public String getRemoteSystemStatus(){
    return showRemoteSystemStatus();
  }

  /**
   * Method to show or not show error details on the task manager web when an exception occurs
   */
  public boolean isRenderErrorDetails()
  {
    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    String renderErrorDetailsProp = propMgr.getStringProperty(PropertyNames.WEB_RENDER_ERROR_DETAILS);
    return new Boolean(renderErrorDetailsProp).booleanValue();
  }

  public String refresh() throws UIException
  {
    refreshCache();
    refreshProperties();
    return null;
  }

  public String refreshGlobal() throws UIException
  {
    refreshCacheGlobal();
    refreshPropertiesGlobal();
    return null;
  }

  public String refreshCache() throws UIException
  {
    try
    {
      CacheRegistration.getInstance().refreshCaches();
      final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
      new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            // refresh task manager UI in separate thread
            String reqServer = externalContext.getRequestServerName();
            int reqPort = externalContext.getRequestServerPort();
            String uiRefreshUrl = "http://" + reqServer + ":" + reqPort + "/" + ApplicationContext.getTaskManagerContextRoot() + "/error.jsf?mdwReloadUi=true";
            new HttpHelper(new URL(uiRefreshUrl)).get();
          }
          catch (Throwable t)
          {
            logger.warn("Unable to refresh task manager UI: " + t.getMessage());
            logger.debugException(t.getMessage(), t);
          }
        }
      }).start();

      FacesVariableUtil.addMessage("Application cache reloaded");
      auditLogUserAction(UserActionVO.Action.Refresh, Entity.Cache, new Long(0), "Local Cache");
      return "go_refreshCache";
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public String refreshProperties() throws UIException
  {
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      propMgr.refreshCache();
      LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
      FacesVariableUtil.addMessage("Application properties reloaded");
      auditLogUserAction(UserActionVO.Action.Refresh, Entity.Property, new Long(0), "Local Properties");
      return null;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public List<String> getRefreshUrls()
  {
    List<String> urls = new ArrayList<String>();
    for (String hostPort : ApplicationContext.getCompleteServerList())
    {
      String url = "http://" + hostPort + "/" + ApplicationContext.getServicesContextRoot() + "/Services/REST";
      if (!urls.contains(url))
        urls.add(url);

      // mdwweb may have separate caches
      url = "http://" + hostPort + "/" + ApplicationContext.getMdwWebContextRoot() + "/Services/REST";
      if (!urls.contains(url))
        urls.add(url);
    }
    return urls;
  }

  public String refreshCacheGlobal() throws UIException
  {
    List<String> serviceUrls = getRefreshUrls();
    String msg = "Application caches reloaded globally:\n";
    for (String serviceUrl : serviceUrls)
    {
      ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
      ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
      Action action = actionRequest.addNewAction();
      action.setName("RefreshProcessCache");

      String response = null;
      try
      {
        HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
        response = httpHelper.post(actionRequestDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)));
        MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response);
        MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
        if (statusMessage.getStatusCode() != 0)
          throw new UIException("Error response in refreshProcessCache(): " + statusMessage.getStatusMessage());
        msg += serviceUrl + "\n";
      }
      catch (IOException ex)
      {
        throw new UIException("Unable to connect in refreshCacheGlobal: " + serviceUrl, ex);
      }
      catch (XmlException ex)
      {
        throw new UIException("Unexpected response in refreshProcessCache():\n" + response, ex);
      }
    }
    FacesVariableUtil.addMessage(msg);
    auditLogUserAction(UserActionVO.Action.Refresh, Entity.Cache, new Long(0), "Global Cache");
    return "go_refreshCache";
  }

  public String refreshPropertiesGlobal() throws UIException
  {
    List<String> serviceUrls = getRefreshUrls();
    String msg = "Application properties reloaded globally:\n";
    for (String serviceUrl : serviceUrls)
    {
      ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
      ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
      Action action = actionRequest.addNewAction();
      action.setName("RefreshProcessCache");
      Parameter parameter = action.addNewParameter();
      parameter.setName("RefreshType");
      parameter.setStringValue("Properties");

      String response = null;
      try
      {
        HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
        response = httpHelper.post(actionRequestDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)));
        MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response);
        MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
        if (statusMessage.getStatusCode() != 0)
          throw new UIException("Error response in refreshProperties(): " + statusMessage.getStatusMessage());
        msg += serviceUrl + "\n";
      }
      catch (IOException ex)
      {
        throw new UIException("Unable to connect in refreshProperties: " + serviceUrl, ex);
      }
      catch (XmlException ex)
      {
        throw new UIException("Unexpected response in refreshProperties():\n" + response, ex);
      }
    }
    FacesVariableUtil.addMessage(msg);
    auditLogUserAction(UserActionVO.Action.Refresh, Entity.Property, new Long(0), "Global Properties");
    return null;
  }

  private String classToLocate;
  public String getClassToLocate() { return classToLocate; }
  public void setClassToLocate(String ctl) { classToLocate = ctl; }

  /**
   * @return
   * @throws UIException
   */
  public String locateClass() throws UIException
  {
    String resource = new String(classToLocate);
    if (!resource.startsWith("/"))
      resource = "/" + resource;
    resource = resource.replace('.', '/');
    resource = resource + ".class";

    URL classUrl = WebUtil.class.getResource(resource);

    String msg;
    if (classUrl == null)
      msg = "\nClass not found: [" + classToLocate + "]";
    else
      msg = classUrl.getFile();

    FacesVariableUtil.addMessage(msg);
    return "go_locateClass";
  }

  public ArrayDataModel<WebLink> getFilePanelLinks() throws UIException
  {
    try
    {
      List<WebLink> filePanelLinks = new ArrayList<WebLink>();
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      Properties properties = propMgr.getProperties("MDWFramework.FilePanel");
      if (properties != null && properties.size()>0)
      {
        for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext(); )
        {
          String key = (String) iter.next();
          if (!key.startsWith("@"))
          {
            filePanelLinks.add(new WebLink(key, properties.getProperty(key) + "?user=" + FacesVariableUtil.getCurrentUser().getCuid()));
          }
        }
      } else {
    	  // new style
    	  List<String> serverList = ApplicationContext.getCompleteServerList();
    	  String hubContextRoot = ApplicationContext.getMdwHubContextRoot();
    	  String cuid = FacesVariableUtil.getCurrentUser().getCuid();
    	  for (String server : serverList) {
    		  String filePanelUrl = "http://" + server + "/" + hubContextRoot + "/system/filepanel/index.jsf";
              String label = server;
              int dot = label.indexOf('.');
              int colon = label.indexOf(':');
              if (dot > 0 && colon > dot)
                label = label.substring(0, dot) + label.substring(colon);
    		  filePanelLinks.add(new WebLink(label, filePanelUrl + "?user=" + cuid));
    	  }
      }
      Collections.sort(filePanelLinks);
      return new ArrayDataModel<WebLink>((WebLink[])filePanelLinks.toArray(new WebLink[0]));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public ArrayDataModel<WebLink> getExternalLinks() throws UIException
  {
    try
    {
      List<WebLink> externalLinks = new ArrayList<WebLink>();
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      Properties properties = propMgr.getProperties("MDWFramework.MDWWeb.ExternalLinks");
      if (properties != null && properties.keySet() != null)
      {
        for (Iterator<?> iter = properties.keySet().iterator(); iter.hasNext(); )
        {
          String key = (String) iter.next();
          if (!key.startsWith("@"))
            externalLinks.add(new WebLink(key, properties.getProperty(key)));
        }
      }
      Collections.sort(externalLinks);
      return new ArrayDataModel<WebLink>((WebLink[])externalLinks.toArray(new WebLink[0]));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public boolean isHasExternalLinks() throws UIException
  {
    return ((WebLink[])getExternalLinks().getWrappedData()).length > 0;
  }

  public void launchTaskManager() throws UIException
  {
    try
    {
      FacesVariableUtil.navigate(new URL(ApplicationContext.getTaskManagerUrl()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void launchMdwHub() throws UIException
  {
    try
    {
      FacesVariableUtil.navigate(new URL(ApplicationContext.getMdwHubUrl()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void launchReports() throws UIException
  {
    try
    {
      String url = ApplicationContext.getReportsUrl();
      if (!ApplicationContext.isOsgi() && url.endsWith("/reports"))
        url += "/reportsList.jsf";
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public String getTaskManagerContextRoot()
  {
    return ApplicationContext.getTaskManagerContextRoot();
  }

  public String getMdwHubContextRoot()
  {
    return ApplicationContext.getMdwHubContextRoot();
  }

  public boolean isHasJavaDocs()
  {
    return FacesContext.getCurrentInstance().getExternalContext().getRealPath("/" + ApplicationContext.getMdwHubContextRoot() + "/javadoc") != null;
  }

  public String getWebContextRoot()
  {
    try
    {
      if (isStandAloneWebApp())
        return "";

      String webToolsUrl = ApplicationContext.getMdwWebUrl();
      return webToolsUrl.substring(webToolsUrl.lastIndexOf('/') + 1);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return "Unknown";
    }
  }

  public boolean isIe()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent").indexOf("MSIE") >= 0;
  }

  public boolean isTrident()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    String userAgent = facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent");
    return userAgent.indexOf("Trident") >= 0;
  }

  public boolean isIeOrTrident()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    String userAgent = facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent");
    return userAgent.indexOf("MSIE") >= 0 || userAgent.indexOf("Trident") >= 0;
  }

  public boolean isFirefox()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent").indexOf("Firefox") >= 0;
  }

  public boolean Chrome()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestHeaderMap().get("User-Agent").indexOf("Chrome") >= 0;
  }

  public void launchPropertyManager() throws UIException
  {
    try
    {
    	URL url = new URL(ApplicationContext.getTaskManagerUrl()+
    			"/form.jsf?formName=property_mgr&inputref=com.centurylink.mdw.listener.formaction.PropertyManagerHandler");
    	FacesVariableUtil.navigate(url);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void auditLogUserAction(UserActionVO.Action action, Entity entity, Long entityId, String description)
  {
    try
    {
      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, action, entity, entityId, description);
      userAction.setSource("MDW Web Tools");
      EventManager eventMgr = RemoteLocator.getEventManager();
      eventMgr.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public long getMaxUploadFileSize()
  {
    return PropertyManager.getLongProperty(PropertyGroups.TASK_MANAGER_WEB+"/max.upload.file.size", 200000000);
  }

  public static String getRuntimeEnv()
  {
    return System.getProperty("runtimeEnv");
  }

  public String getStopScript()
  {
    if (isIe())
      return "document.execCommand('Stop');";
    else
      return "window.stop();";
  }

  public Map<String,String> getProperties()
  {
    if (isStandAloneWebApp())
      return new HashMap<String,String>();

    return new HashMap<String,String>()
    {
      private static final long serialVersionUID = 1L;

      @Override
      public String get(Object key)
      {
        if (key == null)
          return null;
        else
          return FacesVariableUtil.getProperty(key.toString());
      }
    };
  }

  public boolean isStandAloneMode()
  {
    return isStandAloneWebApp();
  }

  public static boolean isStandAloneWebApp()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return "true".equals(facesContext.getExternalContext().getInitParameter("com.centurylink.mdw.web.StandAloneWebApp"));
  }

  public static String getAttachmentLocation() throws PropertyException
  {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      String filePath = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "attachments.storage.location");
      if (StringHelper.isEmpty(filePath) || MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equalsIgnoreCase(filePath)) {
          filePath = MiscConstants.DEFAULT_ATTACHMENT_LOCATION;
      } else if (! filePath.endsWith("/")){
          filePath +="/";
      }
      return filePath;
  }

  public boolean isOsgi()
  {
    return ApplicationContext.isOsgi();
  }

  public boolean isDbConfigEnabled()
  {
    return PropertyManager.getInstance().isDbConfigEnabled();
  }

  public boolean isFilePanelAccess()
  {
    String expression = getProperties().get("mdw.filepanel.access.restrict.expression");
    if (expression != null)
    {
      return evaluateGroovy("filePanelAccess", expression);
    }
    else
    {
      if (isOsgi())
        return getUser() != null;  // default restriction
      else
        return true;  // legacy read access for everyone
    }
  }

  public boolean evaluateGroovy(String name, String exp)
  {
    try
    {
      Map<String,Object> bindings = new HashMap<String,Object>();
      bindings.put("webUtil", this);
      GroovyExecutor exec = new GroovyExecutor();
      exec.setName(name);
      return Boolean.parseBoolean(String.valueOf(exec.evaluate(exp, bindings)));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return false;
    }
  }

  public AuthenticatedUser getUser()
  {
    return FacesVariableUtil.getCurrentUser();
  }

  public boolean isProduction()
  {
    return ApplicationContext.isProduction();
  }

  private SystemUtil sysUtil;
  public String doThreadDump()
  {
    if (sysUtil == null)
      sysUtil = new SystemUtil();
    return "go_threadInfo";
  }
  public String getThreadDump()
  {
    if (sysUtil == null)
      return null;
    else
    {
      String dump = sysUtil.getThreadDump();
      sysUtil = null;
          return dump;
    }
  }

  public boolean isMdwThreadPool()
  {
    return ApplicationContext.getThreadPoolProvider() instanceof CommonThreadPool;
  }
  private ThreadPoolProvider threadPool;
  public String doThreadPoolStatus()
  {
    if (threadPool == null)
      threadPool = ApplicationContext.getThreadPoolProvider();
    return "go_threadInfo";
  }
  public String getThreadPoolStatus()
  {
    if (!(threadPool instanceof CommonThreadPool))
      return null;
    else
    {
      String status = ((CommonThreadPool)threadPool).currentStatus();
      threadPool = null;
      return status;
    }
  }
}