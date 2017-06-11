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
package com.centurylink.mdw.model.workflow;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class ActivityStubRequest implements Jsonable {

    public static final String JSON_NAME = "ActivityStubRequest";

    private ActivityRuntimeContext runtimeContext;
    public ActivityRuntimeContext getRuntimeContext() { return runtimeContext; }
    public void setRuntimeContext(ActivityRuntimeContext runtimeContext) { this.runtimeContext = runtimeContext; }

    public ActivityStubRequest(ActivityRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public ActivityStubRequest(JSONObject json) throws JSONException {
        JSONObject stubRequestJson = json.getJSONObject(JSON_NAME);
        if (stubRequestJson.has("runtimeContext"))
            runtimeContext = new ActivityRuntimeContext(stubRequestJson.getJSONObject("runtimeContext"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        JSONObject stubRequestJson = create();
        if (runtimeContext != null)
            stubRequestJson.put("runtimeContext", runtimeContext.getJson());
        json.put(JSON_NAME, stubRequestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }
}
