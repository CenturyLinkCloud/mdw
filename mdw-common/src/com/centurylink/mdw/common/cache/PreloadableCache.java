/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache;

import java.util.Map;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.provider.CacheService;

/**
 * This interface defines a cache that can be preloaded
 * optionally
 *
 */
public interface PreloadableCache extends CacheEnabled, CacheService {

    public void initialize(Map<String,String> params);
    
	void loadCache() throws CachingException;
	
}
