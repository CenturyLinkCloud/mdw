package com.centurylink.mdw.script;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;

public interface ScriptExecutor extends RegisteredService {

    public String getName();
    public void setName(String name);

    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException;
}
