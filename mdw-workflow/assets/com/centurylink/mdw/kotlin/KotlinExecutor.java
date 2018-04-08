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

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase.CompiledKotlinScript;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptExecutor;

/**
 * Kotlin script support.
 * TODO: Show line numbers for script errors
 */
@RegisteredService(value=ScriptExecutor.class,
parameters={@Parameter(name="language", value="Kotlin Script")})
public class KotlinExecutor implements ScriptExecutor {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object execute(String script, Map<String,Object> bindings)
            throws ExecutionException {
        try {
            KotlinScriptEngine engine = KotlinAccess.getInstance().getScriptEngine();
            for (String bindName : bindings.keySet()) {
                engine.put(bindName, bindings.get(bindName));
            }
            CompiledKotlinScript compiled = KotlinAccess.getScript(name);
            if (compiled == null) {
                compiled = (CompiledKotlinScript) engine.compile(script);
                KotlinAccess.putScript(name, compiled);
            }
            return engine.eval(compiled);
        }
        catch (ScriptException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }
}
