/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import java.math.BigInteger;
import java.util.List;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DiscoveryException;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ProjectImporter;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class MdwWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor
{
  public static final String MDW_HOST = "mdw.host";
  public static final String MDW_PORT = "mdw.port";
  public static final String MDW_CONTEXT_ROOT = "mdw.context.root";
  public static final String MDW_PRESELECT_TYPE = "mdw.preselect.type";
  public static final String MDW_PRESELECT_ID = "mdw.preselect.id";
  public static final String PRESELECT_PROCESS_INSTANCE = "processInstance";
  public static final String MDW_START_MINIMIZED = "mdw.start.minimized";

  // startup params
  private String startMinimized;
  private String mdwHost;
  private String mdwPort;
  private String mdwContextRoot;
  private String preselectType;
  private String preselectId;

  public MdwWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
  {
    super(configurer);
  }

  public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer)
  {
    return new MdwActionBarAdvisor(configurer);
  }

  public void preWindowOpen()
  {
    IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

    // check system params for designated startup env
    startMinimized = System.getProperty(MDW_START_MINIMIZED);
    mdwHost = System.getProperty(MDW_HOST);
    mdwPort = System.getProperty(MDW_PORT);
    mdwContextRoot = System.getProperty(MDW_CONTEXT_ROOT);
    preselectType = System.getProperty(MDW_PRESELECT_TYPE);
    preselectId = System.getProperty(MDW_PRESELECT_ID);

    PluginMessages.log("Startup Params:\n---------------");
    PluginMessages.log("  " + MDW_START_MINIMIZED + "='" + startMinimized + "'");
    PluginMessages.log("  " + MDW_HOST + "='" + mdwHost + "'");
    PluginMessages.log("  " + MDW_PORT + "='" + mdwPort + "'");
    PluginMessages.log("  " + MDW_CONTEXT_ROOT + "='" + mdwContextRoot + "'");
    PluginMessages.log("  " + MDW_PRESELECT_TYPE + "='" + preselectType + "'");
    PluginMessages.log("  " + MDW_PRESELECT_ID + "='" + preselectId + "'");

    // TODO figure out how to support this option to minimize to system tray on startup
    if (Boolean.parseBoolean(startMinimized))
      configurer.setShellStyle(SWT.MIN);

    configurer.setInitialSize(new Point(1000, 750));
    configurer.setShowCoolBar(true);
    configurer.setShowStatusLine(false);
  }

  private WorkflowProject projectToImport;
  private DiscoveryException discoveryException;

  public void postWindowOpen()
  {
    IWorkbenchPage activePage = Activator.getActivePage();

//  check for updates
//    IHandlerService handlerService = (IHandlerService) activePage.getWorkbenchWindow().getService(IHandlerService.class);
//    try
//    {
//      Object result = handlerService.executeCommand("org.eclipse.equinox.p2.ui.sdk.update", null);
//      System.out.println("result: " + result);
//      if (result != null)
//        System.out.println("result class: " + result.getClass().getName());
//    }
//    catch (Exception ex)
//    {
//      ex.printStackTrace();
//    }

    PluginMessages.log("MDW workbench startup...");

    if (activePage != null)
    {
      activePage.hideActionSet("com.centurylink.mdw.plugin.actionset.tools");
      activePage.hideActionSet("com.centurylink.mdw.plugin.actionset.dev");
      activePage.hideActionSet("com.centurylink.mdw.plugin.actionset.designerClassic");
      activePage.hideActionSet("org.eclipse.ui.edit.text.actionSet.navigation");
      activePage.hideActionSet("org.eclipse.ui.edit.text.actionSet.annotationNavigation");
      activePage.hideActionSet("org.eclipse.ui.externaltools.ExternalToolsSet");
      activePage.showActionSet("org.eclipse.search.menu");

      // make sure the process explorer view is visible
      try
      {
        ProcessExplorerView processExplorerView = (ProcessExplorerView) activePage.showView("mdw.views.designer.processes");

        if (mdwHost != null && mdwPort != null)
        {
          final Shell shell = activePage.getActivePart().getSite().getShell();
          BusyIndicator.showWhile(shell.getDisplay(), new Runnable()
          {
            public void run()
            {
              try
              {
                discoveryException = null;
                projectToImport = getWorkflowProject(mdwHost, mdwPort, mdwContextRoot);
                if (projectToImport == null)
                  throw new DiscoveryException("Unable to discover workflow app at: " + mdwHost + ":" + mdwPort);
              }
              catch (DiscoveryException ex)
              {
                discoveryException = ex;
              }
            }
          });

          if (discoveryException != null)
            throw discoveryException;

          WorkflowProject existing = WorkflowProjectManager.getInstance().getRemoteWorkflowProject(projectToImport.getName());
          if (existing != null)
            WorkflowProjectManager.getInstance().deleteProject(existing);

          ProgressMonitorDialog progMonDlg = new ProgressMonitorDialog(shell);
          ProjectInflator projectInflator = new ProjectInflator(projectToImport, null);
          projectInflator.inflateRemoteProject(progMonDlg);
          ProjectImporter projectImporter = new ProjectImporter(projectToImport);
          projectImporter.doImport();
          processExplorerView.handleRefresh();

          // handle preselected entity
          if (preselectType != null && preselectType.trim().length() > 0 && preselectId != null && preselectId.trim().length() > 0)
          {
            if (!preselectType.equals(PRESELECT_PROCESS_INSTANCE))
              throw new UnsupportedOperationException("Unsupported preselect type: " + preselectType);

            BusyIndicator.showWhile(shell.getDisplay(), new Runnable()
            {
              public void run()
              {
                // open the process instance
                IWorkbenchPage page = MdwPlugin.getActivePage();
                try
                {
                  WorkflowProcess instance = getProcessInstance(new Long(preselectId));
                  page.openEditor(instance, "mdw.editors.process");
                  page.showView("org.eclipse.ui.views.PropertySheet");
                }
                catch (PartInitException ex)
                {
                  PluginMessages.uiError(ex, "Open Process Instance", projectToImport);
                }
              }
            });
          }
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Initialize Workspace");
      }
    }
  }

  private WorkflowProcess getProcessInstance(Long processInstanceId)
  {
    PluginDataAccess dataAccess = projectToImport.getDataAccess();
    ProcessInstanceVO processInstanceInfo = dataAccess.getProcessInstance(processInstanceId);

    ProcessVO processVO = new ProcessVO();
    processVO.setProcessId(processInstanceInfo.getProcessId());
    processVO.setProcessName(processInstanceInfo.getProcessName());
    WorkflowProcess instanceVersion = new WorkflowProcess(projectToImport, processVO);
    instanceVersion.setProcessInstance(processInstanceInfo);
    return instanceVersion;
  }

  private WorkflowProject getWorkflowProject(String host, String port, String contextRoot) throws DiscoveryException
  {
    BigInteger portInt = new BigInteger(port);
    WorkflowApplication matchingWorkflowApp = null;
    WorkflowEnvironment matchingWorkflowEnv = null;
    boolean isLocalhost = "localhost".equals(host);
    List<WorkflowApplication> workflowApps = WorkflowProjectManager.getInstance().discoverWorkflowApps();
    for (WorkflowApplication workflowApp : workflowApps)
    {
      if (isLocalhost)
      {
        // assume the first matching environment entry contains the right info except host and port
        if (workflowApp.getWebContextRoot().equals(contextRoot)
            || (workflowApp.getServicesContextRoot() != null && workflowApp.getServicesContextRoot().equals(contextRoot)))
        {
          matchingWorkflowApp = workflowApp;
          matchingWorkflowEnv = workflowApp.getEnvironmentList().get(0);
          ManagedNode server = matchingWorkflowEnv.getManagedServerList().get(0);
          server.setHost(host);
          server.setPort(new BigInteger(port));
          break;
        }
      }
      else
      {
        for (WorkflowEnvironment workflowEnv : workflowApp.getEnvironmentList())
        {
          for (ManagedNode server : workflowEnv.getManagedServerList())
          {
            if (server.getHost().equals(host) && server.getPort().equals(portInt))
            {
              if (matchingWorkflowEnv == null)
              {
                matchingWorkflowEnv = workflowEnv;
                matchingWorkflowApp = workflowApp;
              }
              else
              {
                // context root is only used to break a tie
                if (workflowApp.getWebContextRoot().equals(contextRoot)
                    || (workflowApp.getServicesContextRoot() != null && workflowApp.getServicesContextRoot().equals(contextRoot)))
                {
                  matchingWorkflowEnv = workflowEnv;
                  matchingWorkflowApp = workflowApp;
                }
              }
            }
          }
        }
      }
    }
    if (matchingWorkflowApp == null || matchingWorkflowEnv == null)
      return null;

    return new WorkflowProject(matchingWorkflowApp, matchingWorkflowEnv);
  }
}
