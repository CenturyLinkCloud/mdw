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
package com.centurylink.mdw.testing;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.ActivityStubRequest;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.services.event.StubHelper;
import com.centurylink.mdw.translator.VariableTranslator;

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
                        updates = new HashMap<String,Object>();
                        for (String name : variables.keySet()) {
                            String strValue = variables.get(name);
                            Process process = runtimeContext.getProcess();
                            Variable variable = process.getVariable(name);
                            if (variable == null)
                                throw new IllegalStateException("Variable: " + name + " not defined for process: " + process.getFullLabel());
                            boolean isDoc = VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), variable.getType());
                            Object value;
                            if (isDoc)
                                value = VariableTranslator.realToObject(runtimeContext.getPackage(), variable.getType(), strValue);
                            else
                                value = VariableTranslator.toObject(variable.getType(), strValue);
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
