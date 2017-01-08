/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessLaunchShortcut implements ILaunchShortcut
{
  public static final String PROCESS_LAUNCH_CONFIG_TYPE = "com.centurylink.mdw.plugin.launch.Process";

  public void launch(ISelection selection, String mode)
  {
    Object firstElement = ((StructuredSelection)selection).getFirstElement();
    if (firstElement instanceof WorkflowProcess)
    {
      WorkflowProcess processVersion = (WorkflowProcess) firstElement;
      try
      {
        boolean prevEnablement = disableBuildBeforeLaunch();
        performLaunch(processVersion, mode);
        setBuildBeforeLaunch(prevEnablement);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Process Launch", processVersion.getProject());
      }
    }
  }

  public void launch(IEditorPart editor, String mode)
  {
    if (editor.getEditorInput() instanceof WorkflowProcess)
    {
      WorkflowProcess processVersion = (WorkflowProcess) editor.getEditorInput();
      try
      {
        performLaunch(processVersion, mode);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Process Launch", processVersion.getProject());
      }
    }
  }

  private void performLaunch(WorkflowProcess processVersion, String mode) throws InterruptedException, CoreException
  {
    boolean debug = mode.equals(ILaunchManager.DEBUG_MODE);
    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(PROCESS_LAUNCH_CONFIG_TYPE, processVersion, debug);
    ILaunchConfiguration config = findExistingLaunchConfiguration(workingCopy, processVersion.getProject(), debug);
    if (config == null)
    {
      // no existing found - create a new one
      config = workingCopy.doSave();
    }
    Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
    IStructuredSelection selection = new StructuredSelection(config);
    String groupId = "com.centurylink.mdw.plugin.launch.group.mdw";
    if (mode.equals(ILaunchManager.DEBUG_MODE))
      groupId += ".debug";

    DebugUITools.openLaunchConfigurationDialogOnGroup(shell, selection, groupId);
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(String typeId, WorkflowProcess processVersion, boolean debug) throws CoreException
  {
    WorkflowProject workflowProject = processVersion.getProject();

    ILaunchConfigurationType configType = getLaunchManager().getLaunchConfigurationType(typeId);
    String launchConfigName = getUniqueLaunchConfigName(processVersion);
    ILaunchConfigurationWorkingCopy wc = configType.newInstance(workflowProject.getSourceProject(), launchConfigName);

    wc.setAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, workflowProject.getName());
    wc.setAttribute(ProcessLaunchConfiguration.PROCESS_NAME, processVersion.getName());
    wc.setAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, processVersion.getVersionString());

    if (debug)
    {
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, workflowProject.getSourceProject().getName());
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, "org.eclipse.jdt.launching.socketAttachConnector");
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, getDefaultConnectArgs(workflowProject));
    }
    wc.setContainer(null);
    return wc;
  }

  protected String getUniqueLaunchConfigName(WorkflowProcess processVersion) throws CoreException
  {
    String name = processVersion.getName();
    ILaunchConfigurationType configType = getLaunchManager().getLaunchConfigurationType(PROCESS_LAUNCH_CONFIG_TYPE);
    ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
    for (ILaunchConfiguration launchConfig : configs)
    {
      if (launchConfig.getName().equals(name))
      {
        name = processVersion.getName() + " (" + processVersion.getProject().getName() + ")";
        break;
      }
    }
    return getLaunchManager().generateLaunchConfigurationName(name);
  }

  protected ILaunchConfiguration findExistingLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, WorkflowProject workflowProject, boolean debug) throws CoreException
  {
    String wcWorkflowProj = workingCopy.getAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, "");
    String wcProcessName = workingCopy.getAttribute(ProcessLaunchConfiguration.PROCESS_NAME, "");
    String wcProcessVersion = workingCopy.getAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, "");

    ILaunchConfigurationType configType = workingCopy.getType();
    ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
    for (ILaunchConfiguration launchConfig : configs)
    {
      String workflowProj = launchConfig.getAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, "");
      if (!wcWorkflowProj.equals(workflowProj))
        continue;
      String processName = launchConfig.getAttribute(ProcessLaunchConfiguration.PROCESS_NAME, "");
      if (!wcProcessName.equals(processName))
        continue;

      String processVersion = launchConfig.getAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, "");
      if (!wcProcessVersion.equals(processVersion))
      {
        // match without process version; just update version attribute if not matching
        ILaunchConfigurationWorkingCopy updatedWc = launchConfig.getWorkingCopy();
        updatedWc.setAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, wcProcessVersion);
        launchConfig = updatedWc.doSave();
      }

      if (debug)
      {
        // launch config has matched; just update debug attributes if blank
        String debugProj = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        String vmConnector = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, "");
        Map<?,?> connectAttrMap = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, new HashMap<String,String>());

        ILaunchConfigurationWorkingCopy updatedWc = null;
        if (debugProj.isEmpty())
        {
          updatedWc = launchConfig.getWorkingCopy();
          updatedWc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, workflowProject.getSourceProject().getName());
        }
        if (vmConnector.isEmpty())
        {
          if (updatedWc == null)
            updatedWc = launchConfig.getWorkingCopy();
          updatedWc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, "org.eclipse.jdt.launching.socketAttachConnector");
        }
        if (connectAttrMap.isEmpty())
        {
          if (updatedWc == null)
            updatedWc = launchConfig.getWorkingCopy();
          updatedWc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, getDefaultConnectArgs(workflowProject));
        }
        if (updatedWc != null)
          launchConfig = updatedWc.doSave();
      }

      return launchConfig;
    }
    return null;
  }

  protected Map<String,String> getDefaultConnectArgs(WorkflowProject workflowProject)
  {
    Map<String,String> argDefaults = new HashMap<String,String>();
    argDefaults.put("hostname", workflowProject.isRemote() ? workflowProject.getServerSettings().getHost() : "localhost");
    argDefaults.put("port", ProcessLaunchConfiguration.DEFAULT_DEBUG_PORT);
    return argDefaults;
  }

  protected ILaunchManager getLaunchManager()
  {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  /**
   * Returns previous pref value.
   */
  protected boolean disableBuildBeforeLaunch()
  {
    boolean buildBeforeLaunchPref = MdwPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
    MdwPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, false);
    return buildBeforeLaunchPref;
  }

  protected void setBuildBeforeLaunch(boolean buildBeforeLaunchPref)
  {
    MdwPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, buildBeforeLaunchPref);
  }

}
