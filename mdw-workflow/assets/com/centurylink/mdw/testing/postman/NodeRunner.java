/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing.postman;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
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
    static final String RUNNER = "com.centurylink.mdw.testing.postman/runner.js";

    public void run(TestCase testCase) throws ServiceException {
        // TODO honor testCase

        AssetInfo asset = ServiceLocator.getAssetServices().getAsset(RUNNER);
        final NodeJS nodeJS = NodeJS.createNodeJS();
        V8Object testObj = new V8Object(nodeJS.getRuntime()).add("coll", testCase.getAsset().getFile().getAbsolutePath());
        final V8Array testItems = new V8Array(nodeJS.getRuntime());
        for (TestCaseItem item : testCase.getItems()) {
            V8Object itemObj = new V8Object(nodeJS.getRuntime()).add("name", item.getName());
            testItems.push(itemObj);
            itemObj.release();
        }
        testObj.add("items", testItems);
        testItems.release();

        JavaCallback callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
              return testObj;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "getTestCase");

        callback = new JavaCallback() {
            public Object invoke(V8Object receiver, V8Array parameters) {
                V8Object resultObj =parameters.getObject(0);
                System.out.println("  Test Result: " + resultObj.getString("status") + ": " + resultObj.getString("message"));
                resultObj.release();
                return null;
            }
        };
        nodeJS.getRuntime().registerJavaMethod(callback, "setResult");


        nodeJS.exec(asset.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }

        testObj.release();

        nodeJS.release();
    }
}
