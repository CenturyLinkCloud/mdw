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
package com.centurylink.mdw.plugin.preferences.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;

import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;

/**
 * Global workspace settings for the MDW plugin.
 */
public class MdwSettings implements PreferenceConstants {
    private static final String MDW_COMMON = "com/centurylink/mdw/mdw-common";
    private static final String INTERNAl_MDW_RELEASES_URL = "http://lxdenvmtc143.dev.qintra.com:7021/maven/repository/";

    private String mdwReleasesUrl;

    public String getMdwReleasesUrl() {
        return mdwReleasesUrl;
    }

    public void setMdwReleasesUrl(String s) {
        mdwReleasesUrl = s;
    }

    private boolean includePreviewBuilds;

    public boolean isIncludePreviewBuilds() {
        return includePreviewBuilds;
    }

    public void setIncludePreviewBuilds(boolean b) {
        includePreviewBuilds = b;
    }

    private String workspaceSetupUrl;

    public String getWorkspaceSetupUrl() {
        return workspaceSetupUrl;
    }

    public void setWorkspaceSetupUrl(String s) {
        workspaceSetupUrl = s;
    }

    private String assetDiscoveryUrl;
    public String getAssetDiscoveryUrl() {
        return assetDiscoveryUrl;
    }
    public void setAssetDiscoveryUrl(String s) {
        assetDiscoveryUrl = s;
    }

    private String projectDiscoveryUrl;
    public String getProjectDiscoveryUrl() {
        return projectDiscoveryUrl;
    }
    public void setProjectDiscoveryUrl(String s) {
        projectDiscoveryUrl = s;
    }

    private int httpConnectTimeout;

    public int getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public void setHttpConnectTimeout(int ms) {
        this.httpConnectTimeout = ms;
    }

    private int httpReadTimeout;

    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public void setHttpReadTimeout(int ms) {
        this.httpReadTimeout = ms;
    }

    private String smtpHost;

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String s) {
        smtpHost = s;
    }

    private int smtpPort;

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int i) {
        smtpPort = i;
    }

    private int jdbcFetchSize;

    public int getJdbcFetchSize() {
        return jdbcFetchSize;
    }

    public void setJdbcFetchSize(int i) {
        jdbcFetchSize = i;
    }

    private String copyrightNotice;

    public String getCopyrightNotice() {
        return copyrightNotice;
    }

    public void setCopyrightNotice(String s) {
        copyrightNotice = s;
    }

    private boolean compareConflictingAssetsDuringImport;

    public boolean isCompareConflictingAssetsDuringImport() {
        return compareConflictingAssetsDuringImport;
    }

    public void setCompareConflictingAssetsDuringImport(boolean b) {
        compareConflictingAssetsDuringImport = b;
    }

    private boolean allowDeleteArchivedProcesses;

    public boolean isAllowDeleteArchivedProcesses() {
        return allowDeleteArchivedProcesses;
    }

    public void setAllowDeleteArchivedProcesses(boolean b) {
        allowDeleteArchivedProcesses = b;
    }

    private boolean allowAssetNamesWithoutExtensions;

    public boolean isAllowAssetNamesWithoutExtensions() {
        return allowAssetNamesWithoutExtensions;
    }

    public void setAllowAssetNamesWithoutExtensions(boolean b) {
        allowAssetNamesWithoutExtensions = b;
    }

    private boolean validateProcessVersions;

    public boolean isValidateProcessVersions() {
        return validateProcessVersions;
    }

    public void setValidateProcessVersions(boolean b) {
        this.validateProcessVersions = b;
    }

    private boolean useEmbeddedEditorForExcelAssets;

    public boolean isUseEmbeddedEditorForExcelAssets() {
        return useEmbeddedEditorForExcelAssets;
    }

    public void setUseEmbeddedEditorForExcelAssets(boolean b) {
        useEmbeddedEditorForExcelAssets = b;
    }

    private boolean doubleClickOpensSubprocessesAndScripts;

    public boolean isDoubleClickOpensSubprocessesAndScripts() {
        return doubleClickOpensSubprocessesAndScripts;
    }

    public void setDoubleClickOpensSubprocessesAndScripts(boolean b) {
        doubleClickOpensSubprocessesAndScripts = b;
    }

    private boolean inferSmartSubprocVersionSpec;

    public boolean isInferSmartSubprocVersionSpec() {
        return inferSmartSubprocVersionSpec;
    }

    public void setInferSmartSubprocVersionSpec(boolean b) {
        inferSmartSubprocVersionSpec = b;
    }

    private int mdwReportingLevel;

    public int getMdwReportingLevel() {
        return mdwReportingLevel;
    }

    public void setMdwReportingLevel(int reportingLevel) {
        this.mdwReportingLevel = reportingLevel;
    }

    private RGB readOnlyBackground = new RGB(224, 224, 224);

    public RGB getReadOnlyBackground() {
        return readOnlyBackground;
    }

    public void setReadOnlyBackground(RGB readOnlyBg) {
        this.readOnlyBackground = readOnlyBg;
    }

    private String tempResourceLocation;

    public String getTempResourceLocation() {
        return tempResourceLocation;
    }

    public void setTempResourceLocation(String tempLoc) {
        this.tempResourceLocation = tempLoc;
    }

    private int tempFilesToKeep;

    public int getTempFilesToKeep() {
        return tempFilesToKeep;
    }

    public void setTempFilesToKeep(int tempFilesToKeep) {
        this.tempFilesToKeep = tempFilesToKeep;
    }

    private boolean loadScriptLibsOnEdit;

    public boolean isLoadScriptLibsOnEdit() {
        return loadScriptLibsOnEdit;
    }

    public void setLoadScriptLibsOnEdit(boolean loadScriptLibsOnEdit) {
        this.loadScriptLibsOnEdit = loadScriptLibsOnEdit;
    }

    private boolean warnOverrideAttrsNotCarriedForward;

    public boolean isWarnOverrideAttrsNotCarriedForward() {
        return warnOverrideAttrsNotCarriedForward;
    }

    public void setWarnOverrideAttrsNotCarriedForward(boolean warn) {
        warnOverrideAttrsNotCarriedForward = warn;
    }

    /**
     * No longer used. TODO consider adding support for 5.5+
     */
    private boolean webBasedProcessLaunch;

    public boolean isWebBasedProcessLaunch() {
        return webBasedProcessLaunch;
    }

    public void setWebBasedProcessLaunch(boolean b) {
        webBasedProcessLaunch = b;
    }

    private ServerConsoleSettings serverConsoleSettings;

    public ServerConsoleSettings getServerConsoleSettings() {
        if (serverConsoleSettings == null) {
            serverConsoleSettings = new ServerConsoleSettings();
            serverConsoleSettings.initialize();
        }
        return serverConsoleSettings;
    }

    private boolean logTimings;

    public boolean isLogTimings() {
        return logTimings;
    }

    public void setLogTimings(boolean log) {
        this.logTimings = log;
    }

    private boolean logConnectErrors;

    public boolean isLogConnectErrors() {
        return logConnectErrors;
    }

    public void setLogConnectErrors(boolean log) {
        this.logConnectErrors = log;
    }

    private boolean swingLaunchEventManager;

    public boolean isSwingLaunchEventManager() {
        return swingLaunchEventManager;
    }

    private boolean swingLaunchThreadPoolManager;

    public boolean isSwingLaunchThreadPoolManager() {
        return swingLaunchThreadPoolManager;
    }

    private boolean useDiscoveredVcsCredentials;

    public boolean isUseDiscoveredVcsCredentials() {
        return useDiscoveredVcsCredentials;
    }

    public MdwSettings() {
        initialize();
    }

    public String getWorkspaceDirectory() {
        return ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
    }

    public void initialize() {
        setDefaultValues();

        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();

        String relUrl = store.getString(PREFS_MDW_RELEASES_URL);
        if (relUrl.endsWith("/"))
            relUrl = relUrl.substring(0, relUrl.length() - 1);
        setMdwReleasesUrl(relUrl);
        if (getMdwReleasesUrl().length() == 0)
            setMdwReleasesUrl(store.getDefaultString(PREFS_MDW_RELEASES_URL));
        setWorkspaceSetupUrl(store.getString(PREFS_WORKSPACE_SETUP_URL));
        if (getWorkspaceSetupUrl().length() == 0)
            setWorkspaceSetupUrl(store.getDefaultString(PREFS_WORKSPACE_SETUP_URL));
        setAssetDiscoveryUrl(store.getString(PREFS_ASSET_DISCOVERY_URL));
        if (getAssetDiscoveryUrl().length() == 0)
            setAssetDiscoveryUrl(store.getDefaultString(PREFS_DEFAULT_ASSET_DISCOVERY_URL));
        setProjectDiscoveryUrl(store.getString(PREFS_PROJECT_DISCOVERY_URL));
        if (getProjectDiscoveryUrl().length() == 0)
            setProjectDiscoveryUrl(store.getDefaultString(PREFS_PROJECT_DISCOVERY_URL));
        setIncludePreviewBuilds(store.getBoolean(PREFS_INCLUDE_PREVIEW_BUILDS));
        setJdbcFetchSize(store.getInt(PREFS_JDBC_FETCH_SIZE));
        if (getJdbcFetchSize() == 0)
            setJdbcFetchSize(store.getDefaultInt(PREFS_JDBC_FETCH_SIZE));
        setCopyrightNotice(store.getString(PREFS_COPYRIGHT_NOTICE));
        if (getCopyrightNotice().length() == 0)
            setCopyrightNotice(store.getDefaultString(PREFS_COPYRIGHT_NOTICE));
        setCompareConflictingAssetsDuringImport(store.getBoolean(PREFS_COMPARE_CONFLICTING_ASSETS));
        setAllowDeleteArchivedProcesses(store.getBoolean(PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES));
        setAllowAssetNamesWithoutExtensions(store.getBoolean(PREFS_ALLOW_ASSETS_WITHOUT_EXTENSIONS));
        setValidateProcessVersions(store.getBoolean(PREFS_VALIDATE_PROCESS_VERSIONS));
        setUseEmbeddedEditorForExcelAssets(store.getBoolean(PREFS_EMBEDDED_EDITOR_FOR_EXCEL));
        setDoubleClickOpensSubprocessesAndScripts(
                store.getBoolean(PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS));
        setInferSmartSubprocVersionSpec(store.getBoolean(PREFS_INFER_SMART_SUBPROC_VERSION_SPEC));
        setMdwReportingLevel(store.getInt(PREFS_MDW_REPORTING_LEVEL));
        int red = store.getInt(PREFS_READONLY_BG_RED);
        int green = store.getInt(PREFS_READONLY_BG_GREEN);
        int blue = store.getInt(PREFS_READONLY_BG_BLUE);
        setReadOnlyBackground(new RGB(red, green, blue));
        setTempResourceLocation(store.getString(PREFS_TEMP_RESOURCE_DIRECTORY));
        if (getTempResourceLocation().length() == 0)
            setTempResourceLocation(store.getDefaultString(PREFS_TEMP_RESOURCE_DIRECTORY));
        setTempFilesToKeep(store.getInt(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP));
        if (getTempFilesToKeep() == 0)
            setTempFilesToKeep(store.getDefaultInt(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP));
        setLoadScriptLibsOnEdit(store.getBoolean(PREFS_LOAD_SCRIPT_LIBS_ON_EDIT));
        setWarnOverrideAttrsNotCarriedForward(
                store.getBoolean(PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD));

        logTimings = store.getBoolean(PREFS_LOG_TIMINGS);
        logConnectErrors = store.getBoolean(PREFS_LOG_CONNECT_ERRORS);

        swingLaunchEventManager = store.getBoolean(PREFS_SWING_LAUNCH_EVENT_MANAGER);
        swingLaunchThreadPoolManager = store.getBoolean(PREFS_SWING_LAUNCH_THREAD_POOL_MANAGER);

        useDiscoveredVcsCredentials = store.getBoolean(PREFS_USE_DISCOVERED_VCS_CREDS);

        setHttpConnectTimeout(store.getInt(PREFS_HTTP_CONNECT_TIMEOUT_MS));
        setHttpReadTimeout(store.getInt(PREFS_HTTP_READ_TIMEOUT_MS));
        setSmtpHost(store.getString(PREFS_SMTP_HOST));
        setSmtpPort(store.getInt(PREFS_SMTP_PORT));
    }

    public boolean isComplete() {
        return getMdwReleasesUrl() != null && getMdwReleasesUrl().length() > 0
                && getWorkspaceSetupUrl() != null && getWorkspaceSetupUrl().length() > 0
                && getAssetDiscoveryUrl() != null && getAssetDiscoveryUrl().length() > 0
                && getProjectDiscoveryUrl() != null && getProjectDiscoveryUrl().length() > 0
                && getSmtpHost() != null && getSmtpHost().length() > 0 && getSmtpPort() > 0;
    }

    public static void setDefaultValues() {
        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        store.setDefault(PREFS_MDW_RELEASES_URL, PREFS_DEFAULT_MDW_RELEASES_URL);
        store.setDefault(PREFS_WORKSPACE_SETUP_URL, PREFS_DEFAULT_WORKSPACE_SETUP_URL);
        store.setDefault(PREFS_ASSET_DISCOVERY_URL, PREFS_DEFAULT_ASSET_DISCOVERY_URL);
        store.setDefault(PREFS_PROJECT_DISCOVERY_URL, PREFS_DEFAULT_PROJECT_DISCOVERY_URL);
        store.setDefault(PREFS_HTTP_CONNECT_TIMEOUT_MS, Server.DEFAULT_CONNECT_TIMEOUT);
        store.setDefault(PREFS_HTTP_READ_TIMEOUT_MS, Server.DEFAULT_READ_TIMEOUT);
        store.setDefault(PREFS_JDBC_FETCH_SIZE, PREFS_DEFAULT_JDBC_FETCH_SIZE);
        store.setDefault(PREFS_COPYRIGHT_NOTICE, PREFS_DEFAULT_COPYRIGHT_NOTICE);
        store.setDefault(PREFS_MDW_REPORTING_LEVEL, PREFS_DEFAULT_MDW_REPORTING_LEVEL);
        store.setDefault(PREFS_TEMP_RESOURCE_DIRECTORY, PREFS_DEFAULT_TEMP_RESOURCE_DIRECTORY);
        store.setDefault(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP,
                PREFS_DEFAULT_PREV_TEMP_FILE_VERSIONS);
        store.setDefault(PREFS_SMTP_HOST, PREFS_DEFAULT_SMTP_HOST);
        store.setDefault(PREFS_SMTP_PORT, PREFS_DEFAULT_SMTP_PORT);
        store.setDefault(PREFS_SWING_LAUNCH_EVENT_MANAGER, true);
        store.setDefault(PREFS_SWING_LAUNCH_THREAD_POOL_MANAGER, true);
        store.setDefault(PREFS_USE_DISCOVERED_VCS_CREDS, true);
        store.setDefault(PREFS_LOAD_SCRIPT_LIBS_ON_EDIT, false);
        store.setDefault(PREFS_READONLY_BG_RED, 224);
        store.setDefault(PREFS_READONLY_BG_GREEN, 224);
        store.setDefault(PREFS_READONLY_BG_BLUE, 224);
        store.setDefault(PREFS_EMBEDDED_EDITOR_FOR_EXCEL, !MdwPlugin.isRcp());
        store.setDefault(PREFS_INFER_SMART_SUBPROC_VERSION_SPEC, true);
        store.setDefault(PREFS_COMPARE_CONFLICTING_ASSETS, true);
        store.setDefault(PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD, true);
    }

    public List<String> getMdwVersions() {
        return getMdwVersions(MDW_COMMON);
    }

    public List<String> getMdwVersions(String projectPath) {
        List<String> versions = new ArrayList<String>();
        String releasesUrl;
        boolean snapshotBuilds = MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PREFS_INCLUDE_PREVIEW_BUILDS);
        try {
            releasesUrl = INTERNAl_MDW_RELEASES_URL + projectPath;
            String releasesPage = PluginUtil.downloadContent(new URL(releasesUrl));
            if (versions.isEmpty()) {
                String start = "<tt>";
                String release = "[\\d\\.]*(\\-SNAPSHOT)?";
                if (!snapshotBuilds)
                    release = "[\\d\\.]*";
                String end = "/</tt>";

                versions = findReleases(releasesPage, start, release, end);
            }
            if (versions.isEmpty())
                throw new IOException("Unable to locate any MDW releases at: " + releasesUrl);
        }
        catch (UnknownHostException e) {
            try {
                releasesUrl = getMdwReleasesUrl();
                if (!releasesUrl.endsWith("/"))
                    releasesUrl += "/";
                releasesUrl = releasesUrl + projectPath;
                String releasesPage = PluginUtil.downloadContent(new URL(releasesUrl));
                if (versions.isEmpty()) {
                    // try maven-central list format
                    String start = "<a href=\"";
                    String release = "[\\d\\.]*(\\-SNAPSHOT)?";
                    if (!snapshotBuilds)
                        release = "[\\d\\.]*";
                    String end = "/\"";
                    versions = findReleases(releasesPage, start, release, end);
                }
                if (versions.isEmpty())
                    throw new IOException(
                            "Unable to locate any MDW releases at: http://repo.maven.apache.org/maven2");
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
        }

        return versions;
    }

    private List<String> findReleases(String page, String token, String releaseFormat,
            String endToken) {
        Pattern pattern = Pattern.compile(token + releaseFormat + endToken);
        Matcher matcher = pattern.matcher(page);

        List<String> releases = new ArrayList<String>();
        while (matcher.find()) {
            String match = matcher.group();
            String release = match.substring(token.replaceAll("\\\\", "").length(),
                    match.length() - endToken.replaceAll("\\\\", "").length());
            if (!releases.contains(release) && !"..".equals(release))
                releases.add(release);
        }
        return releases;
    }

    public String getLatestMdwVersion() {
        List<String> versions = getMdwVersions();
        if (versions.isEmpty())
            return "";
        return Collections.max(versions);
    }

}
