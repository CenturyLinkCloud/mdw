/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class JavaSourcePathComputer implements ISourcePathComputerDelegate {
    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration,
            IProgressMonitor monitor) throws CoreException {
        IRuntimeClasspathEntry[] unresolvedEntries = JavaRuntime
                .computeUnresolvedSourceLookupPath(configuration);
        List<ISourceContainer> sourcefolderList = new ArrayList<ISourceContainer>();

        IServer server = ServerUtil.getServer(configuration);
        List<IJavaProject> javaProjectList = new ArrayList<IJavaProject>();
        if (server == null) {
            String projectName = configuration.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
            if (projectName != null) {
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(projectName);
                if (workflowProject != null)
                    javaProjectList.add(workflowProject.getJavaProject());
            }
        }
        else {
            IModule[] modules = server.getModules();
            addProjectsFromModules(sourcefolderList, modules, javaProjectList, server, monitor);
        }

        IRuntimeClasspathEntry[] projectEntries = new IRuntimeClasspathEntry[javaProjectList
                .size()];
        for (int i = 0; i < javaProjectList.size(); i++)
            projectEntries[i] = JavaRuntime.newDefaultProjectClasspathEntry(javaProjectList.get(i));

        IRuntimeClasspathEntry[] entries = new IRuntimeClasspathEntry[projectEntries.length
                + unresolvedEntries.length];
        System.arraycopy(unresolvedEntries, 0, entries, 0, unresolvedEntries.length);
        System.arraycopy(projectEntries, 0, entries, unresolvedEntries.length,
                projectEntries.length);

        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries,
                configuration);
        ISourceContainer[] javaSourceContainers = JavaRuntime.getSourceContainers(resolved);

        if (!sourcefolderList.isEmpty()) {
            ISourceContainer[] combinedSourceContainers = new ISourceContainer[javaSourceContainers.length
                    + sourcefolderList.size()];
            sourcefolderList.toArray(combinedSourceContainers);
            System.arraycopy(javaSourceContainers, 0, combinedSourceContainers,
                    sourcefolderList.size(), javaSourceContainers.length);
            javaSourceContainers = combinedSourceContainers;
        }

        return javaSourceContainers;
    }

    private void addProjectsFromModules(List<ISourceContainer> sourcefolderList, IModule[] modules,
            List<IJavaProject> javaProjectList, IServer server, IProgressMonitor monitor) {
        for (int i = 0; i < modules.length; i++) {
            IProject project = modules[i].getProject();
            IModule[] pModule = new IModule[1];
            pModule[0] = modules[i];
            IModule[] cModule = server.getChildModules(pModule, monitor);
            if (cModule != null && cModule.length > 0)
                addProjectsFromModules(sourcefolderList, cModule, javaProjectList, server, monitor);
            if (project != null) {
                IFolder moduleFolder = project.getFolder(modules[i].getName());
                if (moduleFolder.exists()) {
                    sourcefolderList.add(new FolderSourceContainer(moduleFolder, true));
                }
                else {
                    try {
                        if (project.hasNature(JavaCore.NATURE_ID)) {
                            IJavaProject javaProject = (IJavaProject) project
                                    .getNature(JavaCore.NATURE_ID);
                            if (!javaProjectList.contains(javaProject)) {
                                javaProjectList.add(javaProject);
                            }
                        }
                    }
                    catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
    }
}
