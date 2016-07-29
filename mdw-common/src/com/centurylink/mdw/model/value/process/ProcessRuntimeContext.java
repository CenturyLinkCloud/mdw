/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.variable.XPathELResolver;
import com.sun.el.ExpressionFactoryImpl;
import com.sun.el.ValueExpressionLiteral;

public class ProcessRuntimeContext extends ELContext implements RuntimeContext {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected PackageVO packageVO;
    public PackageVO getPackage() { return packageVO; }

    protected ProcessVO processVO;
    public ProcessVO getProcess() { return processVO; }

    private ProcessInstanceVO processInstanceVO;
    public ProcessInstanceVO getProcessInstance() { return processInstanceVO; }

    /**
     * Purposely separate from processInstanceVO.getVariables().
     */
    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }

    public String getMasterRequestId() {
        return processInstanceVO.getMasterRequestId();
    }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String,String>();
            for (AttributeVO attribute : processVO.getAttributes()) {
                attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }
        return attributes;
    }

    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    public ProcessRuntimeContext(PackageVO packageVO, ProcessVO processVO, ProcessInstanceVO processInstanceVO) {
        this.packageVO = packageVO;
        this.processVO = processVO;
        this.processInstanceVO = processInstanceVO;
        this.variables = new HashMap<String,Object>();
    }

    public ProcessRuntimeContext(PackageVO packageVO, ProcessVO processVO, ProcessInstanceVO processInstanceVO, Map<String,Object> variables) {
        this.packageVO = packageVO;
        this.processVO = processVO;
        this.processInstanceVO = processInstanceVO;
        this.variables = variables;
    }

    public String getProperty(String name) {
        return packageVO.getProperty(name);
    }

    public Long getProcessId() {
        return processVO.getId();
    }

    public Long getProcessInstanceId() {
        return processInstanceVO.getId();
    }

    public void logInfo(String message) {
        logger.info(logtag(), message);
    }

    public void logDebug(String message) {
        logger.debug(logtag(), message);
    }

    public void logWarn(String message) {
        logger.warn(logtag(), message);
    }

    public void logSevere(String message) {
        logger.severe(logtag(), message);
    }

    public void logException(String msg, Exception e) {
        logger.exception(logtag(), msg, e);
    }

    public boolean isLogInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isLogDebugEnabled() {
        return logger.isDebugEnabled();
    }

    protected String logtag() {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(this.getProcessId());
        sb.append(".");
        sb.append(this.getProcessInstanceId());
        return sb.toString();
    }

    public String getCompletionCode() {
        return processInstanceVO.getCompletionCode();
    }

    public void setProcessCompletionCode(String code) {
        processInstanceVO.setCompletionCode(code);
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
                    if (base == null) {
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
            valueExpressionMap = new HashMap<String,ValueExpression>();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
            valueExpressionMap.put("masterRequestId", new ValueExpressionLiteral(getMasterRequestId(), String.class));
            valueExpressionMap.put("mdwWebUrl", new ValueExpressionLiteral(ApplicationContext.getMdwWebUrl(), String.class));
            valueExpressionMap.put("mdwTaskManagerUrl", new ValueExpressionLiteral(ApplicationContext.getTaskManagerUrl(), String.class));
            valueExpressionMap.put("processInstanceId", new ValueExpressionLiteral(this.getProcessInstanceId(), String.class));
            valueExpressionMap.put("processName", new ValueExpressionLiteral(this.processVO.getProcessName(), String.class));
            valueExpressionMap.put("process", new ValueExpressionLiteral(this.processInstanceVO, Object.class));
            valueExpressionMap.put("variables", new ValueExpressionLiteral(this.getVariables() , Object.class));
            valueExpressionMap.put("props", new ValueExpressionLiteral(this.getPropertyAccessorMap(), Map.class));

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

    public String getMdwHubUrl() {
        return ApplicationContext.getMdwHubUrl();
    }

    public String getAdminUrl() {
        return ApplicationContext.getAdminUrl();
    }

    public String getServicesUrl() {
        return ApplicationContext.getServicesUrl();
    }

    public String getTaskManagerUrl() {
        return ApplicationContext.getTaskManagerUrl();
    }

    public String getMdwWebUrl() {
        return ApplicationContext.getMdwWebUrl();
    }

    public String getMdwVersion() {
        return ApplicationContext.getMdwVersion();
    }

    public boolean isExpression(String str) {
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
}
