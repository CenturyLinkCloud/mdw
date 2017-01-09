/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/AutomatedTests")
@Api("Automated tests support")
public class AutomatedTests extends JsonRestService {

    @Path("/{testCase}")
    @ApiOperation(value="If {testCase} asset path not specified, returns all cases",
        response=TestCase.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 6 && "config".equals(segments[5]))
            return ServiceLocator.getTestingServices().getTestExecConfig().getJson();
        TestCase singleCase = getTestCase(segments);
        if (singleCase != null) {
            return singleCase.getJson();
        }
        else {
            return ServiceLocator.getTestingServices().getTestCases().getJson();
        }
    }

    /**
     * For executing test case(s).
     */
    @Override
    @Path("/exec/{testCase}")
    @ApiOperation(value="Executes a test case or a list of cases.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="TestCaseList", paramType="body", dataType="com.centurylink.mdw.test.TestCaseList")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        TestingServices testingServices = ServiceLocator.getTestingServices();
        TestExecConfig config = testingServices.getTestExecConfig();
        String user = getAuthUser(headers);
        String[] segments = getSegments(path);
        if (segments[5].equals("cancel")) {
            testingServices.cancelTestExecution(user);
        }
        else {
            TestCase singleCase = getTestCase(segments);
            try {
                if (singleCase != null) {
                    testingServices.executeCase(singleCase, user, config);
                }
                else {
                    testingServices.executeCases(new TestCaseList(ApplicationContext.getAssetRoot(), content), user, config);
                }
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return null;
    }

    private TestCase getTestCase(String[] segments) throws ServiceException {
        if (segments.length != 7)
            return null;
        String testCasePath = segments[5] + '/' + segments[6];
        TestCase testCase = ServiceLocator.getTestingServices().getTestCase(testCasePath);
        if (testCase == null)
            throw new ServiceException(404, "Test case not found: " + testCasePath);
        return testCase;
    }

    /**
     * Put test exec config.
     */
    @Override
    @Path("/config")
    @ApiOperation(value="Update test exec config.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="TestExecConfig", paramType="body", dataType="com.centurylink.mdw.test.TestExecConfig")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        TestingServices testingServices = ServiceLocator.getTestingServices();
        testingServices.setTestExecConfig(new TestExecConfig(content));
        return null;
    }
}
