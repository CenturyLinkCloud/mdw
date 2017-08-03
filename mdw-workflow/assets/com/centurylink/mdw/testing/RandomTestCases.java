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
package com.centurylink.mdw.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.workflow.RoundRobinScheduledJob;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Run random test cases to generate data for reports
 */
@RegisteredService(ScheduledJob.class)
public class RandomTestCases extends RoundRobinScheduledJob {

    private StandardLogger logger;

    /**
     * Default Constructor
     */
    public RandomTestCases() {
    }

    /**
     * Method that gets invoked periodically by the container
     *
     */
    public void run(CallURL args) {
        logger = LoggerUtil.getStandardLogger();
        logger.info("methodEntry-->RandomTestCases.run()");
        runRandomTests();
        logger.info("methodExit-->RandomTestCases.run()");
    }

    public TestCaseList getTestCaseList(List<TestCase> shuffledList) throws ServiceException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        TestCaseList testCaseList = new TestCaseList(assetServices.getAssetRoot());
        testCaseList.setPackageTests(new ArrayList<PackageTests>());
        List<TestCase> allTests = new ArrayList<TestCase>();

        for (TestCase testCase : shuffledList) {
            if (testCaseList.getPackageTests(testCase.getPackage()) == null) {
                PackageTests pkgTests = new PackageTests(assetServices.getPackage(testCase.getPackage()));
                pkgTests.setTestCases(new ArrayList<TestCase>());
                pkgTests.getTestCases().add(testCase);
                testCaseList.addPackageTests(pkgTests);
            }
            else {
                testCaseList.addTestCase(testCase);
            }
            allTests.add(testCase);
        }
        testCaseList.setCount(allTests.size());

        // sort
        testCaseList.sort();
        return testCaseList;
    }

    public static void main (String args[]) throws Exception
    {
        RandomTestCases me = new RandomTestCases();
        me.runRandomTests();
    }

    private void runRandomTests() {
        TestingServices testingServices = ServiceLocator.getTestingServices();
        try {
            TestCaseList testList  = testingServices.getTestCases();
            List<TestCase> shuffledList = testList.getTestCases();
            // grab some random tests
            Collections.shuffle(shuffledList);
            int randomHourTestCount = 10;
            if (randomHourTestCount < shuffledList.size())
                shuffledList = shuffledList.subList(0, randomHourTestCount);

            TestExecConfig execConfig = new TestExecConfig(System.getProperties());
            execConfig.setStubbing(true);
            testingServices.executeCases(this.getTestCaseList(shuffledList), "mdwapp", execConfig) ;
            logger.info("Running [" + shuffledList.size() + "] random tests");
        }
        catch (Exception e) {
            logger.info("Exception" + e);
        }
    }
}
