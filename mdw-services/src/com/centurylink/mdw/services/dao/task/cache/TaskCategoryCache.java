/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.task.cache;

// CUSTOM IMPORTS -------------------------------------------------------

// JAVA IMPORTS -------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;

public class TaskCategoryCache implements PreloadableCache {

    private static HashMap<Long,TaskCategory> myCache = new HashMap<Long,TaskCategory>();
    private static HashMap<String,Long> reverseCache = new HashMap<String,Long>();

    public void initialize(Map<String,String> params) {}

    public void clearCache(){
        myCache.clear();
        reverseCache.clear();
    }

    public static String getTaskCategoryCode(Long pTaskCatId){
        if(!myCache.containsKey(pTaskCatId)){
            return null;
        }
        return myCache.get(pTaskCatId).getCode();
    }

     /**
     * returns the cached task category Description
     * @param TaskCatId
     * @return TaskCatDesc
     */
    public static String getTaskCategoryDescription(Long pTaskCatId){
        if(!myCache.containsKey(pTaskCatId)) {
            return null;
        }
        return myCache.get(pTaskCatId).getDescription();
    }

    public static Long getTaskCategoryId(String category) {
        if (category == null)
            return null;
        return reverseCache.get(category);
    }

    public void refreshCache() throws CachingException {
        loadCache();
    }

    public void loadCache() throws CachingException {
        load();
    }

    private synchronized void load() throws CachingException {
        HashMap<Long,TaskCategory> myCacheTemp = new HashMap<Long,TaskCategory>();
        HashMap<String,Long> reverseCacheTemp = new HashMap<String,Long>();
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            for (TaskCategory taskCat : taskMgr.getTaskCategories()) {
                myCacheTemp.put(taskCat.getId(), taskCat);
                reverseCacheTemp.put(taskCat.getCode(), taskCat.getId());
            }
            myCache = myCacheTemp;
            reverseCache = reverseCacheTemp;
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }
}
