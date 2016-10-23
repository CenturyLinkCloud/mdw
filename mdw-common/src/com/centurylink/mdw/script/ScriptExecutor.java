/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import java.util.Map;

public interface ScriptExecutor {

    public String getName();
    public void setName(String name);
    
    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException;
}
