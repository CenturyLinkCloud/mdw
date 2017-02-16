/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class VariablesSection extends PropertySection implements IFilter {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private TableEditor tableEditor;
    private List<ColumnSpec> columnSpecs;

    private PropertyEditor variablesHelpPropertyEditor;

    private VariableContentProvider contentProvider;
    private VariableLabelProvider labelProvider;
    private VariableCellModifier cellModifier;
    private VariableModelUpdater modelUpdater;

    private Map<Integer, VariableTypeVO> variableTypes;

    public void setSelection(WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        tableEditor.setElement(process);
        tableEditor.setValue(process.getVariables());
        tableEditor.setEditable(!process.isReadOnly());

        variablesHelpPropertyEditor.setValue("/MDWHub/doc/variable.html");
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        tableEditor = new TableEditor(process, TableEditor.TYPE_TABLE);

        if (columnSpecs == null)
            columnSpecs = createColumnSpecs();
        tableEditor.setColumnSpecs(columnSpecs);

        if (contentProvider == null)
            contentProvider = new VariableContentProvider();
        tableEditor.setContentProvider(contentProvider);

        if (labelProvider == null)
            labelProvider = new VariableLabelProvider();
        tableEditor.setLabelProvider(labelProvider);

        if (cellModifier == null)
            cellModifier = new VariableCellModifier();
        tableEditor.setCellModifier(cellModifier);

        if (modelUpdater == null)
            modelUpdater = new VariableModelUpdater();
        tableEditor.setModelUpdater(modelUpdater);

        tableEditor.render(composite);

        // variables help link
        variablesHelpPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_LINK);
        variablesHelpPropertyEditor.setLabel("Process Variables Help");
        variablesHelpPropertyEditor.render(composite);
    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec nameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Variable Name", "name");
        nameColSpec.width = 150;
        columnSpecs.add(nameColSpec);

        ColumnSpec typeColSpec = new ColumnSpec(PropertyEditor.TYPE_COMBO, "Type", "type");
        typeColSpec.width = 200;
        typeColSpec.readOnly = true;
        typeColSpec.options = getVariableTypeNames();
        columnSpecs.add(typeColSpec);

        ColumnSpec modeColSpec = new ColumnSpec(PropertyEditor.TYPE_COMBO, "Mode", "mode");
        modeColSpec.width = 85;
        modeColSpec.readOnly = true;
        modeColSpec.options = VariableVO.VariableCategories;
        columnSpecs.add(modeColSpec);

        ColumnSpec labelColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "UI Label", "label");
        labelColSpec.width = 100;
        columnSpecs.add(labelColSpec);

        ColumnSpec sequenceColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Seq.",
                "sequenceNumber");
        sequenceColSpec.width = 35;
        columnSpecs.add(sequenceColSpec);

        return columnSpecs;
    }

    private String[] getVariableTypeNames() {
        List<String> names = new ArrayList<String>();
        for (VariableTypeVO variableTypeVO : getVariableTypes().values()) {
            names.add(variableTypeVO.getVariableType());
        }
        return names.toArray(new String[0]);
    }

    private Map<Integer, VariableTypeVO> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new TreeMap<Integer, VariableTypeVO>();
            List<VariableTypeVO> variableTypeVOs = getDataAccess().getVariableTypes(false);
            Collections.sort(variableTypeVOs, new Comparator<VariableTypeVO>() {
                public int compare(VariableTypeVO vt1, VariableTypeVO vt2) {
                    if (vt1.getVariableType().equals(String.class.getName())
                            && !vt2.getVariableType().equals(String.class.getName()))
                        return -1;
                    if (vt2.getVariableType().equals(String.class.getName())
                            && !vt1.getVariableType().equals(String.class.getName()))
                        return +1;
                    if (vt1.getVariableType().startsWith("java.")
                            && !vt2.getVariableType().startsWith("java."))
                        return -1;
                    if (vt2.getVariableType().startsWith("java.")
                            && !vt1.getVariableType().startsWith("java."))
                        return +1;
                    return vt1.getVariableType().compareToIgnoreCase(vt2.getVariableType());
                }
            });
            for (int i = 0; i < variableTypeVOs.size(); i++) {
                VariableTypeVO variableTypeVO = (VariableTypeVO) variableTypeVOs.get(i);
                variableTypes.put(new Integer(i), variableTypeVO);
            }
        }
        return variableTypes;
    }

    class VariableContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<VariableVO> rows = (List<VariableVO>) inputElement;
            Collections.sort(rows, new Comparator<VariableVO>() {
                public int compare(VariableVO v1, VariableVO v2) {
                    return v1.getName().compareToIgnoreCase(v2.getName());
                }
            });
            return rows.toArray(new VariableVO[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class VariableLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            VariableVO variableVO = (VariableVO) element;

            switch (columnIndex) {
            case 0:
                return variableVO.getVariableName();
            case 1:
                return variableVO.getVariableType();
            case 2:
                return lookupVariableMode(variableVO.getVariableCategory());
            case 3:
                return variableVO.getVariableReferredAs();
            case 4:
                return variableVO.getDisplaySequence() == null ? null
                        : variableVO.getDisplaySequence().toString();
            default:
                return null;
            }
        }

        private String lookupVariableMode(Integer cat) {
            if (cat == null || cat.intValue() < 0)
                return VariableVO.VariableCategories[VariableVO.CAT_LOCAL];
            else
                return VariableVO.VariableCategories[cat.intValue()];
        }
    }

    class VariableCellModifier extends TableEditor.DefaultCellModifier {
        VariableCellModifier() {
            tableEditor.super();
        }

        public Object getValue(Object element, String property) {
            VariableVO variableVO = (VariableVO) element;
            int colIndex = getColumnIndex(property);
            switch (colIndex) {
            case 0:
                return variableVO.getVariableName();
            case 1:
                String varType = variableVO.getVariableType();
                for (Integer idx : variableTypes.keySet()) {
                    if (varType.equals(variableTypes.get(idx).getVariableType()))
                        return idx;
                }
            case 2:
                return variableVO.getVariableCategory();
            case 3:
                return variableVO.getVariableReferredAs() == null ? ""
                        : variableVO.getVariableReferredAs();
            case 4:
                return variableVO.getDisplaySequence() == null ? ""
                        : variableVO.getDisplaySequence().toString();
            default:
                return null;
            }
        }

        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            VariableVO variableVO = (VariableVO) item.getData();
            int colIndex = getColumnIndex(property);
            switch (colIndex) {
            case 0:
                variableVO.setVariableName(((String) value).trim());
                break;
            case 1:
                variableVO.setVariableType(variableTypes.get((Integer) value).getVariableType());
                break;
            case 2:
                variableVO.setVariableCategory((Integer) value);
                break;
            case 3:
                String stringVal = value == null ? null : ((String) value).trim();
                variableVO.setVariableReferredAs(stringVal);
                break;
            case 4:
                Integer intVal = null;
                if (value != null) {
                    try {
                        intVal = new Integer((String) value);
                    }
                    catch (NumberFormatException ex) {
                        intVal = variableVO.getDisplaySequence();
                    }
                }
                variableVO.setDisplaySequence(intVal);
                break;
            default:
            }
            tableEditor.getTableViewer().update(variableVO, null);
            modelUpdater.updateModelValue(tableEditor.getTableValue());
            tableEditor.fireValueChanged(tableEditor.getTableValue());
        }
    }

    class VariableModelUpdater implements TableEditor.TableModelUpdater {
        public Object create() {
            VariableVO variableVO = new VariableVO();
            variableVO.setVariableName("NewVariable");
            variableVO.setVariableType(
                    getVariableTypes().values().iterator().next().getVariableType());
            variableVO.setVariableCategory(new Integer(0));
            process.fireDirtyStateChanged(true);
            return variableVO;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void updateModelValue(List tableValue) {
            List<VariableVO> variableVOs = (List<VariableVO>) tableValue;
            process.setVariables(variableVOs);
            process.fireDirtyStateChanged(true);
        }
    }

    /**
     * Show this section for processes that are not stubs.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProcess))
            return false;

        WorkflowProcess processVersion = (WorkflowProcess) toTest;

        return !processVersion.isStub() && !processVersion.hasInstanceInfo()
                && !(processVersion.getVariables() == null);
    }
}