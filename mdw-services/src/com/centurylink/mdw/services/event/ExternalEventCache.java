/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.event;

import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.xml.XmlPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that caches all the tasks
 */
public class ExternalEventCache implements PreloadableCache{

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static HashMap<String,List<ExternalEventVO>> myCache =
    	new HashMap<String,List<ExternalEventVO>>();
    private static ExternalEventVO defaultEventHandler = null;

    public ExternalEventCache(){
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
    public static List<ExternalEventVO> getExternalEvents(String bucket){
      return myCache.get(bucket);
    }

    public static synchronized ExternalEventVO getDefaultEventHandler(){
        if (defaultEventHandler==null) {
        	defaultEventHandler = new ExternalEventVO();
        	defaultEventHandler.setEventName("DefaultEventHandler");
        	defaultEventHandler.setEventHandler("com.centurylink.mdw.listener.DefaultEventHandler");
        	defaultEventHandler.setPackageName("com.centurylink.mdw.base");
        	defaultEventHandler.setId(new Long(0));
        }
        return defaultEventHandler;
    }

    /**
     * Method that re loads the Cache(s)
     * @throws CachingException
     *
     */
    public synchronized void refreshCache() throws CachingException {
    //        clearCache();
            loadCache();
    }


    /**
     * Method that loads the Cache(s)
     * @throws CachingException
     *
     */
    public void loadCache() throws CachingException {
        load();
    }

    /**
     * Method that loads the Cache(s)
     * @throws CachingException
     *
     */
    private synchronized void load() throws CachingException {
        HashMap<String,List<ExternalEventVO>> myCacheTemp = new HashMap<String,List<ExternalEventVO>>();
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
    		List<ExternalEventVO> all = loader.loadExternalEvents();
    		for (ExternalEventVO e : all) {
    			if (e.getEventName().equals("DefaultEventHandler")) {
    				defaultEventHandler = e;
    				continue;
    			}
    			try{
    				XmlPath xpath = e.getXpath();
    				List<ExternalEventVO> bucket = myCacheTemp.get(xpath.getHashBucket());
    				if (bucket==null) {
    					bucket = new ArrayList<ExternalEventVO>();
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
