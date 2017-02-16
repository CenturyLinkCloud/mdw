/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.codegen.meta.Code;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.JavaSource;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class CodeGenWizard extends Wizard implements INewWizard {
    public enum CodeGenType {
        staticJavaCode, dynamicJavaCode, registrationOnly
    }

    DynamicJavaPage dynamicJavaPage;
    StaticJavaPage staticJavaPage;
    RegistrationOnlyPage registrationOnlyPage;

    private CodeGenType codeGenType;

    public CodeGenType getCodeGenType() {
        return codeGenType;
    }

    public void setCodeGenType(CodeGenType codeGenType) {
        this.codeGenType = codeGenType;
    }

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private IStructuredSelection selection;

    public IStructuredSelection getSelection() {
        return selection;
    }

    private MdwSettings mdwSettings = MdwPlugin.getSettings();

    public MdwSettings getMdwSettings() {
        return mdwSettings;
    }

    private Object model;

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    private Code codeElement;

    public Code getCodeElement() {
        return codeElement;
    }

    public void setCodeElement(Code codeElement) {
        this.codeElement = codeElement;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection, Code codeElement) {
        this.workbench = workbench;
        this.selection = selection;
        this.codeElement = codeElement;
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        setNeedsProgressMonitor(true);

        if (selection != null) {
            if (selection.getFirstElement() instanceof WorkflowPackage) {
                WorkflowPackage workflowPackage = (WorkflowPackage) selection.getFirstElement();
                getCodeElement().setProject(workflowPackage.getProject());
                getCodeElement().setPackage(workflowPackage);
            }
            else if (selection.getFirstElement() instanceof WorkflowElement) {
                WorkflowElement workflowElement = ((WorkflowElement) selection.getFirstElement());
                getCodeElement().setProject(workflowElement.getProject());
                getCodeElement().setPackage(workflowElement.getPackage());
            }
            else if (selection.getFirstElement() instanceof IResource) {
                IResource resource = (IResource) selection.getFirstElement();
                if (resource.getProject() != null) {
                    WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                            .getWorkflowProject(resource.getProject());
                    if (workflowProject != null)
                        getCodeElement().setProject(workflowProject);
                }
            }
        }

        if (getCodeElement().getProject() == null) {
            // choose any available workflow project
            List<WorkflowProject> workflowProjects = WorkflowProjectManager.getInstance()
                    .getWorkflowProjects();
            if (workflowProjects == null || workflowProjects.size() == 0)
                MessageDialog.openError(getShell(), "Error", "No MDW projects found");
            else
                getCodeElement().setProject(workflowProjects.get(0));
        }
    }

    /**
     * adds a page and initializes it from the
     * 
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
     */
    public WizardPage addPage(WizardPage page) {
        super.addPage(page);
        page.init(selection);
        return page;
    }

    public void addJavaImplCodeGenPages() {
        staticJavaPage = new StaticJavaPage("Custom Activity",
                "Enter the type info for your Activity.\n"
                        + "This will be used to generate the activity implementor source code.");
        addPage(staticJavaPage);
        dynamicJavaPage = new DynamicJavaPage("Dynamic Java Activity",
                "Enter the type info for your custom Activity.\n"
                        + "This will be used to generate the Dynamic Java source code.");
        addPage(dynamicJavaPage);
        registrationOnlyPage = new RegistrationOnlyPage("Register Activity",
                "Enter the class name for the Activity.\n"
                        + "This will be used to register the activity in the MDW database.");
        addPage(registrationOnlyPage);
    }

    @Override
    public void addPages() {
    }

    public abstract void generate(IProgressMonitor monitor)
            throws InterruptedException, CoreException;

    @SuppressWarnings("restriction")
    @Override
    public boolean performFinish() {
        IWorkspaceRunnable op = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor)
                    throws CoreException, OperationCanceledException {
                try {
                    generate(monitor);
                    monitor.done();
                }
                catch (InterruptedException e) {
                    throw new OperationCanceledException(e.getMessage());
                }
            }
        };

        try {
            getContainer().run(false, true,
                    new org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter(op));
        }
        catch (InvocationTargetException e) {
            return handleGenerationException(getShell(), e);
        }
        catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    /**
     * @return true if registration should continue
     */
    protected boolean generateCode(String jetFile, IProgressMonitor monitor) throws CoreException {
        String targetFolder;
        if (getCodeGenType() == CodeGenType.staticJavaCode) {
            targetFolder = getCodeElement().getPackageFragmentRoot().getPath().toString();
            String targetFile = getCodeElement().getClassName() + ".java";
            IPackageFragment packageFragment = getCodeElement().getPackageFragment();
            JetAccess jet = getJet(jetFile, targetFolder, targetFile, packageFragment);

            Generator generator = new Generator(getShell());
            generator.createFileAndOpen(getShell(), jet, monitor);
        }
        else if (getCodeGenType() == CodeGenType.dynamicJavaCode) {
            String pkgPath = getCodeElement().getPackage().getName().replace('.', '/');
            if (getCodeElement().getProject().isFilePersist())
                targetFolder = getCodeElement().getProject().getAssetFolder().getFullPath() + "/"
                        + pkgPath;
            else
                targetFolder = getCodeElement().getProject().getTempFolder().getFullPath() + "/"
                        + pkgPath;
            String targetFile = getCodeElement().getClassName() + ".java";
            getCodeElement().getProject().setJava();

            JavaSource javaAsset = createJavaAssetAndOpen(getCodeElement().getPackage(), jetFile,
                    targetFolder, targetFile, monitor);
            return javaAsset != null;
        }
        return true;
    }

    protected boolean handleGenerationException(Shell shell, InvocationTargetException e) {
        boolean proceed = false;
        String errorMessage = "Error Generating MDW Code (Troubleshooting tips under Help > Cheat Sheets > MDW Workflow > MDW Updates > Upgrading the MDW Plug-In).";
        String exceptionMessage = "Error when generating the implementation class(es).";
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            exceptionMessage = e.getCause().getMessage();
        }
        else if (e.getMessage() != null) {
            exceptionMessage = e.getMessage();
        }

        IStatus status = new Status(IStatus.ERROR, MdwPlugin.getPluginId(), IStatus.ERROR,
                exceptionMessage, e);
        PluginMessages.log(status);

        ErrorDialog.openError(shell, "Error", errorMessage, status);
        return proceed;
    }

    public JetAccess getJet(String jetFile, String targetFolder, String targetFile,
            IPackageFragment pkg) {
        // prepare config for creating files
        JetConfig jetConfig = new JetConfig();
        jetConfig.setModel(model);
        jetConfig.setSettings(mdwSettings);
        jetConfig.setPluginId(MdwPlugin.getPluginId());

        if (pkg == null)
            jetConfig.setPackageName("");
        else
            jetConfig.setPackageName(pkg.getElementName());
        jetConfig.setTargetFolder(targetFolder);
        jetConfig.setTargetFile(targetFile);
        jetConfig.setTemplateRelativeUri("templates/" + jetFile);

        return new JetAccess(jetConfig);
    }

    protected JetAccess getJet(String jetFile, String targetFolder, String targetPath) {
        JetConfig jetConfig = new JetConfig();
        jetConfig.setModel(model);
        jetConfig.setSettings(mdwSettings);
        jetConfig.setPluginId(MdwPlugin.getPluginId());
        jetConfig.setTargetFolder(targetFolder);
        jetConfig.setTargetFile(targetPath);
        jetConfig.setTemplateRelativeUri("templates/" + jetFile);
        return new JetAccess(jetConfig);
    }

    protected JavaSource createJavaAssetAndOpen(WorkflowPackage workflowPackage, String jetFile,
            String targetFolder, String targetFile, IProgressMonitor monitor) throws CoreException {
        JetAccess jet = getJet(jetFile, targetFolder, targetFile);
        Generator generator = new Generator(getShell());
        IFile contentFile = generator.createFile(jet, monitor);

        WorkflowProject workflowProject = workflowPackage.getProject();
        JavaSource javaAsset = new JavaSource();
        javaAsset.setLanguage(RuleSetVO.JAVA);
        javaAsset.setName(contentFile.getName());
        javaAsset.setProject(workflowProject);
        javaAsset.setPackage(workflowPackage);
        String content = new String(PluginUtil.readFile(contentFile));
        javaAsset.setContent(content);
        contentFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
        contentFile.delete(true, monitor);

        DesignerProxy designerProxy = javaAsset.getProject().getDesignerProxy();
        try {
            designerProxy.createNewWorkflowAsset(javaAsset, true);
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "New " + javaAsset.getTitle(),
                    javaAsset.getProject());
            return null;
        }

        if (designerProxy.getRunnerStatus().equals(RunnerStatus.SUCCESS)) {
            javaAsset.openFile(new NullProgressMonitor());

            javaAsset.addElementChangeListener(javaAsset.getProject());
            javaAsset.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, javaAsset);

            WorkflowAssetFactory.registerAsset(javaAsset);

            return javaAsset;
        }
        else {
            return null;
        }
    }

    /**
     * Perform custom validations in shared wizard pages.
     * 
     * @return null if valid, otherwise an error message
     */
    protected String validate() {
        return null;
    }

    /**
     * default is null
     */
    public String getInfoLabelValue() {
        return null;
    }

    /**
     * default is null
     */
    public String getInfoLabelLabel() {
        return null;
    }

    public WizardPage getPageAfterJavaImplCodeGenPage() {
        return null;
    }
}
