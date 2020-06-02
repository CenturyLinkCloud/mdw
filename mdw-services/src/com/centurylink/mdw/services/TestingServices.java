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
package com.centurylink.mdw.services;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseItem;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;

public interface TestingServices {

    /**
     * Returns the list of workflow test cases with their status information.
     */
    TestCaseList getTestCases() throws ServiceException;

    /**
     * Returns test cases for a specifics formats (eg: Groovy, Cucumber or JS).
     */
    TestCaseList getTestCases(String[] formats) throws ServiceException;

    /**
     * Returns a single test case with it's detailed information.
     */
    TestCase getTestCase(String path) throws ServiceException;
    TestCaseItem getTestCaseItem(String path) throws ServiceException;

    /**
     * Build a TestCaseList which includes single test.
     */
    TestCaseList getTestCaseList(TestCase testCase) throws ServiceException;

    /**
     * @return the test results root dir (testResults sibling to cases dir by default)
     * @throws IOException if the results dir does not exist and cannot be created
     */
    File getTestResultsDir() throws IOException;
    File getTestResultsFile() throws IOException;

    /**
     * Asynchronously executes a single test.  Call getTestCase() for status.
     */
    void executeCase(TestCase testCase, String user, TestExecConfig config) throws ServiceException, IOException;

    /**
     * Asynchronously executes list of tests.  Call getTestCases() for status.
     * Only one automated test execution can be run in a JVM.
     */
    void executeCases(TestCaseList testCaseList, String user, TestExecConfig config) throws ServiceException, IOException;

    /**
     * Cancel currently running tests.
     */
    void cancelTestExecution(String user) throws ServiceException;

    TestExecConfig getTestExecConfig() throws ServiceException;
    void setTestExecConfig(TestExecConfig config) throws ServiceException;
}
