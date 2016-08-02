/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.tomcat.core.internal.Messages;
import org.eclipse.jst.server.tomcat.core.internal.TomcatConfiguration;
import org.eclipse.jst.server.tomcat.core.internal.TomcatPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerUtil;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;


@SuppressWarnings("restriction")
public class TomcatServer extends org.eclipse.jst.server.tomcat.core.internal.TomcatServer
{
  @Override
  public boolean isTestEnvironment()
  {
    return getAttribute(PROPERTY_TEST_ENVIRONMENT, true);
  }

  @Override
  public String getInstanceDirectory()
  {
    String deployDir = null;
    for (IModule serverModule : getServer().getModules())
    {
      WorkflowProject project = getWorkflowProject(serverModule);
      if (project != null)
        deployDir = project.getDeployFolder().getLocation().toPortableString();
    }

    return getAttribute(PROPERTY_INSTANCE_DIR, deployDir);
  }

  public WorkflowProject getWorkflowProject(IModule module)
  {
    for (IModule serverModule : getServer().getModules())
    {
      if (module.getId().equals(serverModule.getId()))
      {
        WorkflowProject workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(serverModule.getProject());
        if (workflowProject != null)
          return workflowProject;
      }
    }
    return null;
  }

  private IProject getWorkflowWebProject(IModule module)
  {
    if (module == null)
      return null;
    WorkflowProject workflowProject = getWorkflowProject(module);
    if (workflowProject == null)
      return null;
    else
      return workflowProject.getWebProject();
  }

  @Override
  public String getDeployDirectory()
  {
    // not used by tomcat 7
    // less confusing to show "webapps" versus super.getDeployDirectory()
    return getAttribute(PROPERTY_DEPLOY_DIR, "webapps");
  }

  public void setDefaults(IProgressMonitor monitor)
  {
    super.setDefaults(monitor);
    setDeployDirectory("webapps");
  }

  @Override
  public IModule[] getRootModules(IModule module) throws CoreException
  {
    String projectName = module.getProject() == null ? null : module.getProject().getName();
    if (("jst.web".equals(module.getModuleType().getId()) && !"mdw-taskmgr".equals(projectName) && !"mdw-web".equals(projectName))
            || (WorkflowProjectManager.getInstance().isWorkflowProject(module.getProject()) && !"mdw-workflow".equals(projectName)))
      return new IModule[]{module};
    else
      return new IModule[0];
  }

  @Override
  public IModule[] getChildModules(IModule[] module)
  {
    if (module == null)
      return null;

    IModule[] serverMods = getServer().getModules();
    if (module.length == 1)
    {
      for (IModule serverMod : serverMods)
      {
        if (serverMod.getId().equals(module[0].getId()))
        {
          IProject workflowWebProject = getWorkflowWebProject(module[0]);
          if (workflowWebProject != null)
          {
            try
            {
              List<IModule> children = new ArrayList<IModule>();
              for (IProject refProj : workflowWebProject.getReferencedProjects())
                children.addAll(Arrays.asList(ServerUtil.getModules(refProj)));
              return children.toArray(new IModule[0]);
            }
            catch (CoreException ex)
            {
              PluginMessages.uiError(ex, "Child Modules", getWorkflowProject(module[0]));
            }
          }
        }
      }
    }

    return new IModule[0];
  }

  /**
   * We don't modify the server.xml file when a module is added or removed,
   * because mdw.war is published by Designer in the webapps directory.
   */
  public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException
  {
    IStatus status = canModifyModules(add, remove);
    if (status == null || !status.isOK())
      throw new CoreException(status);

    List<IModule> serverMods = new ArrayList<IModule>(Arrays.asList(getServer().getModules()));
    for (IModule removeMod : remove)
    {
      if (serverMods.contains(removeMod))
        serverMods.remove(removeMod);
    }
    for (IModule addMod : add)
    {
      if (!serverMods.contains(addMod))
        serverMods.add(addMod);
    }
    int mdwModCount = 0;
    for (IModule serverMod : serverMods)
    {
      if (WorkflowProjectManager.getInstance().getWorkflowProject(serverMod.getProject()) != null)
        mdwModCount++;
    }
    if (mdwModCount != 1)
    {
      String msg;
      if (mdwModCount == 0)
        msg = "MDW Tomcat modules must contain a workflow project.";
      else
        msg = "MDW Tomcat modules cannot contain multiple MDW projects.";
      throw new CoreException(new Status(IStatus.ERROR, MdwPlugin.getPluginId(), 0, msg, null));
    }
  }

  private Object versionLock = new Object();
  private int currentVersion;
  private int loadedVersion;

  @Override
  public void configurationChanged()
  {
    synchronized (versionLock)
    {
      // alter the current version
      currentVersion++;
    }
  }

  @Override
  public TomcatConfiguration getTomcatConfiguration() throws CoreException
  {
    int current;
    TomcatConfiguration tcConfig;
    // grab current state
    synchronized (versionLock)
    {
      current = currentVersion;
      tcConfig = configuration;
    }
    // configuration needs loading
    if (tcConfig == null || loadedVersion != current)
    {
      IFolder folder = getServer().getServerConfiguration();
      if (folder == null || !folder.exists())
      {
        String path = null;
        if (folder != null)
        {
          path = folder.getFullPath().toOSString();
          IProject project = folder.getProject();
          if (project != null && project.exists() && !project.isOpen())
            throw new CoreException(new Status(IStatus.ERROR, TomcatPlugin.PLUGIN_ID, 0, NLS.bind(
                Messages.errorConfigurationProjectClosed, path, project.getName()), null));
        }
        throw new CoreException(new Status(IStatus.ERROR, TomcatPlugin.PLUGIN_ID, 0, NLS.bind(
            Messages.errorNoConfiguration, path), null));
      }
      // not yet loaded
      if (tcConfig == null)
          tcConfig = new Tomcat70Configuration(folder);

      try
      {
        ((Tomcat70Configuration)tcConfig).load(folder, null);
        // update loaded version
        synchronized (versionLock)
        {
          // if newer version not already loaded, update version
          if (configuration == null || loadedVersion < current)
          {
            configuration = tcConfig;
            loadedVersion = current;
          }
        }
      }
      catch (CoreException ce)
      {
        // Ignore
        throw ce;
      }
    }
    return tcConfig;
  }

  @Override
  public void importRuntimeConfiguration(IRuntime runtime, IProgressMonitor monitor) throws CoreException
  {
    // initialize state
    synchronized (versionLock)
    {
      configuration = null;
      currentVersion = 0;
      loadedVersion = 0;
    }
    if (runtime == null)
    {
      return;
    }
    IPath path = runtime.getLocation().append("conf");
    IFolder folder = getServer().getServerConfiguration();
    TomcatConfiguration tcConfig = new Tomcat70Configuration(folder);

    try
    {
      tcConfig.importFromPath(path, isTestEnvironment(), monitor);
    }
    catch (CoreException ce)
    {
      throw ce;
    }
    // update version
    synchronized (versionLock)
    {
      // if not already initialized by some other thread, save the configuration
      if (configuration == null)
      {
        configuration = tcConfig;
      }
    }
  }

}
