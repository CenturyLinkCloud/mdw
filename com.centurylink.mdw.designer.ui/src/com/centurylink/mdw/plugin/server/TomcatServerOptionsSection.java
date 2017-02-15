/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IModule;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class TomcatServerOptionsSection extends MdwServerOptionsSection
{
  private Button forceStopCheckbox;

  @Override
  public void createSection(Composite parent)
  {
    super.createSection(parent);

    // force stop
    forceStopCheckbox = new Button(composite, SWT.CHECK);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    gd.verticalIndent = 5;
    forceStopCheckbox.setLayoutData(gd);
    forceStopCheckbox.setText("Force Terminate when Stopping");
    boolean force = server.getAttribute(FORCE_STOP, false);
    forceStopCheckbox.setSelection(force);
    forceStopCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean force = forceStopCheckbox.getSelection();
        IUndoableOperation cmd = new ServerAttributeSetterCommand(server, FORCE_STOP, force, false);
        execute(cmd);
      }
    });

  }

  protected String getDescription()
  {
    if (server != null && server.getRuntime() != null)
    {
      String tomcatLoc = String.valueOf(server.getRuntime().getLocation());
      WorkflowProject workflowProject = getProject();
      String desc = "Tomcat Location: " + new File(tomcatLoc) + "\nMDW War: ";
      if (workflowProject != null)
        desc += new File(workflowProject.getDeployFolder().getLocation().toPortableString() + "/webapps/mdw.war");
      return desc;
    }
    else
    {
      return "MDW Tomcat Launch Settings";
    }
  }

  @Override
  protected String getDebugModeLabel()
  {
    return "Allow Remote Debugging";
  }

  @Override
  protected boolean getDefaultDebugMode()
  {
    return false;
  }

  protected String getDefaultJavaOptions()
  {
    WorkflowProject project = getProject();
    String runtimeEnv = project.checkRequiredVersion(6, 0) ? "-Dmdw.runtime.env=dev" : "-DruntimeEnv=dev";
    return runtimeEnv + "\n"
      + "-Dmdw.config.location=" + (project == null ? "null" : project.getProjectDir()) + System.getProperty("file.separator") + "config\n"
      + "-Xms512m -Xmx1024m -XX:MaxPermSize=256m";
  }

  private WorkflowProject getProject()
  {
    if (server != null && server.getModules().length > 0)
    {
      for (IModule module : server.getModules())
      {
        WorkflowProject project = WorkflowProjectManager.getInstance().getWorkflowProject(module.getProject());
        if (project != null)
          return project;
      }
    }
    return null;
  }

}
