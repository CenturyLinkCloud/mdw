/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;

public class ProviderRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    public static final List<String> providerServices = new ArrayList<String>(Arrays.asList(new String[] {ActivityProvider.class.getName(),
            EventHandlerProvider.class.getName(), VariableTranslatorProvider.class.getName()}));
    private Map<String,List<Provider<?>>> providers = new HashMap<String,List<Provider<?>>>();
    private Map<String,Set<String>> dynamicProviders = new HashMap<String,Set<String>>();

    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            list.add((Provider<T>)provider);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> List<PackageAwareProvider<T>> getPackageAwareProviders(Class<? extends Provider<?>> providerType) {
        List<PackageAwareProvider<T>> list = new ArrayList<PackageAwareProvider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            if (provider instanceof PackageAwareProvider)
              list.add((PackageAwareProvider<T>)provider);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getNonPackageAwareProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        for (Provider<?> provider : providers.get(providerType.getName())) {
            if (!(provider instanceof PackageAwareProvider))
              list.add((Provider<T>)provider);
        }
        return list;
    }

    protected ProviderRegistry() {
    }

    private static ProviderRegistry instance;
    public static ProviderRegistry getInstance() {
        if (instance == null) {
            instance = new ProviderRegistry();
            instance.providers.put(ActivityProvider.class.getName(), new ArrayList<Provider<?>>());
            instance.providers.put(EventHandlerProvider.class.getName(), new ArrayList<Provider<?>>());
            instance.providers.put(VariableTranslatorProvider.class.getName(), new ArrayList<Provider<?>>());
        }
        return instance;
    }

        /**
     * @param serviceInterface
     * @param className
     */
    public void addDynamicProvider(String serviceInterface, String className) {
        if (dynamicProviders.containsKey(serviceInterface)) {
            dynamicProviders.get(serviceInterface).add(className);
        }
        else {
            Set<String> classNamesSet = new HashSet<String>();
            classNamesSet.add(className);
            dynamicProviders.put(serviceInterface, classNamesSet);
        }
    }

    public void clearDynamicProviders() {
        logger.mdwDebug("Clearing Dynamic providers cache in : " + getClass().getName());
        dynamicProviders.clear();
    }

    public Map<String,Set<String>> getDynamicProviders() {
        return dynamicProviders;
    }

    /**
     * To get all dynamic java Registered service providers
     * @param providerType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> List<Provider<T>> getDynamicProviders(Class<? extends Provider<?>> providerType) {
        List<Provider<T>> list = new ArrayList<Provider<T>>();
        if (dynamicProviders.containsKey(providerType.getName())) {
            for (String dynamicProviderName : dynamicProviders.get(providerType.getName())) {
                try {
                    Class<?> clazz = CompiledJavaCache.getClassFromAssetName(getClass().getClassLoader(), dynamicProviderName);
                    if (clazz != null)
                        list.add((Provider<T>) (clazz).newInstance());
                }
                catch (Exception ex) {
                    logger.severeException("Failed to get the dynamic provider type: " +providerType.getName() +" class: " + dynamicProviderName + " \n " + ex.getMessage(), ex);
                }
            }
        }
        return list;
    }

    /**
     * To get dynamic java variable translator
     * @param translatorClass
     * @param classLoader
     * @return
     */
    public VariableTranslator getDynamicVariableTranslator(String className, ClassLoader parentLoader) {
        try {
            Class<?> clazz = CompiledJavaCache.getClassFromAssetName(parentLoader, className);
            if (clazz != null)
                return (VariableTranslator) (clazz).newInstance();
        }
        catch (Exception ex) {
            logger.trace("Dynamic VariableTranslatorProvider not found: " + className);
        }
        return null;
    }
}
