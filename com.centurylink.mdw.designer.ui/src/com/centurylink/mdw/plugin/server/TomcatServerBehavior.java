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
package com.centurylink.mdw.plugin.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jst.server.tomcat.core.internal.ITomcatServer;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ProgressUtil;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.GradleBuildFile;
import com.centurylink.mdw.plugin.project.model.MavenBuildFile;
import com.centurylink.mdw.plugin.project.model.OsgiBuildFile;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

@SuppressWarnings("restriction")
public class TomcatServerBehavior
        extends org.eclipse.jst.server.tomcat.core.internal.TomcatServerBehaviour
        implements MdwServerConstants {
    static final boolean DEFAULT_DEBUG_MODE = false;

    @Override
    public void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor)
            throws CoreException {
        // prevent dueling debugs
        if ("debug".equals(launchMode)
                && getServer().getAttribute(DEBUG_MODE, DEFAULT_DEBUG_MODE)) {
            String msg = "Cannot start Tomcat in Debug mode with MDW Debug parameters also specified.  "
                    + "Use the MDW Debug for remote debugging of Dynamic Java; otherwise use Tomcat Debug.";
            throw new CoreException(new Status(Status.ERROR, MdwPlugin.getPluginId(), msg));
        }
        super.setupLaunch(launch, launchMode, monitor);
    }

    public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy,
            IProgressMonitor monitor) throws CoreException {
        // remove the debug args when stopping or when MDW debugging is disabled
        if (getServer().getServerState() == IServer.STATE_STOPPING
                || !getServer().getAttribute(DEBUG_MODE, DEFAULT_DEBUG_MODE)) {
            String vmArgs = workingCopy.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
            if (vmArgs != null) {
                List<String> filteredArgs = new ArrayList<String>();
                for (String vmArg : DebugPlugin.parseArguments(vmArgs)) {
                    if (!vmArg.startsWith("-Xdebug") && !vmArg.startsWith("-Xrunjdwp"))
                        filteredArgs.add(vmArg);
                }
                String argString = "";
                for (int i = 0; i < filteredArgs.size(); i++) {
                    argString += filteredArgs.get(i);
                    if (i < filteredArgs.size() - 1)
                        argString += " ";
                }
                workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
                        argString);
            }
        }

        super.setupLaunchConfiguration(workingCopy, monitor);
    }

    @Override
    public void stop(boolean force) {
        boolean forceStop = getServer().getAttribute(FORCE_STOP, false);
        super.stop(forceStop);
    }

    @Override
    protected String[] getRuntimeVMArguments() {
        List<String> vmArgs = new ArrayList<String>();
        vmArgs.addAll(Arrays.asList(super.getRuntimeVMArguments()));
        vmArgs.addAll(Arrays.asList(
                getServer().getAttribute(JAVA_OPTIONS, getDefaultJavaOptions()).split("\\s+")));
        if (getServer().getServerState() != IServer.STATE_STOPPING)
            vmArgs.addAll(getDebugOptions());
        return vmArgs.toArray(new String[0]);
    }

    protected String getDefaultJavaOptions() {
        WorkflowProject project = getProject();
        String runtimeEnv = project.checkRequiredVersion(6, 0) ? "-Dmdw.runtime.env=dev"
                : "-DruntimeEnv=dev";
        return runtimeEnv + "\n" + "-Dmdw.config.location="
                + (project == null ? "null" : project.getProjectDir())
                + System.getProperty("file.separator") + "config\n"
                + "-Xms512m -Xmx1024m -XX:MaxPermSize=256m";
    }

    protected List<String> getDebugOptions() {
        List<String> debugOpts = new ArrayList<String>();
        if (getServer().getAttribute(DEBUG_MODE, DEFAULT_DEBUG_MODE)) {
            debugOpts.add("-Xdebug");
            debugOpts.add("-Xrunjdwp:transport=dt_socket,server=y,"
                    + (getServer().getAttribute(DEBUG_SUSPEND, DEFAULT_DEBUG_SUSPEND) ? "suspend=y,"
                            : "suspend=n,")
                    + "address=" + getServer().getAttribute(DEBUG_PORT, DEFAULT_DEBUG_PORT));
        }
        return debugOpts;
    }

    @Override
    protected void publishServer(int kind, IProgressMonitor monitor) throws CoreException {
        super.publishServer(kind, monitor);
    }

    /**
     * Only overridden to set the monitor ticks.
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected void publishModules(int kind, List modules, List deltaKind2, MultiStatus multi,
            IProgressMonitor monitor) {
        if (modules == null)
            return;

        int size = modules.size();
        if (size == 0)
            return;

        // publish modules
        for (int i = 0; i < size; i++) {
            if (monitor.isCanceled())
                return;

            // should skip this publish
            IModule[] module = (IModule[]) modules.get(i);
            IModule m = module[module.length - 1];
            if (shouldIgnorePublishRequest(m))
                continue;

            int kind2 = kind;
            if (getServer().getModulePublishState(module) == IServer.PUBLISH_STATE_UNKNOWN)
                kind2 = IServer.PUBLISH_FULL;

            // for workflow client apps, download if mdw.war not found
            if (getWebProject() == null && module[0].getProject() != null) {
                WorkflowProject clientWf = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(module[0].getProject());
                if (clientWf != null) {
                    String deployLoc = getServer().getAttribute(ITomcatServer.PROPERTY_INSTANCE_DIR,
                            clientWf.getDeployFolder().getLocation().toPortableString());
                    File mdwWar = new File(deployLoc + "/webapps/mdw.war");

                    if (kind == IServer.PUBLISH_CLEAN && mdwWar.exists() && !mdwWar.delete())
                        showError("Publish Error", "Unable to delete " + mdwWar, clientWf);

                    if (!mdwWar.exists()) {
                        ProjectUpdater updater = new ProjectUpdater(getProject(),
                                MdwPlugin.getSettings());
                        try {
                            updater.updateFrameworkJars(new SubProgressMonitor(monitor, 1));
                        }
                        catch (Exception ex) {
                            showError(ex.getMessage(), "Update Framework Libraries", clientWf);
                        }
                    }
                }
            }

            IStatus status;
            if (getWebProject() != null && kind == IServer.PUBLISH_CLEAN
                    || ((Integer) deltaKind2.get(i)).intValue() == ServerBehaviourDelegate.REMOVED)
                status = publishModule(kind2, module, ((Integer) deltaKind2.get(i)).intValue(),
                        ProgressUtil.getSubMonitorFor(monitor, size * 3000));
            else
                status = publishModule(kind2, module, ((Integer) deltaKind2.get(i)).intValue(),
                        ProgressUtil.getSubMonitorFor(monitor, 3000));

            if (status != null && !status.isOK())
                multi.add(status);
        }
    }

    /**
     * For client apps with no workflow webapp, we take care of publishing in
     * ProjectUpdater.deployCloudWar().
     */
    @Override
    protected void publishModule(int kind, int deltaKind, IModule[] moduleTree,
            IProgressMonitor monitor) throws CoreException {
        IProject webProject = getWebProject();
        boolean isClientWf = false;
        if (webProject == null) // not framework
        {
            if (moduleTree[0].getProject() != null)
                isClientWf = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(moduleTree[0].getProject()) != null;
        }

        if (isClientWf) // nothing to publish -- just refresh
        {
            // refresh the project's deploy folder after full publish
            WorkflowProject project = getProject();
            if (project != null)
                project.getDeployFolder().refreshLocal(IResource.DEPTH_INFINITE,
                        new SubProgressMonitor(monitor, 500));
        }
        else {
            IPath publishPath = getModuleDeployDirectory(moduleTree[0]);
            if (moduleTree.length > 1 && !"jst.web".equals(moduleTree[1].getModuleType().getId()))
                publishPath = publishPath.append("WEB-INF/classes"); // non-web
                                                                     // child
                                                                     // module
            File publishDir = publishPath.toFile();

            // only delete for the top level module, or else we'll undo previous
            // passes
            if (moduleTree.length == 1 && kind == IServer.PUBLISH_CLEAN
                    || deltaKind == ServerBehaviourDelegate.REMOVED
                    || getTomcatServer().isServeModulesWithoutPublish()) {
                if (publishPath != null) {
                    if (publishDir.exists())
                        showError(PublishHelper.deleteDirectory(publishDir, monitor));
                }
                if (deltaKind == ServerBehaviourDelegate.REMOVED
                        || getTomcatServer().isServeModulesWithoutPublish()) {
                    setModulePublishState(moduleTree, IServer.PUBLISH_STATE_NONE);
                    return;
                }
            }
            else if (!publishDir.exists()) {
                if (!publishDir.mkdirs()) {
                    PluginMessages.log("Error creating directory: " + publishDir);
                    showError("Error creating directory: " + publishDir, "Server Deploy",
                            getProject());
                    return;
                }
            }

            PublishHelper publishHelper = new PublishHelper(null);

            if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
                if (moduleTree.length == 1) // publish everything in top level
                                            // module to prevent overwriting by submodules
                {
                    // referenced projects first so that main module overrides any conflicts
                    IModule[] submods = getTomcatServer().getChildModules(moduleTree);
                    for (IModule submod : submods) {
                        if ("mdw-web".equals(submod.getName()) || "mdw-taskmgr".equals(submod.getName()))
                            continue;
                        IModuleResource[] mrs = getResources(new IModule[] { submod });
                        IPath submodPath = publishPath;
                        if (!"jst.web".equals(submod.getModuleType().getId()))
                            submodPath = submodPath.append("WEB-INF/classes");
                        // deployment assembly designates
                        IStatus[] statuses = publishHelper.publishFull(mrs, submodPath, monitor);
                        if (showError(statuses))
                            return;
                        monitor.worked(2000 / submods.length);
                    }
                    // main module publish
                    IModuleResource[] mrs = getResources(moduleTree);
                    IStatus[] statuses = publishHelper.publishFull(mrs, publishPath, monitor);
                    if (showError(statuses))
                        return;
                    monitor.worked(1000);

                    // publish full includes web-inf/lib jars (otherwise assume
                    // wtp components cover deps)
                    try {
                        OsgiBuildFile buildFile = new GradleBuildFile(moduleTree[0].getProject());
                        if (!buildFile.exists())
                            buildFile = new MavenBuildFile(moduleTree[0].getProject()); // fall
                                                                                        // back
                                                                                        // to
                                                                                        // pom.xml
                        buildFile.parse();
                        if (!buildFile.exists()) {
                            PluginMessages.log("neither build.gradle nor pom.xml was found");
                            return; // can happen when project deleted from
                                    // workspace
                        }

                        String archiveName = buildFile.getArtifactName();
                        if (archiveName.startsWith("mdw-hub-")
                                || archiveName.startsWith("mdwhub-")) {
                            // first delete conflicting jsf dependencies
                            File toDelete = publishPath
                                    .append("WEB-INF/lib/mdwweb-" + buildFile.getVersion() + ".jar")
                                    .toFile();
                            if (toDelete.exists()) {
                                if (!toDelete.delete())
                                    throw new IOException("Unable to delete file: " + toDelete);
                            }
                            archiveName = "mdw-" + buildFile.getVersion() + ".war";
                        }
                        if (!archiveName.endsWith(".war"))
                            archiveName += ".war";
                        File archive;
                        if (buildFile.getArtifactGenDir().startsWith("..")) // relative
                                                                            // path
                                                                            // one
                                                                            // level
                                                                            // too
                                                                            // high
                            archive = new File(
                                    moduleTree[0].getProject().getLocation().toFile().toString()
                                            + buildFile.getArtifactGenDir().substring(2) + "/"
                                            + archiveName);
                        else
                            archive = new File(
                                    moduleTree[0].getProject().getLocation().toFile().toString()
                                            + "/" + buildFile.getArtifactGenDir() + "/"
                                            + archiveName);
                        if (!archive.exists()) {
                            PluginMessages.log("Unable to locate web archive: " + archive);
                            showError("Unable to locate web archive: " + archive, "Server Deploy",
                                    getProject());
                            return;
                        }

                        copyWebInfLibArchiveEntriesToDir(archive, publishDir);

                        // refresh the project's deploy folder after full publish
                        WorkflowProject project = getProject();
                        if (project != null)
                            project.getDeployFolder().refreshLocal(IResource.DEPTH_INFINITE,
                                    new SubProgressMonitor(monitor, 500));
                    }
                    catch (OperationCanceledException ex) {
                    }
                    catch (Exception ex) {
                        PluginMessages.log(ex);
                        showError(ex.toString(), "Server Publish", getProject());
                    }
                }
            }
            else {
                IModuleResourceDelta[] deltas = getPublishedResourceDelta(moduleTree);
                IStatus[] statuses = publishHelper.publishDelta(deltas, publishPath, monitor);
                if (showError(statuses))
                    return;
            }
        }

        setModulePublishState(moduleTree, IServer.PUBLISH_STATE_NONE);
    }

    public IPath getModuleDeployDirectory(IModule module) {
        if ("mdw-hub".equals(module.getName()))
            return getServerDeployDirectory().append("mdw");
        else
            return getServerDeployDirectory().append(module.getName());
    }

    private WorkflowProject getProject() {
        for (IModule module : getServer().getModules()) {
            WorkflowProject project = WorkflowProjectManager.getInstance()
                    .getWorkflowProject(module.getProject());
            if (project != null)
                return project;
        }
        return null;
    }

    /**
     * Currently only returns true for framework (mdw-hub). In fact it's used to
     * differentiate framework vs. non-framework deployments.
     */
    private IProject getWebProject() {
        WorkflowProject workflowProject = getProject();
        if (workflowProject == null)
            return null;
        else
            return workflowProject.getWebProject();
    }

    private boolean showError(IStatus[] statuses) {
        if (statuses != null) {
            for (IStatus status : statuses) {
                if (status.getSeverity() > IStatus.WARNING) {
                    PluginMessages.log(status);
                    showError(status.getMessage(), "Server Deploy", getProject());
                    return true;
                }
            }
        }
        return false;
    }

    private void showError(final String message, final String title,
            final WorkflowProject workflowProject) {
        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                PluginMessages.uiError(message, title, workflowProject);
            }
        });
    }

    protected void copyWebInfLibArchiveEntriesToDir(File archive, File destDir) throws IOException {
        JarFile archiveFile = null;
        try {
            archiveFile = new JarFile(archive);
            for (Enumeration<?> entriesEnum = archiveFile.entries(); entriesEnum
                    .hasMoreElements();) {
                JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
                if (!jarEntry.isDirectory()
                        && (jarEntry.getName().startsWith("WEB-INF/lib/")
                                || jarEntry.getName().startsWith("META-INF/"))
                        || jarEntry.getName()
                                .equals("WEB-INF/classes/MdwHubUiMessages_en.properties")) {
                    File destFile = new File(destDir + "/" + jarEntry.getName());
                    if (!destFile.exists()) {
                        if (!destFile.getParentFile().exists()) {
                            if (!destFile.getParentFile().mkdirs())
                                throw new IOException(
                                        "Unable to create web lib destination dir: " + destDir);
                        }
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = archiveFile.getInputStream(jarEntry);
                            byte[] buffer = new byte[1024];
                            os = new FileOutputStream(destFile);
                            while (true) {
                                int bytesRead = is.read(buffer);
                                if (bytesRead == -1)
                                    break;
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                        finally {
                            if (is != null)
                                is.close();
                            if (os != null)
                                os.close();
                        }
                    }
                }
            }
        }
        finally {
            if (archiveFile != null)
                archiveFile.close();
        }
    }

}