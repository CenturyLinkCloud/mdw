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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.dialogs.VariableValueDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableValue.StringDocTranslator;
import com.centurylink.mdw.plugin.designer.model.VariableValue.StringDocument;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.common.translator.impl.StringTranslator;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class VariableInstancesSection extends PropertySection implements IFilter {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private TableEditor tableEditor;

    private List<ColumnSpec> columnSpecs;

    private VariableInstanceContentProvider contentProvider;

    private VariableInstanceLabelProvider labelProvider;

    public void setSelection(WorkflowElement selection) {
        process = (WorkflowProcess) selection;
        tableEditor.setElement(process);
        List<VariableInstanceInfo> variables = process.getProcessInstance().getVariables();
        List<VariableInstanceInfo> uninitializedVariables = getTheUninitializedVariables();
        variables.addAll(uninitializedVariables);
        tableEditor.setValue(variables);
    }

    private List<VariableInstanceInfo> getTheUninitializedVariables() {
        HashMap<String, String> variableNamesMap = new HashMap<String, String>();
        List<String> variableInstances = new ArrayList<String>();
        for (VariableVO variable : process.getProcessVO().getVariables()) {
            variableNamesMap.put(variable.getVariableName(), variable.getVariableType());
        }
        for (VariableInstanceInfo varInst : process.getProcessInstance().getVariables()) {
            variableInstances.add(varInst.getName());
        }
        Set<String> variableNames = variableNamesMap.keySet();
        variableNames.removeAll(variableInstances);
        List<VariableInstanceInfo> uninitializedVariableInstances = new ArrayList<VariableInstanceInfo>();
        for (String varName : variableNames) {
            VariableInstanceInfo newVarInst = new VariableInstanceInfo();
            newVarInst.setName(varName);
            newVarInst.setType(variableNamesMap.get(varName));
            uninitializedVariableInstances.add(newVarInst);
        }
        return uninitializedVariableInstances;
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        tableEditor = new TableEditor(process, TableEditor.TYPE_TABLE);
        tableEditor.setReadOnly(true);
        tableEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                openDialog((VariableInstanceInfo) newValue);
            }
        });

        if (columnSpecs == null)
            columnSpecs = createColumnSpecs();
        tableEditor.setColumnSpecs(columnSpecs);

        if (contentProvider == null)
            contentProvider = new VariableInstanceContentProvider();
        tableEditor.setContentProvider(contentProvider);

        if (labelProvider == null)
            labelProvider = new VariableInstanceLabelProvider();
        tableEditor.setLabelProvider(labelProvider);

        tableEditor.render(composite);
    }

    private void openDialog(VariableInstanceInfo variableInstanceInfo) {
        Integer processStatus = process.getProcessInstance().getStatusCode();
        VariableTypeVO varType = getType(variableInstanceInfo);
        boolean readOnly = WorkStatus.STATUS_COMPLETED.equals(processStatus)
                || WorkStatus.STATUS_CANCELLED.equals(processStatus);
        if (varType.isJavaObjectType()) {
            try {
                // update based on object instance or from server
                varType = getDesignerProxy().getVariableInfo(variableInstanceInfo);
                if (!varType.isUpdateable())
                    readOnly = true;
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
        }
        try {
            String varValue = getDesignerProxy().getVariableValue(getShell(), variableInstanceInfo,
                    true);
            VariableValueDialog variableValueDialog = new VariableValueDialog(getShell(),
                    variableInstanceInfo, varType, varValue, readOnly);
            if (variableValueDialog.open() == Dialog.OK) {
                DesignerProxy designerProxy = process.getProject().getDesignerProxy();
                designerProxy.updateVariableValue(process, variableInstanceInfo,
                        variableValueDialog.getVariableValue().getValue());

                List<VariableInstanceInfo> variables = process.getProcessInstance().getVariables();
                List<VariableInstanceInfo> uninitializedVariables = getTheUninitializedVariables();
                variables.addAll(uninitializedVariables);
                tableEditor.setValue(variables);
            }
        }
        catch (Exception ex) {
            PluginMessages.uiMessage(ex, "Retrieve Variable", process.getProject(),
                    PluginMessages.VALIDATION_MESSAGE);
            return;
        }
    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec nameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Variable Name", "name");
        nameColSpec.width = 150;
        columnSpecs.add(nameColSpec);

        ColumnSpec instanceIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID",
                "instId");
        instanceIdColSpec.width = 80;
        columnSpecs.add(instanceIdColSpec);

        ColumnSpec valueColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Value", "value");
        valueColSpec.width = 300;
        columnSpecs.add(valueColSpec);

        ColumnSpec typeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Type", "type");
        typeColSpec.width = 200;
        columnSpecs.add(typeColSpec);

        return columnSpecs;
    }

    class VariableInstanceContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<VariableInstanceInfo> rows = (List<VariableInstanceInfo>) inputElement;
            Collections.sort(rows, new Comparator<VariableInstanceInfo>() {
                public int compare(VariableInstanceInfo vi1, VariableInstanceInfo vi2) {
                    if (vi1.getInstanceId() == null && vi2.getInstanceId() != null)
                        return +1;
                    else if (vi2.getInstanceId() == null && vi1.getInstanceId() != null)
                        return -1;
                    return vi1.getName().compareToIgnoreCase(vi2.getName());
                }
            });
            return rows.toArray(new VariableInstanceInfo[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class VariableInstanceLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            VariableInstanceInfo variable = (VariableInstanceInfo) element;

            switch (columnIndex) {
            case 0:
                return variable.getName();
            case 1:
                return variable.getInstanceId() == null ? "" : variable.getInstanceId().toString();
            case 2:
                return getValue(variable);
            case 3:
                return variable.getType();
            default:
                return null;
            }
        }
    }

    private VariableTypeVO getType(VariableInstanceInfo variableInstanceInfo) {
        VariableTypeVO varType = null;
        if (variableInstanceInfo.getType() == null)
            varType = new VariableTypeVO(new Long(-1), String.class.getName(),
                    StringTranslator.class.getName());
        else if (variableInstanceInfo.getType().equals(StringDocument.class.getName()))
            varType = new VariableTypeVO(new Long(-1), StringDocument.class.getName(),
                    StringDocTranslator.class.getName());
        else
            varType = process.getProject().getDataAccess()
                    .getVariableType(variableInstanceInfo.getType());

        return varType;
    }

    /**
     * Does not attempt to contact server.
     */
    private String getValue(final VariableInstanceInfo variableInstanceInfo) {
        try {
            return getDesignerProxy().getVariableValue(getShell(), variableInstanceInfo, false);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Retrieve Variable", process.getProject());
            return ex.getMessage();
        }
    }

    /**
     * Show this section for processes that are not stubs.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProcess))
            return false;

        WorkflowProcess processVersion = (WorkflowProcess) toTest;
        return !processVersion.isStub() && processVersion.hasInstanceInfo();
        /* && processVersion.getProcessInstanceInfo().getVariables( ) != null */
    }
}