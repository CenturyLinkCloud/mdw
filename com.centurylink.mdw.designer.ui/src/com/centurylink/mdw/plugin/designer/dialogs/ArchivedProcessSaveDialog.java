package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ArchivedProcessSaveDialog extends TrayDialog
{
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  private int version;
  public int getVersion() { return version; }

  private boolean respondingToClose;

  public ArchivedProcessSaveDialog(Shell shell, WorkflowProcess process, boolean respondingToClose)
  {
    super(shell);
    this.process = process;
    this.respondingToClose = respondingToClose;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Save Archived Process");

    String msg = respondingToClose ? "Save process changes?\n" : "";
    msg += "WARNING: " + process.getLabel() + " is not the latest version.\n"
          + "It's presumed that you are intentionally updating an\narchived process version to affect in-flight instances.\n"
          + "This action will be audit logged for accountability purposes.";
    new Label(composite, SWT.NONE).setText(msg);

    return composite;
  }

  @Override
  protected void cancelPressed()
  {
    setReturnCode(CANCEL);
    close();
  }

  @Override
  protected void okPressed()
  {
    String warning = null;

    if (!getButton(Dialog.OK).getText().equals("Force Save") && process.getProject().isProduction())
    {
      warning = "This process is for project '" + getProcess().getProject().getSourceProjectName()
        + "',\nwhich is flagged as a production environment.\n\nPlease click 'Force Save' to confirm.\n\nThis action will be audit logged.";

      getButton(Dialog.OK).setText("Force Save");
    }

    if (warning != null)
    {
      WarningTray tray = getWarningTray();
      tray.setMessage(warning);
      tray.open();
      return;
    }

    setReturnCode(ProcessSaveDialog.FORCE_UPDATE);
    close();
  }

  private WarningTray warningTray;
  public WarningTray getWarningTray()
  {
    if (warningTray == null)
      warningTray = new WarningTray(this);
    return warningTray;
  }

  protected void dontSavePressed()
  {
    setReturnCode(ProcessSaveDialog.DONT_SAVE);
    close();
  }

  protected void createButtonsForButtonBar(Composite parent)
  {
    createButton(parent, IDialogConstants.OK_ID, respondingToClose ? "Save" : "OK", false);
    if (respondingToClose)
    {
      Button dontSaveButton = createButton(parent, IDialogConstants.CLOSE_ID, "Don't Save", true);
      dontSaveButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          dontSavePressed();
        }
      });
      dontSaveButton.forceFocus();
    }
    Button cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, !respondingToClose);
    if (!respondingToClose)
      cancelButton.forceFocus();
  }
}
