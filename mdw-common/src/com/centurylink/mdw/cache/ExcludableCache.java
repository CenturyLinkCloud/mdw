/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache;

/**
 * Indicates that an asset cache can be excluded from refresh
 * based on its format.
 */
public interface ExcludableCache {
    public String getFormat();
}
