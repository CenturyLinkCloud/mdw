/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.WorkflowException;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.ThrowableJsonable;
import com.centurylink.mdw.model.workflow.ActivityInstance;

/**
 * Exception thrown by a workflow activity.
 */
public class ActivityException extends WorkflowException implements Jsonable {

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

//    public ActivityException(JSONObject json) throws JSONException {
//        if (json.has("activityInstance"))
//            this.activityInstance = new ActivityInstance(json.getJSONObject("activityInstance"));
//    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new ThrowableJsonable(this).getJson();
        if (activityInstance != null)
            json.put("activityInstance", activityInstance.getJson());
        return json;
    }

    @Override
    public String getJsonName() {
        return "activityException";
    }

    public String toString() {
        if (activityInstance == null)
            return super.toString();

        String str = super.toString() + "\n" + activityInstance.getJsonName() + ":\n";
        try {
            str += activityInstance.getJson().toString(2);
            return str;
        }
        catch (JSONException e) {
            return super.toString();
        }
    }


}
