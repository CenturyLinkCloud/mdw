/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.app;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.MdwException;

public class WorkflowException extends MdwException {

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(int code, String message) {
        super(code, message);
    }

    public WorkflowException(int code, String message, Throwable th) {
        super(code, message, th);
    }

    public WorkflowException(String message, Throwable th) {
        super(message, th);
    }

    private int retryDelay;
    public int getRetryDelay() {
        return retryDelay;
    }
    public void setRetryDelay(int delay) {
        this.retryDelay = delay;
    }

    public WorkflowException(JSONObject json) throws JSONException {
        super(json);
        if (json.has("retryDelay"))
            this.retryDelay = json.getInt("retryDelay");
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (retryDelay > 0)
            json.put("retryDelay", retryDelay);
        return json;
    }
}
