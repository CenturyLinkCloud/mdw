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
import com.centurylink.mdw.monitor.ActivityMonitor;

public class ActivityTraceMonitor extends Monitor implements ActivityMonitor {

    @Override
    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nACTIVITY START:\n---------------\n");
        sb.append("activityName: " + runtimeContext.getActivity().getActivityName()).append("\n");
        sb.append("activityInstanceId: " + runtimeContext.getActivityInstanceId()).append("\n");
        runtimeContext.logInfo(sb.toString());
        return null;
    }

    @Override
    public String onExecute(ActivityRuntimeContext runtimeContext) {
        return null;
    }

    @Override
    public Map<String,Object> onFinish(ActivityRuntimeContext runtimeContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nACTIVITY FINISH:\n----------------\n");
        sb.append("activityName: " + runtimeContext.getActivity().getActivityName()).append("\n");
        sb.append("activityInstanceId: " + runtimeContext.getActivityInstanceId()).append("\n");
        sb.append("completionCode: " + runtimeContext.getCompletionCode());
        runtimeContext.logInfo(sb.toString());
        return null;
    }

    @Override
    public void onError(ActivityRuntimeContext runtimeContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nACTIVITY ERROR:\n---------------\n");
        sb.append("activityName: " + runtimeContext.getActivity().getActivityName());
        sb.append("activityInstanceId: " + runtimeContext.getActivityInstanceId()).append("\n");
        sb.append("completionCode: " + runtimeContext.getCompletionCode());
        runtimeContext.logInfo(sb.toString());
    }

}
