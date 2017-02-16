/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.event;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.codegen.CodeGenWizardPage;

public class CamelHandlerPage extends EventHandlerPage {
    public CamelHandlerPage() {
        setTitle("MDW Camel Event Handler Settings");
        setDescription("Enter the information for your Camel Event Handler.\n"
                + "You'll reference this custom handler on the URL in your Camel route.");
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
        createWorkflowPackageControls(composite, ncol);
        createSepLine(composite, ncol);
        createCodeGenerationControls(composite, true, ncol - 1);
        if (getWizard() instanceof CamelProcessHandlerWizard)
            createDocumentControls(composite, ncol);
        createExternalEventHelpControls(composite, ncol, true);

        setControl(composite);
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getEventHandler().getPackage() == null)
            msg = "Please select a valid workflow package";

        if (msg == null)
            return null;
        else
            return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    @Override
    protected boolean isRegistrationSupported() {
        return false;
    }

    @Override
    public IWizardPage getNextPage() {
        if (getWizard() != null) {
            CodeGenWizardPage javaImplPage = selectJavaImplCodeGenPage();
            if (javaImplPage != null)
                return javaImplPage;
        }

        return super.getNextPage();
    }
}
