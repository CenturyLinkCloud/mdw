package com.centurylink.mdw.cache;

import java.util.Map;

/**
 * Defines a cache that can be preloaded.
 */
public interface PreloadableCache extends CacheService {

    default void initialize(Map<String,String> params) throws CachingException {}

    void loadCache() throws CachingException;

}
