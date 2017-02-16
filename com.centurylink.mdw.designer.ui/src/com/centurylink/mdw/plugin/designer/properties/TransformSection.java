/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ArtifactEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.value.TransformEditorValueProvider;

public class TransformSection extends PropertySection implements IFilter {
    private Activity activity;

    private ArtifactEditor artifactEditor;
    private PropertyEditor inputDocumentEditor;
    private PropertyEditor outputDocumentEditor;
    private PropertyEditor helpPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;
        if (activity.getTransformLanguage() == null)
            activity.setTransformLanguage("GPath");

        artifactEditor.setElement(activity);
        artifactEditor.setValueProvider(new TransformEditorValueProvider(activity));
        artifactEditor.setEditable(!activity.isReadOnly());

        inputDocumentEditor.setElement(activity);
        inputDocumentEditor.setValue(activity.getAttribute("Input Documents"));
        inputDocumentEditor.setEditable(!activity.isReadOnly());

        outputDocumentEditor.setElement(activity);
        outputDocumentEditor.setValue(activity.getAttribute("Output Documents"));
        outputDocumentEditor.setEditable(!activity.isReadOnly());

        helpPropertyEditor.setValue("/MDWHub/doc/documentTransform.html");
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        activity = (Activity) selection;

        // artifact editor
        artifactEditor = new ArtifactEditor(activity, new TransformEditorValueProvider(activity),
                null);
        artifactEditor.render(composite);

        // input document combo
        inputDocumentEditor = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
        inputDocumentEditor.setLabel("Input Document");
        inputDocumentEditor.setWidth(150);
        inputDocumentEditor.setReadOnly(true);
        inputDocumentEditor.setValueOptions(activity.getProcess().getDocRefVariableNames());
        inputDocumentEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute("Input Documents", (String) newValue);
            }
        });
        inputDocumentEditor.render(composite);

        // output document combo
        outputDocumentEditor = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
        outputDocumentEditor.setLabel("Output Document");
        outputDocumentEditor.setWidth(150);
        outputDocumentEditor.setReadOnly(true);
        outputDocumentEditor.setValueOptions(activity.getProcess().getDocRefVariableNames());
        outputDocumentEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute("Output Documents", (String) newValue);
            }
        });
        outputDocumentEditor.render(composite);

        // help link
        helpPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
        helpPropertyEditor.setLabel("Transform Activity Help");
        helpPropertyEditor.render(composite);

    }

    /**
     * For IFilter interface, determine which activities include this section in
     * their Design properties tab page.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        activity = (Activity) toTest;

        if (activity.isForProcessInstance())
            return false;

        if (activity.getActivityImpl().getAttrDescriptionXml() == null)
            return false;

        return TransformEditorValueProvider.isTransformActivity(activity);
    }
}
