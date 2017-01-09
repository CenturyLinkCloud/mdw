/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class LibExtension extends ExtensionModule
{
  @Override
  public boolean addTo(WorkflowProject project, IProgressMonitor monitor) throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);
    
    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      updater.addAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
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

  @Override
  public boolean removeFrom(WorkflowProject project, IProgressMonitor monitor) throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);
    
    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      updater.removeAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
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

  @Override
  public boolean update(WorkflowProject project, IProgressMonitor monitor) throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);
    
    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      updater.addAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
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
  
  protected String getZipFile(WorkflowProject project)
  {
    return getId() + "_" + (getVersion() == null ? project.getMdwVersion() : getVersion()) + ".zip";
  }
  
}
