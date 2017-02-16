/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class EmbeddedSubProcessSection extends PropertySection {
    private EmbeddedSubProcess embeddedSubProcess;

    public EmbeddedSubProcess getEmbeddedSubProcess() {
        return embeddedSubProcess;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor namePropertyEditor;
    private PropertyEditor descriptionPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        embeddedSubProcess = (EmbeddedSubProcess) selection;

        idPropertyEditor.setElement(embeddedSubProcess);
        idPropertyEditor.setValue(embeddedSubProcess.getId());

        namePropertyEditor.setElement(embeddedSubProcess);
        namePropertyEditor.setValue(embeddedSubProcess.getName());
        namePropertyEditor.setEditable(!embeddedSubProcess.isReadOnly());

        descriptionPropertyEditor.setElement(embeddedSubProcess);
        descriptionPropertyEditor.setValue(embeddedSubProcess.getDescription());
        descriptionPropertyEditor.setEditable(!embeddedSubProcess.isReadOnly());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        embeddedSubProcess = (EmbeddedSubProcess) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(embeddedSubProcess, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        // name text field
        namePropertyEditor = new PropertyEditor(embeddedSubProcess, PropertyEditor.TYPE_TEXT);
        namePropertyEditor.setLabel("Name");
        namePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                embeddedSubProcess.setName((String) newValue);
            }
        });
        namePropertyEditor.render(composite);

        // description text area
        descriptionPropertyEditor = new PropertyEditor(embeddedSubProcess,
                PropertyEditor.TYPE_TEXT);
        descriptionPropertyEditor.setLabel("Description");
        descriptionPropertyEditor.setHeight(100);
        descriptionPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                embeddedSubProcess.setDescription((String) newValue);
            }
        });
        descriptionPropertyEditor.render(composite);

    }
}