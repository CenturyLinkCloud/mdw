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
package com.centurylink.mdw.monitor.impl;

import java.util.Map;

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.AdapterMonitor;

public class AdapterTraceMonitor extends Monitor implements AdapterMonitor {

    public Object onRequest(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nADAPTER INVOKE:\n---------------\n");
        sb.append("masterRequestId: " + runtimeContext.getMasterRequestId()).append("\n");
        sb.append("activityInstanceId: " + runtimeContext.getActivityInstanceId()).append("\n");
        sb.append("request: " + request).append("\n");
        runtimeContext.logInfo(sb.toString());
        return null;
    }


    @Override
    public Object onInvoke(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        return null;
    }

    @Override
    public Object onResponse(ActivityRuntimeContext runtimeContext, Object response, Map<String,String> headers) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nADAPTER RESPONSE:\n----------------\n");
        sb.append("masterRequestId: " + runtimeContext.getMasterRequestId()).append("\n");
        sb.append("activityInstanceId: " + runtimeContext.getActivityInstanceId()).append("\n");
        sb.append("response: " + response).append("\n");
        sb.append("completionCode: " + runtimeContext.getCompletionCode()).append("\n");
        sb.append("headers:\n--------\n");
        for (String key : headers.keySet())
            sb.append(key).append("=").append(headers.get(key)).append("\n");
        runtimeContext.logInfo(sb.toString());
        return null;
    }

    @Override
    public String onError(ActivityRuntimeContext runtimeContext, Throwable t) {
        return null;
    }

}
