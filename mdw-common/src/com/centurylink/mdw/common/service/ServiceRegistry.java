/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.process.PackageVO;

public class ServiceRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Map<String,List<RegisteredService>> services = new HashMap<String,List<RegisteredService>>();
    private Map<String,ServiceListener> serviceListeners = new HashMap<String,ServiceListener>();
    private Map<String,Set<String>> dynamicServices = new HashMap<String,Set<String>>(); // Dynamic java Registered services
    private Map<String,String> pathToDynamicServiceClass = new HashMap<String,String>(); // resource paths

    private List<Class<? extends RegisteredService>> serviceInterfaces;

    protected ServiceRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        this.serviceInterfaces = serviceInterfaces;
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
    public <T extends RegisteredService> T getDynamicService(PackageVO processPackageVO, Class<T> serviceInterface, String className) {
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
                logger.severeException("Failed to get the dynamic registered service : " + className +" \n "+ex.getMessage(), ex);
            }
        }
        return null;
    }

    public <T extends RegisteredService> T getDynamicServiceForPath(PackageVO pkg, Class<T> serviceInterface, String resourcePath) {
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

    public void startup(final BundleContext bundleContext) throws InvalidSyntaxException {
        for (Class<? extends RegisteredService> serviceInterface : serviceInterfaces) {
            serviceListeners.put(serviceInterface.getName(), register(bundleContext, serviceInterface));
        }
    }

    /**
     * @param bundleContext
     * @throws InvalidSyntaxException
     */
    public void shutdown(final BundleContext bundleContext) throws InvalidSyntaxException {
        List<String> toRemove = new ArrayList<String>();
        for (String serviceInterface : serviceListeners.keySet()) {
            unregister(bundleContext, serviceListeners.get(serviceInterface), serviceInterface);
            toRemove.add(serviceInterface);
        }
        for (String remove : toRemove)
          serviceListeners.remove(remove);
    }

    /**
     * To register dynamic services (Cache Services and Startup Services)
     * @param bundleContext
     */
    public void startupDynamicServices(final BundleContext bundleContext) {
        for (Class<? extends RegisteredService> serviceInterface : serviceInterfaces) {
            for (RegisteredService dynamicService : getDynamicServices(serviceInterface)) {
                registerDynamicServices(bundleContext, JavaNaming.getClassName(dynamicService.getClass().getName()), dynamicService);
            }
        }
    }

    public void shutdownDynamicServices(final BundleContext bundleContext) {
        for (Class<? extends RegisteredService> serviceInterface : serviceInterfaces) {
            for (RegisteredService dynamicService : getDynamicServices(serviceInterface)) {
                unregisterDynamicServices(bundleContext, JavaNaming.getClassName(dynamicService.getClass().getName()), dynamicService);
            }
        }
        clearDynamicServices();
    }

    protected ServiceListener register(final BundleContext bundleContext, final Class<? extends RegisteredService> serviceInterface)
    throws InvalidSyntaxException {

        // registered services
        ServiceListener serviceListener = new ServiceListener() {
            public void serviceChanged(ServiceEvent ev) {
                ServiceReference serviceRef = ev.getServiceReference();
                RegisteredService service = (RegisteredService)bundleContext.getService(serviceRef);
                Map<String,String> serviceProps = new HashMap<String,String>();
                for (String key : serviceRef.getPropertyKeys()) {
                    Object value = serviceRef.getProperty(key);
                    if (value != null)
                      serviceProps.put(key, value.toString());
                }
                switch (ev.getType()) {
                    case ServiceEvent.REGISTERED: {
                        try {
                            if (onRegister(bundleContext, service, serviceProps))
                                services.get(serviceInterface.getName()).add(service);
                        }
                        catch (Exception ex) {
                            logger.severeException("Unable to register service: " + service.getClass().getName(), ex);
                        }
                    }
                    break;
                    case ServiceEvent.UNREGISTERING: {
                        if (service != null) {
                            try {
                                onUnregister(bundleContext, service, serviceProps);
                            }
                            catch (Exception ex) {
                                logger.severeException("Error unregistering service: " + service.getClass().getName(), ex);
                            }
                            services.get(serviceInterface.getName()).remove(service);
                        }
                    }
                    break;
                }
            }
        };

        String filter = "(objectclass=" + serviceInterface.getName() + ")";
        // notify previously started services
        ServiceReference[] serviceRefs = bundleContext.getServiceReferences(null, filter);
        if (serviceRefs != null) {
            for (ServiceReference serviceRef : serviceRefs) {
                serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, serviceRef));
            }
        }
        bundleContext.addServiceListener(serviceListener, filter);

        return serviceListener;
    }

    protected void unregister(final BundleContext bundleContext, final ServiceListener serviceListener, final String serviceInterfaceName)
    throws InvalidSyntaxException {
        if (serviceListener != null) {
            ServiceReference[] serviceRefs = bundleContext.getServiceReferences(null, "(objectclass=" + serviceInterfaceName + ")");
            if (serviceRefs != null) {
                for (ServiceReference serviceRef : serviceRefs) {
                    serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, serviceRef));
                }
            }
        }
    }

    /**
     * Default is true
     */
    protected boolean isEnabled(RegisteredService service) {
        return true;
    }

    protected boolean onRegister(BundleContext bundleContext, RegisteredService service, Map<String,String> serviceProps) throws ServiceRegistryException {
        boolean enabled = isEnabled(service);
        if (enabled)
            logger.info("Registering " + service.getClass().getName() + " from bundle " + bundleContext.getBundle().getSymbolicName());
        else
            logger.debug("Disabling service " + service.getClass().getName() + " from bundle " + bundleContext.getBundle().getSymbolicName());
        return enabled;
    }

    protected void onUnregister(BundleContext bundleContext, RegisteredService service, Map<String,String> serviceProps) throws ServiceRegistryException {
        logger.info("Unregistering " + service.getClass().getName());
    }

    /**
     * @param name
     * @param dynamicService
     */
    protected void registerDynamicServices(BundleContext bundleContext, String name, RegisteredService dynamicService) {
        logger.info("Registering Dynamic services " + name);
    }

    /**
     * @param bundleContext
     * @param name
     * @param dynamicService
     */
    protected void unregisterDynamicServices(BundleContext bundleContext, String name, RegisteredService dynamicService) {
        logger.info("Unregistering Dynamic services " + dynamicService.getClass().getName());
    }
}
