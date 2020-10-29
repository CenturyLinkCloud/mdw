package com.centurylink.mdw.script;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;

public interface ScriptEvaluator extends RegisteredService {

    public String getName();
    public void setName(String name);

    public Object evaluate(String expression, Map<String,Object> bindings) throws ExecutionException;

}
