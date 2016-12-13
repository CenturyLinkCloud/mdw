/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

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
        JSONObject json = new JSONObject();
        JSONObject stubRequestJson = new JSONObject();
        if (runtimeContext != null)
            stubRequestJson.put("runtimeContext", runtimeContext.getJson());
        json.put(JSON_NAME, stubRequestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }
}
