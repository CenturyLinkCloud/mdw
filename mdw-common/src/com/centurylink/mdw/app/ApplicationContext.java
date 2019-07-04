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
package com.centurylink.mdw.app;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.YamlPropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.DataSourceProvider;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.container.plugin.MdwDataSource;
import com.centurylink.mdw.container.plugin.tomcat.TomcatDataSource;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.system.Server;
import com.centurylink.mdw.model.system.ServerList;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.yaml.YamlLoader;
import org.apache.commons.lang.StringUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Class that needs to be used when getting the application context
 */
public class ApplicationContext {

    public static final String BUILD_VERSION_FILE = "buildversion.properties";

    private static NamingProvider namingProvider;
    private static DataSourceProvider dataSourceProvider;
    private static JmsProvider jmsProvider;
    private static ThreadPoolProvider threadPoolProvider;

    private static String appId;
    private static String appVersion;
    private static String mdwVersion;
    private static String mdwBuildTimestamp;
    private static String serverHost;
    private static StandardLogger logger;
    private static String containerName="";

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
            return dataSourceProvider.getDataSource(DatabaseAccess.MDW_DATA_SOURCE);
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
    public static void onStartup(String container, Object containerContext) {
        try {
            startedUp = false;
            logger = LoggerUtil.getStandardLogger();
            containerName = container;

            // use reflection to avoid build-time dependencies
            String pluginPackage = MdwDataSource.class.getPackage().getName() + "." + containerName.toLowerCase();
            String namingProviderClass = pluginPackage + "." + containerName + "Naming";
            namingProvider = Class.forName(namingProviderClass).asSubclass(NamingProvider.class).newInstance();
            logger.info("Naming Provider: " + namingProvider.getClass().getName());

            String ds = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_DATASOURCE_PROVIDER);
            if (ds == null)
                ds = PropertyManager.getProperty("mdw.container.datasource_provider"); // compatibility
            if (StringUtils.isBlank(ds) || ds.equals(DataSourceProvider.TOMCAT)) {
                dataSourceProvider = new TomcatDataSource();
            } else if (DataSourceProvider.MDW.equals(ds)){
                dataSourceProvider = new MdwDataSource();
            } else {
                String dsProviderClass = pluginPackage + "." + ds + "DataSource";
                dataSourceProvider = Class.forName(dsProviderClass).asSubclass(DataSourceProvider.class).newInstance();
            }
            logger.info("Data Source Provider: " + dataSourceProvider.getClass().getName());

            String jms = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_JMS_PROVIDER);
            if (jms == null)
                jms = PropertyManager.getProperty("mdw.container.jms_provider"); // compatibility
            if (StringUtils.isBlank(jms))
                jms = JmsProvider.ACTIVEMQ;

            if (JmsProvider.ACTIVEMQ.equals(jms)) {
                if (jmsProvider == null) {
                    // use below to avoid build time dependency
                    jmsProvider = Class.forName("com.centurylink.mdw.container.plugin.activemq.ActiveMqJms").asSubclass(JmsProvider.class).newInstance();
                }
            }
            else {
                String jmsProviderClass = pluginPackage + "." + jms + "Jms";
                jmsProvider = Class.forName(jmsProviderClass).asSubclass(JmsProvider.class).newInstance();
            }

            logger.info("JMS Provider: " + (jmsProvider==null?"none":jmsProvider.getClass().getName()));

            String tp = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_THREADPOOL_PROVIDER);
            if (tp == null)
                tp = PropertyManager.getProperty("mdw.container.threadpool_provider"); // compatibility
            if (StringUtils.isBlank(tp) || ThreadPoolProvider.MDW.equals(tp)) {
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
            logger.info("Thread Pool Provider: " + threadPoolProvider.getClass().getName());

            startedUp = true;
            startupTime = new Date();
        }
        catch (Exception ex) {
            throw new StartupException("Failed to initialize ApplicationContext", ex);
        }
    }

    public static void onShutdown() {
    }

    /**
     * Verifies a class
     * @param className class name.
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
     * Returns the application name
     */
    public static String getAppId() {
        if (appId != null)
            return appId;
        appId = PropertyManager.getProperty(PropertyNames.MDW_APP_ID);
        if (appId == null) // try legacy property
            appId = PropertyManager.getProperty("mdw.application.name");
        if (appId == null)
            return "Unknown";
        return appId;
    }

    public static String getServerHost() {
        if (serverHost == null) {
            try {
                // unravel cloud deployment host name
                String localIp = InetAddress.getLocalHost().getHostAddress();
                for (Server server : getServerList().getServers()) {
                    String host = server.getHost();
                    if (host.equals("localhost")) {
                        serverHost = host;
                    }
                    else if (host.indexOf('.') < 0) {
                        // encourage fully-qualified domain names
                        logger.severe("*** WARNING *** Use qualified host names in " + PropertyNames.MDW_SERVER_LIST);
                    }
                    for (InetAddress address : InetAddress.getAllByName(host)) {
                        if (address.getHostAddress().equals(localIp)) {
                            serverHost = host;
                        }
                    }
                }
                if (serverHost == null) {
                    // fall back to the hostname as known locally
                    serverHost = InetAddress.getLocalHost().getHostName();
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
    public static String getAppVersion() {
        if (appVersion != null)
            return appVersion;

        String appName = getAppId();
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
        JarFile jarFile = null;
        try {
            String cpUtilLoc = ClasspathUtil.locate(ClasspathUtil.class.getName());
            int jarBangIdx = cpUtilLoc.indexOf(".jar!");
            if (jarBangIdx > 0) {
                String jarFilePath = cpUtilLoc.substring(0, jarBangIdx + 4);
                if (jarFilePath.startsWith("file:/"))
                    jarFilePath = jarFilePath.substring(6);
                if (!jarFilePath.startsWith("/"))
                    jarFilePath = "/" + jarFilePath;
                jarFile = new JarFile(new File(jarFilePath));
                String subpath = cpUtilLoc.substring(jarBangIdx + 5);
                int subjarBang = subpath.indexOf(".jar!");
                if (subjarBang > 0) {
                    // subjars in spring boot client apps
                    String subjarPath = subpath.substring(1, subjarBang + 4);
                    ZipEntry subjar = jarFile.getEntry(subjarPath);
                    File tempjar = Files.createTempFile("mdw", ".jar").toFile();
                    try (InputStream is = jarFile.getInputStream(subjar);
                            OutputStream os = new FileOutputStream(tempjar)) {
                        int bufSize = 16 * 1024;
                        int read;
                        byte[] bytes = new byte[bufSize];
                        while((read = is.read(bytes)) != -1)
                            os.write(bytes, 0, read);
                    }
                    try (JarFile tempJarFile = new JarFile(tempjar)) {
                        Manifest manifest = tempJarFile.getManifest();
                        mdwVersion = manifest.getMainAttributes().getValue("MDW-Version");
                        mdwBuildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                    }
                    tempjar.delete();
                }
                else {
                    Manifest manifest = jarFile.getManifest();
                    mdwVersion = manifest.getMainAttributes().getValue("MDW-Version");
                    mdwBuildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                }
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

            if (mdwVersion != null && mdwVersion.endsWith(".SNAPSHOT")) {
                mdwVersion = mdwVersion.substring(0, mdwVersion.length() - 9) + "-SNAPSHOT";
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally{
            try {
                if (jarFile != null)
                    jarFile.close();
            } catch (IOException e) {
                logger.severeException(e.getMessage(), e);
            }
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
            servicesUrl = servicesUrl.substring(0, servicesUrl.length()-1);
        return servicesUrl;
    }

    private static String localServiceAccessUrl;
    public static String getLocalServiceAccessUrl() {
        if (localServiceAccessUrl == null) {
            localServiceAccessUrl = "http://localhost";
            if (getServerPort() > 0)
                localServiceAccessUrl += ":" + getServerPort();
            if (!getServicesContextRoot().isEmpty())
                localServiceAccessUrl += "/" + getServicesContextRoot();
        }
        return localServiceAccessUrl;
    }

    public static String getMdwHubUrl() {
        String url = PropertyManager.getProperty(PropertyNames.MDW_HUB_URL);
        if (StringUtils.isBlank(url) || url.startsWith("@")) {
            url = "http://" + getServer() + "/mdw";
        }
        if (url.endsWith("/"))
            url = url.substring(0, url.length()-1);
        return url;
    }

    private static String docsUrl;
    public static String getDocsUrl() {
        if (docsUrl == null) {
            docsUrl = PropertyManager.getProperty(PropertyNames.DOCS_URL);
            if (docsUrl == null)
                docsUrl = "https://centurylinkcloud.github.io/mdw/docs";
            if (docsUrl.endsWith("/"))
                docsUrl = docsUrl.substring(0, docsUrl.length()-1);
        }
        return docsUrl;
    }

    private static String getContextRoot(String url) {
        int i1 = url.indexOf("://");
        if (i1 < 0)
            return "Unknown";
        int i2 = url.indexOf("/", i1+3);
        if (i2 < 0)
            return "";  // root path
        int i3 = url.indexOf("/", i2 + 1);
        return (i3 > 0) ? url.substring(i2 + 1, i3) : url.substring(i2 + 1);
    }

    public static String getMdwHubContextRoot() {
        return getContextRoot(getMdwHubUrl());
    }

    public static String getServicesContextRoot() {
        return getContextRoot(getServicesUrl());
    }

    public static Server getServer() {
        return new Server(getServerHost(), getServerPort());
    }

    public static String getTempDirectory() {
        String tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
        if (tempDir == null)
            tempDir = "mdw/.temp";
        else if (tempDir.endsWith("/"))
            tempDir = tempDir.substring(0, tempDir.length() - 1);
        return tempDir;
    }

    public static String getAttachmentsDirectory() {
        String attachDir = PropertyManager.getProperty(PropertyNames.MDW_ATTACHMENTS_DIR);
        if (attachDir == null)
            attachDir = "mdw/attachments";
        else if (attachDir.endsWith("/"))
            attachDir = attachDir.substring(0, attachDir.length() - 1);
        return attachDir;
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

    private static String devUser;
    /**
     * Can only be set once (by AccessFilter) at deploy time.
     */
    public static void setDevUser(String user) {
        if (devUser == null)
            devUser = user;
    }
    public static String getDevUser() {
        if (isDevelopment())
            return devUser;
        else
            return null;
    }

    private static String serviceUser;
    /**
     * Can only be set once (by AccessFilter) at deploy time.
     */
    public static void setServiceUser(String user) {
        if (serviceUser == null) {
            if (StringUtils.isBlank(user))
                serviceUser = "N/A";
            else
                serviceUser = user;
        }
    }
    public static String getServiceUser() {
        if ("N/A".equals(serviceUser))
            return null;
        else
            return serviceUser;
    }

    public static boolean isCloudFoundry() {
         return System.getenv("VCAP_APPLICATION") != null;
    }

    private static Boolean springBoot = null;
    public static boolean isSpringBoot() {
        if (springBoot == null) {
            String protocolHandlerPkgs = System.getProperty("java.protocol.handler.pkgs");
            if (protocolHandlerPkgs != null && protocolHandlerPkgs.contains("org.springframework.boot.loader"))
                springBoot = Boolean.TRUE;
            else
                springBoot = Boolean.FALSE;
        }
        return springBoot;
    }

    private static File bootJar;
    public static File getBootJar() { return bootJar; }
    public static void setBootJar(File jar) { bootJar = jar; }

    private static String deployPath;
    public static String getDeployPath() {
        return deployPath;
    }
    public static void setDeployPath(String path) {
        deployPath = path;
    }

    private static ServerList serverList;
    public static ServerList getServerList() {
        if (serverList == null) {
            if (PropertyManager.isYaml()) {
                YamlLoader loader = ((YamlPropertyManager) PropertyManager.getInstance())
                        .getLoader(PropertyNames.MDW_SERVERS);
                if (loader != null) {
                    serverList = new ServerList(loader.getMap(PropertyNames.MDW_SERVERS, loader.getTop()));
                }
            }
            else {
                List<String> hostPorts = PropertyManager.getListProperty(PropertyNames.MDW_SERVER_LIST);
                if (hostPorts != null)
                    serverList = new ServerList(hostPorts);
            }
            if (serverList == null)
                serverList = new ServerList();
        }
        return serverList;
    }

    private static ServerList routingServerList;
    public static ServerList getRoutingServerList() {
        if (routingServerList == null) {
            if (PropertyManager.isYaml()) {
                YamlLoader loader = ((YamlPropertyManager) PropertyManager.getInstance())
                        .getLoader(PropertyNames.MDW_ROUTING_SERVERS);
                if (loader != null) {
                    routingServerList = new ServerList(loader.getMap(PropertyNames.MDW_ROUTING_SERVERS, loader.getTop()));
                }
            }
            else {
                List<String> hostPorts = PropertyManager.getListProperty(PropertyNames.MDW_ROUTING_SERVER_LIST);
                if (hostPorts != null)
                    routingServerList = new ServerList(hostPorts);
            }
            if (routingServerList == null)
                routingServerList = new ServerList();
        }
        return routingServerList;
    }

    private static ClassLoader defaultClassLoader = ApplicationContext.class.getClassLoader();
    public static ClassLoader setContextCloudClassLoader() {
        return setContextCloudClassLoader(null);
    }

    public static ClassLoader setContextCloudClassLoader(com.centurylink.mdw.model.workflow.Package pkg) {
        ClassLoader originalCL = null;
        try {
            if (pkg == null)
                pkg = PackageCache.getPackages().get(0);

            if (pkg != null) {
                originalCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(pkg.getCloudClassLoader());
            }
        }
        catch (Throwable ex) {
            logger.warnException("Problem loading packages or no Packages were found when trying to set ContextClassLoader", ex);
        }
        return originalCL;
    }

    public static void resetContextClassLoader() {
        resetContextClassLoader(null);
    }

    public static void resetContextClassLoader(ClassLoader classLoader) {
        if (classLoader != null)
            Thread.currentThread().setContextClassLoader(classLoader);
        else
            Thread.currentThread().setContextClassLoader(defaultClassLoader);
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
    /**
     * Use this only when running outside of container.
     */
    public static void setAssetRoot(File assetLoc) {
        assetRoot = assetLoc;
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

    public static String getWebSocketUrl() {
        String websocketUrl = PropertyManager.getProperty(PropertyNames.MDW_WEBSOCKET_URL);
        if (websocketUrl == null) {
            // use default
            String hubUrl = getMdwHubUrl();
            if (hubUrl.startsWith("https://"))
                websocketUrl = "wss://" + hubUrl.substring(8) + "/websocket";
            else if (hubUrl.startsWith("http://"))
                websocketUrl = "ws://" + hubUrl.substring(7) + "/websocket";
        }
        else if (websocketUrl.equals("none")) {
            // force polling for web operations that support it
            websocketUrl = null;
        }
        return websocketUrl;
    }

    public static String getMdwCentralUrl() {
        String mdwCentralUrl = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_URL);
        if (mdwCentralUrl == null)
            mdwCentralUrl = "https://mdw-central.com";
        return mdwCentralUrl;
    }

    /**
     * Compatibility for disparities in mdw-central hosting mechanism.
     */
    public static String getCentralServicesUrl() {
        String centralServicesUrl = getMdwCentralUrl();
        if (centralServicesUrl != null && centralServicesUrl.endsWith("/central"))
            centralServicesUrl = centralServicesUrl.substring(0, centralServicesUrl.length() - 8);
        return centralServicesUrl;
    }

    public static String getMdwAuthUrl() {
        String mdwAuth = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_AUTH_URL);
        if (mdwAuth == null)
            mdwAuth = getCentralServicesUrl() + "/api/com/centurylink/mdw/central/auth";
        return mdwAuth;
    }

    public static String getMdwCentralHost() throws MalformedURLException {
        return new URL(ApplicationContext.getMdwCentralUrl()).getHost();
    }

    public static String getMdwCloudRoutingUrl() {
        String mdwRouting = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_ROUTING_URL);
        if (mdwRouting == null)
            mdwRouting = getMdwCentralUrl() + "/api/routing";
        return mdwRouting;
    }

    private static String authMethod;
    public static String getAuthMethod() {
        return authMethod;
    }
    public static void setAuthMethod(String method) {
        authMethod = method;
    }
    public static boolean isMdwAuth() {
        return "mdw".equals(getAuthMethod());
    }

    private static String hostname;
    public static String getHostname() {
        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return hostname;
    }
}
