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
import com.centurylink.mdw.plugin.designer.properties.value.JavaEditorValueProvider;

public class JavaSection  extends PropertySection implements IFilter
{
    private Activity activity;
    public Activity getActivity() { return activity; }
    public void setActivity(Activity a) { this.activity = a; }

    private ArtifactEditor artifactEditor;
    private PropertyEditor helpPropertyEditor;
    private PropertyEditor outputDocsPropertyEditor;

    public void setSelection(WorkflowElement selection)
    {
      activity = (Activity) selection;

      artifactEditor.setElement(activity);
      artifactEditor.setValueProvider(new JavaEditorValueProvider(activity));
      artifactEditor.setEditable(!activity.isReadOnly());

      outputDocsPropertyEditor.setElement(activity);
      outputDocsPropertyEditor.setValue(activity.getAttribute("Output Documents"));
      outputDocsPropertyEditor.setEditable(!activity.isReadOnly());
      outputDocsPropertyEditor.setVisible(true);

      helpPropertyEditor.setValue("/MDWHub/doc/dynamicJavaActivity.html");
    }

    public void drawWidgets(Composite composite, WorkflowElement selection)
    {
      activity = (Activity) selection;

      // artifact editor
      artifactEditor = new ArtifactEditor(activity, new JavaEditorValueProvider(activity), null);
      artifactEditor.render(composite);

      // output docs
      outputDocsPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_PICKLIST);
      outputDocsPropertyEditor.setLabel("Reference Vars:Read-Only~Writable");

      outputDocsPropertyEditor.setValueOptions(activity.getProcess().getDocRefVariableNames());
      outputDocsPropertyEditor.addValueChangeListener(new ValueChangeListener()
        {
          public void propertyValueChanged(Object newValue)
          {
            activity.setAttribute("Output Documents", (String)newValue);
          }
        });
      outputDocsPropertyEditor.render(composite);
      outputDocsPropertyEditor.setVisible(activity.canWriteOutputDocs());

      // help link
      helpPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
      helpPropertyEditor.setLabel("Dynamic Java Activity Help");
      helpPropertyEditor.render(composite);
    }

    /**
     * For IFilter interface, determine which activities include this section
     * in their Design properties tab page.
     */
    public boolean select(Object toTest)
    {
      if (toTest == null || !(toTest instanceof Activity))
        return false;

      Activity activity = (Activity) toTest;
      if (activity.isForProcessInstance())
        return false;

      return activity.isDynamicJava();
    }
}