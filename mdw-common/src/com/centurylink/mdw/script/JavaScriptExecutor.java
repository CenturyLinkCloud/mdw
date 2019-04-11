/*
 * Copyright (C) 2017 CenturyLink, Inc.
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

import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;


public class JavaScriptExecutor implements ScriptExecutor, ScriptEvaluator {
    
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Object execute(String script, Map<String,Object> bindings)
    throws ExecutionException {
      
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            if (engine == null)
                engine = factory.getEngineByName("Rhino JavaScript"); // ServiceMix
            for (String bindName : bindings.keySet()) {
                engine.put(bindName, bindings.get(bindName));
            }
           
           Object retObj = null;
           try {
               retObj = engine.eval(script);
           }
           catch (ScriptException ex) {
               if (ex.getMessage().indexOf("invalid return") >= 0) {
                   // treat the script as a function call
                   engine.eval("function f(){" + script + "}");
                   try {
                       retObj = ((Invocable)engine).invokeFunction("f");
                   }
                   catch (NoSuchMethodException ex2) {
                       logger.severeException(ex2.getMessage(), ex2);
                   }
               }
               else {
                   throw ex;
               }
           }
            
           for (String bindName : bindings.keySet()) {
               Object jsVarValue = engine.get(bindName);
               if (jsVarValue instanceof java.lang.Double) {
                   java.lang.Double doubleTrouble = (java.lang.Double) jsVarValue;
                   if (doubleTrouble.toString().endsWith(".0"))
                     jsVarValue = new Long(doubleTrouble.longValue());
               }
               bindings.put(bindName, jsVarValue);
           }
           
           return retObj;
        }
        catch (ScriptException ex) {
            throw new ExecutionException("Error executing JavaScript: " + ex.getMessage(), ex);
        }
    }

    public Object evaluate(String expression, Map<String, Object> bindings)
    throws ExecutionException {
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            if (engine == null)
                engine = factory.getEngineByName("Rhino JavaScript");  // ServiceMix
            for (String bindName : bindings.keySet()) {
                engine.put(bindName, bindings.get(bindName));
            }
           
           return engine.eval(expression);
        }
        catch (ScriptException ex) {
            throw new ExecutionException("Error executing JavaScript: " + ex.getMessage(), ex);
        }
    }
    
    public static void initialize() {
        // TODO support preInit for ScriptEngine
    }
    
}
