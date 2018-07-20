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
            json.put("runtimeContext", runtimeContext.getJson());
        return json;
    }
}
