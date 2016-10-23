/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.rest;

import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.test.MockRuntimeContext;

/**
 * Unit test for REST service adapter activity.
 */
public class RestfulServiceAdapterTest {

    static final String GET_RESPONSE = "Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.";

    MockRuntimeContext runtimeContext;
    RestfulServiceAdapter restAdapterActivity;

    @Before
    public void setup() {
        runtimeContext = new MockRuntimeContext("REST Adapter");
        runtimeContext.getAttributes().put(RestfulServiceAdapter.HTTP_METHOD, "GET");
        runtimeContext.setAdapterStubbedResponse(GET_RESPONSE);

        runtimeContext.getAttributes().put("RESPONSE_VARIABLE", "getResponse");
        runtimeContext.getVariables().put("getResponse", "");

        restAdapterActivity = new RestfulServiceAdapter();
        restAdapterActivity.prepare(runtimeContext);
    }

    @Test
    public void testGet() throws ActivityException {
        Object res = restAdapterActivity.execute(runtimeContext);
        assertTrue(res == null);
        runtimeContext = (MockRuntimeContext)restAdapterActivity.getRuntimeContext();
        assertTrue(runtimeContext.getVariables().get("getResponse").equals(GET_RESPONSE));
    }
}
