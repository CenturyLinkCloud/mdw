/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.event.EventHandlerRegistry;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.provider.ProviderRegistry;
import com.centurylink.mdw.task.types.TaskServiceRegistry;

public class DynamicJavaServiceRegistry {

    private static Map<String,Set<String>> registeredServices = new HashMap<String,Set<String>>();

    public static void addRegisteredService(String serviceInterface, String className) {
        if (TaskServiceRegistry.taskServices.contains(serviceInterface)) {
            TaskServiceRegistry.getInstance().addDynamicService(serviceInterface, className);
        }
        else if (MdwServiceRegistry.mdwServices.contains(serviceInterface)) {
            MdwServiceRegistry.getInstance().addDynamicService(serviceInterface, className);
        }
        else if (MonitorRegistry.monitorServices.contains(serviceInterface)) {
            MonitorRegistry.getInstance().addDynamicService(serviceInterface, className);
        }
        else if (EventHandlerRegistry.eventHandlerServices.contains(serviceInterface)) {
            EventHandlerRegistry.getInstance().addDynamicService(serviceInterface, className);
        }
        else if (ProviderRegistry.providerServices.contains(serviceInterface)) {
            ProviderRegistry.getInstance().addDynamicProvider(serviceInterface, className);
        }
        else {
            // To handle CacheRegistry and StartupRegistry services
            Set<String> serviceClass = registeredServices.get(serviceInterface);
            if (serviceClass == null)
                serviceClass = new HashSet<String>();
            serviceClass.add(className);
            registeredServices.put(serviceInterface, serviceClass);
        }
    }

    public static void addRegisteredService(String serviceInterface, String className, String path) {
        if (MdwServiceRegistry.mdwServices.contains(serviceInterface))
            MdwServiceRegistry.getInstance().addDynamicService(serviceInterface, className, path);
    }

    public static void clearRegisteredServices() {
        TaskServiceRegistry.getInstance().clearDynamicServices();
        MdwServiceRegistry.getInstance().clearDynamicServices();
        MonitorRegistry.getInstance().clearDynamicServices();
        EventHandlerRegistry.getInstance().clearDynamicServices();
        registeredServices.clear();
        ProviderRegistry.getInstance().clearDynamicProviders();
    }

    public static Set<String> getRegisteredServices(String serviceInterface) {
        return registeredServices.get(serviceInterface);
    }

    public static boolean isRegisteredService(String className) {
        for (Set<String> oneSet : TaskServiceRegistry.getInstance().getDynamicServices().values()) {
            if (oneSet.contains(className))
                return true;
        }
        for (Set<String> oneSet : MdwServiceRegistry.getInstance().getDynamicServices().values()) {
            if (oneSet.contains(className))
                return true;
        }
        for (Set<String> oneSet : MonitorRegistry.getInstance().getDynamicServices().values()) {
            if (oneSet.contains(className))
                return true;
        }
        for (Set<String> oneSet : EventHandlerRegistry.getInstance().getDynamicServices().values()) {
            if (oneSet.contains(className))
                return true;
        }
        for (Set<String> oneSet : ProviderRegistry.getInstance().getDynamicProviders().values()) {
            if (oneSet.contains(className))
                return true;
        }
        for (Set<String> oneSet : registeredServices.values()) {
            if (oneSet.contains(className))
                return true;
        }
        return false;
    }
}
