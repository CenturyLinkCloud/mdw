/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ExpressionSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }
  public void setActivity(Activity a) { this.activity = a; }

  private PropertyEditor languagePropertyEditor;
  private PropertyEditor expressionPropertyEditor;
  private PropertyEditor helpPropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    activity = (Activity) selection;
    if (activity.getId() < 0 && activity.getScriptLanguage() == null)
      activity.setScriptLanguage("Groovy");

    languagePropertyEditor.setElement(activity);
    languagePropertyEditor.setValue(activity.getScriptLanguage());
    languagePropertyEditor.setEditable(!activity.isReadOnly());

    expressionPropertyEditor.setElement(activity);
    expressionPropertyEditor.setValue(activity.getAttribute("Expression"));
    expressionPropertyEditor.setEditable(!activity.isReadOnly());

    helpPropertyEditor.setValue("/MDWHub/doc/scriptActivity.html");
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    // language combo
    languagePropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_COMBO);
    languagePropertyEditor.setLabel("Language");
    languagePropertyEditor.setReadOnly(true);
    languagePropertyEditor.setWidth(150);
    languagePropertyEditor.setValueOptions(activity.getScriptLanguages());
    languagePropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        activity.setScriptLanguage((String)newValue);
      }
    });
    languagePropertyEditor.render(composite);

    // expression editor
    expressionPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_TEXT);
    expressionPropertyEditor.setLabel("Expression");
    expressionPropertyEditor.setWidth(400);
    expressionPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        activity.setAttribute("Expression", (String)newValue);
      }
    });
    expressionPropertyEditor.render(composite);

    // help link
    helpPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
    helpPropertyEditor.setLabel("Expression Activity Help");
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

    if (activity.getActivityImpl().getAttrDescriptionXml() == null)
      return false;

    PropertyEditorList propEditorList = new PropertyEditorList(activity);
    for (PropertyEditor propertyEditor : propEditorList)
    {
      if (propertyEditor.getType().equals(PropertyEditor.TYPE_SCRIPT))
      {
        activity.setScriptLanguages(propertyEditor.getScriptLanguages());
        return "EXPRESSION".equals(propertyEditor.getScriptType());
      }
    }
    return false;
  }
}