/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listeners.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.centurylink.mdw.common.provider.StartupService;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;

/**
 * Keeps track of custom startup providers.
 */
public class StartupRegistry extends ServiceRegistry {

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
}
