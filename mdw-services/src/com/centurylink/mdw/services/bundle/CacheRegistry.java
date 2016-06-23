/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.cache.AssetCache;
import com.centurylink.mdw.common.cache.FixedCapacityCache;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.common.service.ServiceRegistryException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

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

    @Override
    public void startupDynamicServices(final BundleContext bundleContext) {
        // To get dynamic services before startup
        this.getDynamicCacheServices();
        super.startupDynamicServices(bundleContext);
    }

    protected void registerDynamicServices(BundleContext bundleContext, String name, RegisteredService dynamicService) {
        CacheService cacheService = (CacheService) dynamicService;
        synchronized(byDynamicName){
            byDynamicName.put(name, cacheService);
        }
    }

    @Override
    protected boolean onRegister(BundleContext bundleContext, RegisteredService service, Map<String,String> props)
    throws ServiceRegistryException {
        CacheService cacheService = (CacheService) service;
        String bundleName = bundleContext.getBundle().getSymbolicName();
        if (cacheService instanceof PreloadableCache) {
            PreloadableCache preloadable = (PreloadableCache) cacheService;
            logger.info("Preloading cache '" + service.getClass().getName() + "' from bundle " + bundleName);
            preloadable.initialize(props);
            try {
              preloadable.loadCache();
            }
            catch (CachingException ex) {
                throw new ServiceRegistryException(ex.getMessage(), ex);
            }
        }
        else if (cacheService instanceof FixedCapacityCache) {
            FixedCapacityCache fixedCap = (FixedCapacityCache) cacheService;
            fixedCap.setCapacity(Integer.parseInt(props.get("capacity")));
        }
        synchronized(byName) {
            byName.put(props.get("alias"), cacheService);
        }
        if (props.containsKey("dependsOn")) {
            synchronized(depedencyCacheMap) {
                depedencyCacheMap.put(props.get("alias"), props.get("dependsOn"));
            }
        }
        return isEnabled(service);
    }

    @Override
    protected void onUnregister(BundleContext bundleContext, RegisteredService service, Map<String,String> props)
    throws ServiceRegistryException {
        synchronized(byDynamicName){
            logger.info("Unregistering " + service.getClass().getName());
            byDynamicName.remove(props.get("alias"));
        }
    }

    public void refreshAll(List<String> excludedFormats) {
        if (excludedFormats == null || !excludedFormats.contains(RuleSetVO.JAVA))
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
            if (excluded != null && provider instanceof AssetCache && excluded.contains(((AssetCache)provider).getFormat())) {
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
