/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.test.TestCase.Status;

public class TestCaseMain {
    /**
     * Execute a test case in standalone mode (for Designer debug).
     * Takes a single argument: testCasePath.
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
            throw new IllegalStateException("Missing system property: mdw.asset.root");

        TestingServices testingServices = ServiceLocator.getTestingServices();
        TestCase testCase = testingServices.getTestCase(testCasePath);

        File resultsFile = testingServices.getTestResultsFile(null);

        // TODO
        LogMessageMonitor monitor = null;
        Map<String,Process> processCache = new HashMap<>();
        TestCaseRun run = new TestCaseRun(testCase, user, resultsFile.getParentFile(), 0, masterRequestId, monitor, processCache, execConfig);
        try {
            run.runStandalone();
            if (run.getTestCase().getStatus() == Status.Passed)
                System.exit(0);
            else
                System.exit(1); // fail

        }
        catch (Throwable th) {
            th.printStackTrace(run.getLog());
            throw th;
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
