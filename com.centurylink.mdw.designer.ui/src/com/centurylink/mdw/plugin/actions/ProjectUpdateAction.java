/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ant.AntBuilder;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProjectUpdateAction implements IObjectActionDelegate
{
  private ISelection selection;
  public ISelection getSelection() { return selection; }

  private Shell shell;

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart)
  {
    shell = targetPart.getSite().getShell();
  }

  /**
   * @see IActionDelegate#run(IAction)
   */
  public void run(final IAction action)
  {
    IProject project = null;
    WorkflowProject workflowProject = null;
    try
    {
      if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IProject)
      {
        project = (IProject) ((IStructuredSelection)selection).getFirstElement();
        workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(project);
      }
      if (workflowProject == null)
      {
        MessageDialog.openError(shell, "MDW Update", "Selection must be a Workflow project.");
        return;
      }

      final ProjectUpdater updater = new ProjectUpdater(workflowProject, MdwPlugin.getSettings());

      if (action.getId().equals("mdw.workflow.updateFrameworkJars"))
      {
        updater.updateFrameworkJars(null);
        ExtensionModulesUpdater modulesUpdater = new ExtensionModulesUpdater(workflowProject);
        modulesUpdater.doUpdate(shell);
      }
      else if (action.getId().equals("mdw.workflow.associateFrameworkSourceCode"))
      {
        ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
        final WorkflowProject wfProject = workflowProject;
        pmDialog.run(true, false, new IRunnableWithProgress()
        {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
          {
            monitor.beginTask("Setting Java source attachment locations", 100);
            monitor.worked(20);
            ProjectConfigurator configurator = new ProjectConfigurator(wfProject, MdwPlugin.getSettings());
            try
            {
              configurator.createFrameworkSourceCodeAssociations(shell, monitor);
            }
            catch (CoreException ex)
            {
              PluginMessages.log(ex);
            }
          }
        });
      }
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }
  }

  /**
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection)
  {
    this.selection = selection;
  }

  protected AntBuilder getAntBuilder(IProject project)
  {
    String tempLoc = MdwPlugin.getSettings().getTempResourceLocation();
    File buildFile = project.getFile(tempLoc + "/build.xml").getRawLocation().toFile();
    AntBuilder antBuilder = new AntBuilder(buildFile);
    antBuilder.getAntProject().setBaseDir(project.getLocation().toFile());
    antBuilder.getAntProject().setProperty("env", "dev");
    return antBuilder;
  }
}
