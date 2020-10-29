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

    public ScriptExecutor getExecutor(String language) throws ExecutionException {
        // built-in executors first
        if ("Groovy".equalsIgnoreCase(language)) {
            return new GroovyExecutor();
        }
        else if ("JavaScript".equalsIgnoreCase(language)) {
            return new JavaScriptExecutor();
        }

        Class<? extends ScriptExecutor> executorClass = findExecutorClass(language);
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
                    if ("language".equals(param.name()) && language.equalsIgnoreCase(param.value())) {
                        return execClass;
                    }
                }
            }
        }
        return null;
    }

    public ScriptEvaluator getEvaluator(String language) throws ExecutionException {
        // built-in evaluators first
        if ("Groovy".equalsIgnoreCase(language)) {
            return new GroovyExecutor();
        }
        else if ("JavaScript".equalsIgnoreCase(language)) {
            return new JavaScriptExecutor();
        }

        Class<? extends ScriptEvaluator> evaluatorClass = findEvaluatorClass(language);
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
                    if ("language".equals(param.name()) && language.equalsIgnoreCase(param.value())) {
                        return evalClass;
                    }
                }
            }
        }
        return null;
    }

}
