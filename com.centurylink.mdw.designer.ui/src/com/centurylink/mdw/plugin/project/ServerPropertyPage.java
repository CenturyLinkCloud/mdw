/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

public class ServerPropertyPage extends ProjectPropertyPage
{
  private Text javaOptionsTextField;
  private Button debugCheckbox;
  private Group debugGroup;
  private Label debugPortLabel;
  private Text debugPortTextField;
  private Button suspendCheckbox;

  private Text stubServerHostTextField;
  private Text stubServerPortTextField;
  private Spinner stubServerTimeoutSpinner;

  private Text logWatcherHostTextField;
  private Text logWatcherPortTextField;
  private Spinner logWatcherTimeoutSpinner;

  private ServerSettings workingCopy;

  @Override
  protected Control createContents(Composite parent)
  {
    noDefaultAndApplyButton();
    initializeWorkflowProject();

    workingCopy = new ServerSettings(getProject().getServerSettings());

    Composite composite = createComposite(parent);

    if (!workingCopy.isOsgi())
    {
      createServerRunnerControls(composite);
      addSeparator(composite);
    }
    createLogWatcherControls(composite);
    addSeparator(composite);
    createStubServerControls(composite);

    return composite;
  }

  private void createServerRunnerControls(Composite parent)
  {
    addHeading(parent, "MDW Server Runner");

    Composite composite = createComposite(parent, 4);

    Label javaOptionsLabel = new Label(composite, SWT.NONE);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 4;
    javaOptionsLabel.setLayoutData(gd);
    javaOptionsLabel.setText("Java Options:");

    javaOptionsTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 400;
    gd.horizontalSpan = 4;
    javaOptionsTextField.setLayoutData(gd);
    javaOptionsTextField.setText(workingCopy.getJavaOptions() == null ? "" : workingCopy.getJavaOptions());
    javaOptionsTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String opts = javaOptionsTextField.getText().trim();
        workingCopy.setJavaOptions(opts == "" ? null : opts);
      }
    });

    // debug options
    debugCheckbox = new Button(composite, SWT.CHECK);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    gd.verticalIndent = 5;
    debugCheckbox.setLayoutData(gd);
    debugCheckbox.setText("Run in Debug Mode");
    debugCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean debug = debugCheckbox.getSelection();
        workingCopy.setDebug(debug);
        enableDebugControls(debug);
      }
    });
    debugCheckbox.setSelection(workingCopy.isDebug());

    debugGroup = new Group(composite, SWT.NONE);
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    debugGroup.setLayout(gl);
    debugGroup.setText("Debug Settings");
    gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 4;
    debugGroup.setLayoutData(gd);

    debugPortLabel = new Label(debugGroup, SWT.NONE);
    debugPortLabel.setText("Debug Port:");

    debugPortTextField = new Text(debugGroup, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 100;
    gd.verticalIndent = 5;
    debugPortTextField.setLayoutData(gd);
    debugPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String port = debugPortTextField.getText().trim();
        workingCopy.setDebugPort(port.trim().length() == 0 ? 0 : Integer.parseInt(port.trim()));
      }
    });
    debugPortTextField.setText(String.valueOf(workingCopy.getDebugPort()));

    suspendCheckbox =  new Button(debugGroup, SWT.CHECK);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalIndent = 20;
    gd.verticalIndent = 5;
    suspendCheckbox.setLayoutData(gd);
    suspendCheckbox.setText("Suspend on Startup");
    suspendCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        workingCopy.setSuspend(suspendCheckbox.getSelection());
      }
    });
    suspendCheckbox.setSelection(workingCopy.isSuspend());

    // initial enablement
    enableDebugControls(workingCopy.isDebug());
  }

  private void createLogWatcherControls(Composite parent)
  {
    addHeading(parent, "Log Watcher");

    Composite composite = createComposite(parent, 2);

    // host
    new Label(composite, SWT.NONE).setText("Log Watch Host:");
    logWatcherHostTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 225;
    gd.verticalIndent = 5;
    logWatcherHostTextField.setLayoutData(gd);
    logWatcherHostTextField.setText("localhost");
    logWatcherHostTextField.setEditable(false);  // wired to local host

    // port
    new Label(composite, SWT.NONE).setText("Log Watch Port:");
    logWatcherPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 100;
    gd.verticalIndent = 5;
    logWatcherPortTextField.setLayoutData(gd);
    logWatcherPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String port = logWatcherPortTextField.getText().trim();
        workingCopy.setLogWatcherPort(Integer.parseInt(port));
      }
    });
    logWatcherPortTextField.setText(String.valueOf(workingCopy.getLogWatcherPort()));

    // timeout
    new Label(composite, SWT.NONE).setText("Timeout (secs):");
    logWatcherTimeoutSpinner = new Spinner(composite, SWT.BORDER);
    logWatcherTimeoutSpinner.setMinimum(10);
    logWatcherTimeoutSpinner.setMaximum(300);
    logWatcherTimeoutSpinner.setIncrement(10);
    logWatcherTimeoutSpinner.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        int timeout = logWatcherTimeoutSpinner.getSelection();
        workingCopy.setLogWatcherTimeout(timeout);
      }
    });
    logWatcherTimeoutSpinner.setSelection(workingCopy.getLogWatcherTimeout());
  }

  private void createStubServerControls(Composite parent)
  {
    addHeading(parent, "Stub Server");

    Composite composite = createComposite(parent, 2);

    // stub host
    new Label(composite, SWT.NONE).setText("Stub Server Host:");
    stubServerHostTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 225;
    gd.verticalIndent = 5;
    stubServerHostTextField.setLayoutData(gd);
    stubServerHostTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String host = stubServerHostTextField.getText().trim();
        workingCopy.setStubServerHost(host);
      }
    });
    stubServerHostTextField.setText("localhost");
    stubServerHostTextField.setEditable(false);  // wired to local host

    // stub port
    new Label(composite, SWT.NONE).setText("Stub Server Port:");
    stubServerPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 100;
    gd.verticalIndent = 5;
    stubServerPortTextField.setLayoutData(gd);
    stubServerPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String port = stubServerPortTextField.getText().trim();
        workingCopy.setStubServerPort(Integer.parseInt(port));
      }
    });
    stubServerPortTextField.setText(String.valueOf(workingCopy.getStubServerPort()));

    // timeout
    new Label(composite, SWT.NONE).setText("Timeout (secs):");
    stubServerTimeoutSpinner = new Spinner(composite, SWT.BORDER);
    stubServerTimeoutSpinner.setMinimum(10);
    stubServerTimeoutSpinner.setMaximum(300);
    stubServerTimeoutSpinner.setIncrement(10);
    stubServerTimeoutSpinner.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        int timeout = stubServerTimeoutSpinner.getSelection();
        workingCopy.setStubServerTimeout(timeout);
      }
    });
    stubServerTimeoutSpinner.setSelection(workingCopy.getStubServerTimeout());
  }

  private void enableDebugControls(boolean enabled)
  {
    if (!enabled)
    {
      debugPortTextField.setText("");
      suspendCheckbox.setSelection(false);
    }
    else
    {
      if (workingCopy.getDebugPort() == 0)
        debugPortTextField.setText("8500");
      else
        debugPortTextField.setText(String.valueOf(workingCopy.getDebugPort()));
    }
    debugGroup.setEnabled(enabled);
    debugPortLabel.setEnabled(enabled);
    debugPortTextField.setEnabled(enabled);
    suspendCheckbox.setEnabled(enabled);
  }

  public boolean performOk()
  {
    if (!workingCopy.equals(getProject().getServerSettings()))
    {
      getProject().setServerSettings(workingCopy);
      getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, workingCopy);
    }

    IProject project = (IProject) getElement();
    try
    {
      WorkflowProjectManager.getInstance().save(getProject(), project);
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(getShell(), ex, "Project Settings", getProject());
      return false;
    }

    return true;
  }

}
