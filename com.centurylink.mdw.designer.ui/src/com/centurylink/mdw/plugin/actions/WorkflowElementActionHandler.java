/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.designer.pages.FormPanel;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.ProcessConsoleRunner;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.ant.TestResultsFormatter;
import com.centurylink.mdw.plugin.codegen.activity.ActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.AdapterActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.EvaluatorActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.StartActivityWizard;
import com.centurylink.mdw.plugin.codegen.event.CamelNotifyHandlerWizard;
import com.centurylink.mdw.plugin.codegen.event.CamelProcessHandlerWizard;
import com.centurylink.mdw.plugin.codegen.event.EventHandlerWizard;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.dialogs.ActivityImplDeleteDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ExportAsDialog;
import com.centurylink.mdw.plugin.designer.dialogs.MdwListInputDialog;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.dialogs.PackageDeleteDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ProcessDeleteDialog;
import com.centurylink.mdw.plugin.designer.dialogs.RenameDialog;
import com.centurylink.mdw.plugin.designer.dialogs.SetVersionDialog;
import com.centurylink.mdw.plugin.designer.dialogs.TemplateRunDialog;
import com.centurylink.mdw.plugin.designer.dialogs.WorkflowElementDeleteDialog;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.Activity.AdapterActivity;
import com.centurylink.mdw.plugin.designer.model.Activity.EvaluatorActivity;
import com.centurylink.mdw.plugin.designer.model.Activity.StartActivity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.CamelRoute;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.JarFile;
import com.centurylink.mdw.plugin.designer.model.JavaSource;
import com.centurylink.mdw.plugin.designer.model.Json;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.Page;
import com.centurylink.mdw.plugin.designer.model.Report;
import com.centurylink.mdw.plugin.designer.model.Rule;
import com.centurylink.mdw.plugin.designer.model.Script;
import com.centurylink.mdw.plugin.designer.model.SpringConfig;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.Template;
import com.centurylink.mdw.plugin.designer.model.TextResource;
import com.centurylink.mdw.plugin.designer.model.WebResource;
import com.centurylink.mdw.plugin.designer.model.WordDoc;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset.AssetWorkbenchListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.XmlDoc;
import com.centurylink.mdw.plugin.designer.views.MyTasksView;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerDropTarget;
import com.centurylink.mdw.plugin.designer.views.ProcessHierarchyView;
import com.centurylink.mdw.plugin.designer.views.ProcessInstanceListView;
import com.centurylink.mdw.plugin.designer.views.ProcessLaunchView;
import com.centurylink.mdw.plugin.designer.wizards.ExportAssetWizard;
import com.centurylink.mdw.plugin.designer.wizards.ExportAttributesWizard;
import com.centurylink.mdw.plugin.designer.wizards.ExportPackageWizard;
import com.centurylink.mdw.plugin.designer.wizards.ExportProcessWizard;
import com.centurylink.mdw.plugin.designer.wizards.ExportProjectWizard;
import com.centurylink.mdw.plugin.designer.wizards.ExportTaskTemplatesWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportAssetWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportAttributesWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportPackageWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportProcessWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportProjectWizard;
import com.centurylink.mdw.plugin.designer.wizards.ImportTaskTemplatesWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewCamelRouteWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJarFileWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJavaWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJsonWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewPackageWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewPageWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewProcessWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewReportWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewRuleWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewScriptWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewSpringConfigWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTaskTemplateWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTemplateWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTestCaseWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTextResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWebResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWordDocWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewXmlDocWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewYamlWizard;
import com.centurylink.mdw.plugin.launch.ActivityLaunchShortcut;
import com.centurylink.mdw.plugin.launch.AutomatedTestLaunchShortcut;
import com.centurylink.mdw.plugin.launch.ExternalEventLaunchShortcut;
import com.centurylink.mdw.plugin.launch.ProcessLaunchShortcut;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.LocalCloudProjectWizard;
import com.centurylink.mdw.plugin.project.RemoteWorkflowProjectWizard;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.search.ProcessSearchQuery;
import com.centurylink.mdw.plugin.search.SearchQuery;
import com.centurylink.mdw.plugin.search.SearchResultsPage;
import com.centurylink.mdw.plugin.server.ServerConfigurator;
import com.centurylink.mdw.plugin.server.ServerRunner;

/**
 * Handles the actions that can be performed on various workflow model objects.
 * All access should be in the context of the SWT UI thread.
 */
public class WorkflowElementActionHandler {
    public static final String REFRESH_CACHES = "refreshCaches";
    public static final String STUB_SERVER = "stubServer";
    public static final String LOG_WATCHER = "logWatcher";

    public void create(Class<? extends WorkflowElement> elementClass, WorkflowElement element) {
        if (elementClass.equals(WorkflowElement.class))
            launchWizard(new LocalCloudProjectWizard(), element);
        else if (elementClass.equals(WorkflowProject.class))
            launchWizard(new RemoteWorkflowProjectWizard(), element);
        else if (elementClass.equals(WorkflowPackage.class))
            launchWizard(new NewPackageWizard(), element);
        else if (elementClass.equals(WorkflowProcess.class))
            launchWizard(new NewProcessWizard(), element);
        else if (elementClass.equals(StartActivity.class))
            launchWizard(new StartActivityWizard(), element);
        else if (elementClass.equals(AdapterActivity.class))
            launchWizard(new AdapterActivityWizard(), element);
        else if (elementClass.equals(EvaluatorActivity.class))
            launchWizard(new EvaluatorActivityWizard(), element);
        else if (elementClass.equals(Activity.class))
            launchWizard(new ActivityWizard(), element);
        else if (elementClass.equals(ExternalEvent.class))
            launchWizard(new EventHandlerWizard(), element);
        else if (elementClass.equals(ExternalEvent.CamelProcessLaunch.class))
            launchWizard(new CamelProcessHandlerWizard(), element);
        else if (elementClass.equals(ExternalEvent.CamelEventNotify.class))
            launchWizard(new CamelNotifyHandlerWizard(), element);
        else if (elementClass.equals(Page.class))
            launchWizard(new NewPageWizard(), element);
        else if (elementClass.equals(Report.class))
            launchWizard(new NewReportWizard(), element);
        else if (elementClass.equals(Rule.class))
            launchWizard(new NewRuleWizard(), element);
        else if (elementClass.equals(WordDoc.class))
            launchWizard(new NewWordDocWizard(), element);
        else if (elementClass.equals(Script.class))
            launchWizard(new NewScriptWizard(), element);
        else if (elementClass.equals(JavaSource.class))
            launchWizard(new NewJavaWizard(), element);
        else if (elementClass.equals(CamelRoute.class))
            launchWizard(new NewCamelRouteWizard(), element);
        else if (elementClass.equals(Template.class))
            launchWizard(new NewTemplateWizard(), element);
        else if (elementClass.equals(WebResource.class))
            launchWizard(new NewWebResourceWizard(), element);
        else if (elementClass.equals(SpringConfig.class))
            launchWizard(new NewSpringConfigWizard(), element);
        else if (elementClass.equals(JarFile.class))
            launchWizard(new NewJarFileWizard(), element);
        else if (elementClass.equals(AutomatedTestCase.class))
            launchWizard(new NewTestCaseWizard(), element);
        else if (elementClass.equals(AutomatedTestResults.class))
            launchWizard(new NewYamlWizard(), element);
        else if (elementClass.equals(Json.class))
            launchWizard(new NewJsonWizard(), element);
        else if (elementClass.equals(TaskTemplate.class))
            launchWizard(new NewTaskTemplateWizard(), element);
        else if (elementClass.equals(TextResource.class))
            launchWizard(new NewTextResourceWizard(), element);
        else if (elementClass.equals(XmlDoc.class))
            launchWizard(new NewXmlDocWizard(), element);
    }

    private void launchWizard(IWorkbenchWizard wizard, WorkflowElement element) {
        wizard.init(getWorkbench(), element);
        new WizardDialog(getShell(), wizard).open();
    }

    public boolean open(WorkflowElement element) {
        if (element instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) element;
            try {
                ProcessEditor processEditor = (ProcessEditor) getPage().openEditor(processVersion,
                        "mdw.editors.process");
                showPropertiesView();
                processEditor.setFocus();
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(getShell(), ex, "Open Process", processVersion.getProject());
            }
        }
        else if (element instanceof AutomatedTestCase && ((AutomatedTestCase) element).isLegacy()) {
            // open the old way instead of as workflow asset
            AutomatedTestCase testCase = (AutomatedTestCase) element;
            IFile file = testCase.getCommandsFile();
            IWorkbenchPage activePage = MdwPlugin.getActivePage();
            try {
                IDE.openEditor(activePage, file, true);
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Open Test Case", testCase.getProject());
            }
        }
        else if (element instanceof WorkflowAsset) {
            WorkflowAsset asset = (WorkflowAsset) element;
            asset.openFile(new NullProgressMonitor());
        }
        else if (element instanceof LegacyExpectedResults) {
            LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
            IFile file = expectedResult.getExpectedResult();
            IWorkbenchPage activePage = MdwPlugin.getActivePage();
            try {
                IDE.openEditor(activePage, file, true);
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Open Expected Result", expectedResult.getProject());
            }
        }
        else if (element instanceof com.centurylink.mdw.plugin.designer.model.File) {
            com.centurylink.mdw.plugin.designer.model.File file = (com.centurylink.mdw.plugin.designer.model.File) element;
            IFile workspaceFile = file.getWorkspaceFile();
            IWorkbenchPage activePage = MdwPlugin.getActivePage();
            try {
                IDE.openEditor(activePage, workspaceFile, true);
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Open File", file.getProject());
            }
        }
        else {
            return false;
        }

        return true;
    }

    public void refresh(WorkflowElement element) {
        if (element instanceof WorkflowProject) {
            final WorkflowProject workflowProject = (WorkflowProject) element;
            workflowProject.clear();
            workflowProject.fireElementChangeEvent(workflowProject, ChangeType.SETTINGS_CHANGE,
                    null);
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    if (!workflowProject.isFilePersist())
                        syncOpenEditors(workflowProject);
                }
            });
        }
        else if (element instanceof AutomatedTestSuite) {
            AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
            testSuite.readLegacyCases();
            testSuite.fireElementChangeEvent(testSuite, ChangeType.SETTINGS_CHANGE, null);
        }
        else if (element instanceof AutomatedTestCase) {
            AutomatedTestCase testCase = (AutomatedTestCase) element;
            testCase.setTestCase(testCase.getTestCase());
            testCase.fireElementChangeEvent(testCase, ChangeType.SETTINGS_CHANGE, null);
        }
    }

    public void update(WorkflowElement element) {
        if (element instanceof WorkflowPackage) {
            WorkflowPackage packageVersion = (WorkflowPackage) element;
            if (MessageDialog.openConfirm(getShell(), "Update Package",
                    "Bring latest versions of Processes and Workflow Assets into package '"
                            + packageVersion.getLabel() + "'?")) {
                packageVersion.getProject().getDesignerProxy()
                        .updateProcessesAndAssetsToLatest(packageVersion);
                packageVersion.fireElementChangeEvent(packageVersion, ChangeType.SETTINGS_CHANGE,
                        null);
            }
        }
    }

    public void serverAction(WorkflowElement element, String action) {
        if (element instanceof WorkflowProject) {
            WorkflowProject project = (WorkflowProject) element;
            if (action.equals(REFRESH_CACHES))
                project.getDesignerProxy().getCacheRefresh().doRefresh(false);
            else if (action.equals(STUB_SERVER))
                project.getDesignerProxy().toggleStubServer();
            else if (action.equals(LOG_WATCHER)) {
                // TODO
            }
        }
    }

    public void run(Object element) {
        if (element instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) element;
            IEditorPart editorPart = findOpenEditor(processVersion);
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Process Launch",
                        "Save process '" + processVersion.getLabel() + "' before launching?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            if (MdwPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PreferenceConstants.PREFS_WEB_BASED_PROCESS_LAUNCH)) {
                // web-based process launch
                try {
                    IViewPart viewPart = getPage().showView("mdw.views.designer.process.launch");
                    if (viewPart != null) {
                        ProcessLaunchView launchView = (ProcessLaunchView) viewPart;
                        launchView.setProcess(processVersion);
                    }
                }
                catch (PartInitException ex) {
                    PluginMessages.log(ex);
                }
            }
            else {
                if (editorPart == null) {
                    // process must be open
                    open((WorkflowElement) element);
                }
                ProcessLaunchShortcut launchShortcut = new ProcessLaunchShortcut();
                launchShortcut.launch(new StructuredSelection(processVersion),
                        ILaunchManager.RUN_MODE);
            }
        }
        else if (element instanceof Activity) {
            Activity activity = (Activity) element;
            WorkflowProcess processVersion = activity.getProcess();

            IEditorPart editorPart = findOpenEditor(processVersion);
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Activity Launch",
                        "Save process '" + processVersion.getLabel() + "' before launching?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            ActivityLaunchShortcut launchShortcut = new ActivityLaunchShortcut();
            launchShortcut.launch(new StructuredSelection(activity), ILaunchManager.RUN_MODE);
        }
        else if (element instanceof ExternalEvent) {
            ExternalEvent externalEvent = (ExternalEvent) element;
            ExternalEventLaunchShortcut launchShortcut = new ExternalEventLaunchShortcut();
            launchShortcut.launch(new StructuredSelection(externalEvent), ILaunchManager.RUN_MODE);
        }
        else if (element instanceof Template) {
            Template template = (Template) element;
            IEditorPart editorPart = template.getFileEditor();
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Run Template",
                        "Save template '" + template.getName() + "' before running?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            template.openFile(new NullProgressMonitor());
            new TemplateRunDialog(getShell(), template).open();
        }
        else if (element instanceof Page) {
            Page page = (Page) element;
            IEditorPart editorPart = page.getFileEditor();
            if (editorPart != null) {
                if (editorPart.isDirty()) {
                    if (MessageDialog.openQuestion(getShell(), "Run Page",
                            "Save page '" + page.getName() + "' before running?"))
                        editorPart.doSave(new NullProgressMonitor());
                }
            }
            page.run();
        }
        else if (element instanceof WorkflowProject || element instanceof ServerSettings) {
            ServerSettings serverSettings;
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                if (workflowProject.isRemote())
                    throw new IllegalArgumentException("Cannot run server for remote projects.");
                serverSettings = workflowProject.getServerSettings();
            }
            else {
                serverSettings = (ServerSettings) element;
            }

            if (ServerRunner.isServerRunning()) {
                String question = "A server may be running already.  Shut down the currently-running server?";
                MessageDialog dlg = new MessageDialog(getShell(), "Server Running", null, question,
                        MessageDialog.QUESTION_WITH_CANCEL,
                        new String[] { "Shutdown", "Ignore", "Cancel" }, 0);
                int res = dlg.open();
                if (res == 0)
                    new ServerRunner(serverSettings, getShell().getDisplay()).stop();
                else if (res == 2)
                    return;
            }

            if (serverSettings.getHome() == null && element instanceof WorkflowProject) {
                final IProject project = serverSettings.getProject().isCloudProject()
                        ? serverSettings.getProject().getSourceProject()
                        : serverSettings.getProject().getEarProject();
                @SuppressWarnings("restriction")
                org.eclipse.ui.internal.dialogs.PropertyDialog dialog = org.eclipse.ui.internal.dialogs.PropertyDialog
                        .createDialogOn(getShell(), "mdw.workflow.mdwServerConnectionsPropertyPage",
                                project);
                if (dialog != null)
                    dialog.open();
            }
            else {
                IPreferenceStore prefStore = MdwPlugin.getDefault().getPreferenceStore();
                if (element instanceof WorkflowProject)
                    prefStore.setValue(PreferenceConstants.PREFS_SERVER_WF_PROJECT,
                            ((WorkflowProject) element).getName());
                else
                    prefStore.setValue(PreferenceConstants.PREFS_RUNNING_SERVER,
                            serverSettings.getServerName());

                ServerRunner runner = new ServerRunner(serverSettings, getShell().getDisplay());
                if (serverSettings.getProject() != null)
                    runner.setJavaProject(serverSettings.getProject().getJavaProject());
                runner.start();
            }
        }
    }

    public void runFromPage(WorkflowElement element) {
        if (element instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) element;
            IEditorPart editorPart = findOpenEditor(processVersion);
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Process Launch",
                        "Save process '" + processVersion.getLabel() + "' before launching?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            WebApp webapp = WebApp.TaskManager;
            if (element.getProject().checkRequiredVersion(5, 5)
                    && !processVersion.isCompatibilityRendering())
                webapp = WebApp.MdwHub;
            WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(element.getProject(),
                    webapp);
            String urlPath = "/start.jsf?process=" + processVersion.getName();
            if (webapp.equals(WebApp.MdwHub)) {
                urlPath = urlPath + "&processVersion=" + processVersion.getVersion();
            }
            launchAction.launch(element.getProject(), urlPath);
        }
    }

    public void debug(Object element) {
        if (element instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) element;
            IEditorPart editorPart = findOpenEditor(processVersion);
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Process Launch",
                        "Save process '" + processVersion.getLabel() + "' before launching?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            if (editorPart == null) {
                // process must be open
                open(processVersion);
            }
            ProcessLaunchShortcut launchShortcut = new ProcessLaunchShortcut();
            launchShortcut.launch(new StructuredSelection(processVersion),
                    ILaunchManager.DEBUG_MODE);
        }
        else if (element instanceof Activity) {
            Activity activity = (Activity) element;
            WorkflowProcess processVersion = activity.getProcess();
            IEditorPart editorPart = findOpenEditor(processVersion);
            if (editorPart != null && editorPart.isDirty()) {
                if (MessageDialog.openQuestion(getShell(), "Activity Launch",
                        "Save process '" + processVersion.getLabel() + "' before launching?"))
                    editorPart.doSave(new NullProgressMonitor());
            }

            ActivityLaunchShortcut launchShortcut = new ActivityLaunchShortcut();
            launchShortcut.launch(new StructuredSelection(activity), ILaunchManager.DEBUG_MODE);
        }
        else if (element instanceof ExternalEvent) {
            ExternalEvent externalEvent = (ExternalEvent) element;
            ExternalEventLaunchShortcut launchShortcut = new ExternalEventLaunchShortcut();
            launchShortcut.launch(new StructuredSelection(externalEvent),
                    ILaunchManager.DEBUG_MODE);
        }
        else if (element instanceof WorkflowProject || element instanceof ServerSettings) {
            ServerSettings serverSettings;
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                serverSettings = workflowProject.getServerSettings();
            }
            else {
                serverSettings = (ServerSettings) element;
            }

            serverSettings.setDebug(true);
            if (serverSettings.getDebugPort() == 0)
                serverSettings.setDebugPort(8500);
            run(serverSettings);
        }
    }

    public void clientShell(Object element) {
        ServerSettings serverSettings;
        if (element instanceof WorkflowProject || element instanceof ServerSettings) {
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                serverSettings = workflowProject.getServerSettings();
                MdwPlugin.getDefault().getPreferenceStore().setValue(
                        PreferenceConstants.PREFS_SERVER_WF_PROJECT, workflowProject.getName());
            }
            else {
                serverSettings = (ServerSettings) element;
                MdwPlugin.getDefault().getPreferenceStore().setValue(
                        PreferenceConstants.PREFS_RUNNING_SERVER, serverSettings.getServerName());
            }

            ServerConfigurator configurator = ServerConfigurator.Factory.create(serverSettings);
            configurator.doClientShell(getShell());
        }
    }

    public void deploy(Object element) {
        if (element instanceof WorkflowProject || element instanceof ServerSettings) {
            ServerSettings serverSettings;
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                serverSettings = workflowProject.getServerSettings();
                MdwPlugin.getDefault().getPreferenceStore().setValue(
                        PreferenceConstants.PREFS_SERVER_WF_PROJECT, workflowProject.getName());
            }
            else {
                serverSettings = (ServerSettings) element;
                MdwPlugin.getDefault().getPreferenceStore().setValue(
                        PreferenceConstants.PREFS_RUNNING_SERVER, serverSettings.getServerName());
            }

            ServerConfigurator configurator = ServerConfigurator.Factory.create(serverSettings);
            configurator.doDeploy(getShell());
        }
    }

    public void stop(Object element) {
        if (element instanceof WorkflowProject || element instanceof ServerSettings) {
            ServerSettings serverSettings;
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                serverSettings = workflowProject.getServerSettings();
            }
            else {
                serverSettings = (ServerSettings) element;
            }

            ServerRunner runner = new ServerRunner(serverSettings, getShell().getDisplay());
            runner.stop();
        }
    }

    public void delete(WorkflowElement[] elements) {
        boolean globalConf = false;
        boolean includeInstances = false;

        if (elements.length > 1) {
            boolean globalConfAllowed = true;
            List<WorkflowElement> lockedElems = new ArrayList<WorkflowElement>();
            for (WorkflowElement element : elements) {
                if ((element instanceof WorkflowProject) || (element instanceof WorkflowPackage)) {
                    globalConfAllowed = false;
                    break;
                }
                else if (element instanceof WorkflowProcess) {
                    WorkflowProcess pv = (WorkflowProcess) element;
                    if (pv.getLockingUser() != null && !pv.isLockedToUser())
                        lockedElems.add(pv);
                }
                else if (element instanceof WorkflowAsset) {
                    WorkflowAsset dd = (WorkflowAsset) element;
                    if (dd.getLockingUser() != null && !dd.isLockedToUser())
                        lockedElems.add(dd);
                }
            }
            if (!lockedElems.isEmpty()) {
                PluginMessages.uiList(getShell(),
                        "Error: The following elements are locked to other users.\nPlease exclude them from your selection or have them unlocked before proceeding.",
                        "Delete Elements", lockedElems);
                return;
            }
            if (globalConfAllowed) {
                WorkflowElementDeleteDialog multipleDeleteDialog = new WorkflowElementDeleteDialog(
                        getShell(), Arrays.asList(elements));
                int res = multipleDeleteDialog.open();
                if (res == Dialog.CANCEL)
                    return;
                else if (res == Dialog.OK) {
                    globalConf = true;
                    includeInstances = multipleDeleteDialog.isIncludeInstances();
                }
            }
        }

        for (WorkflowElement element : elements) {
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                if (!workflowProject.isRemote()) {
                    MessageDialog.openWarning(getShell(), "Delete Project",
                            "Please delete the underlying Java Project in Package Explorer view.");
                    return;
                }
                boolean confirmed = MessageDialog.openConfirm(getShell(), "Confirm Delete",
                        "Delete workflow project: " + workflowProject.getName() + "?");
                if (confirmed) {
                    WorkflowProjectManager.getInstance().deleteProject(workflowProject);
                    workflowProject.fireElementChangeEvent(ChangeType.ELEMENT_DELETE, null);
                }
                else {
                    return;
                }
            }
            else if (element instanceof AutomatedTestCase
                    && ((AutomatedTestCase) element).isLegacy()) {
                // still allow deletion of legacy test stuff
                final AutomatedTestCase testCase = (AutomatedTestCase) element;
                if (globalConf || MessageDialog.openConfirm(getShell(), "Delete Legacy Test Case",
                        "Delete " + testCase.getLabel() + "?")) {
                    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                        public void run() {
                            File tcDir = testCase.getTestCaseDirectory();
                            try {
                                PluginUtil.deleteDirectory(tcDir);
                                testCase.getTestSuite().getTestCases().remove(testCase);
                                try {
                                    IFolder folder = testCase.getProject().getOldTestCasesFolder();
                                    if (folder.exists())
                                        folder.refreshLocal(IResource.DEPTH_INFINITE,
                                                new NullProgressMonitor());
                                }
                                catch (CoreException ex) {
                                    PluginMessages.uiError(ex, "Delete Legacy Test Case",
                                            testCase.getProject());
                                }
                                testCase.fireElementChangeEvent(ChangeType.ELEMENT_DELETE, null);
                                testCase.removeElementChangeListener(testCase.getProject());
                            }
                            catch (IOException ex) {
                                PluginMessages.uiError(ex, "Delete Test Case",
                                        testCase.getProject());
                            }
                        }
                    });
                }
            }
            else if (element instanceof LegacyExpectedResults) {
                final LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
                if (globalConf
                        || MessageDialog.openConfirm(getShell(), "Delete Legacy Expected Result",
                                "Delete " + expectedResult.getLabel() + "?")) {
                    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                        public void run() {
                            File file = expectedResult.getExpectedResultFile();
                            if (file.delete()) {
                                expectedResult.getTestCase().getLegacyExpectedResults()
                                        .remove(expectedResult);
                                try {
                                    IFolder folder = expectedResult.getProject()
                                            .getOldTestCasesFolder();
                                    if (folder.exists())
                                        folder.refreshLocal(IResource.DEPTH_INFINITE,
                                                new NullProgressMonitor());
                                }
                                catch (CoreException ex) {
                                    PluginMessages.uiError(ex, "Delete Legacy Expected Result",
                                            expectedResult.getProject());
                                }
                                expectedResult.fireElementChangeEvent(ChangeType.ELEMENT_DELETE,
                                        null);
                                expectedResult
                                        .removeElementChangeListener(expectedResult.getProject());
                            }
                            else {
                                PluginMessages.uiError(
                                        "Cannot delete expected result " + expectedResult.getName(),
                                        "Delete Result", expectedResult.getProject());
                            }
                        }
                    });
                }
            }
            else if (element instanceof com.centurylink.mdw.plugin.designer.model.File) {
                final com.centurylink.mdw.plugin.designer.model.File file = (com.centurylink.mdw.plugin.designer.model.File) element;
                if (globalConf || MessageDialog.openConfirm(getShell(), "Delete File",
                        "Delete " + file.getLabel() + "?")) {
                    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                        public void run() {
                            IFile workspaceFile = file.getWorkspaceFile();
                            try {
                                workspaceFile.delete(true, null);
                                WorkflowElement parent = file.getParent();
                                if (parent instanceof AutomatedTestCase) {
                                    AutomatedTestCase testCase = (AutomatedTestCase) parent;
                                    testCase.getFiles().remove(file);
                                }
                                file.fireElementChangeEvent(ChangeType.ELEMENT_DELETE, null);
                                file.removeElementChangeListener(file.getProject());
                                refresh(file.getProject().getLegacyTestSuite());
                            }
                            catch (Exception ex) {
                                PluginMessages.uiError("Cannot delete file " + file.getName(),
                                        "Delete File", file.getProject());
                            }
                        }
                    });
                }
            }
            else {
                WorkflowProject workflowProject = element.getProject();
                DesignerProxy designerProxy = workflowProject.getDesignerProxy();
                if (element instanceof WorkflowPackage) {
                    WorkflowPackage packageToDelete = (WorkflowPackage) element;
                    PackageDeleteDialog packageDeleteDialog = new PackageDeleteDialog(getShell(),
                            packageToDelete);
                    if (packageDeleteDialog.open() == Dialog.OK) {
                        designerProxy.deletePackage(packageToDelete);
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof WorkflowProcess) {
                    WorkflowProcess processVersion = (WorkflowProcess) element;
                    if (!processVersion.getProject().isFilePersist()
                            && processVersion.getLockingUser() != null
                            && !processVersion.isLockedToUser()) {
                        MessageDialog.openError(getShell(), "Cannot Delete",
                                "Process '" + processVersion.getLabel() + "' is locked to user '"
                                        + processVersion.getLockingUser()
                                        + "'.\nPlease have it unlocked before deleting.");
                        return;
                    }
                    ProcessDeleteDialog deleteDialog = new ProcessDeleteDialog(getShell(),
                            processVersion);
                    if (globalConf || deleteDialog.open() == Dialog.OK) {
                        closeOpenEditor(processVersion, false);
                        includeInstances = includeInstances || deleteDialog.isIncludeInstances();
                        designerProxy.deleteProcess(processVersion, includeInstances);
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof TaskTemplate) {
                    TaskTemplate taskTemplate = (TaskTemplate) element;
                    if (globalConf || MessageDialog.openConfirm(getShell(), "Confirm Delete",
                            "Delete " + taskTemplate.getTitle() + " '" + taskTemplate.getLabel()
                                    + "'?")) {
                        designerProxy.deleteTaskTemplate(taskTemplate);
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof WorkflowAsset) {
                    WorkflowAsset asset = (WorkflowAsset) element;
                    if (!asset.getProject().isFilePersist() && asset.getLockingUser() != null
                            && !asset.isLockedToUser()) {
                        MessageDialog.openError(getShell(), "Cannot Delete",
                                asset.getTitle() + " '" + asset.getLabel() + "' is locked to user '"
                                        + asset.getLockingUser()
                                        + "'.\nPlease have it unlocked before deleting");
                        return;
                    }
                    if (globalConf || MessageDialog.openConfirm(getShell(), "Confirm Delete",
                            "Delete " + asset.getTitle() + " '" + asset.getLabel() + "'?")) {
                        if (asset.getFileEditor() != null) {
                            IEditorInput editorInput = asset.getFileEditor().getEditorInput();

                            if (editorInput != null)
                                closeOpenEditor(editorInput, false);
                        }

                        WorkflowAssetFactory.deRegisterAsset(asset);
                        designerProxy.deleteWorkflowAsset(asset);
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof ActivityImpl) {
                    ActivityImpl activityImpl = (ActivityImpl) element;
                    ActivityImplDeleteDialog deleteDialog = new ActivityImplDeleteDialog(getShell(),
                            activityImpl);
                    if (globalConf || deleteDialog.open() == Dialog.OK) {
                        designerProxy.deleteActivityImpl(activityImpl,
                                deleteDialog.isIncludeActivities());
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof ExternalEvent) {
                    ExternalEvent externalEvent = (ExternalEvent) element;
                    if (globalConf || MessageDialog.openConfirm(getShell(), "Confirm Delete",
                            "Delete Event Handler: " + externalEvent.getLabel() + "?")) {
                        designerProxy.deleteExternalEvent(externalEvent);
                    }
                    else {
                        return;
                    }
                }
                else if (element instanceof TaskTemplate) {
                    MessageDialog.openWarning(getShell(), "TODO",
                            "Delete task template not yet implemented");
                }
                if (RunnerStatus.SUCCESS.equals(designerProxy.getRunnerStatus())) {
                    // notify listeners
                    element.fireElementChangeEvent(ChangeType.ELEMENT_DELETE, null);
                    element.removeElementChangeListener(workflowProject);
                }
            }
        }
    }

    public void rename(WorkflowElement element) {
        RenameDialog renameDialog = new RenameDialog(getShell(), element);
        renameDialog.setTitle("Rename " + element.getTitle());
        if (element instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) element;
            String lbl = "'" + processVersion.getLabel() + "'";
            IEditorPart editor = findOpenEditor(processVersion);
            if (editor != null) {
                String message = lbl
                        + " is currently open in an editor.\nPlease save and close before renaming.";
                MessageDialog.openError(getShell(), "Process Explorer", message);
                return;
            }
            if (processVersion.getLockingUser() != null && !processVersion.isLockedToUser()) {
                String message = lbl + " is currently locked to " + processVersion.getLockingUser()
                        + ", so it cannot be renamed";
                MessageDialog.openError(getShell(), "Process Explorer", message);
                return;
            }
        }
        else if (element instanceof WorkflowAsset) {
            WorkflowAsset asset = (WorkflowAsset) element;
            if (asset.getFileEditor() != null) {
                IEditorPart assetEditor = findOpenEditor(asset.getFileEditor().getEditorInput());
                if (assetEditor != null) {
                    String message = "'" + asset.getName()
                            + "' is currently open in an editor.\nPlease save and close before renaming.";
                    MessageDialog.openError(getShell(), "Process Explorer", message);
                    return;
                }
            }
        }
        else if (element instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) element;
            if (!workflowProject.isRemote()) {
                if (workflowProject.isCloudProject())
                    MessageDialog.openWarning(getShell(), "Cloud Project",
                            "Please rename the underlying Java Project in Package Explorer and then refresh the Process Explorer view.");
                return;
            }
        }
        int result = renameDialog.open();
        if (result == Dialog.OK) {
            String newName = renameDialog.getNewName();
            if (element instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) element;
                if (!workflowProject.isRemote())
                    return;
                WorkflowProjectManager.renameRemoteProject(workflowProject, newName);
                workflowProject.fireElementChangeEvent(ChangeType.RENAME, null);
            }
            else {
                WorkflowProject workflowProject = element.getProject();
                DesignerProxy designerProxy = workflowProject.getDesignerProxy();
                if (element instanceof WorkflowProcess) {
                    WorkflowProcess processVersion = (WorkflowProcess) element;
                    designerProxy.renameProcess(processVersion, newName);
                }
                else if (element instanceof WorkflowPackage) {
                    WorkflowPackage packageVersion = (WorkflowPackage) element;
                    designerProxy.renamePackage(packageVersion, newName);
                }
                else if (element instanceof WorkflowAsset) {
                    WorkflowAsset asset = (WorkflowAsset) element;
                    designerProxy.renameWorkflowAsset(asset, newName);
                }
                element.fireElementChangeEvent(ChangeType.RENAME, element.getName());
            }
        }
    }

    public void showInstances(WorkflowElement element) {
        if (element instanceof WorkflowProject) {
            // TODO myTasks is hardcoded
            WorkflowProject project = (WorkflowProject) element;
            try {
                IViewPart viewPart = getPage().showView("mdw.views.designer.list.myTasks");
                if (viewPart != null) {
                    MyTasksView myTasksView = (MyTasksView) viewPart;
                    myTasksView.setProject(project);
                    myTasksView.refreshTable();
                }
            }
            catch (PartInitException ex) {
                PluginMessages.log(ex);
            }
        }
        else if (element instanceof WorkflowProcess) {
            try {
                IViewPart viewPart = getPage().showView("mdw.views.designer.process.instance.list");
                if (viewPart != null) {
                    ProcessInstanceListView instancesView = (ProcessInstanceListView) viewPart;
                    instancesView.setProcess((WorkflowProcess) element);
                }
            }
            catch (PartInitException ex) {
                PluginMessages.log(ex);
            }
        }
    }

    public void incrementVersion(final WorkflowElement element) {
        if (element instanceof WorkflowPackage) {
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    WorkflowPackage pkg = (WorkflowPackage) element;
                    try {
                        DesignerProxy dp = pkg.getProject().getDesignerProxy();
                        pkg.setExported(true);
                        dp.setPackage(pkg);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "Increment Version", element.getProject());
                    }
                }
            });
        }
    }

    public void setVersion(final WorkflowElement element) {
        if (element instanceof WorkflowPackage) {
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    WorkflowPackage pkg = (WorkflowPackage) element;
                    SetVersionDialog dlg = new SetVersionDialog(getShell(), pkg);
                    int result = dlg.open();
                    if (result == Dialog.OK) {
                        try {
                            DesignerProxy dp = pkg.getProject().getDesignerProxy();
                            pkg.setVersion(dlg.getNewVersion());
                            pkg.setExported(false); // avoid forcing version
                                                    // increment on save
                            dp.setPackage(pkg);
                        }
                        catch (Exception ex) {
                            PluginMessages.uiError(ex, "Set Version", element.getProject());
                        }
                    }
                }
            });
        }
    }

    public void tagVersion(WorkflowElement[] elements) {
        final List<WorkflowPackage> packages = new ArrayList<WorkflowPackage>();

        for (WorkflowElement element : elements) {
            if (element instanceof WorkflowPackage)
                packages.add((WorkflowPackage) element);
            else
                throw new UnsupportedOperationException(
                        "Invalid type for tagVersion: " + element.getClass().getName());
        }

        MdwListInputDialog dlg = new MdwListInputDialog(getShell(), "Tag Workflow Package(s)",
                "Enter tag to apply to package versions", packages);
        if (dlg.open() == Dialog.CANCEL)
            return;

        final String tag = dlg.getInput();
        if (tag == null)
            return;

        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                for (final WorkflowPackage pkg : packages) {
                    try {
                        DesignerProxy dp = pkg.getProject().getDesignerProxy();
                        dp.tagPackage(pkg, tag);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "Tag Package", pkg.getProject());
                        break;
                    }
                }
            }
        });
    }

    public void copy(WorkflowElement[] elements, Clipboard clipboard) {
        String copyElements = "";
        for (int i = 0; i < elements.length; i++) {
            copyElements += elements[i].toString();
            if (i < elements.length - 1)
                copyElements += "#";
        }
        Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
        Object[] data = new Object[] { copyElements };
        clipboard.setContents(data, transfers);
    }

    public void paste(Clipboard clipboard, ProcessExplorerDropTarget dropTarget,
            WorkflowElement targetElement) {
        WorkflowPackage destPackage = targetElement.getPackage();
        if (destPackage != null && !destPackage.isArchived()) {
            Object clipboardContents = clipboard.getContents(TextTransfer.getInstance());
            if (clipboardContents instanceof String) {
                String clipString = (String) clipboardContents;
                if (clipString != null && (clipString.startsWith("Process~")
                        || clipString.startsWith("ExternalEvent~")
                        || clipString.startsWith("ActivityImpl~")
                        || clipString.startsWith("WorkflowAsset~"))) {
                    if (dropTarget.isValidDrop(clipString, destPackage, DND.DROP_COPY))
                        dropTarget.drop(clipString, destPackage, DND.DROP_COPY);
                }
            }
        }
    }

    public void export(Class<? extends WorkflowElement> elementClass,
            IStructuredSelection selection) {
        if (elementClass.equals(WorkflowProject.class)) {
            ExportProjectWizard exportProjectWizard = new ExportProjectWizard();
            exportProjectWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), exportProjectWizard).open();
        }
        else if (elementClass.equals(WorkflowPackage.class)) {
            ExportPackageWizard exportPackageWizard = new ExportPackageWizard();
            exportPackageWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), exportPackageWizard).open();
        }
        else if (elementClass.equals(WorkflowProcess.class)) {
            ExportProcessWizard exportProcessWizard = new ExportProcessWizard();
            exportProcessWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), exportProcessWizard).open();
        }
        else if (elementClass.equals(WorkflowAsset.class)) {
            ExportAssetWizard exportAssetWizard = new ExportAssetWizard();
            exportAssetWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), exportAssetWizard).open();
        }
        else if (elementClass.equals(TaskTemplate.class)) {
            ExportTaskTemplatesWizard exportTemplateWizard = new ExportTaskTemplatesWizard();
            exportTemplateWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), exportTemplateWizard).open();
        }
    }

    public void exportAttributes(String attributePrefix, WorkflowElement element) {
        if (element instanceof WorkflowPackage || element instanceof WorkflowProcess) {
            ExportAttributesWizard exportAttributesWizard = new ExportAttributesWizard();
            exportAttributesWizard.init(getWorkbench(), element);
            exportAttributesWizard.setPrefix(attributePrefix);
            new WizardDialog(getShell(), exportAttributesWizard).open();
        }
    }

    public void importAttributes(String attributePrefix, WorkflowElement element) {
        if (element instanceof WorkflowPackage || element instanceof WorkflowProcess) {
            ImportAttributesWizard importAttributesWizard = new ImportAttributesWizard();
            importAttributesWizard.init(getWorkbench(), element);
            importAttributesWizard.setPrefix(attributePrefix);
            new WizardDialog(getShell(), importAttributesWizard).open();
        }
    }

    public void exportAs(WorkflowElement selection) {
        WorkflowProcess processVersion = (WorkflowProcess) selection;
        open(processVersion);
        ProcessEditor procEd = (ProcessEditor) findOpenEditor(processVersion);
        ExportAsDialog saveImageDialog = new ExportAsDialog(getShell(), processVersion,
                procEd.getProcessCanvasWrapper().getFlowchartPage());
        saveImageDialog.open();
    }

    public void imporT(Class<? extends WorkflowElement> elementClass, WorkflowElement selection) {
        if (elementClass.equals(WorkflowProject.class)) {
            ImportProjectWizard importProjectWizard = new ImportProjectWizard();
            importProjectWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), importProjectWizard).open();
        }
        else if (elementClass.equals(WorkflowPackage.class)) {
            ImportPackageWizard importPackageWizard = new ImportPackageWizard();
            importPackageWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), importPackageWizard).open();
        }
        else if (elementClass.equals(WorkflowProcess.class)) {
            ImportProcessWizard importProcessWizard = new ImportProcessWizard();
            importProcessWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), importProcessWizard).open();
        }
        else if (elementClass.equals(WorkflowAsset.class)) {
            ImportAssetWizard importAssetWizard = new ImportAssetWizard();
            importAssetWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), importAssetWizard).open();
        }
        else if (elementClass.equals(TaskTemplate.class)) {
            ImportTaskTemplatesWizard importTemplateWizard = new ImportTaskTemplatesWizard();
            importTemplateWizard.init(getWorkbench(), selection);
            new WizardDialog(getShell(), importTemplateWizard).open();
        }
    }

    public void remoteImportVcs(final WorkflowProject workflowProject) {
        VcsRepository repo = workflowProject.getMdwVcsRepository();
        String msg = "Pull latest assets into " + workflowProject.getName() + " from Git branch: "
                + repo.getBranch() + "?";
        boolean proceed = MessageDialog.openConfirm(getShell(), "Import from VCS", msg);
        if (proceed) {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(getShell());
            try {
                pmDialog.run(true, false, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Importing remote project from Git...",
                                IProgressMonitor.UNKNOWN);
                        monitor.worked(20);
                        try {
                            workflowProject.getDesignerProxy().remoteImportVcs();
                        }
                        catch (Exception ex) {
                            PluginMessages.log(ex);
                            throw new InvocationTargetException(ex);
                        }
                    }
                });
            }
            catch (Exception ex) {
                PluginMessages.uiError(getShell(), ex, "Import From VCS", workflowProject);
            }
        }
    }

    @SuppressWarnings("restriction")
    public void search(WorkflowElement[] elements) {
        org.eclipse.search.internal.ui.SearchDialog dialog = new org.eclipse.search.internal.ui.SearchDialog(
                MdwPlugin.getActiveWorkbenchWindow(), "mdw.search.processSearchPage");
        dialog.create();
        dialog.setSelectedScope(ISearchPageContainer.SELECTION_SCOPE);
        dialog.open();
    }

    public void findCallers(WorkflowElement element) {
        WorkflowProcess processVersion = (WorkflowProcess) element;
        List<WorkflowProject> projects = new ArrayList<WorkflowProject>();
        projects.add(processVersion.getProject());
        Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
        ProcessSearchQuery searchQuery = new ProcessSearchQuery(projects,
                SearchQuery.SearchType.INVOKING_ENTITY, "*", true, shell);
        searchQuery.setInvokedEntityId(processVersion.getId());

        try {
            ProgressMonitorDialog context = new MdwProgressMonitorDialog(shell);
            NewSearchUI.runQueryInForeground(context, searchQuery);

            // this shouldn't be necessary according to the Eclipse API docs
            NewSearchUI.activateSearchResultView();
            ISearchResultViewPart part = NewSearchUI.getSearchResultView();
            part.updateLabel();
            SearchResultsPage page = (SearchResultsPage) part.getActivePage();
            page.setSearchQuery(searchQuery);
            page.setInput(searchQuery.getSearchResult(), null);
        }
        catch (OperationCanceledException ex) {
            MessageDialog.openInformation(shell, "Search Cancelled",
                    "Search for callers cancelled.");
        }
        catch (Exception ex) {
            PluginMessages.uiError(shell, ex, "Search for Callers", processVersion.getProject());
        }
    }

    public void showHierarchy(final WorkflowElement element) {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                WorkflowProcess processVersion = (WorkflowProcess) element;
                try {
                    IViewPart viewPart = getPage().showView(ProcessHierarchyView.VIEW_ID);
                    if (viewPart != null) {
                        ProcessHierarchyView view = (ProcessHierarchyView) viewPart;
                        view.setProcess(processVersion);
                    }
                }
                catch (PartInitException ex) {
                    PluginMessages.uiError(getShell(), ex, "Process Hierarchy",
                            processVersion.getProject());
                }
            }
        });
    }

    public void test(IStructuredSelection selection) {
        AutomatedTestLaunchShortcut launchShortcut = new AutomatedTestLaunchShortcut();
        launchShortcut.launch(selection, ILaunchManager.RUN_MODE);
    }

    public void formatResults(final WorkflowElement workflowElement, final String type) {
        try {
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    try {
                        TestResultsFormatter formatter = new TestResultsFormatter(
                                workflowElement.getProject());
                        if (type.equals(AutomatedTestCase.FUNCTION_TEST))
                            formatter.formatFunctionTestResults();
                        else if (type.equals(AutomatedTestCase.LOAD_TEST))
                            formatter.formatLoadTestResults();
                    }
                    catch (Exception ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                }
            });
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Format Test Results", workflowElement.getProject());
        }
    }

    public void launchSwing(WorkflowProject project, String title) {
        launchSwing(project, title, null, null);
    }

    public void launchSwing(final WorkflowProject project, final String title,
            final String formName, String dataInit) {
        try {
            if (title.equals("Java VisualVM")) {
                String javaVersion = System.getProperty("java.version");
                String badVersionMsg = "Cannot launch Java VisualVM.\nRequires Java version >= 1.6.0_07.\nRunning version: "
                        + javaVersion + ".";
                if (!javaVersion.startsWith("1.6") && !javaVersion.startsWith("1.7")
                        && !javaVersion.startsWith("1.8")) {
                    MessageDialog.openError(getShell(), "Launch Java VisualVM", badVersionMsg);
                    return;
                }
                else if (javaVersion.startsWith("1.6")) {
                    // check minor version
                    String minorVer = javaVersion.substring(javaVersion.indexOf('_') + 1);
                    if (Integer.parseInt(minorVer) < 7) {
                        MessageDialog.openError(getShell(), "Launch Java VisualVM", badVersionMsg);
                        return;
                    }
                }

                String jdkBin = PluginUtil.getJdkBin();
                File exe = new File(jdkBin + File.separator + "jvisualvm.exe");
                if (!exe.exists()) {
                    MessageDialog.openError(getShell(), "Launch Java VisualVM",
                            "Executable not found: '" + exe + "'");
                    return;
                }

                String[] cmdArr = new String[] { exe.getAbsolutePath() };
                if (project.isRemote()) {
                    cmdArr = (String[]) PluginUtil.appendArrays(cmdArr, new String[] { "--openjmx",
                            project.getServerSettings().getHost() + ":" + project.getJmxPort() });
                }
                else {
                    cmdArr = (String[]) PluginUtil.appendArrays(cmdArr,
                            new String[] { "--openid", String.valueOf(project.getVisualVmId()) });
                }

                new ProcessConsoleRunner(getShell().getDisplay(), cmdArr).start();
            }
            else if (title.equals("JConsole")) {
                String jdkBin = PluginUtil.getJdkBin();
                File exe = new File(jdkBin + File.separator + "jconsole.exe");
                if (!exe.exists()) {
                    MessageDialog.openError(getShell(), "Launch JConsole",
                            "Executable not found: '" + exe + "'");
                    return;
                }
                String[] cmdArr = new String[] { exe.getAbsolutePath() };
                if (project.isRemote())
                    cmdArr = (String[]) PluginUtil.appendArrays(cmdArr, new String[] {
                            project.getServerSettings().getHost() + ":" + project.getJmxPort() });
                else {
                    String serverProcId = project.getServerSettings().getServerProcessId();
                    if (serverProcId != null)
                        cmdArr = (String[]) PluginUtil.appendArrays(cmdArr,
                                new String[] { serverProcId });
                }
                new ProcessConsoleRunner(getShell().getDisplay(), cmdArr).start();
            }
            else {
                String request = null;
                if (dataInit != null) {
                    FormDataDocument datadoc = new FormDataDocument();
                    datadoc.setAttribute(FormDataDocument.ATTR_ACTION, dataInit);
                    request = datadoc.format();
                }
                final String response = dataInit == null ? null
                        : project.getDesignerProxy().getDesignerDataAccess().engineCall(request);

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            DesignerProxy designerProxy = project.getDesignerProxy();
                            JFrame propmgr_frame = new JFrame(title);
                            FormPanel mainform = new FormPanel(propmgr_frame,
                                    designerProxy.getDesignerDataAccess(),
                                    designerProxy.getPluginDataAccess().getPrivileges());
                            mainform.setData(formName, response);
                            propmgr_frame.setVisible(true);
                        }
                        catch (final Exception ex) {
                            if (getShell() == null) {
                                PluginMessages.log(ex);
                            }
                            else {
                                getShell().getDisplay().asyncExec(new Runnable() {
                                    public void run() {
                                        if (ex.getCause() instanceof ConnectException)
                                            MessageDialog.openError(getShell(), "Launch " + title,
                                                    ex.getCause().getMessage());
                                        else
                                            PluginMessages.uiError(ex, "Launch " + title, project);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }
        catch (Exception ex) {
            if (ex.getCause() instanceof ConnectException)
                MessageDialog.openError(getShell(), "Launch " + title, ex.getCause().getMessage());
            else
                PluginMessages.uiError(ex, "Launch " + title, project);
        }
    }

    public IEditorPart findOpenEditor(IEditorInput workflowElement) {
        return getPage().findEditor(workflowElement);
    }

    private boolean closeOpenEditor(IEditorInput workflowElement, boolean promptForSave) {
        IEditorPart editor = findOpenEditor(workflowElement);
        if (editor != null)
            return getPage().closeEditor(editor, promptForSave);

        return false;
    }

    private Shell getShell() {
        if (MdwPlugin.getActiveWorkbenchWindow() == null)
            return null;
        return MdwPlugin.getActiveWorkbenchWindow().getShell();
    }

    private IWorkbench getWorkbench() {
        return PlatformUI.getWorkbench();
    }

    private IWorkbenchPage getPage() {
        return MdwPlugin.getActivePage();
    }

    public IViewPart showPropertiesView() {
        try {
            return getPage().showView("org.eclipse.ui.views.PropertySheet");
        }
        catch (PartInitException ex) {
            PluginMessages.log(ex);
            return null;
        }
    }

    public List<ProcessEditor> getOpenProcessEditors(WorkflowProject project) {
        List<ProcessEditor> editors = new ArrayList<ProcessEditor>();
        for (IEditorReference edRef : getPage().getEditorReferences()) {
            IEditorPart edPart = edRef.getEditor(false);
            if (edPart instanceof ProcessEditor) {
                // find any open process editors
                WorkflowProcess pv = (WorkflowProcess) edPart.getEditorInput();
                if (pv.getProject().equals(project)) {
                    if (pv.hasInstanceInfo())
                        getPage().closeEditor(edPart, false);
                    else
                        editors.add((ProcessEditor) edPart);
                }
            }
        }
        return editors;
    }

    public List<IEditorPart> getOpenAssetEditors(WorkflowProject project) {
        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        Map<IFile, AssetWorkbenchListener> assetListeners = WorkflowAssetFactory
                .getWorkbenchListeners();

        for (IEditorReference edRef : getPage().getEditorReferences()) {
            IEditorPart edPart = edRef.getEditor(false);
            if (assetListeners != null && edPart.getEditorInput() instanceof FileEditorInput) {
                // find open workflow asset editors
                FileEditorInput fileInput = (FileEditorInput) edPart.getEditorInput();
                AssetWorkbenchListener wbListener = assetListeners.get(fileInput.getFile());
                if (wbListener != null) {
                    WorkflowAsset asset = wbListener.getAsset();
                    if (asset.getProject().equals(project))
                        editors.add(edPart);
                }
            }
        }

        return editors;
    }

    public void syncOpenEditors(final WorkflowProject project) {
        final WorkflowProjectManager projectMgr = WorkflowProjectManager.getInstance();
        final List<ProcessEditor> processEditors = getOpenProcessEditors(project);
        final List<IEditorPart> assetEditors = getOpenAssetEditors(project);

        if (!processEditors.isEmpty() || !assetEditors.isEmpty()) {
            // sync open editors
            IRunnableWithProgress loader = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Loading " + project.getLabel(), 100);
                    monitor.worked(25);

                    // sync process editors
                    for (ProcessEditor ed : processEditors) {
                        WorkflowProcess pv = ed.getProcess();
                        pv.setProject(projectMgr.getWorkflowProject(pv.getProject().getName()));
                        if (pv.getPackage() != null && !pv.getPackage().isDefaultPackage()) {
                            WorkflowPackage pkg = pv.getProject()
                                    .getPackage(pv.getPackage().getName());
                            WorkflowProcess oldPv = pkg.getProcess(pv.getName());
                            if (oldPv != null)
                                pkg.removeProcess(oldPv);
                            pkg.addProcess(pv);
                            pv.setPackage(pkg);
                        }
                    }
                    monitor.worked(50);
                    // sync asset editors
                    Map<IFile, AssetWorkbenchListener> assetListeners = WorkflowAssetFactory
                            .getWorkbenchListeners();
                    for (IEditorPart assetEd : assetEditors) {
                        FileEditorInput fileInput = (FileEditorInput) assetEd.getEditorInput();
                        AssetWorkbenchListener wbListener = assetListeners.get(fileInput.getFile());
                        if (wbListener != null) {
                            WorkflowAsset asset = wbListener.getAsset();
                            WorkflowProject wfProj = projectMgr
                                    .getWorkflowProject(asset.getProject().getName());
                            WorkflowAsset oldAsset = wfProj.getAsset(asset.getName(),
                                    asset.getLanguage(), asset.getVersion());
                            if (asset.getPackage() != null
                                    && !asset.getPackage().isDefaultPackage()) {
                                WorkflowPackage pkg = wfProj
                                        .getPackage(asset.getPackage().getName());
                                if (pkg != null) {
                                    pkg.removeAsset(oldAsset);
                                    pkg.addAsset(asset);
                                    asset.setPackage(pkg);
                                }
                                else {
                                    asset.setPackage(wfProj.getDefaultPackage());
                                }
                            }
                            else {
                                asset.setPackage(wfProj.getDefaultPackage());
                            }

                            asset.addElementChangeListener(wfProj);

                            if (oldAsset != null)
                                WorkflowAssetFactory.deRegisterAsset(oldAsset);
                            WorkflowAssetFactory.registerAsset(asset);
                            assetEd.addPropertyListener(
                                    asset.new AssetEditorPropertyListener(assetEd));
                            WorkflowAssetFactory.registerWorkbenchListener(fileInput.getFile(),
                                    wbListener);
                        }
                    }
                    monitor.worked(25);
                    monitor.done();
                }
            };

            ProgressMonitorDialog progMonDlg = new MdwProgressMonitorDialog(
                    Display.getCurrent().getActiveShell());
            try {
                progMonDlg.run(true, false, loader);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Sync Open Editors", project);
            }

        }
    }
}
