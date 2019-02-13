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
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestCaseItem;
import com.centurylink.mdw.test.TestExecConfig;
import com.eclipsesource.v8.*;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs NodeJS test assets (through runner.js).
 */
public class TestRunner {
    static final String PARSER = "com.centurylink.mdw.node/parser.js";
    static final String RUNNER = "com.centurylink.mdw.node/testRunner.js";

    public void run(TestCase testCase) throws ServiceException {

        AssetServices assets = ServiceLocator.getAssetServices();

        NodeJS nodeJS = NodeJS.createNodeJS();

        System.out.println("NODE JS: " + nodeJS.getNodeVersion());

        AssetInfo runnerAsset = assets.getAsset(RUNNER);
        if (runnerAsset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + RUNNER);
        V8Object fileObj = new V8Object(nodeJS.getRuntime()).add("file", runnerAsset.getFile().getAbsolutePath());
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
                parseResult.message = resultObj.getString("message");
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setParseResult");

        AssetInfo parserAsset = assets.getAsset(PARSER);
        if (parserAsset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + PARSER);
        nodeJS.exec(parserAsset.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        fileObj.release();
        nodeJS.release();

        if (!parseResult.status.equals("OK"))
            throw new ServiceException(PARSER + parseResult);

        TestExecConfig config = ServiceLocator.getTestingServices().getTestExecConfig();

        nodeJS = NodeJS.createNodeJS();

        V8Object testObj = new V8Object(nodeJS.getRuntime());
        testObj.add("file", testCase.getAsset().getFile().getAbsolutePath());
        V8Array valueFiles = new V8Array(nodeJS.getRuntime());
        String valueFile = "localhost.env";
        if (config.getPostmanEnv() != null) {
            String env = config.getPostmanEnv();
            int lastSlash = env.lastIndexOf('/');
            if (lastSlash > 0) {
                // package path syntax
                valueFile = ServiceLocator.getAssetServices().getAssetRoot().getAbsolutePath() + "/"
                        + env.substring(0, lastSlash).replace('.', '/') + env.substring(lastSlash);
            }
            else {
                valueFile = env;
            }
        }
        valueFiles.push(valueFile);
        testObj.add("valueFiles", valueFiles);
        valueFiles.release();
        try {
            testObj.add("resultDir", ServiceLocator.getTestingServices().getTestResultsDir() + "/" + testCase.getPackage());
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        final Map<String,TestCaseItem> testCaseItems = new HashMap<>();
        final V8Array testItems = new V8Array(nodeJS.getRuntime());
        for (TestCaseItem item : testCase.getItems()) {
            String itemId = item.getName();
            V8Object itemObj = new V8Object(nodeJS.getRuntime()).add("name", item.getName());
            if (item.getObject().has("request")) {
                JSONObject request = item.getObject().getJSONObject("request");
                if (request.has("method")) {
                    itemObj.add("method", request.getString("method"));
                    itemId = request.getString("method") + ":" + itemId;
                }
            }
            JSONObject options = item.getOptions() == null ? new JSONObject() : item.getOptions();
            if (config.isVerbose() && !options.has("debug"))
                options.put("debug", true);
            if (config.isCreateReplace() && !options.has("overwriteExpected"))
                options.put("overwriteExpected", true);
            options.put("qualifyLocations", false);
            if (JSONObject.getNames(options) != null) {
                V8Object json = nodeJS.getRuntime().getObject("JSON");
                V8Array params = new V8Array(nodeJS.getRuntime()).push(options.toString());
                V8Object jsonObj = json.executeObjectFunction("parse", params);
                itemObj.add("options", jsonObj);
                params.release();
                json.release();
                jsonObj.release();
            }
            if (item.getValues() != null) {
                V8Object json = nodeJS.getRuntime().getObject("JSON");
                V8Array params = new V8Array(nodeJS.getRuntime()).push(item.getValues().toString());
                V8Object jsonObj = json.executeObjectFunction("parse", params);
                itemObj.add("values", jsonObj);
                params.release();
                json.release();
                jsonObj.release();
            }
            testItems.push(itemObj);
            testCaseItems.put(itemId, item);
            itemObj.release();
        }
        testObj.add("items", testItems);
        testItems.release();

        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return testObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getTestCase");

        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                String itemId = parameters.getString(0);
                V8Object resultObj = parameters.getObject(1);
                if (itemId == null) {
                    for (TestCaseItem item : testCase.getItems()) {
                        updateItem(item, resultObj);
                    }
                }
                else {
                    TestCaseItem item = testCaseItems.get(itemId);
                    if (item != null) {
                        updateItem(item, resultObj);
                    }
                }
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setTestResult");

        final V8 v8 = nodeJS.getRuntime();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                String itemId = parameters.getString(0);
                V8Object responseObj = parameters.getObject(1);
                TestCaseItem item = testCaseItems.get(itemId);
                if (item != null) {
                    V8Object json = v8.getObject("JSON");
                    V8Array params = new V8Array(v8).push(responseObj);
                    String jsonStr = json.executeStringFunction("stringify", params);
                    params.release();
                    json.release();
                    item.setResponse(new JSONObject(jsonStr));
                }
                responseObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setTestResponse");

        AssetInfo runner = assets.getAsset(RUNNER);
        if (runner == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + RUNNER);
        nodeJS.exec(runner.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        testObj.release();
        nodeJS.release();
    }

    private class Result {
        String status;
        String message;
        public String toString() {
            return status + ": " + message;
        }
    }

    private void updateItem(TestCaseItem item, V8Object resultObj) {
        if (Arrays.asList(resultObj.getKeys()).contains("start"))
            item.setStart(parseIso(resultObj.getString("start")));
        item.setStatus(Status.valueOf(resultObj.getString("status")));
        item.setMessage(resultObj.getString("message"));
        if (Arrays.asList(resultObj.getKeys()).contains("end"))
            item.setEnd(parseIso(resultObj.getString("end")));
    }

    private Date parseIso(String iso) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(iso);
        }
        catch (ParseException ex) {
            return null;
        }
    }
}
