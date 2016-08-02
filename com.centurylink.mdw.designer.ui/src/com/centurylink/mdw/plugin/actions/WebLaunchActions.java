/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class WebLaunchActions
{
  public enum WebApp
  {
    TaskManager,
    WebTools,
    Reports,
    MdwHub
  }

  public static WebLaunchAction getLaunchAction(WorkflowProject project, WebApp webApp)
  {
    return getInstance().findLaunchAction(project, webApp);
  }

  private static WebLaunchActions instance;
  public static WebLaunchActions getInstance()
  {
    if (instance == null)
      instance = new WebLaunchActions();
    return instance;
  }

  public WebLaunchAction findLaunchAction(WorkflowProject project, WebApp webApp)
  {
    switch (webApp)
    {
      case TaskManager:
        return new WebLaunchAction(WebApp.TaskManager, "Task Manager", "taskmgr.gif", project == null ? null : project.getTaskManagerMyTasksPath());
      case WebTools:
        return new WebLaunchAction(WebApp.WebTools, "Web Tools", "webtools.gif", project == null ? null : project.getSystemInfoPath());
      case Reports:
        return new WebLaunchAction(WebApp.Reports, "Reports List", "report.gif", project == null ? null : project.getReportsListPath());
      case MdwHub:
        return new WebLaunchAction(WebApp.MdwHub, "MDW Hub", "hub.gif", project == null ? null : project.getMyTasksPath());
      default:
        return null;
    }
  }

  public class WebLaunchAction
  {
    public WebLaunchAction(WebApp webApp, String label, String icon, String urlPath)
    {
      this.webApp = webApp;
      this.label = label;
      this.icon = icon;
      this.imageDescriptor = MdwPlugin.getImageDescriptor("icons/" + icon);
      this.iconImage = this.imageDescriptor.createImage();
      this.urlPath = urlPath;
    }

    private WebApp webApp;
    public WebApp getWebApp() { return webApp; }

    private String label;
    public String getLabel() { return label; }

    private String icon;
    public String getIcon() { return icon; }

    private ImageDescriptor imageDescriptor;
    public ImageDescriptor getImageDescriptor() { return imageDescriptor; }

    private Image iconImage;
    public Image getIconImage() { return iconImage; }

    private String urlPath = "";
    public String getUrlPath() { return urlPath; }

    public void launch(WorkflowProject project)
    {
      String baseUrl = null;
      if (webApp == WebApp.TaskManager)
        baseUrl = project.getTaskManagerUrl();
      else if (webApp == WebApp.WebTools)
        baseUrl = project.getWebToolsUserAccessUrl();
      else if (webApp == WebApp.Reports)
        baseUrl = project.getReportsUrl();
      else if (webApp == WebApp.MdwHub)
        baseUrl = project.getMdwHubUrl();
      Program.launch(baseUrl + addSessionParams(urlPath));
    }

    public void launch(WorkflowPackage packageVersion)
    {
      WorkflowProject project = packageVersion.getProject();
      if (webApp == WebApp.TaskManager)
        Program.launch(project.getTaskManagerUrl() + addSessionParams(packageVersion.getWelcomePagePath()));
      else if (webApp == WebApp.MdwHub)
        Program.launch(project.getMdwHubUrl() + addSessionParams(packageVersion.getWelcomePagePath()));
      else
        Program.launch(project.getWebToolsUserAccessUrl() + addSessionParams(urlPath));
    }

    public void launch(WorkflowProject project, String urlPath)
    {
      String baseUrl = null;
      if (webApp == WebApp.TaskManager)
        baseUrl = project.getTaskManagerUrl();
      else if (webApp == WebApp.WebTools)
        baseUrl = project.getWebToolsUserAccessUrl();
      else if (webApp == WebApp.Reports)
        baseUrl = project.getReportsUrl();
      else if (webApp == WebApp.MdwHub)
        baseUrl = project.getMdwHubUrl();
      Program.launch(baseUrl + addSessionParams(urlPath));
    }

    public void launch(WorkflowPackage packageVersion, String urlPath)
    {
      if (urlPath == null)
      {
        launch(packageVersion);
        return;
      }

      WorkflowProject project = packageVersion.getProject();
      if (webApp == WebApp.TaskManager)
        Program.launch(project.getTaskManagerUrl() + "/" + packageVersion.getName() + addSessionParams(urlPath));
      else if (webApp == WebApp.MdwHub)
        Program.launch(project.getMdwHubUrl() + "/" + packageVersion.getName() + addSessionParams(urlPath));
      else
        Program.launch(project.getWebToolsUserAccessUrl() + addSessionParams(urlPath));
    }
  }

  private String addSessionParams(String urlPath)
  {
    String newPath = urlPath;
    if (!newPath.startsWith("/"))
      newPath = "/" + newPath;

    return newPath;
  }
}
