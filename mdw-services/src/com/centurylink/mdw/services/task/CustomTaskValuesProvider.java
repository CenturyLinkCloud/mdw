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
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.PropertyNotFoundException;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.Value.Display;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.observer.task.TaskValuesProvider;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;

public class CustomTaskValuesProvider implements TaskValuesProvider {

    @Override
    public Map<String,Value> collect(TaskRuntimeContext runtimeContext) {
        Map<String,Value> values = new HashMap<String,Value>();

        for (Value value : getDefinedValues(runtimeContext)) {
            if (!Display.Hidden.equals(value.getDisplay())) {
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
            if (TaskRuntimeContext.isExpression(name)) {
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
                    String type = var.getType();
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

    protected List<Value> getDefinedValues(TaskRuntimeContext runtimeContext) {
        List<Value> values = new ArrayList<Value>();
        for (Variable var : runtimeContext.getProcess().getVariables()) {
            values.add(var.toValue());
        }
        return values;
    }
}
