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

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.json.JSONObject;
import java.io.File;

/**
 * Transpiles JSX.
 */
public class WebpackJsx {
    public JSONObject webpack(AssetInfo jsxAsset, File output) throws ServiceException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        AssetInfo parser = assetServices.getAsset("com.centurylink.mdw.node/parser.js");
        AssetInfo runner = assetServices.getAsset("com.centurylink.mdw.node/webpackRunner.js");
    
        NodeJS nodeJS = NodeJS.createNodeJS();

        System.out.println("NODE JS: " + nodeJS.getNodeVersion());

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
        V8Object jsxAssetObj = new V8Object(nodeJS.getRuntime());
        jsxAssetObj.add("file", jsxAsset.getFile().getAbsolutePath());
        jsxAssetObj.add("root", assetServices.getAssetRoot().getAbsolutePath());
        jsxAssetObj.add("output", output.getAbsolutePath());
        jsxAssetObj.add("debug", true); // TODO
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return jsxAssetObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getJsxAsset");

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
        jsxAssetObj.release();
        nodeJS.release();
    
        if (!webpackResult.status.equals("OK"))
            throw new ServiceException(jsxAsset + " -> " + webpackResult);
    
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
