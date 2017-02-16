/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class NewProcessWizard extends Wizard implements INewWizard {
    public static final String WIZARD_ID = "mdw.designer.new.process";

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private NewProcessPage newProcessPage;

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        setNeedsProgressMonitor(true);

        newProcessPage = new NewProcessPage();
        process = new WorkflowProcess();
        if (selection != null && selection.getFirstElement() instanceof WorkflowPackage) {
            WorkflowPackage processPackage = (WorkflowPackage) selection.getFirstElement();
            process.setProject(processPackage.getProject());
            process.setPackage(processPackage);
        }
        else if (selection != null && selection.getFirstElement() instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
            process.setProject(workflowProject);
            if (workflowProject.isShowDefaultPackage())
                process.setPackage(workflowProject.getDefaultPackage());
        }
        else if (selection != null && selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowProject workflowProject = ((WorkflowElement) selection.getFirstElement())
                    .getProject();
            process.setProject(workflowProject);
        }
        else {
            WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                    .findWorkflowProject(selection);
            if (workflowProject != null)
                process.setProject(workflowProject);
        }

    }

    @Override
    public void addPages() {
        addPage(newProcessPage);
    }

    @Override
    public boolean performFinish() {
        DesignerProxy designerProxy = process.getProject().getDesignerProxy();
        designerProxy.createNewProcess(process);
        if (designerProxy.getRunnerStatus().equals(RunnerStatus.SUCCESS)) {
            process.sync();

            process.addElementChangeListener(process.getProject());
            process.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, process);

            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage();
            try {
                page.openEditor(process, "mdw.editors.process");
                DesignerPerspective.promptForShowPerspective(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow(), process);
                return true;
            }
            catch (Exception ex) {
                PluginMessages.uiError(getShell(), ex, "Open Process", process.getProject());
                return false;
            }
        }
        else {
            return false;
        }
    }
}