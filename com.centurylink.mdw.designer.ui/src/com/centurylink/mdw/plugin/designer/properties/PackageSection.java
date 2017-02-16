/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.properties.convert.DateConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class PackageSection extends PropertySection implements ElementChangeListener {
    private WorkflowPackage workflowPackage;

    public WorkflowPackage getPackage() {
        return workflowPackage;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor namePropertyEditor;
    private PropertyEditor schemaVersionPropertyEditor;
    private PropertyEditor lastModifiedPropertyEditor;
    private PropertyEditor exportedPropertyEditor;
    private PropertyEditor metaFilePropertyEditor;
    private PropertyEditor groupPropertyEditor;
    private PropertyEditor saveGroupEditor;
    boolean dirty;

    public void setSelection(WorkflowElement selection) {
        if (workflowPackage != null)
            workflowPackage.removeElementChangeListener(this);

        workflowPackage = (WorkflowPackage) selection;
        workflowPackage.addElementChangeListener(this);

        idPropertyEditor.setElement(workflowPackage);
        idPropertyEditor.setValue(workflowPackage.getIdLabel());

        namePropertyEditor.setElement(workflowPackage);
        namePropertyEditor.setValue(workflowPackage.getName());

        schemaVersionPropertyEditor.setElement(workflowPackage);
        schemaVersionPropertyEditor
                .setValue(RuleSetVO.formatVersion(workflowPackage.getSchemaVersion()));

        lastModifiedPropertyEditor.setElement(workflowPackage);
        lastModifiedPropertyEditor.setValue(workflowPackage.getModifyDate());

        if (metaFilePropertyEditor != null) {
            metaFilePropertyEditor.setElement(workflowPackage);
            metaFilePropertyEditor.setValue(workflowPackage.getMetaFolder().getName() + "/"
                    + workflowPackage.getMetaFile().getName());
        }

        if (exportedPropertyEditor != null) {
            exportedPropertyEditor.setElement(workflowPackage);
            exportedPropertyEditor.setValue(String.valueOf(workflowPackage.isExported()));
        }

        groupPropertyEditor.setElement(workflowPackage);
        groupPropertyEditor.setValue(UserGroupVO.COMMON_GROUP.equals(getPackage().getGroup()) ? ""
                : getPackage().getGroup());
        groupPropertyEditor.setVisible(workflowPackage.getProject().checkRequiredVersion(5, 5, 8));
        groupPropertyEditor.setEditable(!workflowPackage.isReadOnly());

        saveGroupEditor.setElement(workflowPackage);
        saveGroupEditor.setLabel("Save");
        saveGroupEditor.setVisible(workflowPackage.getProject().checkRequiredVersion(5, 5, 8));
        saveGroupEditor.setEnabled(dirty);
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        workflowPackage = (WorkflowPackage) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        // name text field
        namePropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
        namePropertyEditor.setLabel("Name");
        namePropertyEditor.setReadOnly(true);
        namePropertyEditor.render(composite);

        // schema version text field
        schemaVersionPropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
        schemaVersionPropertyEditor.setLabel("Schema Version");
        schemaVersionPropertyEditor.setWidth(150);
        schemaVersionPropertyEditor.setReadOnly(true);
        schemaVersionPropertyEditor.render(composite);

        // last modified text field
        lastModifiedPropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
        lastModifiedPropertyEditor.setLabel("Last Modified");
        lastModifiedPropertyEditor.setWidth(150);
        lastModifiedPropertyEditor.setReadOnly(true);
        lastModifiedPropertyEditor.setValueConverter(new DateConverter());
        lastModifiedPropertyEditor.render(composite);

        if (workflowPackage.getProject().isFilePersist()) {
            // metafile text field
            metaFilePropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
            metaFilePropertyEditor.setLabel("Meta File");
            metaFilePropertyEditor.setWidth(150);
            metaFilePropertyEditor.setReadOnly(true);
            metaFilePropertyEditor.render(composite);
        }
        else {
            // exported text field
            exportedPropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
            exportedPropertyEditor.setLabel("Exported");
            exportedPropertyEditor.setWidth(100);
            exportedPropertyEditor.setReadOnly(true);
            exportedPropertyEditor.render(composite);
        }

        groupPropertyEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_COMBO);
        groupPropertyEditor.setLabel("Workgroup");
        groupPropertyEditor.setWidth(300);
        groupPropertyEditor.setReadOnly(true);
        List<String> options = new ArrayList<String>();
        options.add("");
        options.addAll(getDesignerDataModel().getWorkgroupNames());
        groupPropertyEditor.setValueOptions(options);
        groupPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                String group = newValue == null || newValue.toString().length() == 0
                        ? UserGroupVO.COMMON_GROUP : newValue.toString();
                workflowPackage.setGroup(group);
                dirty = true;
                saveGroupEditor.setEnabled(dirty);
            }
        });
        groupPropertyEditor.render(composite);

        saveGroupEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_BUTTON);
        saveGroupEditor.setLabel("Save");
        saveGroupEditor.setWidth(65);
        saveGroupEditor.setEnabled(dirty);
        saveGroupEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                savePackage();
            }
        });
        saveGroupEditor.render(composite);

    }

    private void savePackage() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                try {
                    getDesignerProxy().savePackage(workflowPackage);
                    dirty = false;
                    getDesignerProxy().getCacheRefresh().fireRefresh(false);
                }
                catch (Exception ex) {
                    PluginMessages.uiError(getShell(), ex, "Save Package",
                            workflowPackage.getProject());
                }
            }
        });
        saveGroupEditor.setEnabled(dirty);
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getElement().equals(workflowPackage)) {
            if (ece.getChangeType().equals(ChangeType.RENAME)) {
                if (!namePropertyEditor.getValue().equals(ece.getNewValue()))
                    namePropertyEditor.setValue(ece.getNewValue().toString());
                notifyLabelChange();
            }
            else if (ece.getChangeType().equals(ChangeType.STATUS_CHANGE)) {
                if (exportedPropertyEditor != null) {
                    Boolean exported = ece.getNewValue().equals(WorkflowPackage.STATUS_EXPORTED);
                    if (!exportedPropertyEditor.getValue().equals(exported.toString()))
                        exportedPropertyEditor.setValue(exported.toString());
                    notifyLabelChange();
                }
            }
            else if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE)) {
                if (exportedPropertyEditor != null) {
                    Boolean exported = new Boolean(false);
                    if (exportedPropertyEditor.getValue().equals(exported))
                        exportedPropertyEditor.setValue(exported);
                    notifyLabelChange();
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (workflowPackage != null)
            workflowPackage.removeElementChangeListener(this);
    }

}