package com.centurylink.mdw.python;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptExecutor;

import javax.script.ScriptException;
import java.util.Map;

/**
 * Python script executor.
 */
@RegisteredService(value=ScriptExecutor.class,
        parameters={@Parameter(name="language", value="Python")})
public class PythonExecutor implements ScriptExecutor {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException {
        PythonRunner runner = new PythonRunner();
        try {
            return runner.exec(script, bindings);
        }
        catch (ScriptException ex) {
            throw new ExecutionException("Error running script " + name + ": " + ex.getMessage(), ex);
        }
    }
}
