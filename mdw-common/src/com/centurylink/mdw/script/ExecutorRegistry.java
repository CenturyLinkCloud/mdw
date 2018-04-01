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
package com.centurylink.mdw.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.annotations.Parameter;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.common.service.ServiceRegistry;

public class ExecutorRegistry extends ServiceRegistry {

    public static final List<String> scriptServices = new ArrayList<String>(Arrays.asList(
            new String[] { ScriptExecutor.class.getName(), ScriptEvaluator.class.getName() }));

    protected ExecutorRegistry(List<Class<? extends RegisteredService>> serviceInterfaces) {
        super(serviceInterfaces);
    }

    private static ExecutorRegistry instance;
    public static ExecutorRegistry getInstance() {
        if (instance == null) {
            List<Class<? extends RegisteredService>> services = new ArrayList<Class<? extends RegisteredService>>();
            services.add(ScriptExecutor.class);
            services.add(ScriptEvaluator.class);
            instance = new ExecutorRegistry(services);
        }
        return instance;
    }

    private static Map<String,Class<? extends ScriptExecutor>> executors = new HashMap<>();
    public ScriptExecutor getExecutor(String language) throws ExecutionException {
        // built-in executors first
        if ("Groovy".equalsIgnoreCase(language)) {
            return new GroovyExecutor();
        }
        else if ("JavaScript".equalsIgnoreCase(language)) {
            return new JavaScriptExecutor();
        }

        Class<? extends ScriptExecutor> executorClass = executors.get(language);
        if (executorClass == null) {
            executorClass = findExecutorClass(language);
            executors.put(language, executorClass);
        }
        if (executorClass == null)
            throw new ExecutionException("No ScriptExecutor found for " + language + " (needs an optional asset package?)");

        try {
            return executorClass.newInstance();
        }
        catch (ReflectiveOperationException ex) {
            throw new ExecutionException("Cannot instantiate " + executorClass, ex);
        }
    }

    protected Class<? extends ScriptExecutor> findExecutorClass(String language) {
        for (Class<? extends ScriptExecutor> execClass : getDynamicServiceClasses(ScriptExecutor.class)) {
            com.centurylink.mdw.annotations.RegisteredService annotation = execClass
                    .getAnnotation(com.centurylink.mdw.annotations.RegisteredService.class);
            if (annotation.parameters() != null) {
                for (Parameter param : annotation.parameters()) {
                    if ("language".equals(param.name()) && language.equals(param.value())) {
                        return execClass;
                    }
                }
            }
        }
        return null;
    }

    private static Map<String,Class<? extends ScriptEvaluator>> evaluators = new HashMap<>();
    public ScriptEvaluator getEvaluator(String language) throws ExecutionException {
        // built-in evaluators first
        if ("Groovy".equalsIgnoreCase(language)) {
            return new GroovyExecutor();
        }
        else if ("JavaScript".equalsIgnoreCase(language)) {
            return new JavaScriptExecutor();
        }

        Class<? extends ScriptEvaluator> evaluatorClass = evaluators.get(language);
        if (evaluatorClass == null) {
            evaluatorClass = findEvaluatorClass(language);
            evaluators.put(language, evaluatorClass);
        }
        if (evaluatorClass == null)
            throw new ExecutionException("No ScriptEvaluator found for " + language + " (needs an optional asset package?)");

        try {
            return evaluatorClass.newInstance();
        }
        catch (ReflectiveOperationException ex) {
            throw new ExecutionException("Cannot instantiate " + evaluatorClass, ex);
        }
    }

    protected Class<? extends ScriptEvaluator> findEvaluatorClass(String language) {
        for (Class<? extends ScriptEvaluator> evalClass : getDynamicServiceClasses(ScriptEvaluator.class)) {
            com.centurylink.mdw.annotations.RegisteredService annotation = evalClass
                    .getAnnotation(com.centurylink.mdw.annotations.RegisteredService.class);
            if (annotation.parameters() != null) {
                for (Parameter param : annotation.parameters()) {
                    if ("language".equals(param.name()) && language.equals(param.value())) {
                        return evalClass;
                    }
                }
            }
        }
        return null;
    }

}
