/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        for (Asset sqlAsset : AssetCache.getAssets("sql")) {
            sqlSourceAssets.add(sqlAsset.getPath());
        }
        return sqlSourceAssets;
    }

    /**
     * Implement this to perform any arbitrary one-time db initialization.
     */
    void initialize() throws SQLException;

}
