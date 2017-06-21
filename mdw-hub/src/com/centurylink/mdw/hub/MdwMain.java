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
package com.centurylink.mdw.hub;

import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.MdwWebSocketServer;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.constant.PropertyGroups;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.SpringConstants;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.listener.jms.ConfigurationEventListener;
import com.centurylink.mdw.listener.jms.ExternalEventListener;
import com.centurylink.mdw.listener.jms.InternalEventListener;
import com.centurylink.mdw.listener.rmi.RMIListenerImpl;
import com.centurylink.mdw.listeners.startup.StartupRegistry;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.startup.StartupClass;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.timer.startup.TimerTaskRegistration;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MdwMain {

    private static ThreadPoolProvider thread_pool;
    private static InternalEventListener internalEventListener;
    private static ExternalEventListener intraMdwEventListener;
    private static ExternalEventListener externalEventListener;
    private static ConfigurationEventListener configurationEventListener;
    private static PropertyManager propertyManager;
    private static RMIListener listener;    // do not remove - need to keep reference to prevent GC
    private static String containerName;    // typically can get from ApplicationContext; need here when startup fails
    private static String deployPath;
    private static String contextPath;

    public void startup(String deployPath, String contextPath) {

        StandardLogger logger = null;

        try {
            System.out.println("MDW initialization...");
            MdwMain.deployPath = deployPath;
            System.out.println("  deployPath: " + deployPath);
            MdwMain.contextPath = contextPath;
            System.out.println("  contextPath: " + contextPath);

            logger = LoggerUtil.getStandardLogger();
            containerName = NamingProvider.TOMCAT;
            ApplicationContext.setDeployPath(deployPath);

            // load properties
            propertyManager = PropertyManager.initializeContainerPropertyManager(containerName, deployPath);

            if (ApplicationContext.isSpringBoot()) {
                String mainLoc = ClasspathUtil.locate(MdwMain.class.getName());
                File bootJar = new File(new java.net.URI(mainLoc.substring(0, mainLoc.indexOf('!'))));
                if (!bootJar.exists())
                    throw new StartupException("No Spring Boot jar: " + mainLoc);
                System.out.println("Spring Boot Jar: " + bootJar.getAbsolutePath());
                File bootDir = new File(deployPath);
                if (bootDir.isDirectory())
                    FileHelper.deleteRecursive(bootDir);
                if (!bootDir.mkdirs())
                    throw new StartupException("Cannot create boot dir: " + bootDir.getAbsolutePath());
                ZipHelper.unzip(bootJar, bootDir);
            }

            // initialize ApplicationContext
            logger.info("Initialize " + ApplicationContext.class.getName());
            ApplicationContext.onStartup(containerName, null);

            // initialize db access and set database time
            try {
                DatabaseAccess db = new DatabaseAccess(null);
                // set db time difference so that later call does not go to db
                long dbtime = db.getDatabaseTime();
                System.out.println("Database time: " + StringHelper.dateToString(new Date(dbtime)));
            }
            catch (Exception e) {
                throw new StartupException(StartupException.FAIL_TO_START_DATABASE_POOL,
                        "Failed to connect through database connection pool", e);
            }

            logger.refreshCache();    // now update based on properties loaded from database

            String v = PropertyManager.getProperty(PropertyNames.MDW_DB_VERSION_SUPPORTED);
            if (v!=null) DataAccess.supportedSchemaVersion = Integer.parseInt(v);

            logger.info("Initialize " + CacheRegistration.class.getName());
            (new CacheRegistration()).onStartup();
            CacheRegistration cacheMgr = new CacheRegistration();
            cacheMgr.registerCache(PropertyManager.class.getName(), propertyManager);

            logger.info("Starting Thread Pool");
            thread_pool = ApplicationContext.getThreadPoolProvider();
            thread_pool.start();

            MessengerFactory.init(contextPath);

            logger.info("Initialize " + RMIListener.class.getName());
            try {
                listener = new RMIListenerImpl(thread_pool);
                ApplicationContext.getNamingProvider().bind(RMIListener.JNDI_NAME, listener);
            }
            catch (Exception e) {
                throw new StartupException(StartupException.FAIL_TO_START_RMI_LISTENER, "Failed to start RMI listener");
            }

            logger.info("Initialize " + ConnectionPoolRegistration.class.getName()) ;
            (new ConnectionPoolRegistration()).onStartup();

            // Spring will inject the listeners below on Tomcat
            // Check that bean is loaded, otherwise use MDW internal mechanism
            // This allows us just to switch using the bean definitions in application-context.xml
            if (!SpringAppContext.getInstance().isBeanDefined(SpringConstants.MDW_SPRING_INTERNAL_EVENT_LISTENER)) {

                if (MessengerFactory.internalMessageUsingJms()) {
                    internalEventListener = new InternalEventListener(thread_pool);
                    internalEventListener.start();
                }

                if (ApplicationContext.getJmsProvider() != null) {
                    intraMdwEventListener = new ExternalEventListener(JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, thread_pool);
                    intraMdwEventListener.start();

                    externalEventListener = new ExternalEventListener(JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, thread_pool);
                    externalEventListener.start();

                    configurationEventListener = new ConfigurationEventListener();
                    configurationEventListener.start();
                }
            }

            logger.info("Initialize " + TimerTaskRegistration.class.getName());
            (new TimerTaskRegistration()).onStartup();

            logger.info("Initialize " + MdwWebSocketServer.class.getName());
            (new MdwWebSocketServer()).onStartup();

            List<StartupClass> coll = this.getAllStartupClasses(logger);
            for (StartupClass startupClass : coll) {
                logger.info("Running startup class " + startupClass.getClass().getName());
                startupClass.onStartup();
            }
            List<StartupService> dynamicStartupServices = StartupRegistry.getInstance().getDynamicStartupServices();
            for (StartupService dynamicService : dynamicStartupServices) {
                logger.info("Running dynamic startup service " + dynamicService.getClass().getName());
                dynamicService.onStartup();
            }
            logger.info("StartupListener:onStartup exits");

        }
        catch (Exception e) {
            e.printStackTrace();
            if (logger != null)
                logger.severeException(e.getMessage(), e);
            System.out.println("Starting up MDW failed, shut down now - " + e.getMessage());
            shutdown();
        }
    }

    public void shutdown() {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.info("Shutdown MDW --> calling StartupListener:onShutdown");

        try {
            Collection<StartupClass> coll = this.getAllStartupClasses(logger);
            Iterator<StartupClass> it = coll.iterator();
            while (it.hasNext()) {
                StartupClass startupClass = (StartupClass) it.next();
                logger.info("Shutdown startup class " + startupClass.getClass().getName());
                startupClass.onShutdown();
            }
            List<StartupService> dynamicStartupServices = StartupRegistry.getInstance().getDynamicStartupServices();
            System.out.println(dynamicStartupServices.size()+" dynamic services size in shutdown************" );
            for (StartupService dynamicService : dynamicStartupServices) {
                logger.info("Shutdown dynamic startup service " + dynamicService.getClass().getName());
                dynamicService.onShutdown();
            }
            StartupRegistry.getInstance().clearDynamicServices();
        }
        catch (Throwable e) {
            logger.severeException("Failed to shutdown startup classes", e);
        }

        try {
            MdwWebSocketServer.getInstance().onShutdown();

            (new TimerTaskRegistration()).onShutdown();

            logger.info("Shutdown common thread pool");
            if (thread_pool != null)
                thread_pool.stop();

            if (configurationEventListener != null)
                configurationEventListener.stop();
            if (externalEventListener != null)
                externalEventListener.stop();
            if (internalEventListener != null )
                internalEventListener.stop();


            logger.info("Shutdown " + ConnectionPoolRegistration.class.getName());
            (new ConnectionPoolRegistration()).onShutdown();

            Thread.sleep(2000); // give the listeners a few seconds
            SpringAppContext.getInstance().shutDown();

            try {
                ApplicationContext.getNamingProvider().unbind(RMIListener.JNDI_NAME);
            }
            catch (Exception e) {        // container plug-in is not even initialized
            }

            logger.info("Shutdown " + CacheRegistration.class.getName());
            (new CacheRegistration()).onShutdown();
            logger.info("Shutdown " + ApplicationContext.class.getName());
            ApplicationContext.onShutdown();
            logger.info("Shutdown database connection pool");

            // deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(driver);
                    logger.info("deregistering jdbc driver");
                } catch (Exception e) {
                    logger.severeException("Error deregistering jdbc driver", e);
                }
            }

            PropertyManager.getInstance().clearCache();
            logger.info("StartupListener:onShutdown exits");
        } catch (Throwable ex) {
            logger.severeException("StartupListener:onShutdown fails", ex);
        }
    }

    public String getContainerName() {
        return containerName;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public String getContextPath() {
        return contextPath;
    }

    List<StartupClass> getAllStartupClasses(StandardLogger logger) throws StartupException {
        List<StartupClass> startupClasses = new ArrayList<StartupClass>();

        Properties props;
        try {
            props = PropertyManager.getInstance().getProperties(PropertyGroups.STARTUP_CLASSES);
        }
        catch (PropertyException e) {
            throw new StartupException(StartupException.FAIL_TO_LOAD_STARTUP_CLASSES, "Failed to load startup classes", e);
        }
        Iterator<Object> it = props.keySet().iterator();
        HashMap<String,String> mamp = new HashMap<String,String>();
        while (it.hasNext()) {
            String name = (String) it.next();
            String className = props.getProperty(name).trim();
            if (className.length() == 0) continue;

            StartupClass instance = null;
            try {
                ClassLoader clsloader = Package.getDefaultPackage().getClassLoader();
                Class<? extends StartupClass> startupClass = clsloader.loadClass(className).asSubclass(StartupClass.class);
                instance = startupClass.newInstance();
            }
            catch (Exception ex) {
              logger.severeException(ex.getMessage(), ex);
            }
            if (instance == null) {
                logger.info("Failed to load Startup class. ClassName " + className);
                continue;
            }
            mamp.put(className, name);
            int i, n = startupClasses.size();
            for (i=0; i<n; i++) {
                String clsn1 = startupClasses.get(i).getClass().getName();
                if (name.compareTo(mamp.get(clsn1))<0) break;
            }
            if (i<n) startupClasses.add(i, instance);
            else startupClasses.add(instance);
        }
        return startupClasses;
    }
}
