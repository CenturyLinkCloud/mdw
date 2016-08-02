/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.utilities.file.TeePrintStream;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.testing.StubServer.Stubber;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;

/**
 * @deprecated This is only to be used for legacy test cases.
 * For asset-based test cases, see {@link AutoTestAntTask}.
 */
@Deprecated
public class RegressionTestAntTask extends MatchingTask {

    private String suiteName;
    public String getSuiteName() { return suiteName; }
    public void setSuiteName(String suiteName) { this.suiteName = suiteName; }

    private URL serverUrl;
    public URL getServerUrl() { return serverUrl; }
    public void setServerUrl(URL url) { this.serverUrl = url; }

    private String jdbcUrl;
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }

    private File workflowDir;
    public File getWorkflowDir() { return workflowDir; }
    public void setWorkflowDir(File dir) { this.workflowDir = dir; }

    private File testCasesDir;
    public File getTestCasesDir() { return testCasesDir; }
    public void setTestCasesDir(File dir) { this.testCasesDir = dir; }

    private File testResultsDir;
    public File getTestResultsDir() { return testResultsDir; }
    public void setTestResultsDir(File dir) { this.testResultsDir = dir; }

    private File testResultsSummaryFile;
    public File getTestResultsSummaryFile() { return testResultsSummaryFile; }
    public void setTestResultsSummaryFile(File file) { this.testResultsSummaryFile = file; }

    private int threadCount;
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int count) { this.threadCount = count; }

    private int intervalSecs;
    public int getIntervalSecs() { return intervalSecs; }
    public void setIntervalSecs(int secs) { this.intervalSecs = secs; }

    private String user;
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    private String password;
    public void setPassword(String password) { this.password = password; }

    private boolean verbose;
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    private boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    public void setStubbing(boolean stubbing) { this.stubbing = stubbing; }

    private boolean singleServer;
    public boolean isSingleServer() { return singleServer; }
    public void setSingleServer(boolean ss) { this.singleServer = ss; }

    private int stubPort = Integer.parseInt(AdapterActivity.DEFAULT_STUBBER_PORT);
    public int getStubPort() { return stubPort; }
    public void setStubPort(int stubPort) { this.stubPort = stubPort; }

    private boolean loadTests;
    public boolean isLoadTests() { return loadTests; }
    public void setLoadTests(boolean loadTests) { this.loadTests = loadTests; }

    private boolean oldNamespaces = false;
    public boolean isOldNamespaces() { return oldNamespaces; }
    public void setOldNamespaces(boolean oldNamespaces) { this.oldNamespaces = oldNamespaces; }

    private boolean failOnError = true;
    public boolean isFailOnError() { return failOnError; }
    public void setFailOnError(boolean foe) { this.failOnError = foe; }

    private URL testResultsBaseUrl;
    public URL getTestResultsBaseUrl() { return testResultsBaseUrl; }
    public void setTestResultsBaseUrl(URL testResultsBaseUrl) { this.testResultsBaseUrl = testResultsBaseUrl; }

    private File sslTrustStore;
    public File getSslTrustStore() { return sslTrustStore; }
    public void setSslTrustStore(File trustStore) { this.sslTrustStore = trustStore; }

    private DesignerDataAccess designerDataAccess;
    private Map<String,String> testCaseStatuses = new HashMap<String,String>();
    private Map<String,TestCaseRun> masterRequestRunMap = new HashMap<String,TestCaseRun>();

    public RegressionTestAntTask() {
    }

    @Override
    public void execute() throws BuildException {
        try {
            if (sslTrustStore != null)
                System.setProperty("javax.net.ssl.trustStore", sslTrustStore.getAbsolutePath());

            DesignerDataAccess.getAuthenticator().authenticate(user, password);

            RestfulServer restfulServer = new RestfulServer(jdbcUrl == null ? "dummy" : jdbcUrl, user, serverUrl.toString());

            if (workflowDir != null) {
                // vcs-based assets
                VersionControl versionControl = new VersionControlGit();
                versionControl.connect(null, null, null, workflowDir);
                restfulServer.setVersionControl(versionControl);
                restfulServer.setRootDirectory(workflowDir);
                restfulServer.setDatabaseUrl("jdbc://dummy");
                designerDataAccess = new DesignerDataAccess(restfulServer, null, user, false);
                designerDataAccess.setCurrentServer(restfulServer);
            }
            else {
                designerDataAccess = new DesignerDataAccess(restfulServer, null, user, oldNamespaces);
                if (designerDataAccess.getUser(user) == null)
                    throw new BuildException("User '" + user + "' not found to run regression tests for this environment.");
                if (!designerDataAccess.getUser(user).hasRole(UserGroupVO.COMMON_GROUP, UserRoleVO.PROCESS_EXECUTION)) {
                     if(!designerDataAccess.getUser(user).hasRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION))
                         throw new BuildException("User '" + user + "' not authorized to run regression tests for this environment.");
                }
            }
            VariableTypeCache.loadCache(designerDataAccess.getVariableTypes());
            log("Setup completed for test suite: '" + suiteName + "'", Project.MSG_ERR);

            if (stubbing) {
                Stubber stubber = new Stubber() {
                    public String processMessage(String masterRequestId, String request) {
                        TestCaseRun run = masterRequestRunMap.get(masterRequestId);
                        if (run == null) {
                            if (request != null && request.startsWith("{\"ActivityRuntimeContext\":"))
                                return "(EXECUTE_ACTIVITY)";
                            else
                                return AdapterActivity.MAKE_ACTUAL_CALL;
                        }
                        return run.getStubResponse(masterRequestId, request, run.getRunNumber());
                    }
                };
                if (StubServer.isRunning())
                  StubServer.stop();
                StubServer.start(restfulServer, stubPort, stubber, this.oldNamespaces);
            }

            runTests();

            StubServer.stop();
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Exception ex) {
            try {
                updateResults(true);
            }
            catch (BuildException bex) {
                throw bex;
            }
            catch (IOException ioex) {
                log(ioex.getMessage(), ioex, Project.MSG_ERR);
            }
            log(ex.getMessage(), ex, Project.MSG_ERR);
            throw new BuildException(ex);
        }
    }

    private List<TestCase> testCases = new ArrayList<TestCase>();

    public void runTests() throws Exception {
        boolean update = false;

        if (!testCasesDir.exists() || !testCasesDir.isDirectory())
            throw new IOException("Missing test cases directory: " + testCasesDir);

        DirectoryScanner scanner = getDirectoryScanner(testCasesDir);
        scanner.scan();
        String[] caseDirNames = scanner.getIncludedDirectories();

        ThreadPool threadPool = new ThreadPool(threadCount);
        HashMap<String,ProcessVO> processCache = new HashMap<String,ProcessVO>();
        boolean hasGherkin = false;
        boolean hasNonGherkin = false;
        for (String caseDirName : caseDirNames) {
            File caseDir = new File(testCasesDir + "/" + caseDirName);
            String caseName = caseDir.getName();
            if (!caseDir.equals(testCasesDir) && !caseName.startsWith(".") && !caseName.startsWith("CVS")) {
                TestCase tc = new TestCase("Legacy", caseDir);
                if (tc.isGherkin())
                    hasGherkin = true;
                else
                    hasNonGherkin = true;
                testCases.add(tc);
            }
        }

        if (hasGherkin && hasNonGherkin)
            throw new BuildException("Mixed Gherkin/non-Gherkin test selections not currently supported.");

        boolean useStdErr = System.getProperty("org.gradle.appname") != null;  // gradle does not show output otherwise

        LogMessageMonitor monitor = hasGherkin ? null : new LogMessageMonitor(designerDataAccess, oldNamespaces);
        if (monitor != null)
            monitor.start(true);

        for (TestCase testCase : testCases) {
            String masterRequestId = user + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File resultDir = new File(testResultsDir + "/" + testCase.getCaseName());
            testCase.prepare();
            TestCaseRun run;
            if (testCase.isGherkin()) { //User defined masterRequestId
                if (testCase.getMasterRequestId() != null) {
                    if (testCase.getMasterRequestId().indexOf("${masterRequestId}") != -1){
                        masterRequestId = testCase.getMasterRequestId().replace("${masterRequestId}", masterRequestId);
                    }
                    else
                        masterRequestId = testCase.getMasterRequestId();
                }
                testCase.setMasterRequestId(masterRequestId);
                run = new GherkinTestCaseRun(testCase, 0, masterRequestId, new DesignerDataAccess(designerDataAccess), monitor, processCache, oldNamespaces, testResultsDir, stubbing, stubPort);
            }
            else if (testCase.isGroovy())
                run = new GroovyTestCaseRun(testCase, 0, masterRequestId, new DesignerDataAccess(designerDataAccess), monitor, processCache, loadTests, true, oldNamespaces);
            else
                run = new TestCaseRun(testCase, 0, masterRequestId, new DesignerDataAccess(designerDataAccess), monitor, processCache, loadTests, true, oldNamespaces);
            File executeLog = new File(resultDir.getPath() + "/execute.log");
            if (!executeLog.getParentFile().exists() && !executeLog.getParentFile().mkdirs())
                throw new IOException("Unable to create test run directory: " + executeLog.getParentFile());
            PrintStream log = verbose ? new TeePrintStream(useStdErr ? System.err : System.out, executeLog) : new PrintStream(executeLog);
            log("*** WARNING: RegressionTestAntTask is deprecated.  Please migrate to asset-based test cases and use AutomatedTestAntTask. ***", Project.MSG_ERR);
            run.prepareTest(update, resultDir, true, singleServer, stubbing, log);
            if (verbose)
                log("Test case prepared: " + testCase.getCaseName(), useStdErr ? Project.MSG_ERR : Project.MSG_INFO);

            masterRequestRunMap.put(run.getMasterRequestId(), run);

            threadPool.execute(run);
            updateResults(false);

            try {
                Thread.sleep(intervalSecs * 1000);
            }
            catch (InterruptedException e) {
            }
        }

        log("All cases prepared. Waiting for completion ...", Project.MSG_INFO);
        threadPool.shutdown();
        while (!threadPool.isTerminated()) {
            updateResults(false);
            try {
                Thread.sleep(2500);
            }
            catch (InterruptedException e) {
            }
        }

        if (monitor != null)
            monitor.shutdown();
        updateResults(true);
        log("All cases completed.", Project.MSG_ERR);
    }

    private synchronized void updateResults(boolean done) throws IOException {
        int completedCases = 0;
        int erroredCases = 0;
        int failedCases = 0;
        for (TestCase testCase : testCases) {
            String oldStatus = testCaseStatuses.get(testCase.getCaseName());
            boolean statusChanged = oldStatus == null || !oldStatus.equals(testCase.getStatus());
            testCaseStatuses.put(testCase.getCaseName(), testCase.getStatus());

            if (isErrored(testCase)) {
                if (statusChanged) {
                    log("Test case errored: " + testCase.getCaseName(), Project.MSG_ERR);
                    log("   ---> " + testCase.getMessage(), Project.MSG_ERR);
                    if (testResultsBaseUrl != null)
                        log("   ---> " + testResultsBaseUrl + "/" + testCase.getCaseName(), Project.MSG_ERR);
                }
                erroredCases++;
            }
            else if (isFailed(testCase)) {
                if (statusChanged) {
                    log("Test case failed: " + testCase.getCaseName(), Project.MSG_ERR);
                    log("   ---> " + testCase.getMessage(), Project.MSG_ERR);
                    if (testResultsBaseUrl != null)
                        log("   ---> " + testResultsBaseUrl + "/" + testCase.getCaseName(), Project.MSG_ERR);
                }
                failedCases++;
            }
            if (isFinished(testCase)) {
                if (statusChanged && !isErrored(testCase) && !isFailed(testCase))
                    log("Test case passed: " + testCase.getCaseName(), Project.MSG_ERR);

                completedCases++;
            }

            writeTestCaseResults(testCase);
        }

        if (done) {
            log("completed: " + completedCases + "   errored: " + erroredCases + "   failed: " + failedCases, Project.MSG_ERR);
            if (testCases.size() != 0) {
                float bad = erroredCases + failedCases;
                float good = testCases.size() - bad;
                log("percent success: " + Math.round((good/(good + bad))*100), Project.MSG_ERR);
                if (bad > 0 && failOnError)
                    throw new BuildException((int)bad + " Failed or Errored test cases");
            }
            else {
                log("No test cases executed", Project.MSG_ERR);
            }
        }
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
            results.append("name=\"").append(testCase.getCaseName()).append("\" ");
            Date start = testCase.getStartDate();
            if (start != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(start);
                results.append("timestamp=\"").append(DatatypeConverter.printDateTime(startCal))
                        .append("\" ");
                Date end = testCase.getEndDate();
                if (end != null) {
                    long ms = end.getTime() - start.getTime();
                    results.append("time=\"").append(ms / 1000).append("\" ");
                }
            }
            if (isFinished(testCase))
                completed++;
            if (isErrored(testCase)) {
                errors++;
                results.append(">\n");
                results.append("    <error ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("/>\n");
                results.append("  </testcase>\n");
            }
            else if (isFailed(testCase)) {
                failures++;
                results.append(">\n");
                results.append("    <failure ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("/>\n");
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

        writeFile(testResultsSummaryFile, suiteBuf.toString().getBytes());
    }

    public boolean isErrored(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_ERROR);
    }

    public boolean isFailed(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_FAIL);
    }

    public boolean isSuccess(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_PASS);
    }

    public boolean isRunning(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_RUNNING);
    }

    public boolean isWaiting(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_WAITING);
    }

    public boolean isStopped(TestCase testCase) {
        return testCase.getStatus().equals(TestCase.STATUS_STOP);
    }

    public boolean isFinished(TestCase testCase) {
        return !isWaiting(testCase) && !isRunning(testCase)
                && !testCase.getStatus().equals(TestCase.STATUS_NOT_RUN);
    }

    public void writeFile(File file, byte[] contents) throws IOException {

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
