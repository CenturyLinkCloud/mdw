/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ActivityImplDesignSection extends PropertySection
{
  private ActivityImpl activityImpl;
  public ActivityImpl getActivityImpl() { return activityImpl; }

  private PropertyEditor baseClassPropertyEditor;
  private PropertyEditor attrDescriptionPropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    activityImpl = (ActivityImpl) selection;

    baseClassPropertyEditor.setElement(activityImpl);
    baseClassPropertyEditor.setValue(activityImpl.getBaseClassName());
    baseClassPropertyEditor.setEditable(!activityImpl.isReadOnly());

    attrDescriptionPropertyEditor.setElement(activityImpl);
    attrDescriptionPropertyEditor.setValue(activityImpl.getAttrDescriptionXml());
    attrDescriptionPropertyEditor.setEditable(!activityImpl.isReadOnly());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activityImpl = (ActivityImpl) selection;

    // base class combo
    baseClassPropertyEditor = new PropertyEditor(activityImpl, PropertyEditor.TYPE_COMBO);
    baseClassPropertyEditor.setLabel("Category");
    baseClassPropertyEditor.setReadOnly(true);
    baseClassPropertyEditor.setWidth(475);
    List<String> implementorBaseClasses = new ArrayList<String>();
    if (!activityImpl.getProject().checkRequiredVersion(5, 5))
    {
      implementorBaseClasses = Arrays.asList(ActivityImpl.getOldBaseClasses());
    }
    else
    {
      for (Class<?> baseClass : ActivityImpl.getBaseClasses())
        implementorBaseClasses.add(baseClass.getName());
    }
    baseClassPropertyEditor.setValueOptions(implementorBaseClasses);

    baseClassPropertyEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          activityImpl.setBaseClassName((String)newValue);
        }
      });
    baseClassPropertyEditor.render(composite);

    // attr description text area
    attrDescriptionPropertyEditor = new PropertyEditor(activityImpl, PropertyEditor.TYPE_TEXT);
    attrDescriptionPropertyEditor.setLabel("Pagelet");
    attrDescriptionPropertyEditor.setMultiLine(true);
    attrDescriptionPropertyEditor.setWidth(475);
    attrDescriptionPropertyEditor.setHeight(150);
    attrDescriptionPropertyEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          activityImpl.setAttrDescriptionXml((String)newValue);
        }
      });
    attrDescriptionPropertyEditor.render(composite);
  }
}