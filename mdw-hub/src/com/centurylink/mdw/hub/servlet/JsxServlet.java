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
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * TODO: JSX Asset List Caching.
 */
@WebServlet(urlPatterns={"/jsx.js", "*.jsx"})
public class JsxServlet extends HttpServlet {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String NODE_PACKAGE = "com.centurylink.mdw.node";
    private static final String WEBPACK_CACHE_CLASS = NODE_PACKAGE + ".WebpackCache";
    private AssetServices assetServices;
    private CacheService webpackCacheInstance;
    private Method compiledAssetGetter;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.assetServices = ServiceLocator.getAssetServices();

        try {
            Class<?> webpackCacheClass = CompiledJavaCache.getResourceClass(WEBPACK_CACHE_CLASS,
                    getClass().getClassLoader(), PackageCache.getPackage(NODE_PACKAGE));
            webpackCacheInstance = CacheRegistration.getInstance().getCache(WEBPACK_CACHE_CLASS);
            compiledAssetGetter = webpackCacheClass.getMethod("getCompiled", AssetInfo.class);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/jsx.js")) {
                response.getWriter().println(getJsxJs());
            }
            else if (request.getServletPath().endsWith(".jsx")) {
                String path = request.getServletPath();
                String p = path.substring(0);
                int lastSlash = p.lastIndexOf('/');
                if (lastSlash == -1)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Bad path: " + path);

                String pkgName = p.substring(0, lastSlash).replace('/', '.');
                String assetPath = pkgName + p.substring(lastSlash);
                AssetInfo jsxAsset = assetServices.getAsset(assetPath);

                File file = (File) compiledAssetGetter.invoke(webpackCacheInstance, jsxAsset);

                if (shouldCache(file, request.getHeader("If-None-Match"))) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
                else {
                    response.setHeader("ETag", String.valueOf(file.lastModified()));
                    OutputStream out = response.getOutputStream();
                    try (InputStream in = new FileInputStream(file)) {
                        int read = 0;
                        byte[] bytes = new byte[1024 * 16];
                        while((read = in.read(bytes)) != -1)
                            out.write(bytes, 0, read);
                    }
                }
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            if (ex.getCode() > 0) {
                response.setStatus(ex.getCode());
                response.getWriter().println(ex.getMessage());
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    private boolean shouldCache(File file, String ifNoneMatchHeader) {
        if (ifNoneMatchHeader == null)
            return false; // no cache
        try {
            long clientTime = Long.parseLong(ifNoneMatchHeader);
            return clientTime >= file.lastModified();
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    private String getJsxJs() throws ServiceException {
        // add a route for each jsx asset
        Map<String,List<AssetInfo>> jsxAssets = assetServices.getAssetsOfType("jsx");
        StringBuilder sb = new StringBuilder();
        if (!jsxAssets.isEmpty()) {
            sb.append("'use strict';\n\n");
            sb.append("var app = angular.module('adminApp');\n\n");
            sb.append("app.config(['$routeProvider', function($routeProvider) {\n");
            for (String pkg : jsxAssets.keySet()) {
                String pkgPath = pkg.replace('.', '/');
                for (AssetInfo asset : jsxAssets.get(pkg)) {
                    String assetPath = pkgPath + '/' + asset.getRootName();
                    sb.append("  $routeProvider.when('/" + assetPath + "', {\n");
                    sb.append("    templateUrl: '" + "demo/bugs.html" + "',\n"); // TODO
                    sb.append("    controller: '" + "BugsController" + "'\n"); // TODO
                    sb.append("  });\n");
                }
            }
            sb.append("}]);\n");
        }
        return sb.toString();
    }

}
