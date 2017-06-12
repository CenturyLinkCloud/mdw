/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing.postman;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.test.TestCase;
import com.eclipsesource.v8.NodeJS;

/**
 * Runs NodeJS test assets (through runner.js).
 */
public class NodeRunner {
    static final String RUNNER = "com.centurylink.mdw.testing.postman/runner.js";

    public void run(TestCase testCase) throws ServiceException {
        // TODO honor testCase

        AssetInfo asset = ServiceLocator.getAssetServices().getAsset(RUNNER);
        final NodeJS nodeJS = NodeJS.createNodeJS();
        nodeJS.exec(asset.getFile());
        while (nodeJS.isRunning()) {
            nodeJS.handleMessage();
        }
        nodeJS.release();
    }
}
