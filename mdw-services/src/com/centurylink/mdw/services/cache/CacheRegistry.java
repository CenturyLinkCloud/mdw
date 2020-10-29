package com.centurylink.mdw.services.cache;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.util.*;

public class CacheRegistry extends ServiceRegistry {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private final Map<String,CacheService> byName = new HashMap<>();

    protected CacheRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static CacheRegistry instance;
    public synchronized static CacheRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<>();
            services.add(CacheService.class);
            instance = new CacheRegistry(services);
        }
        return instance;
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

    public void refresh(String name) {
        CacheService provider = byName.get(name);
        if (provider != null) {
            logger.info("Refresh cache " + name + " (class=" + provider.getClass().getName() + ")");
            try {
                provider.refreshCache();
            } catch (Exception ex) {
                logger.error("Failed to refresh cache: " + name, ex);
            }
        }
    }

    public void register(String name, CacheService provider) {
        synchronized(byName) {
            byName.put(name, provider);
        }
    }
}
