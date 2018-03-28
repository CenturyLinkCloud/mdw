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
