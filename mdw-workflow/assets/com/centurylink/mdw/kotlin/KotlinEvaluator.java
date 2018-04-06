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
import com.centurylink.mdw.script.ScriptEvaluator;
import com.centurylink.mdw.script.TypedEvaluator;

@RegisteredService(value=ScriptEvaluator.class,
parameters={@Parameter(name="language", value="Kotlin")})
public class KotlinEvaluator extends KotlinExecutor implements TypedEvaluator {

    @Override
    public Object evaluate(String script, Map<String,Object> bindings)
            throws ExecutionException {
        return evaluate(script, bindings, null);
    }

    @Override
    public Object evaluate(String script, Map<String,Object> bindings, Map<String,String> types)
            throws ExecutionException {
        return execute(script, bindings, types);
    }
}