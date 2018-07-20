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
package com.centurylink.mdw.common.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.provider.ProviderRegistry;
import com.centurylink.mdw.script.ExecutorRegistry;
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
        else if (ProviderRegistry.providerServices.contains(serviceInterface)) {
            ProviderRegistry.getInstance().addDynamicProvider(serviceInterface, className);
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
        ProviderRegistry.getInstance().clearDynamicProviders();
    }

    public static Set<String> getRegisteredServices(String serviceInterface) {
        return registeredServices.get(serviceInterface);
    }
}
