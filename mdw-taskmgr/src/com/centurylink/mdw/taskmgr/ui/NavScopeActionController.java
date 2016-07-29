/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import java.net.URL;

import javax.faces.context.FacesContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Handles redirect navigation for remote/central TaskManager.
 */
public class NavScopeActionController
{

  /**
   * Means that the old TaskManager is being accessed versus MDWHub.
   */
  private boolean compatibilityMode;
  public boolean isCompatibilityMode() { return compatibilityMode; }
  public void setCompatibilityMode(boolean compatibility) { this.compatibilityMode = compatibility; }

  private String adminPage = "/admin/groups.jsf";
  public String getAdminPage() { return adminPage; }
  public void setAdminPage(String page) { this.adminPage = page; }

  private String metricsPage = "/metrics/metricsBirt.jsf";
  public String getMetricsPage() { return metricsPage; }
  public void setMetricsPage(String page) { this.metricsPage = page; }

  private String activeTabParam;
  public String getActiveTabParam() { return activeTabParam; }
  public void setActiveTabParam(String atp) { this.activeTabParam = atp; }

  /**
   * This is only a global default.  The more dynamic way is to include the param
   * name and value directly in the path.
   */
  private String activeMenuItemParam;
  public String getActiveMenuItemParam() { return activeMenuItemParam; }
  public void setActiveMenuItemParam(String amip) { this.activeMenuItemParam = amip; }

  public String goMetrics() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary(metricsPage);
      return null;
    }
    else
    {
      if (ApplicationContext.isOsgi())
      {
        boolean reportsInstalled = false;
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
                if ("com.centurylink.mdw.reports".equals(bundle.getSymbolicName()))
                {
                  reportsInstalled = true;
                  break;
                }
              }
              if (!reportsInstalled) {
                redirectToMdwHub("/reports/notInstalled.jsf", "metricsTab");
                return null;
              }
            }
          }
        }
        catch (Exception ex)
        {
          throw new UIException(ex.getMessage(), ex);
        }
      }

      try
      {
        String url = ApplicationContext.getReportsUrl();
        FacesVariableUtil.navigate(new URL(url));
        return null;
      }
      catch (Exception ex)
      {
        throw new UIException(ex.getMessage(), ex);
      }
    }
  }

  public String goOrders() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary("/orders/orderList.jsf", "ordersTab");
      return null;
    }
    else if (isMdwHubRequest())
    {
      return "go_orders";
    }
    else
    {
      redirectToMdwHub("/orders/orderList.jsf");
      return null;
    }
  }

  public String goProcesses() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary("/process/instanceList.jsf", "processesTab");
      return null;
    }
    else if (isMdwHubRequest())
    {
      return "go_processes";
    }
    else
    {
      redirectToMdwHub("/process/instanceList.jsf");
      return null;
    }
  }

  public String goBlv() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary("/process/blv.jsf", "blvTab");
      return null;
    }
    else if (isMdwHubRequest())
    {
      return "go_blv";
    }
    else
    {
      redirectToMdwHub("/process/blv.jsf");
      return null;
    }
  }

  /**
   * for value expressions
   */
  public String getGoBlv()
  {
    return "go_blv";
  }


  /**
   * for value expressions
   */
  public String getGoOrders()
  {
    return "go_orders";
  }

  /**
   * Returns non-null if new Admin UI is used.
   */
  public String getAdminUrl()
  {
    return ApplicationContext.getAdminUrl();
  }

  public String getTasksUi()
  {
    return ApplicationContext.getTasksUi();
  }

  /**
   * Used for old TaskManager admin tab
   */
  public String goAdmin() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary(adminPage);
      return null;
    }
    else if (isMdwHubRequest() || isMdwReportsRequest())
    {
      redirectToCompatibilityWeb(adminPage + "?mdwHub=true");
      return null;
    }
    else
    {
      return "go_admin";
    }
  }

  public String getGoAdmin()
  {
    return "go_admin";
  }

  public String goRequests() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      redirectToRemoteSummary("/requests/requestsList.jsf", "requestsTab");
      return null;
    }
    else if (isMdwHubRequest())
    {
      return "go_requests";
    }
    else
    {
      redirectToMdwHub("/requests/requestsList.jsf");
      return null;
    }
  }

  /**
   * for value expressions
   */
  public String getGoRequests()
  {
    return "go_requests";
  }

  public String getGoProcesses()
  {
    return "go_processes";
  }

  public String goSystem() throws UIException
  {
    if (isMdwHubRequest())
    {
      return "go_system";
    }
    else
    {
      redirectToMdwHub("/system/systemInformation.jsf");
      return null;
    }
  }

  /**
   * for value expressions
   */
  public String getGoSystem()
  {
    return "go_system";
  }

  protected void redirectToRemoteSummary(String path) throws UIException
  {
    redirectToRemoteSummary(path, null, null);
  }

  protected void redirectToRemoteSummary(String path, String activeTab) throws UIException
  {
    redirectToRemoteSummary(path, activeTab, null);
  }

  protected void redirect(String url) throws UIException
  {
    try
    {
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * @param path - should begin with '/'
   * @param activeTab - the tabId to display as active
   * @param activeMenuItem - the menuItemId to display as active
   */
  protected void redirectToRemoteSummary(String path, String activeTab, String activeMenuItem) throws UIException
  {
    try
    {
      String pathWithParams = path.startsWith("/") ? path : "/" + path;
      if (activeTab != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeTabParam + "=" + activeTab;
      }
      if (activeMenuItem != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeMenuItemParam + "=" + activeMenuItem;
      }

      String url = TaskManagerAccess.getInstance().getSummaryTaskManagerUrl() + pathWithParams;
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  protected void redirectToCompatibilityWeb(String path) throws UIException
  {
    redirectToCompatibilityWeb(path, null, null);
  }

  protected void redirectToCompatibilityWeb(String path, String activeTab) throws UIException
  {
    redirectToCompatibilityWeb(path, activeTab, null);
  }

  protected void redirectToCompatibilityWeb(String path, String activeTab, String activeMenuItem) throws UIException
  {
    try
    {
      String pathWithParams = path.startsWith("/") ? path : "/" + path;
      if (activeTab != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeTabParam + "=" + activeTab;
      }
      if (activeMenuItem != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeMenuItemParam + "=" + activeMenuItem;
      }

      String url = ApplicationContext.getTaskManagerUrl() + pathWithParams;
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  protected void redirectToMdwHub(String path) throws UIException
  {
    redirectToMdwHub(path, null, null);
  }

  protected void redirectToMdwHub(String path, String activeTab) throws UIException
  {
    redirectToMdwHub(path, activeTab, null);
  }

  protected void redirectToMdwHub(String path, String activeTab, String activeMenuItem) throws UIException
  {
    try
    {
      String pathWithParams = path.startsWith("/") ? path : "/" + path;
      if (pathWithParams.startsWith("/facelets/"))
        pathWithParams = pathWithParams.substring(9);
      if (activeTab != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeTabParam + "=" + activeTab;
      }
      if (activeMenuItem != null)
      {
        pathWithParams += (path.indexOf('?') > 0 ? "&" : "?") + activeMenuItemParam + "=" + activeMenuItem;
      }

      String url = ApplicationContext.getMdwHubUrl() + pathWithParams;
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  // for a request
  private Boolean mdwHubRequest;
  public boolean isMdwHubRequest()
  {
    if (mdwHubRequest == null)
    {
      String ctxRoot = FacesVariableUtil.getRequestContextRoot();
      mdwHubRequest = ctxRoot.equals("/" + ApplicationContext.getMdwHubContextRoot());
    }
    return mdwHubRequest;
  }

  private Boolean mdwReportsRequest;
  public boolean isMdwReportsRequest()
  {
    if (mdwReportsRequest == null)
    {
      String ctxRoot = FacesVariableUtil.getRequestContextRoot();
      mdwReportsRequest = ctxRoot.equals("/" + ApplicationContext.getReportsContextRoot());
    }
    return mdwReportsRequest || isReportsNotInstalledRequest();
  }

  public boolean isReportsNotInstalledRequest()
  {
    return "/reports/notInstalled.xhtml".equals(FacesContext.getCurrentInstance().getViewRoot().getViewId());
  }

  // for a session (in TaskManager webapp)
  private boolean mdwHubSession;
  public boolean isMdwHubSession() { return mdwHubSession; }
  public void setMdwHubSession(boolean b) { this.mdwHubSession = b; }
}
