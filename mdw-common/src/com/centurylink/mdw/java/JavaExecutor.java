package com.centurylink.mdw.java;

import java.util.Map;

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

public interface JavaExecutor {

    default void initialize(ActivityRuntimeContext runtimeContext) throws MdwJavaException {
        // do nothing
    }


    Object execute(Map<String,Object> variables) throws JavaExecutionException;
}
