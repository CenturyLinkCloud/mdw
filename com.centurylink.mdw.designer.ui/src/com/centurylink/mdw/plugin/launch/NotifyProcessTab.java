/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * TODO: Ability to add message headers.
 */
public class NotifyProcessTab extends AbstractLaunchConfigurationTab
{
  private WorkflowProject workflowProject;

  private Button notifyProcessCheckbox;
  private Text eventNameText;
  private Text requestText;

  private Image image = MdwPlugin.getImageDescriptor("icons/notify.gif").createImage();

  public NotifyProcessTab()
  {
  }

  public void createControl(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    setControl(composite);

    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 2;
    composite.setLayout(topLayout);

    createNotifyProcessSection(composite);
  }

  public String getName()
  {
    return "Notify Process";
  }

  public Image getImage()
  {
    return image;
  }

  public void initializeFrom(ILaunchConfiguration launchConfig)
  {
    try
    {
      String wfProject = launchConfig.getAttribute(WorkflowLaunchConfiguration.WORKFLOW_PROJECT, "");
      workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(wfProject);

      boolean notifyProcess = launchConfig.getAttribute(ProcessLaunchConfiguration.NOTIFY_PROCESS, false);
      notifyProcessCheckbox.setSelection(notifyProcess);
      eventNameText.setEnabled(notifyProcess);
      requestText.setEnabled(notifyProcess);

      String eventName = launchConfig.getAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_EVENT, "");
      eventNameText.setText(eventName);
      String request = launchConfig.getAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_REQUEST, "");
      requestText.setText(request);

      validatePage();
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(ex, "Launch Init", workflowProject);
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy launchConfig)
  {
    boolean notifyProcess = notifyProcessCheckbox.getSelection();
    launchConfig.setAttribute(ProcessLaunchConfiguration.NOTIFY_PROCESS, notifyProcess);
    if (notifyProcess)
      launchConfig.setAttribute(ProcessLaunchConfiguration.LAUNCH_VIA_EXTERNAL_EVENT, false);
    String eventName = eventNameText.getText();
    launchConfig.setAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_EVENT, eventName);
    String request = requestText.getText();
    launchConfig.setAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_REQUEST, request);
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig)
  {
    launchConfig.setAttribute(ProcessLaunchConfiguration.NOTIFY_PROCESS, false);
    launchConfig.setAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_EVENT, "");
    launchConfig.setAttribute(WorkflowLaunchConfiguration.NOTIFY_PROCESS_REQUEST, "");
  }

  private void createNotifyProcessSection(Composite parent)
  {
    notifyProcessCheckbox = new Button(parent, SWT.CHECK);
    notifyProcessCheckbox.setText("Notify Process");
    notifyProcessCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        eventNameText.setEnabled(notifyProcessCheckbox.getSelection());
        requestText.setEnabled(notifyProcessCheckbox.getSelection());
        setDirty(true);
        validatePage();
      }
    });

    Group requestGroup = new Group(parent, SWT.NONE);
    requestGroup.setText("Notify Process Request");
    GridLayout gl = new GridLayout();
    gl.numColumns = 1;
    requestGroup.setLayout(gl);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 2;
    requestGroup.setLayoutData(gd);

    new Label(requestGroup, SWT.NONE).setText("Event Name");
    eventNameText = new Text(requestGroup, SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    eventNameText.setLayoutData(gd);
    eventNameText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    new Label(requestGroup, SWT.NONE).setText("Message to Activity");
    requestText = new Text(requestGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    gd = new GridData(GridData.FILL_BOTH);
    requestText.setLayoutData(gd);
    requestText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });
  }

  private void validatePage()
  {
    setErrorMessage(null);
    setMessage(null);

    if (notifyProcessCheckbox.getSelection())
    {
      if (eventNameText.getText().trim().length() == 0)
      {
        setErrorMessage("Please enter the notify event name.");
        updateLaunchConfigurationDialog();
        return;
      }
      if (requestText.getText().trim().length() == 0)
      {
        setErrorMessage("Please enter the notify event request data.");
        updateLaunchConfigurationDialog();
        return;
      }
    }

    updateLaunchConfigurationDialog();
  }

  @Override
  public boolean canSave()
  {
    return getErrorMessage() == null;
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig)
  {
    return canSave();
  }

  public void dispose()
  {
    super.dispose();
    image.dispose();
  }
}
