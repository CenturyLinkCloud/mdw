/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.XmlDoc;

public class NewXmlDocWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.xml";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new XmlDoc());
    }
}