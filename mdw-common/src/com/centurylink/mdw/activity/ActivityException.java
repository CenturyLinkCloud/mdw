/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import org.json.JSONException;

import com.centurylink.mdw.app.WorkflowException;
import com.centurylink.mdw.model.workflow.ActivityInstanceInfo;

/**
 * Exception thrown by a workflow activity.
 */
public class ActivityException extends WorkflowException {

    public ActivityException(String message){
        super(message);
    }

    public ActivityException(int code, String message){
        super(code, message);

    }

    public ActivityException(int code, String message, Throwable th){
        super(code, message, th);

    }

    public ActivityException(String message, Throwable th) {
      super(message, th);

    }

    private ActivityInstanceInfo actInst = null;

    public ActivityInstanceInfo getActivityInstance() {
        return actInst;
    }

    public void setActivityInstance(ActivityInstanceInfo activityInst) {
        actInst = activityInst;
    }

    public String toString() {
        if (actInst == null)
            return super.toString();

        String str = super.toString() + "\n" + actInst.getJsonName() + ":\n";
        try {
            str += actInst.getJson().toString(2);
            return str;
        }
        catch (JSONException e) {
            return super.toString();
        }
    }
}
