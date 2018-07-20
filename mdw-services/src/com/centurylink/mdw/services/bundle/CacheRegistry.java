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
package com.centurylink.mdw.services.bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.ExcludableCache;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class CacheRegistry extends ServiceRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Map<String,CacheService> byName = new HashMap<String,CacheService>();
    private Map<String,CacheService> byDynamicName = new HashMap<String,CacheService>();
    private Map<String,String> depedencyCacheMap = new HashMap<String, String>();

    protected CacheRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static CacheRegistry instance;
    public synchronized static CacheRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(CacheService.class);
            instance = new CacheRegistry(services);
        }
        return instance;
    }

    public List<CacheService> getCacheServices() {
        return getServices(CacheService.class);
    }

    public List<CacheService> getDynamicCacheServices() {
        if (getDynamicServices().isEmpty()) {
            this.addDynamicCacheServices();
        }
        return super.getDynamicServices(CacheService.class);
    }

    private void addDynamicCacheServices() {
        Set<String> dynamicCacheServices = DynamicJavaServiceRegistry.getRegisteredServices(CacheService.class.getName());
        if (dynamicCacheServices != null)
            super.addDynamicServices(CacheService.class.getName(), dynamicCacheServices);
    }

    public void refreshAll(List<String> excludedFormats) {
        if (excludedFormats == null || !excludedFormats.contains(Asset.JAVA))
          refreshAllDynamicCache();
        List<String> toRefresh = getNamesBasedOnDependencyHierarchy();
        synchronized(byName) {
            for (String name : byName.keySet()) {
                if (!name.equals(PropertyManager.class.getName())) {
                    if (!toRefresh.contains(name))
                        toRefresh.add(name);
                }
            }
        }
        logger.info("cache refresh order : "+toRefresh);
        // avoid concurrent modification
        for (String name : toRefresh) {
            refresh(name, excludedFormats);
        }
    }

    /**
     *
     */
    public void refreshAllDynamicCache() {
        if (byDynamicName.isEmpty()) {
            List<CacheService> cacheServices = getDynamicCacheServices();
            synchronized(byDynamicName){
                for (CacheService dynamicService : cacheServices) {
                    byDynamicName.put(dynamicService.getClass().getName(), dynamicService);
                }
            }
        }
        synchronized(byDynamicName){
            for (String name : byDynamicName.keySet()) {
                refresh(name);
            }

            byDynamicName.clear();
        }
        super.clearDynamicServices();
    }

    public void refresh(String name) {
        refresh(name, null);
    }

    public void refresh(String name, List<String> excluded) {
        CacheService provider = byName.get(name);
        if (provider == null)
            provider = byDynamicName.get(name);
        if (provider != null) {
            if (excluded != null && provider instanceof ExcludableCache && excluded.contains(((ExcludableCache)provider).getFormat())) {
                logger.debug("Omitting cache " + name + " (class=" + provider.getClass().getName() + ")");
            }
            else {
                logger.info("Refresh cache " + name + " (class=" + provider.getClass().getName() + ")");
                try {
                    provider.refreshCache();
                } catch (Exception ex) {
                    logger.severeException("Failed to refresh cache: " + name, ex);
                }
            }
        }
    }

    public void register(String name, CacheService provider) {
        synchronized(byName) {
            byName.put(name, provider);
        }
    }

    // Order the caches based on dependency hierarchy (dependency chain)
    private List<String> getNamesBasedOnDependencyHierarchy() {
        List<String> orderedCaches = new ArrayList<String>();
        synchronized(depedencyCacheMap) {
            for (String key : depedencyCacheMap.keySet()) {
                int keyIndex = orderedCaches.indexOf(key);
                for (String cache : depedencyCacheMap.get(key).split(",")) {
                    int cacheIdx = orderedCaches.indexOf(cache);
                    if (keyIndex >= 0 && cacheIdx >= 0 && cacheIdx > keyIndex) {
                        orderedCaches.remove(cache);
                        orderedCaches.add(keyIndex, cache);
                    }
                    if (cacheIdx < 0) {
                        if (keyIndex < 0)
                            orderedCaches.add(cache);
                        else
                            orderedCaches.add(keyIndex, cache);
                    }
                }
                if (keyIndex < 0)
                    orderedCaches.add(key);
            }
        }
        return orderedCaches;
    }
}
