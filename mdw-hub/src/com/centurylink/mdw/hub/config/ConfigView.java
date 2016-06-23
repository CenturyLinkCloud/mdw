/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.service.ApplicationSummaryDocument.ApplicationSummary;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.prefs.Preferences;
import com.centurylink.mdw.web.util.WebUtil;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument.ConfigManagerProjects;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;
import com.centurylink.mdw.workflow.WorkflowUrl;

public class ConfigView {
    public static final int MDW_MAJOR_VERSION_3 = 3;
    public static final int MDW_VERSION_UNKNOWN = -1;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    public static final String PROJECT_CONFIG_FILE = "ConfigManagerProjects.xml";

    private ConfigManagerProjects configManagerProjects;

    public ConfigManagerProjects getConfigManager() {
        return configManagerProjects;
    }

    private Boolean master;

    public boolean isMaster() {
        if (master == null) {
            try {
                master = FileHelper.openConfigurationFile(PROJECT_CONFIG_FILE) != null;
            }
            catch (FileNotFoundException ex) {
                master = false;
            }
        }
        return master;
    }

    private WorkflowApplication workflowApplication;
    private ApplicationSummary appSummary;

    private int mdwMajorVersion = MDW_VERSION_UNKNOWN;

    public int getMdwMajorVersion() {
        return mdwMajorVersion;
    }

    private int mdwMinorVersion = MDW_VERSION_UNKNOWN;

    public int getMdwMinorVersion() {
        return mdwMinorVersion;
    }

    private int mdwBuildNumber = MDW_VERSION_UNKNOWN;

    public int getMdwBuildNumber() {
        return mdwBuildNumber;
    }

    private String workflowApp;

    public String getWorkflowApp() {
        return workflowApp;
    }

    public void setWorkflowApp(String workflowApp) throws IOException, XmlException {
        if (configManagerProjects == null)
            loadProjects();

        this.workflowApp = workflowApp;
        this.workflowApplication = getWorkflowApplication(workflowApp);
    }

    private WorkflowEnvironment workflowEnvironment;

    private String environment;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) throws IOException, XmlException {
        if (configManagerProjects == null)
            loadProjects();

        this.environment = environment;
        this.workflowEnvironment = getWorkflowEnvironment(workflowApplication, environment);

        appSummary = loadApplicationSummary();

        // clear config manager
        ConfigManager configManager = (ConfigManager) FacesVariableUtil.getValue("configManager");
        configManager.clear();

    }

    /**
     * Loads the MDW workflow projects from the XML file on the classpath.
     */
    private void loadProjects() throws IOException, XmlException {
        String configFile = FileHelper.getConfigFile(PROJECT_CONFIG_FILE);
        ConfigManagerProjectsDocument configManagerProjectsDocument = ConfigManagerProjectsDocument.Factory.parse(configFile);
        configManagerProjects = configManagerProjectsDocument.getConfigManagerProjects();
    }

    public String getApplicationName() {
        return workflowApplication.getName();
    }

    public String getEnvironmentName() {
        return workflowEnvironment.getName();
    }

    public String getAppDetails() throws IOException, XmlException {
        if (configManagerProjects == null)
            loadProjects();

        String formalName = workflowApplication.getMalAppName();
        String acronym = workflowApplication.getEcomAcronym();
        return "Formal Name: " + formalName + "<br/>" + "Acronym: " + acronym;
    }

    public String getSysInfo() throws IOException, XmlException {
        if (configManagerProjects == null)
            loadProjects();

        String webToolsUrl = getServicesUrl();
        URL sysInfoUrl = new URL(webToolsUrl + "/system/sysInfoPlain.jsf");

        try {
            HttpHelper httpHelper = new HttpHelper(sysInfoUrl);
            // use local images, etc locations (relative URLs only)
            WebUtil webUtil = (WebUtil) FacesVariableUtil.getValue("webUtil");
            return httpHelper.get().replaceAll("\"/" + getServicesContextRoot(), "\"/" + webUtil.getWebContextRoot());
        }
        catch (FileNotFoundException ex) {
            logger.severeException(ex.getMessage(), ex);
            return errorMessage("Can't locate system info on: " + webToolsUrl);
        }
        catch (IOException ex) {
            logger.severeException(ex.getMessage(), ex);
            return errorMessage("Unable to connect to server at: " + webToolsUrl);
        }
    }

    public String getBuildInfo() throws IOException, XmlException {
        if (configManagerProjects == null)
            loadProjects();

        String webToolsUrl = getServicesUrl();
        String ownerInfo = "Owner: " + getEnvironmentOwner();

        if (appSummary != null) // MDW >= 4.1
        {
            String mdwVersion = appSummary.getMdwVersion();
            int firstDot = mdwVersion.indexOf('.');
            int secondDot = mdwVersion.indexOf('.', firstDot + 1);
            mdwMajorVersion = Integer.parseInt(mdwVersion.substring(0, firstDot));
            mdwMinorVersion = Integer.parseInt(mdwVersion.substring(firstDot + 1, secondDot));
            int hyphen = mdwVersion.indexOf('-', secondDot + 1);
            if (hyphen == -1)
                mdwBuildNumber = Integer.parseInt(mdwVersion.substring(secondDot + 1));
            else
                mdwBuildNumber = Integer.parseInt(mdwVersion.substring(secondDot + 1, hyphen));
            String appName = getApplicationName();  // more reliable than appSummary.getApplicationName()
            String appInfo = isMdw(appName) || "Unknown".equals(appSummary.getVersion()) ? "" : "App Build: " + appSummary.getVersion() + "&#160;&#160;&#160;&#160;&#160;";
            String mdwInfo = "MDW Build: " + appSummary.getMdwVersion() + "&#160;&#160;&#160;&#160;&#160;";
            String server = appSummary.getContainer();
            if ("OSGi".equals(server)) {
                // TODO: revisit hard-coded SMX 5.5 app names
                if ((mdwMajorVersion == 5 && mdwMinorVersion <= 2)
                        || ("Notification WF 55".equals(appName) || "NIC-OSR".equals(appName) || "Dispatch Gateway".equals(appName)))
                    server = "ServiceMix";
                else
                    server = "Fuse";
            }
            if (server == null && mdwMajorVersion <= 4 || (mdwMajorVersion == 5 && mdwMinorVersion < 2))
                server = "WebLogic";
            String serverInfo = server == null ? "" : "Server: " + server + "&#160;&#160;&#160;&#160;&#160;";
            String db = appSummary.getDatabase();
            if (db == null && mdwMajorVersion <= 4 || (mdwMajorVersion == 5 && mdwMinorVersion <= 2))
                db = "Oracle";
            String dbInfo = db == null ? "" : "Database: " + db;
            return appInfo + mdwInfo + ownerInfo + (serverInfo.isEmpty() && dbInfo.isEmpty() ? "" : "<br/>" + serverInfo + dbInfo);
        }
        else // MDW < 4.1
        {
            // try to get build info from buildInfoPlain.jsf URL (MDW < 4.1)
            try {
                URL buildInfoUrl = new URL(webToolsUrl + "/system/buildInfoPlain.jsf");
                HttpHelper httpHelper = new HttpHelper(buildInfoUrl);
                String buildInfo = httpHelper.get();
                mdwMajorVersion = MDW_MAJOR_VERSION_3;
                return buildInfo + ownerInfo;
            }
            catch (FileNotFoundException fnfex) {
                // MDW 3 assumed since we can connect but can't find info
                // (less likely to happen for MDW 4 since service should be available)
                mdwMajorVersion = MDW_MAJOR_VERSION_3;
                logger.severeException("Can't locate build info", fnfex);
                return errorMessage("Can't locate build info");
            }
            catch (IOException ioex) {
                mdwMajorVersion = MDW_VERSION_UNKNOWN;
                logger.severeException("Unable to connect", ioex);
                return errorMessage("Unable to connect");
            }
        }
    }

    private boolean isMdw(String appName) {
        return appName.toUpperCase().startsWith("MDW");
    }

    /**
     * Loads the app summary using the RESTful service.
     *
     * @return null if unable to retrieve
     */
    private ApplicationSummary loadApplicationSummary() {
        try {
            // try to get app summary info using RESTful service
            URL appSummaryUrl = new URL(getServicesUrl() + "/Services/GetAppSummary");
            HttpHelper httpHelper = new HttpHelper(appSummaryUrl);
            String response = httpHelper.get();
            ApplicationSummaryDocument appSummaryDoc = ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
            return appSummaryDoc.getApplicationSummary();
        }
        catch (Exception ex) {
            // this will happen for MDW < 4.1
            logger.warnException("Unable to retrieve app summary info from server.", ex);
            return null;
        }
    }

    private WorkflowApplication getWorkflowApplication(String app) {
        for (WorkflowApplication workflowApplication : configManagerProjects.getWorkflowAppList()) {
            if (workflowApplication.getName().equals(app))
                return workflowApplication;
        }
        return null;
    }

    private WorkflowEnvironment getWorkflowEnvironment(WorkflowApplication workflowApp, String env) {
        for (WorkflowEnvironment workflowEnvironment : workflowApp.getEnvironmentList()) {
            if (workflowEnvironment.getName().equals(env))
                return workflowEnvironment;
        }
        return null;
    }

    public String getMdwWebUrl() {
        if (appSummary != null) // MDW >= 4.1
        {
            if (getMdwMajorVersion() >= 5 && getMdwMinorVersion() >= 5)
                return null;
            else
                return appSummary.getMdwWebUrl();
        }
        else // MDW < 4.1 or appSummary service down
        {
            // hopefully the URLs are specified in ConfigManagerProjects.xml
            if (mdwMajorVersion == MDW_MAJOR_VERSION_3) {
                String webContextRoot = workflowApplication.getWebContextRoot();
                if (workflowApplication.getServicesContextRoot() != null) {
                    // means we have a 4.2 project in transition and some envs are still on 3.2
                    webContextRoot = workflowApplication.getServicesContextRoot();
                }
                return getUserAccessBaseUrl().replaceFirst("https", "http") + "/" + webContextRoot;
            }
            else {
                return getUserAccessBaseUrl() + "/" + workflowApplication.getWebContextRoot();
            }
        }
    }

    public String getContainer() {
        if (appSummary == null)
            return null;
        else
            return appSummary.getContainer();
    }

    public boolean isOsgi() {
        if (appSummary == null)
            return false;
        else
            return "OSGi".equals(appSummary.getContainer());
    }

    public String getDesignerRcpUrl() {
        return ApplicationContext.getDesignerUrl(); // actually url for install guide
    }

    public String getDesignerRcpLabel() {
        return "Designer Install Guide";
    }

    public String getTaskManagerUrl() {
        if (appSummary != null) // MDW >= 4.1
        {
            if (getMdwMajorVersion() >= 5 && getMdwMinorVersion() >= 5)
                return null;
            else
                return appSummary.getTaskManagerUrl();
        }
        else // MDW < 4.1
        {
            // hopefully the URLs are specified in ConfigManagerProjects.xml
            return getUserAccessBaseUrl() + "/" + workflowApplication.getTaskManagerContextRoot();
        }
    }

    public String getReportsUrl() {
        if (appSummary != null) { // MDW >= 4.1
            return appSummary.getReportsUrl();
        }
        else { // MDW < 4.1
            return null;
        }
    }

    public String getMdwHubUrl() {
        if (appSummary != null) { // MDW >= 4.1
            return appSummary.getMdwHubUrl();
        }
        else { // MDW < 4.1
            return null;
        }
    }

    public String getAppLinks() throws IOException, XmlException {
        StringBuffer appLinks = new StringBuffer();
        for (WorkflowUrl wfUrl : workflowApplication.getUrlList()) {
            String url = wfUrl.getStringValue();
            appLinks.append("<li id='" + wfUrl.getId() + "' class='detailLink'>\n  ");
            appLinks.append(wfUrl.getLabel()).append(": <a id='").append(wfUrl.getId())
                    .append("' href='").append(url).append("' target='_blank'>").append(url).append("</a>");
            appLinks.append("\n</li>");
        }

        return appLinks.toString();
    }

    public String getServicesUrl() {
        return getManagedServerBaseUrl() + "/" + getServicesContextRoot();
    }

    public String getManagedServerBaseUrl() {
        ManagedNode managedServer = workflowEnvironment.getManagedServerList().get(0);
        return "http://" + managedServer.getHost() + ":" + managedServer.getPort();
    }

    public String getUserAccessBaseUrl() {
        List<WorkflowUrl> workflowUrls = workflowEnvironment.getUrlList();
        for (WorkflowUrl workflowUrl : workflowUrls) {
            if (workflowUrl.getId().equals("userAccessBaseUrl"))
                return workflowUrl.getStringValue();
        }
        return null;
    }

    public String getConsoleUrl() {
        List<WorkflowUrl> workflowUrls = workflowEnvironment.getUrlList();
        for (WorkflowUrl workflowUrl : workflowUrls) {
            if (workflowUrl.getId().equals("consoleUrl")) {
                String consoleUrl = workflowUrl.getStringValue();
                if (consoleUrl.contains("%MDW_CLOUD_USER%")) {
                    String cloudUser = getCloudUser();
                    if (cloudUser != null)
                        consoleUrl = consoleUrl.replaceAll("%MDW_CLOUD_USER%", cloudUser);
                    else if (consoleUrl.contains("%MDW_CLOUD_USER%@"))
                        consoleUrl = consoleUrl.replaceAll("%MDW_CLOUD_USER%@", "");
                }
                return consoleUrl;
            }
        }
        return null;
    }

    public String getEnvironmentOwner() {
        return workflowEnvironment.getOwner();
    }

    public String getWebContextRoot() {
        return workflowApplication.getWebContextRoot();
    }

    public String getServicesContextRoot() {
        // prefer managed server web root
        ManagedNode managedServer = workflowEnvironment.getManagedServerList().get(0);
        String contextRoot = managedServer.getWebRoot();
        if (contextRoot == null || contextRoot.trim().length() == 0)
            contextRoot = workflowApplication.getServicesContextRoot();
        if (contextRoot == null || contextRoot.trim().length() == 0)
            contextRoot = workflowApplication.getWebContextRoot();

        return contextRoot;
    }

    private String errorMessage(String message) {
        return "<font color='red'>" + message + "</font>";
    }

    public String getCloudUser() {
        return ((Preferences)FacesVariableUtil.getValue("prefs")).getCloudUser();
    }

    public void setCloudUser(String user) {
        ((Preferences)FacesVariableUtil.getValue("prefs")).setCloudUser(user);
    }
}
