/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
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