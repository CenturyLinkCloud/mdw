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
package com.centurylink.mdw.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;

/**
 * Registry for MDW monitoring services.
 */
public class MonitorRegistry extends ServiceRegistry {

    public static final List<String> monitorServices = new ArrayList<String>(Arrays.asList(new String[] {ProcessMonitor.class.getName(), ActivityMonitor.class.getName(),
            AdapterMonitor.class.getName(), ServiceMonitor.class.getName()}));

    protected MonitorRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static MonitorRegistry instance;
    public static MonitorRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(ProcessMonitor.class);
            services.add(ActivityMonitor.class);
            services.add(AdapterMonitor.class);
            services.add(ServiceMonitor.class);
            instance = new MonitorRegistry(services);
        }
        return instance;
    }

    public List<ProcessMonitor> getProcessMonitors() {
        return getDynamicServices(ProcessMonitor.class);
    }

    public List<ActivityMonitor> getActivityMonitors() {
        return getDynamicServices(ActivityMonitor.class);
    }

    public List<AdapterMonitor> getAdapterMonitors() {
        return getDynamicServices(AdapterMonitor.class);
    }

    public List<TaskMonitor> getTaskMonitors() {
        return getDynamicServices(TaskMonitor.class);
    }

    /**
     * TODO: handle specified or alpha ordering of monitor chains
     */
    public List<ServiceMonitor> getServiceMonitors() {
        List<ServiceMonitor> serviceMonitors = new ArrayList<ServiceMonitor>();
        serviceMonitors.addAll(getDynamicServices(ServiceMonitor.class));
        return serviceMonitors;
    }
}
