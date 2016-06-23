/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.ant.taskdef.BirtFaces;

// TODO update taskmgr faces-config.xml and WEB-INF/libs
public class BirtReporting extends WebExtension
{
  public static final String MDW_FACES_CONFIG = "WEB-INF/mdw-faces-config.xml";
  public static final String FACES_CONFIG = "WEB-INF/faces-config.xml";
  public static final String MDWWEB_PHASE_LISTENER = "com.qwest.mdw.web.jsf.phase.ReportsPhaseListener";
  public static final String TASKMGR_PHASE_LISTENER = "com.qwest.mdw.taskmgr.jsf.phase.ReportsPhaseListener";

  @Override
  public boolean addTo(WorkflowProject project, IProgressMonitor monitor) throws ExtensionModuleException, InterruptedException
  {
    monitor.worked(5);

    try
    {
      ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
      // MDWWeb.war
      List<DescriptorUpdater> mdwWebUpdaters = new ArrayList<DescriptorUpdater>();
      mdwWebUpdaters.add(getMdwWebUpdaterForAdd());
      updater.addWebLibs(getZipFile(project), "MDWWeb.war", mdwWebUpdaters, true, false, new SubProgressMonitor(monitor, 50));
      // MDWTaskManagerWeb.war
      List<DescriptorUpdater> taskMgrUpdaters = new ArrayList<DescriptorUpdater>();
      taskMgrUpdaters.add(getTaskManagerUpdaterForAdd());
      updater.addWebLibs(getZipFile(project), "MDWTaskManagerWeb.war", taskMgrUpdaters, false, true, new SubProgressMonitor(monitor, 40));
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
      // MDWWeb.war
      List<DescriptorUpdater> mdwWebUpdaters = new ArrayList<DescriptorUpdater>();
      mdwWebUpdaters.add(getMdwWebUpdaterForRemove());
      updater.removeWebLibs(getZipFile(project), "MDWWeb.war", mdwWebUpdaters, true, false, new SubProgressMonitor(monitor, 50));
      // MDWTaskManagerWeb.war
      List<DescriptorUpdater> taskMgrUpdaters = new ArrayList<DescriptorUpdater>();
      taskMgrUpdaters.add(getTaskManagerUpdaterForRemove());
      updater.removeWebLibs(getZipFile(project), "MDWTaskManagerWeb.war", taskMgrUpdaters, false, true, new SubProgressMonitor(monitor, 40));
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

  public DescriptorUpdater getMdwWebUpdaterForAdd()
  {
    return new DescriptorUpdater()
    {
      public String getFilePath()
      {
        return MDW_FACES_CONFIG;
      }
      public String processContents(String rawContents, IProgressMonitor monitor) throws IOException
      {
        monitor.subTask("Updating " + getFilePath());
        BirtFaces birtFaces = new BirtFaces();
        birtFaces.setFile(new File(MDW_FACES_CONFIG));
        birtFaces.setPhaseListener(MDWWEB_PHASE_LISTENER);
        birtFaces.setIncludeComponent(true);
        return birtFaces.add(rawContents);
      }
    };
  }

  public DescriptorUpdater getTaskManagerUpdaterForAdd()
  {
    return new DescriptorUpdater()
    {
      public String getFilePath()
      {
        return FACES_CONFIG;
      }
      public String processContents(String rawContents, IProgressMonitor monitor) throws IOException
      {
        monitor.subTask("Updating " + getFilePath());
        BirtFaces birtFaces = new BirtFaces();
        birtFaces.setFile(new File(FACES_CONFIG));
        birtFaces.setPhaseListener(TASKMGR_PHASE_LISTENER);
        return birtFaces.add(rawContents);
      }
    };
  }

  public DescriptorUpdater getMdwWebUpdaterForRemove()
  {
    return new DescriptorUpdater()
    {
      public String getFilePath()
      {
        return MDW_FACES_CONFIG;
      }
      public String processContents(String rawContents, IProgressMonitor monitor) throws IOException
      {
        monitor.subTask("Updating " + getFilePath());
        BirtFaces birtFaces = new BirtFaces();
        birtFaces.setFile(new File(MDW_FACES_CONFIG));
        birtFaces.setPhaseListener(MDWWEB_PHASE_LISTENER);
        birtFaces.setIncludeComponent(true);
        return birtFaces.remove(rawContents);
      }
    };
  }

  public DescriptorUpdater getTaskManagerUpdaterForRemove()
  {
    return new DescriptorUpdater()
    {
      public String getFilePath()
      {
        return FACES_CONFIG;
      }
      public String processContents(String rawContents, IProgressMonitor monitor) throws IOException
      {
        monitor.subTask("Updating " + getFilePath());
        BirtFaces birtFaces = new BirtFaces();
        birtFaces.setFile(new File(FACES_CONFIG));
        birtFaces.setPhaseListener(TASKMGR_PHASE_LISTENER);
        birtFaces.setIncludeComponent(true);
        return birtFaces.remove(rawContents);
      }
    };
  }
}
