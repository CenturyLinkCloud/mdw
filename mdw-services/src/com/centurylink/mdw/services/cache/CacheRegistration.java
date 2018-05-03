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
package com.centurylink.mdw.services.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;
import org.json.JSONObject;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.bpm.ApplicationCacheDocument.ApplicationCache;
import com.centurylink.mdw.bpm.CacheDocument.Cache;
import com.centurylink.mdw.bpm.PropertyDocument.Property;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.ExcludableCache;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.services.bundle.CacheRegistry;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.system.SystemMessages;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Startup class that manages registration of all the caches
 */

public class CacheRegistration implements StartupService {

    private static final String APPLICATION_CACHE_FILE_NAME = "application-cache.xml";
    // following 2 lines cannot be initialized in onStartup() - too late
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<String,CacheService> allCaches = new LinkedHashMap<>();

    private static CacheRegistration instance;
    public static synchronized CacheRegistration getInstance() {
        if (instance == null)
            instance = new CacheRegistration();
        return instance;
    }


    /**
     * Method that gets invoked when the server comes up
     * Load all the cache objects when the server starts
     *
     * @throws StartupException
     */
    public void onStartup() {
        try {
            preloadCaches();
            SpringAppContext.getInstance().loadPackageContexts();  // trigger dynamic context loading
            preloadDynamicCaches();
            performInitialRequest();
        }
        catch (Exception ex){
            String message = "Failed to load caches";
            logger.severeException(message, ex);
            throw new StartupException(message, ex);
        }
    }

    /**
     * Method that gets invoked when the server comes up
     * Load all the cache objects when the server starts
     * @throws Exception
     * @throws StartupException
     */
    private void preloadCaches() throws IOException, XmlException {
        Map<String,Properties> caches = getPreloadCacheSpecs();
        for (String cacheName : caches.keySet()) {
            Properties cacheProps = (Properties)caches.get(cacheName);
            String cacheClassName = cacheProps.getProperty("ClassName");
            logger.info(" - loading cache " + cacheName);
            CacheService cachingObj = getCacheInstance(cacheClassName, cacheProps);
            if (cachingObj != null) {
                long before = System.currentTimeMillis();
                if (cachingObj instanceof PreloadableCache) {
                    ((PreloadableCache)cachingObj).loadCache();
                }
                synchronized(allCaches) {
                    allCaches.put(cacheName, cachingObj);
                }
                logger.debug("    - " + cacheName + " loaded in " + (System.currentTimeMillis() - before) + " ms");
            }
            else {
                logger.warn("Caching Class is  invalid. Name-->"+cacheClassName);
            }
        }
    }

    /**
     * Load caches registered as dynamic java services.
     */
    private void preloadDynamicCaches() {
        List<CacheService> dynamicCacheServices = CacheRegistry.getInstance().getDynamicCacheServices();
        for (CacheService dynamicCacheService : dynamicCacheServices) {
            if (dynamicCacheService instanceof PreloadableCache) {
                try {
                    PreloadableCache preloadableCache = (PreloadableCache)dynamicCacheService;
                    RegisteredService regServ = preloadableCache.getClass().getAnnotation(RegisteredService.class);
                    Map<String,String> params = new HashMap<>();
                    Parameter[] parameters = regServ.parameters();
                    if (parameters != null) {
                        for (Parameter parameter : parameters) {
                            if (parameter.name().length() > 0)
                                params.put(parameter.name(), parameter.value());
                        }
                    }
                    preloadableCache.initialize(params);
                    preloadableCache.loadCache();
                }
                catch (Exception ex) {
                    logger.severeException("Failed to preload " + dynamicCacheService.getClass(), ex);
                }
            }
            synchronized(allCaches) {
                allCaches.put(dynamicCacheService.getClass().getName(), dynamicCacheService);
            }
        }
    }

    private CacheService getCacheInstance(String className, Properties cacheProps) {
        try {
            Class<? extends CacheService> cl = Class.forName(className).asSubclass(CacheService.class);
            CacheService cache = cl.newInstance();
            if (cache instanceof PreloadableCache) {
                ((PreloadableCache)cache).initialize(getMap(cacheProps));
            }
            return cache;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    private Map<String,String> getMap(Properties properties) {
        if (properties == null)
            return null;

        Map<String,String> params = new HashMap<String,String>();
        for (Object name : properties.keySet()) {
            if (name != null)
              params.put(name.toString(), properties.getProperty(name.toString()));
        }
        return params;
    }

    /**
     * Method that gets invoked when the server
     * shuts down
     */
    public void onShutdown(){
        CacheRegistry.getInstance().clearDynamicServices();// clear dynamic cache services
        synchronized (allCaches) {
            for (String cacheName : allCaches.keySet()) {
                CacheService cachingObj= allCaches.get(cacheName);
                cachingObj.clearCache();
            }
        }
    }

    public void refreshCaches() throws StartupException {
        refreshCaches(null);
    }

    public synchronized void refreshCaches(List<String> excludedFormats) throws StartupException {
        Bulletin bulletin = null;
        try {
            bulletin = SystemMessages.bulletinOn("Cache refresh in progress...");
            String propmgr = PropertyManager.class.getName();
            if (excludedFormats == null || !excludedFormats.contains("PROPERTIES"))
                refreshCache(propmgr);
            if (excludedFormats == null || !excludedFormats.contains(Asset.JAVA))
                CacheRegistry.getInstance().clearDynamicServices();
            synchronized (allCaches) {
                for (String cacheName : allCaches.keySet()) {
                    if (!cacheName.equals(propmgr))
                        refreshCache(cacheName, excludedFormats);
                }
            }
            SpringAppContext.getInstance().loadPackageContexts();  // trigger dynamic context loading
            performInitialRequest();
            SystemMessages.bulletinOff(bulletin, "Cache refresh completed");

        } catch (Exception ex) {
            String message = "Failed to load caches";
            logger.severeException(message, ex);
            if (bulletin != null) {
                SystemMessages.bulletinOff(bulletin, Level.Error, "Cache refresh failed");
            }
            throw new StartupException(message, ex);
        }
    }

    public void refreshCache(String cacheName) {
        refreshCache(cacheName, null);
    }

    public CacheService getCache(String name) {
        return allCaches.get(name);
    }

    /**
     * Refreshes a particular cache by name.
     * @param name the cache to refresh
     */
    public void refreshCache(String cacheName, List<String> excludedFormats) {
        CacheService cache = allCaches.get(cacheName);
        if (cache != null) {
            if (excludedFormats != null && cache instanceof ExcludableCache && excludedFormats.contains(((ExcludableCache)cache).getFormat())) {
                logger.debug(" - omitting cache " + cacheName);
            }
            else {
                logger.info(" - refresh cache " + cacheName);
                try {
                    cache.refreshCache();
                } catch (Exception e) {
                    logger.severeException("failed to refresh cache", e);
                }
            }
        }
    }

    private Map<String,Properties> getPreloadCacheSpecs() throws IOException, XmlException {
        Map<String,String> depedencyCacheMap = new HashMap<String, String>();
        ApplicationCacheDocument appCacheDoc = null;
        Map<String,Properties> retPropsTemp = new HashMap<String,Properties>();
        Map<String,Properties> retProps = new LinkedHashMap<String,Properties>();
        List<String> tempList;
        try (InputStream stream = FileHelper.openConfigurationFile(APPLICATION_CACHE_FILE_NAME)) {
            appCacheDoc = ApplicationCacheDocument.Factory.parse(stream, Compatibility.namespaceOptions());
            ApplicationCache appCache = appCacheDoc.getApplicationCache();
            for (Cache cache : appCache.getCacheList()) {
                Properties cacheProps = new Properties();
                for (Property prop : cache.getPropertyList()) {
                    if ("dependsOn".equalsIgnoreCase(prop.getName()))
                        depedencyCacheMap.put(cache.getName(), prop.getStringValue());
                    cacheProps.put(prop.getName(), prop.getStringValue());
                }
                retPropsTemp.put(cache.getName(), cacheProps);
            }
            tempList = getNamesBasedOnDependencyHierarchy(depedencyCacheMap);

            for (String cache : tempList) {
                if (!cache.equals(PropertyManager.class.getName()))
                    retProps.put(cache, retPropsTemp.get(cache));
            }

            for (String name : retPropsTemp.keySet()) {
                if (!name.equals(PropertyManager.class.getName())) {
                    if (!retProps.containsKey(name))
                        retProps.put(name, retPropsTemp.get(name));
                }
            }
            return retProps;
        }
    }

    public void registerCache(String name, CacheService cache) {
        logger.info("Register cache " + name);
        synchronized(allCaches) {
            allCaches.put(name, cache);
        }
    }

    public static void broadcastRefresh(String cacheNames, InternalMessenger messenger) {
        try {
            JSONObject json = new JsonObject();
            json.put("ACTION", "REFRESH_CACHES");
            if (!StringHelper.isEmpty(cacheNames)) json.put("CACHE_NAMES", cacheNames);
            messenger.broadcastMessage(json.toString());
        } catch (Exception e) {
            logger.severeException("Failed to publish cashe refresh message", e);
        }
    }

    private List<String> getNamesBasedOnDependencyHierarchy(Map<String,String> depedencyCacheMap) {
        List<String> orderedCaches = new ArrayList<String>();
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
        return orderedCaches;
    }

    /**
     * After all caches have been loaded or refreshed (startup or a cache refresh request),
     * call this method to eliminate "first request" performance penalty.
     */
    public static void performInitialRequest() {
        try {
            logger.info("Submit initial request.");
            HttpHelper helper = new HttpHelper(new URL(ApplicationContext.getLocalServiceUrl() + "/services/AppSummary"));
            helper.setConnectTimeout(1000);
            helper.setReadTimeout(1000);
            helper.get();
        }
        catch (SocketTimeoutException ex) {
            // no need to wait for response
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}