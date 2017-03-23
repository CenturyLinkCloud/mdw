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

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ServerRunnerLaunchShortcut implements ILaunchShortcut {
    public void launch(ISelection selection, String mode) {
        Object firstElement = ((StructuredSelection) selection).getFirstElement();
        IProject project = null;
        if (firstElement instanceof IProject)
            project = (IProject) firstElement;
        else if (firstElement instanceof IJavaProject)
            project = ((IJavaProject) firstElement).getProject();
        else
            throw new IllegalArgumentException("Unsupported selection: " + firstElement);

        WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                .getWorkflowProject(project);
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.run(workflowProject);
    }

    public void launch(IEditorPart editor, String mode) {
    }
}