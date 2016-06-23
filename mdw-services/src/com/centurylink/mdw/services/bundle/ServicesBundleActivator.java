/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.bundle;

import java.util.Date;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DataAccess;

public class ServicesBundleActivator implements BundleActivator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("mdw-services startup...");

        try {
            ApplicationContext.setOsgiBundleContext(bundleContext);

            // spring context loading
            String[] configLocs = new String[] {"META-INF/spring/bundle-context.xml"};
            OsgiBundleXmlApplicationContext ctx = new OsgiBundleXmlApplicationContext(configLocs);
            ctx.setPublishContextAsService(false);
            ctx.setBundleContext(bundleContext);
            ctx.refresh();

            // cache refresh needs to be aware of supported schema version
            String v = PropertyManager.getProperty(PropertyNames.MDW_DB_VERSION_SUPPORTED);
            if (v!=null) DataAccess.supportedSchemaVersion = Integer.parseInt(v);

            logger.info("Initialize " + CacheRegistry.class.getName());
            CacheRegistry.getInstance().startup(bundleContext);
            CacheRegistry.getInstance().startupDynamicServices(bundleContext);

            ApplicationContext.setBundleActivationTime("mdw-services", new Date());
            logger.info("mdw-services startup completed.");
        }
        catch (AwaitingStartupException ex) {
            logger.info("Awaiting startup: " + ex.getMessage());
            throw ex;
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            logger.severeException(ex.getMessage(), ex);
            throw new Exception(ex);
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        CacheRegistry.getInstance().shutdownDynamicServices(bundleContext);
        CacheRegistry.getInstance().shutdown(bundleContext);
        logger.info("MDW Services shutdown completed.");
    }
}
