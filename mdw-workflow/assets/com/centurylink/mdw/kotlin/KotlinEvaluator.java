package com.centurylink.mdw.kotlin;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptEvaluator;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Map;

@RegisteredService(value=ScriptEvaluator.class,
parameters={@Parameter(name="language", value="Kotlin Script")})
public class KotlinEvaluator implements ScriptEvaluator {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object evaluate(String script, Map<String,Object> bindings)
            throws ExecutionException {
        try {
            KotlinCompiledScript compiled = KotlinAccess.getScript(name);
            if (compiled == null) {
                synchronized(KotlinAccess.getInstance().getScripts()) {
                    compiled = KotlinAccess.getScript(name);
                    if (compiled == null) {
                        KotlinScriptEngine scriptEngine = KotlinAccess.getInstance().getScriptEngine();
                        compiled = (KotlinCompiledScript) scriptEngine.compile(name, script);
                        KotlinAccess.putScript(name, compiled);
                    }
                }
            }
            return compiled.eval(new SimpleBindings(bindings));
        }
        catch (KotlinScriptException ex) {
            Exception withName = new KotlinScriptException(ex.getMessage(), name, ex.getLineNumber());
            throw new ExecutionException(withName.getMessage(), withName);
        }
        catch (ScriptException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }
}
