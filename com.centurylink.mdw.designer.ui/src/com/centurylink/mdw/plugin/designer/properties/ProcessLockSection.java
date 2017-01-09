/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.DateConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ProcessLockSection extends PropertySection implements IFilter, ElementChangeListener
{
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  private PropertyEditor lockUserPropertyEditor;
  private PropertyEditor lockDatePropertyEditor;
  private PropertyEditor lockButtonPropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (process != null)
      process.removeElementChangeListener(this);

    process = (WorkflowProcess) selection;
    process.addElementChangeListener(this);

    lockUserPropertyEditor.setElement(process);
    lockUserPropertyEditor.setValue(process.getLockingUser());

    lockDatePropertyEditor.setElement(process);
    lockDatePropertyEditor.setValue(process.getLockedDate());

    lockButtonPropertyEditor.setElement(process);
    lockButtonPropertyEditor.setLabel(process.isLockedToUser() ? "Unlock" : "Lock");
    lockButtonPropertyEditor.setEnabled(process.isLockedToUser() && !process.isDummy()
        || (process.getLockingUser() == null && process.isUserAuthorized(UserRoleVO.ASSET_DESIGN)));
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    process = (WorkflowProcess) selection;

    // lock user read-only text field
    lockUserPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    lockUserPropertyEditor.setLabel("Locked By");
    lockUserPropertyEditor.setWidth(150);
    lockUserPropertyEditor.setReadOnly(true);
    lockUserPropertyEditor.render(composite);

    // lock date read-only text field
    lockDatePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
    lockDatePropertyEditor.setLabel("Lock Date");
    lockDatePropertyEditor.setWidth(150);
    lockDatePropertyEditor.setValueConverter(new DateConverter());
    lockDatePropertyEditor.setReadOnly(true);
    lockDatePropertyEditor.render(composite);

    // lock button
    lockButtonPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_BUTTON);
    lockButtonPropertyEditor.setLabel("");
    lockButtonPropertyEditor.setWidth(65);
    lockButtonPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        toggleProcessLock();
      }
    });
    lockButtonPropertyEditor.render(composite);
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowProcess))
      return false;

    WorkflowProcess pv = (WorkflowProcess) toTest;
    if (pv.getProject().isFilePersist() || pv.isArchived() || pv.hasInstanceInfo())
      return false;

    return true;
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getElement().equals(process))
    {
      if (ece.getChangeType().equals(ChangeType.PROPERTIES_CHANGE))
      {
        WorkflowProcess changedVersion = (WorkflowProcess) ece.getElement();
        if (!lockUserPropertyEditor.getValue().equals(changedVersion.getLockingUser()))
        {
          lockUserPropertyEditor.setValue(changedVersion.getLockingUser());
          lockDatePropertyEditor.setValue(changedVersion.getLockedDate());
          lockButtonPropertyEditor.setLabel(changedVersion.isLockedToUser() ? "Unlock" : "Lock");
        }
        notifyLabelChange();
      }
    }
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (process != null)
      process.removeElementChangeListener(this);
  }

  private void toggleProcessLock()
  {
    IEditorPart editor = getPart().getSite().getPage().findEditor(process);
    if (editor != null && editor instanceof ProcessEditor)
    {
      ((ProcessEditor)editor).toggleProcessLock(!process.isLockedToUser());
    }
  }

}