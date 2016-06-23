/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listeners.startup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.JMSException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.spring.MdwOsgiBundleContext;
import com.centurylink.mdw.common.task.TaskServiceRegistry;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugins.activemq.ActiveMqJms;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.listener.jms.ConfigurationEventListener;
import com.centurylink.mdw.listener.jms.ExternalEventListener;
import com.centurylink.mdw.listener.jms.InternalEventListener;
import com.centurylink.mdw.listener.rmi.RMIListenerImpl;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.timer.startup.TimerTaskRegistration;

public class StartupBundleActivator implements BundleActivator, ServiceListener {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String STARTUP_DELAY = "mdw.jms.startup.delay";

    private ThreadPoolProvider threadPool;
    private InternalEventListener internalEventListener;
    private ExternalEventListener externalEventListener;
    private ExternalEventListener externalEventListener2;
    private ConfigurationEventListener configurationEventListener;

    private List<ServiceRegistration> mdwOsgiBundleContexts = new ArrayList<ServiceRegistration>();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("mdw-listeners startup...");

        try {
            ApplicationContext.setOsgiBundleContext(bundleContext);

            if (ApplicationContext.isDevelopment())
                logger.severe("\n***WARNING***\nMDW is running in DEVELOPMENT mode.  Do not deploy this way in a production environment.\n***WARNING***");

            // below is copied from StartupListener
            MessengerFactory.init(ApplicationContext.getMdwWebContextRoot());

            logger.info("Starting Thread Pool");
            threadPool = ApplicationContext.getThreadPoolProvider();
            threadPool.start();

            logger.info("Initialize " + ConnectionPoolRegistration.class.getName());
            (new ConnectionPoolRegistration()).onStartup();

            logger.info("Initialize " + RMIListener.class.getName());
            RMIListener listener = new RMIListenerImpl(threadPool);
            ApplicationContext.getNamingProvider().bind(RMIListener.JNDI_NAME, listener);

            long delay = 5000;
            String waitStr = System.getProperty(STARTUP_DELAY);
            if (waitStr != null)
                delay = Integer.parseInt(waitStr) * 1000;
            Thread.sleep(delay);    // wait for JMS to be initialized

            // wait for JMS connection factory to become available
            if (((ActiveMqJms)ApplicationContext.getJmsProvider()).getConnectionFactory() == null)
                throw new JMSException("JMS Connection Factory not Available");

            if (MessengerFactory.internalMessageUsingJms()) {
                internalEventListener = new InternalEventListener(threadPool);
                internalEventListener.start();
            }

            if (ApplicationContext.getJmsProvider()!=null) {
                externalEventListener = new ExternalEventListener(JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, threadPool);
                externalEventListener.start();

                externalEventListener2 = new ExternalEventListener(JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, threadPool);
                externalEventListener2.start();

                configurationEventListener = new ConfigurationEventListener();
                configurationEventListener.start();
            }

            System.out.println("Server time: " + StringHelper.serviceDateToString(new Date()));
            DatabaseAccess db = new DatabaseAccess(null);
            // set db time difference so that later call does not go to db
            long dbtime = db.getDatabaseTime();
            System.out.println("Database time: " + StringHelper.serviceDateToString(new Date(dbtime)));

            LoggerUtil.getStandardLogger().refreshCache();

            String v = PropertyManager.getProperty(PropertyNames.MDW_DB_VERSION_SUPPORTED);
            if (v!=null) DataAccess.supportedSchemaVersion = Integer.parseInt(v);

            // TODO: prevent failed StartupBundleActivator activations from starting multiple timers
            logger.info("Initialize " + TimerTaskRegistration.class.getName());
            (new TimerTaskRegistration()).onStartup();

            // replaces startup class registration
            StartupRegistry.getInstance().startup(bundleContext);
            // service registries
            MonitorRegistry.getInstance().startup(bundleContext);
            TaskServiceRegistry.getInstance().startup(bundleContext);
            MdwServiceRegistry.getInstance().startup(bundleContext);
            // Dynamic Startup Services
            StartupRegistry.getInstance().startupDynamicServices(bundleContext);

            // service listener for Spring configs
            String filter = "(objectclass=" + org.springframework.context.ApplicationContext.class.getName() + ")";
            // register previously started AppContext services
            ServiceReference[] serviceRefs = bundleContext.getServiceReferences(null, filter);
            if (serviceRefs != null) {
                for (ServiceReference serviceRef : serviceRefs) {
                    Object appContext = serviceRef.getBundle().getBundleContext().getService(serviceRef);
                    if (appContext instanceof OsgiBundleXmlApplicationContext)
                        register(bundleContext, (OsgiBundleXmlApplicationContext)appContext);
                }
            }
            // listen for Spring ApplicationContext service events
            bundleContext.addServiceListener(this, filter);

            ApplicationContext.setBundleActivationTime("mdw-listeners", new Date());
            logger.info("mdw-listeners startup completed.");
        }
        catch (AwaitingStartupException ex) {
            logger.info("Awaiting startup: " + ex.getMessage());
            throw ex;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.severeException(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        try {
            bundleContext.removeServiceListener(this);
            for (ServiceRegistration serviceReg : mdwOsgiBundleContexts) {
                try {
                    ServiceReference serviceRef = serviceReg.getReference();
                    if (serviceRef != null) {
                        MdwOsgiBundleContext mdwBundleContext = (MdwOsgiBundleContext)bundleContext.getService(serviceRef);
                        if (mdwBundleContext != null)
                            mdwBundleContext.close();
                    }
                    serviceReg.unregister();
                }
                catch (IllegalStateException ex) {
                    // java.lang.IllegalStateException: The service registration is no longer valid.
                    logger.info("Service registration no longer valid: " + serviceReg);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        MonitorRegistry.getInstance().shutdown(bundleContext);
        StartupRegistry.getInstance().shutdownDynamicServices(bundleContext);
        StartupRegistry.getInstance().shutdown(bundleContext);

        (new TimerTaskRegistration()).onShutdown();
        if (configurationEventListener != null)
            configurationEventListener.stop();
        if (externalEventListener != null)
            externalEventListener.stop();
        if (externalEventListener2 != null)
            externalEventListener2.stop();
        if (internalEventListener != null)
            internalEventListener.stop();

        logger.info("Shutdown " + ConnectionPoolRegistration.class.getName());
        (new ConnectionPoolRegistration()).onShutdown();

        try {
            ApplicationContext.getNamingProvider().unbind(RMIListener.JNDI_NAME);
        }
        catch (Exception e) {     // container plug-in is not even initialized
        }

        logger.info("Shutdown MDW common thread pool");
        if (threadPool != null)
            threadPool.stop();

        logger.info("Shutdown " + ApplicationContext.class.getName());
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference serviceRef = event.getServiceReference();
        if (serviceRef != null && serviceRef.getBundle() != null && serviceRef.getBundle().getBundleContext() != null) {
            Object appContext = serviceRef.getBundle().getBundleContext().getService(serviceRef);
            if (appContext instanceof OsgiBundleXmlApplicationContext) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    register(event.getServiceReference().getBundle().getBundleContext(), (OsgiBundleXmlApplicationContext)appContext);
                }
                else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    unregister(event.getServiceReference().getBundle().getBundleContext(), (OsgiBundleXmlApplicationContext)appContext);
                }
            }
        }
    }

    private void register(BundleContext bundleContext, OsgiBundleXmlApplicationContext appContext) {
        if (appContext instanceof MdwOsgiBundleContext)
            return; // avoid recursive looping
        try {
            logger.debug("Registering Spring OSGi App Context: " + appContext);
            List<String> assetUrls = new ArrayList<String>();

            for (RuleSetVO springRuleSet : RuleSetCache.getRuleSets(RuleSetVO.SPRING)) {
                PackageVO pkg = PackageVOCache.getRuleSetPackage(springRuleSet.getId());
                if (pkg != null && pkg.getBundleSpec() != null && pkg.getBundleSpec().meetsSpec(appContext.getBundle())) {
                    String url = MdwOsgiBundleContext.MDW_SPRING_PREFIX + pkg.getName() + "/" + springRuleSet.getName();
                    logger.info("Adding Spring Config Asset: '" + url + " ' with ApplicationContext: " + appContext);
                    assetUrls.add(url);
                }
            }
            MdwOsgiBundleContext mdwBundleContext = new MdwOsgiBundleContext(assetUrls.toArray(new String[0]), appContext);
            mdwBundleContext.refresh();
            ServiceRegistration serviceReg = bundleContext.registerService(MdwOsgiBundleContext.class.getName(), mdwBundleContext, null);
            mdwOsgiBundleContexts.add(serviceReg);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    private void unregister(BundleContext bundleContext, OsgiBundleXmlApplicationContext appContext) {
        if (appContext instanceof MdwOsgiBundleContext)
            return; // avoid recursive looping
        try {
            logger.mdwDebug("Unregistering Spring OSGi App Context: " + appContext);
            for (ServiceRegistration serviceReg : mdwOsgiBundleContexts) {
                try {
                    ServiceReference serviceRef = serviceReg.getReference();
                    if (serviceRef != null) {
                        MdwOsgiBundleContext mdwBundleContext = (MdwOsgiBundleContext)bundleContext.getService(serviceRef);
                        if (mdwBundleContext != null && mdwBundleContext.getParent().equals(appContext)) {
                            mdwBundleContext.close();
                            serviceReg.unregister();
                        }
                    }
                }
                catch (IllegalStateException ex) {
                    // don't prevent other services from unregistering
                    logger.severe("***MDW Unregister***: ServiceReference is not valid: " + ex.toString());
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
