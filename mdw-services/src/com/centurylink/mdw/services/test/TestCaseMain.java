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
package com.centurylink.mdw.services.test;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.HttpHelper;

public class TestCaseMain {
    /**
     * Execute a test case in standalone mode (for Designer debug).
     * Takes a single argument: testCasePath.
     * TODO: Refactor when implementing Gradle test run task.
     */
    public static void main(String args[]) throws Throwable {

        // asset root
        String assetLoc = System.getProperty("mdw.asset.root");
        if (assetLoc == null)
            throw new IllegalStateException("Missing system property: mdw.asset.root");
        File assetRoot = new File(assetLoc);
        if (!assetRoot.isDirectory())
            throw new IllegalStateException("Asset directory not found: " + assetRoot.getAbsolutePath());
        ApplicationContext.setAssetRoot(assetRoot);

        // test case path
        if (args.length != 1)
            throw new IllegalArgumentException("Supports a single arg: testCasePath");
        String testCasePath = args[0];
        if (testCasePath.startsWith("${resource_loc:") && testCasePath.endsWith("}"))
            testCasePath = testCasePath.substring(15, testCasePath.length() - 1);
        // convert to asset path format
        if (testCasePath.startsWith(assetLoc))
            testCasePath = testCasePath.substring(assetLoc.length() + 1);
        testCasePath = testCasePath.replace('\\', '/');
        int lastSlash = testCasePath.lastIndexOf('/');
        testCasePath = testCasePath.substring(0, lastSlash).replace('/', '.') + testCasePath.substring(lastSlash);

        TestExecConfig execConfig = new TestExecConfig(System.getProperties());

        // user & masterRequestId
        String user = System.getProperty("mdw.test.user");
        if (user == null)
            throw new IllegalStateException("Missing system property: mdw.test.user");
        String masterRequestId = System.getProperty("mdw.test.master.request.id");
        if (masterRequestId == null)
            throw new IllegalStateException("Missing system property: mdw.test.master.request.id");

        TestingServices testingServices = ServiceLocator.getTestingServices();
        TestCase testCase = testingServices.getTestCase(testCasePath);

        File resultsFile = testingServices.getTestResultsFile(null);

        LogMessageMonitor monitor = new LogMessageMonitor();
        monitor.start(true);

        final Map<String,TestCaseRun> masterRequestRuns = new HashMap<>();

        if (StubServer.isRunning())
            StubServer.stop();
        if (execConfig.isStubbing())
            StubServer.start(new TestStubber(masterRequestRuns));

        Map<String,Process> processCache = new HashMap<>();
        TestCaseRun run = new StandaloneTestCaseRun(testCase, user, resultsFile.getParentFile(), 0,
                masterRequestId, monitor, processCache, execConfig);
        masterRequestRuns.put(masterRequestId, run);
        run.setMasterRequestListener(new MasterRequestListener() {
            public void syncMasterRequestId(String oldId, String newId) {
                TestCaseRun run = masterRequestRuns.remove(oldId);
                if (run != null) {
                    masterRequestRuns.put(newId, run);
                }
            }
        });

        try {
            // tell the server we're monitoring
            HttpHelper httpHelper = new HttpHelper(new URL(execConfig.getServerUrl() + "/services/System/config"));
            JSONObject configJson = new JsonObject();
            configJson.put(PropertyNames.MDW_LOGGING_WATCHER, InetAddress.getLocalHost().getHostAddress());
            if (execConfig.isStubbing())
                configJson.put(PropertyNames.MDW_STUB_SERVER, InetAddress.getLocalHost().getHostAddress());
            StatusMessage msg = new StatusMessage(new JsonObject(httpHelper.put(configJson.toString(2))));
            if (!msg.isSuccess())
                System.out.println("Error setting server config: " + msg.getMessage());

            run.run();

            // stop monitoring
            httpHelper = new HttpHelper(new URL(execConfig.getServerUrl() + "/services/System/config"));
            configJson.put(PropertyNames.MDW_LOGGING_WATCHER, "");
            if (execConfig.isStubbing())
                configJson.put(PropertyNames.MDW_STUB_SERVER, "");
            msg = new StatusMessage(new JsonObject(httpHelper.put(configJson.toString(2))));
            if (!msg.isSuccess())
                System.out.println("Error setting server config: " + msg.getMessage());

            if (run.getTestCase().getStatus() == Status.Passed)
                System.exit(0);
            else
                System.exit(1); // fail

        }
        catch (Throwable th) {
            th.printStackTrace(run.getLog());
            throw th;
        }
        finally {
            monitor.shutdown();
        }

        // reach here only when error
        System.exit(1);
    }

    /**
     * Replaces main() in GroovyStarter in order to call System.exit() when finished.
     */
    public static class GroovyStarter {
        public static void main(String args[]) {
            org.codehaus.groovy.tools.GroovyStarter.main(args);
        }
    }
}
