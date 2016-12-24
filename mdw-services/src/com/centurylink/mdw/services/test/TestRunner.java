/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * TODO: user clicks Run without selecting anything results in this:
 * java.net.SocketException: Socket is closed
 *   at java.net.ServerSocket.setSoTimeout(ServerSocket.java:651)
 *   at com.centurylink.mdw.soccom.SoccomServer.start_sub(SoccomServer.java:115)
 *   at com.centurylink.mdw.soccom.SoccomServer.access$0(SoccomServer.java:109)
 *   at com.centurylink.mdw.soccom.SoccomServer$1.run(SoccomServer.java:99)
 *
 */
public class TestRunner implements Runnable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int PAUSE = 2500;
    private static LogMessageMonitor monitor;

    private String suiteName;
    private List<TestCase> testCases;
    private String user;
    private File resultsFile;
    private TestExecConfig config;

    private Map<String,Process> processCache;
    private Map<String,TestCase.Status> testCaseStatuses;

    public TestRunner(String suiteName, List<TestCase> testCases, String user, File resultsFile, TestExecConfig config) {
        this.suiteName = suiteName;
        this.testCases = testCases;
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

            for (TestCase testCase : testCases) {
                String masterRequestId = user + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                TestCaseRun run = new TestCaseRun(testCase, user, resultsFile.getParentFile(), 0, masterRequestId, monitor, processCache, config);

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

            while (!updateResults()) {
                try {
                    Thread.sleep(PAUSE);
                }
                catch (InterruptedException e) {
                }
            }
        }
        catch (IOException ex) {
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
    private synchronized boolean updateResults() throws IOException {
        boolean allDone = true;
        for (TestCase testCase : testCases) {
            if (!testCase.isFinished())
                allDone = false;
            Status oldStatus = testCaseStatuses.get(testCase.getPath());
            boolean statusChanged = oldStatus != testCase.getStatus();
            testCaseStatuses.put(testCase.getPath(), testCase.getStatus());
            if (statusChanged) {
                writeTestCaseResults(testCase);
            }
        }
        return allDone;
    }

    public void writeTestCaseResults(TestCase exeTestCase) throws IOException {

        int errors = 0;
        int failures = 0;
        int completed = 0;

        StringBuffer suiteBuf = new StringBuffer();
        suiteBuf.append("<testsuite ");
        suiteBuf.append("name=\"").append(suiteName).append("\" ");
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

    private void writeFile(File file, byte[] contents) throws IOException {
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
