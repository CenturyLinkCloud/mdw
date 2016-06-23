/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.io.File;
import java.io.IOException;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;

public interface TestingServices {

    /**
     * Returns the list of workflow test cases with their status information.
     */
    public TestCaseList getTestCases() throws ServiceException;

    /**
     * Returns test cases for a specific format (eg: Groovy or Cucumber).
     */
    public TestCaseList getTestCases(String format) throws ServiceException;

    /**
     * Returns a single test case with it's detailed information.
     */
    public TestCase getTestCase(String path) throws ServiceException;

    /**
     * @return the test results root dir (testResults sibling to cases dir by default)
     * @throws IOException if the results dir does not exist and cannot be created
     */
    public File getTestResultsDir() throws IOException;

}
