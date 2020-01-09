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
package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.*;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;
import com.sun.el.ExpressionFactoryImpl;
import com.sun.el.ValueExpressionLiteral;
import org.json.JSONException;
import org.json.JSONObject;

import javax.el.*;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProcessRuntimeContext extends ELContext implements RuntimeContext, Jsonable {

    protected StandardLogger logger;

    protected Package pkg;
    public Package getPackage() { return pkg; }

    protected Process process;
    public Process getProcess() { return process; }

    protected ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }

    private int performanceLevel;
    public int getPerformanceLevel() { return performanceLevel; }

    private boolean inService;
    public boolean isInService() { return inService; }

    @Override
    public Long getInstanceId() {
        return null;
    }

    /**
     * Purposely separate from processInstance.getVariables().
     */
    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }

    /**
     * For read-only access.
     */
    public Map<String,String> getDocRefs() {
        return new HashMap<String,String>() {
            @Override
            public String get(Object varName) {
                VariableInstance varInst = processInstance.getVariable((String)varName);
                if (varInst != null && varInst.getData() instanceof DocumentReference)
                    return varInst.getData().toString();
                return null;
            }
        };
    }

    public String getMasterRequestId() {
        return processInstance.getMasterRequestId();
    }

    public String getAppId() { return ApplicationContext.getAppId(); }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
            for (Attribute attribute : process.getAttributes()) {
                attributes.put(attribute.getName(), attribute.getValue());
            }
        }
        return attributes;
    }

    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    public boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : v.equalsIgnoreCase("true");
    }

    public int getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    public String getAttribute(String name, String defaultValue) {
        String v = getAttribute(name);
        return v == null ? defaultValue : v;
    }

    public ProcessRuntimeContext(StandardLogger logger, Package pkg, Process process, ProcessInstance processInstance, int performanceLevel, boolean inService) {
        if (logger == null)
            this.logger = LoggerUtil.getStandardLogger();
        else
            this.logger = logger;
        this.pkg = pkg;
        this.process = process;
        this.processInstance = processInstance;
        this.performanceLevel = performanceLevel;
        this.inService = inService;
        this.variables = new HashMap<>();
    }

    public ProcessRuntimeContext(StandardLogger logger, Package pkg, Process process, ProcessInstance processInstance, int performanceLevel, boolean inService, Map<String,Object> variables) {
        if (logger == null)
            this.logger = LoggerUtil.getStandardLogger();
        else
            this.logger = logger;
        this.pkg = pkg;
        this.process = process;
        this.processInstance = processInstance;
        this.performanceLevel = performanceLevel;
        this.inService = inService;
        this.variables = variables;
    }

    public String getProperty(String name) {
        return PropertyManager.getProperty(name);
    }

    public Long getProcessId() {
        return process.getId();
    }

    public Long getProcessInstanceId() {
        return processInstance.getId();
    }

    public void logInfo(String message) {
        logger.info(logtag(), message);
        if (logPersister != null)
            logPersister.persist(getProcessInstanceId(), getInstanceId(), StandardLogger.LogLevel.INFO, message);
    }

    public void logDebug(String message) {
        logger.debug(logtag(), message);
        if (logPersister != null)
            logPersister.persist(getProcessInstanceId(), getInstanceId(), StandardLogger.LogLevel.DEBUG, message);
    }

    public void logTrace(String message) {
        logger.trace(logtag(), message);
    }

    public void logWarn(String message) {
        logger.warn(logtag(), message);
        if (logPersister != null)
            logPersister.persist(getProcessInstanceId(), getInstanceId(), StandardLogger.LogLevel.WARN, message);
    }

    public void logSevere(String message) {
        logError(message);
    }

    public void logError(String message) {
        logger.severe(logtag(), message);
        if (logPersister != null)
            logPersister.persist(getProcessInstanceId(), getInstanceId(), StandardLogger.LogLevel.ERROR, message);
    }

    public void logException(String msg, Throwable t) {
        logError(msg, t);
    }

    public void logError(String msg, Throwable t) {
        logger.exception(logtag(), msg, t);
        if (logPersister != null)
            logPersister.persist(getProcessInstanceId(), getInstanceId(), StandardLogger.LogLevel.ERROR, msg, t);
    }

    public boolean isLogInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isLogDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isLogTraceEnabled() {
        return logger.isTraceEnabled();
    }

    protected String logtag() {
        return "p" + getProcessId() + "." + getProcessInstanceId();
    }

    public String getCompletionCode() {
        return processInstance.getCompletionCode();
    }

    public void setProcessCompletionCode(String code) {
        processInstance.setCompletionCode(code);
    }

    public Object evaluate(String expression) {
        ExpressionFactory factory = getExpressionFactory();
        ValueExpression valueExp = factory.createValueExpression(this, expression, Object.class);
        return valueExp.getValue(this);
    }

    public void set(String expression, Object value) {
        ExpressionFactory factory = getExpressionFactory();
        ValueExpression valueExp = factory.createValueExpression(this, expression, Object.class);
        valueExp.setValue(this, value);
    }

    public String evaluateToString(String expression) {
        Object obj = evaluate(expression);
        return obj == null ? "" : obj.toString();
    }

    private static ExpressionFactory expressionFactory;
    private CompositeELResolver elResolver;
    private VariableMapper variableMapper;
    private FunctionMapper functionMapper;

    public static ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = new ExpressionFactoryImpl();
        }
        return expressionFactory;
    }

    public ELResolver getELResolver() {
        if (elResolver == null) {
            elResolver = new CompositeELResolver() {
                @Override
                public Object getValue(ELContext elContext, Object base, Object property) {
                    if (base == null || base.equals("")) {
                        elContext.setPropertyResolved(true);
                        return "";  // don't blow up on empty variables
                    }
                    else {
                        return super.getValue(elContext, base, property);
                    }
                }
            };
            elResolver.add(new XPathELResolver());
            elResolver.add(new MapELResolver());
            elResolver.add(new ListELResolver());
            elResolver.add(new ArrayELResolver());
            elResolver.add(new BeanELResolver());
        }
        return elResolver;
    }

    public VariableMapper getVariableMapper() {
        if (variableMapper == null) {
            variableMapper = new ProcessVariableMapper();
        }
        return variableMapper;
    }

    public FunctionMapper getFunctionMapper() {
        // method expressions not yet supported
        if (functionMapper == null) {
            functionMapper = new FunctionMapper() {
                public Method resolveFunction(String prefix, String localName) {
                    return null;
                }
            };
        }
        return functionMapper;
    }

    class ProcessVariableMapper extends VariableMapper {

        public ValueExpression resolveVariable(String variable) {
            return getValueExpressionMap().get(variable);
        }

        public ValueExpression setVariable(String variable, ValueExpression expression) {
            return getValueExpressionMap().put(variable, expression);
        }
    }

    private Map<String,ValueExpression> valueExpressionMap;

    protected Map<String,ValueExpression> getValueExpressionMap() {
        if (valueExpressionMap == null) {
            valueExpressionMap = new HashMap<>();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
            valueExpressionMap.put("process", new ValueExpressionLiteral(this.processInstance, Object.class));
            valueExpressionMap.put("masterRequestId", new ValueExpressionLiteral(getMasterRequestId(), String.class));
            valueExpressionMap.put("mdwHubUrl", new ValueExpressionLiteral(ApplicationContext.getMdwHubUrl(), String.class));
            valueExpressionMap.put("processInstanceId", new ValueExpressionLiteral(this.getProcessInstanceId(), String.class));
            valueExpressionMap.put("processName", new ValueExpressionLiteral(this.process.getName(), String.class));
            valueExpressionMap.put("variables", new ValueExpressionLiteral(this.getVariables() , Object.class));
            valueExpressionMap.put("props", new ValueExpressionLiteral(this.getPropertyAccessorMap(), Map.class));
            valueExpressionMap.put("env", new ValueExpressionLiteral(this.getEnvironmentAccessorMap(), Map.class));

            Map<String,Object> variables = getVariables();
            if (variables != null) {
                for (String varName : variables.keySet()) {
                    valueExpressionMap.put(varName, new ValueExpressionLiteral(variables.get(varName), Object.class));
                }
            }
        }
        return valueExpressionMap;
    }

    private Map<String,String> getPropertyAccessorMap() {
        return new HashMap<String,String>() {
            @Override
            public String get(Object key) {
                return PropertyManager.getProperty(key.toString());
            }
        };
    }

    private String propOr(String prop, String defaultValue) {
        return PropertyManager.getProperty(prop, defaultValue);
    }

    private Map<String,String> getEnvironmentAccessorMap() {
        return new HashMap<String,String>() {
            @Override
            public String get(Object key) {
                return System.getenv(key.toString());
            }
        };
    }

    public Map<String,String> getProps() {
        return getPropertyAccessorMap();
    }

    public Map<String,String> getEnv() {
        return getEnvironmentAccessorMap();
    }

    public String getMdwHubUrl() {
        return ApplicationContext.getMdwHubUrl();
    }

    public String getServicesUrl() {
        return ApplicationContext.getServicesUrl();
    }

    public String getMdwVersion() {
        return ApplicationContext.getMdwVersion();
    }

    public static boolean isExpression(String str) {
        if (str == null)
            return false;
        int start;
        return ((start = str.indexOf("#{")) != -1) && (start < str.indexOf('}')) ||
                ((start = str.indexOf("${")) != -1) && (start < str.indexOf('}'));
    }

    /**
     * Returns a variable value.  Key can be a var name or an expression.
     */
    public Object getValue(String key) {
        if (isExpression(key))
            return evaluate(key);
        else
            return getVariables().get(key);
    }

    public String getValueAsString(String key) {
        if (isExpression(key)) {
            return evaluateToString(key);
        }
        else {
            Object obj = getVariables().get(key);
            Variable var = getProcess().getVariable(key);
            if (var == null)
                throw new IllegalArgumentException("Variable not defined: " + key);
            if (obj == null)
                return "";
            if (obj instanceof Date)
                return ((Date)obj).toInstant().toString();  // dates always resolve to ISO time
            VariableTranslator translator = com.centurylink.mdw.translator.VariableTranslator
                    .getTranslator(getPackage(), var.getType());
            if (translator instanceof JavaObjectTranslator)
                return obj.toString();
            else if (translator instanceof DocumentReferenceTranslator)
                return ((DocumentReferenceTranslator)translator).realToString(obj);
            else
                return translator.toString(obj);
        }
    }

    public Object getValueForString(String varName, String strVal) {
        Variable var = getProcess().getVariable(varName);
        if (var == null)
            throw new IllegalArgumentException("Variable not defined: " + varName);
        VariableTranslator translator = com.centurylink.mdw.translator.VariableTranslator
                .getTranslator(getPackage(), var.getType());
        if (translator instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)translator).realToObject(strVal);
        else
            return translator.toObject(strVal);
    }

    private ServiceValuesAccess serviceValues;
    public ServiceValuesAccess getServiceValues() {
        if (serviceValues == null)
            serviceValues = new ServiceValuesAccess(this);
        return serviceValues;
    }

    public ProcessRuntimeContext(JSONObject json) throws JSONException {
        String procPath = json.getString("process");
        int slash = procPath.indexOf("/");
        if (slash > 0) {
            pkg = new Package();
            pkg.setName(procPath.substring(0, slash));
            process = new Process();
            process.setName(procPath.substring(slash + 1));
            process.setPackageName(pkg.getName());
        }
        else {
            process = new Process();
            process.setName(procPath);
        }
        this.processInstance = new ProcessInstance(json.getJSONObject("processInstance"));
        if (json.has("variables")) {
            variables = new HashMap<>();
            Map<String,String> varMap = JsonUtil.getMap(json.getJSONObject("variables"));
            for (String name : varMap.keySet()) {
                String val = varMap.get(name);
                getVariables().put(name, getValueForString(name, val));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("process", getPackage().getName() + "/" + getProcess().getName());
        json.put("processInstance", getProcessInstance().getJson());
        return json;
    }

    public String getJsonName() {
        return "processRuntimeContext";
    }

    private Map<Object,Object> runtimeAttributes;
    public Map<Object,Object> getRuntimeAttributes() {
        if (runtimeAttributes == null)
            runtimeAttributes = new HashMap<>();
        return runtimeAttributes;
    }

    private LogPersister logPersister;
    public LogPersister getLogPersister() { return logPersister; }
    public void setLogPersister(LogPersister logPersister) {
        this.logPersister = logPersister;
    }
}
