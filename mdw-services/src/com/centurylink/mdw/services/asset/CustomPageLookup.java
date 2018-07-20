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
    private Long id;

    public CustomPageLookup(AssetVersionSpec customPage, Long id) {
        this.customPage = customPage;
        this.id = id;
    }

    public String getUrl() throws ReflectiveOperationException {
        if (uiRouteCacheInstance == null && !init()) {
            return null;
        }

        String path = (String) routePathGetter.invoke(uiRouteCacheInstance,
                customPage.getPackageName() + '/' + customPage.getName(), id != null);
        if (path == null) {
            // TODO: implement this default convention based on JSX asset path without extension.
            path = '/' + customPage.getPackageName() + '/'
                    + customPage.getName().substring(0, customPage.getName().length() - 4);
            if (id != null)
                path = path + '/' + id;
        }
        else {
            path = "/#" + path;
            if (path.endsWith("/:id") && id != null) {
                path = path.substring(0, path.length() - 3) + id;
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
            routePathGetter = uiRouteCacheInstance.getClass().getMethod("getPath", String.class,
                    boolean.class);
            routesGetter = uiRouteCacheInstance.getClass().getMethod("getRoutes");
            return true;
        }
    }
}
