/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.email;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.workflow.ProcessInstance;

public class ProcessEmailModel extends HashMap<String,Object> implements TemplatedEmail.Model  {

    private static final long serialVersionUID = 1L;

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
