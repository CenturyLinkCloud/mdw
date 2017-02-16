/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.IDebugUIConstants;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ExternalEventLaunchConfiguration extends WorkflowLaunchConfiguration {
    public static final String MESSAGE_PATTERN = "messagePattern";

    public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        String wfProjectName = launchConfig.getAttribute(WORKFLOW_PROJECT, "");
        WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                .getWorkflowProject(wfProjectName);
        if (workflowProject == null) {
            showError("Can't locate workflow project: '" + wfProjectName + "'.", "Process Launch",
                    null);
            return;
        }

        setWriteToConsole(
                launchConfig.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true));

        String messagePattern = launchConfig.getAttribute(MESSAGE_PATTERN, "");
        if (!workflowProject.externalEventMessagePatternExists(messagePattern)) {
            showError("Can't locate external event with message pattern: '" + messagePattern
                    + "' in " + wfProjectName + ".", "Launch External Event", workflowProject);
            return;
        }

        String request = launchConfig.getAttribute(EXTERNAL_EVENT_REQUEST, "");

        fireExternalEvent(workflowProject, request, null);
    }
}
