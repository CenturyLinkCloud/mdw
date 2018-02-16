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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.YamlPropertyManager;
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
import com.centurylink.mdw.model.system.Server;
import com.centurylink.mdw.model.system.ServerList;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.yaml.YamlLoader;

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
    private static String engineContextPath = null;

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
    public static void onStartup(String container, Object containerContext) {
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
            if (ds == null)
                ds = PropertyManager.getProperty("mdw.container.datasource_provider"); // compatibility
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
            if (jms == null)
                jms = PropertyManager.getProperty("mdw.container.jms_provider"); // compatibility
            if (StringHelper.isEmpty(jms))
                jms = JmsProvider.ACTIVEMQ;
            else if (JmsProvider.ACTIVEMQ.equals(jms)) {
                if (jmsProvider == null) {
                    // use below to avoid build time dependency
                    jmsProvider = Class.forName("com.centurylink.mdw.container.plugin.activemq.ActiveMqJms").asSubclass(JmsProvider.class).newInstance();
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
            if (tp == null)
                tp = PropertyManager.getProperty("mdw.container.threadpool_provider"); // compatibility
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
            throw new StartupException("Failed to initialize ApplicationContext", ex);
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

    /**
     * Returns the server host name
     * TODO: infer default port based on https/http in mdw.services.url.
     */
    public static String getServerHost(){
        if (serverHost == null) {
            if (isPaaS()) {
                serverHost = getMasterServer().getHost();
            }
            else {
                try {
                    // unravel cloud deployment host name
                    String localIp = new String(InetAddress.getLocalHost().getHostAddress());
                    for (Server server : getCompleteServerList().getServers()) {
                        String host = server.getHost();
                        if (host.equals("localhost")) {
                            serverHost = host;
                        }
                        else if (host.indexOf('.') < 0) {
                            // encourage fully-qualified domain names
                            Exception ex = new UnknownHostException("Use qualified host names in " + PropertyNames.MDW_SERVER_LIST);
                            logger.severeException(ex.getMessage(), ex);
                        }
                        if (server.getPort() <= 0) {
                            // We need the port specified, even if it's just port 80
                            logger.severe("Specify the port for each instance (use 80 if default port) in " + PropertyNames.MDW_SERVER_LIST);
                        }
                        for (InetAddress address : InetAddress.getAllByName(host)) {
                            if (address.getHostAddress().equals(localIp)) {
                                serverHost = host;
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }

        return serverHost;
    }

    private static int serverPort = -1;
    public static int getServerPort() {
        if (serverPort == -1) {
            if (isPaaS()) {
                serverPort = getMasterServer().getPort();
            }
            else {
                try {
                    serverPort = getNamingProvider().getServerPort();
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
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
                        int read = 0;
                        byte[] bytes = new byte[bufSize];
                        while((read = is.read(bytes)) != -1)
                            os.write(bytes, 0, read);
                    }
                    JarFile tempJarFile = new JarFile(tempjar);
                    Manifest manifest = tempJarFile.getManifest();
                    mdwVersion = manifest.getMainAttributes().getValue("MDW-Version");
                    mdwBuildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                    tempJarFile.close();
                    tempjar.delete();
                }
                else {
                    Manifest manifest = jarFile.getManifest();
                    mdwVersion = manifest.getMainAttributes().getValue("MDW-Version");
                    mdwBuildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                }
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

            if (mdwVersion != null && mdwVersion.endsWith(".SNAPSHOT")) {
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
        return "http://" + getServer() + "/" + getServicesContextRoot();
    }

    public static String getMdwHubUrl() {
        String url = PropertyManager.getProperty(PropertyNames.MDW_HUB_URL);
        if (StringHelper.isEmpty(url) || url.startsWith("@")) {
            url = "http://" + getServer() + "/mdw";
        }
        if (url.endsWith("/"))
            url = url.substring(1);
        return url;
    }

    private static String docsUrl;
    public static String getDocsUrl() {
        if (docsUrl == null) {
            docsUrl = PropertyManager.getProperty(PropertyNames.DOCS_URL);
            if (docsUrl == null)
                docsUrl = "https://centurylinkcloud.github.io/mdw/docs";
            if (docsUrl.endsWith("/"))
                docsUrl = docsUrl.substring(1);
        }
        return docsUrl;
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

    public static Server getServer() {
        return new Server(getServerHost(), getServerPort());
    }

    public static Server getMasterServer() {
        return getServerList().get(0);
    }

    public static boolean isMasterServer() {
        // getServerHost(), getServerPort() do not work in PaaS
        return isPaaS() || getMasterServer().equals(getServer());
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

    public static boolean isServiceApiOpen() {
        return "true".equalsIgnoreCase(System.getProperty("mdw.service.api.open"));
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
        if (serviceUser == null)
            serviceUser = user;
    }
    public static String getServiceUser() {
        if (isServiceApiOpen())
            return serviceUser;
        else
            return null;
    }

    public static boolean isPaaS() {
         return PaaSConstants.PAAS_VCAP_APPLICATION != null;
    }

    public static boolean isSpringBoot() {
        return "org.springframework.boot.loader".equals(System.getProperty("java.protocol.handler.pkgs"));
    }

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

    private static ServerList completeServerList;
    /**
     * @return hosta:8080,hosta:8181,hostb:8080
     */
    public static ServerList getCompleteServerList() {
        if (completeServerList == null) {
            completeServerList = new ServerList(getServerList(), getRoutingServerList());
        }
        return completeServerList;
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

    public static List<URL> getOtherServerUrls(URL thisUrl) throws IOException {
        List<URL> serverUrls = new ArrayList<URL>();
        // Due to different domains for same servers in some environments
        // (host1.ne1.savvis.net and host1.dev.intranet), compare host names sans domain
        String thisHost = thisUrl.getHost().indexOf(".") > 0 ? thisUrl.getHost().substring(0, thisUrl.getHost().indexOf(".")) : thisUrl.getHost();
        int thisPort = thisUrl.getPort() == 80 || thisUrl.getPort() == 443 ? -1 : thisUrl.getPort();
        for (Server server : getCompleteServerList().getServers()) {
            String serviceUrl = "http://" + server.toString() + thisUrl.getPath();
            URL otherUrl = new URL(serviceUrl);
            String otherHost = otherUrl.getHost().indexOf(".") > 0 ? otherUrl.getHost().substring(0, otherUrl.getHost().indexOf(".")) : otherUrl.getHost();
            int otherPort = otherUrl.getPort() == 80 || otherUrl.getPort() == 443 ? -1 : otherUrl.getPort();
            if (!(thisHost.equalsIgnoreCase(otherHost)) || thisPort != otherPort)
                serverUrls.add(otherUrl);
        }
        return serverUrls;
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

    public static String getMdwCentralHost() {
        String mdwCentral = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_HOST);
        if (mdwCentral == null)
            mdwCentral = "mdw.useast.appfog.ctl.io";
        return mdwCentral;
    }

    public static String getMdwAuthUrl() {
        String mdwAuth = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_AUTH_URL);
        if (mdwAuth == null)
            mdwAuth = "https://" + getMdwCentralHost() + "/mdw/services/com/centurylink/mdw/central/auth";
        return mdwAuth;
    }

    public static String getMdwCloudRoutingUrl() {
        String mdwRouting = PropertyManager.getProperty(PropertyNames.MDW_CENTRAL_ROUTING_URL);
        if (mdwRouting == null)
            mdwRouting = "https://" + getMdwCentralHost() + "/mdw/services/routing";
        return mdwRouting;
    }

    private static String authMethod;
    public static String getAuthMethod() {
        return authMethod;
    }
    public static void setAuthMethod(String method) {
        authMethod = method;
    }
 }
