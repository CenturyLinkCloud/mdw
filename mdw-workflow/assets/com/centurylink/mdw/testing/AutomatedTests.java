/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCase;

@RegisteredService(JsonService.class)
public class AutomatedTests implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> headers) throws ServiceException {
        TestingServices testServices = ServiceLocator.getTestingServices();
        String testCaseName = headers.get(Listener.METAINFO_RESOURCE_SUBPATH);
        try {
            if (testCaseName == null) {
                return testServices.getTestCases().getJson().toString(2);
            }
            else {
                TestCase testCase = testServices.getTestCase(testCaseName);
                if (testCase == null)
                    throw new ServiceException(404, "Test case not found: " + testCaseName);
                return testCase.getJson().toString(2);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> headers) throws ServiceException {
        return getJson(parameters, headers);
    }
}
