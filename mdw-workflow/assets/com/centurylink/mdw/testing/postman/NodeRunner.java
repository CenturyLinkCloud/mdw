/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing.postman;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseItem;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * Runs NodeJS test assets (through runner.js).
 */
public class NodeRunner {
    static final String PARSER = "com.centurylink.mdw.testing.postman/parser.js";
    static final String RUNNER = "com.centurylink.mdw.testing.postman/runner.js";

    public void run(TestCase testCase) throws ServiceException {

        AssetServices assets = ServiceLocator.getAssetServices();

        NodeJS nodeJS = NodeJS.createNodeJS();

        final Result parseResult = new Result();
        JavaCallback callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj = parameters.getObject(0);
                parseResult.status = resultObj.getString("status");
                parseResult.message = resultObj.getString("message");
                System.out.println("  Parse Result: " + parseResult);
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setParseResult");

        nodeJS.exec(assets.getAsset(PARSER).getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        if (!parseResult.status.equals("OK"))
            throw new ServiceException(PARSER + parseResult);

        nodeJS.release();

        nodeJS = NodeJS.createNodeJS();

        V8Object testObj = new V8Object(nodeJS.getRuntime()).add("coll", testCase.getAsset().getFile().getAbsolutePath());
        final V8Array testItems = new V8Array(nodeJS.getRuntime());
        for (TestCaseItem item : testCase.getItems()) {
            V8Object itemObj = new V8Object(nodeJS.getRuntime()).add("name", item.getName());
            testItems.push(itemObj);
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

        final Result testResult = new Result();
        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj =parameters.getObject(0);
                testResult.status = resultObj.getString("status");
                testResult.message = resultObj.getString("message");
                System.out.println("  Test Result: " + testResult);
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setTestResult");

        nodeJS.exec(assets.getAsset(RUNNER).getFile());
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
}
