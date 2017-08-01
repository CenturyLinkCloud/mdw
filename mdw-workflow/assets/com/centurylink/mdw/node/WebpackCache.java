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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

// TODO precompilation
@RegisteredService(CacheService.class)
public class WebpackCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<AssetInfo,File> webpackAssets = new HashMap<>();

    @Override
    public void refreshCache() throws Exception {
        clearCache();  // lazy loading
    }

    @Override
    public void clearCache() {
        webpackAssets.clear();
    }

    public File getCompiled(AssetInfo asset) throws IOException, ServiceException {
        return getCompiled(asset, null);
    }

    /**
     * Starter file will be compiled, but asset used to compute output path.
     */
    public File getCompiled(AssetInfo asset, File starter) throws IOException, ServiceException {
        File file = null;
        synchronized(WebpackCache.class) {
            file = webpackAssets.get(asset);
            if (file == null || !file.exists() || file.lastModified() < asset.getFile().lastModified()
                    || (starter != null && file.lastModified() < starter.lastModified())) {
                file = compile(asset, starter);
            }
        }
        return file;
    }

    private File compile(AssetInfo asset, File source) throws IOException, ServiceException {
        File outputFile = new File(
                ApplicationContext.getTempDirectory() + "/" + asset.getExtension()
                    + asset.getFile().getAbsolutePath().substring(
                            ApplicationContext.getAssetRoot().getAbsolutePath().length()) + ".out");

        long before = System.currentTimeMillis();
        JSONObject statsJson = compile(source, outputFile);
        if (logger.isDebugEnabled()) {
            logger.debug("*** Webpack stats:\n" + statsJson.toString(2));
            long time = System.currentTimeMillis() - before;
            logger.debug("*** Webpack build time: " + time + " ms");
        }
        if (statsJson.has("errors")) {
            JSONArray errors = statsJson.getJSONArray("errors");
            if (errors.length() > 0) {
                throw new ServiceException(
                    new Status(Status.OK, "console.error(JSON.stringify({webpackErrors: " + errors + "}, null, 2));"));
            }
        }
        webpackAssets.put(asset, outputFile);
        return outputFile;
    }

    /**
     * This can take a while.
     */
    private JSONObject compile(File source, File target) throws IOException, ServiceException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        AssetInfo parser = assetServices.getAsset("com.centurylink.mdw.node/parser.js");
        AssetInfo runner = assetServices.getAsset("com.centurylink.mdw.node/webpackRunner.js");

        NodeJS nodeJS = NodeJS.createNodeJS();

        logger.info("Compiling " + source + " using NODE JS: " + nodeJS.getNodeVersion());
        V8Object fileObj = new V8Object(nodeJS.getRuntime()).add("file", runner.getFile().getAbsolutePath());
        JavaCallback callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return fileObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getRunner");

        final Result parseResult = new Result();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj = parameters.getObject(0);
                parseResult.status = resultObj.getString("status");
                parseResult.content = resultObj.getString("message");
                resultObj.release();
                return null;
            }
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
        inputObj.add("debug", true); // TODO
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return inputObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getInput");

        final Result webpackResult = new Result();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj = parameters.getObject(0);
                webpackResult.status = resultObj.getString("status");
                webpackResult.content = resultObj.getString("content");
                resultObj.release();
                return null;
            }
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

        return new JSONObject(webpackResult.content);
    }

    private class Result {
        String status;
        String content;
        public String toString() {
            return status + ": " + content;
        }
    }

}
