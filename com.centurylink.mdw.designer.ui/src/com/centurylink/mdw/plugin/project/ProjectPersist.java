/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class ProjectPersist extends DefaultHandler {
    public static final String SETTINGS_FILE = "com.centurylink.mdw.plugin.xml";
    public static final String LEGACY_SETTINGS_FILE = "com.qwest.mdw.plugin.attributes";

    public static final String MDW_SERVER_TYPE = "mdwServerType";
    public static final String MDW_SERVER_HOME = "mdwServerHome";
    public static final String MDW_SERVER_LOCATION = "mdwServerLocation";
    public static final String MDW_SERVER_USER = "mdwServerUser";
    public static final String MDW_SERVER_PASSWORD = "mdwServerPassword";
    public static final String MDW_SERVER_JAVA_OPTS = "mdwServerJavaOpts";
    public static final String MDW_SERVER_DEBUG = "mdwServerDebug";
    public static final String MDW_SERVER_DEBUG_PORT = "mdwServerDebugPort";
    public static final String MDW_SERVER_SUSPEND = "mdwServerSuspend";
    public static final String LOG_WATCHER_PORT = "mdwLogWatcherPort";
    public static final String LOG_WATCHER_TIMEOUT = "mdwLogWatcherTimeout";
    public static final String STUB_SERVER_PORT = "mdwStubServerPort";
    public static final String STUB_SERVER_TIMEOUT = "mdwStubServerTimeout";
    public static final String MDW_SERVER_VERSION = "mdwServerVersion";
    public static final String MDW_SERVER_HOST = "mdwServerHost";
    public static final String MDW_SERVER_PORT = "mdwServerPort";
    public static final String MDW_SERVER_CMD_PORT = "mdwServerCommandPort";
    public static final String MDW_SERVER_JDK_HOME = "mdwServerJdkHome";
    public static final String MDW_DB_DRIVER = "mdwJdbcDriver";
    public static final String MDW_DB_URL = "mdwJdbcUrl";
    public static final String MDW_DB_USER = "mdwJdbcUser";
    public static final String MDW_PERSIST_TYPE = "mdwPersistType";
    public static final String MDW_VCS_GIT = "mdwVcsGit";
    public static final String MDW_VCS_REPO_URL = "mdwVcsRepoUrl";
    public static final String MDW_VCS_USER = "mdwVcsUser";
    public static final String MDW_VCS_SYNC_ARCHIVE = "mdwVcsSyncArchive";
    public static final String MDW_UPDATE_SERVER_CACHE = "mdwUpdateServerCache";

    public static final int DEFAULT_STUB_PORT = 7183;
    public static final int DEFAULT_STUB_TIMEOUT = 120;

    public static final int DEFAULT_LOG_PORT = 7181;
    public static final int DEFAULT_LOG_TIMEOUT = 120;

    WorkflowProject workflowProject;

    public ProjectPersist(WorkflowProject project) {
        this.workflowProject = project;
    }

    /**
     * Read the settings file and parse into workflow project model object.
     * 
     * @param settingsFile
     * @param project
     *            for storing personalized settings
     * @return populated workflow project
     */
    public WorkflowProject read(IProject project) throws CoreException {
        IFile settingsFile = project.getFile(".settings/" + SETTINGS_FILE);
        if (!settingsFile.exists()) {
            // try legacy file
            IFile legacySettingsFile = project.getFile(".settings/" + LEGACY_SETTINGS_FILE);
            if (legacySettingsFile.exists()) {
                if (!ResourcesPlugin.getWorkspace().isTreeLocked()) {
                    legacySettingsFile.move(new Path(SETTINGS_FILE), true,
                            new NullProgressMonitor());
                    settingsFile = project.getFile(".settings/" + SETTINGS_FILE);
                }
                else {
                    // locked for updates so read the old settings file
                    settingsFile = legacySettingsFile;
                }
            }
        }

        if (!settingsFile.exists())
            return null; // project does not have MDW attributes
        settingsFile.refreshLocal(IResource.DEPTH_ZERO, null);
        workflowProject = fromStream(settingsFile.getContents());
        retrieveProjectPrefs(project);
        return workflowProject;
    }

    public WorkflowProject fromStream(InputStream inStream) {
        InputSource src = new InputSource(inStream);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        try {
            // hold these until everything else is parsed
            final Map<String, Map<String, String>> extensionMods = new HashMap<String, Map<String, String>>();

            workflowProject.setDefaultFilesToIgnoreDuringUpdate();

            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(src, new DefaultHandler() {
                // attributes for workflow project
                public void startElement(String uri, String localName, String qName,
                        Attributes attrs) throws SAXException {
                    if (qName.equals("mdw-workflow")) {
                        // outer element
                    }
                    else if (qName.equals("sourceProject")) {
                        workflowProject.setSourceProjectName(attrs.getValue("name"));
                        workflowProject.setAuthor(attrs.getValue("author"));
                        workflowProject
                                .setProduction(Boolean.parseBoolean(attrs.getValue("production")));
                    }
                    else if (qName.equals("mdwFramework")) {
                        workflowProject.setMdwVersion(attrs.getValue("version"));
                    }
                    else if (qName.equals("webLogic")) {
                        workflowProject.getServerSettings().setHost(attrs.getValue("host"));
                        workflowProject.getServerSettings()
                                .setPort(Integer.parseInt(attrs.getValue("port")));
                    }
                    else if (qName.equals("server")) {
                        String container = attrs.getValue("container");
                        if (container != null)
                            workflowProject.getServerSettings()
                                    .setContainerType(ContainerType.valueOf(container));
                        workflowProject.getServerSettings().setHost(attrs.getValue("host"));
                        workflowProject.getServerSettings()
                                .setPort(Integer.parseInt(attrs.getValue("port")));
                        workflowProject.setWebContextRoot(attrs.getValue("contextRoot"));
                    }
                    else if (qName.equals("database")) {
                        workflowProject.getMdwDataSource()
                                .setJdbcUrlWithCredentials(attrs.getValue("jdbcUrl"));
                        workflowProject.getMdwDataSource()
                                .setSchemaOwner(attrs.getValue("schemaOwner"));
                    }
                    else if (qName.equals("repository")) {
                        workflowProject.getMdwVcsRepository()
                                .setProvider(attrs.getValue("provider"));
                        String repoUrl = attrs.getValue("url");
                        if (repoUrl != null && repoUrl.trim().length() > 0)
                            workflowProject.getMdwVcsRepository()
                                    .setRepositoryUrlWithCredentials(repoUrl);
                        String branch = attrs.getValue("branch");
                        if (branch != null && branch.trim().length() > 0)
                            workflowProject.getMdwVcsRepository().setBranch(branch);
                        String localPath = attrs.getValue("localPath");
                        if (localPath == null || localPath.trim().length() == 0)
                            localPath = attrs.getValue("localDir"); // compatibility
                        if (localPath != null && localPath.trim().length() > 0)
                            workflowProject.getMdwVcsRepository().setLocalPath(localPath);
                        String pkgPrefixes = attrs.getValue("pkgPrefixes");
                        if (pkgPrefixes != null && pkgPrefixes.trim().length() > 0)
                            workflowProject.getMdwVcsRepository().setPkgPrefixes(
                                    PluginUtil.arrayToList(pkgPrefixes.split("\\s*,\\s*")));
                    }
                    else if (qName.equals("filesToIgnore")) {
                        workflowProject
                                .setFilesToIgnoreDuringUpdate(attrs.getValue("duringUpdate"));
                    }
                    else {
                        // extension modules (need fully parsed workflowProject,
                        // so save)
                        Map<String, String> attrMap = new HashMap<String, String>();
                        for (int i = 0; i < attrs.getLength(); i++)
                            attrMap.put(attrs.getQName(i), attrs.getValue(i));
                        extensionMods.put(qName, attrMap);
                    }
                }
            });
            inStream.close();

            workflowProject.setRemote(workflowProject.getMdwVersion() == null); // null
                                                                                // version
                                                                                // must
                                                                                // be
                                                                                // remote
            if ("Git".equals(workflowProject.getMdwVcsRepository().getProvider()))
                workflowProject.setPersistType(PersistType.Git);
            else if (workflowProject.getMdwDataSource().getJdbcUrl() != null)
                workflowProject.setPersistType(PersistType.Database);
            else
                workflowProject.setPersistType(PersistType.None);

            if (!workflowProject.isRemote()) {
                // process extensions
                for (ExtensionModule extension : WorkflowProjectManager.getInstance()
                        .getAvailableExtensions(workflowProject)) {
                    for (String extensionName : extensionMods.keySet())
                        extension.readConfigElement(extensionName, extensionMods.get(extensionName),
                                workflowProject);
                }
            }
            else {
                // undo defaults
                workflowProject.setFilesToIgnoreDuringUpdate(null);
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            return null;
        }

        return workflowProject;
    }

    /**
     * Persist settings file from workflowProject.
     * 
     * @param settingsFile
     * @param project
     *            for storing personalized settings
     */
    public void write(IProject project) {
        IFile settingsFile = project.getFile(".settings/" + SETTINGS_FILE);
        PluginUtil.writeFile(settingsFile, toXml(), null);
        saveProjectPrefs(project);
    }

    public String toXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<mdw-workflow>\n");
        sb.append("  <sourceProject name=\"" + workflowProject.getSourceProjectName()
                + "\" author=\"" + workflowProject.getAuthor() + "\"");
        if (workflowProject.isProduction())
            sb.append(" production=\"true\"");
        sb.append("/>\n");

        for (ExtensionModule extension : workflowProject.getExtensionModules()) {
            String extensionElementText = extension.writeConfigElement(workflowProject);
            if (extensionElementText != null)
                sb.append(extensionElementText);
        }

        if (!workflowProject.isRemote()) // remote projects get version
                                         // dynamically from server
            sb.append("  <mdwFramework version=\"" + workflowProject.getMdwVersion() + "\"/>\n");

        ServerSettings serverSettings = workflowProject.getServerSettings();
        sb.append("  <server host=\"" + serverSettings.getHost() + "\" port=\""
                + serverSettings.getPort() + "\"");
        if (workflowProject.isRemote())
            sb.append(" contextRoot=\"" + workflowProject.getWebContextRoot() + "\"");
        else {
            if (isMakeLocal())
                sb.append(" contextRoot=\"" + workflowProject.getWebContextRoot() + "\"");
            else {
                ContainerType containerType = workflowProject.getServerSettings()
                        .getContainerType();
                if (containerType != null)
                    sb.append(" container=\"" + containerType.toString() + "\"");
            }
        }
        sb.append("/>\n");

        if (workflowProject.getPersistType() == PersistType.Git) {
            sb.append("  <repository");
            VcsRepository repo = workflowProject.getMdwVcsRepository();
            sb.append(" provider=\"" + repo.getProvider() + "\"");
            if (repo.getRepositoryUrl() != null && repo.getRepositoryUrl().trim().length() > 0)
                sb.append(" url=\"").append(repo.getRepositoryUrlWithEncryptedCredentials() + "\"");
            if (repo.getBranch() != null && repo.getBranch().trim().length() > 0)
                sb.append(" branch=\"").append(repo.getBranch() + "\"");
            if (repo.getLocalPath() != null && repo.getLocalPath().trim().length() > 0)
                sb.append(" localPath=\"").append(repo.getLocalPath().replace("\\", "/") + "\"");
            if (repo.getPkgPrefixes() != null && !repo.getPkgPrefixes().isEmpty()) {
                sb.append(" pkgPrefixes=\"");
                for (int i = 0; i < repo.getPkgPrefixes().size(); i++) {
                    String prefix = repo.getPkgPrefixes().get(i).trim();
                    sb.append(prefix);
                    if (i < repo.getPkgPrefixes().size() - 1)
                        sb.append(",");
                }
                sb.append("\"");
            }
            sb.append("/>\n");
        }

        if (workflowProject.getPersistType() == PersistType.None) {
            // do nothing
        }
        else {
            sb.append("  <database jdbcUrl=\""
                    + workflowProject.getMdwDataSource().getJdbcUrlWithEncryptedCredentials()
                    + "\"");
            if (workflowProject.getMdwDataSource().getSchemaOwner() != null)
                sb.append(" schemaOwner=\"" + workflowProject.getMdwDataSource().getSchemaOwner()
                        + "\"");
            sb.append("/>\n");
        }

        if (workflowProject.getFilesToIgnore() != null)
            sb.append("  <filesToIgnore duringUpdate=\""
                    + workflowProject.getFilesToIgnoreDuringUpdate() + "\"/>\n");

        sb.append("</mdw-workflow>\n");

        return sb.toString();
    }

    public void retrieveProjectPrefs(IProject project) throws CoreException {
        ServerSettings serverSettings = workflowProject.getServerSettings();

        String home = getPersistentProperty(project, MDW_SERVER_HOME);
        if (home != null)
            serverSettings.setHome(home);
        String jdkHome = getPersistentProperty(project, MDW_SERVER_JDK_HOME);
        if (jdkHome != null)
            serverSettings.setJdkHome(jdkHome);
        String location = getPersistentProperty(project, MDW_SERVER_LOCATION);
        if (location != null)
            serverSettings.setServerLoc(location);
        String cmdPort = getPersistentProperty(project, MDW_SERVER_CMD_PORT);
        if (cmdPort != null)
            serverSettings.setCommandPort(Integer.parseInt(cmdPort));
        String user = getPersistentProperty(project, MDW_SERVER_USER);
        if (user != null) {
            serverSettings.setUser(user);
            String encPass = getPersistentProperty(project, MDW_SERVER_PASSWORD);
            if (encPass != null) {
                try {
                    serverSettings.setPassword(CryptUtil.decrypt(encPass));
                }
                catch (GeneralSecurityException ex) {
                    PluginMessages.log(ex);
                }
            }
        }

        String serverJavaOpts = getPersistentProperty(project, MDW_SERVER_JAVA_OPTS);
        if (serverJavaOpts != null)
            serverSettings.setJavaOptions(serverJavaOpts);

        if (Boolean.parseBoolean(getPersistentProperty(project, MDW_SERVER_DEBUG))) {
            serverSettings.setDebug(true);
            String debugPort = getPersistentProperty(project, MDW_SERVER_DEBUG_PORT);
            if (debugPort != null)
                serverSettings.setDebugPort(Integer.parseInt(debugPort));
            if (serverSettings.getDebugPort() == 0)
                serverSettings.setDebugPort(8500);
            serverSettings.setSuspend(
                    Boolean.parseBoolean(getPersistentProperty(project, MDW_SERVER_SUSPEND)));
        }

        // log watcher and stub host are hardwired to localhost
        try {
            String host = InetAddress.getLocalHost().getHostName();
            serverSettings.setLogWatcherHost(host);
            serverSettings.setStubServerHost(host);
        }
        catch (UnknownHostException ex) {
            PluginMessages.log(ex);
        }

        int logPort = DEFAULT_LOG_PORT;
        String logPortStr = getPersistentProperty(project, LOG_WATCHER_PORT);
        if (logPortStr != null && !logPortStr.equals("0"))
            logPort = Integer.parseInt(logPortStr);
        serverSettings.setLogWatcherPort(logPort);

        int logTimeout = DEFAULT_LOG_TIMEOUT;
        String logTimeoutStr = getPersistentProperty(project, LOG_WATCHER_TIMEOUT);
        if (logTimeoutStr != null)
            logTimeout = Integer.parseInt(logTimeoutStr);
        serverSettings.setLogWatcherTimeout(logTimeout);

        int stubPort = DEFAULT_STUB_PORT;
        String stubPortStr = getPersistentProperty(project, STUB_SERVER_PORT);
        if (stubPortStr != null && !stubPortStr.equals("0"))
            stubPort = Integer.parseInt(stubPortStr);
        serverSettings.setStubServerPort(stubPort);

        int stubTimeout = DEFAULT_STUB_TIMEOUT;
        String stubTimeoutStr = getPersistentProperty(project, STUB_SERVER_TIMEOUT);
        if (stubTimeoutStr != null)
            stubTimeout = Integer.parseInt(stubTimeoutStr);
        serverSettings.setStubServerTimeout(stubTimeout);

        if (workflowProject.getPersistType() == PersistType.Git) {
            String archiveSync = getPersistentProperty(workflowProject.getSourceProject(),
                    MDW_VCS_SYNC_ARCHIVE);
            workflowProject.getMdwVcsRepository()
                    .setSyncAssetArchive("true".equalsIgnoreCase(archiveSync));
        }

        String refr = getPersistentProperty(workflowProject.getSourceProject(),
                MDW_UPDATE_SERVER_CACHE);
        workflowProject.setUpdateServerCache(!"false".equalsIgnoreCase(refr));
    }

    /**
     * Preferences may be local to a particular user, so save with workspace
     * prefs
     */
    public void saveProjectPrefs(IProject project) {
        ServerSettings serverSettings = workflowProject.getServerSettings();

        try {
            setPersistentProperty(project, MDW_SERVER_HOME, serverSettings.getHome());
            setPersistentProperty(project, MDW_SERVER_JDK_HOME, serverSettings.getJdkHome());
            setPersistentProperty(project, MDW_SERVER_LOCATION, serverSettings.getServerLoc());
            int cmdPort = serverSettings.getCommandPort();
            if (cmdPort != 0)
                setPersistentProperty(project, MDW_SERVER_CMD_PORT, String.valueOf(cmdPort));
            String user = serverSettings.getUser();
            setPersistentProperty(project, MDW_SERVER_USER, user);
            if (user != null) {
                String password = serverSettings.getPassword();
                if (password != null) {
                    try {
                        setPersistentProperty(project, MDW_SERVER_PASSWORD,
                                CryptUtil.encrypt(password));
                    }
                    catch (GeneralSecurityException ex) {
                        PluginMessages.log(ex);
                    }
                }
            }

            if (!workflowProject.isRemote()) {
                // default prefs for server type
                setPersistentProperty(MDW_SERVER_TYPE,
                        serverSettings.getContainerType().toString());
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_HOME,
                        serverSettings.getHome());
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_JDK_HOME,
                        serverSettings.getJdkHome());
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_HOST,
                        serverSettings.getHost());
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_PORT,
                        String.valueOf(
                                serverSettings.getPort() == 0 ? "" : serverSettings.getPort()));
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_CMD_PORT,
                        String.valueOf(serverSettings.getCommandPort() == 0 ? ""
                                : serverSettings.getCommandPort()));
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_USER,
                        serverSettings.getUser());
                setPersistentProperty(serverSettings.getContainerType() + "-" + MDW_SERVER_VERSION,
                        serverSettings.getContainerVersion());
                if (workflowProject.getPersistType() == PersistType.Git) {
                    VcsRepository vcsRepo = workflowProject.getMdwVcsRepository();
                    setPersistentProperty(
                            "MDW" + workflowProject.getMdwVersion() + "-" + MDW_VCS_GIT,
                            vcsRepo.getProvider());
                    setPersistentProperty(
                            "MDW" + workflowProject.getMdwVersion() + "-" + MDW_VCS_REPO_URL,
                            vcsRepo.getRepositoryUrl());
                    setPersistentProperty(
                            "MDW" + workflowProject.getMdwVersion() + "-" + MDW_VCS_USER,
                            vcsRepo.getUser());
                }
                JdbcDataSource dataSource = workflowProject.getMdwDataSource();
                setPersistentProperty("MDW" + workflowProject.getMdwVersion() + "-" + MDW_DB_DRIVER,
                        dataSource.getDriver());
                setPersistentProperty("MDW" + workflowProject.getMdwVersion() + "-" + MDW_DB_URL,
                        dataSource.getJdbcUrl());
                setPersistentProperty("MDW" + workflowProject.getMdwVersion() + "-" + MDW_DB_USER,
                        dataSource.getDbUser());
                setPersistentProperty(
                        serverSettings.getContainerType() + "-" + MDW_SERVER_JAVA_OPTS,
                        serverSettings.getJavaOptions());
            }

            if (serverSettings.isDebug()) {
                setPersistentProperty(project, MDW_SERVER_DEBUG, "true");
                setPersistentProperty(project, MDW_SERVER_DEBUG_PORT,
                        String.valueOf(serverSettings.getDebugPort()));
                setPersistentProperty(project, MDW_SERVER_SUSPEND,
                        String.valueOf(serverSettings.isSuspend()));
            }
            else {
                setPersistentProperty(project, MDW_SERVER_DEBUG, null);
            }

            setPersistentProperty(project, LOG_WATCHER_PORT,
                    String.valueOf(serverSettings.getLogWatcherPort()));
            setPersistentProperty(project, LOG_WATCHER_TIMEOUT,
                    String.valueOf(serverSettings.getLogWatcherTimeout()));
            setPersistentProperty(project, STUB_SERVER_PORT,
                    String.valueOf(serverSettings.getStubServerPort()));
            setPersistentProperty(project, STUB_SERVER_TIMEOUT,
                    String.valueOf(serverSettings.getStubServerTimeout()));

            if (workflowProject.getPersistType() == PersistType.Git) {
                setPersistentProperty(project, MDW_VCS_SYNC_ARCHIVE,
                        String.valueOf(workflowProject.getMdwVcsRepository().isSyncAssetArchive()));
            }

            setPersistentProperty(project, MDW_UPDATE_SERVER_CACHE,
                    String.valueOf(workflowProject.isUpdateServerCache()));
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }
    }

    private String getPersistentProperty(IProject project, String name) throws CoreException {
        QualifiedName qName = new QualifiedName(MdwPlugin.getPluginId(), name);
        String prop = project.getPersistentProperty(qName);
        if (prop == null || prop.trim().length() == 0)
            return null;
        return prop;
    }

    private void setPersistentProperty(IProject project, String name, String value)
            throws CoreException {
        QualifiedName qName = new QualifiedName(MdwPlugin.getPluginId(), name);
        if (value != null && value.trim().length() == 0)
            project.setPersistentProperty(qName, null);
        else
            project.setPersistentProperty(qName, value);
    }

    private void setPersistentProperty(String name, String value) {
        if (value != null)
            MdwPlugin.setStringPref(name, value);
    }

    public boolean isMakeLocal() {
        IProject project = MdwPlugin.getWorkspaceRoot().getProject(workflowProject.getName());
        if (project.exists()) {
            IFile file = project.getFile(".settings/" + ProjectPersist.SETTINGS_FILE);
            if (file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(file.getContents()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.indexOf("mdwFramework") != -1)
                            return false;
                    }
                    reader.close();
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                }
            }
            else {
                return false;
            }
        }
        return true;
    }
}
