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
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.editor.WorkflowAssetEditor;

public class OsgiAdapterDesignSection extends PropertySection implements IFilter {
    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity a) {
        this.activity = a;
    }

    private WorkflowAssetEditor interfaceSelector;
    private PropertyEditor methodDropdown;
    private TableEditor paramsTable;
    private PropertyEditor resultDropdown;
    private PropertyEditor helpLink;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;

        interfaceSelector.setElement(activity);
        interfaceSelector.setValue(activity.getAttribute(WorkAttributeConstant.SERVICE_INTERFACE));

        methodDropdown.setElement(activity);
        methodDropdown.setValue(activity.getAttribute(WorkAttributeConstant.SERVICE_METHOD));

        resultDropdown.setElement(activity);
        resultDropdown.setValue(activity.getAttribute(WorkAttributeConstant.SERVICE_RESULT));
        resultDropdown.setFocus();

        paramsTable.setElement(activity);
        paramsTable.setValue(activity.getAttribute(WorkAttributeConstant.SERVICE_PARAMETERS));

        helpLink.setElement(activity);
        helpLink.setValue("/MDWHub/doc/OsgiServiceAdapter.html");

    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        activity = (Activity) selection;

        interfaceSelector = new WorkflowAssetEditor(activity,
                WorkAttributeConstant.SERVICE_INTERFACE, getAssetTypes());
        interfaceSelector.setLabel("Service Interface");
        interfaceSelector.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute(WorkAttributeConstant.SERVICE_INTERFACE, (String) newValue);
                methodDropdown.setValueOptions(getServiceMethodOptions());
            }
        });
        interfaceSelector.render(composite);

        methodDropdown = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
        methodDropdown.setLabel("Method");
        methodDropdown.setWidth(277);
        methodDropdown.setValueOptions(getServiceMethodOptions());
        methodDropdown.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute(WorkAttributeConstant.SERVICE_METHOD, (String) newValue);
                paramsTable.setValue(getParamsTableValue());
            }
        });
        methodDropdown.render(composite);

        resultDropdown = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
        resultDropdown.setLabel("Result Variable");
        resultDropdown.setWidth(277);
        resultDropdown.setValueOptions(getResultVariableOptions());
        resultDropdown.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute(WorkAttributeConstant.SERVICE_RESULT, (String) newValue);
            }
        });
        resultDropdown.render(composite);

        paramsTable = new TableEditor(activity, TableEditor.TYPE_TABLE);
        paramsTable.setColumnSpecs(getInputTableColumns());
        paramsTable.setColumnDelimiter('=');
        paramsTable.setLabel("Parameters");
        paramsTable.setAttributeName(WorkAttributeConstant.SERVICE_PARAMETERS);
        paramsTable.setHorizontalSpan(3);
        paramsTable.render(composite, false);

        helpLink = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
        helpLink.setLabel("OSGi Service Adapter Help");
        helpLink.render(composite);
    }

    private List<String> getAssetTypes() {
        List<String> assetTypes = new ArrayList<String>();
        assetTypes.add(RuleSetVO.JAVA);
        assetTypes.add(RuleSetVO.GROOVY);
        return assetTypes;
    }

    private List<ColumnSpec> getInputTableColumns() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();
        ColumnSpec paramColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "name");
        paramColSpec.readOnly = true;
        paramColSpec.width = 300;
        columnSpecs.add(paramColSpec);
        ColumnSpec valueColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Value", "value");
        valueColSpec.width = 300;
        columnSpecs.add(valueColSpec);
        return columnSpecs;
    }

    private List<String> getResultVariableOptions() {
        List<String> options = activity.getProcess().getDocRefVariableNames();
        // add string variables
        for (VariableVO var : activity.getProcess().getVariables()) {
            if (String.class.getName().equals(var.getVariableType()))
                options.add(var.getName());
        }
        Collections.sort(options);
        return options;
    }

    private List<String> getServiceMethodOptions() {
        List<String> options = new ArrayList<String>();
        WorkflowAsset asset = (WorkflowAsset) interfaceSelector.getWorkflowAsset();
        if (asset != null) {
            try {
                ICompilationUnit unit = getCompilationUnit(asset);
                if (unit != null) {
                    for (IType type : unit.getAllTypes()) {
                        for (IMethod method : type.getMethods()) {
                            String option = method.getElementName() + "(";
                            for (int i = 0; i < method.getNumberOfParameters(); i++) {
                                String paramType = method.getParameterTypes()[i];
                                option += paramType.substring(1, paramType.length() - 1) + " "
                                        + method.getParameterNames()[i];
                                if (i < method.getNumberOfParameters() - 1)
                                    option += ", ";
                            }
                            option += ")";
                            options.add(option);
                        }
                    }
                }
                else {
                    options.add("Type must be opened to read methods");
                }
            }
            catch (JavaModelException ex) {
                PluginMessages.uiError(ex, "Service Methods", activity.getProject());
            }
        }
        else {
            options.add("");
        }

        return options;
    }

    private List<DefaultRowImpl> getParamsTableValue() {
        List<DefaultRowImpl> rows = new ArrayList<DefaultRowImpl>();
        String methodSig = methodDropdown.getValue();
        if (methodSig != null && !methodSig.isEmpty()) {
            int parenOpen = methodSig.indexOf('(');
            String params = methodSig.substring(parenOpen + 1, methodSig.length() - 1);
            for (String p : params.split(",")) {
                p = p.trim();
                int space = p.indexOf(' ');
                String paramName = p.substring(space + 1);
                String paramType = p.substring(0, space);
                String[] colVals = new String[2];
                colVals[0] = paramName + " (" + paramType + ")";
                colVals[1] = "";
                DefaultRowImpl row = paramsTable.new DefaultRowImpl(colVals);
                rows.add(row);
            }
        }

        return rows;
    }

    private ICompilationUnit getCompilationUnit(WorkflowAsset asset) throws JavaModelException {
        IJavaProject javaProj = activity.getProject().getJavaProject();
        IPackageFragment javaPkg = (IPackageFragment) javaProj
                .findPackageFragment(asset.getTempFolder().getFullPath());
        if (javaPkg.exists()) {
            for (ICompilationUnit compUnit : javaPkg.getCompilationUnits()) {
                if (compUnit.getElementName().equals(asset.getName())) {
                    return compUnit;
                }
            }
        }
        return null;
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (activity.isForProcessInstance())
            return false;

        return activity.isOsgiAdapter();
    }

}
