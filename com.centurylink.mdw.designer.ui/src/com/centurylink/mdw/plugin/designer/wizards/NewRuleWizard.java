/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.Rule;

public class NewRuleWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.rule";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new Rule());
    }

    @Override
    public DocumentTemplate getNewDocTemplate() {
        if (((Rule) getWorkflowAsset()).isExcel())
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/excel");
        else if (((Rule) getWorkflowAsset()).isExcel2007())
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/excel");
        else
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/drools");
    }
}