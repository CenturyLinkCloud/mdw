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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ServiceRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Map<String,List<RegisteredService>> services = new HashMap<String,List<RegisteredService>>();
    private Map<String,Set<String>> dynamicServices = new HashMap<String,Set<String>>(); // Dynamic java Registered services
    private Map<String,String> pathToDynamicServiceClass = new HashMap<String,String>(); // resource paths

    protected ServiceRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        for (Class<? extends RegisteredService> serviceInterface : serviceInterfaces) {
            services.put(serviceInterface.getName(), new ArrayList<RegisteredService>());
        }
    }
    public <T extends RegisteredService> List<T> getServices(Class<T> serviceInterface) {
        List<T> list = new ArrayList<T>();
        for (RegisteredService service : services.get(serviceInterface.getName())) {
            T rs = serviceInterface.cast(service);
            list.add(rs);
        }
        return list;
    }

    /**
     * Get the Dynamic java instance for Registered Service
     */
    public <T extends RegisteredService> T getDynamicService(Package pkg, Class<T> serviceInterface, String className) {
        if (dynamicServices.containsKey(serviceInterface.getName())
                && dynamicServices.get(serviceInterface.getName()).contains(className)) {
            try {
                ClassLoader parentClassLoader = pkg == null ? getClass().getClassLoader() : pkg.getClassLoader();
                Class<?> clazz = CompiledJavaCache.getClassFromAssetName(parentClassLoader, className);
                if (clazz == null)
                    return null;
                RegisteredService rs = (RegisteredService) (clazz).newInstance();
                T drs = serviceInterface.cast(rs);
                return drs;
            }
            catch (Exception ex) {
                logger.severeException("Failed to get the dynamic registered service : " + className +" \n " + ex.getMessage(), ex);
            }
        }
        return null;
    }

    public <T extends RegisteredService> T getDynamicServiceForPath(Package pkg, Class<T> serviceInterface, String resourcePath) {
        String className = pathToDynamicServiceClass.get(pkg.getName().replace('.',  '/') + resourcePath);
        if (className != null)
            return getDynamicService(pkg, serviceInterface, className);
        return null;
    }

    public Map<String, Set<String>> getDynamicServices() {
        return dynamicServices;
    }

    /**
     * To get List of dynamic java registered services based on service interface
     * Example: To get list of registered process Monitors
     * @param serviceInterface
     * @return
     */
    public <T extends RegisteredService> List<T> getDynamicServices(Class<T> serviceInterface) {
        List<T> dynServices = new ArrayList<T>();
        if (dynamicServices.containsKey(serviceInterface.getName())) {
            Set<String> deregister = new HashSet<String>();
            for (String serviceClassName : dynamicServices.get(serviceInterface.getName())) {
                try {
                    Class<?> clazz = CompiledJavaCache.getClassFromAssetName(null, serviceClassName);
                    if (clazz != null)  {
                        RegisteredService rs = (RegisteredService)(clazz).newInstance();
                        dynServices.add(serviceInterface.cast(rs));
                    }
                } catch (Exception ex) {
                    logger.severeException("Failed to get Dynamic Java service : " + serviceClassName + " (removing from registry)", ex);
                    deregister.add(serviceClassName);
                }
            }
            // avoid repeated attempts to recompile until cache is refreshed
            dynamicServices.get(serviceInterface.getName()).removeAll(deregister);
        }
        return dynServices;
    }

    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> List<Class<T>> getDynamicServiceClasses(Class<T> serviceInterface) {
        List<Class<T>> dynServiceClasses = new ArrayList<Class<T>>();
        if (dynamicServices.containsKey(serviceInterface.getName())) {
            Set<String> deregister = new HashSet<String>();
            for (String serviceClassName : dynamicServices.get(serviceInterface.getName())) {
                try {
                    Class<?> clazz = CompiledJavaCache.getClassFromAssetName(null, serviceClassName);
                    if (clazz != null)  {
                        dynServiceClasses.add(serviceInterface.getClass().cast(clazz));
                    }
                } catch (Exception ex) {
                    logger.severeException("Failed to get Dynamic Java service class: " + serviceClassName + " (removing from registry)", ex);
                    deregister.add(serviceClassName);
                }
            }
            // avoid repeated attempts to recompile until cache is refreshed
            dynamicServices.get(serviceInterface.getName()).removeAll(deregister);
        }
        return dynServiceClasses;
    }

    /**
     * Add Dynamic Java Registered Service class names for each service
     * @param serviceInterface
     * @param className
     */
    public void addDynamicService(String serviceInterface, String className) {
        if (dynamicServices.containsKey(serviceInterface)) {
            dynamicServices.get(serviceInterface).add(className);
        }
        else {
            Set<String> classNamesSet = new HashSet<String>();
            classNamesSet.add(className);
            dynamicServices.put(serviceInterface, classNamesSet);
        }
    }

    public void addDynamicService(String serviceInterface, String className, String path) {
        addDynamicService(serviceInterface, className);
        // prefix path with pkg name for full service path
        String regPath = path;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            regPath = className.substring(0, lastDot).replace('.', '/') + path;
        }
        pathToDynamicServiceClass.put(regPath, className);
    }

    public void addDynamicServices(String serviceInterface, Set<String> classNames) {
        if (dynamicServices.containsKey(serviceInterface)) {
            dynamicServices.get(serviceInterface).addAll(classNames);
        }
        else {
            dynamicServices.put(serviceInterface, classNames);
        }
    }

    /**
     * Clear the list while clearing the cache
     */
    public void clearDynamicServices() {
        logger.info("Clearing Dynamic services cache in : " + getClass().getName());
        dynamicServices.clear();
        pathToDynamicServiceClass.clear();
    }

    /**
     * Default is true
     */
    protected boolean isEnabled(RegisteredService service) {
        return true;
    }

}
