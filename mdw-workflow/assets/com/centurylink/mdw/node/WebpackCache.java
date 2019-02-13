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
package com.centurylink.mdw.node;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.file.MdwIgnore;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.file.ZipHelper.Exist;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Object;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In non-dev environments, precompiles all Index.jsx assets.
 */
@RegisteredService(value=CacheService.class)
public class WebpackCache implements PreloadableCache {

    private static final String NODE_PACKAGE = "com.centurylink.mdw.node";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<AssetInfo,File> webpackAssets = new HashMap<>();
    private static Map<AssetInfo,File> watchedAssets = new HashMap<>();  // for devMode

    @Override
    public void initialize(Map<String,String> params) {
        File assetRoot = ApplicationContext.getAssetRoot();
        File nodeDir = new File(assetRoot + "/" + NODE_PACKAGE.replace('.', '/'));
        if (!nodeDir.exists())
            throw new CachingException("Node dir not found: " + nodeDir);

        try {
            unzipNodeModules(nodeDir);

            try {
                // also unzip any custom node_modules
                Map<String, List<AssetInfo>> zipAssets = ServiceLocator.getAssetServices().getAssetsOfType("zip");
                for (String pkg : zipAssets.keySet()) {
                    for (AssetInfo zipAsset : zipAssets.get(pkg)) {
                        if (zipAsset.getName().equals("node_modules.zip") && !pkg.equals(NODE_PACKAGE)) {
                            File pkgDir = new File(assetRoot + "/" + pkg.replace('.', '/'));
                            unzipNodeModules(pkgDir);
                        }
                    }
                }
            }
            catch (Exception ex) {
                // but don't let any exceptions stop main processing
                logger.severeException(ex.getMessage(), ex);
            }

            AssetServices assetServices = ServiceLocator.getAssetServices();
            List<AssetInfo> precompiledJsx = new ArrayList<>();
            AssetInfo runJsx = assetServices.getAsset("com.centurylink.mdw.react/Run.jsx");
            if (runJsx != null) {
                // conditionally compile in dev for faster startup
                if (!isDevMode() || !getOutput(runJsx).exists()) {
                    precompiledJsx.add(runJsx);
                }
            }
            if (!isDevMode()) {
                // in non-dev everything is precompiled
                AssetInfo taskMain = assetServices.getAsset("com.centurylink.mdw.task/Main.jsx");
                if (taskMain != null)
                    precompiledJsx.add(taskMain);

                // add all Index.jsx assets
                for (List<AssetInfo> assets : assetServices.findAssets(file -> file.getName().equals("Index.jsx")).values()) {
                    precompiledJsx.addAll(assets);
                }
            }
            if (precompiledJsx.size() > 0)
                logger.info("Precompiling JSX assets:");
            for (AssetInfo jsxAsset : precompiledJsx) {
                getCompiled(jsxAsset);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    private void unzipNodeModules(File pkgDir) throws IOException {
        File modulesZip = new File(pkgDir + "/node_modules.zip");
        long before = System.currentTimeMillis();
        logger.info("Unzipping " + pkgDir + "/node_modules...");
        ZipHelper.unzip(modulesZip, pkgDir, null, null, Exist.Ignore);
        if (logger.isDebugEnabled())
            logger.debug("  - node_modules unzipped in " + (System.currentTimeMillis() - before) + " ms");
    }

    @Override
    public void loadCache() throws CachingException {
    }

    @Override
    public void refreshCache() {
        clearCache();  // lazy loading
    }

    @Override
    public void clearCache() {
        webpackAssets.clear();
        watchedAssets.clear();
    }

    public File getCompiled(AssetInfo asset) throws IOException, ServiceException {
        return getCompiled(asset, getStarter(asset));
    }


    /**
     * Starter file will be compiled, but asset used to compute output path.
     */
    public File getCompiled(AssetInfo asset, File starter) throws IOException, ServiceException {
        File file;
        synchronized(WebpackCache.class) {
            file = webpackAssets.get(asset);
            if (file == null || !file.exists() || file.lastModified() < asset.getFile().lastModified()
                    || (starter != null && file.lastModified() < starter.lastModified())) {
                file = getOutput(asset);
                compile(asset, starter, file);
                return file;
            }
        }
        return file;
    }

    private File getStarter(AssetInfo asset) throws IOException {
        File starter = null;

        if ("jsx".equals(asset.getExtension())) {
            String filePath = asset.getFile().getAbsolutePath();
            String pkgPath = filePath
                    .substring(ApplicationContext.getAssetRoot().getAbsolutePath().length() + 1,
                            filePath.length() - asset.getFile().getName().length() - 1).replace('/', '.').replace('\\', '.');
            // generate the specialized starter script for this jsx
            starter = new File(ApplicationContext.getTempDirectory() + "/mdw.start/" + pkgPath + "/" + asset.getRootName() + ".js");
            if (!starter.exists()) {
                if (!starter.getParentFile().isDirectory() && !starter.getParentFile().mkdirs())
                    throw new IOException("Cannot create starter directory: " + starter.getParentFile().getAbsolutePath());
                Files.write(Paths.get(starter.getPath()), getStarterContent(pkgPath, asset).getBytes());
            }
        }

        return starter;
    }

    private String getStarterContent(String pkgPath, AssetInfo jsxAsset) {
        String assetRoot = ApplicationContext.getAssetRoot().getAbsolutePath().replace('\\', '/');
        StringBuilder sb = new StringBuilder();
        String nodeModules = assetRoot + "/com/centurylink/mdw/node/node_modules";
        sb.append("import React from '").append(nodeModules).append("/react';\n");
        sb.append("import ReactDOM from '").append(nodeModules).append("/react-dom';\n");
        sb.append("import ").append(jsxAsset.getRootName()).append(" from '").append(jsxAsset.getFile().getAbsolutePath().replace('\\', '/')).append("';\n\n");

        if (logger.isDebugEnabled())
            sb.append("console.log('Starting: ").append(pkgPath).append("/").append(jsxAsset.getName()).append("');\n\n");

        sb.append("ReactDOM.render(\n");
        sb.append("  React.createElement(").append(jsxAsset.getRootName()).append(", {}, null),\n");
        sb.append("  document.querySelector('[mdw-jsx=\"").append(pkgPath).append("/").append(jsxAsset.getName()).append("\"]')\n");
        sb.append(");");
        return sb.toString();
    }

    private File getOutput(AssetInfo asset) {
        return new File(ApplicationContext.getTempDirectory() + "/" + asset.getExtension()
            + asset.getFile().getAbsolutePath().substring(
                ApplicationContext.getAssetRoot().getAbsolutePath().length()) + ".out");
    }

    /**
     * Returns null except in dev mode.
     */
    private void compile(AssetInfo asset, File source, File target) throws ServiceException {
        File watched = watchedAssets.get(asset);
        if (watched == null) {
            if (isDevMode()) {
                new Thread(() -> {
                    try {
                        // avoid recursive compiles
                        if (isDevMode())
                            watchedAssets.put(asset, target);
                        doCompile(asset, source, target);
                    }
                    catch (Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }).start();
            }
            else {
                doCompile(asset, source, target);
            }
        }
    }

    private void doCompile(AssetInfo asset, File source, File target) throws ServiceException {
        long before = System.currentTimeMillis();
        Result webpackResult = compile(source, target);
        JSONObject resultsJson = new JSONObject(webpackResult.message);
        if (logger.isDebugEnabled()) {
            logger.debug("*** Webpack stats:\n" + resultsJson.toString(2));
            long time = System.currentTimeMillis() - before;
            logger.debug("*** Webpack build time: " + time + " ms");
        }
        if (resultsJson.has("errors")) {
            JSONArray errors = resultsJson.getJSONArray("errors");
            if (errors.length() > 0) {
                throw new ServiceException(
                    new Status(Status.OK, "console.error(JSON.stringify({webpackErrors: " + errors + "}, null, 2));"));
            }
        }
        webpackAssets.put(asset, target);
    }

    /**
     * This can take a while.
     */
    private Result compile(File source, File target) throws ServiceException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        AssetInfo parser = assetServices.getAsset(NODE_PACKAGE + "/parser.js");
        if (parser == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + parser);
        AssetInfo runner = assetServices.getAsset(NODE_PACKAGE + "/webpackRunner.js");
        if (runner == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + runner);

        NodeJS nodeJS = NodeJS.createNodeJS();

        logger.info("Compiling " + source + " using NODE JS: " + nodeJS.getNodeVersion());
        V8Object fileObj = new V8Object(nodeJS.getRuntime()).add("file", runner.getFile().getAbsolutePath());
        JavaCallback callback = (receiver, parameters) -> fileObj;
        nodeJS.getRuntime().registerJavaMethod(callback, "getRunner");

        final Result parseResult = new Result();
        callback = (receiver, parameters) -> {
            V8Object resultObj = parameters.getObject(0);
            parseResult.status = resultObj.getString("status");
            parseResult.message = resultObj.getString("message");
            resultObj.release();
            return null;
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setParseResult");

        nodeJS.exec(parser.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        fileObj.release();
        nodeJS.release();

        if (!parseResult.status.equals("OK"))
            throw new ServiceException(runner + " -> " + parseResult);

        // run
        nodeJS = NodeJS.createNodeJS();
        V8Object inputObj = new V8Object(nodeJS.getRuntime());
        inputObj.add("source", source.getAbsolutePath());
        inputObj.add("root", assetServices.getAssetRoot().getAbsolutePath());
        inputObj.add("output", target.getAbsolutePath());
        inputObj.add("debug", logger.isDebugEnabled());
        inputObj.add("devMode", isDevMode());
        callback = (receiver, parameters) -> inputObj;
        nodeJS.getRuntime().registerJavaMethod(callback, "getInput");

        final Result webpackResult = new Result();
        callback = (receiver, parameters) -> {
            V8Object resultObj = parameters.getObject(0);
            webpackResult.status = resultObj.getString("status");
            webpackResult.message = resultObj.getString("message");
            resultObj.release();
            return null;
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setWebpackResult");

        nodeJS.exec(runner.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }
        inputObj.release();
        nodeJS.release();

        if (!webpackResult.status.equals("OK"))
            throw new ServiceException(source + " -> " + webpackResult);

        return webpackResult;
    }

    private boolean isDevMode() {
        String devMode = System.getProperty("mdw.webpack.dev.mode");
        if (devMode == null)
            return ApplicationContext.isDevelopment();
        else
            return devMode.equals("true");
    }

    private class Result {
        String status;
        String message;
        public String toString() {
            return status + ": " + message;
        }
    }
}
