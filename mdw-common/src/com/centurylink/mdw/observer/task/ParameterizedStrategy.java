/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.HashMap;
import java.util.Map;

public abstract class ParameterizedStrategy {
    
    private Map<String,Object> parameters;
    
    public Map<String,Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String,Object> parameters) {
        this.parameters = parameters;
    }
    
    public Object getParameter(String paramName) {
        if (parameters == null)
            return null;
        
        return parameters.get(paramName);
    }
    
    public void setParameter(String paramName, Object paramVal) {
        if (parameters == null)
            parameters = new HashMap<String,Object>();
        parameters.put(paramName,  paramVal);
    }
}
