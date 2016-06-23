/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;


/**
 * Cache for variable translators
 */
public class VariableTypeCache implements PreloadableCache {

    private static HashMap<String,VariableTypeVO> myCache
    	= new HashMap<String,VariableTypeVO>();
    private static HashMap<Long,VariableTypeVO> yourCache
    	= new HashMap<Long,VariableTypeVO>();

    public void initialize(Map<String,String> params) {}

    public void loadCache() throws CachingException {
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            List<VariableTypeVO> types = loader.getVariableTypes();
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

    public static VariableTypeVO getVariableTypeVO(String pVarTypeName){
        return myCache.get(pVarTypeName);
    }

    public static String getTypeName(Long typeId) {
    	VariableTypeVO vo = yourCache.get(typeId);
    	return vo==null?null:vo.getVariableType();
    }

    public static Long getTypeId(String typeName) {
    	VariableTypeVO vo = myCache.get(typeName);
    	return vo==null?null:vo.getVariableTypeId();
    }

    public static void loadCache(List<VariableTypeVO> types) throws DataAccessException {
        reloadCache(types);
    }

    public static synchronized void reloadCache(List<VariableTypeVO> types) throws DataAccessException {
        HashMap<String,VariableTypeVO> myCacheTemp = new HashMap<String,VariableTypeVO>();
        HashMap<Long,VariableTypeVO> yourCacheTemp = new HashMap<Long,VariableTypeVO>();
        try {
            for (VariableTypeVO type : types) {
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

    public static Collection<VariableTypeVO> getVariableTypes() {
        return myCache.values();
    }
}
