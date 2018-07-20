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
package com.centurylink.mdw.services.test;

import java.util.Map;

import org.json.JSONException;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.model.event.AdapterStubRequest;
import com.centurylink.mdw.model.event.AdapterStubResponse;
import com.centurylink.mdw.model.workflow.ActivityStubRequest;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.services.test.StubServer.Stubber;
import com.centurylink.mdw.test.TestException;

public class TestStubber implements Stubber {

    private Map<String,TestCaseRun> masterRequestRuns;

    public TestStubber(Map<String,TestCaseRun> masterRequestRuns) {
        this.masterRequestRuns = masterRequestRuns;
    }

    public ActivityStubResponse processRequest(ActivityStubRequest request) throws JSONException, TestException {
        TestCaseRun run = masterRequestRuns.get(request.getRuntimeContext().getMasterRequestId());
        if (run == null) {
            ActivityStubResponse activityStubResponse = new ActivityStubResponse();
            activityStubResponse.setPassthrough(true);
            return activityStubResponse;
        }
        return run.getStubResponse(request);
    }

    public AdapterStubResponse processRequest(AdapterStubRequest request) throws JSONException, TestException {
        TestCaseRun run = masterRequestRuns.get(request.getMasterRequestId());
        if (run == null) {
            AdapterStubResponse stubResponse = new AdapterStubResponse(AdapterActivity.MAKE_ACTUAL_CALL);
            stubResponse.setPassthrough(true);
            return stubResponse;
        }
        return run.getStubResponse(request);
    }
}