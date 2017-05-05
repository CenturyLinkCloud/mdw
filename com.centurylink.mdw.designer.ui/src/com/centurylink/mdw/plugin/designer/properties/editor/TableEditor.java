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
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.json.JSONArray;

import com.centurylink.jface.viewers.TreeComboCellEditor;
import com.centurylink.jface.viewers.TreeComboCellEditor.SelectionModifier;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.swt.widgets.CTreeCombo;
import com.centurylink.swt.widgets.CTreeComboItem;
import com.qwest.mbeng.MbengNode;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class TableEditor extends PropertyEditor {
    public static final String TYPE_TABLE = "TABLE";
    public static final String TYPE_MAPPING = "MAPPING";

    private char rowDelimiter = ';';

    public char getRowDelimiter() {
        return rowDelimiter;
    }

    public void setRowDelimiter(char c) {
        this.rowDelimiter = c;
    }

    private char columnDelimiter = ',';

    public char getColumnDelimiter() {
        return columnDelimiter;
    }

    public void setColumnDelimiter(char c) {
        this.columnDelimiter = c;
    }

    public int horizontalSpan;

    public int getHorizontalSpan() {
        return horizontalSpan;
    }

    public void setHorizontalSpan(int span) {
        this.horizontalSpan = span;
    }

    private List<ColumnSpec> columnSpecs;
    private String[] columnProps;
    private TableViewer tableViewer;

    public TableViewer getTableViewer() {
        return tableViewer;
    }

    private Composite buttonComposite;
    private Button addButton;

    public Button getAddButton() {
        return addButton;
    }

    private Button deleteButton;

    public Button getDeleteButton() {
        return deleteButton;
    }

    private boolean fillWidth; // if true then last column will expand to fill
                               // extra space

    public boolean isFillWidth() {
        return fillWidth;
    }

    public void setFillWidth(boolean fill) {
        this.fillWidth = fill;
    }

    private IStructuredContentProvider contentProvider;

    public IStructuredContentProvider getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(IStructuredContentProvider cp) {
        this.contentProvider = cp;
    }

    private ITableLabelProvider labelProvider;

    public ITableLabelProvider getLabelProvider() {
        return labelProvider;
    }

    public void setLabelProvider(ITableLabelProvider lp) {
        this.labelProvider = lp;
    }

    private ICellModifier cellModifier;

    public ICellModifier getCellModifier() {
        return cellModifier;
    }

    public void setCellModifier(ICellModifier cm) {
        this.cellModifier = cm;
    }

    private TableModelUpdater modelUpdater;

    public TableModelUpdater getModelUpdater() {
        return modelUpdater;
    }

    public void setModelUpdater(TableModelUpdater mu) {
        this.modelUpdater = mu;
    }

    public Table getTable() {
        return (Table) getWidget();
    }

    private List tableValue;

    public List getTableValue() {
        return tableValue;
    }

    public void setTableValue(List value) {
        this.tableValue = value;
    }

    // instead of mbeng
    private String attributeName;

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String name) {
        this.attributeName = name;
    }

    private boolean suppressAddDeleteEvenForEditable;

    public boolean isSuppressAddDeleteEvenForEditable() {
        return suppressAddDeleteEvenForEditable;
    }

    public TableEditor(WorkflowElement canvasSelection, MbengNode mbengNode) {
        super(canvasSelection, mbengNode);
        if (mbengNode.getAttribute("MODIFYONLY") != null) {
            suppressAddDeleteEvenForEditable = Boolean
                    .parseBoolean(mbengNode.getAttribute("MODIFYONLY"));
        }

        columnSpecs = new ArrayList<ColumnSpec>();
        columnSpecs.add(getDummyColumn());

        for (MbengNode childNode = mbengNode
                .getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            String type = childNode.getName();
            String label = childNode.getAttribute("LABEL");
            String property = childNode.getAttribute("NAME");
            if (property == null)
                property = "columnValue_" + columnSpecs.size();
            ColumnSpec colSpec = new ColumnSpec(type, label, property);
            if (type.equals(TYPE_CHECKBOX)) {
                colSpec.width = 75;
            }
            else if (type.equals(TYPE_COMBO)) {
                colSpec.source = childNode.getAttribute("SOURCE");
                if ("Process".equals(colSpec.source)) {
                    colSpec.type = WorkflowAssetEditor.TYPE_ASSET;
                    columnSpecs.get(columnSpecs.size() - 1).width = 150;
                    colSpec.width = 300;
                    colSpec.readOnly = false;
                    colSpec.assetTypes = new String[] { "Process" };
                }
                else if ("RuleSets".equals(colSpec.source)) {
                    colSpec.type = WorkflowAssetEditor.TYPE_ASSET;
                    colSpec.width = Integer.parseInt(childNode.getAttribute("VW"));
                    colSpec.readOnly = false;
                    String typeAttr = childNode.getAttribute("TYPE");
                    List<String> assetTypes = null;
                    if (typeAttr != null) {
                        assetTypes = new ArrayList<String>();
                        for (String assetType : typeAttr.split(","))
                            assetTypes.add(assetType);
                    }

                    colSpec.assetTypes = assetTypes.toArray(new String[0]);
                }
                else if ("ProcessVersion".equals(colSpec.source)
                        || "AssetVersion".equals(colSpec.source)) {
                    // this field is suppressed now (populated through asset
                    // picker)
                    colSpec.hidden = true;
                    colSpec.options = new String[] { "" };
                }
                else {
                    List<String> valueOptions = new ArrayList<String>();
                    for (MbengNode optionNode = childNode
                            .getFirstChild(); optionNode != null; optionNode = optionNode
                                    .getNextSibling()) {
                        valueOptions.add(optionNode.getValue());
                    }
                    colSpec.options = valueOptions.toArray(new String[0]);
                }
            }

            if (childNode.getAttribute("READONLY") != null) {
                colSpec.readOnly = Boolean.parseBoolean(childNode.getAttribute("READONLY"));
            }
            if (childNode.getAttribute("VW") != null) {
                colSpec.width = Integer.parseInt(childNode.getAttribute("VW"));
            }

            colSpec.defaultValue = childNode.getAttribute("DEFAULT");

            columnSpecs.add(colSpec);
        }

        setColumnSpecs(columnSpecs);
    }

    public TableEditor(WorkflowElement canvasSelection, String type) {
        super(canvasSelection, type);
    }

    public void setColumnSpecs(List<ColumnSpec> columnSpecs) {
        this.columnSpecs = columnSpecs;
        columnProps = new String[columnSpecs.size()];
        for (int i = 0; i < columnSpecs.size(); i++) {
            columnProps[i] = columnSpecs.get(i).property;
        }
    }

    public List<ColumnSpec> getColumnSpecs() {
        return columnSpecs;
    }

    /**
     * Populates the table widget from the model
     */
    public void setValue(Activity activity) {
        super.setValue(activity);

        if (contentProvider instanceof DefaultContentProvider) {
            List<String[]> entries = StringHelper.parseTable(getValue(), columnDelimiter,
                    rowDelimiter, columnSpecs.size());
            tableValue = new ArrayList<DefaultRowImpl>();
            for (int i = 0; i < entries.size(); i++) {
                tableValue.add(new DefaultRowImpl(entries.get(i), true));
            }
        }

        tableViewer.setInput(tableValue);
    }

    public void setValue(WorkflowAsset workflowAsset) {
        super.setValue(workflowAsset);

        if (contentProvider instanceof DefaultContentProvider) {
            List<String[]> entries = StringHelper.parseTable(getValue(), columnDelimiter,
                    rowDelimiter, columnSpecs.size());
            tableValue = new ArrayList<DefaultRowImpl>();
            for (int i = 0; i < entries.size(); i++) {
                tableValue.add(new DefaultRowImpl(entries.get(i), true));
            }
        }
        tableViewer.setInput(tableValue);
    }

    public void setValue(List value) {
        tableValue = value;
        tableViewer.setInput(tableValue);
    }

    public void setValue(String stringVal) {
        List<DefaultRowImpl> listVal = new ArrayList<DefaultRowImpl>();
        if (stringVal != null) {
            StringTokenizer rowTokenizer = new StringTokenizer(stringVal,
                    String.valueOf(rowDelimiter));
            while (rowTokenizer.hasMoreTokens()) {
                String row = rowTokenizer.nextToken();
                StringTokenizer columnTokenizer = new StringTokenizer(row,
                        String.valueOf(columnDelimiter));
                int numCols = columnTokenizer.countTokens();
                String[] columns = new String[numCols];
                for (int i = 0; i < numCols; i++)
                    columns[i] = columnTokenizer.nextToken();
                listVal.add(new DefaultRowImpl(columns));
            }
        }
        setValue(listVal);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (addButton != null)
            addButton.setEnabled(enabled);
        if (deleteButton != null)
            deleteButton.setEnabled(enabled);
    }

    private boolean editable = true;

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (addButton != null)
            addButton.setEnabled(editable);
        if (deleteButton != null)
            deleteButton.setEnabled(editable);
    }

    public void disposeWidget() {
        super.disposeWidget();
        if (buttonComposite != null)
            buttonComposite.dispose();
    }

    /**
     * @see com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor#render(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void render(Composite parent) {
        render(parent, !isReadOnly() && !suppressAddDeleteEvenForEditable);
    }

    @Override
    public String getLabel() {
        if (getSection() != null)
            return null; // suppress label for dedicated section
        else
            return super.getLabel();
    }

    public void render(Composite parent, boolean includeAddDeleteButtons) {
        if (contentProvider == null)
            contentProvider = new DefaultContentProvider();
        if (labelProvider == null)
            labelProvider = new DefaultLabelProvider();
        if (!isReadOnly() && cellModifier == null)
            cellModifier = new DefaultCellModifier();
        if (modelUpdater == null)
            modelUpdater = new DefaultModelUpdater();

        if (getLabel() != null)
            createLabel(parent);

        setWidget(createTable(parent));
        tableViewer = createTableViewer(getTable());
        tableViewer.setContentProvider(contentProvider);
        tableViewer.setLabelProvider(labelProvider);

        if (includeAddDeleteButtons) {
            createButtons(parent);
        }
    }

    private Table createTable(Composite parent) {
        int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
                | SWT.HIDE_SELECTION;

        final Table table = new Table(parent, style);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        if (horizontalSpan == 0)
            gridData.horizontalSpan = getLabel() == null ? 3 : 2;
        else
            gridData.horizontalSpan = horizontalSpan;
        if (getType().equals(TYPE_TABLE))
            gridData.heightHint = 100;
        if (getHeight() != SWT.DEFAULT)
            gridData.heightHint = getHeight();
        if (getWidth() != DEFAULT_VALUE_WIDTH)
            gridData.widthHint = getWidth();
        table.setLayoutData(gridData);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec colSpec = columnSpecs.get(i);
            int colStyle = SWT.LEFT | colSpec.style;
            if (colSpec.readOnly)
                colStyle = colStyle | SWT.READ_ONLY;
            TableColumn column = new TableColumn(table, colStyle, i);
            column.setText(colSpec.label);
            if (colSpec.hidden) {
                column.setWidth(0);
                column.setResizable(false);
            }
            else {
                column.setWidth(colSpec.width);
                column.setResizable(colSpec.resizable);
            }
            if (colSpec.height != ColumnSpec.DEFAULT_ROW_HEIGHT) {
                table.addListener(SWT.MeasureItem, new RowHeightListener(colSpec.height));
            }
        }

        if (isFillWidth()) {
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
        }

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                fireValueChanged(e.item.getData(), false);
            }
        });

        // double-click handler for asset rows
        int assetColIdx = -1;
        for (int i = 0; i < columnSpecs.size(); i++) {
            if (WorkflowAssetEditor.TYPE_ASSET.equals(columnSpecs.get(i).type))
                assetColIdx = i;
        }
        if (assetColIdx != -1) {
            final int colIdx = assetColIdx;
            addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    if (newValue instanceof DefaultRowImpl) {
                        // is the case for double-click
                        DefaultRowImpl row = (DefaultRowImpl) newValue;
                        String name = row.getColumnValues()[colIdx];
                        String ver = "0";
                        if (columnSpecs.size() > colIdx + 1 && ("ProcessVersion"
                                .equals(columnSpecs.get(colIdx + 1).source)
                                || "AssetVersion".equals(columnSpecs.get(colIdx + 1).source)))
                            ver = row.getColumnValues()[colIdx + 1];
                        if (name != null && ver != null) {
                            AssetVersionSpec spec = new AssetVersionSpec(name, ver);
                            AssetLocator.Type type = AssetLocator.Type.Asset;
                            if ("Process".equals(columnSpecs.get(colIdx).source))
                                type = AssetLocator.Type.Process;
                            else if (RuleSetVO.TASK.equals(columnSpecs.get(colIdx).source))
                                type = AssetLocator.Type.TaskTemplate;
                            AssetLocator locator = new AssetLocator(getElement(), type);
                            locator.openAsset(spec);
                        }
                    }
                }
            });
        }

        return table;
    }

    private TableViewer createTableViewer(Table table) {
        TableViewer tableViewer = new TableViewer(table);
        tableViewer.setUseHashlookup(true);

        tableViewer.setColumnProperties(columnProps);

        if (!isReadOnly()) {
            CellEditor[] cellEditors = new CellEditor[columnSpecs.size()];
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec colSpec = columnSpecs.get(i);
                CellEditor cellEditor = null;
                if (!colSpec.hidden) {
                    if (colSpec.type.equals(TYPE_TEXT)) {
                        if (colSpec.style != 0)
                            cellEditor = new TextCellEditor(table, colSpec.style);
                        else
                            cellEditor = new TextCellEditor(table);
                    }
                    else if (colSpec.type.equals(TYPE_CHECKBOX)) {
                        cellEditor = new CheckboxCellEditor(table);
                    }
                    else if (colSpec.type.equals(TYPE_COMBO)) {
                        int style = SWT.None;
                        if (colSpec.readOnly)
                            style = style | SWT.READ_ONLY;
                        cellEditor = new ComboBoxCellEditor(table, colSpec.options, style);
                        if (!colSpec.readOnly)
                            cellEditor.addListener(new EditableComboCellEditorListener(
                                    (ComboBoxCellEditor) cellEditor, i));
                    }
                    else if (colSpec.type.equals(WorkflowAssetEditor.TYPE_ASSET)) {
                        int style = SWT.None;
                        if (colSpec.readOnly)
                            style = style | SWT.READ_ONLY;

                        cellEditor = new TreeComboCellEditor(table, style) {
                            @Override
                            protected void doSetValue(Object value) {
                                Object val = getValue();
                                if (val instanceof CTreeComboItem) {
                                    CTreeComboItem selItem = (CTreeComboItem) getValue();
                                    if (selItem != null) {
                                        super.doSetValue(selItem.getText());
                                        return;
                                    }
                                }

                                if (value instanceof String) {
                                    String strVal = (String) value;
                                    if (strVal.indexOf('/') > 0) {
                                        super.doSetValue(strVal.substring(strVal.indexOf('/') + 1));
                                        return;
                                    }
                                }

                                super.doSetValue(value);
                            }
                        };
                        ((TreeComboCellEditor) cellEditor)
                                .setSelectionModifier(new SelectionModifier() {
                                    public String modify(CTreeComboItem selection) {
                                        if (selection.getParentItem() != null) {
                                            WorkflowPackage pkg = getProject().getPackage(
                                                    selection.getParentItem().getText());
                                            if (pkg == null || pkg.isDefaultPackage())
                                                return selection.getText();
                                            else
                                                return pkg.getName() + "/" + selection.getText();
                                        }
                                        else {
                                            // ignore packages
                                            return null;
                                        }
                                    }
                                });

                        // populate the treecombo
                        if (colSpec.source.equals("Process")) {
                            List<WorkflowPackage> packages = getProject()
                                    .getTopLevelUserVisiblePackages();
                            for (WorkflowPackage pkg : packages) {
                                CTreeComboItem packageItem = ((TreeComboCellEditor) cellEditor)
                                        .addItem(pkg.getName());
                                packageItem.setText(pkg.getName());
                                packageItem.setImage(pkg.getIconImage());
                                for (WorkflowProcess process : pkg.getProcesses()) {
                                    CTreeComboItem processItem = new CTreeComboItem(packageItem,
                                            SWT.NONE);
                                    processItem.setText(process.getLabel());
                                    processItem.setImage(process.getIconImage());
                                }
                            }
                        }
                        else {
                            List<WorkflowAsset> assets = getProject()
                                    .getAssetList(Arrays.asList(colSpec.assetTypes));
                            Map<String, CTreeComboItem> packageItems = new HashMap<String, CTreeComboItem>();
                            for (WorkflowAsset asset : assets) {
                                String pkgName = asset.getPackage().getName();
                                CTreeComboItem packageItem = packageItems.get(pkgName);
                                if (packageItem == null) {
                                    packageItem = ((TreeComboCellEditor) cellEditor)
                                            .addItem(pkgName);
                                    packageItem.setImage(asset.getPackage().getIconImage());
                                    packageItems.put(pkgName, packageItem);
                                }
                                CTreeComboItem assetItem = new CTreeComboItem(packageItem,
                                        SWT.NONE);
                                assetItem.setText(asset.getLabel());
                                assetItem.setImage(asset.getIconImage());
                            }
                        }
                    }
                    if (colSpec.listener != null)
                        cellEditor.addListener(colSpec.listener);
                    cellEditors[i] = cellEditor;
                }
            }
            tableViewer.setCellEditors(cellEditors);
            tableViewer.setCellModifier(cellModifier);
        }

        return tableViewer;
    }

    private void createButtons(Composite parent) {
        buttonComposite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.numColumns = horizontalSpan == 4 ? 2 : 1;
        buttonComposite.setLayout(gl);

        // add
        addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
        addButton.setText("Add");
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.widthHint = 60;
        addButton.setLayoutData(gridData);
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Object row = modelUpdater.create();
                tableViewer.add(row);
                tableValue.add(row);
                modelUpdater.updateModelValue(getTableValue());
                fireValueChanged(tableValue);
            }
        });

        // delete
        deleteButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
        deleteButton.setText("Delete");
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.widthHint = 60;
        deleteButton.setLayoutData(gridData);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Object row = ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
                if (row != null) {
                    tableViewer.remove(row);
                    tableValue.remove(row);
                    modelUpdater.updateModelValue(getTableValue());
                    fireValueChanged(tableValue);
                }
            }
        });
    }

    ColumnSpec getDummyColumn() {
        // add a dummy column to work around Eclipse bug 43910
        ColumnSpec dummyCol = new ColumnSpec("TEXT", "", "dummy");
        dummyCol.width = 0;
        dummyCol.readOnly = true;
        dummyCol.resizable = false;
        return dummyCol;
    }

    /**
     * Default column provider populates cells based on delimited attribute
     * values
     */
    public class DefaultContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            List<DefaultRowImpl> rows = (List<DefaultRowImpl>) inputElement;
            return rows.toArray(new DefaultRowImpl[0]);
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }
    }

    public class DefaultRowImpl {
        private String[] columnValues;

        public String[] getColumnValues() {
            return columnValues;
        }

        private boolean padded;

        public boolean isPadded() {
            return padded;
        }

        public void setPadded(boolean padded) {
            this.padded = padded;
        }

        public DefaultRowImpl() {
            this(false);
        }

        public DefaultRowImpl(boolean padded) {
            this.padded = padded;
            String[] colVals = null;

            if (padded) {
                // add a value for the dummy column
                colVals = new String[columnSpecs.size() + 1];
                for (int i = 0; i < columnSpecs.size(); i++) {
                    String defaultVal = columnSpecs.get(i).defaultValue;
                    if (i == 1)
                        colVals[i] = defaultVal == null ? "New Item" : defaultVal;
                    else
                        colVals[i] = defaultVal == null ? "" : defaultVal;
                }
            }
            else {
                colVals = new String[columnSpecs.size()];
                for (int i = 0; i < columnSpecs.size(); i++) {
                    String defaultVal = columnSpecs.get(i).defaultValue;
                    if (i == 0)
                        colVals[i] = defaultVal == null ? "New Item" : defaultVal;
                    else
                        colVals[i] = defaultVal == null ? "" : defaultVal;
                }
            }

            this.columnValues = colVals;
        }

        public DefaultRowImpl(String[] columnValues) {
            this(columnValues, false);
        }

        public DefaultRowImpl(String[] columnValues, boolean padded) {
            this.padded = padded;
            String[] colVals = null;

            if (padded) {
                // add a value for the dummy column
                colVals = new String[columnValues.length + 1];
                colVals[0] = "";
                for (int i = 0; i < columnValues.length; i++)
                    colVals[i + 1] = columnValues[i];
            }
            else {
                colVals = new String[columnValues.length];
                for (int i = 0; i < columnValues.length; i++)
                    colVals[i] = columnValues[i];
            }

            this.columnValues = colVals;
        }

        public Object getColumnValue(int col) {
            String colType = columnSpecs.get(col).type;
            if (col > columnValues.length - 1) {
                if (colType.equals(TYPE_CHECKBOX))
                    return new Boolean(false);
                else if (colType.equals(TYPE_COMBO))
                    return new Integer(-1);
                else
                    return "";
            }

            String strVal = columnValues[col];
            if (colType.equals(TYPE_CHECKBOX)) {
                return new Boolean(strVal);
            }
            if (colType.equals(TYPE_COMBO)) {
                String[] options = columnSpecs.get(col).options;
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(strVal))
                        return new Integer(i);
                }
                return new Integer(-1);
            }
            else {
                return strVal;
            }
        }

        public void setColumnValue(int col, Object value) {
            if (col > columnValues.length - 1) {
                String[] newColVals = new String[columnValues.length + 1];
                for (int i = 0; i < columnValues.length; i++)
                    newColVals[i] = columnValues[i];
                newColVals[columnValues.length] = "";
                columnValues = newColVals;
            }

            String colType = columnSpecs.get(col).type;
            if (colType.equals(TYPE_CHECKBOX)) {
                columnValues[col] = value.toString();
            }
            else if (colType.equals(TYPE_COMBO)) {
                columnValues[col] = columnSpecs.get(col).options[((Integer) value).intValue()];
            }
            else if (colType.equals(WorkflowAssetEditor.TYPE_ASSET)) {
                if (value instanceof String) {
                    // ** set asset
                    AssetVersionSpec spec = AssetVersionSpec.parse((String) value);
                    if (spec.getPackageName() != null && getProject().checkRequiredVersion(5, 5))
                        columnValues[col] = spec.getPackageName() + "/" + spec.getName();
                    else
                        columnValues[col] = spec.getName();

                    if (columnSpecs.size() > col + 1
                            && ("ProcessVersion".equals(columnSpecs.get(col + 1).source)
                                    || "AssetVersion".equals(columnSpecs.get(col + 1).source))) {
                        // set the version column
                        String versionAttr = null;
                        if (getProject().checkRequiredVersion(5, 5)) {
                            boolean wasTyped = spec.getVersion().equals("0")
                                    || spec.getVersion().startsWith("[");
                            if (MdwPlugin.getSettings().isInferSmartSubprocVersionSpec()
                                    && !wasTyped) {
                                try {
                                    versionAttr = AssetVersionSpec
                                            .getDefaultSmartVersionSpec(spec.getVersion());
                                }
                                catch (NumberFormatException ex) {
                                    versionAttr = spec.getVersion();
                                }
                            }
                            else {
                                versionAttr = spec.getVersion();
                            }
                        }
                        else {
                            versionAttr = spec.getVersion(); // pre-5.5
                                                             // compatibility
                        }
                        columnValues[col + 1] = versionAttr;
                    }
                }
                else if (value instanceof CTreeComboItem) {
                    // can happen but ignored because above is triggered by loss
                    // of focus
                }
            }
            else {
                if (value == null)
                    columnValues[col] = "";
                else
                    columnValues[col] = ((String) value).trim();
            }

            tableViewer.update(this, null);
        }
    }

    public interface TableModelUpdater {
        /**
         * Creates a new row instance
         *
         * @return the newly-created row object
         */
        public Object create();

        /**
         * Saves the tableValue back to the model
         */
        public void updateModelValue(List tableValue);
    }

    public class DefaultModelUpdater implements TableModelUpdater {
        public Object create() {
            return new DefaultRowImpl(true);
        }

        public void updateModelValue(List tableValue) {
            String serialized = serialize(tableValue);
            if (getMbengNode() != null)
                getElement().setAttribute(getMbengNode().getAttribute("NAME"), serialized);
            else if (getAttributeName() != null)
                getElement().setAttribute(getAttributeName(), serialized);
        }

        protected String serialize(List tableValue) {
            if (getProject().checkRequiredVersion(6, 0, 4)) {
                JSONArray outer = new JSONArray();
                for (int i = 0; i < tableValue.size(); i++) {
                    JSONArray inner = new JSONArray();
                    DefaultRowImpl row = (DefaultRowImpl) tableValue.get(i);
                    for (int j = 0; j < row.columnValues.length; j++) {
                        if (row.isPadded()) {
                            if (j == 0)
                                continue; // ignore dummy column
                        }
                        if (row.columnValues[j] != null)
                            inner.put(row.columnValues[j]);
                    }
                    outer.put(inner);
                }
                return outer.toString();
            }
            else {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < tableValue.size(); i++) {
                    if (i > 0)
                        sb.append(rowDelimiter);
                    DefaultRowImpl row = (DefaultRowImpl) tableValue.get(i);
                    for (int j = 0; j < row.columnValues.length; j++) {
                        if (row.isPadded()) {
                            if (j == 0)
                                continue; // ignore dummy column
                            if (j > 1)
                                sb.append(columnDelimiter);
                        }
                        else {
                            if (j > 0)
                                sb.append(columnDelimiter);
                        }
                        if (row.columnValues[j] != null)
                            sb.append(StringHelper.escapeWithBackslash(row.columnValues[j], ",;"));
                    }
                }
                return sb.toString();
            }
        }
    }

    public class DefaultLabelProvider extends LabelProvider implements ITableLabelProvider {
        private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

        public Image getColumnImage(Object element, int columnIndex) {
            DefaultRowImpl row = (DefaultRowImpl) element;
            ColumnSpec colspec = columnSpecs.get(columnIndex);
            if (colspec.type.equals(TYPE_CHECKBOX)) {
                ImageDescriptor descriptor = null;
                Boolean value = (Boolean) row.getColumnValue(columnIndex);
                if (value.booleanValue()) {
                    descriptor = MdwPlugin.getImageDescriptor("icons/checked.gif");
                }
                else {
                    descriptor = MdwPlugin.getImageDescriptor("icons/unchecked.gif");
                }
                Image image = (Image) imageCache.get(descriptor);
                if (image == null) {
                    image = descriptor.createImage();
                    imageCache.put(descriptor, image);
                }
                return image;
            }
            else if (colspec.type.equals(WorkflowAssetEditor.TYPE_ASSET)) {
                if (!"".equals(getColumnText(element, columnIndex))) {
                    ImageDescriptor descriptor = MdwPlugin
                            .getImageDescriptor("Process".equals(colspec.source)
                                    ? "icons/process.gif" : "icons/doc.gif");
                    Image image = (Image) imageCache.get(descriptor);
                    if (image == null) {
                        image = descriptor.createImage();
                        imageCache.put(descriptor, image);
                    }
                    return image;
                }
                return null;
            }
            else {
                return null;
            }
        }

        public String getColumnText(Object element, int columnIndex) {
            DefaultRowImpl row = (DefaultRowImpl) element;
            ColumnSpec colspec = columnSpecs.get(columnIndex);
            if (colspec.type.equals(TYPE_CHECKBOX)) {
                return null;
            }
            else if (colspec.type.equals(TYPE_COMBO)) {
                int optionIdx = (Integer) row.getColumnValue(columnIndex);
                if (colspec.readOnly || optionIdx >= 0) {
                    return colspec.options[optionIdx];
                }
                else {
                    // non-readonly and option not present
                    return row.columnValues[columnIndex];
                }
            }
            else if (colspec.type.equals(WorkflowAssetEditor.TYPE_ASSET)) {
                // ** display asset
                String stringVal = (String) row.getColumnValue(columnIndex);
                // can happen that value was set with version due to friendly
                // display
                AssetVersionSpec spec = AssetVersionSpec.parse(stringVal);
                if (spec.getVersion() != null)
                    stringVal = (spec.getQualifiedName()); // strip off version

                if (stringVal != null && stringVal.length() > 0) {
                    if (row.columnValues.length > columnIndex + 1 && ("ProcessVersion"
                            .equals(columnSpecs.get(columnIndex + 1).source)
                            || "AssetVersion".equals(columnSpecs.get(columnIndex + 1).source))) {
                        stringVal += " v" + row.columnValues[columnIndex + 1];
                    }
                    else {
                        stringVal += " v0";
                    }

                    AssetLocator.Type type = AssetLocator.Type.Asset;
                    if ("Process".equals(colspec.source))
                        type = AssetLocator.Type.Process;
                    else if (RuleSetVO.TASK.equals(colspec.source))
                        type = AssetLocator.Type.TaskTemplate;
                    AssetLocator assetLocator = new AssetLocator(getElement(), type);
                    WorkflowElement workflowAsset = assetLocator.assetFromAttr(stringVal);
                    if (workflowAsset == null)
                        return stringVal;
                    TreeComboCellEditor tcce = (TreeComboCellEditor) tableViewer
                            .getCellEditors()[columnIndex];
                    CTreeCombo treeCombo = tcce.getTreeCombo();
                    if (workflowAsset != null) {
                        // preselect in tree combo
                        CTreeComboItem selItem = null;
                        for (CTreeComboItem pkgItem : treeCombo.getItems()) {
                            if (pkgItem.getText().equals(workflowAsset.getPackage().getName())) {
                                for (CTreeComboItem assetItem : pkgItem.getItems()) {
                                    if (assetItem.getText().equals(workflowAsset.getName())
                                            || assetItem.getText()
                                                    .equals(workflowAsset.getLabel())) {
                                        selItem = assetItem;
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        if (selItem == null) {
                            return workflowAsset.getLabel();
                        }
                        else {
                            treeCombo.select(selItem);
                            return selItem.getText();
                        }
                    }
                }

                if (stringVal != null && stringVal.indexOf('/') > 0)
                    return stringVal.substring(stringVal.indexOf('/') + 1);
                else
                    return stringVal;
            }
            else {
                return (String) row.getColumnValue(columnIndex);
            }
        }
    }

    public class DefaultCellModifier implements ICellModifier {
        public boolean canModify(Object element, String property) {
            if (!editable)
                return false;
            ColumnSpec colSpec = columnSpecs.get(getColumnIndex(property));
            return colSpec.type.equals(TYPE_COMBO) || !colSpec.readOnly;
        }

        public Object getValue(Object element, String property) {
            DefaultRowImpl row = (DefaultRowImpl) element;
            int colIndex = getColumnIndex(property);
            return row.getColumnValue(colIndex);
        }

        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            DefaultRowImpl row = (DefaultRowImpl) item.getData();
            int colIndex = getColumnIndex(property);
            row.setColumnValue(colIndex, value);
            modelUpdater.updateModelValue(getTableValue());
            fireValueChanged(tableValue);
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

    class RowHeightListener implements Listener {
        int height = ColumnSpec.DEFAULT_ROW_HEIGHT;

        public RowHeightListener(int height) {
            this.height = height;
        }

        public void handleEvent(Event event) {
            if (event.type == SWT.MeasureItem) {
                event.height = this.height;
            }
        }
    }

    /**
     * Workaround for Eclipse bug:
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=67069
     */
    class EditableComboCellEditorListener implements ICellEditorListener {
        private ComboBoxCellEditor editor;
        int colSpecIndex;

        public EditableComboCellEditorListener(ComboBoxCellEditor editor, int colSpecIndex) {
            this.editor = editor;
            this.colSpecIndex = colSpecIndex;
        }

        public void applyEditorValue() {
            String newText = ((CCombo) editor.getControl()).getText();

            ColumnSpec colSpec = columnSpecs.get(colSpecIndex);
            for (int i = 0; i < colSpec.options.length; i++) {
                if (colSpec.options[i].equals(newText))
                    return;
            }
            // not found, so add the string to the list and select it
            String[] items = editor.getItems();
            String[] options = new String[items.length + 1];
            for (int i = 0; i < items.length; i++)
                options[i] = items[i];
            options[options.length - 1] = newText;
            editor.setItems(options);
            editor.setValue(options.length - 1);
            colSpec.options = options;
        }

        public void cancelEditor() {
        }

        public void editorValueChanged(boolean oldValidState, boolean newValidState) {
        }
    }
}
