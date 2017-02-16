/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.designer.model.WebResource;
import com.centurylink.mdw.plugin.designer.model.WebResource.ImageFormat;
import com.centurylink.mdw.plugin.designer.model.WebResource.ResourceType;

public class NewWebResourcePage extends WorkflowAssetPage {
    private Button textBasedRadio;
    private Combo textBasedTypeCombo;
    private Button binaryRadio;
    private Combo binaryTypeCombo;
    private Text binaryFileText;
    private Button binaryBrowseButton;

    private String resourceFilePath;

    public String getResourceFilePath() {
        return resourceFilePath;
    }

    public NewWebResourcePage(WebResource webResource) {
        super(webResource);
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

        createWorkflowProjectControls(composite, ncol);
        if (getProject().checkRequiredVersion(5, 0))
            createWorkflowPackageControls(composite, ncol);
        else
            setPackage(getProject().getDefaultPackage());
        createNameControls(composite, ncol);
        createSpacer(composite, ncol);
        createSepLine(composite, ncol);
        createSpacer(composite, ncol);
        createTextBasedControls(composite, ncol);
        createSpacer(composite, ncol);
        createBinaryControls(composite, ncol);

        setControl(composite);
        enableBinaryControls(false);
        getNameTextField().forceFocus();
    }

    private void createTextBasedControls(Composite parent, int ncol) {
        textBasedRadio = new Button(parent, SWT.RADIO | SWT.LEFT);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        textBasedRadio.setLayoutData(gd);
        textBasedRadio.setSelection(true);
        textBasedRadio.setText("Text-Based Resources");
        textBasedRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = textBasedRadio.getSelection();
                binaryRadio.setSelection(!selected);
                enableBinaryControls(!selected);
                enableTextBasedControls(selected);
                handleFieldChanged();
            }
        });

        new Label(parent, SWT.NONE).setText("Resource Type:");
        textBasedTypeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 200;
        textBasedTypeCombo.setLayoutData(gd);

        textBasedTypeCombo.removeAll();
        textBasedTypeCombo.add("");
        for (String language : WebResource.getTextBasedTypes()) {
            textBasedTypeCombo.add(language);
        }

        textBasedTypeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ResourceType resourceType = ResourceType
                        .valueOf(textBasedTypeCombo.getText().trim());
                WebResource webResource = new WebResource(resourceType);
                webResource.setName(getNameTextField().getText().trim());
                webResource.setPackage(getWebResource().getPackage());
                setWebResource(webResource);
                handleFieldChanged();
            }
        });
        textBasedTypeCombo.select(0);
    }

    private void createBinaryControls(Composite parent, int ncol) {
        binaryRadio = new Button(parent, SWT.RADIO | SWT.LEFT);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        binaryRadio.setLayoutData(gd);
        binaryRadio.setSelection(false);
        binaryRadio.setText("Binary Resources");
        binaryRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = binaryRadio.getSelection();
                textBasedRadio.setSelection(!selected);
                enableTextBasedControls(!selected);
                enableBinaryControls(selected);
                handleFieldChanged();
            }
        });

        new Label(parent, SWT.NONE).setText("Resource Type:");
        binaryTypeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 200;
        binaryTypeCombo.setLayoutData(gd);

        binaryTypeCombo.removeAll();
        binaryTypeCombo.add("");
        for (String language : WebResource.getBinaryTypes()) {
            binaryTypeCombo.add(language);
        }

        binaryTypeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                WebResource webResource = new WebResource(ResourceType.Image);
                String type = binaryTypeCombo.getText().trim().toUpperCase();
                if (type.length() == 0)
                    webResource.setImageFormat(null);
                else
                    webResource.setImageFormat(ImageFormat.valueOf(type));

                webResource.setName(getNameTextField().getText().trim());
                webResource.setPackage(getWebResource().getPackage());
                setWebResource(webResource);
                handleFieldChanged();
            }
        });
        binaryTypeCombo.select(0);

        Label label = new Label(parent, SWT.NONE);
        label.setText("Resource File:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        binaryFileText = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 300;
        gd.horizontalSpan = ncol - 2;
        binaryFileText.setLayoutData(gd);
        binaryFileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                resourceFilePath = binaryFileText.getText().trim();
                handleFieldChanged();
            }
        });

        binaryBrowseButton = new Button(parent, SWT.PUSH);
        binaryBrowseButton.setText("Browse...");
        binaryBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                String path = dlg.open();
                if (path != null)
                    binaryFileText.setText(path);
            }
        });
    }

    private void enableTextBasedControls(boolean enabled) {
        if (!enabled)
            textBasedTypeCombo.select(0);
        textBasedTypeCombo.setEnabled(enabled);
    }

    private void enableBinaryControls(boolean enabled) {
        if (!enabled) {
            binaryTypeCombo.select(0);
            binaryFileText.setText("");
        }
        binaryTypeCombo.setEnabled(enabled);
        binaryFileText.setEnabled(enabled);
        binaryBrowseButton.setEnabled(enabled);
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        return getProject() != null && getWebResource() != null
                && checkString(getWebResource().getName())
                && (textBasedRadio.getSelection() || checkFile(resourceFilePath))
                && ((textBasedRadio.getSelection()
                        && checkString(textBasedTypeCombo.getText().trim())
                        || (binaryRadio.getSelection()
                                && checkString(binaryTypeCombo.getText().trim()))))
                && getWorkflowAsset().isUserAuthorized(UserRoleVO.ASSET_DESIGN)
                && !getWorkflowAsset().getPackage()
                        .workflowAssetNameExists(getWorkflowAsset().getName());
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() == null)
            msg = "Please select a valid workflow project";
        else if (!checkString(getWebResource().getName()))
            msg = "Please enter a resource name";
        else if (binaryRadio.getSelection() && !checkFile(resourceFilePath))
            msg = "Please enter a valid file path for Resource File";
        else if (!getWorkflowAsset().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
            msg = "You're not authorized to add resource to this workflow package.";
        else if (getWorkflowAsset().getPackage()
                .workflowAssetNameExists(getWorkflowAsset().getName()))
            msg = "Name already exists";

        if (msg == null)
            return null;

        IStatus[] is = { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
        return is;
    }

    public WebResource getWebResource() {
        return (WebResource) getWorkflowAsset();
    }

    public void setWebResource(WebResource webResource) {
        setWorkflowAsset(webResource);
    }
}