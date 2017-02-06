/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.dialogs.MdwChoiceDialog;
import com.centurylink.mdw.plugin.designer.dialogs.MdwSelectDialog;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AdminUi extends ExtensionModule
{
  private static final String MDW_ADMIN = "com/centurylink/mdw/mdw-admin";

  public boolean select(Object object)
  {
    WorkflowProject project = (WorkflowProject) object;
    return project.checkRequiredVersion(5, 5) && !project.checkRequiredVersion(6, 0) && project.isWar();
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
          return super.getRepositoryFileUrl("mdw-admin", getVersion());
        }
      };
      updater.addWarLib("mdw-admin-" + getVersion() + ".war", "mdw-admin.war", new SubProgressMonitor(monitor, 90));
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
      updater.removeWarLib("mdw-admin.war", new SubProgressMonitor(monitor, 90));
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
          return super.getRepositoryFileUrl("mdw-admin", getVersion());
        }
      };
      updater.addWarLib("mdw-admin-" + getVersion() + ".war", "mdw-admin.war", new SubProgressMonitor(monitor, 90));
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
  public String getRequiredMdwVersion()
  {
    return "5.5.24";
  }

  @Override
  public void readConfigElement(String qName, Map<String,String> attrs, WorkflowProject project)
  {
    if (qName.equals("adminUi"))
    {
      setVersion(attrs.get("version"));
      if (!project.getExtensionModules().contains(this))
        project.getExtensionModules().add(this);
    }

  }

  @Override
  public String writeConfigElement(WorkflowProject project)
  {
    return "  <adminUi version=\"" + getVersion() + "\" />\n";
  }

  @Override
  public boolean addUi(WorkflowProject project, Shell shell) throws ExtensionModuleException
  {
    List<String> adminVersions = MdwPlugin.getSettings().getMdwVersions(MDW_ADMIN);
    MdwSelectDialog selectDlg = new MdwSelectDialog(shell, "Admin UI Version", adminVersions);
    selectDlg.setTitle("Admin UI Extension");
    int res = selectDlg.open();
    if (res == MdwChoiceDialog.CANCEL)
      return false;
    else
    {
      String selection = selectDlg.getSelection();
      if (selection == null)
        return false;
      else
      {
        setVersion(selection);
        return true;
      }
    }
  }

  @Override
  public boolean removeUi(WorkflowProject project, Shell shell) throws ExtensionModuleException
  {
    return super.removeUi(project, shell);
  }

}
