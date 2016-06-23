/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class WorkflowFacetInstallDelegate implements IDelegate
{
  public void execute(IProject project, IProjectFacetVersion fv, Object config, IProgressMonitor monitor) throws CoreException
  {
    WorkflowProject workflowProject = (WorkflowProject) config;
    if (workflowProject.isCloudProject())
    {
      WorkflowProjectManager.addProject(workflowProject);
    }
    else
    {
      workflowProject.setEarProjectName(project.getName());  // may deviate from convention
      WorkflowProjectManager.getInstance().save(workflowProject, project);
    }
    
    monitor.done();  // project update happens in WorkflowFacetPostInstallDelegate
  }  
}
