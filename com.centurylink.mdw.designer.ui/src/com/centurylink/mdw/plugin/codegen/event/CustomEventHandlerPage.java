/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.event;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;
import com.centurylink.mdw.plugin.codegen.event.EventHandlerWizard.HandlerAction;

public class CustomEventHandlerPage extends EventHandlerPage
{
  public CustomEventHandlerPage()
  {
    setTitle("Event Handler Settings");
    setDescription("Enter the information for your Event Handler.\n"
      + "This will be used to generate the source code for your implementation.");
  }

  public void init(IStructuredSelection selection)
  {
    super.init(selection);
  }

  @Override
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout for this wizard page
    GridLayout gl = new GridLayout();
    int ncol = 4;
    gl.numColumns = ncol;
    composite.setLayout(gl);

    createInfoControls(composite, ncol, getCodeGenWizard().getInfoLabelLabel());
    createSepLine(composite, ncol);
    createProcessLaunchNotifyControls(composite, true, ncol);
    createDocumentControls(composite, ncol);
    createExternalEventHelpControls(composite, ncol, false);

    setControl(composite);
  }

  @Override
  public void saveDataToModel()
  {
    // clear unused fields since presence is used by templates to control code generation
    if (getEventHandlerWizard().getHandlerAction() == HandlerAction.launchProcess)
      getEventHandler().setEvent(null);
    else if (getEventHandlerWizard().getHandlerAction() == HandlerAction.notifyProcess)
      getEventHandler().setProcess(null);
  }

  @Override
  protected boolean isPageValid()
  {
    return getStatuses() == null;
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getEventHandlerWizard().getCodeGenType() != CodeGenType.registrationOnly)
    {
      if (getEventHandlerWizard().getHandlerAction() == HandlerAction.launchProcess && !checkString(getEventHandler().getProcess()))
        msg = "Please select a process to invoke via the built-in handler";
      else if (getEventHandlerWizard().getHandlerAction() == HandlerAction.notifyProcess && !checkString(getEventHandler().getEvent()))
        msg = "Please enter an Event ID for the built-in handler notification";
    }

    if (msg == null)
      return null;
    else
      return new IStatus[] {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
  }

  @Override
  public IWizardPage getNextPage()
  {
    return null;
  }
}
