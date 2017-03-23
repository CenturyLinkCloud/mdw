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
package com.centurylink.mdw.listeners.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.provider.StartupService;

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

    public StartupService getDynamicStartupService(String className) {
        for (StartupService startupService : getDynamicStartupServices()) {
            if (startupService.getClass().getName().equals(className))
                return startupService;
        }
        return null;
    }

    private void addDynamicStartupServices() {
        Set<String> dynamicStartupServices = DynamicJavaServiceRegistry.getRegisteredServices(StartupService.class.getName());
        if (dynamicStartupServices != null)
            super.addDynamicServices(StartupService.class.getName(), dynamicStartupServices);
    }
}
