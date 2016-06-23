/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.email;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

public class ProcessEmailModel extends HashMap<String,Object> implements TemplatedEmail.Model  {

    private static final long serialVersionUID = 1L;

    private ProcessInstanceVO processInstance;
    public ProcessInstanceVO getProcessInstance() { return processInstance; }

    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }

    public ProcessEmailModel(ProcessInstanceVO processInstance, Map<String,Object> variables) {
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
        else if ("mdwWebUrl".equals(key))
            return ApplicationContext.getMdwWebUrl();
        else if ("mdwTaskManagerUrl".equals(key))
            return ApplicationContext.getTaskManagerUrl();
        else if ("variables".equals(key))
            return variables;
        else
            return variables.get(key);
    }

}
