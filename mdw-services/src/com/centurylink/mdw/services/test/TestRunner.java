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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.MdwWebSocketServer;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestCaseItem;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Test runner for in-container execution.
 */
public class TestRunner implements Runnable, MasterRequestListener {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int PAUSE = 2500;
    private static LogMessageMonitor monitor;

    private ExecutorService threadPool; // using common thread pool causes deadlock
    private TestCaseList testCaseList;
    private String user;
    private File resultsFile;
    private TestExecConfig config;

    private Map<String,Process> processCache;
    private Map<String,TestCase.Status> testCaseStatuses;
    private Map<String,TestCaseRun> masterRequestRuns;

    private boolean running;
    public boolean isRunning() {
        return running;
    }
    public void terminate() {
        if (running) {
            running = false;
            if (testCaseList != null) {
                try {
                    Thread.sleep(PAUSE * 2);
                    for (TestCase testCase : testCaseList.getTestCases()) {
                        if (testCase.getStatus() == Status.InProgress)
                            testCase.setStatus(Status.Stopped);
                    }
                    updateResults();
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }

        }
    }

    public void init(TestCaseList testCaseList, String user, File resultsFile, TestExecConfig config) {
        this.testCaseList = testCaseList;
        this.user = user;
        this.resultsFile = resultsFile;
        this.config = config;
    }

    public void run() {
        threadPool = Executors.newFixedThreadPool(config.getThreads());
        processCache = new HashMap<String,Process>();
        testCaseStatuses = new HashMap<String,TestCase.Status>();
        masterRequestRuns = new ConcurrentHashMap<>();

        running = true;

        try {
            if (monitor == null || monitor.isClosed()) {
                monitor = new LogMessageMonitor();
                monitor.start(true);
            }

            // socket client
            setLogWatchState(true);

            if (StubServer.isRunning())
                StubServer.stop();
            if (config.isStubbing())
                StubServer.start(new TestStubber(masterRequestRuns));

            // stubbing
            setStubServerState(config.isStubbing());

            // clear statutes for selected tests
            initResults();

            for (TestCase testCase : testCaseList.getTestCases()) {
                if (!running)
                    return;

                String masterRequestId = user + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                TestCaseRun run = new TestCaseRun(testCase, user, resultsFile.getParentFile(), 0, masterRequestId, monitor, processCache, config);
                masterRequestRuns.put(masterRequestId, run);
                run.setMasterRequestListener(this);

                logger.debug(" -> Executing test: " + testCase.getPath());
                threadPool.execute(run);

                if (updateResults() || !running)
                    return;

                Thread.sleep(config.getInterval() * 1000);
            }

            // wait for all tests to finish
            do {
                Thread.sleep(PAUSE); // pause at least once to avoid too-quick socket shutdown
            } while (!updateResults() && running);

            setLogWatchState(false);
            setStubServerState(false);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            running = false;
            threadPool.shutdown();
            if (StubServer.isRunning())
                StubServer.stop();
            if (monitor != null)
                monitor.shutdown();
        }
    }

    private void initResults() throws JSONException, IOException {
        for (TestCase testCase : testCaseList.getTestCases()) {
            testCase.setStatus(null);
            testCaseStatuses.put(testCase.getPath(), null);
            if (testCase.getItems() != null) {
                for (TestCaseItem item : testCase.getItems()) {
                    item.setStatus(null);
                    testCaseStatuses.put(testCase.getItemPath(item.getName()), null);
                }
            }
            writeTestResults(testCase);
        }
    }

    /**
     * Returns true if all done.
     */
    private synchronized boolean updateResults() throws JSONException, IOException {

        boolean allDone = true;
        TestCaseList fullTestCaseList = null;
        for (TestCase testCase : testCaseList.getTestCases()) {
            boolean statusChanged = false;
            if (!isFinished(testCase.getStatus()))
                allDone = false;
            Status oldStatus = testCaseStatuses.get(testCase.getPath());
            if (oldStatus != testCase.getStatus())
                statusChanged = true;
            if (testCase.getItems() == null) {
                testCaseStatuses.put(testCase.getPath(), testCase.getStatus());
            }
            else {
                // case status applies to all running items (currently one)
                for (TestCaseItem item : testCase.getItems()) {
                    if (!isFinished(item.getStatus()))
                        allDone = false;
                    Status oldItemStatus = testCaseStatuses.get(testCase.getItemPath(item.getName()));
                    if (oldItemStatus != item.getStatus())
                        statusChanged = true;
                    testCaseStatuses.put(testCase.getItemPath(item.getName()), testCase.getStatus());
                }
            }

            if (statusChanged) {
                fullTestCaseList = writeTestResults(testCase);
                if (fullTestCaseList != null)
                    updateWebSocket(fullTestCaseList);
            }
        }

        return allDone;
    }

    private boolean isFinished(Status status) {
        return status != null && status != Status.Waiting
                && status != Status.InProgress;
    }

    /**
     * force immediate update through WebSocket
     */
    private void updateWebSocket(TestCaseList testCaseList) {
        MdwWebSocketServer webSocketServer = MdwWebSocketServer.getInstance();

        if (webSocketServer.hasInterestedConnections("AutomatedTests")) {
            try {
                webSocketServer.send(testCaseList.getJson().toString(2), "AutomatedTests");
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
    }

    public TestCaseList writeTestResults(TestCase exeTestCase) throws JSONException, IOException {
        if (!resultsFile.exists())
            writeFile(resultsFile, testCaseList.getJson().toString(2).getBytes());
        String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        TestCaseList fullTestCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JsonObject(jsonString));
        PackageTests pkgTests = fullTestCaseList.getPackageTests(exeTestCase.getPackage());
        if (pkgTests == null) {
            pkgTests = new PackageTests(new PackageDir(ApplicationContext.getAssetRoot(), exeTestCase.getPackage(), null));
            pkgTests.setTestCases(new ArrayList<TestCase>());
            fullTestCaseList.addPackageTests(pkgTests);
        }
        TestCase testCase = fullTestCaseList.getTestCase(exeTestCase.getPath());
        if (testCase == null)
            testCase = fullTestCaseList.addTestCase(exeTestCase);
        if (testCase != null) {
            testCase.setStatus(exeTestCase.getStatus());
            testCase.setStart(exeTestCase.getStart());
            testCase.setEnd(exeTestCase.getEnd());
            testCase.setMessage(exeTestCase.getMessage());
            if (exeTestCase.getItems() != null) {
                List<TestCaseItem> toAdd = new ArrayList<>();
                for (TestCaseItem exeItem : exeTestCase.getItems()) {
                    TestCaseItem item = testCase.getItem(exeItem.getName());
                    if (exeItem.getObject().has("request")) {
                        JSONObject request = exeItem.getObject().getJSONObject("request");
                        if (request.has("method"))
                            item = testCase.getItem(exeItem.getName(), request.getString("method"));
                    }
                    if (item == null) {
                        item = exeItem;
                        toAdd.add(item);
                    }
                    if (item != null) {
                        item.setStatus(exeItem.getStatus());
                        item.setStart(exeItem.getStart());
                        item.setEnd(exeItem.getEnd());
                        item.setMessage(exeItem.getMessage());
                    }
                }
                testCase.getItems().addAll(toAdd);
            }
            fullTestCaseList.setCount(fullTestCaseList.getTestCases().size());
            fullTestCaseList.sort();
            writeFile(resultsFile, fullTestCaseList.getJson().toString(2).getBytes());
        }
        return fullTestCaseList;
    }

    private void writeFile(File file, byte[] contents) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new IOException("Unable to create directory: " + file.getParentFile());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(contents);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    @Override
    public void syncMasterRequestId(String oldId, String newId) {
        TestCaseRun run = masterRequestRuns.remove(oldId);
        if (run != null) {
            masterRequestRuns.put(newId, run);
        }
    }

    private void setLogWatchState(boolean on) throws JSONException, ProcessException, UnknownHostException {
        JSONObject json = new JsonObject();
        json.put("ACTION", "REFRESH_PROPERTY");
        json.put("NAME", PropertyNames.MDW_LOGGING_WATCHER);
        json.put("VALUE", on ? InetAddress.getLocalHost().getHostAddress() : "");
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }

    private void setStubServerState(boolean on) throws JSONException, ProcessException, UnknownHostException {
        JSONObject json = new JsonObject();
        json.put("ACTION", "REFRESH_PROPERTY");
        json.put("NAME", PropertyNames.MDW_STUB_SERVER);
        json.put("VALUE", on ?  InetAddress.getLocalHost().getHostAddress() : "");
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }
}
