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
