/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Test runner for in-container execution.
 */
public class TestRunner implements Runnable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int PAUSE = 2500;
    private static LogMessageMonitor monitor;

    private TestCaseList testCaseList;
    private String user;
    private File resultsFile;
    private TestExecConfig config;

    private Map<String,Process> processCache;
    private Map<String,TestCase.Status> testCaseStatuses;

    public TestRunner(TestCaseList testCaseList, String user, File resultsFile, TestExecConfig config) {
        this.testCaseList = testCaseList;
        this.user = user;
        this.resultsFile = resultsFile;
        this.config = config;
    }

    public void run() {

        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        processCache = new HashMap<String,Process>();
        testCaseStatuses = new HashMap<String,TestCase.Status>();

        try {
            if (monitor == null || monitor.isClosed()) {
                monitor = new LogMessageMonitor();
                monitor.start(true);
            }

            for (TestCase testCase : testCaseList.getTestCases()) {
                String masterRequestId = user + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                TestCaseRun run = new TestCaseRun(testCase, user, resultsFile.getParentFile(), 0, masterRequestId, monitor, processCache, config);
                logger.info(" -> Executing test: " + testCase.getPath());
                if (!threadPool.execute(ThreadPoolProvider.WORKER_TESTING, getClass().getSimpleName(), run))
                    throw new IllegalStateException("No available thread: " + ThreadPoolProvider.WORKER_TESTING);
                if (updateResults())
                    return;

                try {
                    Thread.sleep(config.getInterval() * 1000);
                }
                catch (InterruptedException e) {
                }
            }

            do {
                try {
                    Thread.sleep(PAUSE); // pause at least once to too-quick socket shutdown
                }
                catch (InterruptedException e) {
                }
            } while (!updateResults());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            if (monitor != null)
                monitor.shutdown();
        }
    }

    /**
     * Returns true if all done.
     */
    private synchronized boolean updateResults() throws JSONException, IOException {
        boolean allDone = true;
        for (TestCase testCase : testCaseList.getTestCases()) {
            if (!testCase.isFinished())
                allDone = false;
            Status oldStatus = testCaseStatuses.get(testCase.getPath());
            boolean statusChanged = oldStatus != testCase.getStatus();
            testCaseStatuses.put(testCase.getPath(), testCase.getStatus());
            if (statusChanged) {
                if (resultsFile.getName().endsWith(".json"))
                    writeTestResultsJSON(testCase);
                else if (resultsFile.getName().endsWith(".xml"))
                        writeTestResultsXml(testCase);
                    else
                    writeTestResults(testCase);
            }
        }
        return allDone;
    }

    public void writeTestResults(TestCase exeTestCase) throws JSONException, IOException {
        if (!resultsFile.exists())
            writeFile(resultsFile, testCaseList.getJson().toString(2).getBytes());
        String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        TestCaseList testCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JSONObject(jsonString));
        TestCase testCase = testCaseList.getTestCase(exeTestCase.getPath());
        if (testCase == null)
            testCase = testCaseList.addTestCase(exeTestCase);
        if (testCase != null) {
            testCase.setStatus(exeTestCase.getStatus());
            testCase.setStart(exeTestCase.getStart());
            testCase.setEnd(exeTestCase.getEnd());
            testCase.setMessage(exeTestCase.getMessage());
            testCaseList.setCount(testCaseList.getTestCases().size());
            writeFile(resultsFile, testCaseList.getJson().toString(2).getBytes());
        }
    }

    public void writeTestResultsXml(TestCase exeTestCase) throws IOException {

        List<TestCase> testCases = testCaseList.getTestCases();
        int errors = 0;
        int failures = 0;
        int completed = 0;

        StringBuffer suiteBuf = new StringBuffer();
        suiteBuf.append("<testsuite ");
        suiteBuf.append("name=\"").append(testCaseList.getSuite()).append("\" ");
        suiteBuf.append("tests=\"").append(testCases.size()).append("\" ");

        StringBuffer results = new StringBuffer();
        for (TestCase testCase : testCases) {
            results.append("  <testcase ");
            results.append("name=\"").append(testCase.getPath()).append("\" ");
            Date start = testCase.getStart();
            if (start != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(start);
                results.append("timestamp=\"").append(DatatypeConverter.printDateTime(startCal))
                        .append("\" ");
                Date end = testCase.getEnd();
                if (end != null) {
                    long ms = end.getTime() - start.getTime();
                    results.append("time=\"").append(ms / 1000).append("\" ");
                }
            }
            if (testCase.isFinished()) {
                completed++;
            }
            if (testCase.getStatus() == Status.Errored) {
                errors++;
                results.append(">\n");
                results.append("    <error ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("/>\n");
                results.append("  </testcase>\n");
            }
            else if (testCase.getStatus() == Status.Failed) {
                failures++;
                results.append(">\n");
                results.append("    <failure ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("/>\n");
                results.append("  </testcase>\n");
            }
            else if (testCase.getStatus() == Status.InProgress) {
                failures++;
                results.append(">\n");
                results.append("    <running />\n");
                results.append("  </testcase>\n");
            }
            else {
                results.append("/>\n");
            }
        }
        suiteBuf.append("completed=\"").append(completed).append("\" ");
        suiteBuf.append("errors=\"").append(errors).append("\" ");
        suiteBuf.append("failures=\"").append(failures).append("\" ");
        suiteBuf.append(">\n");
        suiteBuf.append(results);
        suiteBuf.append("</testsuite>");

        writeFile(resultsFile, suiteBuf.toString().getBytes());
    }


    public void writeTestResultsJSON(TestCase exeTestCase) throws IOException {

        List<TestCase> testCases = testCaseList.getTestCases();
        int errors = 0;
        int failures = 0;
        int completed = 0;

        StringBuffer suiteBuf = new StringBuffer();
        suiteBuf.append("\"testsuite\": [ ");
        suiteBuf.append("name=\"").append(testCaseList.getSuite()).append("\" ");
        suiteBuf.append("tests=\"").append(testCases.size()).append("\" ");

        StringBuffer results = new StringBuffer();
        for (TestCase testCase : testCases) {
            results.append("  \"testcase\": [{");
            results.append("\"name=\" : \"").append(testCase.getPath()).append("\" ,");
            Date start = testCase.getStart();
            if (start != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(start);
                results.append("\"timestamp=\":\"").append(DatatypeConverter.printDateTime(startCal))
                        .append("\" ,");
                Date end = testCase.getEnd();
                if (end != null) {
                    long ms = end.getTime() - start.getTime();
                    results.append("\"time=\" :\"").append(ms / 1000).append("\" ,");
                }
            }
            if (testCase.isFinished()) {
                completed++;
            }
            if (testCase.getStatus() == Status.Errored) {
                errors++;
                results.append("]\n");
                results.append("    error[ ");
                if (testCase.getMessage() != null)
                    results.append("\"message=\" :\"").append(testCase.getMessage()).append("\" ");
                results.append("]");
                results.append("  }\n");
            }
            else if (testCase.getStatus() == Status.Failed) {
                failures++;
                results.append("]\n");
                results.append("    \"failure \" :\"");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("]\n");
                results.append("  }\n");
            }
            else if (testCase.getStatus() == Status.InProgress) {
                failures++;
                results.append("]\n");
                results.append("    \"running \"\n");
                results.append("  }\n");
            }
            else {
                results.append("]\n");
            }
        }
        suiteBuf.append("\"completed=\": \"").append(completed).append("\" ");
        suiteBuf.append("\"errors=\": \"").append(errors).append("\" ");
        suiteBuf.append("\"failures=\":\"").append(failures).append("\" ");
        suiteBuf.append("]\n");
        suiteBuf.append(results);
        suiteBuf.append("}");

        writeFile(resultsFile, suiteBuf.toString().getBytes());
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

}
