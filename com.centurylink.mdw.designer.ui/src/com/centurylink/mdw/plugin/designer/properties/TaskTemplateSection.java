/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class TaskTemplateSection extends PropertySection
{
  private TaskTemplate taskTemplate;
  public TaskTemplate getTaskTemplate() { return taskTemplate; }

  private PropertyEditor idEditor;
  private PropertyEditor nameEditor;

  public void setSelection(WorkflowElement selection)
  {
    taskTemplate = (TaskTemplate) selection;

    idEditor.setElement(taskTemplate);
    idEditor.setValue(taskTemplate.getId());

    nameEditor.setElement(taskTemplate);
    nameEditor.setValue(taskTemplate.getName());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    taskTemplate = (TaskTemplate) selection;

    // id text field
    idEditor = new PropertyEditor(taskTemplate, PropertyEditor.TYPE_TEXT);
    idEditor.setLabel("ID");
    idEditor.setWidth(150);
    idEditor.setReadOnly(true);
    idEditor.render(composite);

    // name text field
    nameEditor = new PropertyEditor(taskTemplate, PropertyEditor.TYPE_TEXT);
    nameEditor.setLabel("Name");
    nameEditor.setWidth(300);
    nameEditor.setReadOnly(true);
    nameEditor.render(composite);

  }

}