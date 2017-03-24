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
