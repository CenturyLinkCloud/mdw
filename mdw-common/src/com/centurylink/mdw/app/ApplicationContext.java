/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.app;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.ApplicationConstants;
import com.centurylink.mdw.constant.PaaSConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.DataSourceProvider;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.container.plugin.MdwDataSource;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

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
    private static String mdwBuildTimestamp;
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
                if (jmsProvider == null) {
                    // use below to avoid build time dependency
                    jmsProvider = Class.forName("com.centurylink.mdw.container.plugin.activemq.ActiveMqJms").asSubclass(JmsProvider.class).newInstance();
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
                ObjectName mbeanName = new ObjectName("com.centurylink.mdw.container.plugin:type=CommonThreadPool,name=MDW Thread Pool Info");
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
                mdwVersion = manifest.getMainAttributes().getValue("MDW-Version");
                mdwBuildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                jarFile.close();
            }
            else {
                // try tomcat deploy structure
                String catalinaBase = System.getProperty("catalina.base");
                if (catalinaBase != null) {
                    File webInfLib = new File(catalinaBase + "/webapps/mdw/WEB-INF/lib");
                    if (webInfLib.exists()) {
                        for (File file : webInfLib.listFiles()) {
                            if (file.getName().startsWith("mdw-common")) {
                                mdwVersion = file.getName().substring(11, file.getName().length() - 4);
                                String mfPath = "jar:" + file.toURI() + "!/META-INF/MANIFEST.MF";
                                Manifest manifest = new Manifest(new URL(mfPath).openStream());
                                Attributes attrs = manifest.getMainAttributes();
                                mdwBuildTimestamp = attrs.getValue("MDW-Build");
                                break;
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

    public static String getMdwBuildTimestamp() {
        return mdwBuildTimestamp == null ? "" : mdwBuildTimestamp;
    }

    public static void setMdwBuildTimestamp(String timestamp) {
        mdwBuildTimestamp = timestamp;
    }

    /**
     * Returns the web services URL
     */
    public static String getServicesUrl() {
        String servicesUrl = PropertyManager.getProperty(PropertyNames.MDW_SERVICES_URL);
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

    public static String getMdwHubUrl() {
        String url = PropertyManager.getProperty(PropertyNames.MDW_HUB_URL);
        if (StringHelper.isEmpty(url) || url.startsWith("@")) {
            String thisServer = getServerHostPort();
            url = "http://" + thisServer + "/mdw";
        }
        if (url.endsWith("/"))
            url = url.substring(1);
        return url;
    }

    private static String getContextRoot(String url) {
        int k1 = url.indexOf("://");
        if (k1<0) return "Unknown";
        int k2 = url.indexOf("/", k1+3);
        if (k2<0) return "Unknown";
        int k3 = url.indexOf("/", k2+1);
        return (k3>0)?url.substring(k2+1,k3):url.substring(k2+1);
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
        if (tempDir == null)
            tempDir = "mdw/.temp";
        return tempDir;
    }

    public static String getRuntimeEnvironment() {
        return System.getProperty("mdw.runtime.env");
    }

    public static boolean isProduction() {
        return "prod".equalsIgnoreCase(getRuntimeEnvironment());
    }

    public static boolean isDevelopment() {
        return "dev".equalsIgnoreCase(getRuntimeEnvironment());
    }

    public static boolean isServiceApiOpen() {
        return "true".equalsIgnoreCase(System.getProperty("mdw.service.api.open"));
    }

    public static String getDevUser() {
        if (isDevelopment()) {
            String devUser = PropertyManager.getProperty(PropertyNames.MDW_DEV_USER);
            if (devUser == null) // compatibility fallback
                devUser = PropertyManager.getProperty("mdw.hub.user");
            return devUser;
        }
        else {
            return null;
        }
    }

    public static String getServiceUser() {
        if (isServiceApiOpen()) {
            return PropertyManager.getProperty(PropertyNames.MDW_SERVICE_USER);
        }
        else {
            return null;
        }
    }

    public static boolean isWar() {
        return NamingProvider.TOMCAT.equals(getContainerName());
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
            hubOverridePackage = PropertyManager.getProperty(PropertyNames.MDW_HUB_OVERRIDE_PACKAGE);
            if (hubOverridePackage == null)
                hubOverridePackage = "mdw-hub";
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
