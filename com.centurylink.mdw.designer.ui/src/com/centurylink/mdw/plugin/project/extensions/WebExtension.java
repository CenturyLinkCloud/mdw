/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Extension module for adding libraries to the MDWWeb webapp war file.
 */
public class WebExtension extends ExtensionModule
{
  @Override
  public boolean addTo(WorkflowProject project, IProgressMonitor monitor) throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);
    
    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      updater.addWebLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
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
      updater.removeWebLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
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
    return addTo(project, monitor);
  }

  protected String getZipFile(WorkflowProject project)
  {
    return getId() + "_" + (getVersion() == null ? project.getMdwVersion() : getVersion()) + ".zip";
  }

}
