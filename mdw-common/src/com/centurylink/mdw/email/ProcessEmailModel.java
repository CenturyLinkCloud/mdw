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
package com.centurylink.mdw.email;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.workflow.ProcessInstance;

public class ProcessEmailModel extends HashMap<String,Object> implements TemplatedEmail.Model  {

    private ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }

    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }

    public ProcessEmailModel(ProcessInstance processInstance, Map<String,Object> variables) {
        this.processInstance = processInstance;
        this.variables = variables;
    }

    @Override
    public Map<String,String> getKeyParameters() {
        Map<String,String> keyParams = new HashMap<String,String>();
        keyParams.put("processInstanceId", processInstance.getId().toString());
        return keyParams;
    }

    @Override
    public Object get(Object key) {
        if ("masterRequestId".equals(key))
            return processInstance.getMasterRequestId();
        else if ("processInstanceId".equals(key))
            return processInstance.getId();
        else if ("processName".equals(key))
            return processInstance.getProcessName();
        else if ("processInstance".equals(key))
            return processInstance;
        else if ("variables".equals(key))
            return variables;
        else
            return variables.get(key);
    }

}
