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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.AssetHistory;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.listener.jms.ConfigurationEventListener;
import com.centurylink.mdw.listener.jms.ExternalEventListener;
import com.centurylink.mdw.listener.jms.InternalEventListener;
import com.centurylink.mdw.listener.rmi.RMIListenerImpl;
import com.centurylink.mdw.listeners.startup.StartupRegistry;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.timer.startup.AssetImportMonitor;
import com.centurylink.mdw.timer.startup.TimerTaskRegistration;
import com.centurylink.mdw.timer.startup.UserGroupMonitor;
import com.centurylink.mdw.util.DateHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class MdwMain {

    private static ThreadPoolProvider threadPool;
    private static InternalEventListener internalEventListener;
    private static ExternalEventListener intraMdwEventListener;
    private static ExternalEventListener externalEventListener;
    private static ConfigurationEventListener configurationEventListener;
    private static RMIListener listener;    // do not remove - need to keep reference to prevent GC

    public void startup(String container, String deployPath, String contextPath) {

        StandardLogger logger = null;

        try {
            long before = System.currentTimeMillis();
            System.out.println("MDW initialization...");
            System.out.println("Java: " + System.getProperty("java.version"));
            System.out.println(System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
            System.out.println("  deployPath: " + deployPath);
            System.out.println("  contextPath: " + contextPath);

            System.setProperty("javax.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.ContextFactory");

            logger = LoggerUtil.getStandardLogger();
            ApplicationContext.setDeployPath(deployPath);

            // initialize ApplicationContext
            logger.info("Initialize " + ApplicationContext.class.getName());
            ApplicationContext.onStartup(container);

            // initialize db access and set database time
            try {
                DatabaseAccess db = new DatabaseAccess(null);
                db.checkAndUpgradeSchema();

                // set db time difference so that later call does not go to db
                long dbtime = db.getDatabaseTime();
                System.out.println("Database time: " + DateHelper.dateToString(new Date(dbtime)));
            }
            catch (Exception e) {
                throw new StartupException("Failed to connect through database connection pool", e);
            }

            logger.info("Initialize " + CacheRegistration.class.getName());
            (new CacheRegistration()).onStartup();
            DatabaseAccess.initDocumentDb();

            logger.info("Starting Thread Pool");
            threadPool = ApplicationContext.getThreadPoolProvider();
            threadPool.start();

            MessengerFactory.init(contextPath);

            logger.info("Initialize " + RMIListener.class.getName());
            try {
                listener = new RMIListenerImpl(threadPool);
                ApplicationContext.getContextProvider().bind(RMIListener.JNDI_NAME, listener);
            }
            catch (Exception e) {
                throw new StartupException("Failed to start RMI listener", e);
            }

            if (MessengerFactory.internalMessageUsingJms()) {
                internalEventListener = new InternalEventListener(threadPool);
                internalEventListener.start();
            }

            if (ApplicationContext.getJmsProvider() != null) {
                intraMdwEventListener = new ExternalEventListener(JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, threadPool);
                intraMdwEventListener.start();

                externalEventListener = new ExternalEventListener(JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, threadPool);
                externalEventListener.start();

                configurationEventListener = new ConfigurationEventListener();
                configurationEventListener.start();
            }

            logger.info("Initialize " + TimerTaskRegistration.class.getName());
            (new TimerTaskRegistration()).onStartup();

            List<StartupService> startupServices = StartupRegistry.getInstance().getDynamicStartupServices();
            for (StartupService startupService : startupServices) {
                if (startupService.isEnabled()) {
                    logger.info("Running startup service " + startupService.getClass());
                    startupService.onStartup();
                }
            }

            if (PropertyManager.getBooleanProperty("mdw.asset.history.preload", true)) {
                // pre-load asset history
                AssetHistory.load();
            }

            logger.info("Initialize " + AssetImportMonitor.class.getName());
            (new AssetImportMonitor()).onStartup();

            logger.info("Initialize " + UserGroupMonitor.class.getName());
            (new UserGroupMonitor()).onStartup();

            logger.info("MDW initialization completed after " + (System.currentTimeMillis() - before) + " ms");
        }
        catch (Exception e) {
            e.printStackTrace();
            if (logger != null)
                logger.error(e.getMessage(), e);
            System.out.println("Starting up MDW failed, shut down now - " + e.getMessage());
            shutdown();
        }
    }

    public void shutdown() {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.info("MDW shutdown...");

        try {
            List<StartupService> startupServices = StartupRegistry.getInstance().getDynamicStartupServices();
            for (StartupService startupService : startupServices) {
                if (startupService.isEnabled()) {
                    logger.info("Shutdown startup service " + startupService.getClass().getName());
                    startupService.onShutdown();
                }
            }
        }
        catch (Throwable e) {
            logger.error("Failed to shutdown startup classes", e);
        }

        try {
            (new AssetImportMonitor()).onShutdown();
            (new TimerTaskRegistration()).onShutdown();

            if (configurationEventListener != null)
                configurationEventListener.stop();
                if (externalEventListener != null)
                externalEventListener.stop();
            if (intraMdwEventListener != null)
                intraMdwEventListener.stop();
            if (internalEventListener != null )
                internalEventListener.stop();

            Thread.sleep(2000); // give the listeners a few seconds

            if (threadPool != null) {
                threadPool.stop();
            }

            SpringAppContext.getInstance().shutDown();

            try {
                ApplicationContext.getContextProvider().unbind(RMIListener.JNDI_NAME);
            }
            catch (Exception ignored) {
                // container plug-in is not even initialized
            }

            // deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(driver);
                    logger.info("deregistering jdbc driver");
                } catch (Exception e) {
                    logger.error("Error deregistering jdbc driver", e);
                }
            }

            logger.info("MDW shutdown complete at " + new Date());
        } catch (Throwable ex) {
            logger.error("MdwMain.shutdown() fails", ex);
        }
    }
}
