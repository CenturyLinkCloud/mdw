/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.views.AutomatedTestView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutomatedTestLaunchConfiguration extends LaunchConfigurationDelegate {
    public static final String WORKFLOW_PROJECT = "workflowProject";
    public static final String WORKFLOW_PACKAGE = "workflowPackage";
    public static final String IS_LEGACY_LAUNCH = "isLegacyLaunch";
    public static final String RESULTS_PATH = "resultsPath";
    public static final String THREAD_COUNT = "threadCount";
    public static final String THREAD_INTERVAL = "threadInterval";
    public static final String RUN_COUNT = "runCount";
    public static final String RUN_INTERVAL = "runInterval";
    public static final String TEST_CASES = "testCases";
    public static final String VERBOSE = "verbose";
    public static final String STUBBING = "stubbing";
    public static final String SINGLE_SERVER = "singleServer";
    public static final String CREATE_REPLACE_RESULTS = "createReplaceResults";
    public static final String IS_LOAD_TEST = "isLoadTest";
    public static final String TESTCASE_COUNTS_MAP = "testCaseCountsMap";
    public static final String DEBUG = "debug";

    private AutomatedTestSuite testSuite;

    public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                .getWorkflowProject(launchConfig.getAttribute(WORKFLOW_PROJECT, ""));

        boolean isLoadTest = launchConfig.getAttribute(IS_LOAD_TEST, false);
        String attrPrefix = isLoadTest ? AutomatedTestCase.LOAD_TEST
                : AutomatedTestCase.FUNCTION_TEST;

        String resultsPath = launchConfig.getAttribute(attrPrefix + "_" + RESULTS_PATH, "");
        if (resultsPath.isEmpty())
            resultsPath = WorkflowProject.DEFAULT_TEST_RESULTS_PATH;
        workflowProject.setTestResultsPath(attrPrefix, resultsPath);

        int threadCount = launchConfig.getAttribute(attrPrefix + "_" + THREAD_COUNT, 5);

        int runCount = launchConfig.getAttribute(attrPrefix + "_" + RUN_COUNT, 100);
        int threadInterval = launchConfig.getAttribute(attrPrefix + "_" + THREAD_INTERVAL, 2);
        int runInterval = launchConfig.getAttribute(attrPrefix + "_" + RUN_INTERVAL, 2);
        boolean verbose = launchConfig.getAttribute(attrPrefix + "_" + VERBOSE, true);
        boolean stubbing = launchConfig.getAttribute(attrPrefix + "_" + STUBBING, false);
        boolean singleServer = launchConfig.getAttribute(attrPrefix + "_" + SINGLE_SERVER, false);
        boolean createReplace = launchConfig.getAttribute(attrPrefix + "_" + CREATE_REPLACE_RESULTS,
                false);
        boolean debug = launchConfig.getAttribute(attrPrefix + "_" + DEBUG, false);

        testSuite = new AutomatedTestSuite(workflowProject);
        testSuite.setLoadTest(isLoadTest);
        testSuite.setThreadCount(threadCount);
        testSuite.setRunCount(runCount);
        if (isLoadTest)
            testSuite.setThreadInterval(runInterval);
        else
            testSuite.setThreadInterval(threadInterval);
        testSuite.setVerbose(verbose);
        testSuite.setStubbing(stubbing);
        testSuite.setSingleServer(singleServer);
        testSuite.setCreateReplaceResults(createReplace);
        testSuite.setDebug(debug);

        List<AutomatedTestCase> testCases = new ArrayList<AutomatedTestCase>();
        List<String> testCasesStr = launchConfig.getAttribute(attrPrefix + "_" + TEST_CASES,
                new ArrayList<String>());
        for (String testCaseStr : testCasesStr) {
            AutomatedTestCase autoTestCase;
            if (testCaseStr.startsWith("Legacy/"))
                autoTestCase = workflowProject.getLegacyTestSuite().getTestCase(testCaseStr);
            else
                autoTestCase = (AutomatedTestCase) workflowProject.getAsset(testCaseStr);

            autoTestCase.setTestSuite(testSuite);
            if (!autoTestCase.getResultsDir().exists())
                autoTestCase.getResultsDir().mkdirs();
            testCases.add(autoTestCase);
        }
        testSuite.setTestCases(testCases);

        if (isLoadTest) {
            Map<String, String> testCaseCounts = launchConfig.getAttribute(
                    attrPrefix + "_" + TESTCASE_COUNTS_MAP, new HashMap<String, String>());
            for (String name : testCaseCounts.keySet()) {
                int count = Integer.parseInt(testCaseCounts.get(name));
                testSuite.getTestCase(name).setRunCount(count);
            }
        }

        if (!testSuite.getResultsDir().exists())
            testSuite.getResultsDir().mkdirs();

        showResultsView();
    }

    private void showResultsView() {
        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
                try {
                    IViewPart viewPart = page.showView("mdw.views.launch.automatedTest");
                    if (viewPart != null) {
                        AutomatedTestView testView = (AutomatedTestView) viewPart;
                        if (testView.isLocked()) {
                            MessageDialog.openError(page.getActivePart().getSite().getShell(),
                                    "Test Exec",
                                    "A test appears to be already running. Please stop the current test or wait for it to complete before launching another one.");
                            return;
                        }
                        testView.setTestSuite(testSuite);
                        if (testSuite.isLoadTest())
                            testView.runLoadTests();
                        else
                            testView.runTests();
                    }
                }
                catch (PartInitException ex) {
                    PluginMessages.uiError(ex, "Test Results", testSuite.getProject());
                }
            }
        });
    }
}
