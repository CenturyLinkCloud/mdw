/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.testing;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.activity.ActivityStubResponse;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.services.event.StubHelper;

@RegisteredService(ActivityMonitor.class)
public class TestCaseActivityMonitor implements ActivityMonitor {
    StubHelper stubber = new StubHelper();

    // TODO remove this and perform updates through runtimeContext
    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext) {
        return null;
    }

    public String onExecute(ActivityRuntimeContext runtimeContext) {
        if (stubber.isStubbing()) {
            try {
                String stubRequest = runtimeContext.getJson().toString(2);
                String stubResponse = stubber.getStubResponse(runtimeContext.getMasterRequestId(), stubRequest);
                if ("(EXECUTE_ACTIVITY)".equals(stubResponse)) {
                    runtimeContext.logInfo("Stubber executing activity: " + runtimeContext.getActivityLogicalId());
                    return null;
                }
                else {
                    String resultCode;
                    if (stubResponse.charAt(0) == '{') {
                        ActivityStubResponse activityStubResponse = new ActivityStubResponse(new JSONObject(stubResponse));
                        resultCode = activityStubResponse.getResultCode();
                    }
                    else {
                        // compatibility for non-json response
                        resultCode = stubResponse;
                    }
                    runtimeContext.logInfo("Stubbing: " + runtimeContext.getActivityLogicalId() + " with result code: " + resultCode);
                    return resultCode == null ? "null" : resultCode;
                }
            }
            catch (Exception ex) {
                runtimeContext.logException(ex.getMessage(), ex);
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Map<String,Object> onFinish(ActivityRuntimeContext runtimeContext) {

        if (stubber.isStubbing()) {
            try {
                String stubRequest = runtimeContext.getJson().toString(2);
                String stubResponse = stubber.getStubResponse(runtimeContext.getMasterRequestId(), stubRequest);
                if ("(EXECUTE_ACTIVITY)".equals(stubResponse)) {
                    return null;
                }
                else {
                    if (stubResponse.charAt(0) == '{') {
                        ActivityStubResponse activityStubResponse = new ActivityStubResponse(new JSONObject(stubResponse));
                        Map<String,Object> updates = null;
                        Map<String,String> variables = activityStubResponse.getVariables();
                        if (variables != null) {
                            updates = new HashMap<String,Object>();
                            for (String name : variables.keySet()) {
                                String strValue = variables.get(name);
                                ProcessVO process = runtimeContext.getProcess();
                                VariableVO variable = process.getVariable(name);
                                if (variable == null)
                                    throw new IllegalStateException("Variable: " + name + " not defined for process: " + process.getLabel());
                                boolean isDoc = VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), variable.getVariableType());
                                Object value;
                                if (isDoc)
                                    value = VariableTranslator.realToObject(runtimeContext.getPackage(), variable.getVariableType(), strValue);
                                else
                                    value = VariableTranslator.toObject(variable.getVariableType(), strValue);
                                updates.put(name,  value);
                            }
                        }
                        return updates;
                    }
                    else {
                        // non-json response
                        return null;
                    }
                }
            }
            catch (Exception ex) {
                runtimeContext.logException(ex.getMessage(), ex);
                return null;
            }
        }
        else {
            return null;
        }
    }

    public void onError(ActivityRuntimeContext runtimeContext) {
    }


}
