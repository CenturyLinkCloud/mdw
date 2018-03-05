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
package com.centurylink.mdw.services.pooling;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.monitor.UnscheduledEvent;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Startup class that manages registration of all the caches
 */

public class ConnectionPoolRegistration implements StartupService, CacheService {

    private static ConnectionPoolRegistration singleton;

    private Map<String,AdapterConnectionPool> pools;

    public void onStartup() {
        singleton = this;
        (new CacheRegistration()).registerCache(ConnectionPoolRegistration.class.getName(), singleton);
        pools = new HashMap<String,AdapterConnectionPool>();
        StandardLogger logger = LoggerUtil.getStandardLogger();
        try {
            Map<String, Properties> poolSpecs = getAllPoolProperties();
            for (String name : poolSpecs.keySet()) {
                Properties props = poolSpecs.get(name);
                AdapterConnectionPool pool = this.addPool(name, props);
                try {
                    if (pool.isDisabled()) {
                        logger.info(" - connection pool is disabled - " + name);
                    } else {
                        pool.start();
                        logger.info(" - connection pool is started " + name);
//                        pool.processWaitingQueue();        does not work here - WorkManager scheduling does nothing
                    }
                } catch (Exception e) {
                    logger.severeException("Failed to start connection pool " + name, e);
                }
            }
        } catch (Exception e) {
            logger.severeException("Failed to initialize connection pools", e);
        }
    }

    /**
     * Method that gets invoked when the server
     * shuts down
     */
    public void onShutdown(){
        if (singleton==null) return;
        for (String name : singleton.pools.keySet()) {
            AdapterConnectionPool pool = singleton.pools.get(name);
            pool.shutdown(true);
        }
    }

    public static AdapterConnectionPool getPool(String name) {
        return singleton==null?null:singleton.pools.get(name);
    }

    public static Set<String> getPoolNames() {
        return singleton.pools.keySet();
    }

    public static void removePool(String name) {
        singleton.pools.remove(name);
    }

    public static AdapterConnectionPool addPool(String name) {
        Properties props = new Properties();
        return singleton.addPool(name, props);
    }

    private Map<String, Properties> getAllPoolProperties() throws IOException, PropertyException {
        Map<String, Properties> pools = new HashMap<String, Properties>();
        PropertyManager propmgr = PropertyUtil.getInstance().getPropertyManager();
        Properties timerTasksProperties = propmgr.getProperties(PropertyNames.MDW_CONNECTION_POOL);
         for (Object key : timerTasksProperties.keySet()) {
             String pn = (String)key;
             String[] pnParsed = pn.split("\\.");
             if (pnParsed.length==5) {
                 String name = pnParsed[3];
                 String attrname = pnParsed[4];
                 Properties procspec = pools.get(name);
                 if (procspec==null) {
                     procspec = new Properties();
                     pools.put(name, procspec);
                 }
                 String value = timerTasksProperties.getProperty(pn);
                 procspec.put(attrname, value);
             }
         }
        return pools;
    }

    private AdapterConnectionPool addPool(String name, Properties props) {
        AdapterConnectionPool pool = new AdapterConnectionPool(name, props);
        (new CacheRegistration()).registerCache(AdapterConnectionPool.class.getName() + ":" + name, pool);
        pools.put(name, pool);
        return pool;
    }

    public void clearCache() {
    }

    public void refreshCache() throws Exception {
        // this is called when a new connection pool is added or an existing connection pool is deleted
//        Thread.sleep(2000);        // wait till database update committed
//        PropertyManager.getInstance().refresh();
        Map<String, Properties> poolSpecs = getAllPoolProperties();
        for (String name : poolSpecs.keySet()) {
            if (!pools.containsKey(name)) {        // new pool
                addPool(name, poolSpecs.get(name));
            }
        }
        for (String name : pools.keySet()) {
            if (!poolSpecs.containsKey(name)) {    // deleted pool
                pools.remove(name);
            }
        }
    }

    public static void processUnscheduledEvents(List<UnscheduledEvent> unscheduledEvents) {
        if (singleton==null) return;    // no connection pool is register
        for (UnscheduledEvent one : unscheduledEvents) {
            if (one.getReference().startsWith("pool:")) {
                String poolname = one.getReference().substring(5);
                AdapterConnectionPool pool = singleton.pools.get(poolname);
                if (pool==null) {
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    logger.severe(LoggerUtil.getStandardLogger().getSentryMark() + "Cannot process unscheduled event "
                            + one.getName() + " because the connection pool does not exist");
                } else pool.addWaitingRequest(one);
            }
        }
        for (String poolname : singleton.pools.keySet()) {
            AdapterConnectionPool pool = singleton.pools.get(poolname);
            pool.processWaitingRequests();
        }
    }

}