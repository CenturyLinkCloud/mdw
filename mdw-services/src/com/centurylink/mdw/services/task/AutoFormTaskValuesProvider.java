/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.PropertyNotFoundException;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.Value.Display;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.observer.task.TaskValuesProvider;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;

public class AutoFormTaskValuesProvider implements TaskValuesProvider {

    public Map<String,Value> collect(TaskRuntimeContext runtimeContext) {
        Map<String,Value> values = new HashMap<String,Value>();

        for (Value value : getDefinedValues(runtimeContext)) {
            if (value.getDisplay() != null) {
                if (value.isExpression()) {
                    String str = runtimeContext.evaluateToString(value.getName());
                    if (str != null && !str.isEmpty())
                      value.setValue(str);
                }
                else {
                    Variable var = runtimeContext.getProcess().getVariable(value.getName());
                    if (var != null) {
                        com.centurylink.mdw.variable.VariableTranslator vt = VariableTranslator.getTranslator(runtimeContext.getPackage(), value.getType());
                        if (VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), value.getType())) {
                            Object docVal = runtimeContext.getVariables().get(value.getName());
                            if (docVal != null)
                              value.setValue(((DocumentReferenceTranslator)vt).realToString(docVal));
                        }
                        else {
                            Object obj = runtimeContext.getVariables().get(value.getName());
                            if (obj != null)
                                value.setValue(vt.toString(obj));
                        }
                    }
                }
                values.put(value.getName(), value);
            }
        }
        return values;
    }

    public void apply(TaskRuntimeContext runtimeContext, Map<String,String> values) throws ServiceException {
        List<Value> definedValues = getDefinedValues(runtimeContext);
        List<String> readOnly = new ArrayList<String>();
        for (Value v : definedValues) {
            if (v.getDisplay() == Display.ReadOnly)
                readOnly.add(v.getName());
        }
        for (String name : values.keySet()) {
            if (readOnly.contains(name))
                throw new ServiceException(400, "Read-only value: " + name);
        }
        for (String name : values.keySet()) {
            if (runtimeContext.isExpression(name)) {
                try {
                    runtimeContext.set(name, values.get(name));
                }
                catch (PropertyNotFoundException ex) {
                    throw new ServiceException(400, "Variable not found for expression: " + name);
                }
            }
            else {
                Variable var = runtimeContext.getProcess().getVariable(name);
                if (var == null) {
                    throw new ServiceException(400, "Variable not found: " + name);
                }
                else {
                    String type = var.getVariableType();
                    com.centurylink.mdw.variable.VariableTranslator vt = VariableTranslator.getTranslator(runtimeContext.getPackage(), type);
                    if (VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), type)) {
                        Object newValue = ((DocumentReferenceTranslator)vt).realToObject(values.get(name));
                        runtimeContext.getVariables().put(name, newValue);
                    }
                    else {
                        Object newValue = vt.toObject(values.get(name));
                        runtimeContext.getVariables().put(name, newValue);
                    }
                }
            }
        }
    }

    /**
     * Minus runtime values.
     */
    protected List<Value> getDefinedValues(TaskRuntimeContext runtimeContext) {
        List<Value> values = new ArrayList<Value>();
        String varAttr = runtimeContext.getTaskAttribute(TaskAttributeConstant.VARIABLES);
        if (!StringHelper.isEmpty(varAttr)) {
            List<String[]> parsed = StringHelper.parseTable(varAttr, ',', ';', 5);
            for (String[] one : parsed) {
                String name = one[0];
                Value value = new Value(name);
                if (one[1] != null && !one[1].isEmpty())
                    value.setLabel(one[1]);
                value.setDisplay(Value.getDisplay(one[2]));
                if (one[3] != null && !one[3].isEmpty())
                    value.setSequence(Integer.parseInt(one[3]));
                if (one[4] != null && !one[4].isEmpty())
                    value.setIndexKey(one[4]);
                if (value.isExpression()) {
                    value.setType(String.class.getName());
                }
                else {
                    Variable var = runtimeContext.getProcess().getVariable(name);
                    if (var != null)
                        value.setType(var.getVariableType());
                }
                values.add(value);
            }
        }
        return values;
    }
}
