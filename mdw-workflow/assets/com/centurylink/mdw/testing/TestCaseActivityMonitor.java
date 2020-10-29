package com.centurylink.mdw.testing;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.ActivityStubRequest;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.services.event.StubHelper;

import java.util.HashMap;
import java.util.Map;

@Monitor(value="Test Cases", category=ActivityMonitor.class, defaultEnabled=true)
public class TestCaseActivityMonitor implements ActivityMonitor {
    private StubHelper stubber = new StubHelper();

    // TODO remove this and perform updates through runtimeContext
    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext) {
        return null;
    }

    public String onExecute(ActivityRuntimeContext runtimeContext) {
        if (stubber.isStubbing()) {
            try {
                ActivityStubRequest stubRequest = new ActivityStubRequest(runtimeContext);
                ActivityStubResponse stubResponse = (ActivityStubResponse) stubber.getStubResponse(
                        runtimeContext.getMasterRequestId(), stubRequest.getJson().toString(2));
                if (stubResponse.isPassthrough()) {
                    runtimeContext.logInfo("Stubber executing activity: " + runtimeContext.getActivityLogicalId());
                    return null;
                }
                else {
                    String resultCode = stubResponse.getResultCode();
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
                String stubRequest = new ActivityStubRequest(runtimeContext).getJson().toString(2);
                ActivityStubResponse stubResponse = (ActivityStubResponse)stubber.getStubResponse(runtimeContext.getMasterRequestId(), stubRequest);
                if (stubResponse.isPassthrough()) {
                    return null;
                }
                else {
                    Map<String,Object> updates = null;
                    Map<String,String> variables = stubResponse.getVariables();
                    if (variables != null) {
                        updates = new HashMap<>();
                        for (String name : variables.keySet()) {
                            String strValue = variables.get(name);
                            Process process = runtimeContext.getProcess();
                            Variable variable = process.getVariable(name);
                            if (variable == null)
                                throw new IllegalStateException("Variable: " + name + " not defined for process: " + process.getQualifiedLabel());
                            Object value = runtimeContext.getPackage().getObjectValue(variable.getType(), strValue, true);
                            updates.put(name,  value);
                        }
                    }
                    return updates;
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
