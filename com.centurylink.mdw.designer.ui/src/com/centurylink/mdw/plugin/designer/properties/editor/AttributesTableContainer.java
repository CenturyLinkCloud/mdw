/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.List;

import noNamespace.BooleanT;
import noNamespace.DropdownT;
import noNamespace.OptionT;
import noNamespace.TableT;
import noNamespace.TextT;
import noNamespace.WidgetT;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.TableModelUpdater;
import com.qwest.mbeng.FormatXml;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengDocumentClass;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class AttributesTableContainer {
    public static final String NAME = "name";
    public static final String VALUE = "value";

    private Label label;

    private boolean readOnly;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setEditable(boolean editable) {
        // need values to select
        WidgetT valueWidget = getValueWidget(pageletTable);
        if (valueWidget instanceof DropdownT) {
            DropdownT dropdown = (DropdownT) valueWidget;
            if (dropdown.getOPTIONList().size() == 0)
                editable = false;
        }

        tableEditor.setEditable(editable);
    }

    private int width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private TableT pageletTable;
    private Composite tableComposite;

    TableEditor tableEditor;

    public AttributesTableContainer(Composite parent, boolean readOnly, int width, int colspan,
            TableT pageletTable) {
        this.readOnly = readOnly;
        this.width = width;
        this.pageletTable = pageletTable;

        tableComposite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.numColumns = 4;
        gl.marginLeft = -5;
        gl.marginTop = -5;
        tableComposite.setLayout(gl);
        GridData gridData = new GridData(SWT.LEFT);
        gridData.horizontalSpan = colspan;
        tableComposite.setLayoutData(gridData);

        if (pageletTable.getLABEL() != null) {
            label = new Label(tableComposite, SWT.NONE);
            label.setText(pageletTable.getLABEL());
            gridData = new GridData(SWT.LEFT);
            gridData.horizontalSpan = gl.numColumns;
            label.setLayoutData(gridData);
        }

        createTableEditor(tableComposite);
    }

    public void dispose() {
        if (label != null)
            label.dispose();
        if (tableEditor != null)
            tableEditor.dispose();
        if (tableComposite != null)
            tableComposite.dispose();
    }

    static WidgetT getNameWidget(TableT pageletTable) {
        return getWidget(pageletTable, NAME);
    }

    static WidgetT getValueWidget(TableT pageletTable) {
        return getWidget(pageletTable, VALUE);
    }

    private static WidgetT getWidget(TableT pageletTable, String name) {
        for (BooleanT b : pageletTable.getBOOLEANList()) {
            if (name.equals(b.getNAME()))
                return b;
        }
        for (TextT t : pageletTable.getTEXTList()) {
            if (name.equals(t.getNAME()))
                return t;
        }
        for (DropdownT d : pageletTable.getDROPDOWNList()) {
            if (name.equals(d.getNAME()))
                return d;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<AttributeVO> getAttributes() {
        return (List<AttributeVO>) tableEditor.getTableValue();
    }

    public void setAttributes(List<AttributeVO> attributes) {
        tableEditor.setValue(attributes);
    }

    private void createTableEditor(Composite parent) {
        FormatXml fmter = new FormatXml();
        MbengDocument mbengDocument = new MbengDocumentClass();
        try {
            fmter.load(mbengDocument, pageletTable.xmlText());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        tableEditor = new TableEditor(null, mbengDocument.getRootNode());
        tableEditor.setLabel(null); // label was already rendered
        // tableEditor.setReadOnly(readOnly);
        // tableEditor.setFillWidth(true);

        tableEditor.setModelUpdater(new TableModelUpdater() {
            public Object create() {
                int i = 1;
                String attrName = "New Field";
                while (getAttribute(attrName) != null)
                    attrName = "New Field (" + i++ + ")";
                return new AttributeVO(attrName, null);
            }

            @SuppressWarnings("rawtypes")
            public void updateModelValue(List tableValue) {
                fireDirtyStateChange(true);
            }
        });

        tableEditor.setCellModifier(new AttributeCellModifier());
        tableEditor.setContentProvider(new AttributeContentProvider());
        tableEditor.setLabelProvider(new AttributeLabelProvider());

        tableEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                // value changed

                // double click
                // AttributeVO attributeVO = (AttributeVO) e.item.getData();
                // AttributeDialog dialog = new
                // AttributeDialog(parent.getShell(), attributeVO);
                // dialog.open();
            }
        });

        tableEditor.render(parent, true);
    }

    private AttributeVO getAttribute(String name) {
        for (AttributeVO attr : getAttributes()) {
            if (name.equals(attr.getAttributeName()))
                return attr;
        }
        return null;
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

    class AttributeCellModifier implements ICellModifier {
        public boolean canModify(Object element, String property) {
            ColumnSpec colSpec = tableEditor.getColumnSpecs().get(getColumnIndex(property));
            return !isReadOnly() && !colSpec.readOnly;
        }

        public Object getValue(Object element, String property) {
            AttributeVO attribute = (AttributeVO) element;
            ColumnSpec colSpec = tableEditor.getColumnSpecs().get(getColumnIndex(property));
            WidgetT widget = getWidget(pageletTable, colSpec.property);
            if (widget instanceof DropdownT && VALUE.equals(colSpec.property)) {
                DropdownT dropdown = (DropdownT) widget;
                for (int i = 0; i < dropdown.getOPTIONList().size(); i++) {
                    OptionT option = dropdown.getOPTIONList().get(i);
                    if (option.getStringValue().equals(attribute.getAttributeValue())
                            || (option.getVALUE() != null
                                    && option.getVALUE().equals(attribute.getAttributeValue())))
                        return i;
                }
                // could have been dynamically added
                for (int i = dropdown.getOPTIONList().size(); i < colSpec.options.length; i++) {
                    if (colSpec.options[i].equals(attribute.getAttributeValue()))
                        return i;
                }
                return new Integer(0);
            }
            else {
                if (NAME.equals(colSpec.property))
                    return attribute.getAttributeName();
                else if (VALUE.equals(colSpec.property))
                    return attribute.getAttributeValue() == null ? ""
                            : attribute.getAttributeValue();
            }

            return null;
        }

        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            AttributeVO attr = (AttributeVO) item.getData();
            ColumnSpec colSpec = tableEditor.getColumnSpecs().get(getColumnIndex(property));
            WidgetT widget = getWidget(pageletTable, colSpec.property);
            if (widget instanceof DropdownT) {
                DropdownT dropdown = (DropdownT) widget;
                if (VALUE.equals(colSpec.property) && value instanceof Integer) {
                    int sel = (Integer) value;
                    if (sel > dropdown.getOPTIONList().size() - 1 && sel < colSpec.options.length) {
                        // value was typed in (and dynamically added to colspec
                        // options)
                        attr.setAttributeValue(colSpec.options[sel]);
                    }
                    else {
                        OptionT option = dropdown.getOPTIONList().get(sel);
                        String valueSpec = option.getVALUE();
                        if (valueSpec == null)
                            valueSpec = option.getStringValue();
                        attr.setAttributeValue(valueSpec);
                    }
                }
                else {
                    return;
                }
            }
            else {
                if (NAME.equals(colSpec.property))
                    attr.setAttributeName((String) value);
                else if (VALUE.equals(colSpec.property))
                    attr.setAttributeValue((String) value);
                else
                    return;
            }

            tableEditor.getTableViewer().update(item.getData(), new String[] { property });
            fireDirtyStateChange(true);
        }

        protected int getColumnIndex(String property) {
            for (int i = 0; i < tableEditor.getColumnSpecs().size(); i++) {
                ColumnSpec colSpec = tableEditor.getColumnSpecs().get(i);
                if (colSpec.property.equals(property)) {
                    return i;
                }
            }
            return -1;
        }
    }

    class AttributeLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            AttributeVO attribute = (AttributeVO) element;
            ColumnSpec colSpec = tableEditor.getColumnSpecs().get(columnIndex);
            if (NAME.equals(colSpec.property)) {
                return attribute.getAttributeName();
            }
            else if (VALUE.equals(colSpec.property)) {
                WidgetT widget = getWidget(pageletTable, colSpec.property);
                if (widget instanceof DropdownT) {
                    String value = attribute.getAttributeValue();
                    DropdownT dropdown = (DropdownT) widget;
                    for (OptionT option : dropdown.getOPTIONList()) {
                        String valueSpec = option.getVALUE();
                        if (valueSpec != null && valueSpec.equals(value))
                            return option.getStringValue();
                        else if (option.getStringValue().equals(value))
                            return option.getStringValue();
                    }
                    if (value == null)
                        return "";
                    else
                        return value; // compatibility
                }
                else {
                    return attribute.getAttributeValue();
                }
            }
            else
                return null;
        }
    }

    class AttributeContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<AttributeVO> variables = inputElement == null ? new ArrayList<AttributeVO>()
                    : (List<AttributeVO>) inputElement;
            return (AttributeVO[]) variables.toArray(new AttributeVO[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }
}
