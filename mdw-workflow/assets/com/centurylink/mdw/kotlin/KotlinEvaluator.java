/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.kotlin;

import java.util.Map;

import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptEvaluator;
import com.centurylink.mdw.services.process.ActivityLogger;

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
