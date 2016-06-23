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
import com.centurylink.mdw.plugin.designer.properties.value.ScriptEditorValueProvider;

/**
 * Specialized Design tab section for Script Activities.
 */
public class ScriptSection extends PropertySection implements IFilter
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
    if (activity.getScriptLanguage() == null)
    {
      activity.setScriptLanguage(artifactEditor.getValueProvider().getDefaultLanguage());
      activity.fireAttributeValueChanged("SCRIPT", activity.getScriptLanguage());
    }

    artifactEditor.setElement(activity);
    artifactEditor.setValueProvider(new ScriptEditorValueProvider(activity));
    artifactEditor.setEditable(!activity.isReadOnly());

    if (activity.canWriteOutputDocs())
    {
      outputDocsPropertyEditor.setElement(activity);
      outputDocsPropertyEditor.setValue(activity.getAttribute("Output Documents"));
      outputDocsPropertyEditor.setEditable(!activity.isReadOnly());
      outputDocsPropertyEditor.setVisible(true);
    }
    else
    {
      outputDocsPropertyEditor.setVisible(false);
    }

    helpPropertyEditor.setValue("/MDWHub/doc/scriptActivity.html");
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    // artifact editor
    artifactEditor = new ArtifactEditor(activity, new ScriptEditorValueProvider(activity), null);
    artifactEditor.render(composite);

    // output docs
    outputDocsPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_PICKLIST);
    outputDocsPropertyEditor.setLabel("Documents:Read-Only~Writable");

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
    helpPropertyEditor.setLabel("Script Activity Help");
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

    activity = (Activity) toTest;
    if (activity.isForProcessInstance())
      return false;

    return showScriptSection(activity);
  }
}