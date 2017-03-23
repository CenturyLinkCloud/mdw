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

import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.ProcessMonitor;

public class ProcessTraceMonitor extends Monitor implements ProcessMonitor {

    @Override
    public Map<String,Object> onStart(ProcessRuntimeContext runtimeContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nPROCESS START:\n--------------\n");
        sb.append("processName: " + runtimeContext.getProcess().getName()).append("\n");
        sb.append("masterRequestId: " + runtimeContext.getMasterRequestId()).append("\n");
        sb.append("processInstanceId: " + runtimeContext.getProcessInstanceId()).append("\n");
        sb.append("variables:\n----------\n");
        for (String varName : runtimeContext.getVariables().keySet()) {
            sb.append(varName + "=" + runtimeContext.getVariables().get(varName)).append("\n");
        }
        runtimeContext.logInfo(sb.toString());
        return null;
    }

    @Override
    public Map<String,Object> onFinish(ProcessRuntimeContext runtimeContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nPROCESS FINISH:\n---------------\n");
        sb.append("processName: " + runtimeContext.getProcess().getName()).append("\n");
        sb.append("masterRequestId: " + runtimeContext.getMasterRequestId()).append("\n");
        sb.append("processInstanceId: " + runtimeContext.getProcessInstanceId()).append("\n");
        sb.append("completionCode: " + runtimeContext.getCompletionCode()).append("\n");
        sb.append("variables:\n----------\n");
        for (String varName : runtimeContext.getVariables().keySet()) {
            sb.append(varName + "=" + runtimeContext.getVariables().get(varName)).append("\n");
        }
        runtimeContext.logInfo(sb.toString());
        return null;
    }

}
