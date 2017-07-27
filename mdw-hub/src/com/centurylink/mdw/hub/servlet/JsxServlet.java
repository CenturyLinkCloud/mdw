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
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * TODO: JSX Asset List Caching.
 */
@WebServlet(urlPatterns={"/jsx.js", "*.jsx"})
public class JsxServlet extends HttpServlet {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private AssetServices assetServices;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.assetServices = ServiceLocator.getAssetServices();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/jsx.js")) {
                response.getWriter().println(getJsxJs());
            }
            else if (request.getServletPath().endsWith(".jsx")) {
                response.getWriter().println(getTranspiledJsx(request.getServletPath()));

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

    private String getTranspiledJsx(String path) throws ServiceException {
        String p = path.substring(0);
        int lastSlash = p.lastIndexOf('/');
        if (lastSlash == -1)
            throw new ServiceException(ServiceException.NOT_FOUND, "Bad path: " + path);

        // TODO: cache file if no need to compile
        File outputFile = new File(ApplicationContext.getTempDirectory() + "/jsx" + path);
        JSONObject json;
        try {
            String pkgName = p.substring(0, lastSlash).replace('/', '.');
            String assetPath = pkgName + p.substring(lastSlash);
            AssetInfo jsxAsset = assetServices.getAsset(assetPath);
            String runnerClass = "com.centurylink.mdw.node.WebpackJsx";
            Package pkg = PackageCache.getPackage(pkgName);
            Class<?> nodeRunnerClass = CompiledJavaCache.getResourceClass(runnerClass, getClass().getClassLoader(), pkg);
            Object runner = nodeRunnerClass.newInstance();
            Method webpackMethod = nodeRunnerClass.getMethod("webpack", AssetInfo.class, File.class);
            json = (JSONObject)webpackMethod.invoke(runner, jsxAsset, outputFile);
            if (logger.isDebugEnabled()) {
                logger.debug("*** Webpack stats:\n" + json.toString(2));
            }
            if (json.has("errors")) {
                JSONArray errors = json.getJSONArray("errors");
                if (errors.length() > 0)
                    throw new ServiceException(new Status(Status.OK, "console.error(JSON.stringify({webpackErrors: " + errors + "}, null, 2));"));
            }
            return new String(Files.readAllBytes(Paths.get(outputFile.getPath())));
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
