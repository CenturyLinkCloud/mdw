/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ActivitySection extends PropertySection {
    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor logicalIdPropertyEditor;
    private PropertyEditor namePropertyEditor;
    private PropertyEditor implementorPropertyEditor;
    private PropertyEditor linkPropertyEditor;
    private PropertyEditor descriptionPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;

        idPropertyEditor.setElement(activity);
        idPropertyEditor.setValue(activity.getId());

        logicalIdPropertyEditor.setElement(activity);
        logicalIdPropertyEditor.setValue(activity.getLogicalId());

        namePropertyEditor.setElement(activity);
        namePropertyEditor.setValue(activity.getName());
        namePropertyEditor.setEditable(!activity.isReadOnly());

        implementorPropertyEditor.setElement(activity);
        implementorPropertyEditor.setValue(activity.getActivityImpl().getImplClassName());
        implementorPropertyEditor.setEditable(!activity.isReadOnly());

        linkPropertyEditor.setElement(activity);

        descriptionPropertyEditor.setElement(activity);
        descriptionPropertyEditor.setValue(activity.getDescription());
        descriptionPropertyEditor.setEditable(!activity.isReadOnly());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        activity = (Activity) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        // logical id text field
        logicalIdPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_TEXT);
        logicalIdPropertyEditor.setLabel("Logical ID");
        logicalIdPropertyEditor.setWidth(150);
        logicalIdPropertyEditor.setReadOnly(true);
        logicalIdPropertyEditor.render(composite);

        // name text field
        namePropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_TEXT);
        namePropertyEditor.setLabel("Label");
        namePropertyEditor.setMultiLine(true);
        namePropertyEditor.setWidth(475);
        namePropertyEditor.setHeight(30);
        namePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setName((String) newValue);
            }
        });
        namePropertyEditor.render(composite);

        // implementor combo
        implementorPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
        implementorPropertyEditor.setLabel("Implementor");
        implementorPropertyEditor.setWidth(475);
        List<String> implementorNames = new ArrayList<String>();
        for (ActivityImplementorVO implVo : getDataAccess().getActivityImplementors(false))
            implementorNames.add(implVo.getImplementorClassName());
        implementorPropertyEditor.setValueOptions(implementorNames);
        implementorPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setActivityImpl(activity.getProject().getActivityImpl((String) newValue));
            }
        });
        implementorPropertyEditor.render(composite);
        ((Combo) implementorPropertyEditor.getWidget()).setVisibleItemCount(10);

        // implementor link
        linkPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
        linkPropertyEditor.setLabel("Open Implementor Source Code");
        linkPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.getProject().viewSource(activity.getActivityImpl().getImplClassName());
            }
        });
        linkPropertyEditor.render(composite);

        // description text area
        descriptionPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_TEXT);
        descriptionPropertyEditor.setLabel("Description");
        descriptionPropertyEditor.setWidth(475);
        descriptionPropertyEditor.setHeight(70);
        descriptionPropertyEditor.setMultiLine(true);
        descriptionPropertyEditor.setTextLimit(1000);
        descriptionPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setDescription((String) newValue);
            }
        });
        descriptionPropertyEditor.render(composite);
    }
}