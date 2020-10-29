package com.centurylink.mdw.kotlin;

import java.util.Map;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptExecutor;

/**
 * Kotlin script support.
 */
@RegisteredService(value=ScriptExecutor.class,
parameters={@Parameter(name="language", value="Kotlin Script")})
public class KotlinExecutor extends KotlinEvaluator implements ScriptExecutor {

    @Override
    public Object execute(String script, Map<String,Object> bindings)
            throws ExecutionException {
        return evaluate(script, bindings);
    }
}
