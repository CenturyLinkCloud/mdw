/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listeners.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.provider.StartupException;
import com.centurylink.mdw.common.provider.StartupService;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.common.service.ServiceRegistryException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

/**
 * In an OSGi container this registry keeps track of custom startup providers.
 */
public class StartupRegistry extends ServiceRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected StartupRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static StartupRegistry instance;
    public static StartupRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(StartupService.class);
            instance = new StartupRegistry(services);
        }
        return instance;
    }

    @Override
    protected boolean isEnabled(RegisteredService service) {
        return ((StartupService)service).isEnabled();
    }

    public List<StartupService> getDynamicStartupServices() {
        if (getDynamicServices().isEmpty()) {
            this.addDynamicStartupServices();
        }
        return super.getDynamicServices(StartupService.class);
    }

    private void addDynamicStartupServices() {
        Set<String> dynamicStartupServices = DynamicJavaServiceRegistry.getRegisteredServices(StartupService.class.getName());
        if (dynamicStartupServices != null)
            super.addDynamicServices(StartupService.class.getName(), dynamicStartupServices);
    }

    @Override
    public void startupDynamicServices(final BundleContext bundleContext) {
        // To get dynamic services before startup
        this.getDynamicStartupServices();
        super.startupDynamicServices(bundleContext);
    }

    @Override
    protected boolean onRegister(BundleContext bundleContext, RegisteredService service, Map<String,String> props)
    throws ServiceRegistryException {
        String alias = props.get("alias");
        try {
            String bundleName = bundleContext.getBundle().getSymbolicName();
            if (((StartupService)service).isEnabled()) {
                logger.info("Running StartupService '" + alias + "' from bundle " + bundleName + " (class=" + service.getClass().getName() + ")");
                ((StartupService)service).onStartup();
                return true;
            }
            else {
                logger.info("Not running disabled StartupService '" + alias + "' from bundle " + bundleName);
                return false;
            }
        }
        catch (Exception ex) {
            throw new ServiceRegistryException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void onUnregister(BundleContext bundleContext, RegisteredService service, Map<String,String> props) {
        String alias = props.get("alias");
        logger.info("Shutting down StartupProvider '" + alias + "'");
        ((StartupService)service).onShutdown();
    }

    @Override
    protected void registerDynamicServices(BundleContext bundleContext,String name, RegisteredService dynamicService) {
        try {
            String bundleName = bundleContext.getBundle().getSymbolicName();
            if (((StartupService)dynamicService).isEnabled()) {
                logger.info("Running Dynamic StartupService '" + name + "' from bundle " + bundleName + " (class=" + dynamicService.getClass().getName() + ")");
                ((StartupService) dynamicService).onStartup();
            }
        }
        catch (StartupException ex) {
            logger.severeException("Unable to register dynamic service: " + dynamicService.getClass().getName(), ex);
        }
    }

    @Override
    protected void unregisterDynamicServices(BundleContext bundleContext, String name, RegisteredService dynamicService) {
        logger.info("Shutting down StartupProvider '" + name + "'");
        ((StartupService) dynamicService).onShutdown();
    }
}
