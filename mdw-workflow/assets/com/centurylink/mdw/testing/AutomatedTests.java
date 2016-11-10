/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.test.TestCase;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/AutomatedTests")
@Api("Automated tests support")
public class AutomatedTests extends JsonRestService {

    @Path("/{testCase}")
    @ApiOperation(value="If {testCase} asset path not specified, returns all cases",
        response=TestCase.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        TestingServices testServices = ServiceLocator.getTestingServices();
        String[] segments = getSegments(path);
        if (segments.length == 7) {
            String testCasePath = segments[5] + '/' + segments[6];
            TestCase testCase = testServices.getTestCase(testCasePath);
            if (testCase == null)
                throw new ServiceException(404, "Test case not found: " + testCasePath);
            return testCase.getJson();
        }
        else {
            return testServices.getTestCases().getJson();
        }
    }
}
