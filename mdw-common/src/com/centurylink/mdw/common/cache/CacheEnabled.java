/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache;

public interface CacheEnabled {
	
	void refreshCache() throws Exception;
	
	void clearCache();

}
