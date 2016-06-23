/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.bundle;

import java.util.Date;

import javax.jms.ConnectionFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.config.OsgiPropertyManager;
import com.centurylink.mdw.common.provider.ProviderRegistry;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.plugins.osgi.OSGiNaming;

public class FrameworkBundleActivator implements BundleActivator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("MDW Framework initialization...");

        // may need to be set by client if their bundle level is lower than mdw-common's
        System.setProperty(PropertyManager.MDW_PROPERTY_MANAGER, OsgiPropertyManager.class.getName());

        try {
            Version bundleVersion = bundleContext.getBundle().getVersion();
            ApplicationContext.setMdwVersion(bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "."
                + (bundleVersion.getMicro() > 9 ? bundleVersion.getMicro() : "0" + bundleVersion.getMicro()));
            ApplicationContext.setOsgiBundleContext(bundleContext);
            PropertyManager.getLocalInstance();  // trigger initialization for OSGi service availability

            // spring context loading
            String[] configLocs = new String[] {"META-INF/spring/bundle-context.xml"};
            OsgiBundleXmlApplicationContext ctx = new OsgiBundleXmlApplicationContext(configLocs);
            ctx.setPublishContextAsService(false);
            ctx.setBundleContext(bundleContext);
            ctx.refresh();

            ApplicationContext.onStartup(NamingProvider.OSGI, bundleContext);

            String port = OsgiPropertyManager.getOsgiProperty(bundleContext, "org.ops4j.pax.web", "org.osgi.service.http.port");
            if (port != null)
                ((OSGiNaming)ApplicationContext.getNamingProvider()).setServerPort(Integer.parseInt(port));

            ProviderRegistry.getInstance().startup(bundleContext);

            // service listener for JMS - avoid holding on to stale ConnectionFactory references
            JmsServiceListener jmsServiceListener = new JmsServiceListener();
            bundleContext.addServiceListener(jmsServiceListener, jmsServiceListener.FILTER);

            ApplicationContext.setBundleActivationTime("mdw-common", new Date());
            logger.info("mdw-common startup completed for: " + ApplicationContext.getApplicationName() + " " + bundleVersion);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.severeException(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        ProviderRegistry.getInstance().shutdown(bundleContext);
        ApplicationContext.onShutdown();
        logger.info("Shutdown completed for MDW Framework.");
    }

    class JmsServiceListener implements ServiceListener {
        public String FILTER = "(objectclass=" + ConnectionFactory.class.getName() + ")";

        public void serviceChanged(ServiceEvent event) {
            JMSServices.getInstance().clearCached();
        }
    }

}
