/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
