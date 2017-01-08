/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class MdwAdmin extends ExtensionModule
{
  public boolean select(Object object)
  {
    WorkflowProject workflowProject = (WorkflowProject) object;
    return workflowProject.checkRequiredVersion(5, 5) && workflowProject.isWar();
  }

  public boolean addTo(WorkflowProject project, IProgressMonitor monitor)
  throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);

    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings())
      {
        protected URL getFileUrl() throws IOException
        {
          return super.getRepositoryFileUrl("mdwadmin");
        }
      };
      updater.addWarLib("mdwadmin-" + project.getMdwVersion() + ".war", "mdwadmin.war", new SubProgressMonitor(monitor, 90));
      monitor.worked(5);
    }
    catch (InterruptedException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ExtensionModuleException(ex.getMessage(), ex);
    }

    return true;
  }

  public boolean removeFrom(WorkflowProject project, IProgressMonitor monitor)
  throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);

    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      updater.removeWarLib("mdwadmin.war", new SubProgressMonitor(monitor, 90));
      monitor.worked(5);
    }
    catch (InterruptedException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ExtensionModuleException(ex.getMessage(), ex);
    }

    return true;
  }

  public boolean update(WorkflowProject project, IProgressMonitor monitor)
  throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);

    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings())
      {
        protected URL getFileUrl() throws IOException
        {
          return super.getRepositoryFileUrl("mdwadmin");
        }
      };
      updater.addWarLib("mdwadmin-" + project.getMdwVersion() + ".war", "mdwadmin.war", new SubProgressMonitor(monitor, 90));
      monitor.worked(5);
    }
    catch (InterruptedException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new ExtensionModuleException(ex.getMessage(), ex);
    }

    return true;
  }

}
