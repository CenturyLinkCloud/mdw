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
package com.centurylink.mdw.plugin.variables;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.dialogs.VariableValueDialog;
import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class VariableValuesTableContainer {
    public VariableValuesTableContainer() {
        createColumnSpecs();
    }

    private List<ColumnSpec> columnSpecs;

    public List<ColumnSpec> getColumnSpecs() {
        return columnSpecs;
    }

    private String[] columnProps;

    public String[] getColumnProps() {
        return columnProps;
    }

    private Table table;

    public Table getTable() {
        return table;
    }

    private TableViewer tableViewer;

    public TableViewer getTableViewer() {
        return tableViewer;
    }

    private Composite parent;

    private Shell getShell() {
        return parent.getShell();
    }

    public void create(Composite parent) {
        this.parent = parent;
        createTable(parent);
        createTableViewer();
    }

    public void setInput(List<VariableValue> variableValues) {
        tableViewer.setInput(variableValues);
    }

    private void createColumnSpecs() {
        columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec inputVarColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Variable",
                "variable");
        inputVarColSpec.width = 150;
        inputVarColSpec.readOnly = true;
        columnSpecs.add(inputVarColSpec);

        ColumnSpec varTypeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Type", "type");
        varTypeColSpec.width = 150;
        varTypeColSpec.readOnly = true;
        columnSpecs.add(varTypeColSpec);

        ColumnSpec valueColSpec = new ColumnSpec(PropertyEditor.TYPE_DIALOG, "Value", "value");
        valueColSpec.width = 200;
        columnSpecs.add(valueColSpec);

        columnProps = new String[columnSpecs.size()];
        for (int i = 0; i < columnSpecs.size(); i++) {
            columnProps[i] = columnSpecs.get(i).property;
        }
    }

    private void createTable(Composite parent) {
        int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;

        table = new Table(parent, style);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        gridData.verticalIndent = 3;
        gridData.verticalAlignment = SWT.FILL;
        gridData.heightHint = 150;
        table.setLayoutData(gridData);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec colSpec = columnSpecs.get(i);

            int styles = SWT.LEFT;
            if (colSpec.readOnly)
                style = style | SWT.READ_ONLY;
            TableColumn column = new TableColumn(table, styles, i);
            column.setText(colSpec.label);
            column.setWidth(colSpec.width);
            column.setResizable(colSpec.resizable);
        }

        table.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                int tableWidth = table.getBounds().width;
                int cumulative = 0;
                TableColumn[] tableColumns = table.getColumns();
                for (int i = 0; i < tableColumns.length; i++) {
                    if (i == tableColumns.length - 1)
                        tableColumns[i].setWidth(tableWidth - cumulative - 5);
                    cumulative += tableColumns[i].getWidth();
                }
            }
        });

        table.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                VariableValue varVal = (VariableValue) e.item.getData();
                String originalValue = varVal.getValue();
                VariableValueDialog varValDlg = new VariableValueDialog(getShell(), varVal);
                varValDlg.setHelpAvailable(false);
                if (varValDlg.open() == Dialog.OK && !originalValue.equals(varVal.getValue())) {
                    varVal.setValue(varValDlg.getVariableValue().getValue());
                    tableViewer.update(varVal, null);
                    fireDirtyStateChange(true);
                }
                else {
                    varVal.setValue(originalValue);
                }
            }
        });
    }

    private void createTableViewer() {
        tableViewer = new TableViewer(table);
        tableViewer.setUseHashlookup(true);

        tableViewer.setColumnProperties(columnProps);

        CellEditor[] editors = new CellEditor[columnSpecs.size()];
        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec colSpec = columnSpecs.get(i);
            CellEditor cellEditor = null;
            if (colSpec.type.equals(PropertyEditor.TYPE_TEXT)) {
                cellEditor = new TextCellEditor(table);
            }
            else if (colSpec.type.equals(PropertyEditor.TYPE_DIALOG)) {
                cellEditor = new VariableCellEditor(table);
            }

            editors[i] = cellEditor;
        }
        tableViewer.setCellEditors(editors);
        tableViewer.setCellModifier(new VariableCellModifier());
        tableViewer.setLabelProvider(new VariablesLabelProvider());
        tableViewer.setContentProvider(new VariablesContentProvider());
    }

    class VariableCellModifier implements ICellModifier {
        public boolean canModify(Object element, String property) {
            ColumnSpec colSpec = columnSpecs.get(getColumnIndex(property));
            return !colSpec.readOnly;
        }

        public Object getValue(Object element, String property) {
            VariableValue variableValue = (VariableValue) element;
            int colIndex = getColumnIndex(property);
            String colType = columnSpecs.get(colIndex).type;
            if (colType.equals(PropertyEditor.TYPE_DIALOG)) {
                return variableValue;
            }
            else if (colType.equals(PropertyEditor.TYPE_TEXT)) {
                switch (colIndex) {
                case 0:
                    return variableValue.getName();
                case 1:
                    return variableValue.getType().getVariableType();
                }
            }
            return null;
        }

        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            tableViewer.update(item.getData(), new String[] { property });
            fireDirtyStateChange(true);
        }

        protected int getColumnIndex(String property) {
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec colSpec = columnSpecs.get(i);
                if (colSpec.property.equals(property)) {
                    return i;
                }
            }
            return -1;
        }
    }

    class VariablesLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            VariableValue variableValue = (VariableValue) element;
            ColumnSpec colspec = columnSpecs.get(columnIndex);
            if (colspec.type.equals(PropertyEditor.TYPE_DIALOG)) {
                return variableValue.getValue();
            }
            else if (colspec.type.equals(PropertyEditor.TYPE_TEXT)) {
                switch (columnIndex) {
                case 0:
                    return variableValue.getName();
                case 1:
                    return variableValue.getType() == null ? null
                            : variableValue.getType().getVariableType();
                }
            }
            return null;
        }
    }

    class VariablesContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<VariableValue> variables = (List<VariableValue>) inputElement;
            return (VariableValue[]) variables.toArray(new VariableValue[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class VariableCellEditor extends DialogCellEditor {
        public VariableCellEditor(Composite parent) {
            super(parent);
        }

        @Override
        protected Object openDialogBox(Control cellEditorWindow) {
            VariableValueDialog varValDlg = new VariableValueDialog(getShell(),
                    (VariableValue) this.getValue());
            varValDlg.setHelpAvailable(false);
            if (varValDlg.open() == Dialog.OK)
                return varValDlg.getVariableValue();
            else
                return this.getValue();
        }

        protected Control createContents(Composite cell) {
            Control contents = super.createContents(cell);
            contents.addListener(SWT.MouseDoubleClick, new Listener() {
                public void handleEvent(Event e) {
                    VariableValue varVal = (VariableValue) getValue();
                    String originalValue = varVal.getValue();
                    VariableValueDialog varValDlg = new VariableValueDialog(getShell(), varVal);
                    varValDlg.setHelpAvailable(false);
                    if (varValDlg.open() == Dialog.OK && (originalValue == null
                            || !originalValue.equals(varVal.getValue()))) {
                        varVal.setValue(varValDlg.getVariableValue().getValue());
                        tableViewer.update(varVal, null);
                        fireDirtyStateChange(true);
                    }
                    else {
                        varVal.setValue(originalValue);
                    }
                }
            });
            return contents;
        }
    }

    private List<DirtyStateListener> dirtyStateListeners = new ArrayList<DirtyStateListener>();

    public void addDirtyStateListener(DirtyStateListener dsl) {
        dirtyStateListeners.add(dsl);
    }

    public void removeDirtyStateListener(DirtyStateListener dsl) {
        dirtyStateListeners.remove(dsl);
    }

    public void fireDirtyStateChange(boolean dirty) {
        for (DirtyStateListener listener : dirtyStateListeners)
            listener.dirtyStateChanged(dirty);
    }

}
