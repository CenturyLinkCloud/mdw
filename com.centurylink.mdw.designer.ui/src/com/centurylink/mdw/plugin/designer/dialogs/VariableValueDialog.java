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
package com.centurylink.mdw.plugin.designer.dialogs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.MappingEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class VariableValueDialog extends TrayDialog {
    private static final char ROW_DELIMITER = '~';
    private static final char COLUMN_DELIMITER = '=';

    private VariableValue variableValue;

    public VariableValue getVariableValue() {
        return variableValue;
    }

    private PropertyEditor valueEditor;

    private String tentativeValueForCollection;
    private String okButtonLabel;
    private boolean okInitiallyDisabled;

    public VariableValueDialog(Shell shell, VariableValue variableValue) {
        super(shell);
        this.variableValue = variableValue;
        if (variableValue.getType() != null && variableValue.getType().isJavaObjectType())
            variableValue.setReadOnly(true); // only updateable from var inst
                                             // section
    }

    public VariableValueDialog(Shell shell, VariableInstanceInfo variableInstanceInfo,
            VariableTypeVO type, String value, boolean readOnly) {
        super(shell);
        variableValue = new VariableValue(variableInstanceInfo, type, value);
        variableValue.setReadOnly(readOnly);
        okButtonLabel = "Save";
        okInitiallyDisabled = true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Variable Value");

        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setFont(new Font(nameLabel.getDisplay(), new FontData("Tahoma", 8, SWT.BOLD)));
        nameLabel.setText(variableValue.getName());

        final String type = variableValue.getType() == null ? "Unknown"
                : variableValue.getType().getVariableType();
        new Label(composite, SWT.NONE).setText("(" + type + ")");

        if (type.equals("java.lang.Boolean")) {
            valueEditor = new PropertyEditor(null, PropertyEditor.TYPE_CHECKBOX);
        }
        else if (type.equals("java.util.Date")) {
            valueEditor = new PropertyEditor(null, PropertyEditor.TYPE_DATE_PICKER);
            valueEditor.setWidth(100);
        }
        else if (type.equals("java.lang.Integer") || type.equals("java.lang.Long")) {
            valueEditor = new PropertyEditor(null, PropertyEditor.TYPE_TEXT);
            valueEditor.setWidth(250);
            valueEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    String stringValue = (String) newValue;
                    try {
                        long val = type.equals("java.lang.Long") ? Long.parseLong(stringValue)
                                : Integer.parseInt(stringValue);
                        variableValue.setValue(String.valueOf(val));
                    }
                    catch (NumberFormatException ex) {
                        String oldValue = variableValue.getValue();
                        valueEditor.setValue(oldValue);
                    }
                }
            });
        }
        else if (type.equals("java.net.URI")) {
            valueEditor = new PropertyEditor(null, PropertyEditor.TYPE_TEXT);
            valueEditor.setWidth(450);
            valueEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    try {
                        new URI((String) newValue);
                        valueEditor.setLabel("");
                    }
                    catch (URISyntaxException ex) {
                        valueEditor.setLabel(ex.getMessage());
                    }
                }
            });
        }
        else if (type.equals("java.lang.Integer[]") || type.equals("java.lang.Long[]")
                || type.equals("java.lang.String[]")) {
            valueEditor = new TableEditor(null, TableEditor.TYPE_TABLE);
            TableEditor tableEditor = (TableEditor) valueEditor;
            List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();
            ColumnSpec valueColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT,
                    type.substring(type.lastIndexOf('.') + 1, type.length() - 2) + " Values", type);
            columnSpecs.add(valueColSpec);
            tableEditor.setColumnSpecs(columnSpecs);
            tableEditor.setFillWidth(true);
            tableEditor.setRowDelimiter(ROW_DELIMITER);
            tableEditor.setModelUpdater(new CollectionModelUpdater(tableEditor));
        }
        else if (type.equals("java.util.Map")) {
            valueEditor = new MappingEditor(null);
            MappingEditor mappingEditor = (MappingEditor) valueEditor;
            List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();
            ColumnSpec keyColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Key", "key");
            keyColSpec.width = 150;
            columnSpecs.add(keyColSpec);
            ColumnSpec valColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Value", "value");
            valColSpec.width = 150;
            columnSpecs.add(valColSpec);
            mappingEditor.setColumnSpecs(columnSpecs);
            mappingEditor.setHeight(150);
            mappingEditor.setFillWidth(true);
            mappingEditor.setRowDelimiter(ROW_DELIMITER);
            mappingEditor.setColumnDelimiter(COLUMN_DELIMITER);
            mappingEditor.setContentProvider(mappingEditor.new DefaultContentProvider());
            mappingEditor
                    .setLabelProvider(((TableEditor) mappingEditor).new DefaultLabelProvider());
            mappingEditor.setCellModifier(((TableEditor) mappingEditor).new DefaultCellModifier());
            mappingEditor.setModelUpdater(new CollectionModelUpdater(mappingEditor));
        }
        else {
            valueEditor = new PropertyEditor(null, PropertyEditor.TYPE_TEXT);
            valueEditor.setMultiLine(true);
            valueEditor.setWidth(500);
            valueEditor.setHeight(500);
        }
        valueEditor.setReadOnly(variableValue.isReadOnly());
        valueEditor.render(composite);
        valueEditor.setValue(variableValue.getValue());
        if (!variableValue.isReadOnly()) {
            valueEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    Button okButton = getButton(Dialog.OK);
                    if (okButton != null) // else not editable
                        okButton.setEnabled(true);
                }
            });
        }
        if (isCollectionType(type))
            tentativeValueForCollection = variableValue.getValue();

        if (type.equals("java.lang.Boolean"))
            ((Button) valueEditor.getWidget()).setText("(checked = true)");
        else if (type.equals("java.lang.Object"))
            valueEditor.setValue(
                    variableValue.getValue() == null ? null : variableValue.getValue().toString());
        else if (type.equals("java.lang.Integer[]") || type.equals("java.lang.Long[]")) {
            if (!variableValue.isReadOnly()) {
                TableEditor tableEditor = (TableEditor) valueEditor;
                final CellEditor cellEditor = tableEditor.getTableViewer().getCellEditors()[0];
                cellEditor.setValidator(new ICellEditorValidator() {
                    private String oldValue = "";

                    public String isValid(Object value) {
                        String message = validateValue((String) value,
                                variableValue.getType().getVariableType());
                        if (message != null)
                            cellEditor.setValue(oldValue);
                        else
                            oldValue = (String) value;
                        return null;
                    }
                });
            }
        }
        else if (type.equals("java.util.Map")) {
            if (!variableValue.isReadOnly()) {
                MappingEditor mappingEditor = (MappingEditor) valueEditor;
                final CellEditor valueCellEditor = mappingEditor.getTableViewer()
                        .getCellEditors()[1];
                valueCellEditor.setValidator(new ICellEditorValidator() {
                    public String isValid(Object value) {
                        String message = validateValue((String) value,
                                variableValue.getType().getVariableType());
                        if (message != null)
                            getButton(Dialog.OK).setEnabled(false);
                        return null;
                    }
                });
            }
        }

        return composite;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control buttonBar = super.createButtonBar(parent);
        if (okButtonLabel != null)
            getButton(Dialog.OK).setText(okButtonLabel);
        if (okInitiallyDisabled)
            getButton(Dialog.OK).setEnabled(false);
        if (variableValue.isReadOnly()) {
            getButton(Dialog.OK).setVisible(false);
            getButton(Dialog.CANCEL).setText("Close");
        }
        return buttonBar;
    }

    @Override
    protected void okPressed() {
        String type = variableValue.getType() == null ? null
                : variableValue.getType().getVariableType();
        if (isCollectionType(type))
            variableValue.setValue(tentativeValueForCollection);
        else
            variableValue.setValue(valueEditor.getValue());

        super.okPressed();
    }

    private boolean isCollectionType(String type) {
        return "java.lang.Integer[]".equals(type) || "java.lang.Long[]".equals(type)
                || "java.lang.String[]".equals(type) || "java.util.Map".equals(type);
    }

    private String validateValue(String value, String type) {
        if (type.equals("java.lang.Integer[]")) {
            try {
                Integer.parseInt(value);
            }
            catch (NumberFormatException ex) {
                return "Invalid Integer Value: '" + value + "'";
            }
        }
        else if (type.equals("java.lang.Long[]")) {
            try {
                Long.parseLong(value);
            }
            catch (NumberFormatException ex) {
                return "Invalid Long Value: '" + value + "'";
            }
        }
        else if (type.equals("java.util.Map")) {
            if (value == null)
                return "Value entry required for java.util.Map";
        }
        return null;
    }

    class CollectionModelUpdater extends TableEditor.DefaultModelUpdater {
        private TableEditor tableEditor;

        public CollectionModelUpdater(TableEditor tableEditor) {
            tableEditor.super();
            this.tableEditor = tableEditor;
        }

        @Override
        public Object create() {
            String type = variableValue.getType().getVariableType();
            if (type.equals("java.lang.Integer[]") || type.equals("java.lang.Long[]"))
                return tableEditor.new DefaultRowImpl(new String[] { "0" });
            else if (type.equals("java.util.Map"))
                return tableEditor.new DefaultRowImpl(new String[] { "New Key", "New Value" });
            else
                return tableEditor.new DefaultRowImpl(new String[] { "New Value" });
        }

        @SuppressWarnings("rawtypes")
        public void updateModelValue(List tableValue) {
            String serialized = "";
            for (int i = 0; i < tableValue.size(); i++) {
                String firstColStringVal = (String) ((DefaultRowImpl) tableValue.get(i))
                        .getColumnValue(0);

                if (firstColStringVal == null || firstColStringVal.trim().length() == 0)
                    continue;
                if (!(validateValue(firstColStringVal,
                        variableValue.getType().getVariableType()) == null))
                    return;
                serialized += firstColStringVal;

                if (variableValue.getType().getVariableType().equals("java.util.Map")) {
                    String secondColStringVal = (String) ((DefaultRowImpl) tableValue.get(i))
                            .getColumnValue(1);
                    if (!(validateValue(secondColStringVal,
                            variableValue.getType().getVariableType()) == null))
                        return;
                    serialized += COLUMN_DELIMITER + secondColStringVal;
                }

                if (i < tableValue.size() - 1)
                    serialized += ROW_DELIMITER;
            }

            getButton(Dialog.OK).setEnabled(true);
            tentativeValueForCollection = serialized;
        }
    }
}