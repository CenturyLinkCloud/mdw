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
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class MdwProjectPropertyTester extends org.eclipse.core.expressions.PropertyTester {
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IAdaptable adaptable = (IAdaptable) receiver;
        if ("mdwWorkflowProject".equals(property)) {
            if (adaptable instanceof IProject) {
                return WorkflowProjectManager.getInstance()
                        .getWorkflowProject((IProject) adaptable) != null;
            }
        }
        if ("mdwWorkflowLocalProject".equals(property)) {
            if (adaptable instanceof IProject) {
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject((IProject) adaptable);
                if (workflowProject == null)
                    return false;
                else
                    return !workflowProject.isRemote();
            }
        }
        if ("mdwWorkflowWebProject".equals(property)) {
            // returns false for Cloud projects
            if (adaptable instanceof IProject) {
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject((IProject) adaptable);
                return workflowProject != null && !workflowProject.isCloudProject()
                        && (adaptable.equals(workflowProject.getWebProject()));
            }
        }
        if ("notMdwWorkflowOsgiProject".equals(property)) {
            if (adaptable instanceof IProject) {
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject((IProject) adaptable);
                if (workflowProject == null)
                    return false;
                else
                    return !workflowProject.isOsgi();
            }
        }
        if ("mdwProjectVersion".equals(property)) {
            if (adaptable instanceof IProject) {
                IProject project = (IProject) adaptable;
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(project);
                if (workflowProject == null)
                    return false;
                if (args == null)
                    return false;
                if (args.length < 2)
                    return false;

                if (args.length == 2)
                    return workflowProject.checkRequiredVersion((Integer) args[0],
                            (Integer) args[1]);
                else
                    return workflowProject.checkRequiredVersion((Integer) args[0],
                            (Integer) args[1], (Integer) args[2]);
            }
            else {
                return false;
            }
        }
        if ("mdwProjectVersionLessThan".equals(property)) {
            if (adaptable instanceof IProject) {
                IProject project = (IProject) adaptable;
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(project);
                if (workflowProject == null)
                    return false;
                if (args == null)
                    return false;
                if (args.length < 2)
                    return false;

                if (args.length == 2)
                    return !workflowProject.checkRequiredVersion((Integer) args[0],
                            (Integer) args[1]);
                else
                    return !workflowProject.checkRequiredVersion((Integer) args[0],
                            (Integer) args[1], (Integer) args[2]);
            }
            else {
                return false;
            }
        }

        return false;
    }

}
