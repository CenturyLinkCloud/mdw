package com.centurylink.mdw.common.service;

import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.script.ExecutorRegistry;
import com.centurylink.mdw.task.types.TaskServiceRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DynamicJavaServiceRegistry {

    private static final Map<String,Set<String>> registeredServices = new HashMap<>();

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
        else if (ExecutorRegistry.scriptServices.contains(serviceInterface)) {
            ExecutorRegistry.getInstance().addDynamicService(serviceInterface, className);
        }
        else {
            // handle Cache, Startup and other services
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
        // script executor services omitted (needs server bounce)
        TaskServiceRegistry.getInstance().clearDynamicServices();
        MdwServiceRegistry.getInstance().clearDynamicServices();
        MonitorRegistry.getInstance().clearDynamicServices();
        registeredServices.clear();
    }

    public static Set<String> getRegisteredServices(String serviceInterface) {
        return registeredServices.get(serviceInterface);
    }
}
