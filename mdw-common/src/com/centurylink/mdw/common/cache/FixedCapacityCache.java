/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache;

public interface FixedCapacityCache {

    /**
     * For fixed size caches, set the size of the cache
     * @param size
     */
    public void setCapacity(int size);
    
}
