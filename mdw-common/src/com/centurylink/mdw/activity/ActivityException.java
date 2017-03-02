/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.WorkflowException;
import com.centurylink.mdw.model.workflow.ActivityInstance;

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

    private ActivityInstance activityInstance = null;
    public ActivityInstance getActivityInstance() {
        return activityInstance;
    }
    public void setActivityInstance(ActivityInstance activityInstance) {
        this.activityInstance = activityInstance;
    }

    public ActivityException(JSONObject json) throws JSONException {
        super(json);
        if (json.has("activityInstance"))
            this.activityInstance = new ActivityInstance(json.getJSONObject("activityInstance"));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (activityInstance != null)
            json.put("activityInstance", activityInstance.getJson());
        return json;
    }
}
