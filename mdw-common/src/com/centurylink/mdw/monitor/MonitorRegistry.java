/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;

/**
 * Registry for MDW workflow monitoring services.
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
        List<ProcessMonitor> processMonitors = new ArrayList<ProcessMonitor>();
        processMonitors.addAll(super.getDynamicServices(ProcessMonitor.class));
        if (ApplicationContext.isOsgi())
            processMonitors.addAll(super.getServices(ProcessMonitor.class));
        return processMonitors;
    }

    public List<ActivityMonitor> getActivityMonitors() {
        List<ActivityMonitor> activityMonitors = new ArrayList<ActivityMonitor>();
        activityMonitors.addAll(super.getDynamicServices(ActivityMonitor.class));
        if (ApplicationContext.isOsgi())
            activityMonitors.addAll(super.getServices(ActivityMonitor.class));
        return activityMonitors;
    }

    public List<AdapterMonitor> getAdapterMonitors() {
        List<AdapterMonitor> adapterMonitors =  new ArrayList<AdapterMonitor>();
        adapterMonitors.addAll(super.getDynamicServices(AdapterMonitor.class));
        if (ApplicationContext.isOsgi())
            adapterMonitors.addAll(super.getServices(AdapterMonitor.class));
        return adapterMonitors;
    }

    /**
     * TODO: handle specified or alpha ordering of monitor chains
     */
    public List<ServiceMonitor> getServiceMonitors() {
        List<ServiceMonitor> serviceMonitors = new ArrayList<ServiceMonitor>();
        serviceMonitors.addAll(getDynamicServices(ServiceMonitor.class));
        if (ApplicationContext.isOsgi())
            serviceMonitors.addAll(getServices(ServiceMonitor.class));
        return serviceMonitors;
    }
}
