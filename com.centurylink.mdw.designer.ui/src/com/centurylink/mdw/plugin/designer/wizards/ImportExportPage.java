/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.dialogs.FileSaveDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Wizard selection should be a workflow project (import only), a (non-default)
 * workflow package, or an asset within a (non-default) package (export only).
 */
public abstract class ImportExportPage extends WizardPage {
    boolean isExport;

    private Label projectLabel;
    private Label packageLabel;
    private Label processLabel;
    private Label assetLabel;
    private Text filePathText;
    private Button browseFileButton;
    private Text commentsText;
    private Button lockCheckbox;

    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    private String comments;

    public String getComments() {
        return comments;
    }

    private boolean locked;

    public boolean isLocked() {
        return locked;
    }

    /**
     * Must override for export
     */
    protected String getDefaultFileName() {
        return null;
    };

    protected String getFileExtension() {
        return ".xml";
    };

    public ImportExportPage(String title, String description) {
        setTitle(title);
        setDescription(description);
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createControls(composite, ncol);
        setControl(composite);

        filePathText.forceFocus();
    }

    protected void createControls(Composite composite, int ncol) {
        createProjectControls(composite, ncol);
        if (getPackage() != null)
            createPackageControls(composite, ncol);
        if (getProcess() != null)
            createProcessControls(composite, ncol);
        if (getAsset() != null)
            createAssetControls(composite, ncol);
        createFileControls(composite, ncol);
    }

    protected void createProjectControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Project:");
        projectLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        projectLabel.setLayoutData(gd);
        FontData font = projectLabel.getFont().getFontData()[0];
        font.setStyle(font.getStyle() | SWT.BOLD);
        projectLabel.setFont(new Font(this.getShell().getDisplay(), font));
        projectLabel.setText(getProject().getLabel());
    }

    protected void createPackageControls(Composite parent, int ncol) {
        List<WorkflowPackage> packages = getPackages();
        String label = packages.get(0).getLabel();
        if (packages.size() > 1)
            label += ", " + packages.get(1).getLabel() + "...";

        new Label(parent, SWT.NONE).setText(packages.size() == 1 ? "Package:" : "Packages:");
        packageLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        packageLabel.setLayoutData(gd);
        FontData font = packageLabel.getFont().getFontData()[0];
        font.setStyle(font.getStyle() | SWT.BOLD);
        packageLabel.setFont(new Font(this.getShell().getDisplay(), font));
        packageLabel.setText(label);
    }

    protected void createProcessControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Process:");
        processLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        processLabel.setLayoutData(gd);
        FontData font = processLabel.getFont().getFontData()[0];
        font.setStyle(font.getStyle() | SWT.BOLD);
        processLabel.setFont(new Font(this.getShell().getDisplay(), font));
        processLabel.setText(getProcess().getLabel());
    }

    protected void createAssetControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Asset:");
        assetLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        assetLabel.setLayoutData(gd);
        FontData font = assetLabel.getFont().getFontData()[0];
        font.setStyle(font.getStyle() | SWT.BOLD);
        assetLabel.setFont(new Font(this.getShell().getDisplay(), font));
        assetLabel.setText(getAsset().getLabel());
    }

    protected void createFileControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText(isExport ? "Export File:" : "Import File:");
        filePathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        gd.horizontalSpan = ncol - 2;
        filePathText.setLayoutData(gd);
        filePathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                filePath = filePathText.getText().trim();
                handleFieldChanged();
            }
        });

        browseFileButton = new Button(parent, SWT.PUSH);
        browseFileButton.setText("Browse...");
        browseFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (isExport) {
                    FileSaveDialog dlg = new FileSaveDialog(getShell());
                    dlg.setFilterExtensions(new String[] { "*" + getFileExtension() });
                    dlg.setFileName(getDefaultFileName());
                    String path = dlg.open();
                    if (path != null)
                        filePathText.setText(path);
                }
                else {
                    FileDialog dlg = new FileDialog(getShell());
                    dlg.setFilterExtensions(new String[] { "*" + getFileExtension() });
                    String path = dlg.open();
                    if (path != null)
                        filePathText.setText(path);
                }
            }
        });
    }

    protected void createCommentsControls(Composite parent, int ncol) {
        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText("Comments:");
        GridData gd = new GridData(SWT.LEFT);
        gd.verticalAlignment = SWT.TOP;
        lbl.setLayoutData(gd);
        commentsText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 350;
        gd.heightHint = 100;
        gd.horizontalSpan = ncol - 1;
        commentsText.setLayoutData(gd);
        commentsText.setTextLimit(1000);
        commentsText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                comments = commentsText.getText().trim();
                handleFieldChanged();
            }
        });
    }

    protected void createLockControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE);
        lockCheckbox = new Button(parent, SWT.CHECK);
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = ncol - 1;
        gd.verticalIndent = 5;
        lockCheckbox.setLayoutData(gd);
        lockCheckbox.setText("Lock new version to " + getProject().getUser().getUsername());
        lockCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                locked = lockCheckbox.getSelection();
            }
        });
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        return getProject() != null && getFilePath() != null && getFilePath().length() > 0;
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() == null)
            msg = "Please select a valid workflow project";
        else if (getFilePath() == null || getFilePath().trim().length() == 0)
            msg = "Please enter a file path";
        if (msg == null)
            return null;
        return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    public WorkflowProject getProject() {
        return ((ImportExportWizard) getWizard()).getProject();
    }

    protected WorkflowPackage getPackage() {
        return ((ImportExportWizard) getWizard()).getPackage();
    }

    protected WorkflowProcess getProcess() {
        return ((ImportExportWizard) getWizard()).getProcess();
    }

    protected WorkflowAsset getAsset() {
        return ((ImportExportWizard) getWizard()).getAsset();
    }

    protected List<WorkflowPackage> getPackages() {
        return ((ImportExportWizard) getWizard()).getPackages();
    }

}