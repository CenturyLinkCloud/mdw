/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import java.util.Map;

public interface ScriptEvaluator {
    
    public String getName();
    public void setName(String name);

    public Object evaluate(String expression, Map<String,Object> bindings) throws ExecutionException;

}
