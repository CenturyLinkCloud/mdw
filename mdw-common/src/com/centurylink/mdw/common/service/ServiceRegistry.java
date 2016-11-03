/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
     * To get the Dynamic java class for Registered Service
     * @param processPackageVO
     * @param serviceInterface
     * @param className
     * @return
     */
    public <T extends RegisteredService> T getDynamicService(Package processPackageVO, Class<T> serviceInterface, String className) {
        if (dynamicServices.containsKey(serviceInterface.getName())
                && dynamicServices.get(serviceInterface.getName()).contains(className)) {
            try {
                ClassLoader parentClassLoader = processPackageVO == null ? getClass().getClassLoader() : processPackageVO.getClassLoader();
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
        String className = pathToDynamicServiceClass.get(resourcePath);
        if (className != null)
            return getDynamicService(pkg, serviceInterface, className);
        return null;
    }

    public String getPathForDynamicService(Class<? extends RegisteredService> serviceClass) {
        for (String path : pathToDynamicServiceClass.keySet()) {
            if (serviceClass.equals(pathToDynamicServiceClass.get(path)))
                return path;
        }
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
        pathToDynamicServiceClass.put(path, className);
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
