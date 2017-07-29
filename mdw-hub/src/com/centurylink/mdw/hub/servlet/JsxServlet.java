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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
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
@WebServlet(urlPatterns={"*.jsx"})
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
            compiledAssetGetter = webpackCacheClass.getMethod("getCompiled", AssetInfo.class, File.class);
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
            if (request.getServletPath().endsWith(".jsx")) {
                String path = request.getServletPath();
                String p = path.substring(1);
                int lastSlash = p.lastIndexOf('/');
                if (lastSlash == -1)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Bad path: " + path);

                String pkgPath = p.substring(0, lastSlash);
                String pkgName = pkgPath.replace('/', '.');
                String assetPath = pkgName + p.substring(lastSlash);
                AssetInfo jsxAsset = assetServices.getAsset(assetPath);

                // generate the specialized starter script for this jsx
                File starter = new File(ApplicationContext.getTempDirectory() + "/mdw.start/" + pkgPath + "/" + jsxAsset.getRootName() + ".js");
                if (!starter.exists()) {
                    if (!starter.getParentFile().isDirectory() && !starter.getParentFile().mkdirs())
                        throw new IOException("Cannot create starter directory: " + starter.getParentFile().getAbsolutePath());
                    Files.write(Paths.get(starter.getPath()), getStarter(pkgPath, jsxAsset).getBytes());
                }

                try {

                    File file = (File) compiledAssetGetter.invoke(webpackCacheInstance, jsxAsset, starter);

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
                catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof ServiceException)
                        throw (ServiceException)ex.getCause();
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

    private String getStarter(String pkgPath, AssetInfo jsxAsset) {
        StringBuilder sb = new StringBuilder();
        String nodeModules = ApplicationContext.getAssetRoot().toString().replace('\\', '/') + "/com/centurylink/mdw/node/node_modules";
        sb.append("import React from '" + nodeModules + "/react';\n");
        sb.append("import ReactDOM from '" + nodeModules + "/react-dom';\n");
        sb.append("import " + jsxAsset.getRootName() + " from '" + jsxAsset.getFile().getAbsolutePath().replace('\\', '/') + "';\n\n");

        if (logger.isDebugEnabled())
            sb.append("console.log('Starting: " + pkgPath + "/" + jsxAsset.getName() + "');\n\n");

        sb.append("ReactDOM.render(\n");
        sb.append("  React.createElement(" + jsxAsset.getRootName() + ", {}, null),\n");
        sb.append("  document.querySelector('[mdw-jsx=\"" + pkgPath + "/" + jsxAsset.getName() + "\"]')\n");
        sb.append(");");
        return sb.toString();
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

}
