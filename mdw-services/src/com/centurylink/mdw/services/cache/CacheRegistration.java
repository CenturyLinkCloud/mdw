/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.cache;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.bpm.ApplicationCacheDocument.ApplicationCache;
import com.centurylink.mdw.bpm.CacheDocument.Cache;
import com.centurylink.mdw.bpm.PropertyDocument.Property;
import com.centurylink.mdw.cache.CacheEnabled;
import com.centurylink.mdw.cache.ExcludableCache;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.provider.CacheService;
import com.centurylink.mdw.services.bundle.CacheRegistry;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.startup.StartupClass;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;


/**
 * Startup class that manages registration of all the caches
 */

public class CacheRegistration implements StartupClass {

    private static final String APPLICATION_CACHE_FILE_NAME = "application-cache.xml";
    // following 2 lines cannot be initialized in onStartup() - too late
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<String,CacheEnabled> allCaches
        = new LinkedHashMap<String,CacheEnabled>();

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
    public void onStartup() throws StartupException{
        try{
            preloadCache();
            SpringAppContext.getInstance().loadPackageContexts();  // trigger dynamic context loading
            preloadDynamicCache();
            performInitialRequest();
        } catch(Exception ex){
            String message = "Failed to load caches";
            logger.severeException(message, ex);
            throw new StartupException(-1, message, ex);
        }
    }

    /**
     * Method that gets invoked when the server comes up
     * Load all the cache objects when the server starts
     * @throws Exception
     * @throws StartupException
     */
    private void preloadCache() throws Exception {
        Map<String,Properties> caches = getPreloadCacheSpecs();
        for (String cacheName : caches.keySet()) {
            Properties cacheProps = (Properties)caches.get(cacheName);
            String cacheClassName = cacheProps.getProperty("ClassName");
            logger.info(" - loading cache " + cacheName);
            CacheEnabled cachingObj = getCacheInstance(cacheClassName, cacheProps);

            if (cachingObj != null) {
                if (cachingObj instanceof PreloadableCache) {
                    ((PreloadableCache)cachingObj).loadCache();
                }
                synchronized(allCaches) {
                    allCaches.put(cacheName, cachingObj);
                }
            }
            else {
                logger.warn("Caching Class is  invalid. Name-->"+cacheClassName);
            }
        }
    }

    /**
     * To load the cache that registered through dynamic java services
     *
     */
    private void preloadDynamicCache() throws Exception {
        List<CacheService> dynamicCacheServices = CacheRegistry.getInstance().getDynamicCacheServices();
        for (CacheService dynamicCacheService : dynamicCacheServices) {
            if (dynamicCacheService instanceof PreloadableCache) {
                ((PreloadableCache) dynamicCacheService).loadCache();
            }
            synchronized(allCaches) {
                allCaches.put(dynamicCacheService.getClass().getName(), dynamicCacheService);
            }
        }
    }

    private CacheEnabled getCacheInstance(String className, Properties cacheProps) {
        try {
            Class<?> cl = Class.forName(className);
            if (cacheProps == null)
                return (CacheEnabled) cl.newInstance();
            Class<?>[] argTypes = new Class<?>[] { Map.class };
            try {
                Constructor<?> constructor = cl.getConstructor(argTypes);
                return (CacheEnabled) constructor.newInstance(getMap(cacheProps));
            }
            catch (NoSuchMethodException notFound) {
                return (CacheEnabled) cl.newInstance();
            }
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
                CacheEnabled cachingObj= allCaches.get(cacheName);
                cachingObj.clearCache();
            }
        }
    }

    public void refreshCaches() throws StartupException {
        refreshCaches(null);
    }

    public void refreshCaches(List<String> excludedFormats) throws StartupException {
        try {
            String propmgr = PropertyManager.class.getName();
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

        } catch (Exception ex) {
             String message = "Failed to load caches";
             logger.severeException(message, ex);
             throw new StartupException(-1, message, ex);
        }
    }

    public void refreshCache(String cacheName) {
        refreshCache(cacheName, null);
    }

    /**
     * Refreshes a particular cache by name.
     * @param name the cache to refresh
     */
    public void refreshCache(String cacheName, List<String> excludedFormats) {
        CacheEnabled cache = allCaches.get(cacheName);
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

 /**
     * Returns the list of all the start up classes that has been
     * defined in the application properties file
     * @throws Exception
     * @return Collection of StartUPClasses
     */
    private Map<String,Properties> getPreloadCacheSpecs() throws Exception {

        Map<String,String> depedencyCacheMap = new HashMap<String, String>();
        ApplicationCacheDocument appCacheDoc = null;
        InputStream stream = null;
        Map<String,Properties> retPropsTemp = new HashMap<String,Properties>();
        Map<String,Properties> retProps = new LinkedHashMap<String,Properties>();
        List<String> tempList;
        try {
            stream = FileHelper.openConfigurationFile(APPLICATION_CACHE_FILE_NAME);
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
        } catch (Throwable e) {
            logger.severeException("Failed to load cache configuration", e);
        } finally {
            if (stream!=null) stream.close();
        }
        return retProps;

    }

    public void registerCache(String name, CacheEnabled cache) {
        logger.info("Register cache " + name);
        synchronized(allCaches) {
            allCaches.put(name, cache);
        }
    }

    public static void broadcastRefresh(String cacheNames, InternalMessenger messenger) {
        try {
            JSONObject json = new JSONObject();
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

    // After all caches have been loaded or refreshed (startup or a cache refresh request), call this method to eliminate "first request" performance penalty
    // No need to wait for response
    public static void performInitialRequest() {
        try {
            logger.info("Performing initial request...");
            HttpHelper helper = new HttpHelper(new URL(ApplicationContext.getLocalServiceUrl() + "/Services/SystemInfo?type=threadDumpCount&format=text"));
            helper.setConnectTimeout(1000);
            helper.setReadTimeout(1000);
            helper.get();
        }
        catch(Exception ex) {}
    }
}