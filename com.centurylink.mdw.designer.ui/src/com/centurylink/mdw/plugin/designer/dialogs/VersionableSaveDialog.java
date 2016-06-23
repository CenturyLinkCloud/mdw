/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.Versionable;
import com.centurylink.mdw.plugin.designer.model.Versionable.Increment;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class VersionableSaveDialog extends TrayDialog
{
  public static final int CANCEL = 1;
  public static final int CLOSE_WITHOUT_SAVE = 2;

  private static final String GIT_REMOTE_NOT_UNLOCKED = "Project is not unlocked,\nso asset cannot be pushed to remote";

  private Versionable versionable;
  public Versionable getVersionable() { return versionable; }

  private Increment versionIncrement;
  public Increment getVersionIncrement() { return versionIncrement; }

  private String versionComment;
  public String getVersionComment() { return versionComment; }

  private Button overwriteButton;
  private Button newMinorButton;
  private Button newMajorButton;
  private CommentTray commentTray;
  private WarningTray warningTray;

  private Button rememberSelectionCheckbox;
  private Button keepLockedCheckbox;
  private Button pushToRemoteCheckbox;
  private Button enforceValidationCheckbox;

  private Button saveButton;

  private boolean isGitRemote;
  private boolean isGitRemoteUnlocked;

  private boolean hasCloseWithoutSaveOption;

  public VersionableSaveDialog(Shell shell, Versionable versionable)
  {
    this(shell, versionable, false);
  }

  public VersionableSaveDialog(Shell shell, Versionable versionable, boolean closeWithoutSaveOption)
  {
    super(shell);
    this.versionable = versionable;
    isGitRemote = versionable.getProject().isRemote() && versionable.getProject().isGitVcs();
    if (isGitRemote)
      isGitRemoteUnlocked = !versionable.getProject().getMdwVcsRepository().isGitProjectSync();
    this.hasCloseWithoutSaveOption = closeWithoutSaveOption;
  }

  @Override
  public void create()
  {
    super.create();

    if (isGitRemote && !isGitRemoteUnlocked)
    {
      warningTray = new WarningTray(this);
      warningTray.setMessage(GIT_REMOTE_NOT_UNLOCKED);
      openTray(warningTray);
    }
    if (versionIncrement != Increment.Overwrite && (!isGitRemote || isGitRemoteUnlocked))
    {
      commentTray = new CommentTray();
      openTray(commentTray);
      commentTray.getCommentText().addModifyListener(trayModifyListener);
    }

    initializeBounds();
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Save " + versionable.getTitle());

    Group radioGroup = new Group(composite, SWT.NONE);
    radioGroup.setText("Version to Save");
    GridLayout gl = new GridLayout();
    radioGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    radioGroup.setLayoutData(gd);
    overwriteButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    overwriteButton.setText("Overwrite current version: " + versionable.getVersionString());
    overwriteButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        versionButtonSelected(e);
      }
    });
    newMinorButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    newMinorButton.setText("Save as new minor version: " + versionable.formatVersion(versionable.getNextMinorVersion()));
    newMinorButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        versionButtonSelected(e);
      }
    });
    newMajorButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    newMajorButton.setText("Save as new major version: " + versionable.formatVersion(versionable.getNextMajorVersion()));
    newMajorButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        versionButtonSelected(e);
      }
    });

    boolean prod = versionable.getProject().isProduction();
    if (prod)
      overwriteButton.setEnabled(false);

    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();

    String prefsIncrement = prefsStore.getString(PreferenceConstants.PREFS_ASSET_SAVE_INCREMENT);
    boolean rememberSelection = !prefsIncrement.isEmpty();
    if (prefsIncrement.isEmpty())
      prefsIncrement = Increment.Minor.toString();
    versionIncrement = Increment.valueOf(prefsIncrement);
    if (prod && versionIncrement == Increment.Overwrite)
      versionIncrement = Increment.Minor;

    if (versionIncrement == Increment.Overwrite)
      overwriteButton.setSelection(true);
    else if (versionIncrement == Increment.Major)
      newMajorButton.setSelection(true);
    else
      newMinorButton.setSelection(true);

    rememberSelectionCheckbox = new Button(composite, SWT.CHECK);
    rememberSelectionCheckbox.setText("Remember this selection for future saves");
    rememberSelectionCheckbox.setSelection(rememberSelection);

    enforceValidationCheckbox = new Button(composite, SWT.CHECK);
    enforceValidationCheckbox.setText("Enforce asset validation rules");
    enforceValidationCheckbox.setSelection(prefsStore.getBoolean(PreferenceConstants.PREFS_ENFORCE_ASSET_VALIDATION_RULES));

    if (versionable.getProject().isFilePersist())
    {
      if (isGitRemote)
      {
        pushToRemoteCheckbox = new Button(composite, SWT.CHECK);
        pushToRemoteCheckbox.setText("Push to Remote");
        enablePushToRemote(!overwriteButton.getSelection() && isGitRemoteUnlocked);
        if (isGitRemoteUnlocked)
        {
          pushToRemoteCheckbox.addSelectionListener(new SelectionAdapter()
          {
            public void widgetSelected(SelectionEvent e)
            {
              if (pushToRemoteCheckbox.getSelection())
              {
                String comment = commentTray.getCommentText().getText();
                saveButton.setEnabled(comment.trim().length() > 0);
              }
              else
              {
                saveButton.setEnabled(true);
              }
            }
          });
          if (!overwriteButton.getSelection())
            pushToRemoteCheckbox.setSelection(prefsStore.getBoolean(PreferenceConstants.PREFS_PUSH_TO_GIT_REMOTE_WHEN_SAVING));
        }
      }
    }
    else
    {
      keepLockedCheckbox = new Button(composite, SWT.CHECK);
      keepLockedCheckbox.setText("Keep " + versionable.getTitle() + " locked after saving");
      keepLockedCheckbox.setSelection(prefsStore.getBoolean(PreferenceConstants.PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING));
    }

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
    if (overwriteButton.getSelection())
      versionIncrement = Increment.Overwrite;
    if (newMinorButton.getSelection())
      versionIncrement = Increment.Minor;
    else if (newMajorButton.getSelection())
      versionIncrement = Increment.Major;

    if (versionIncrement != Increment.Overwrite && commentTray != null && !StringHelper.isEmpty(commentTray.getComment()))
      versionComment = commentTray.getComment();

    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
    if (rememberSelectionCheckbox.getSelection())
      prefsStore.setValue(PreferenceConstants.PREFS_ASSET_SAVE_INCREMENT, versionIncrement.toString());
    else
      prefsStore.setValue(PreferenceConstants.PREFS_ASSET_SAVE_INCREMENT, "");
    prefsStore.setValue(PreferenceConstants.PREFS_ENFORCE_ASSET_VALIDATION_RULES, enforceValidationCheckbox.getSelection());


    if (versionable.getProject().isFilePersist())
    {
      if (isGitRemote)
        prefsStore.setValue(PreferenceConstants.PREFS_PUSH_TO_GIT_REMOTE_WHEN_SAVING, pushToRemoteCheckbox.getSelection());
    }
    else
    {
      prefsStore.setValue(PreferenceConstants.PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING, keepLockedCheckbox.getSelection());
    }

    close();
  }

  protected void buttonPressed(int buttonId)
  {
    if (buttonId == IDialogConstants.NO_ID)
    {
      setReturnCode(CLOSE_WITHOUT_SAVE);
      close();
    }
    else
    {
      super.buttonPressed(buttonId);
    }
  }

  protected void createButtonsForButtonBar(Composite parent)
  {
    saveButton = createButton(parent, IDialogConstants.OK_ID, "Save", true);
    if (versionable.getProject().isProduction())
      saveButton.setEnabled(false);

    if (hasCloseWithoutSaveOption)
      createButton(parent, IDialogConstants.NO_ID, "Don't Save", false);

    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }


  private void versionButtonSelected(SelectionEvent e)
  {
    boolean overwrite = overwriteButton.getSelection();
    if (isGitRemote)
    {
      enablePushToRemote(!overwrite && isGitRemoteUnlocked);
      if (!isGitRemoteUnlocked)
        return;
    }

    if (!overwrite)
    {
      if (getTray() == null)
      {
        commentTray = new CommentTray();
        openTray(commentTray);
        commentTray.getCommentText().addModifyListener(trayModifyListener);
      }
    }
    else
    {
      if (getTray() != null)
        closeTray();
    }
  }

  private void enablePushToRemote(boolean enabled)
  {
    if (pushToRemoteCheckbox != null)
    {
      if (!enabled)
        pushToRemoteCheckbox.setSelection(false);
      pushToRemoteCheckbox.setEnabled(enabled);
    }
  }

  private ModifyListener trayModifyListener = new ModifyListener()
  {
    public void modifyText(ModifyEvent e)
    {
      String comment = commentTray.getCommentText().getText();
      boolean hasComment = comment.trim().length() > 0;
      if (versionable.getProject().isProduction() || (pushToRemoteCheckbox != null && pushToRemoteCheckbox.getSelection()))
        saveButton.setEnabled(hasComment);
    }
  };
}
