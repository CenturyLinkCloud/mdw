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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.HttpHelper;

public class MultiTestCaseMain {
    /**
     * Execute a test case in standalone mode (for Designer debug).
     */
    public static void main(String args[]) throws Throwable {
        final int PAUSE = 2500;

        CommandLine clArgs = StandaloneTestCaseRun.getCommandLine(args);
        Map<String,TestCase.Status> testCaseStatuses = new HashMap<String,TestCase.Status>();
        // asset root
        String assetLoc = System.getProperty("mdw.asset.root");
        if (assetLoc == null)
            throw new IllegalStateException("Missing system property: mdw.asset.root");
        File assetRoot = new File(assetLoc);
        if (!assetRoot.isDirectory())
            throw new IllegalStateException("Asset directory not found: " + assetRoot.getAbsolutePath());
        ApplicationContext.setAssetRoot(assetRoot);
        TestCaseList testCaseList = new TestCaseList(assetRoot);
        String user = System.getProperty("mdw.test.user");
        if (user == null)
            throw new IllegalStateException("Missing system property: mdw.test.user");
        String interval = System.getProperty("mdw.test.intervalSecs");
        if (interval == null)
            interval = "2";

        // test case path
        if (args.length < 1)
            throw new IllegalArgumentException("Supports a single arg: testCasePaths separated by ,");

        StandaloneTestCaseRun run = null;
        TestingServices testingServices = ServiceLocator.getTestingServices();
        File resultsFile = testingServices.getTestResultsFile(null);

        LogMessageMonitor monitor = new LogMessageMonitor();
        boolean running = true;

        try {
            monitor.start(true);

            Map<String,Process> processCache = new HashMap<>();
            TestExecConfig execConfig = new TestExecConfig(System.getProperties());
            String stubbing = System.getProperty("mdw.test.stubbing");
            if (stubbing != null && "true".equalsIgnoreCase(stubbing)) {
                execConfig.setStubbing(true);
            }

            if (clArgs.hasOption("byFormat")) {
                System.out.println("Option byFormat is present.  The value is: " + clArgs.getOptionValue("byFormat"));
                testCaseList = testingServices.getTestCases(new String[]{clArgs.getOptionValue("byFormat")});
            }
            if (clArgs.hasOption("includes")) {
                System.out.println("Option includes is present.  The value is: "  + clArgs.getOptionValue("includes"));
                testCaseList = StandaloneTestCaseRun.addTestCases(testingServices, assetLoc, testCaseList, clArgs.getOptionValue("includes"));
            }

            //TODO  implement logic to excludes test cases

         // tell the server we're monitoring
            HttpHelper httpHelper = new HttpHelper(new URL(execConfig.getServerUrl() + "/services/System/config"));

            JSONObject configJson = new JSONObject();
            configJson.put(PropertyNames.MDW_LOGGING_WATCHER, InetAddress.getLocalHost().getHostAddress());
            if (execConfig.isStubbing())
                configJson.put(PropertyNames.MDW_STUB_SERVER, InetAddress.getLocalHost().getHostAddress());
            StatusMessage msg = new StatusMessage(new JSONObject(httpHelper.put(configJson.toString(2))));
            if (!msg.isSuccess())
                System.out.println("Error setting server config: " + msg.getMessage());


            final Map<String,TestCaseRun> masterRequestRuns = new HashMap<>();
            if (StubServer.isRunning())
                StubServer.stop();
            if (execConfig.isStubbing())
                StubServer.start(new TestStubber(masterRequestRuns));

            for (TestCase testCase : testCaseList.getTestCases()) {
                if (!running)
                    return;
                testCase.setStatus(null);
                testCaseStatuses.put(testCase.getPath(), null);
                String masterRequestId = user + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                run = new StandaloneTestCaseRun(testCase, user, resultsFile, 0,
                        masterRequestId, monitor, processCache, execConfig, true);
                masterRequestRuns.put(masterRequestId, run);
                run.setMasterRequestListener(new MasterRequestListener() {
                    public void syncMasterRequestId(String oldId, String newId) {
                        TestCaseRun run = masterRequestRuns.remove(oldId);
                        if (run != null) {
                            masterRequestRuns.put(newId, run);
                        }
                    }
                });

                run.run();
                testCaseStatuses.put(testCase.getPath(), testCase.getStatus());
                run.writeTestResults(testCase, testCaseList);
                Thread.sleep((new Integer(interval).intValue()));
            }

            // wait for all tests to finish
            Thread.sleep(PAUSE); // pause at least once to avoid too-quick socket shutdown
            httpHelper = new HttpHelper(new URL(execConfig.getServerUrl() + "/services/System/config"));
            configJson.put(PropertyNames.MDW_LOGGING_WATCHER, "");
            if (execConfig.isStubbing())
                configJson.put(PropertyNames.MDW_STUB_SERVER, "");
            msg = new StatusMessage(new JSONObject(httpHelper.put(configJson.toString(2))));
            if (!msg.isSuccess())
                System.out.println("Error setting server config: " + msg.getMessage());

           System.exit(0);
        }
        catch (Throwable th) {
            th.printStackTrace();
            throw th;
        }
        finally {
            running = false;
            if (StubServer.isRunning())
                StubServer.stop();
            if (monitor != null)
                monitor.shutdown();
            System.exit(0);
        }
        // reach here only when error
        //System.exit(0);
    }
}

