package com.centurylink.mdw.container;

import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.asset.Asset;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface EmbeddedDbExtension extends RegisteredService {

    /**
     * Returns a list of SQL assets to be sourced to create this custom
     * embedded db extension.  Default impl returns all sql assets in the
     * package containing this dynamic java class.
     */
    default List<String> getSqlSourceAssets() {
        List<String> sqlSourceAssets = new ArrayList<>();
        for (Asset pkgAsset : AssetCache.getPackageAssets(getClass().getPackage().getName())) {
            if ("sql".equals(pkgAsset.getExtension())) {
                sqlSourceAssets.add(pkgAsset.getPath());
            }
        }
        return sqlSourceAssets;
    }

    /**
     * Implement this to perform any arbitrary one-time db initialization.
     */
    void initialize() throws SQLException;

}
