package com.centurylink.mdw.python;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptEvaluator;

import javax.script.ScriptException;
import java.util.Map;

/**
 * Python expression evaluator.
 */
@RegisteredService(value=ScriptEvaluator.class,
        parameters={@Parameter(name="language", value="Python")})
public class PythonEvaluator implements ScriptEvaluator {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object evaluate(String expression, Map<String,Object> bindings) throws ExecutionException {
        PythonRunner runner = new PythonRunner();
        try {
            return runner.eval(expression, bindings);
        }
        catch (ScriptException ex) {
            throw new ExecutionException("Error evaluating expression " + name + ": " + ex.getMessage(), ex);
        }
    }
}
