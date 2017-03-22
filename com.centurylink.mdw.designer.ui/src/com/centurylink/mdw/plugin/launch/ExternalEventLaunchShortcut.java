/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ExternalEventLaunchShortcut implements ILaunchShortcut {
    public static final String EXTERNAL_EVENT_LAUNCH_CONFIG_TYPE = "com.centurylink.mdw.plugin.launch.ExternalEvent";

    public void launch(ISelection selection, String mode) {
        Object firstElement = ((StructuredSelection) selection).getFirstElement();
        if (firstElement instanceof ExternalEvent) {
            ExternalEvent externalEvent = (ExternalEvent) firstElement;
            try {
                boolean prevEnablement = disableBuildBeforeLaunch();
                performLaunch(externalEvent, mode);
                setBuildBeforeLaunch(prevEnablement);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Launch External Event", externalEvent.getProject());
            }
        }
    }

    public void launch(IEditorPart editor, String mode) {
        // no editor for external events
    }

    private void performLaunch(ExternalEvent externalEvent, String mode)
            throws InterruptedException, CoreException {
        ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(externalEvent);
        ILaunchConfiguration config = findExistingLaunchConfiguration(workingCopy);
        if (config == null) {
            // no existing found - create a new one
            config = workingCopy.doSave();
        }

        Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
        IStructuredSelection selection = new StructuredSelection(config);
        String groupId = "com.centurylink.mdw.plugin.launch.group.extevent";
        if (mode.equals(ILaunchManager.DEBUG_MODE))
            groupId += ".debug";
        DebugUITools.openLaunchConfigurationDialogOnGroup(shell, selection, groupId);
    }

    protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(ExternalEvent externalEvent)
            throws CoreException {
        WorkflowProject workflowProject = externalEvent.getProject();

        String launchConfigName = getUniqueLaunchConfigName(externalEvent);
        ILaunchConfigurationType configType = getLaunchManager()
                .getLaunchConfigurationType(EXTERNAL_EVENT_LAUNCH_CONFIG_TYPE);
        ILaunchConfigurationWorkingCopy wc = configType
                .newInstance(workflowProject.getSourceProject(), launchConfigName);

        wc.setAttribute(ExternalEventLaunchConfiguration.WORKFLOW_PROJECT,
                workflowProject.getName());
        wc.setAttribute(ExternalEventLaunchConfiguration.EVENT_NAME,
                externalEvent.getName());
        return wc;
    }

    protected String getUniqueLaunchConfigName(ExternalEvent externalEvent) throws CoreException {
        String name = externalEvent.getName();
        ILaunchConfigurationType configType = getLaunchManager()
                .getLaunchConfigurationType(EXTERNAL_EVENT_LAUNCH_CONFIG_TYPE);
        ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
        for (ILaunchConfiguration launchConfig : configs) {
            if (launchConfig.getName().equals(name)) {
                name = externalEvent.getName() + " (" + externalEvent.getProject().getName() + ")";
                break;
            }
        }
        return getLaunchManager().generateLaunchConfigurationName(name);
    }

    private ILaunchConfiguration findExistingLaunchConfiguration(
            ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
        String wcWorkflowProject = workingCopy
                .getAttribute(ExternalEventLaunchConfiguration.WORKFLOW_PROJECT, "");
        String wcMessagePattern = workingCopy
                .getAttribute(ExternalEventLaunchConfiguration.EVENT_NAME, "");

        ILaunchConfigurationType configType = workingCopy.getType();
        ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
        for (ILaunchConfiguration launchConfig : configs) {
            String workflowProject = launchConfig
                    .getAttribute(ExternalEventLaunchConfiguration.WORKFLOW_PROJECT, "");
            if (!wcWorkflowProject.equals(workflowProject))
                continue;
            String messagePattern = launchConfig
                    .getAttribute(ExternalEventLaunchConfiguration.EVENT_NAME, "");
            if (!wcMessagePattern.equals(messagePattern))
                continue;

            return launchConfig;
        }
        return null;
    }

    private ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }

    /**
     * Returns previous pref value.
     */
    protected boolean disableBuildBeforeLaunch() {
        boolean buildBeforeLaunchPref = MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
        MdwPlugin.getDefault().getPreferenceStore()
                .setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, false);
        return buildBeforeLaunchPref;
    }

    protected void setBuildBeforeLaunch(boolean buildBeforeLaunchPref) {
        MdwPlugin.getDefault().getPreferenceStore()
                .setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, buildBeforeLaunchPref);
    }
}
