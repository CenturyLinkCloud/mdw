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
