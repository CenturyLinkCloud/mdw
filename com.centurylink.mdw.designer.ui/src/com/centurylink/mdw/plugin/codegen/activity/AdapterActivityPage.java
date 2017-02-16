/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.activity;

import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.codegen.CodeGenWizardPage;
import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;
import com.centurylink.mdw.plugin.codegen.meta.AdapterActivity;

public class AdapterActivityPage extends CodeGenWizardPage {
    private Combo adapterTypeComboBox;

    private Label mdwWebServiceLabel;
    private Label webServiceSpacer;
    private Label httpMethodLabel;
    private Combo httpMethodComboBox;
    private Label busServiceTomIdLabel;
    private Text busServiceTomIdTextField;
    private Label synchronousJmsLabel;
    private Button synchronousJmsCheckbox;
    private Label explanatoryTextLabel;

    public AdapterActivityPage() {
        setTitle("Adapter Activity Settings");
        setDescription("Enter the information for your Adapter.\n"
                + "This will be used to generate the source code for your adapter implementation.");
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

        createInfoControls(composite, ncol, getCodeGenWizard().getInfoLabelLabel());
        createSepLine(composite, ncol);
        createAdapterTypeControls(composite, ncol);
        createBusServiceAdapterControls(composite, ncol);
        createJmsAdapterControls(composite, ncol);
        createWebServiceAdapterControls(composite, ncol);
        createRestfulServiceAdapterControls(composite, ncol);
        createExplanatoryLabelControls(composite, ncol);
        createHelpLinkControls(composite, ncol);

        showBusServiceAdapterControls(false);
        showJmsAdapterControls(false);
        showWebServiceAdapterControls(false);
        showRestfulServiceAdapterControls(false);
        setControl(composite);
    }

    private void createAdapterTypeControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Adapter Type:");
        adapterTypeComboBox = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 200;
        adapterTypeComboBox.setLayoutData(gd);

        adapterTypeComboBox.removeAll();
        for (String type : AdapterActivity.getAdapterTypeNames())
            adapterTypeComboBox.add(type);

        adapterTypeComboBox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String adapterType = adapterTypeComboBox.getText().trim();
                getAdapterActivity().setAdapterType(adapterType);

                showBusServiceAdapterControls(false);
                showJmsAdapterControls(false);
                showWebServiceAdapterControls(false);
                showRestfulServiceAdapterControls(false);

                if (adapterType.equals(AdapterActivity.ADAPTER_TYPE_BUS))
                    showBusServiceAdapterControls(true);
                else if (adapterType.equals(AdapterActivity.ADAPTER_TYPE_JMS))
                    showJmsAdapterControls(true);
                else if (adapterType.equals(AdapterActivity.ADAPTER_TYPE_WEB_SERVICE))
                    showWebServiceAdapterControls(true);
                else if (adapterType.equals(AdapterActivity.ADAPTER_TYPE_RESTFUL))
                    showRestfulServiceAdapterControls(true);

                handleFieldChanged();
            }
        });

        if (getAdapterActivity().getAdapterType() != null)
            adapterTypeComboBox.setText(getAdapterActivity().getAdapterType());
    }

    private void createWebServiceAdapterControls(Composite parent, int ncol) {
        GridData gd = new GridData(GridData.BEGINNING);
        mdwWebServiceLabel = new Label(parent, SWT.NONE);
        mdwWebServiceLabel.setLayoutData(gd);

        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        webServiceSpacer = new Label(parent, SWT.NONE);
        webServiceSpacer.setLayoutData(gd);
    }

    private void showWebServiceAdapterControls(boolean show) {
        // label
        mdwWebServiceLabel.setVisible(show);
        ((GridData) mdwWebServiceLabel.getLayoutData()).exclude = !show;
        webServiceSpacer.setVisible(show);
        ((GridData) webServiceSpacer.getLayoutData()).exclude = !show;

        explanatoryTextLabel.setVisible(show);
        if (show)
            explanatoryTextLabel.setText("For SOAP-based web services");
        mdwWebServiceLabel.getParent().layout();
    }

    private void createRestfulServiceAdapterControls(Composite parent, int ncol) {
        GridData gd = new GridData(GridData.BEGINNING);
        httpMethodLabel = new Label(parent, SWT.NONE);
        httpMethodLabel.setText("HTTP Method:");
        mdwWebServiceLabel.setLayoutData(gd);

        httpMethodComboBox = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 100;
        httpMethodComboBox.setLayoutData(gd);

        httpMethodComboBox.removeAll();
        httpMethodComboBox.add("GET");
        httpMethodComboBox.add("POST");
        httpMethodComboBox.add("PUT");
        httpMethodComboBox.add("DELETE");

        httpMethodComboBox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String httpMethod = httpMethodComboBox.getText();
                getAdapterActivity().setHttpMethod(httpMethod);
                handleFieldChanged();
            }
        });
    }

    private void showRestfulServiceAdapterControls(boolean show) {
        httpMethodLabel.setVisible(show);
        httpMethodComboBox.setVisible(show);
        explanatoryTextLabel.setVisible(show);
        if (show)
            explanatoryTextLabel
                    .setText("(Endpoint configuration can be specified in MDW Designer.)");
        adapterTypeComboBox.getParent().layout();
    }

    private void createHelpLinkControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("");
        new Label(parent, SWT.NONE).setText("");
        new Label(parent, SWT.NONE).setText("");
        Link link = new Link(parent, SWT.SINGLE);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = getMaxFieldWidth();
        gd.horizontalSpan = ncol - 2;
        link.setText(" <A>Adapter Activity Help</A>");
        link.setLayoutData(gd);
        link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String href = "/" + MdwPlugin.getPluginId() + "/help/doc/AdapterActivityBase.html";
                PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
            }
        });
    }

    private void createBusServiceAdapterControls(Composite parent, int ncol) {
        GridData gd = new GridData(GridData.BEGINNING);
        busServiceTomIdLabel = new Label(parent, SWT.NONE);
        busServiceTomIdLabel.setText("TOMID:");
        busServiceTomIdLabel.setLayoutData(gd);

        busServiceTomIdTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 215;
        gd.horizontalSpan = ncol - 1;
        busServiceTomIdTextField.setLayoutData(gd);
        busServiceTomIdTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getAdapterActivity().setBusServiceTomId(busServiceTomIdTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    private void showBusServiceAdapterControls(boolean show) {
        // label
        busServiceTomIdLabel.setVisible(show);
        ((GridData) busServiceTomIdLabel.getLayoutData()).exclude = !show;
        // text field
        busServiceTomIdTextField.setVisible(show);
        ((GridData) busServiceTomIdTextField.getLayoutData()).exclude = !show;

        explanatoryTextLabel.setVisible(show);
        if (show)
            explanatoryTextLabel.setText("(Use a temporary value if not yet available.)");
        busServiceTomIdLabel.getParent().layout();
    }

    private void createJmsAdapterControls(Composite parent, int ncol) {
        GridData gd = new GridData(GridData.BEGINNING);
        synchronousJmsLabel = new Label(parent, SWT.NONE);
        synchronousJmsLabel.setLayoutData(gd);

        synchronousJmsCheckbox = new Button(parent, SWT.CHECK);
        synchronousJmsCheckbox.setText("Synchronous");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        synchronousJmsCheckbox.setLayoutData(gd);
        synchronousJmsCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean synchronous = synchronousJmsCheckbox.getSelection();
                getAdapterActivity().setSynchronousJms(synchronous);
                handleFieldChanged();
            }
        });
    }

    private void showJmsAdapterControls(boolean show) {
        // label
        synchronousJmsLabel.setVisible(show);
        ((GridData) synchronousJmsLabel.getLayoutData()).exclude = !show;
        // checkbox
        synchronousJmsCheckbox.setVisible(show);
        ((GridData) synchronousJmsCheckbox.getLayoutData()).exclude = !show;

        explanatoryTextLabel.setVisible(show);
        if (show)
            explanatoryTextLabel
                    .setText("(JMS Queue configuration can be specified in MDW Designer.)");
        synchronousJmsLabel.getParent().layout();
    }

    private void createExplanatoryLabelControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("");

        explanatoryTextLabel = new Label(parent, SWT.WRAP);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 1;
        gd.widthHint = 375;
        explanatoryTextLabel.setLayoutData(gd);
    }

    @Override
    public boolean isPageComplete() {
        return getCodeGenWizard().getCodeGenType() == CodeGenType.registrationOnly || isPageValid();
    }

    @Override
    protected boolean isPageValid() {
        if (!checkString(getAdapterActivity().getAdapterType()))
            return false;
        if (getAdapterActivity().getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_BUS))
            return checkString(getAdapterActivity().getBusServiceTomId());
        if (getAdapterActivity().getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_RESTFUL))
            return checkString(getAdapterActivity().getHttpMethod());
        return true;
    }

    public IStatus[] getStatuses() {
        return null;
    }

    public AdapterActivity getAdapterActivity() {
        return (AdapterActivity) getCodeElement();
    }
}
