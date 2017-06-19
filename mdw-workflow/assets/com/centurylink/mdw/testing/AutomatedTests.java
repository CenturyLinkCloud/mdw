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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseItem;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/AutomatedTests")
@Api("Automated tests support")
public class AutomatedTests extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Path("/{testCase}/{item}")
    @ApiOperation(value="If {testCase} asset path not specified, returns all cases",
        notes="{item} can be a test case item like from a postman collection",
        response=TestCase.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        // results file resides on master instance (but avoid loop)
        if (ApplicationContext.isMasterServer() || headers.get(Listener.METAINFO_MASTER_OP) != null) {
            String[] segments = getSegments(path);
            if (segments.length == 6) {
                if ("config".equals(segments[5]))
                    return ServiceLocator.getTestingServices().getTestExecConfig().getJson();
                else
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
            }
            TestCaseItem item = getTestCaseItem(segments);
            if (item != null) {
                return item.getJson();
            }
            TestCase singleCase = getTestCase(segments);
            if (singleCase != null) {
                return singleCase.getJson();
            }
            return ServiceLocator.getTestingServices().getTestCases().getJson();
        }
        else {
            return masterServerGet(path);
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
        // results file resides on master instance
        if (ApplicationContext.isMasterServer() || headers.get(Listener.METAINFO_MASTER_OP) != null) {
            TestingServices testingServices = ServiceLocator.getTestingServices();
            TestExecConfig config = testingServices.getTestExecConfig();
            String user = getAuthUser(headers);
            String[] segments = getSegments(path);
            if (segments[5].equals("cancel")) {
                testingServices.cancelTestExecution(user);
            }
            else {
                try {
                    TestCase singleCase = getTestCase(segments);
                    if (singleCase != null)
                        testingServices.executeCase(singleCase, user, config);
                    else
                        testingServices.executeCases(new TestCaseList(ApplicationContext.getAssetRoot(), content), user, config);
                }
                catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
                }
            }
            return null;
        }
        else {
            return masterServerPost(path, content);
        }
    }

    private TestCase getTestCase(String[] segments) throws ServiceException {
        if (segments.length < 7)
            return null;
        String testCasePath = segments[5] + '/' + segments[6];
        TestCase testCase = ServiceLocator.getTestingServices().getTestCase(testCasePath);
        if (testCase == null)
            throw new ServiceException(404, "Test case not found: " + testCasePath);
        if (segments.length == 8)
            testCase.addItem(getTestCaseItem(segments));

        return testCase;
    }

    private TestCaseItem getTestCaseItem(String[] segments) throws ServiceException {
        if (segments.length != 8)
            return null;
        String testCaseItemPath = segments[5] + '/' + segments[6] + '/' + segments[7];
        TestCaseItem item = ServiceLocator.getTestingServices().getTestCaseItem(testCaseItemPath);
        if (item == null)
            throw new ServiceException(404, "Test case item not found: " + testCaseItemPath);
        return item;
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
        if (ApplicationContext.isMasterServer() || headers.get(Listener.METAINFO_MASTER_OP) != null) {
            TestingServices testingServices = ServiceLocator.getTestingServices();
            testingServices.setTestExecConfig(new TestExecConfig(content));
            return null;
        }
        else {
            return masterServerPut(path, content);
        }
    }
}
