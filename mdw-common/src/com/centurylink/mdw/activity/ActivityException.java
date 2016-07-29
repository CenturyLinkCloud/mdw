/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import org.json.JSONException;

import com.centurylink.mdw.common.WorkflowException;
import com.centurylink.mdw.model.value.activity.ActivityInstance;

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

    private ActivityInstance actInst = null;

    public ActivityInstance getActivityInstance() {
        return actInst;
    }

    public void setActivityInstance(ActivityInstance activityInst) {
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
