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

    static final String GET_RESPONSE = "Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.";

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
