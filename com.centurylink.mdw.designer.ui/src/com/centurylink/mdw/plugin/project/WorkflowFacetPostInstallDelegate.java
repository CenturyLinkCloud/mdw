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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class WorkflowFacetPostInstallDelegate implements IDelegate {
    public void execute(IProject project, IProjectFacetVersion fv, Object config,
            final IProgressMonitor monitor) throws CoreException {
        final WorkflowProject workflowProject = (WorkflowProject) config;

        if (workflowProject == null || !workflowProject.isSkipFacetPostInstallUpdates()) {
            MdwPlugin.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    workflowProject.setDefaultFilesToIgnoreDuringUpdate();

                    ProjectUpdater updater = new ProjectUpdater(workflowProject,
                            MdwPlugin.getSettings());

                    ProjectConfigurator configurator = new ProjectConfigurator(workflowProject,
                            MdwPlugin.getSettings());

                    try {
                        updater.updateFrameworkJars(monitor);
                        final IProject project = workflowProject.isCloudProject()
                                ? workflowProject.getSourceProject()
                                : workflowProject.getEarProject();
                        updater.updateMappingTemplates(project.getFolder("deploy/config"), null);

                        if (workflowProject.isCloudProject()) {
                            updater.updateWebProjectJars(monitor);
                            configurator.addFrameworkJarsToClasspath(monitor);
                            configurator.setJavaBuildOutputPath("build/classes", monitor);
                        }
                        else {
                            ProjectInflator inflator = new ProjectInflator(workflowProject,
                                    MdwPlugin.getSettings());
                            inflator.generateEarArtifacts(monitor);
                            configurator.configureEarProject(monitor);
                            inflator.generateSourceArtifacts(monitor);
                            configurator.configureSourceProject(MdwPlugin.getShell(), monitor);
                        }

                        configurator.createFrameworkSourceCodeAssociations(MdwPlugin.getShell(),
                                monitor);

                        // take care of any extension modules
                        ExtensionModulesUpdater changer = new ExtensionModulesUpdater(
                                workflowProject);
                        changer.setAdds(workflowProject.getExtensionModules());
                        changer.doChanges(MdwPlugin.getShell());

                        DesignerPerspective.promptForShowPerspective(
                                MdwPlugin.getActiveWorkbenchWindow(), workflowProject);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "Update Framework Libraries", workflowProject);
                    }
                }
            });
        }
    }
}
