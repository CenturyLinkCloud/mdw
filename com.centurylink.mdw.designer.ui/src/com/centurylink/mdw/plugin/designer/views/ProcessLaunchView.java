/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.browser.WebBrowserView;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

@SuppressWarnings("restriction")
public class ProcessLaunchView extends WebBrowserView
{
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }
  public void setProcess(WorkflowProcess process)
  {
    this.process = process;
    WorkflowProject workflowProject = process.getProject();
    String procLaunchPath = "/facelets/process/plainLaunch.jsf";
    String processIdParam = "processId=" + process.getId();
    setURL(workflowProject.getTaskManagerUrl() + procLaunchPath + "?" + processIdParam);
  }

  @Override
  public void createPartControl(Composite parent)
  {
    super.createPartControl(parent);
  }
}
