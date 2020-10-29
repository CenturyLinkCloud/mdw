package com.centurylink.mdw.cache;

import com.centurylink.mdw.common.service.RegisteredService;

public interface CacheService extends RegisteredService {

    void refreshCache() throws Exception;

    void clearCache();

}
