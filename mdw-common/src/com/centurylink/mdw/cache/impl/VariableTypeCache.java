/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.variable.VariableType;

/**
 * Cache for variable translators
 */
public class VariableTypeCache implements PreloadableCache {

    private static HashMap<String,VariableType> myCache
    	= new HashMap<String,VariableType>();
    private static HashMap<Long,VariableType> yourCache
    	= new HashMap<Long,VariableType>();

    public void initialize(Map<String,String> params) {}

    public void loadCache() throws CachingException {
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            List<VariableType> types = loader.getVariableTypes();
            loadCache(types);
        } catch(Exception ex){
            throw new CachingException(-1, ex.getMessage(), ex);
        }

    }

	public void clearCache() {
		 myCache.clear();
         yourCache.clear();
	}

    public synchronized void refreshCache() throws CachingException {
     //   	 clearCache();
             loadCache();
    }

    public static VariableType getVariableTypeVO(String pVarTypeName){
        return myCache.get(pVarTypeName);
    }

    public static String getTypeName(Long typeId) {
    	VariableType vo = yourCache.get(typeId);
    	return vo==null?null:vo.getVariableType();
    }

    public static Long getTypeId(String typeName) {
    	VariableType vo = myCache.get(typeName);
    	return vo==null?null:vo.getVariableTypeId();
    }

    public static void loadCache(List<VariableType> types) throws DataAccessException {
        reloadCache(types);
    }

    public static synchronized void reloadCache(List<VariableType> types) throws DataAccessException {
        HashMap<String,VariableType> myCacheTemp = new HashMap<String,VariableType>();
        HashMap<Long,VariableType> yourCacheTemp = new HashMap<Long,VariableType>();
        try {
            for (VariableType type : types) {
                String transClass = Compatibility.getVariableTranslator(type.getTranslatorClass());
                type.setTranslatorClass(transClass);
                if (!myCacheTemp.containsKey(type.getVariableType())) // prefer previously-defined typedefs (non-compat)
                    myCacheTemp.put(type.getVariableType(), type);
                yourCacheTemp.put(type.getVariableTypeId(), type);
            }
            myCache = myCacheTemp;
            yourCache = yourCacheTemp;
        }
        catch (IOException ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
    }

    public static Collection<VariableType> getVariableTypes() {
        return myCache.values();
    }
}
