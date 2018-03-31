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

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.centurylink.mdw.kotlin.script.KotlinScriptEngineFactory;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.ScriptEvaluator;
import com.centurylink.mdw.script.ScriptExecutor;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Kotlin script support.
 */
public class KotlinExecutor implements ScriptExecutor, ScriptEvaluator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException {
        logger.info("Executing script: " + name);
        return null;
    }

    @Override
    public Object evaluate(String expression, Map<String,Object> bindings)
            throws ExecutionException {
        try {
            ScriptEngine engine = getScriptEngine();
            for (String bindName : bindings.keySet()) {
                engine.put(bindName, bindings.get(bindName));
            }
            return engine.eval(expression);
        }
        catch (ScriptException ex) {
            throw new ExecutionException("Kotlin evaluation error: " + ex.getMessage(), ex);
        }
    }

    // TODO reuse engine
    private ScriptEngine scriptEngine;
    protected ScriptEngine getScriptEngine() {
        return new KotlinScriptEngineFactory().getScriptEngine();
    }
}
