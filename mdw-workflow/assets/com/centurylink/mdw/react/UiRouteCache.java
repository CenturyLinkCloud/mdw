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
package com.centurylink.mdw.react;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.asset.Asset;

/**
 * Caches the UI route mapping from JSX asset to associated location hash route
 * so that MDW's AngularJS base webapp can properly handle JSX asset routing.
 */
@RegisteredService(value=CacheService.class)
public class UiRouteCache implements CacheService {

    // Null if not loaded; empty if no routes.json asset.
    private static JSONArray routesJson;

    @Override
    public void clearCache() {
        routesJson = null;
    }

    @Override
    public void refreshCache() throws Exception {
        loadCache();
    }

    public static synchronized void loadCache() {
        String hubOverridePackage = ApplicationContext.getHubOverridePackage();
        Asset routesAsset = AssetCache.getAsset(hubOverridePackage + ".js/routes.json");
        if (routesAsset == null) {
            routesJson = new JSONArray();
        }
        else {
            routesJson = new JSONArray(routesAsset.getStringContent());
        }
    }

    public static String getPath(String jsxAsset, boolean parameterized) {
        if (routesJson == null)
            loadCache();
        for (int i = 0; i < routesJson.length(); i++) {
            JSONObject route = routesJson.getJSONObject(i);
            String asset = route.getString("asset");
            if (asset.endsWith(".jsx")) {
                if (asset.equals(jsxAsset)) {
                    String path = route.getString("path");
                    boolean hasParam = path.indexOf("/:") >= 0;
                    if ((parameterized && hasParam) || (!parameterized && !hasParam))
                        return path;
                }
            }
        }
        return null;
    }

    public static JSONArray getRoutes() {
        if (routesJson == null)
            loadCache();
        return routesJson;
    }
}
