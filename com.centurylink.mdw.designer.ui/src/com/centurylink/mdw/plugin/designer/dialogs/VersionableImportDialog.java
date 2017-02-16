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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.Versionable;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class VersionableImportDialog extends TrayDialog {
    public static final int CANCEL = 1;

    private Versionable versionable;

    public Versionable getVersionable() {
        return versionable;
    }

    private String versionComment;

    public String getVersionComment() {
        return versionComment;
    }

    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    private Text fileText;
    private Button fileBrowseButton;
    private Text commentText;
    private Button keepLockedCheckbox;
    private Button importButton;

    public VersionableImportDialog(Shell shell, Versionable versionable) {
        super(shell);
        this.versionable = versionable;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell()
                .setText("Import " + versionable.getTitle() + ": " + versionable.getName());

        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        composite.setLayout(gl);

        // file text
        Label lbl = new Label(composite, SWT.NONE);
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        lbl.setLayoutData(gd);
        lbl.setText("File");
        fileText = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 300;
        fileText.setLayoutData(gd);
        fileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                filePath = fileText.getText().trim();
                if (versionable.getProject().isProduction())
                    importButton.setEnabled(
                            filePath.length() > 0 && commentText.getText().trim().length() > 0);
                else
                    importButton.setEnabled(filePath.length() > 0);
            }
        });

        // browse button
        fileBrowseButton = new Button(composite, SWT.PUSH);
        fileBrowseButton.setText("Browse...");
        fileBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                dlg.setFileName(filePath);
                if (versionable.getExtension() != null)
                    dlg.setFilterExtensions(new String[] { "*" + versionable.getExtension() });
                String result = dlg.open();
                if (result != null) {
                    filePath = result;
                    fileText.setText(filePath);
                }
            }
        });

        // comments text
        lbl = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.verticalIndent = 5;
        lbl.setLayoutData(gd);
        lbl.setText("Comments");
        commentText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 365;
        gd.heightHint = 75;
        gd.horizontalSpan = 2;
        commentText.setLayoutData(gd);
        commentText.setTextLimit(1000);
        commentText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                versionComment = commentText.getText().trim();
                if (versionable.getProject().isProduction())
                    importButton.setEnabled(
                            versionComment.length() > 0 && !fileText.getText().isEmpty());
            }
        });

        // keep locked checkbox
        keepLockedCheckbox = new Button(composite, SWT.CHECK);
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.verticalIndent = 5;
        keepLockedCheckbox.setLayoutData(gd);
        keepLockedCheckbox.setText("Keep " + versionable.getTitle() + " locked after saving");
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        keepLockedCheckbox.setSelection(
                prefsStore.getBoolean(PreferenceConstants.PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING));

        return composite;
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        prefsStore.setValue(PreferenceConstants.PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING,
                keepLockedCheckbox.getSelection());

        close();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        importButton = createButton(parent, IDialogConstants.OK_ID, "Import", true);
        importButton.setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
}
