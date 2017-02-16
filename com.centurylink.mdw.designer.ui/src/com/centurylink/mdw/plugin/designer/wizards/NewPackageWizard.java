/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class NewPackageWizard extends Wizard implements INewWizard {
    public static final String WIZARD_ID = "mdw.designer.new.package";

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private NewPackagePage newPackagePage;

    private WorkflowPackage newPackage;

    public WorkflowPackage getPackage() {
        return newPackage;
    }

    private boolean json;

    public boolean isJson() {
        return json;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        setNeedsProgressMonitor(true);

        newPackagePage = new NewPackagePage();
        newPackage = new WorkflowPackage();
        if (selection != null && selection.getFirstElement() instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
            newPackage.setProject(workflowProject);
        }
        else if (selection != null && selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowProject workflowProject = ((WorkflowElement) selection.getFirstElement())
                    .getProject();
            newPackage.setProject(workflowProject);
        }
        else {
            WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                    .findWorkflowProject(selection);
            if (workflowProject != null)
                newPackage.setProject(workflowProject);
        }
    }

    @Override
    public void addPages() {
        addPage(newPackagePage);
    }

    @Override
    public boolean performFinish() {
        DesignerProxy designerProxy = newPackage.getProject().getDesignerProxy();
        designerProxy.createNewPackage(newPackage, isJson());
        newPackage.addElementChangeListener(newPackage.getProject());
        newPackage.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newPackage);
        DesignerPerspective.promptForShowPerspective(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(), newPackage);
        return true;
    }
}