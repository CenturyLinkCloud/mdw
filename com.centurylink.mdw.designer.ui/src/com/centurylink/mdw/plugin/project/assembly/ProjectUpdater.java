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
package com.centurylink.mdw.plugin.project.assembly;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.codegen.Generator;
import com.centurylink.mdw.plugin.codegen.JetAccess;
import com.centurylink.mdw.plugin.codegen.JetConfig;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.extensions.DescriptorUpdater;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Updates a project from an archive file located at a url by downloading the
 * archived resource into the project and then unpacking it in place before
 * deleting it.
 */
public class ProjectUpdater implements IRunnableWithProgress {
    private WorkflowProject workflowProject;

    public WorkflowProject getWorkflowProject() {
        return workflowProject;
    }

    private MdwSettings mdwSettings;

    public MdwSettings getSettings() {
        return mdwSettings;
    }

    private IProject project;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    private IFolder localFolder;

    public IFolder getLocalFolder() {
        return localFolder;
    }

    public void setLocalFolder(IFolder localFolder) {
        this.localFolder = localFolder;
    }

    private IFile file;

    public IFile getFile() {
        return file;
    }

    public void setFile(IFile file) {
        this.file = file;
    }

    private boolean unzip = true;

    public ProjectUpdater(WorkflowProject workflowProject, MdwSettings mdwSettings) {
        this.workflowProject = workflowProject;
        this.mdwSettings = mdwSettings;
    }

    /**
     * Update framework jars in the context of another operation.
     *
     * @param monitor
     *            if null, will launch a stand-alone progress dialog
     */
    public void updateFrameworkJars(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        if (workflowProject.isRemote()) {
            monitor.worked(20);
            return; // dependencies added in the remote pom.xml
        }

        if (workflowProject.isCloudProject()) {
            project = workflowProject.getSourceProject();
            if (workflowProject.isOsgi()) {
                // TODO lib dir always goes to default when updating a
                // pre-existing project
                localFolder = workflowProject.getSourceProject()
                        .getFolder(workflowProject.getOsgiSettings().getLibDir());
            }
            else if (workflowProject.isWar()) {
                localFolder = workflowProject.getSourceProject()
                        .getFolder(MdwPlugin.getSettings().getTempResourceLocation() + "/deploy");
            }
            else {
                localFolder = workflowProject.getSourceProject()
                        .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
            }
        }
        else {
            project = workflowProject.getEarProject();
            localFolder = workflowProject.getEarContentFolder();
        }

        String filename = null;
        if (workflowProject.isOsgi()) {
            filename = "mdw-base-" + workflowProject.getMdwVersion() + ".jar";
            unzip = false;
        }
        else if (workflowProject.isWar()) {
            filename = "mdw-" + workflowProject.getMdwVersion() + ".war";
            unzip = false;
        }
        else {
            filename = "MDWFramework_" + workflowProject.getMdwVersion() + ".zip";
        }

        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + filename);
        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);

        try {
            if (workflowProject.isRemote()) {
                IFolder appInfLibFolder = localFolder.getFolder("APP-INF/lib");

                IFolder libFolder = workflowProject.getSourceProject().getFolder("lib");
                if (!libFolder.exists())
                    appInfLibFolder.move(libFolder.getFullPath(), true, monitor);

                if (localFolder.getFile("MDWServices.jar").exists()) // won't
                                                                     // exist in
                                                                     // >= 5.2
                {
                    IFile servicesJar = libFolder.getFile("MDWServices.jar");
                    if (servicesJar.exists())
                        servicesJar.delete(true, monitor);
                    localFolder.getFile("MDWServices.jar").copy(servicesJar.getFullPath(), true,
                            monitor);
                    localFolder.getFile("MDWServices.jar").delete(true, monitor);
                    IFile servicesSrcJar = libFolder.getFile("MDWServices_src.jar");
                    if (servicesSrcJar.exists())
                        servicesSrcJar.delete(true, monitor);
                    localFolder.getFile("MDWServices_src.jar").copy(servicesSrcJar.getFullPath(),
                            true, monitor);
                    localFolder.getFile("MDWServices_src.jar").delete(true, monitor);
                }

                localFolder.getFolder("APP-INF").delete(true, monitor);
            }
            else if (workflowProject.isCloudProject() && !workflowProject.isOsgi()) {
                if (workflowProject.isWar()) {
                    unDeployCloudWar("mdw.war", monitor);
                    deployCloudWar("mdw.war", monitor);
                    if (workflowProject.isFilePersist())
                        localFolder.getParent().delete(true, monitor); // delete
                                                                       // the
                                                                       // .temp
                                                                       // dir
                                                                       // since
                                                                       // it's
                                                                       // not
                                                                       // used
                                                                       // for
                                                                       // assets
                }
                else {
                    createEarContentsForCloud(monitor);
                }
            }
        }
        catch (Exception ex) {
            throw new InvocationTargetException(ex);
        }
    }

    public void createEarContentsForCloud(IProgressMonitor monitor)
            throws IOException, CoreException {
        IFolder appInfLibFolder = localFolder.getFolder("APP-INF/lib");

        // create the ear
        IFolder deployFolder = workflowProject.getSourceProject().getFolder("deploy");
        if (!deployFolder.exists())
            deployFolder.create(true, true, monitor);
        IFolder earFolder = deployFolder.getFolder("ear");
        if (earFolder.exists()) {
            try {
                earFolder.delete(true, monitor);
            }
            catch (CoreException ex) {
                PluginMessages.log(ex);
                if ("Problems encountered while deleting resources.".equals(ex.getMessage()))
                    throw new IOException(
                            "Unable to delete existing EAR folder.\nMake sure your server is not running");
            }
        }
        earFolder.create(true, true, monitor);

        String[] earJars = (String[]) PluginUtil.appendArrays(WorkflowProject.MDW_SERVICES_LIBS,
                WorkflowProject.MDW_WARS);
        for (String earJar : earJars) {
            IFile earJarFile = localFolder.getFile(earJar);
            earJarFile.copy(earFolder.getFile(earJar).getFullPath(), true, monitor);
            earJarFile.delete(true, monitor);
            String earSrcJar = earJar.endsWith(".war") ? earJar.replaceFirst("\\.war", "_src.jar")
                    : earJar.replaceFirst("\\.jar", "_src.jar");
            IFile earSrcJarFile = localFolder.getFile(earSrcJar);
            if (earSrcJarFile.exists()) {
                earSrcJarFile.copy(earFolder.getFile(earSrcJar).getFullPath(), true, monitor);
                earSrcJarFile.delete(true, monitor);
            }
        }

        IFolder earAppInf = earFolder.getFolder("APP-INF");
        earAppInf.create(true, true, monitor);
        appInfLibFolder.copy(earAppInf.getFolder("lib").getFullPath(), true, monitor);
        localFolder.getFolder("APP-INF").delete(true, monitor);

        // create application.xml
        IFolder earMetaInf = earFolder.getFolder("META-INF");
        earMetaInf.create(true, true, monitor);

        JetAccess jet = getJet("cloud/application.xmljet", workflowProject.getSourceProject(),
                "deploy/ear/META-INF/application.xml");
        Generator generator = new Generator(null);
        generator.createFile(jet, monitor);
    }

    public void deployCloudWar(String warFile, IProgressMonitor monitor)
            throws IOException, CoreException {
        // deploy the war into tomcat
        file.refreshLocal(IResource.DEPTH_ZERO,
                monitor == null ? null : new SubProgressMonitor(monitor, 5));
        File sourceWar = file.getLocation().toFile();
        IFolder destFolder = workflowProject.getDeployFolder().getFolder("webapps");
        File destDir = destFolder.getLocation().toFile();
        if (!destDir.exists()) {
            if (!destDir.mkdirs())
                throw new IOException("Unable to create directory: " + destDir);
        }
        File destWar = new File(destDir + "/" + warFile);
        PluginUtil.copyFile(sourceWar, destWar);
        try {
            file.delete(true, monitor == null ? null : new SubProgressMonitor(monitor, 5));
            workflowProject.getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
            if ("Problems encountered while deleting resources.".equals(ex.getMessage()))
                throw new IOException(
                        "Unable to delete temporary WAR folder: " + file.getLocation().toFile());
        }
    }

    public void unDeployCloudWar(String warFile, IProgressMonitor monitor)
            throws IOException, CoreException {
        // undeploy the war from tomcat
        IFolder destFolder = workflowProject.getDeployFolder().getFolder("webapps");
        File destDir = destFolder.getLocation().toFile();
        File oldWar = new File(destDir + "/" + warFile);
        if (oldWar.exists() && !oldWar.delete())
            throw new IOException("Cannot delete deployed war: " + oldWar);
        workflowProject.getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }

    /**
     * Update webapp jars in the context of another operation.
     *
     * @param monitor
     *            if null, will launch a stand-alone progress dialog
     */
    public void updateWebProjectJars(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        if (workflowProject.isRemote())
            return; // dependencies added in remote pom.xml

        if (workflowProject.isOsgi() || workflowProject.isWar())
            return; // TODO webapps for these project types

        if (workflowProject.isCloudProject() || !workflowProject.isCustomTaskManager()) {
            project = workflowProject.getSourceProject();
            localFolder = workflowProject.getSourceProject()
                    .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
        }
        else {
            project = workflowProject.getWebProject();
            localFolder = workflowProject.getWebContentFolder();
        }
        String filename = "MDWTaskManager_" + workflowProject.getMdwVersion() + ".zip";
        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + filename);
        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);

        if (workflowProject.isRemote() || workflowProject.isCloudProject()
                || !workflowProject.isCustomTaskManager()) {
            IFolder webInfLibFolder = localFolder.getFolder("WEB-INF/lib");
            try {
                File webInfLibDir = webInfLibFolder.getRawLocation().toFile();
                File[] jarFiles = webInfLibDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });

                IFolder webFolder = workflowProject.getSourceProject().getFolder("web");
                if (!webFolder.exists())
                    webFolder.create(true, true, monitor);
                IFolder webWebInf = webFolder.getFolder("WEB-INF");
                if (!webWebInf.exists())
                    webWebInf.create(true, true, monitor);
                IFolder webLibFolder = webWebInf.getFolder("lib");
                if (!webLibFolder.exists())
                    webLibFolder.create(true, true, monitor);
                for (File jarFile : jarFiles) {
                    IFile weblibJar = webLibFolder.getFile(jarFile.getName());
                    if (weblibJar.exists())
                        weblibJar.delete(true, monitor);
                    webInfLibFolder.getFile(jarFile.getName()).copy(weblibJar.getFullPath(), true,
                            monitor);
                }

                localFolder.getFolder("WEB-INF").delete(true, monitor);
            }
            catch (CoreException ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    /**
     * Update the mapping files in the context of another operation.
     *
     * @param monitor
     *            if null, will launch a stand-alone progress dialog
     */
    public void updateMappingFiles(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        project = workflowProject.isCloudProject() ? workflowProject.getSourceProject()
                : workflowProject.getEarProject();
        String configPath = "deploy/config";
        if (workflowProject.isOsgi()) {
            String configLoc = workflowProject.checkRequiredVersion(5, 5) ? "etc" : "deploy/config";
            configPath = workflowProject.getOsgiSettings().getResourceDir() + "/" + configLoc;
        }
        else if (workflowProject.isWar()) {
            configPath = "config";
        }
        localFolder = project.getFolder(configPath);
        String filename = (workflowProject.isCloudProject() ? "mdw-config-" : "MDWMappings_")
                + workflowProject.getMdwVersion() + ".zip";
        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + filename);
        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);

        postFilterProperties(
                workflowProject.isOsgi() ? localFolder.getFile("com.centurylink.mdw.cfg")
                        : localFolder.getFile("mdw.properties"),
                monitor);
    }

    private List<String> exclusionOverrides;

    public void updateMappingTemplates(IFolder destFolder, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        exclusionOverrides = workflowProject.isCloudProject() ? new ArrayList<String>()
                : workflowProject.getFilesToIgnore();

        project = workflowProject.isCloudProject() ? workflowProject.getSourceProject()
                : workflowProject.getEarProject();
        localFolder = destFolder;
        String filename = (workflowProject.isOsgi() ? "mdw-config-" : "MDWMappings_")
                + workflowProject.getMdwVersion() + ".zip";
        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + filename);

        // avoid overriding app-specific ApplicationProperties.xml
        if (localFolder.getFile("ApplicationProperties.xml").exists())
            exclusionOverrides.add("ApplicationProperties.xml");

        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);
        exclusionOverrides = null;

        postFilterProperties(
                workflowProject.isOsgi() ? localFolder.getFile("com.centurylink.mdw.cfg")
                        : localFolder.getFile("mdw.properties"),
                monitor);
    }

    private void postFilterProperties(IFile mdwPropsFile, IProgressMonitor monitor) {
        if (mdwPropsFile.exists()) {
            String mdwProps = new String(PluginUtil.readFile(mdwPropsFile));
            mdwProps = mdwProps
                    .replaceFirst("MDWFramework\\.ApplicationDetails-@GROUP_SEP@MdwVersion.*", "");
            mdwProps = mdwProps.replaceFirst(
                    "MDWFramework\\.ApplicationDetails@GROUP_SEP@EnvironmentName.*", "");
            mdwProps = mdwProps.replaceAll("@GROUP_SEP@", workflowProject.isOsgi() ? "-" : "/");
            mdwProps = mdwProps.replaceFirst("MDWFramework.TaskManagerWeb/dev.tm.gui.user=.*",
                    "MDWFramework.TaskManagerWeb/dev.tm.gui.user="
                            + System.getProperty("user.name"));
            mdwProps = mdwProps.replaceFirst("mdw.hub.user=.*",
                    "mdw.hub.user=" + System.getProperty("user.name"));
            // db settings
            JdbcDataSource dataSource = workflowProject.getMdwDataSource();
            if (dataSource.getJdbcUrl() != null) {
                mdwProps = mdwProps.replaceFirst("mdw.database.driver=.*",
                        "mdw.database.driver=" + dataSource.getDriver());
                mdwProps = mdwProps.replaceFirst("mdw.database.url=.*",
                        "mdw.database.url=" + dataSource.getJdbcUrl());
                mdwProps = mdwProps.replaceFirst("mdw.database.username=.*",
                        "mdw.database.username=" + dataSource.getDbUser());
                mdwProps = mdwProps.replaceFirst("mdw.database.password=.*",
                        "mdw.database.password=" + dataSource.getDbPassword());
            }
            if (workflowProject.isFilePersist()) {
                // also handle case where properties are commented out
                mdwProps = mdwProps.replaceFirst("#?mdw.asset.location=.*", "mdw.asset.location="
                        + workflowProject.getAssetDir().toString().replace('\\', '/'));
                if (workflowProject.getMdwVcsRepository().hasRemoteRepository()) {
                    mdwProps = mdwProps.replaceFirst("#?mdw.git.local.path=.*", "mdw.git.local.path="
                            + workflowProject.getProjectDir().toString().replace('\\', '/'));
                    if (workflowProject.getMdwVcsRepository().getRepositoryUrl() != null)
                        mdwProps = mdwProps.replaceFirst("#?mdw.git.remote.url=.*",
                                "mdw.git.remote.url="
                                        + workflowProject.getMdwVcsRepository().getRepositoryUrl());
                } else
                {
                    mdwProps = mdwProps.replaceFirst("mdw.git.local.path=", "# mdw.git.local.path=");
                    mdwProps = mdwProps.replaceFirst("mdw.git.remote.url=", "# mdw.git.remote.url=");
                    mdwProps = mdwProps.replaceFirst("mdw.git.branch=", "# mdw.git.branch=");
                }
            }
            else {
                mdwProps = mdwProps.replaceFirst("mdw.asset.location=", "# mdw.asset.location=");
                mdwProps = mdwProps.replaceFirst("mdw.git.local.path=", "# mdw.git.local.path=");
                mdwProps = mdwProps.replaceFirst("mdw.git.remote.url=", "# mdw.git.remote.url=");
            }

            if (workflowProject.isOsgi()) {
                mdwProps = mdwProps.replaceAll("@@", "~at~@");
                StringBuffer substituted = new StringBuffer(mdwProps.length());
                Pattern tokenPattern = Pattern.compile("(@.*?@)");
                Matcher matcher = tokenPattern.matcher(mdwProps);
                int index = 0;
                while (matcher.find()) {
                    String match = matcher.group();
                    substituted.append(mdwProps.substring(index, matcher.start()));
                    String value = match.substring(1, match.length() - 1);
                    substituted.append("${").append(value).append("}");
                    index = matcher.end();
                }
                substituted.append(mdwProps.substring(index));
                mdwProps = substituted.toString();
                mdwProps = mdwProps.replaceAll("~at~", "@");

                if (workflowProject.checkRequiredVersion(5, 5)) {
                    Map<String, String> values = getPropertiesMap();
                    try {
                        mdwProps = ExpressionUtil.substitute(mdwProps, values, true);
                    }
                    catch (MDWException ex) {
                        PluginMessages.log(ex);
                    }
                }
            }

            PluginUtil.writeFile(mdwPropsFile, mdwProps, monitor);
        }
    }

    private Map<String, String> getPropertiesMap() {
        Map<String, String> propsMap = new HashMap<String, String>();
        ServerSettings serverSettings = workflowProject.getServerSettings();
        JdbcDataSource mdwDataSource = workflowProject.getMdwDataSource();
        propsMap.put("USER_NAME", System.getProperty("user.name"));
        propsMap.put("CONTAINER", serverSettings.getContainerName());
        propsMap.put("CONTAINER_VERSION", serverSettings.getContainerVersion());
        propsMap.put("CONTAINER_HOME", serverSettings.getHomeWithFwdSlashes());
        propsMap.put("NAMING_PROVIDER", serverSettings.getNamingProvider());
        propsMap.put("DATASOURCE_PROVIDER", serverSettings.getDataSourceProvider());
        propsMap.put("JMS_PROVIDER", serverSettings.getJmsProvider());
        propsMap.put("THREADPOOL_PROVIDER", serverSettings.getThreadPoolProvider());
        propsMap.put("MESSENGER", serverSettings.getMessenger());
        propsMap.put("SERVER_ROOT", serverSettings.getServerLocWithFwdSlashes());
        propsMap.put("SERVER_HOST", serverSettings.getHost());
        propsMap.put("SERVER_PORT", String.valueOf(serverSettings.getPort()));
        propsMap.put("SERVER_USER", serverSettings.getUser());
        propsMap.put("SERVER_PASSWORD", serverSettings.getPassword());
        propsMap.put("JAVA_HOME", serverSettings.getJdkHomeWithFwdSlashes());
        propsMap.put("APP_DIR", mdwSettings.getWorkspaceDirectory());
        propsMap.put("APP_NAME", workflowProject.getSourceProjectName());
        propsMap.put("SERVER_APP_SUBDIR", "mdw");
        propsMap.put("MDW_JDBC_URL", mdwDataSource.getJdbcUrl());
        propsMap.put("MDW_DB_USER", mdwDataSource.getDbUser());
        propsMap.put("MDW_DB_PASSWORD", mdwDataSource.getDbPassword());
        propsMap.put("MDW_DB_POOLSIZE", "5");
        propsMap.put("MDW_DB_POOLMAXIDLE", "3");
        propsMap.put("GENERIC_MDW_BUS_TOPIC", "Q.*.ORDEH.MDW.@USER_NAME@");
        propsMap.put("DQNAME_GENERIC_MDW_BUS_TOPIC", "LOCAL_ORDEH_MDW_@USER_NAME@");
        propsMap.put("GENERIC_MDW_LISTENER_COUNT", "1");
        propsMap.put("GENERIC_MDW_BUS_URI", "rvd://239.75.2.3:7523/denvzd.qwest.net:7523");
        propsMap.put("DEFAULT_BUS_URI", "rvd://239.75.2.3:7523/denvzd.qwest.net:7523");
        propsMap.put("LDAP_HOST", "ldapt.dev.qintra.com");
        propsMap.put("MDW_HUB_URL", serverSettings.getUrlBase() + "/MDWHub");
        propsMap.put("SERVICES_URL",
                serverSettings.getUrlBase() + "/" + workflowProject.getMdwWebProjectName());
        propsMap.put("REPORTS_URL", serverSettings.getUrlBase() + "/MDWReports");
        propsMap.put("HELPERS_URL",
                serverSettings.getUrlBase() + "/" + workflowProject.getMdwWebProjectName());
        propsMap.put("TASK_MANAGER_URL",
                serverSettings.getUrlBase() + "/" + workflowProject.getTaskManagerWebProjectName());
        propsMap.put("WORKFLOW_SNAPSHOT_IMAGE_URL", serverSettings.getUrlBase() + "/"
                + workflowProject.getDesignerWebProjectName() + "/servlet/imageServlet");
        propsMap.put("ATTACHMENTS_STORAGE_LOCATION", "C:/temp/");
        propsMap.put("MANAGED_SERVER_LIST", "localhost:8181");

        return propsMap;
    }

    /**
     * Update APP-INF/lib jars from the MDW Release site.
     *
     * @param monitor
     *            if null, will launch a stand-alone progress dialog
     */
    public void addAppLibs(String zipFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        if (workflowProject.isCloudProject()) {
            project = workflowProject.getSourceProject();
            localFolder = workflowProject.getSourceProject().getFolder(new Path("deploy/ear"));
        }
        else {
            project = workflowProject.getEarProject();
            localFolder = workflowProject.getEarContentFolder();
        }

        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + zipFile);
        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);
    }

    public void removeAppLibs(String zipFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        if (workflowProject.isCloudProject()) {
            project = workflowProject.getSourceProject();
            localFolder = workflowProject.getSourceProject().getFolder(new Path("deploy/ear"));
        }
        else {
            project = workflowProject.getEarProject();
            localFolder = workflowProject.getEarContentFolder();
        }

        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + zipFile);
        ZipFile zip = null;

        try {
            PluginUtil.downloadIntoProject(project, getFileUrl(), localFolder, file, "Check",
                    monitor);

            zip = new ZipFile(new File(file.getLocationURI()));
            for (Enumeration<?> entriesEnum = zip.entries(); entriesEnum.hasMoreElements();) {
                ZipEntry zipEntry = (ZipEntry) entriesEnum.nextElement();
                if (!zipEntry.isDirectory()) {
                    IFile toRemove = localFolder.getFile(new Path(zipEntry.getName()));
                    toRemove.refreshLocal(1,
                            monitor.isCanceled() ? null : new SubProgressMonitor(monitor, 25));
                    if (toRemove.exists())
                        toRemove.delete(true,
                                monitor.isCanceled() ? null : new SubProgressMonitor(monitor, 25));
                }
            }
            monitor.worked(10);
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            throw new InvocationTargetException(ex);
        }
        finally {
            if (zip != null)
                zip.close();

            SubProgressMonitor subMon = monitor.isCanceled() ? null
                    : new SubProgressMonitor(monitor, 25);
            monitor.subTask("Cleaning up");
            file.refreshLocal(1, subMon);
            if (file.exists())
                file.delete(true, subMon);
            monitor.done();
        }
    }

    public void addWebLibs(String zipFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        addWebLibs(zipFile, "MDWWeb.war", null, monitor);
    }

    public void addWebLibs(String zipFile, String webAppWarName,
            List<DescriptorUpdater> descriptorUpdaters, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        addWebLibs(zipFile, webAppWarName, descriptorUpdaters, true, true, monitor);
    }

    /**
     * Update WEB-INF/lib jars in MDWWeb.war with contents of zip from the MDW
     * Release site. (Assumes a clean MDWWeb.war without any extensions.)
     */
    public void addWebLibs(String zipFile, String webAppWarName,
            List<DescriptorUpdater> descriptorUpdaters, boolean doDownload, boolean deleteZip,
            IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        IFile webAppWar;

        if (workflowProject.isCloudProject()) {
            project = workflowProject.getSourceProject();
            localFolder = workflowProject.getSourceProject()
                    .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
            IFolder deployEarFolder = workflowProject.getSourceProject()
                    .getFolder(new Path("deploy/ear"));
            webAppWar = deployEarFolder.getFile(webAppWarName);
        }
        else {
            project = workflowProject.getEarProject();
            localFolder = workflowProject.getEarContentFolder();
            webAppWar = localFolder.getFile(webAppWarName);
        }

        if (!webAppWar.exists())
            return; // may not exist in some scenarios

        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + zipFile);
        IFile newWebAppWar = localFolder.getFile(webAppWarName + ".tmp");
        ZipFile zip = null;
        JarFile warArchive = null;
        JarOutputStream jarOut = null;
        BufferedOutputStream bufferedOut = null;

        monitor.beginTask("Building " + webAppWarName, 200);
        try {
            monitor.worked(5);
            if (doDownload)
                PluginUtil.downloadIntoProject(project, getFileUrl(), localFolder, file, "Download",
                        monitor); // 75 ticks
            else
                monitor.worked(75);

            File outFile = new File(newWebAppWar.getLocationURI());

            bufferedOut = new BufferedOutputStream(new FileOutputStream(outFile));
            jarOut = new JarOutputStream(bufferedOut);

            int read = 0;
            byte[] buf = new byte[1024];

            monitor.subTask("Add existing entries to WAR file");
            SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
            subMonitor.beginTask("Add existing entries", 500); // lots of
                                                               // entries
            subMonitor.worked(5);
            warArchive = new JarFile(new File(webAppWar.getLocationURI()));
            for (Enumeration<?> entriesEnum = warArchive.entries(); entriesEnum
                    .hasMoreElements();) {
                JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
                DescriptorUpdater dUpdater = null;
                if (descriptorUpdaters != null) {
                    for (DescriptorUpdater descriptorUpdater : descriptorUpdaters) {
                        if (descriptorUpdater.getFilePath().equals(jarEntry.getName())) {
                            dUpdater = descriptorUpdater;
                            break;
                        }
                    }
                }
                InputStream warIn = warArchive.getInputStream(jarEntry);
                if (dUpdater == null) {
                    // straight pass-through
                    jarOut.putNextEntry(jarEntry);
                    while ((read = warIn.read(buf)) != -1)
                        jarOut.write(buf, 0, read);
                }
                else {
                    jarOut.putNextEntry(new JarEntry(jarEntry.getName()));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // let the updater process the file (only works for text
                    // files)
                    while ((read = warIn.read(buf)) != -1)
                        baos.write(buf, 0, read);
                    baos.flush();
                    byte[] contents = dUpdater
                            .processContents(new String(baos.toByteArray()), monitor).getBytes();
                    jarOut.write(contents, 0, contents.length);
                }
                warIn.close();
                subMonitor.worked(1);
            }
            subMonitor.done();

            monitor.subTask("Add new entries to WAR file");
            subMonitor = new SubProgressMonitor(monitor, 50);
            subMonitor.beginTask("Add new entries", 500); // potentially lots of
                                                          // entries
            subMonitor.worked(5);
            zip = new ZipFile(new File(file.getLocationURI()));
            for (Enumeration<?> entriesEnum = zip.entries(); entriesEnum.hasMoreElements();) {
                ZipEntry zipEntry = (ZipEntry) entriesEnum.nextElement();
                try {
                    jarOut.putNextEntry(zipEntry);
                }
                catch (ZipException ex) {
                    // ignore duplicate entries
                    if (ex.getMessage().startsWith("duplicate entry:"))
                        PluginMessages.log(ex.getMessage());
                    else
                        throw ex;
                }
                InputStream zipIn = zip.getInputStream(zipEntry);
                while ((read = zipIn.read(buf)) != -1)
                    jarOut.write(buf, 0, read);
                zipIn.close();
                subMonitor.worked(1);
            }
            subMonitor.done();
        }
        finally {
            if (zip != null)
                zip.close();
            if (warArchive != null)
                warArchive.close();
            if (jarOut != null)
                jarOut.close();
            if (bufferedOut != null)
                bufferedOut.close();

            SubProgressMonitor subMon = monitor.isCanceled() ? null
                    : new SubProgressMonitor(monitor, 25);
            monitor.subTask("Cleaning up");
            if (deleteZip) {
                file.refreshLocal(1, subMon);
                file.delete(true, subMon);
            }
            webAppWar.refreshLocal(1, subMon);
            if (webAppWar.exists())
                webAppWar.delete(true, subMon);
            newWebAppWar.refreshLocal(1, subMon);
            if (newWebAppWar.exists())
                newWebAppWar.move(webAppWar.getFullPath(), true, subMon);
        }
    }

    public void removeWebLibs(String zipFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        removeWebLibs(zipFile, "MDWWeb.war", null, monitor);
    }

    public void removeWebLibs(String zipFile, String webAppWarName,
            List<DescriptorUpdater> descriptorUpdaters, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        removeWebLibs(zipFile, webAppWarName, descriptorUpdaters, true, true, monitor);
    }

    /**
     * Update WEB-INF/lib jars in MDWWeb.war to remove contents of zip from the
     * MDW Release site. (Assumes a clean MDWWeb.war without any extensions.)
     */
    public void removeWebLibs(String zipFile, String webAppWarName,
            List<DescriptorUpdater> descriptorUpdaters, boolean doDownload, boolean deleteZip,
            IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        IFile webAppWar;

        if (workflowProject.isCloudProject()) {
            project = workflowProject.getSourceProject();
            localFolder = workflowProject.getSourceProject()
                    .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
            IFolder deployEarFolder = workflowProject.getSourceProject()
                    .getFolder(new Path("deploy/ear"));
            webAppWar = deployEarFolder.getFile(webAppWarName);
        }
        else {
            project = workflowProject.getEarProject();
            localFolder = workflowProject.getEarContentFolder();
            webAppWar = localFolder.getFile(webAppWarName);
        }

        file = project.getFile(localFolder.getProjectRelativePath().toString() + "/" + zipFile);
        IFile newWebAppWar = localFolder.getFile(webAppWarName + ".tmp");
        ZipFile zip = null;
        JarFile warArchive = null;
        JarOutputStream jarOut = null;
        BufferedOutputStream bufferedOut = null;

        monitor.beginTask("Remove Web Libraries from " + webAppWarName, 200);
        try {
            monitor.worked(5);
            if (doDownload)
                PluginUtil.downloadIntoProject(project, getFileUrl(), localFolder, file, "Check",
                        monitor); // 75 ticks
            else
                monitor.worked(75);

            File outFile = new File(newWebAppWar.getLocationURI());
            monitor.subTask("Updating: " + outFile.getName());

            int read = 0;
            byte[] buf = new byte[1024];

            monitor.subTask("Find WAR entries to remove");
            SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
            subMonitor.beginTask("Find entries to remove", 500); // potentially
                                                                 // lots of
                                                                 // entries
            subMonitor.worked(5);
            List<String> zipFileList = new ArrayList<String>();
            zip = new ZipFile(new File(file.getLocationURI()));
            for (Enumeration<?> entriesEnum = zip.entries(); entriesEnum.hasMoreElements();) {
                ZipEntry zipEntry = (ZipEntry) entriesEnum.nextElement();
                zipFileList.add(zipEntry.getName());
                subMonitor.worked(1);
            }
            subMonitor.done();

            bufferedOut = new BufferedOutputStream(new FileOutputStream(outFile));
            jarOut = new JarOutputStream(bufferedOut);

            monitor.subTask("Rebuild webapp WAR file");
            subMonitor = new SubProgressMonitor(monitor, 50);
            subMonitor.beginTask("Rebuild webapp war", 500); // lots of entries
            subMonitor.worked(5);
            warArchive = new JarFile(new File(webAppWar.getLocationURI()));
            for (Enumeration<?> entriesEnum = warArchive.entries(); entriesEnum
                    .hasMoreElements();) {
                JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
                // exclude those from the zip list when repackaging
                if (!zipFileList.contains(jarEntry.getName())) {
                    DescriptorUpdater dUpdater = null;
                    if (descriptorUpdaters != null) {
                        for (DescriptorUpdater descriptorUpdater : descriptorUpdaters) {
                            if (descriptorUpdater.getFilePath().equals(jarEntry.getName())) {
                                dUpdater = descriptorUpdater;
                                break;
                            }
                        }
                    }
                    InputStream warIn = warArchive.getInputStream(jarEntry);
                    if (dUpdater == null) {
                        // straight pass-through
                        jarOut.putNextEntry(jarEntry);
                        while ((read = warIn.read(buf)) != -1)
                            jarOut.write(buf, 0, read);
                    }
                    else {
                        jarOut.putNextEntry(new JarEntry(jarEntry.getName()));
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // let the updater process the file (only works for text
                        // files)
                        while ((read = warIn.read(buf)) != -1)
                            baos.write(buf, 0, read);
                        baos.flush();
                        byte[] contents = dUpdater
                                .processContents(new String(baos.toByteArray()), monitor)
                                .getBytes();
                        jarOut.write(contents, 0, contents.length);
                    }
                    warIn.close();
                }
                subMonitor.worked(1);
            }
            subMonitor.done();
        }
        finally {
            if (zip != null)
                zip.close();
            if (warArchive != null)
                warArchive.close();
            if (jarOut != null)
                jarOut.close();
            if (bufferedOut != null)
                bufferedOut.close();

            SubProgressMonitor subMon = monitor.isCanceled() ? null
                    : new SubProgressMonitor(monitor, 25);
            monitor.subTask("Cleaning up");
            if (deleteZip) {
                file.refreshLocal(1, subMon);
                file.delete(true, subMon);
            }
            webAppWar.refreshLocal(1, subMon);
            if (webAppWar.exists())
                webAppWar.delete(true, subMon);
            newWebAppWar.refreshLocal(1, subMon);
            if (newWebAppWar.exists())
                newWebAppWar.move(webAppWar.getFullPath(), true, subMon);

            monitor.done();
        }
    }

    /**
     * Download the file into the local folder in the specified project.
     */
    public void updateWithDialog() {
        Shell shell = MdwPlugin.getShell();
        try {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
            pmDialog.run(true, true, this);
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(shell, ex, "MDW Update", workflowProject);
        }
        catch (InterruptedException ex) {
            PluginMessages.log(ex);
            MessageDialog.openError(shell, "MDW Update", "Update cancelled");
        }
    }

    public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Updating project " + project.getName() + "...", 130);
        monitor.worked(10);
        try {
            try {
                PluginUtil.downloadIntoProject(project, getFileUrl(), localFolder, file, "Download",
                        monitor);
                if (unzip)
                    PluginUtil.unzipProjectResource(
                            project, file, exclusionOverrides == null
                                    ? workflowProject.getFilesToIgnore() : exclusionOverrides,
                            localFolder, monitor);
            }
            catch (InterruptedException ex) {
                // download cancelled
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Update Project", workflowProject);
                throw new InvocationTargetException(ex);
            }
            finally {
                file.refreshLocal(1,
                        monitor.isCanceled() ? null : new SubProgressMonitor(monitor, 5));
                if (unzip && file.exists())
                    file.delete(true,
                            monitor.isCanceled() ? null : new SubProgressMonitor(monitor, 5));
            }
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }
    }

    protected URL getFileUrl() throws IOException {
        if (workflowProject.isCloudProject())
            return getRepositoryFileUrl("mdw");
        else
            return getReleaseFileUrl();
    }

    protected URL getReleaseFileUrl() throws IOException {
        String baseUrl = mdwSettings.getMdwReleasesUrl();
        if (!baseUrl.endsWith("/"))
            baseUrl += "/";

        String subpath = "";
        if (!workflowProject.isWar())
            subpath = "javaee/";
        URL fileUrl = new URL(
                baseUrl + subpath + workflowProject.getMdwVersion() + "/" + file.getName());
        if (((HttpURLConnection) fileUrl.openConnection()).getResponseCode() == 404
                && MdwPlugin.getDefault().getPreferenceStore()
                        .getBoolean(PreferenceConstants.PREFS_INCLUDE_PREVIEW_BUILDS)) {
            PluginMessages.log("Release file not found: " + fileUrl + "\nTrying Preview location",
                    PluginMessages.INFO_MESSAGE);
            fileUrl = new URL(baseUrl + "../snapshots/" + subpath + workflowProject.getMdwVersion()
                    + "/" + file.getName());
        }
        return fileUrl;
    }

    protected URL getRepositoryFileUrl(String subpath) throws IOException {
        return getRepositoryFileUrl(subpath, workflowProject.getMdwVersion());
    }

    protected URL getRepositoryFileUrl(String subpath, String version) throws IOException {
        String baseUrl = mdwSettings.getRepositoryUrl();

        String urlPath = "com/centurylink/mdw/" + subpath + "/";

        URL fileUrl = new URL(baseUrl + urlPath + version + "/" + file.getName());

        try {
           ((HttpURLConnection)fileUrl.openConnection()).getResponseCode();
        }
        catch (UnknownHostException e) {
            baseUrl = mdwSettings.getMdwReleasesUrl();
            if (!baseUrl.endsWith("/"))
                baseUrl += "/";

            fileUrl = new URL(baseUrl + urlPath + version + "/" + file.getName());
        }
        return fileUrl;
    }

    protected JetAccess getJet(String jetFile, IProject targetProject, String targetPath) {
        JetConfig jetConfig = new JetConfig();
        jetConfig.setModel(workflowProject);
        jetConfig.setSettings(MdwPlugin.getSettings());
        jetConfig.setPluginId(MdwPlugin.getPluginId());
        jetConfig.setTargetFolder(targetProject.getName());
        jetConfig.setTargetFile(targetPath);
        jetConfig.setTemplateRelativeUri("templates/" + jetFile);
        return new JetAccess(jetConfig);
    }

    /**
     * Update .temp/deploy war from the MDW Release site.
     *
     * @param monitor
     *            if null, will launch a stand-alone progress dialog
     */
    public void addWarLib(String versionedWarFile, String warFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        unzip = false;
        project = workflowProject.getSourceProject();
        localFolder = workflowProject.getSourceProject()
                .getFolder(MdwPlugin.getSettings().getTempResourceLocation() + "/deploy");
        file = project
                .getFile(localFolder.getProjectRelativePath().toString() + "/" + versionedWarFile);
        if (monitor == null)
            updateWithDialog();
        else
            run(monitor);

        unDeployCloudWar(warFile, monitor);
        deployCloudWar(warFile, monitor);
    }

    public void removeWarLib(String warFile, IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, CoreException, IOException {
        project = workflowProject.getSourceProject();
        unDeployCloudWar(warFile, monitor);
    }

}
