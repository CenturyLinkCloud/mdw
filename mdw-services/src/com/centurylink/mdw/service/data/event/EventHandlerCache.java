/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.data.event;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandlerCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static HashMap<String,List<ExternalEvent>> myCache =
        new HashMap<String,List<ExternalEvent>>();

    public EventHandlerCache(){
        super();
    }

    public void initialize(Map<String,String> params) {}

    /**
     * Method that clears the cache
     * @return ObjectCache
     */
    public void clearCache(){
        myCache.clear();
    }

    /**
     * returns the cached external event
     * @param pEventName
     * @return Cached Item
     */
    public static List<ExternalEvent> getExternalEvents(String bucket){
      return myCache.get(bucket);
    }

    public static ExternalEvent fallbackHandler = new ExternalEvent();
    static {
        fallbackHandler.setEventName("FallbackHandler");
        fallbackHandler.setEventHandler("com.centurylink.mdw.listener.FallbackEventHandler");
        fallbackHandler.setPackageName("com.centurylink.mdw.base");
        fallbackHandler.setId(new Long(0));
    }

    public static ExternalEvent serviceHandler = new ExternalEvent();
    static {
        serviceHandler.setEventName("ServiceHandler");
        serviceHandler.setEventHandler("com.centurylink.mdw.service.handler.ServiceRequestHandler");
        serviceHandler.setPackageName("com.centurylink.mdw.base");
        serviceHandler.setId(new Long(0));
    }

    public synchronized void refreshCache() throws CachingException {
        loadCache();
    }

    public void loadCache() throws CachingException {
        load();
    }

    private synchronized void load() throws CachingException {
        HashMap<String,List<ExternalEvent>> myCacheTemp = new HashMap<String,List<ExternalEvent>>();
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            List<ExternalEvent> all = loader.loadExternalEvents();
            for (ExternalEvent e : all) {
                if (e.getEventName().equals("DefaultEventHandler")) {
                    fallbackHandler = e;
                    continue;
                }
                try{
                    XmlPath xpath = e.getXpath();
                    List<ExternalEvent> bucket = myCacheTemp.get(xpath.getHashBucket());
                    if (bucket==null) {
                        bucket = new ArrayList<ExternalEvent>();
                        myCacheTemp.put(xpath.getHashBucket(), bucket);
                    }
                    bucket.add(e);
                } catch (Exception ex) {
                    logger.severeException("Cannot parse event pattern '" + e.getEventName() + "'", ex);
                }
            }
            myCache = myCacheTemp;
        } catch(Exception ex){
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }


}
