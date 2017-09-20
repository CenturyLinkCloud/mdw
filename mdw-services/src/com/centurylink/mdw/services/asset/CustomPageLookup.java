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
package com.centurylink.mdw.services.asset;

import java.lang.reflect.Method;

import org.json.JSONArray;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.services.cache.CacheRegistration;

/**
 * Translate custom page assets to instance urls.
 */
public class CustomPageLookup {

    private static final String REACT_PACKAGE = "com.centurylink.mdw.react";
    private static final String UI_ROUTE_CACHE_CLASS = REACT_PACKAGE + ".UiRouteCache";
    private static CacheService uiRouteCacheInstance;
    private static Method routePathGetter;
    private static Method routesGetter;

    private AssetVersionSpec customPage;
    private Long instanceId;

    public CustomPageLookup(AssetVersionSpec customPage, Long instanceId) {
        this.customPage = customPage;
        this.instanceId = instanceId;
    }

    public String getUrl() throws ReflectiveOperationException {
        if (uiRouteCacheInstance == null && !init()) {
            return null;
        }

        String path = (String)routePathGetter.invoke(uiRouteCacheInstance, customPage.getPackageName() + '/' + customPage.getName());
        if (path == null) {
            // TODO: establish a default convention based on JSX asset name without extension.
            path = '/' + customPage.getPackageName() + '/' + customPage.getName().substring(0, customPage.getName().length() - 4);
            if (instanceId != null)
                path = path + '/' + instanceId;
        }
        else {
            path = "/#" + path;
            if (path.endsWith("/:id") && instanceId != null) {
                path = path.substring(0, path.length() - 3) + instanceId;
            }
        }
        return ApplicationContext.getMdwHubUrl() + path;
    }

    public static JSONArray getUiRoutes() throws ReflectiveOperationException {
        if (uiRouteCacheInstance == null && !init()) {
            return null;
        }
        return (JSONArray)routesGetter.invoke(uiRouteCacheInstance);
    }

    private static boolean init() throws ReflectiveOperationException {
        uiRouteCacheInstance = CacheRegistration.getInstance().getCache(UI_ROUTE_CACHE_CLASS);
        if (uiRouteCacheInstance == null) {
            return false;
        }
        else {
            routePathGetter = uiRouteCacheInstance.getClass().getMethod("getPath", String.class);
            routesGetter = uiRouteCacheInstance.getClass().getMethod("getRoutes");
            return true;
        }
    }
}
