/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.service.data.event;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.util.file.Packages;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandlerCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static HashMap<String,List<ExternalEvent>> myContentCache = new HashMap<>();
    private static HashMap<String,List<ExternalEvent>> myPathCache = new HashMap<>();

    public EventHandlerCache(){
        super();
    }

    public void initialize(Map<String,String> params) {}

    /**
     * Method that clears the cache
     */
    public void clearCache(){
        myPathCache.clear();
        myContentCache.clear();
    }

    /**
     * returns the cached content-based external event
     * @param bucket
     * @return Cached Item
     */
    @Deprecated
    public static List<ExternalEvent> getExternalEvents(String bucket){
      return getContentExternalEvents(bucket);
    }

    /**
     * returns the cached content-based external event
     * @param bucket
     * @return Cached Item
     */
    public static List<ExternalEvent> getContentExternalEvents(String bucket){
        return myContentCache.get(bucket);
    }

    /**
     * returns the cached path-based external event
     * @param bucket
     * @return Cached Item
     */
    public static List<ExternalEvent> getPathExternalEvents(String bucket){
        if (myPathCache.get(bucket) != null)
            return myPathCache.get(bucket);
        else if (bucket.indexOf('/') > 0) {  // We could have a sub-path
            return getPathExternalEvents(bucket.substring(0, bucket.lastIndexOf('/')));
        }
        return null;
    }

    public static ExternalEvent fallbackHandler = new ExternalEvent();
    static {
        fallbackHandler.setEventName("FallbackHandler");
        fallbackHandler.setEventHandler("com.centurylink.mdw.listener.FallbackEventHandler");
        fallbackHandler.setPackageName(Packages.MDW_BASE);
        fallbackHandler.setId(new Long(0));
    }

    public static ExternalEvent serviceHandler = new ExternalEvent();
    static {
        serviceHandler.setEventName("ServiceHandler");
        serviceHandler.setEventHandler("com.centurylink.mdw.service.handler.ServiceRequestHandler");
        serviceHandler.setPackageName(Packages.MDW_BASE);
        serviceHandler.setId(new Long(0));
    }

    public static ExternalEvent regressionTestHandler = new ExternalEvent();
    static {
        regressionTestHandler.setEventName("RegressionTestHandler");
        regressionTestHandler.setEventHandler("com.centurylink.mdw.listener.RegressionTestEventHandler");
        regressionTestHandler.setPackageName(Packages.MDW_BASE);
        regressionTestHandler.setId(new Long(0));
        regressionTestHandler.setMessagePattern("ActionRequest/Action[@Name=RegressionTest]");
    }

    public synchronized void refreshCache() throws CachingException {
        loadCache();
    }

    public void loadCache() throws CachingException {
        load();
    }

    private synchronized void load() throws CachingException {
        HashMap<String,List<ExternalEvent>> myContentCacheTemp = new HashMap<>();
        HashMap<String,List<ExternalEvent>> myPathCacheTemp = new HashMap<>();
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            List<ExternalEvent> all = loader.loadExternalEvents();
            for (ExternalEvent e : all) {
                if (e.getEventName().equals("DefaultEventHandler")) {
                    fallbackHandler = e;
                    continue;
                }
                try{
                    if (e.isContentRouting()) {
                        XmlPath xpath = e.getXpath();
                        List<ExternalEvent> bucket = myContentCacheTemp.get(xpath.getHashBucket());
                        if (bucket == null) {
                            bucket = new ArrayList<>();
                            myContentCacheTemp.put(xpath.getHashBucket(), bucket);
                        }
                        bucket.add(e);
                    }
                    else {
                        if (e.getMessagePattern().startsWith("/"))
                            e.setMessagePattern(e.getMessagePattern().substring(1));
                     //   if (e.getMessagePattern().endsWith("/"))
                     //       e.setMessagePattern(e.getMessagePattern().substring(0, e.getMessagePattern().length()-1));
                        List<ExternalEvent> bucket = myPathCacheTemp.get(e.getMessagePattern());
                        if (bucket == null) {
                            bucket = new ArrayList<>();
                            myPathCacheTemp.put(e.getMessagePattern(), bucket);
                        }
                        bucket.add(e);
                    }
                } catch (Exception ex) {
                    logger.severeException("Cannot parse event pattern '" + e.getEventName() + "'", ex);
                }
            }
            myContentCache = myContentCacheTemp;
            myPathCache = myPathCacheTemp;
        } catch(Exception ex){
            throw new CachingException(ex.getMessage(), ex);
        }
    }


}
