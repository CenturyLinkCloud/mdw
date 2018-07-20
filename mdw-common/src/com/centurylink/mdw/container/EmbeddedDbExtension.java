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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;

public interface EmbeddedDbExtension extends RegisteredService {

    /**
     * Returns a list of SQL assets to be sourced to create this custom
     * embedded db extension.  Default impl returns all sql assets in the
     * package containing this dynamic java class.
     */
    default List<String> getSqlSourceAssets() {
        List<String> sqlSourceAssets = null;
        Package pkg = PackageCache.getPackage(this.getClass().getPackage().getName());
        for (Asset asset : pkg.getAssets()) {
            if (sqlSourceAssets == null)
                sqlSourceAssets = new ArrayList<String>();
            if (".sql".equals(asset.getFileExtension()))
            sqlSourceAssets.add(pkg.getName() + "/" + asset.getName());
        }

        return sqlSourceAssets;
    }

    /**
     * Implement this to perform any arbitrary one-time db initialization.
     */
    void initialize() throws SQLException;

}
