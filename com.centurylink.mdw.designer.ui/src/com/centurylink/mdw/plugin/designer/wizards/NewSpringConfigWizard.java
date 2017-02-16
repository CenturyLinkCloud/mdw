/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.SpringConfig;

public class NewSpringConfigWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.spring.config";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new SpringConfig());
    }

    @Override
    public String getTemplateLocation() {
        return "/templates/spring";
    }
}