package com.centurylink.mdw.staging;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.ServiceLocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisteredService(value=CacheService.class)
public class StagedAssetCache implements CacheService {

    private static volatile Map<String,Map<String,List<AssetInfo>>> stagedAssets = new HashMap<>();

    /**
     * Returns map of package name to asset list.
     */
    public static Map<String,List<AssetInfo>> getStagedAssets(String cuid) {
        Map<String,List<AssetInfo>> staged;
        Map<String,Map<String,List<AssetInfo>>> stagedAssetMap = stagedAssets;
        if (stagedAssetMap.containsKey(cuid)){
            staged = stagedAssetMap.get(cuid);
        } else {
            synchronized(StagedAssetCache.class) {
                stagedAssetMap = stagedAssets;
                if (stagedAssetMap.containsKey(cuid)) {
                    staged = stagedAssetMap.get(cuid);
                } else {
                    staged = loadStagedAssets(cuid);
                    // null values are stored to avoid repeated processing
                    stagedAssets.put(cuid, staged);
                }
            }
        }
        return staged;
    }

    private static Map<String,List<AssetInfo>> loadStagedAssets(String cuid) {
        try {
            return ServiceLocator.getStagingServices().getStagedAssets(cuid);
        } catch (ServiceException ex) {
            throw new CachingException("Error loading staged assets for user: " + cuid, ex);
        }
    }


    @Override
    public void refreshCache() {
        // dynamically loaded
        clearCache();
    }

    @Override
    public void clearCache() {
        synchronized (StagedAssetCache.class) {
            stagedAssets.clear();
        }
    }
}
