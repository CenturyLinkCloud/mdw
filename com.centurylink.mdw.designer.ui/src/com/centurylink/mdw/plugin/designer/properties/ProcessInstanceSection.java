/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

public class ProcessInstanceSection extends PropertySection implements IFilter
{
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  private PropertyEditor idPropertyEditor;
  private PropertyEditor masterRequestIdPropertyEditor;
  private PropertyEditor statusPropertyEditor;
  private PropertyEditor ownerPropertyEditor;
  private PropertyEditor ownerIdPropertyEditor;
  private PropertyEditor startDatePropertyEditor;
  private PropertyEditor endDatePropertyEditor;
  private PropertyEditor processLabelPropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    process = (WorkflowProcess) selection;
    ProcessInstanceVO instanceInfo = process.getProcessInstance();

    idPropertyEditor.setElement(process);
    idPropertyEditor.setValue(instanceInfo.getId());

    masterRequestIdPropertyEditor.setElement(process);
    masterRequestIdPropertyEditor.setValue(instanceInfo.getMasterRequestId());

    statusPropertyEditor.setElement(process);
    statusPropertyEditor.setValue(WorkStatuses.getWorkStatuses().get(new Integer(instanceInfo.getStatusCode())));

    ownerPropertyEditor.setElement(process);
    ownerPropertyEditor.setValue(instanceInfo.getOwner());

    ownerIdPropertyEditor.setElement(process);
    ownerIdPropertyEditor.setValue(instanceInfo.getOwnerId());

    startDatePropertyEditor.setElement(process);
    if (instanceInfo.getStartDate() != null)
      startDatePropertyEditor.setValue(instanceInfo.getStartDate().toString());

    endDatePropertyEditor.setElement(process);
    if (instanceInfo.getEndDate() != null)
      endDatePropertyEditor.setValue(instanceInfo.getEndDate().toString());

    processLabelPropertyEditor.setElement(process);
    if (instanceInfo.getComment() != null)
      processLabelPropertyEditor.setValue(instanceInfo.getComment());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    process = (WorkflowProcess) selection;

    // id text field
    idPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    idPropertyEditor.setLabel("Instance ID");
    idPropertyEditor.setWidth(150);
    idPropertyEditor.setReadOnly(true);
    idPropertyEditor.render(composite);

    // master request id text field
    masterRequestIdPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    masterRequestIdPropertyEditor.setLabel("Master Req. ID");
    masterRequestIdPropertyEditor.setWidth(150);
    masterRequestIdPropertyEditor.setReadOnly(true);
    masterRequestIdPropertyEditor.render(composite);

    // status text field
    statusPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    statusPropertyEditor.setLabel("Status");
    statusPropertyEditor.setWidth(150);
    statusPropertyEditor.setReadOnly(true);
    statusPropertyEditor.render(composite);

    // owner text field
    ownerPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    ownerPropertyEditor.setLabel("Owner");
    ownerPropertyEditor.setWidth(150);
    ownerPropertyEditor.setReadOnly(true);
    ownerPropertyEditor.render(composite);

    // owner id text field
    ownerIdPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    ownerIdPropertyEditor.setLabel("Owner ID");
    ownerIdPropertyEditor.setWidth(150);
    ownerIdPropertyEditor.setReadOnly(true);
    ownerIdPropertyEditor.render(composite);

    // start date text field
    startDatePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    startDatePropertyEditor.setLabel("Start");
    startDatePropertyEditor.setWidth(200);
    startDatePropertyEditor.setReadOnly(true);
    startDatePropertyEditor.render(composite);

    // end date text field
    endDatePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    endDatePropertyEditor.setLabel("Finish");
    endDatePropertyEditor.setWidth(200);
    endDatePropertyEditor.setReadOnly(true);
    endDatePropertyEditor.render(composite);

    // label text field
    processLabelPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    processLabelPropertyEditor.setLabel("Label: ");
    processLabelPropertyEditor.setWidth(500);
    processLabelPropertyEditor.setReadOnly(true);
    processLabelPropertyEditor.render(composite);

  }

  /**
   * For IFilter interface.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowProcess))
      return false;

    process = (WorkflowProcess) toTest;
    return process.hasInstanceInfo();
  }

}