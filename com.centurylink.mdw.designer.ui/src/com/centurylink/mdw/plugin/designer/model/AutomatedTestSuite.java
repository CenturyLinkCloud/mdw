/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestResultsParser;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.test.AssetInfo;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCaseList;

public class AutomatedTestSuite extends WorkflowElement {
    public Entity getActionEntity() {
        return Entity.Folder;
    }

    private boolean loadTest;

    public boolean isLoadTest() {
        return loadTest;
    }

    public void setLoadTest(boolean loadTest) {
        this.loadTest = loadTest;
    }

    private int threadCount;

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    private int runCount;

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    private int threadInterval;

    public int getThreadInterval() {
        return threadInterval;
    }

    public void setThreadInterval(int threadInterval) {
        this.threadInterval = threadInterval;
    }

    private boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private boolean stubbing;

    public boolean isStubbing() {
        return stubbing;
    }

    public void setStubbing(boolean stubbing) {
        this.stubbing = stubbing;
    }

    private boolean singleServer;

    public boolean isSingleServer() {
        return singleServer;
    }

    public void setSingleServer(boolean singleServer) {
        this.singleServer = singleServer;
    }

    private boolean createReplaceResults;

    public boolean isCreateReplaceResults() {
        return createReplaceResults;
    }

    public void setCreateReplaceResults(boolean createReplace) {
        this.createReplaceResults = createReplace;
    }

    private boolean debug;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private List<AutomatedTestCase> testCases;

    public List<AutomatedTestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<AutomatedTestCase> cases) {
        this.testCases = cases;
    }

    public List<String> getTestCaseStringList() {
        List<String> lst = new ArrayList<String>();
        for (AutomatedTestCase testCase : getTestCases())
            lst.add(testCase.getPath());
        return lst;
    }

    public File getResultsDir() {
        return getProject().getTestResultsDir(
                isLoadTest() ? AutomatedTestCase.LOAD_TEST : AutomatedTestCase.FUNCTION_TEST);
    }

    public AutomatedTestCase getTestCase(String path) {
        for (AutomatedTestCase testCase : testCases) {
            if (testCase.getPath().equals(path))
                return testCase;
        }
        return null;
    }

    private boolean running;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;

        String suiteStatus = TestCase.STATUS_NOT_RUN;
        if (running) {
            suiteStatus = TestCase.STATUS_RUNNING;
        }
        else {
            // find the most severe test case status
            for (AutomatedTestCase regrTestCase : testCases) {
                if (regrTestCase.isErrored()) {
                    suiteStatus = TestCase.STATUS_ERROR;
                    break;
                }
                else if (regrTestCase.isFailed()) {
                    if (!suiteStatus.equals(TestCase.STATUS_ERROR))
                        suiteStatus = TestCase.STATUS_FAIL;
                }
                else if (regrTestCase.isSuccess()) {
                    if (!suiteStatus.equals(TestCase.STATUS_ERROR)
                            && !suiteStatus.equals(TestCase.STATUS_FAIL))
                        suiteStatus = TestCase.STATUS_PASS;
                }
            }
        }

        fireElementChangeEvent(ChangeType.STATUS_CHANGE, suiteStatus);
    }

    public AutomatedTestSuite(WorkflowProject workflowProject) {
        setProject(workflowProject);
    }

    public boolean hasErrors() {
        for (AutomatedTestCase testCase : testCases) {
            if (testCase.isErrored())
                return true;
        }
        return false;
    }

    public boolean hasFailures() {
        for (AutomatedTestCase testCase : testCases) {
            if (testCase.isFailed())
                return true;
        }
        return false;
    }

    public boolean isSuccess() {
        for (AutomatedTestCase testCase : testCases) {
            if (!testCase.isSuccess())
                return false;
        }
        return true;
    }

    public boolean isStopped() {
        for (AutomatedTestCase testCase : testCases) {
            if (testCase.isStopped())
                return true;
        }
        return false;
    }

    public boolean isFinished() {
        for (AutomatedTestCase testCase : testCases) {
            if (!testCase.isFinished())
                return false;
        }
        return true;
    }

    @Override
    public String getTitle() {
        return "Test Suite";
    }

    @Override
    public Long getId() {
        return new Long(-1); // TODO
    }

    @Override
    public String getName() {
        return getProject().getLabel();
    }

    private String label;

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        if (label == null)
            return getName() + " " + getTitle();
        else
            return label;
    }

    @Override
    public String getPath() {
        String path = getProjectPrefix();
        if (getProject() != null)
            path += "Tests/";
        return path;
    }

    private String icon = "tsuite.gif";

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        if (icon.equals("tsuite.gif")) {
            if (hasErrors())
                return "tsuiteerror.gif";
            else if (hasFailures())
                return "tsuitefail.gif";
            else if (isRunning())
                return "tsuiterun.gif";
            else if (isSuccess())
                return "tsuiteok.gif";
            else
                return icon;
        }
        else {
            return icon;
        }
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    public boolean readLegacyCases() {
        testCases = new ArrayList<AutomatedTestCase>();

        File oldTestCasesDir = getProject().getOldTestCasesDir();
        if (oldTestCasesDir.exists() && oldTestCasesDir.isDirectory()) {
            File[] caseDirs = oldTestCasesDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (!file.isDirectory())
                        return false;
                    if (file.getName().equalsIgnoreCase("CVS")
                            || file.getName().equals(".metadata"))
                        return false;
                    for (String commandsFileName : TestCase.LEGACY_TEST_CASE_FILENAMES.values()) {
                        if (new File(file.toString() + "/" + commandsFileName).exists())
                            return true;
                    }
                    return false;
                }
            });

            List<TestCase> cases = new ArrayList<TestCase>();
            for (File caseDir : caseDirs) {
                AutomatedTestCase testCase = new AutomatedTestCase(getProject(), this,
                        new TestCase("Legacy", caseDir));
                testCases.add(testCase);
                cases.add(testCase.getTestCase());
            }

            // update case statuses
            File suiteResults = getProject().getFunctionTestResultsFile();
            if (suiteResults != null && suiteResults.exists()) {
                try {
                    TestResultsParser parser = new TestResultsParser(suiteResults, cases);
                    if (suiteResults.getName().endsWith(".xml"))
                        parser.parseXml();
                    else
                        parser.parseJson(getProject().getAssetDir());
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                }
            }

            return true;
        }
        return false;
    }

    public void clearCases() {
        for (AutomatedTestCase testCase : testCases)
            testCase.clear();
    }

    @Override
    public boolean isEmpty() {
        return testCases == null || testCases.isEmpty();
    }

    public boolean equals(Object o) {
        if (!(o instanceof AutomatedTestSuite) || o == null)
            return false;
        AutomatedTestSuite other = (AutomatedTestSuite) o;

        if (!getProject().equals(other.getProject()))
            return false;

        return getName().equals(other.getName());
    }

    public void writeTestCaseResults(AutomatedTestCase exeTestCase) {
        File resultsFile = getProject().getFunctionTestResultsFile();
        try {
            if (resultsFile.getName().endsWith(".json"))
                writeTestResults(exeTestCase);
            else if (resultsFile.getName().endsWith(".xml"))
                writeTestCaseResultsXml(exeTestCase);
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
        }
    }

    public void writeTestCaseResultsXml(AutomatedTestCase exeTestCase) {
        int errors = 0;
        int failures = 0;
        int completed = 0;

        StringBuffer suiteBuf = new StringBuffer();
        suiteBuf.append("<testsuite ");
        suiteBuf.append("name=\"").append(getProject().getLabel()).append("\" ");
        suiteBuf.append("tests=\"").append(getProject().getTestCases().size()).append("\" ");

        StringBuffer results = new StringBuffer();
        for (AutomatedTestCase testCase : getProject().getTestCases()) {
            if (exeTestCase.getName().equals(testCase.getName()))
                getProject().fireTestCaseStatusChange(testCase, testCase.getStatus());

            results.append("  <testcase ");
            results.append("name=\"").append(testCase.getPath()).append("\" ");
            Date start = testCase.getStartTime();
            if (start != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(start);
                results.append("timestamp=\"").append(DatatypeConverter.printDateTime(startCal))
                        .append("\" ");
                Date end = testCase.getEndTime();
                if (end != null) {
                    long ms = end.getTime() - start.getTime();
                    results.append("time=\"").append(ms / 1000).append("\" ");
                }
            }
            if (testCase.isFinished())
                completed++;
            if (testCase.isErrored()) {
                errors++;
                results.append(">\n");
                results.append("    <error ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()).append("\" ");
                results.append("/>\n");
                results.append("  </testcase>\n");
            }
            else if (testCase.isFailed()) {
                failures++;
                results.append(">\n");
                results.append("    <failure ");
                if (testCase.getMessage() != null)
                    results.append("message=\"").append(testCase.getMessage()
                            .replaceAll("\"", "&quot;").replaceAll("\n", "&#10;")).append("\" ");
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

        try {
            PluginUtil.writeFile(getProject().getFunctionTestResultsFile(),
                    suiteBuf.toString().getBytes());
        }
        catch (IOException ex) {
            PluginMessages.log(ex);
        }
    }

    public void writeTestResults(AutomatedTestCase exeTestCase) throws JSONException, IOException {
        String jsonString = null;
        File resultsFile = getProject().getFunctionTestResultsFile();
        TestCaseList testCaseList = null;
        if (resultsFile.exists())
            jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        if (jsonString != null && !jsonString.isEmpty())
            testCaseList = new TestCaseList(getProject().getAssetDir(), new JSONObject(jsonString));
        if (testCaseList == null) {
            testCaseList = new TestCaseList(getProject().getAssetDir());
            testCaseList.setPackageTests(new ArrayList<PackageTests>());
        }
        for (WorkflowPackage pkg : getProject().getTopLevelUserVisiblePackages()) {
            if (pkg.getTestCases() != null && !pkg.getTestCases().isEmpty()) {
                PackageTests pkgTests = testCaseList.getPackageTests(pkg.getName());
                for (AutomatedTestCase autoTestCase : pkg.getTestCases()) {
                    com.centurylink.mdw.test.TestCase testCase = testCaseList
                            .getTestCase(exeTestCase.getPath());
                    if (testCase == null)
                        testCase = new com.centurylink.mdw.test.TestCase(pkg.getName(),
                                new AssetInfo(autoTestCase.getRawFile()));
                    if (pkgTests == null) {
                        PackageDir pkgDir = new PackageDir(getProject().getAssetDir(),
                                pkg.getPackageVO(), null);
                        pkgTests = new PackageTests(pkgDir);
                        pkgTests.setTestCases(new ArrayList<com.centurylink.mdw.test.TestCase>());
                    }
                    if (testCase.getPackage().equals(pkg.getName())
                            && !pkgTests.getTestCases().contains(testCase))
                        pkgTests.getTestCases().add(testCase);
                    if (exeTestCase.getPath().equals(autoTestCase.getPath())) {
                        getProject().fireTestCaseStatusChange(autoTestCase,
                                autoTestCase.getStatus());
                        testCase.setStatus(autoTestCase.getCaseStatus());
                        testCase.setStart(exeTestCase.getStartTime());
                        testCase.setEnd(exeTestCase.getEndTime());
                        testCase.setMessage(exeTestCase.getMessage());
                    }
                }
                if (!testCaseList.getPackageTests().contains(pkgTests))
                    testCaseList.getPackageTests().add(pkgTests);
            }
        }
        testCaseList.setCount(testCaseList.getTestCases().size());
        PluginUtil.writeFile(resultsFile, testCaseList.getJson().toString(2).getBytes());
    }

    public void writeLoadTestResults(int totalPrepared, int totalCompleted,
            int totalActivityStarted, int totalActivityCompleted, String finalStartTime,
            String finalEndTime, int totalProcessStarted, double speed, String resultDirectory) {
        StringBuffer suiteBuf = new StringBuffer();
        suiteBuf.append("<LoadTestResults>\n");
        suiteBuf.append(" <testsuite ");
        suiteBuf.append("name=\"").append(getProject().getLabel()).append("\" ");
        suiteBuf.append("tests=\"").append(getProject().getTestCases().size()).append("\" ");
        suiteBuf.append(">\n");

        StringBuffer results = new StringBuffer();
        results.append("  <CasesPrepared>").append(totalPrepared).append("</CasesPrepared>\n");
        results.append("  <CasesCompleted>").append(totalCompleted).append("</CasesCompleted>\n");
        results.append("  <ProcessesStarted>").append(totalProcessStarted)
                .append("</ProcessesStarted>\n");
        results.append("  <ActivitiesStarted>").append(totalActivityStarted)
                .append("</ActivitiesStarted>\n");
        results.append("  <ActivitiesCompleted>").append(totalActivityCompleted)
                .append("</ActivitiesCompleted>\n");
        results.append("  <StartTime>").append(finalStartTime).append("</StartTime>\n");
        results.append("  <EndTime>").append(finalEndTime).append("</EndTime>\n");
        results.append("  <ActivitiesPerHour>").append(speed).append("</ActivitiesPerHour>\n");

        suiteBuf.append(results);
        suiteBuf.append(" </testsuite>\n");
        suiteBuf.append("</LoadTestResults>");

        try {
            PluginUtil.writeFile(getProject().getLoadTestResultsFile(),
                    suiteBuf.toString().getBytes());
        }
        catch (IOException ex) {
            PluginMessages.log(ex);
        }
    }
}
