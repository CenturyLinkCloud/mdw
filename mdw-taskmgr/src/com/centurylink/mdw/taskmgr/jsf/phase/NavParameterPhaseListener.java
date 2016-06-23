/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.taskmgr.ui.NavScopeActionController;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.process.FullProcessInstance;
import com.centurylink.mdw.taskmgr.ui.process.Processes;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.view.MDWPageContent;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Allows unlimited customization/override of default Task Manager web page resources.
 * The basic semantics are: if the URL contains a package name to start the servlet path
 * (eg: http://localhost:7001/MDWTaskManagerWeb/MyPackage), we will attempt to locate the
 * requested page as a workflow asset in the package, which is stored at the session level.
 *
 * The syntax includes sub-pathing as follows:
 * http://localhost:8181/MdwHub/Dons/tasks/myTasks.jsf
 * equates to the myTasks.xhtml file in package Dons.tasks
 * Note: a base package like Dons MUST exist and contain either HubView.xml or TaskView.xml.
 *
 * Also handles processId parameter for web process launch.
 */
public class NavParameterPhaseListener implements PhaseListener
{
  static final String TASKMGR_DEFAULT_WELCOME_PAGE = "/facelets/tasks/myTasks.jsf";
  static final String MDWHUB_DEFAULT_WELCOME_PAGE = "/tasks/myTasks.jsf";
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void beforePhase(PhaseEvent event)
  {
    if (event.getPhaseId().equals(PhaseId.RESTORE_VIEW))
    {
      FacesContext facesContext = event.getFacesContext();
      Map<String, String> requestParams = facesContext.getExternalContext().getRequestParameterMap();

      ExternalContext externalContext = event.getFacesContext().getExternalContext();
      Object request = externalContext.getRequest();

      if (request instanceof HttpServletRequest)
      {
        PackageVO packageVO = null;

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String servletPath = httpRequest.getServletPath();

        // check for packageName parameter
        String packageName = externalContext.getRequestParameterMap().get("packageName");

        if (packageName == null)
          packageVO = FacesVariableUtil.getCurrentPackage();
        else if (packageName.length() == 0)
          packageVO = null;  // purposely resetting?
        else
          packageVO = getPackageVO(packageName);

        NavScopeActionController navScope = TaskListScopeActionController.getInstance();
        boolean isMdwHubRequest = navScope.isMdwHubRequest();

        if (httpRequest.getMethod().equalsIgnoreCase("get"))
        {
          // i've been redirected from MDWHub (admin, etc)
          navScope.setMdwHubSession("true".equals(externalContext.getRequestParameterMap().get("mdwHub")));

          if (isMdwHubRequest)
          {
            // get request for standard task manager sets compatibility mode
            if (!servletPath.equals(navScope.getAdminPage()) && !servletPath.equals(navScope.getMetricsPage()))
              navScope.setCompatibilityMode(true);
          }

          // get request for non-existent resource without package name parameter should initialize the package
          if (facesContext.getViewRoot() == null && servletPath.indexOf('/', 1) >= 0 && packageName == null)
          {
            packageName = servletPath.substring(1, servletPath.indexOf('/', 1));
            if (getPackageVO(packageName) == null) // no custom path specified
            {
              String welcomePagePath = isMdwHubRequest ? MDWHUB_DEFAULT_WELCOME_PAGE : TASKMGR_DEFAULT_WELCOME_PAGE;
              if (packageVO != null && packageVO.getProperty(PropertyNames.MDW_WELCOME_PAGE) != null)
                welcomePagePath = packageVO.getProperty(PropertyNames.MDW_WELCOME_PAGE);

              // initial GET request should clear session package if URL and referer do not contain package name
              if (servletPath.equals(welcomePagePath))
              {
                String referer = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("Referer");
                if (packageVO != null && (referer == null || referer.indexOf("/" + packageVO.getPackageName() + "/") == -1))
                  packageVO = null; // (reverting to standard webapp)
              }
            }
            else if ("a4j".equals(packageName))
            {
              return;  // ignore a4j requests
            }
            else
            {
              packageVO = getPackageVO(packageName);
            }
          }
        }

        String customPage = null;

        if (packageVO == null)
        {
          if (FacesVariableUtil.getCurrentPackage() != null)
          {
            FacesVariableUtil.setSkin(null);  // reset skin
            ListManager.getInstance().invalidate();
            FilterManager.getInstance().invalidate();
            FacesVariableUtil.removeValue("mdwPackage");
          }
          if (isMdwHubRequest)
          {
            // no package specified, but check MDW_HUB for overrides
            customPage = MDWPageContent.getCustomPage(getPackageVO(PackageVO.MDW_HUB), httpRequest.getServletPath().substring(1), httpRequest.getQueryString());
            // note: for user-overridden standard hub pages, see mdw-hub's FaceletCacheFactory
          }
        }
        else
        {
          PackageVO currentPackage = FacesVariableUtil.getCurrentPackage();
          if (currentPackage == null || !packageVO.getPackageName().equals(currentPackage.getPackageName()))
          {
            FacesVariableUtil.setSkin(null);  // reset skin
            ListManager.getInstance().invalidate();
            FilterManager.getInstance().invalidate();
          }
          FacesVariableUtil.setValue("mdwPackage", packageVO);

          if (httpRequest.getServletPath().startsWith("/" + packageVO.getPackageName() + "/"))
          {
            // does the resource exist in the package ?
            String resourcePath = httpRequest.getServletPath().substring(packageVO.getPackageName().length() + 2);

            if (resourcePath.equals("index.jsf"))
            {
              // redirect to welcome page
              String welcomePage = FacesVariableUtil.getProperty(PropertyNames.MDW_WELCOME_PAGE);
              if (welcomePage == null)
                welcomePage = "tasks/myTasks.jsf";
              try
              {
                FacesVariableUtil.navigate(new URL(ApplicationContext.getTaskManagerUrl() + "/" + packageVO.getPackageName() + "/" + welcomePage));
                return;
              }
              catch (IOException ex)
              {
                throw new FacesException(ex.getMessage(), ex);
              }
            }

            customPage = MDWPageContent.getCustomPage(resourcePath, httpRequest.getQueryString());

            if (customPage == null && !isMdwHubRequest)
            {
              // default to regular Task Manager resource (adding /facelets/ to the path)
              facesContext.setViewRoot(facesContext.getApplication().getViewHandler().createView(facesContext, "/facelets/" + resourcePath));
            }
          }
        }

        if (customPage != null)
        {
          try
          {
            FacesVariableUtil.setValue("pageName", URLEncoder.encode(customPage, "UTF-8"));
            facesContext.setViewRoot(facesContext.getApplication().getViewHandler().createView(facesContext, "/page.jsf"));
          }
          catch (UnsupportedEncodingException ex)
          {
            throw new FacesException(ex.getMessage(), ex);
          }
        }
      }

      // handle processId parameter
      if (requestParams.containsKey("processId")) // old-style web-based process launch
      {
        // refresh process cache in case process was newly-added
        Processes processes = (Processes) FacesVariableUtil.getValue("processes");
        processes.refresh();
        // select the process instance
        FullProcessInstance processInstance = (FullProcessInstance) FacesVariableUtil.getValue("newProcessInstance");
        processInstance.setProcessId(new Long(requestParams.get("processId")));
        try
        {
          requestParams.put("tmTabbedPage", "processesTab");
        }
        catch (UnsupportedOperationException ex)
        {
          // fail silently
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
          UIError error = new UIError("Problem locating process ID: " + requestParams.get("processId"), ex);
          FacesVariableUtil.setValue("error", error);
          FacesVariableUtil.navigate("go_error");
        }
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
  }

  private PackageVO getPackageVO(String packageName) throws FacesException
  {
    try
    {
      return PackageVOCache.getPackageVO(packageName);
    }
    catch (CachingException ex)
    {
      throw new FacesException(ex.getMessage(), ex);
    }
  }
}
