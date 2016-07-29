/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.common.constant.ApplicationConstants;
import com.centurylink.mdw.common.constant.PaaSConstants;
import com.centurylink.mdw.common.constant.PropertyGroups;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.StartupException;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.ClasspathUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.DataSourceProvider;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugins.CommonThreadPool;
import com.centurylink.mdw.container.plugins.MdwDataSource;

/**
 * Class that needs to be used when getting the application context
 */
public class ApplicationContext {

    public static final String BUILD_VERSION_FILE = "buildversion.properties";

    private static NamingProvider namingProvider;
    private static DataSourceProvider dataSourceProvider;
    private static JmsProvider jmsProvider;
    private static ThreadPoolProvider threadPoolProvider;

    private static String appName;
    private static String appVersion;
    private static String mdwVersion;
    private static String serverHost;
    private static StandardLogger logger;
    private static String proxyServerName=null;
    private static String containerName="";
    private static String engineContextPath = null;
    private static String contextPath = null;

    public static NamingProvider getNamingProvider() {
        return namingProvider;
    }

    public static JmsProvider getJmsProvider() {
        return jmsProvider;
    }
    public void setJmsProvider(JmsProvider provider) {
        jmsProvider = provider;
    }

    public static DataSourceProvider getDataSourceProvider() {
        return dataSourceProvider;
    }

    public static ThreadPoolProvider getThreadPoolProvider() {
        return threadPoolProvider;
    }

    public static String getContainerName() {
        return containerName;
    }

    public static DataSource getMdwDataSource() {
        try {
            return dataSourceProvider.getDataSource(ApplicationConstants.MDW_FRAMEWORK_DATA_SOURCE_NAME);
        } catch (NamingException e) {
            logger.severeException("Failed to get MDWDataSource", e);
            return null;
        }
    }

    private static boolean startedUp;
    public static boolean isStartedUp() { return startedUp; }

    private static Date startupTime;
    public static Date getStartupTime() { return startupTime; }

    /**
     * Gets invoked when the server comes up
     */
    public static void onStartup(String container, Object containerContext) throws StartupException {
        try {
            startedUp = false;
            logger = LoggerUtil.getStandardLogger();
            containerName = container;

            // use reflection (or better yet (TODO) injection) to avoid build-time dependencies
            String pluginPackage = MdwDataSource.class.getPackage().getName() + "." + containerName.toLowerCase();
            String namingProviderClass = pluginPackage + "." + containerName + "Naming";
            namingProvider = Class.forName(namingProviderClass).asSubclass(NamingProvider.class).newInstance();
            if (namingProvider instanceof ContainerContextAware)
                ((ContainerContextAware)namingProvider).setContainerContext(containerContext);
            logger.info("Naming Provider: " + namingProvider.getClass().getName());

            String ds = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_DATASOURCE_PROVIDER);
            if (StringHelper.isEmpty(ds) || ds.equals(DataSourceProvider.MDW)) {
                dataSourceProvider = new MdwDataSource();
            } else if (DataSourceProvider.TOMCAT.equals(ds)){
                    String dsProviderClass = pluginPackage + "." + ds + "DataSource";
                    dataSourceProvider = Class.forName(dsProviderClass).asSubclass(DataSourceProvider.class).newInstance();
            } else {
                String dsProviderClass = pluginPackage + "." + ds + "DataSource";
                dataSourceProvider = Class.forName(dsProviderClass).asSubclass(DataSourceProvider.class).newInstance();
            }
            if (dataSourceProvider instanceof ContainerContextAware)
                ((ContainerContextAware)dataSourceProvider).setContainerContext(containerContext);
            logger.info("Data Source Provider: " + dataSourceProvider.getClass().getName());

            String jms = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_JMS_PROVIDER);
            if (StringHelper.isEmpty(jms))
                jms = JmsProvider.ACTIVEMQ;
            else if (JmsProvider.ACTIVEMQ.equals(jms)) {
                if (containerContext instanceof BundleContext) {
                    try {
                        BundleContext bundleContext = (BundleContext) containerContext;
                        ServiceReference sr = bundleContext.getServiceReference(JmsProvider.class.getName());
                        if (sr != null) {
                            jmsProvider = (JmsProvider)bundleContext.getService(sr);
                            logger.debug("Injected JmsProvider: " + jmsProvider.getClass());
                        }
                    }
                    catch (Exception ex) {
                        logger.severeException("Error injecting JmsProvider (" + ex.toString() + "); instantiating directly", ex);
                    }
                }
                if (jmsProvider == null) {
                    // use below to avoid build time dependency
                    jmsProvider = Class.forName("com.centurylink.mdw.container.plugins.activemq.ActiveMqJms").asSubclass(JmsProvider.class).newInstance();
                    logger.severe("Cannot inject JmsProvider.  No service reference found");
                }
            }
            else {
                String jmsProviderClass = pluginPackage + "." + jms + "Jms";
                jmsProvider = Class.forName(jmsProviderClass).asSubclass(JmsProvider.class).newInstance();
            }

            if (jmsProvider instanceof ContainerContextAware)
                ((ContainerContextAware)jmsProvider).setContainerContext(containerContext);
            logger.info("JMS Provider: " + (jmsProvider==null?"none":jmsProvider.getClass().getName()));

            String tp = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_THREADPOOL_PROVIDER);
            if (StringHelper.isEmpty(tp) || ThreadPoolProvider.MDW.equals(tp)) {
                threadPoolProvider = new CommonThreadPool();
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                ObjectName mbeanName = new ObjectName("com.centurylink.mdw.container.plugins:type=CommonThreadPool,name=MDW Thread Pool Info");
                if (!mbs.isRegistered(mbeanName))
                    mbs.registerMBean(threadPoolProvider, mbeanName);
            }
            else {
                String tpProviderClass = pluginPackage + "." + tp + "ThreadPool";
                threadPoolProvider = Class.forName(tpProviderClass).asSubclass(ThreadPoolProvider.class).newInstance();
            }
            if (threadPoolProvider instanceof ContainerContextAware)
                ((ContainerContextAware)threadPoolProvider).setContainerContext(containerContext);
            logger.info("Thread Pool Provider: " + threadPoolProvider.getClass().getName());

            startedUp = true;
            startupTime = new Date();
        }
        catch (Exception ex) {
            throw new StartupException(StartupException.FAIL_INIT_APPLICATION_CONTEXT,
                    "Failed to initialize ApplicationContext", ex);
        }
    }

    public static void onShutdown() {
    }

    /**
     * Verifies a class
     * @param className
     * @return boolean status
     */
    public static boolean verifyClass(String className){
        try {
            Class<?> cl = Class.forName(className);
            cl.newInstance();
            return true;
        }
        catch (Exception ex) {
          logger.severeException("ApplicationContext: verifyClass(): General Exception occurred: " + ex.getMessage(), ex);
          return false;
        }
    }

    /**
     * Instantiates a class
     * @param className
     * @return boolean status
     */
    public static Object getClassInstance(String className){
        try{
            Class<?> cl = Class.forName(className);
            return cl.newInstance();
        }
        catch (Exception ex) {
            logger.severeException("ApplicationContext: getClassInstance(): General Exception occurred: " + ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Gets a class based on its name
     * @param className
     * @return the class
     */
    public static Class<?> getClass(String className){
        try {
             return Class.forName(className);
        }
        catch (Exception ex) {
          logger.severeException("ApplicationContext: getClass(): General Exception occurred: " + ex.getMessage(), ex);
          return null;
        }
    }

    /**
     * Returns the application name
     */
    public static String getApplicationName(){
        if (appName != null)
            return appName;
        appName = PropertyManager.getProperty(PropertyNames.APPLICATION_NAME);
        if (appName == null) // try legacy property
            appName = PropertyManager.getProperty("MDWFramework.ApplicationDetails/ApplicationName");
        if (appName == null)
            return "Unknown";
        return appName;
    }

    /**
     * Returns the server host name
     */
    public static String getServerHost(){
        if (serverHost == null) {
            try {
                // unravel cloud deployment host name
                String localIp = new String(InetAddress.getLocalHost().getHostAddress());
                for (String hostPort : getCompleteServerList()) {
                    String host = hostPort.substring(0, hostPort.indexOf(':'));
                    if (host.equals("localhost")) {
                        serverHost = host;
                    }
                    else if (host.indexOf('.') < 0) {
                        // encourage fully-qualified domain names
                        Exception ex = new UnknownHostException("Use fully qualified host names in " + PropertyNames.MDW_SERVER_LIST);
                        logger.severeException(ex.getMessage(), ex);
                    }
                    for (InetAddress address : InetAddress.getAllByName(host)) {
                        if (address.getHostAddress().equals(localIp))
                            serverHost = host;
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }

        return serverHost;
    }

    private static int serverPort = -1;
    public static int getServerPort() {
        if (serverPort == -1) {
            try {
                serverPort = getNamingProvider().getServerPort();
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return serverPort;
    }

    /**
     * Returns the application version read from the build version file
     */
    public static String getApplicationVersion() {
        if (appVersion != null)
            return appVersion;

        String appName = getApplicationName();
        if ("mdw".equalsIgnoreCase(appName)) {
            appVersion = getMdwVersion();
        }
        else {
            appVersion = "Unknown";
            try {
                InputStream stream = ApplicationContext.class.getClassLoader().getResourceAsStream(BUILD_VERSION_FILE);
                if (stream != null) {
                    appVersion = "";
                    int i;
                    while ((i = stream.read()) != -1) {
                        appVersion += (char) i;
                    }
                    stream.close();
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }

        return appVersion;
    }

    /**
     * Returns the MDW version read from mdw-common.jar's manifest file
     * (In OSGi it will have been set by the common bundle activator.
     */
    public static String getMdwVersion() {
        if (mdwVersion != null)
            return mdwVersion;

        mdwVersion = "Unknown";
        try {
            String cpUtilLoc = ClasspathUtil.locate(ClasspathUtil.class.getName());
            int jarBangIdx = cpUtilLoc.indexOf(".jar!");
            if (jarBangIdx > 0) {
                String jarFilePath = cpUtilLoc.substring(0, jarBangIdx + 4);
                if (jarFilePath.startsWith("file:/"))
                    jarFilePath = jarFilePath.substring(6);
                if (!jarFilePath.startsWith("/"))
                    jarFilePath = "/" + jarFilePath;
                JarFile jarFile = new JarFile(new File(jarFilePath));
                Manifest manifest = jarFile.getManifest();
                mdwVersion = manifest.getMainAttributes().getValue("Bundle-Version");
            }
            else {
                // local build -- try tomcat deploy structure
                String catalinaBase = System.getProperty("catalina.base");
                if (catalinaBase != null) {
                    File webInfLib = new File(catalinaBase + "/webapps/mdw/WEB-INF/lib");
                    if (webInfLib.exists()) {
                        for (File file : webInfLib.listFiles()) {
                            if (file.getName().startsWith("mdw-common")) {
                                mdwVersion = file.getName().substring(11, file.getName().length() - 4);
                            }
                        }
                    }
                }
            }

            if (mdwVersion.endsWith(".SNAPSHOT")) {
                mdwVersion = mdwVersion.substring(0, mdwVersion.length() - 9) + "-SNAPSHOT";
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        return mdwVersion;
    }

    public static void setMdwVersion(String version) {
        mdwVersion = version;
    }

    /**
     * Returns the MDW web app URL
     */
    public static String getMdwWebUrl() {
        String url = PropertyManager.getProperty(PropertyNames.MDW_WEB_URL);
        if (url == null)
            url = PropertyManager.getProperty(PropertyNames.MDW_WEB_URL_OLD);
        if (url == null) {
            String thisServer = getServerHostPort();
            if (isWar()) {
                url = getMdwHubUrl();
                if (url == null)
                    url = "http://" + thisServer + "/mdw";
            }
            else
                url = "http://" + thisServer + "/MDWWeb";
        }
        else if (url.endsWith("/tools")) {
            url = url.substring(0, url.length() - 6) + getMdwWebWelcomePath();
        }
        if (url.endsWith("/"))
            url = url.substring(1);
        return url;
    }

    public static String getMdwWebWelcomePath() {
        return "/system/systemInformation.jsf";
    }

    /**
     * Returns the web services URL
     */
    public static String getServicesUrl() {
        String servicesUrl = PropertyManager.getProperty(PropertyNames.MDW_SERVICES_URL);
        if (servicesUrl == null)
            servicesUrl = PropertyManager.getProperty(PropertyNames.MDW_SERVICES_URL_OLD);
        if (servicesUrl == null) {
            servicesUrl = getMdwHubUrl();
        }
        if (servicesUrl.endsWith("/"))
            servicesUrl = servicesUrl.substring(1);
        return servicesUrl;
    }

    public static String getLocalServiceUrl() {
        return "http://" + getServerHostPort() + "/" + getServicesContextRoot();
    }

    /**
     * Returns the Task Manager URL
     */
    public static String getTaskManagerUrl() {
        String url = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_URL);
        if (url == null)
            url = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_URL_OLD);
        if (url == null) {
            url = getMdwHubUrl();  // default to MDWHub url
            if (url == null) {
                String thisServer = getServerHostPort();
                if (isWar()) {
                    if (url == null)
                        url = "http://" + thisServer + "/mdw";
                }
                else
                    url = "http://" + thisServer + "/MDWTaskManagerWeb";
            }
        }
        else if (url.endsWith("/tasks")) {
            url = url.substring(0, url.length() - 6) + getTaskWelcomePath();
        }
        if (url.endsWith("/"))
            url = url.substring(1);
        return url;
    }

    public static String getMdwHubUrl() {
        String url = PropertyManager.getProperty(PropertyNames.MDW_HUB_URL);
        if (StringHelper.isEmpty(url) || url.startsWith("@")) {
            String thisServer = getServerHostPort();
            if (isWar())
                url = "http://" + thisServer + "/mdw";
            else
                url = "http://" + thisServer + "/MDWHub";
        }
        if (url.endsWith("/"))
            url = url.substring(1);
        return url;
    }

    /**
     * Returns null unless Admin UI is used.
     */
    public static String getAdminUrl() {
        return PropertyManager.getProperty(PropertyNames.MDW_ADMIN_URL);
    }

    public static String getAdminContextRoot() {
        return getContextRoot(getAdminUrl());
    }

    /**
     * mdw-hub or mdw-admin
     */
    public static String getTasksUi() {
        String tasksUi = PropertyManager.getProperty(PropertyNames.MDW_TASKS_UI);
        return tasksUi == null ? "mdw-hub" : tasksUi; // TODO: will be mdw-admin
    }

    public static String getReportsUrl() {
        String reportsUrl = PropertyManager.getProperty(PropertyNames.MDW_REPORTS_URL);
        if (reportsUrl == null) {
            if (isOsgi()) {
                String thisServer = getServerHostPort();
                reportsUrl = "http://" + thisServer + "/MDWReports";
            }
            else {
                reportsUrl = getMdwWebUrl() + "/reports/reportsList.jsf";
            }
        }
        if (reportsUrl.endsWith("/"))
            reportsUrl = reportsUrl.substring(1);

        return reportsUrl;
    }

    /**
     * Returns null unless mdw.dashboard.url property is set.
     */
    public static String getDashboardUrl() {
        return PropertyManager.getProperty(PropertyNames.MDW_DASHBOARD_URL);
    }

    /**
     * Returns null unless mdw.solutions.url property is set.
     */
    public static String getSolutionsUrl() {
        return PropertyManager.getProperty(PropertyNames.MDW_SOLUTIONS_URL);
    }

    public static String getDesignerUrl() {
        String designerRcpUrl = "Unknown";
        try {
            String sharePointUrl = PropertyManager.getProperty("MDWFramework.MDWWeb.ExternalLinks/MDW_SharePoint_Site");
            if (sharePointUrl == null)
                sharePointUrl =  PropertyManager.getProperty("MDWFramework.MDWWeb.ExternalLinks/MDW_QShare_Site"); // compatibility
            if (sharePointUrl == null)
                sharePointUrl = "http://cshare.ad.qintra.com/sites/MDW";
            designerRcpUrl =  sharePointUrl + "/Developer%20Resources/Designer%20Install%20Guide.html";
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return designerRcpUrl;
    }

    public static String getTaskWelcomePath() {
        String path = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_WELCOME_PATH);
        if (path == null)
            return "/facelets/tasks/myTasks.jsf";
        else
          return path;
    }

    public static String getHubWelcomePath() {
        // index.html containing this redirect is wired as welcome page in mdw-hub/web.xml
        return "/taskList/myTasks";
    }

    private static String getContextRoot(String url) {
        int k1 = url.indexOf("://");
        if (k1<0) return "Unknown";
        int k2 = url.indexOf("/", k1+3);
        if (k2<0) return "Unknown";
        int k3 = url.indexOf("/", k2+1);
        return (k3>0)?url.substring(k2+1,k3):url.substring(k2+1);
    }

    public static String getTaskManagerContextRoot() {
        return getContextRoot(getTaskManagerUrl());
    }

    public static String getReportsContextRoot() {
        return getContextRoot(getReportsUrl());
    }

    public static String getMdwWebContextRoot() {
        return getContextRoot(getMdwWebUrl());
    }

    public static String getMdwHubContextRoot() {
        return getContextRoot(getMdwHubUrl());
    }

    public static String getServicesContextRoot() {
        return getContextRoot(getServicesUrl());
    }

    public static void setEngineContextPath(String v) {
        engineContextPath = v;
    }

    public static String getEngineContextPath() {
        return engineContextPath;
    }

    public static String getServerHostPort() {
        return getServerHost() + ":" + getServerPort();
    }

    public static String getTempDirectory() {
        String tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
        if (tempDir == null) // fall back to old property name
            tempDir = PropertyManager.getProperty(PropertyGroups.APPLICATION_DETAILS + "/TempDir");
        if (tempDir == null)
            tempDir = "mdw/.temp";  // backward compatibility

        return tempDir;
    }

    public static String getRuntimeEnvironment() {
        return System.getProperty("runtimeEnv");
    }

    public static boolean isProduction() {
        return "prod".equalsIgnoreCase(getRuntimeEnvironment());
    }

    public static boolean isDevelopment() {
        return "dev".equalsIgnoreCase(getRuntimeEnvironment());
    }

    public static String getDevUser() {
        if (isDevelopment()) {
            String devUser = PropertyManager.getProperty(PropertyNames.MDW_DEV_USER);
            if (devUser == null) // compatibility fallback
                devUser = PropertyManager.getProperty("mdw.hub.user");
            if (devUser == null)
                devUser = PropertyManager.getProperty("MDWFramework.TaskManagerWeb/dev.tm.gui.user");
            return devUser;
        }
        else {
            return null;
        }
    }

    public static boolean isOsgi() {
        return NamingProvider.OSGI.equals(getContainerName());
    }

    public static boolean isWar() {
        return NamingProvider.TOMCAT.equals(getContainerName());
    }

    public static boolean isCloud() {
        // TODO option for cloud mode in ServiceMix
        return isWar();
    }

    public static boolean isPaaS() {
         return isWar() &&  (PaaSConstants.PAAS_VCAP_APPLICATION != null);
    }

    private static String warDeployPath;
    public static String getWarDeployPath() {
        return warDeployPath;
    }
    public static void setWarDeployPath(String path) {
        warDeployPath = path;
    }

    private static List<String> serverList;
    /**
     * @return hosta:8080,hosta:8181,hostb:8080
     */
    public static List<String> getManagedServerList() {
        if (serverList == null) {
            serverList = new ArrayList<String>();
            String prop = PropertyManager.getProperty(PropertyNames.MDW_SERVER_LIST);
            if (prop != null) {
                for (String hostPort : prop.split(","))
                    serverList.add(hostPort);
            }
        }
        return serverList;
    }

    private static List<String> routingServerList;
    /**
     * @return hosta:8080,hosta:8181,hostb:8080
     */
    public static List<String> getRoutingServerList() {
        if (routingServerList == null) {
            routingServerList = new ArrayList<String>();
            String prop = PropertyManager.getProperty(PropertyNames.MDW_ROUTING_SERVER_LIST);
            if (prop != null) {
                for (String hostPort : prop.split(","))
                    routingServerList.add(hostPort);
            }
        }
        return routingServerList;
    }

    private static List<String> completeServerList;
    /**
     * @return hosta:8080,hosta:8181,hostb:8080
     */
    public static List<String> getCompleteServerList() {
        if (completeServerList == null) {
            completeServerList = new ArrayList<String>();
            for (String server : getManagedServerList()) {
                completeServerList.add(server);
            }
            for (String server : getRoutingServerList()) {
                if (!completeServerList.contains(server))
                    completeServerList.add(server);
            }
        }
        return completeServerList;
    }

    private static BundleContext osgiBundleContext;
    public static BundleContext getOsgiBundleContext() { return osgiBundleContext; }
    public static void setOsgiBundleContext(BundleContext bundleContext) throws AwaitingStartupException {
        if (!"com.centurylink.mdw.common".equals(bundleContext.getBundle().getSymbolicName()) && !startedUp)
            throw new AwaitingStartupException(bundleContext.getBundle().getSymbolicName() + " is waiting for MDW ApplicationContext...");
        osgiBundleContext = bundleContext;
    }

    public static String getProxyServerName() {
        if (proxyServerName==null) {
            proxyServerName = PropertyManager.getProperty(PropertyNames.MDW_SERVER_PROXY);
        }
        return proxyServerName;
    }

    private static Map<String,Date> mdwBundleActivationTimes;
    public static void setBundleActivationTime(String name, Date time) {
        getMdwBundleActivationTimes().put(name, time);
    }
    public static Map<String,Date> getMdwBundleActivationTimes() {
        if (mdwBundleActivationTimes == null)
            mdwBundleActivationTimes = new LinkedHashMap<String,Date>();
        return mdwBundleActivationTimes;
    }

    public static boolean isFileBasedAssetPersist() {
        return PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION) != null;
    }

    public static String getContextPath() {
        return contextPath;
    }

    public static void setContextPath(String contextPath) {
        ApplicationContext.contextPath = contextPath;
    }

    private static File assetRoot;
    public static File getAssetRoot() {
        if (assetRoot == null) {
            String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
            if (assetLoc != null)
                assetRoot = new File(assetLoc);
        }
        return assetRoot;
    }

    private static String hubOverridePackage;
    public static String getHubOverridePackage() {
        if (hubOverridePackage == null) {
            File assetRoot = getAssetRoot();
            if (assetRoot == null) {
                hubOverridePackage = "MDWHub"; // compatibility for non-vcs
            }
            else {
                hubOverridePackage = PropertyManager.getProperty(PropertyNames.MDW_HUB_OVERRIDE_PACKAGE);
                if (hubOverridePackage == null)
                    hubOverridePackage = "mdw-hub";
                File hubOverrideRoot = new File(assetRoot + "/" + hubOverridePackage.replace('.', '/'));
                if (!hubOverrideRoot.exists())
                    hubOverridePackage = "MDWHub"; // compatibility for older projects non using mdw-hub convention
            }
        }
        return hubOverridePackage;
    }

    private static File hubOverrideRoot;
    public static File getHubOverrideRoot() {
        if (hubOverrideRoot == null) {
            File assetRoot = getAssetRoot();
            if (assetRoot != null)
                hubOverrideRoot = new File(assetRoot + "/" + getHubOverridePackage().replace('.', '/'));
        }
        return hubOverrideRoot;
    }
 }
