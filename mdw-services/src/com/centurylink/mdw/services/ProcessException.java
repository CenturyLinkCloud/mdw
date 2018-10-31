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
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import org.json.JSONException;
import org.json.JSONObject;

public class ProcessException extends MdwException {

    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(int code, String message) {
        super(code, message);
    }

    public ProcessException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public ProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    private ProcessRuntimeContext runtimeContext;
    public ProcessRuntimeContext getRuntimeContext() { return runtimeContext; }
    public void setRuntimeContext(ProcessRuntimeContext context) { this.runtimeContext = context; }

    public ProcessException(JSONObject json) throws JSONException {
        super(json);
        if (json.has("runtimeContext"))
            this.runtimeContext = new ProcessRuntimeContext(json.getJSONObject("runtimeContext"));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (runtimeContext != null)
            json.put("runtimeContext", runtimeContext.getJson());
        return json;
    }
}
