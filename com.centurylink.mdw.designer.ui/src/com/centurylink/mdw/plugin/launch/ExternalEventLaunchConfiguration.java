/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public static final String EVENT_NAME = "eventName";

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

        String eventName = launchConfig.getAttribute(EVENT_NAME, "");
        if (!workflowProject.externalEventNameExists(eventName)) {
            if(workflowProject.checkRequiredVersion(6, 0))
                showError("Can't locate external event with event name: '" + eventName
                        + "' in " + wfProjectName + ".", "Launch External Event", workflowProject);
            else
                showError("Can't locate external event with message pattern: '" + eventName
                    + "' in " + wfProjectName + ".", "Launch External Event", workflowProject);
            return;
        }

        String request = launchConfig.getAttribute(EXTERNAL_EVENT_REQUEST, "");

        fireExternalEvent(workflowProject, request, null);
    }
}
