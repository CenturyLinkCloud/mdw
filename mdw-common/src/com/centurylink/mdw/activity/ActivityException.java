/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.WorkflowException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

/**
 * Exception thrown by a workflow activity.
 */
public class ActivityException extends WorkflowException {

    public ActivityException(String message) {
        super(message);
    }

    public ActivityException(int code, String message) {
        super(code, message);

    }

    public ActivityException(int code, String message, Throwable th) {
        super(code, message, th);

    }

    public ActivityException(String message, Throwable th) {
        super(message, th);
    }

    private ActivityRuntimeContext runtimeContext;
    public ActivityRuntimeContext getRuntimeContext() { return runtimeContext; }
    public void setRuntimeContext(ActivityRuntimeContext context) { this.runtimeContext = context; }

    public ActivityException(JSONObject json) throws JSONException {
        super(json);
        if (json.has("runtimeContext"))
            this.runtimeContext = new ActivityRuntimeContext(json.getJSONObject("runtimeContext"));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (runtimeContext != null)
            json.put("runtimeContext", runtimeContext.getJson(true));
        return json;
    }
}
