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
package com.centurylink.mdw.plugin.designer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.test.TestCase.Status;

public class AutomatedTestCase extends WorkflowAsset {
    public static final String LOAD_TEST = "loadTest";
    public static final String FUNCTION_TEST = "functionTest";

    private boolean legacy;

    public boolean isLegacy() {
        return legacy;
    }

    private TestCase testCase;

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;

        if (isLegacy()) {
            legacyExpectedResults = new ArrayList<LegacyExpectedResults>();
            files = new ArrayList<com.centurylink.mdw.plugin.designer.model.File>();

            File testCaseDir = new File(getProject().getOldTestCasesDir() + "/" + getName());
            if (testCaseDir != null) {
                File[] testCaseFiles = testCaseDir.listFiles();
                if (testCaseFiles != null) {
                    for (File file : testCaseDir.listFiles()) {
                        if (file.getName().startsWith("E_"))
                            legacyExpectedResults.add(new LegacyExpectedResults(this, file));
                        else if (!TestCase.LEGACY_TEST_CASE_FILENAMES.values()
                                .contains(file.getName()) && !file.isDirectory()) {
                            IFolder testCaseFolder = getProject().getOldTestCasesFolder()
                                    .getFolder(getName());
                            files.add(new com.centurylink.mdw.plugin.designer.model.File(
                                    getProject(), this, testCaseFolder.getFile(file.getName())));
                        }
                    }
                    Collections.sort(legacyExpectedResults);
                    Collections.sort(files);
                }
            }
        }
    }

    private AutomatedTestSuite testSuite;

    public AutomatedTestSuite getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(AutomatedTestSuite suite) {
        this.testSuite = suite;
    }

    private int runCount;

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int count) {
        this.runCount = count;
    }

    private List<LegacyExpectedResults> legacyExpectedResults;

    public List<LegacyExpectedResults> getLegacyExpectedResults() {
        return legacyExpectedResults;
    }

    /**
     * Relies on the convention that results have the same name and reside in
     * the same workflow package as the test case asset.
     */
    public AutomatedTestResults getExpectedResults() {
        String assetName = testCase.getName() + RuleSetVO.getFileExtension(RuleSetVO.YAML);
        return (AutomatedTestResults) getProject()
                .getAsset(getPackage().getName() + "/" + assetName);
    }

    private List<com.centurylink.mdw.plugin.designer.model.File> files = new ArrayList<com.centurylink.mdw.plugin.designer.model.File>();

    public List<com.centurylink.mdw.plugin.designer.model.File> getFiles() {
        return files;
    }

    public Entity getActionEntity() {
        return Entity.TestCase;
    }

    /**
     * Constructor for wizard
     */
    public AutomatedTestCase() {
        this.testCase = new TestCase();
    }

    public AutomatedTestCase(AutomatedTestCase cloneFrom) {
        super(cloneFrom);
        this.testCase = new TestCase(cloneFrom.testCase);
    }

    /**
     * Constructor for new-style workflow asset.
     */
    public AutomatedTestCase(RuleSetVO ruleSet, WorkflowPackage workflowPackage,
            TestCase testCase) {
        super(ruleSet, workflowPackage);
        setTestCase(testCase);
    }

    /**
     * Old-style legacy tests.
     */
    public AutomatedTestCase(WorkflowProject workflowProject, AutomatedTestSuite testSuite,
            TestCase testCase) {
        legacy = true;
        setProject(workflowProject);
        this.testSuite = testSuite;
        setTestCase(testCase);
    }

    public String getMasterRequestId() {
        return testCase.getMasterRequestId();
    }

    public void setMasterRequestId(String masterRequestId) {
        testCase.setMasterRequestId(masterRequestId);
    }

    public boolean isLoadTest() {
        return testSuite.isLoadTest();
    }

    public String getType() {
        return isLoadTest() ? LOAD_TEST : FUNCTION_TEST;
    }

    public String getLabel() {
        if (isLegacy())
            return getName();
        else
            return super.getLabel();
    }

    /**
     * For test cases, path is used to determine asset location.
     */
    @Override
    public String getPath() {
        if (isLegacy())
            return "Legacy/" + getName();
        else
            return getPackage().getName() + "/" + getName();
    }

    /**
     * Override so display looks correct despite path override.
     */
    @Override
    public String getFullPathLabel() {
        String path = getProjectPrefix();
        if (getProject() != null && !isInDefaultPackage())
            path += getPackage().getName() + "/";
        else
            path += "Legacy Tests/";
        return path + getLabel();
    }

    @Override
    public String getName() {
        if (isLegacy()) {
            if (testCase == null)
                return null;
            return testCase.getCaseName();
        }
        else {
            return super.getName();
        }
    }

    /**
     * not called for legacy
     */
    @Override
    public void setName(String name) {
        super.setName(name);
        if (getProject().isFilePersist())
            testCase.setTestCaseFile(getRawFile());
        else
            testCase.setTestCaseRuleSet(getRuleSetVO());
    }

    public File getResultsDir() {
        if (isLegacy())
            return new File(getProject().getTestResultsDir(getType()) + "/" + getName());
        else
            return new File(
                    getProject().getTestResultsDir(getType()) + "/" + getPackage().getName());
    }

    @Override
    public String getIcon() {
        if (isErrored())
            return "testerr.gif";
        else if (isFailed())
            return "testfail.gif";
        else if (isRunning())
            return "testrun.gif";
        else if (isSuccess())
            return "testok.gif";
        else
            return "test.gif";
    }

    public File getTestCaseDirectory() {
        return testCase.getCaseDirectory();
    }

    public String getLanguage() {
        if (isLegacy())
            return testCase.getLanguage();
        else
            return testCase.getLanguage().equals(TestCase.LANGUAGE_GROOVY) ? RuleSetVO.TEST
                    : RuleSetVO.FEATURE;
    }

    public void setLanguage(String language) {
        super.setLanguage(language);
        if (RuleSetVO.TEST.equals(language))
            testCase.setLanguage(TestCase.LANGUAGE_GROOVY);
        else if (RuleSetVO.FEATURE.equals(language))
            testCase.setLanguage(TestCase.LANGUAGE_GHERKIN);
    }

    public String getStatus() {
        return testCase.getStatus();
    }

    public Status getCaseStatus() {
        return testCase.getCaseStatus();
    }

    public void setStatus(String status) {
        testCase.setStatus(status);
    }

    public void setStatus(Status caseStatus) {
        testCase.setStatus(caseStatus);
    }

    public Date getStartTime() {
        return testCase.getStartDate();
    }

    public void setStartTime(Date start) {
        testCase.setStartDate(start);
    }

    public Date getEndTime() {
        return testCase.getEndDate();
    }

    public void setEndTime(Date end) {
        testCase.setEndDate(end);
    }

    public int getTotalSteps() {
        return testCase.getTotalSteps();
    }

    public int getStepsCompleted() {
        if (testCase.getFirstRun() == null)
            return 0;
        return testCase.getFirstRun().getStepsCompleted();
    }

    public boolean isErrored() {
        return testCase.getStatus().equals(TestCase.STATUS_ERROR);
    }

    public void setErrored() {
        testCase.setStatus(TestCase.STATUS_ERROR);
    }

    public boolean isFailed() {
        return testCase.getStatus().equals(TestCase.STATUS_FAIL);
    }

    public boolean isSuccess() {
        return testCase.getStatus().equals(TestCase.STATUS_PASS);
    }

    public boolean isRunning() {
        return testCase.getStatus().equals(TestCase.STATUS_RUNNING);
    }

    public boolean isWaiting() {
        return testCase.getStatus().equals(TestCase.STATUS_WAITING);
    }

    public boolean isStopped() {
        return testCase.getStatus().equals(TestCase.STATUS_STOP);
    }

    public boolean isFinished() {
        return !isWaiting() && !isRunning()
                && !testCase.getStatus().equals(TestCase.STATUS_NOT_RUN);
    }

    private String message;

    public String getMessage() {
        if (testCase.getMessage() == null)
            return message;
        else
            return testCase.getMessage();
    }

    public void setMessage(String message) {
        this.message = message;
        TestCaseRun firstRun = testCase.getFirstRun();
        if (firstRun != null)
            firstRun.setMessage(message);
    }

    public IFile getCommandsFile() {
        if (isLegacy())
            return getProject().getOldTestCasesFolder().getFolder(getName())
                    .getFile(testCase.getCommandsFileName());
        else
            return getProject().getProjectFolder(getPackage().getName().replace('.', '/'))
                    .getFile(getName());
    }

    public boolean isGroovy() {
        return TestCase.LANGUAGE_GROOVY.equals(getLanguage())
                || RuleSetVO.TEST.equals(getLanguage());
    }

    public boolean isGherkin() {
        return TestCase.LANGUAGE_GHERKIN.equals(getLanguage())
                || RuleSetVO.FEATURE.equals(getLanguage());
    }

    public File getOutputFile() {
        if (getResultsDir() == null)
            return null;
        if (isLegacy())
            return new File(getResultsDir().getPath() + "/execute.log");
        else
            return new File(getResultsDir().getPath() + "/" + testCase.getCaseName() + ".log");
    }

    public IFolder getActualResultsFolder() {
        String resultsDir = getResultsDir().toString();
        String projectDir = getProject().getProjectDir().toString();
        String relativePath = resultsDir.substring(projectDir.length() + 1).replace('\\', '/');
        return getProject().getProjectFolder(relativePath);
    }

    public void clear() {
        testCase.clear();
    }

    @Override
    public String getTitle() {
        return "Test Case";
    }

    public boolean isArchived() {
        return isLegacy() ? false : super.isArchived();
    }

    @Override
    public Long getId() {
        if (isLegacy())
            return new Long(0); // n/a
        else
            return super.getId();
    }

    @Override
    public boolean isReadOnly() {
        if (isLegacy())
            return false;
        else
            return super.isReadOnly();
    }

    public boolean equals(Object o) {
        if (isLegacy()) {
            if (!(o instanceof AutomatedTestCase) || o == null)
                return false;
            AutomatedTestCase other = (AutomatedTestCase) o;
            if (!getProject().equals(other.getProject()))
                return false;
            return getName().equals(other.getName());
        }
        else {
            return super.equals(o);
        }
    }

    @Override
    public String getDefaultExtension() {
        return RuleSetVO.getFileExtension(RuleSetVO.TEST);
    }

    private List<String> testCaseLanguages;

    @Override
    public List<String> getLanguages() {
        if (testCaseLanguages == null) {
            testCaseLanguages = new ArrayList<String>();
            testCaseLanguages.add(RuleSetVO.TEST);
            if (getProject().isFilePersist())
                testCaseLanguages.add(RuleSetVO.FEATURE);
        }
        return testCaseLanguages;
    }
}
