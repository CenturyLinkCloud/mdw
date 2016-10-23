/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import java.util.Map;

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

public interface JavaExecutor {

    public void initialize(ActivityRuntimeContext runtimeContext) throws MdwJavaException;
    
    public Object execute(Map<String,Object> variables) throws JavaExecutionException;
}
