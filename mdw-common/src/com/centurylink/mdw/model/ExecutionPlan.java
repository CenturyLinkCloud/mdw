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
package com.centurylink.mdw.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.bpm.ParameterDocument.Parameter;
import com.centurylink.mdw.bpm.ProcessExecutionPlanDocument;
import com.centurylink.mdw.bpm.ProcessExecutionPlanDocument.ProcessExecutionPlan;
import com.centurylink.mdw.bpm.SubprocessInstanceDocument.SubprocessInstance;

/**
 * Bean to represent an execution plan.
 */
public class ExecutionPlan {

    private List<Subprocess> subprocesses = new ArrayList<Subprocess>();
    public List<Subprocess> getSubprocesses() { return subprocesses; }
    public void setSubprocesses(List<Subprocess> subprocs) { this.subprocesses = subprocs; }

    public ProcessExecutionPlanDocument toDocument() {
        ProcessExecutionPlanDocument doc = ProcessExecutionPlanDocument.Factory.newInstance();
        ProcessExecutionPlan plan = doc.addNewProcessExecutionPlan();
        if (subprocesses != null) {
            for (Subprocess subprocess : subprocesses) {
                SubprocessInstance subproc = plan.addNewSubprocessInstance();
                subproc.setLogicalProcessName(subprocess.logicalName);
                if (subprocess.instanceId != null)
                    subproc.setInstanceId(String.valueOf(subprocess.instanceId));
                if (subprocess.statusCode != null)
                    subproc.setStatusCode(subprocess.statusCode);
                if (subprocess.parameters != null) {
                    for (String key : subprocess.parameters.keySet()) {
                        Parameter param = subproc.addNewParameter();
                        param.setName(key);
                        param.setStringValue(subprocess.parameters.get(key));
                    }
                }
            }
        }
        return doc;
    }

    public void fromDocument(ProcessExecutionPlanDocument doc) {
        subprocesses = new ArrayList<Subprocess>();
        ProcessExecutionPlan plan = doc.getProcessExecutionPlan();
        if (plan != null) {
            List<SubprocessInstance> subprocs = plan.getSubprocessInstanceList();
            if (subprocs != null) {
                for (SubprocessInstance subproc : subprocs) {
                    Subprocess subprocess = new Subprocess();
                    subprocess.setLogicalName(subproc.getLogicalProcessName());
                    if (subproc.getInstanceId() != null)
                        subprocess.setInstanceId(Long.valueOf(subproc.getInstanceId()));
                    subprocess.setStatusCode(subproc.getStatusCode());
                    List<Parameter> params = subproc.getParameterList();
                    if (params != null) {
                        for (Parameter param : params) {
                            subprocess.parameters.put(param.getName(), param.getStringValue());
                        }
                    }
                    subprocesses.add(subprocess);
                }
            }
        }
    }
}
