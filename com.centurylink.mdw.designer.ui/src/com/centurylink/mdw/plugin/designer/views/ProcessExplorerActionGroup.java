/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestFile;
import com.centurylink.mdw.designer.testing.TestFileLine;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.MdwMenuManager;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.Activity.AdapterActivity;
import com.centurylink.mdw.plugin.designer.model.Activity.EvaluatorActivity;
import com.centurylink.mdw.plugin.designer.model.Activity.StartActivity;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.CamelRoute;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
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
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.XmlDoc;
import com.centurylink.mdw.plugin.designer.properties.PageletTab;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessExplorerActionGroup extends ActionGroup {
    private ProcessExplorerView view;
    private WorkflowElementActionHandler actionHandler;

    WorkflowElementActionHandler getActionHandler() {
        return actionHandler;
    }

    private ActionGroup newActionGroup;
    private ActionGroup importActionGroup;
    private ActionGroup exportActionGroup;
    private ActionGroup runActionGroup;
    private ActionGroup webActionGroup;
    private ActionGroup formatActionGroup;
    private ActionGroup serverActionGroup;
    private ActionGroup swingActionGroup;

    private MenuManager newMenu;
    private MenuManager importMenu;
    private MenuManager exportMenu;
    private MenuManager runMenu;
    private MenuManager webMenu;
    private MenuManager formatMenu;
    private MenuManager serverMenu;
    private MenuManager swingMenu;

    private IAction openAction;
    private IAction propertiesAction;
    private IAction incrementVersionAction;
    private IAction setVersionAction;
    private IAction tagVersionAction;
    private IAction copyAction;
    private IAction pasteAction;
    private IAction deleteAction;
    private IAction renameAction;
    private IAction importPackageAction;
    private IAction importVcsAction;
    private IAction importProjectAction;
    private IAction importProcessAction;
    private IAction importWorkflowAssetAction;
    private IAction importTaskTemplateAction;
    private IAction exportPackageAction;
    private IAction exportProcessAction;
    private IAction exportTaskTemplateAction;
    private IAction exportWorkflowAssetAction;
    private IAction exportAsAction;
    private IAction runAction;
    private IAction runOnServerAction;
    private IAction runTestsAction;
    private IAction pageRunAction;
    private IAction debugAction;
    private IAction instancesAction;
    private IAction collapseAllAction;
    private IAction refreshAction;
    private IAction refreshToolbarAction;
    private IAction refreshCachesAction;
    private IAction updateAction;
    private IAction stubServerAction;
    private IAction logWatcherAction;
    private IAction newCloudProjectAction;
    private IAction newRemoteProjectAction;
    private IAction newPackageAction;
    private IAction newPackageToolbarAction;
    private IAction newProcessAction;
    private IAction newGeneralActivityAction;
    private IAction newStartActivityAction;
    private IAction newAdapterActivityAction;
    private IAction newEvaluatorActivityAction;
    private IAction newEventHandlerAction;
    private IAction newCamelProcessHandlerAction;
    private IAction newCamelNotifyHandlerAction;
    private IAction newPageAction;
    private IAction newReportAction;
    private IAction newRuleAction;
    private IAction newWordDocAction;
    private IAction newCamelRouteAction;
    private IAction newScriptAction;
    private IAction newTextResourceAction;
    private IAction newJavaSourceAction;
    private IAction newTemplateAction;
    private IAction newWebResourceAction;
    private IAction newSpringConfigAction;
    private IAction newXmlDocAction;
    private IAction newJarFileAction;
    private IAction newTestCaseAction;
    private IAction newYamlAction;
    private IAction newJsonAction;
    private IAction newTaskTemplateAction;
    private IAction searchAction;
    private IAction findCallersAction;
    private IAction showProcessHierarchyAction;
    private IAction compareResultsAction;
    private IAction openInstanceAction;
    private IAction mdwHubAction;
    private IAction taskManagerAction;
    private IAction webToolsAction;
    private IAction sortToolbarAction;
    private IAction filterToolbarAction;
    private IAction eventManagerAction;
    private IAction threadPoolManagerAction;
    private IAction visualVmAction;
    private IAction jconsoleAction;
    private IAction formatFunctionTestResultsAction;
    private IAction formatLoadTestResultsAction;
    private IAction unlockAction;

    private IAction myTasksAction; // TODO maybe submenu

    public ProcessExplorerActionGroup(ProcessExplorerView view) {
        this.view = view;
        this.actionHandler = new WorkflowElementActionHandler();

        newActionGroup = createNewActionGroup();
        importActionGroup = createImportActionGroup();
        exportActionGroup = createExportActionGroup();
        runActionGroup = createRunActionGroup();
        webActionGroup = createWebActionGroup();
        formatActionGroup = createFormatActionGroup();
        serverActionGroup = createServerActionGroup();
        swingActionGroup = createSwingActionGroup();

        openAction = createOpenAction();
        propertiesAction = createPropertiesAction();
        incrementVersionAction = createIncrementVersionAction();
        setVersionAction = createSetVersionAction();
        tagVersionAction = createTagVersionAction();
        copyAction = createCopyAction();
        pasteAction = createPasteAction();
        deleteAction = createDeleteAction();
        renameAction = createRenameAction();
        importPackageAction = createImportPackageAction();
        importVcsAction = createImportVcsAction();
        importProjectAction = createImportProjectAction();
        importProcessAction = createImportProcessAction();
        importTaskTemplateAction = createImportTaskTemplateAction();
        importWorkflowAssetAction = createImportWorkflowAssetAction();
        exportPackageAction = createExportPackageAction();
        exportProcessAction = createExportProcessAction();
        exportTaskTemplateAction = createExportTaskTemplateAction();
        exportWorkflowAssetAction = createExportWorkflowAssetAction();
        exportAsAction = createExportAsAction();
        runAction = createRunAction();
        runOnServerAction = createRunOnServerAction();
        runTestsAction = createRunTestsAction();
        pageRunAction = createRunFromStartPageAction();
        debugAction = createDebugAction();
        instancesAction = createInstancesAction();
        collapseAllAction = createCollapseAllAction();
        refreshAction = createRefreshAction(false);
        refreshToolbarAction = createRefreshAction(true);
        refreshCachesAction = createRefreshCachesAction();
        updateAction = createUpdateAction();
        stubServerAction = createStubServerAction();
        logWatcherAction = createLogWatcherAction();
        newCloudProjectAction = createNewAction(WorkflowElement.class, "Local Project",
                "cloud_project.gif");
        newRemoteProjectAction = createNewAction(WorkflowProject.class,
                MdwPlugin.isRcp() ? "Workflow Project" : "Remote Project", "remote_project.gif");
        newPackageAction = createNewAction(WorkflowPackage.class, "MDW Package", "package.gif");
        newPackageToolbarAction = createNewAction(WorkflowPackage.class, "New Package",
                "new_package.gif");
        newProcessAction = createNewAction(WorkflowProcess.class, "MDW Process", "process.gif");
        newGeneralActivityAction = createNewAction(Activity.class, "General Activity",
                "genact_wiz.gif");
        newStartActivityAction = createNewAction(StartActivity.class, "Start Activity",
                "startact_wiz.gif");
        newAdapterActivityAction = createNewAction(AdapterActivity.class, "Adapter Activity",
                "adaptact_wiz.gif");
        newEvaluatorActivityAction = createNewAction(EvaluatorActivity.class, "Evaluator Activity",
                "evalact_wiz.gif");
        newEventHandlerAction = createNewAction(ExternalEvent.class, "External Event Handler",
                "extevent_wiz.gif");
        newCamelProcessHandlerAction = createNewAction(ExternalEvent.CamelProcessLaunch.class,
                "Camel Process Launch Handler", "camel.gif");
        newCamelNotifyHandlerAction = createNewAction(ExternalEvent.CamelEventNotify.class,
                "Camel Event Notify Handler", "camel.gif");
        newPageAction = createNewAction(Page.class, "Page", "page.gif");
        newReportAction = createNewAction(Report.class, "Report", "report.gif");
        newRuleAction = createNewAction(Rule.class, "Rule", "drools.gif");
        newWordDocAction = createNewAction(WordDoc.class, "Word Document", "word.gif");
        newScriptAction = createNewAction(Script.class, "Script", "script.gif");
        newCamelRouteAction = createNewAction(CamelRoute.class, "Camel Route", "camel.gif");
        newTextResourceAction = createNewAction(TextResource.class, "Text Resource", "doc.gif");
        newXmlDocAction = createNewAction(XmlDoc.class, "XML Document", "xml.gif");
        newJavaSourceAction = createNewAction(JavaSource.class, "Java Source", "java.gif");
        newTemplateAction = createNewAction(Template.class, "Template", "template.gif");
        newWebResourceAction = createNewAction(WebResource.class, "Web Resource", "webtools.gif");
        newSpringConfigAction = createNewAction(SpringConfig.class, "Spring Config", "spring.gif");
        newJarFileAction = createNewAction(JarFile.class, "Jar File", "jar.gif");
        newTestCaseAction = createNewAction(AutomatedTestCase.class, "Test Case", "test.gif");
        newYamlAction = createNewAction(AutomatedTestResults.class, "YAML", "yaml.gif");
        newJsonAction = createNewAction(Json.class, "JSON", "javascript.gif");
        newTaskTemplateAction = createNewAction(TaskTemplate.class, "Task Template", "task.gif");
        searchAction = createSearchAction();
        findCallersAction = createFindCallersAction();
        showProcessHierarchyAction = createShowProcessHierarchyAction();
        compareResultsAction = createCompareResultsAction();
        openInstanceAction = createOpenInstanceAction();
        formatFunctionTestResultsAction = createFormatFunctionTestResultsAction();
        formatLoadTestResultsAction = createFormatLoadTestResultsAction();
        mdwHubAction = createMdwHubAction();
        taskManagerAction = createTaskManagerAction();
        webToolsAction = createWebToolsAction();
        sortToolbarAction = createSortToolbarAction();
        filterToolbarAction = createFilterToolbarAction();
        unlockAction = createUnlockAction();

        eventManagerAction = createSwingLaunchAction("Event Manager", null, "event_mgr",
                "EventManagerHandler");
        threadPoolManagerAction = createSwingLaunchAction("Thread Pool Manager", null,
                "thread_pool_mgr", "ThreadPoolManagerHandler");
        visualVmAction = createSwingLaunchAction("Java VisualVM", "visualvm.gif", null, null);
        jconsoleAction = createSwingLaunchAction("JConsole", "jmxconsole.gif", null, null);

        myTasksAction = createMyTasksAction();
    }

    @Override
    public void setContext(ActionContext context) {
        super.setContext(context);
        newActionGroup.setContext(context);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
        menu.appendToGroup(IContextMenuConstants.GROUP_NEW, newMenu);
        newActionGroup.fillContextMenu(menu);

        final IStructuredSelection selection = getSelection();
        List<?> elements = selection.toList();
        if (elements.size() > 0) {
            // open
            menu.add(new Separator());
            if (openApplies(selection))
                menu.add(openAction);

            // refresh
            if (refreshApplies(selection))
                menu.add(refreshAction);

            // update
            if (updateApplies(selection))
                menu.add(updateAction);

            // properties
            if (showPropertiesApplies(selection))
                menu.add(propertiesAction);

            // inc version
            if (incrementVersionApplies(selection))
                menu.add(incrementVersionAction);

            // inc version
            if (setVersionApplies(selection))
                menu.add(setVersionAction);

            // tag version
            if (tagVersionApplies(selection))
                menu.add(tagVersionAction);

            menu.add(new Separator());

            // copy
            if (copyApplies(selection))
                menu.add(copyAction);

            // paste
            if (pasteApplies(selection)) {
                pasteAction.setEnabled(isSomethingToPaste(selection));
                menu.add(pasteAction);
            }

            // delete
            if (deleteApplies(selection))
                menu.add(deleteAction);

            // rename
            if (renameApplies(selection))
                menu.add(renameAction);
        }

        // determine which menu items should appear according to selection
        menu.add(new Separator());
        menu.add(new Separator("ImportExport"));
        menu.appendToGroup("ImportExport", importMenu);
        importActionGroup.fillContextMenu(menu);

        if (elements.size() > 0) {
            if (exportApplies(selection)) {
                menu.appendToGroup("ImportExport", exportMenu);
                exportActionGroup.fillContextMenu(menu);
            }

            if (exportAsApplies(selection))
                menu.add(exportAsAction);

            menu.add(new Separator("MdwActions"));

            if (runMenuApplies(selection)) {
                menu.appendToGroup("MdwActions", runMenu);
                runActionGroup.fillContextMenu(menu);
            }

            // run
            if (runApplies(selection))
                menu.add(runAction);

            if (runFromPageApplies(selection))
                menu.add(pageRunAction);

            if (debugApplies(selection))
                menu.add(debugAction);

            // instances
            if (showInstancesApplies(selection))
                menu.add(instancesAction);

            // web launch
            if (webLaunchApplies(selection)) {
                menu.appendToGroup("MdwActions", webMenu);
                webActionGroup.fillContextMenu(menu);
            }

            // format
            if (formatMenuApplies(selection)) {
                menu.appendToGroup("MdwActions", formatMenu);
                formatActionGroup.fillContextMenu(menu);
            }

            // server
            if (serverActionApplies(selection)) {
                menu.appendToGroup("MdwActions", serverMenu);
                serverActionGroup.fillContextMenu(menu);
            }

            // swing tools
            if (swingLaunchApplies(selection)) {
                menu.appendToGroup("MdwActions", swingMenu);
                swingActionGroup.fillContextMenu(menu);
            }

            // search
            if (searchApplies(selection))
                menu.add(searchAction);

            // find callers
            if (findCallersApplies(selection))
                menu.add(findCallersAction);

            // show process hierarchy
            if (showProcessHierarchyApplies(selection))
                menu.add(showProcessHierarchyAction);

            // compare results
            if (compareResultsApplies(selection))
                menu.add(compareResultsAction);

            // open instance
            if (openInstanceApplies(selection))
                menu.add(openInstanceAction);

            if (myTasksApplies(selection))
                menu.add(myTasksAction);

            // unlock project
            if (unlockApplies(selection))
                menu.add(unlockAction);
        }
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        IToolBarManager toolbar = actionBars.getToolBarManager();
        toolbar.add(new GroupMarker("mdw.process.explorer.group"));
        toolbar.add(collapseAllAction);
        toolbar.add(sortToolbarAction);
        toolbar.add(filterToolbarAction);
        toolbar.add(refreshToolbarAction);
        toolbar.add(new Separator());
        toolbar.add(newPackageToolbarAction);
    }

    private ActionGroup createNewActionGroup() {
        newMenu = new MenuManager("New", null, MdwMenuManager.MDW_MENU_PREFIX + "menu.new");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                newMenu.removeAll();
                IStructuredSelection selection = getSelection();

                if (!newMenuApplies(selection))
                    return;

                if (!MdwPlugin.isRcp())
                    newMenu.add(newCloudProjectAction);

                if (createApplies(WorkflowProject.class, selection))
                    newMenu.add(newRemoteProjectAction);

                newMenu.add(new Separator());

                if (createApplies(WorkflowPackage.class, selection))
                    newMenu.add(newPackageAction);

                if (createApplies(WorkflowProcess.class, selection))
                    newMenu.add(newProcessAction);

                newMenu.add(new Separator());

                if (createApplies(Activity.class, selection)) {
                    MenuManager activityMenu = new MenuManager("Activity", null,
                            MdwMenuManager.MDW_MENU_PREFIX + "menu.new.activity");
                    activityMenu.add(newGeneralActivityAction);
                    activityMenu.add(newStartActivityAction);
                    activityMenu.add(newAdapterActivityAction);
                    activityMenu.add(newEvaluatorActivityAction);
                    newMenu.add(activityMenu);
                }

                if (createApplies(ExternalEvent.class, selection)) {
                    MenuManager eventHandlerMenu = new MenuManager("Event Handler", null,
                            MdwMenuManager.MDW_MENU_PREFIX + "menu.new.event.handler");
                    eventHandlerMenu.add(newEventHandlerAction);
                    eventHandlerMenu.add(newCamelProcessHandlerAction);
                    eventHandlerMenu.add(newCamelNotifyHandlerAction);
                    newMenu.add(eventHandlerMenu);
                }

                newMenu.add(new Separator());

                if (createApplies(WorkflowAsset.class, selection)) {
                    newMenu.add(new Separator());
                    newMenu.add(newCamelRouteAction);
                    newMenu.add(newJarFileAction);
                    newMenu.add(newJavaSourceAction);
                    newMenu.add(newJsonAction);
                    newMenu.add(newPageAction);
                    newMenu.add(newReportAction);
                    newMenu.add(newRuleAction);
                    newMenu.add(newScriptAction);
                    newMenu.add(newSpringConfigAction);
                    if (selection.getFirstElement() instanceof WorkflowElement
                            && ((WorkflowElement) selection.getFirstElement()).getProject()
                                    .isFilePersist())
                        newMenu.add(newTaskTemplateAction);
                    newMenu.add(newTemplateAction);
                    newMenu.add(newTestCaseAction);
                    newMenu.add(newTextResourceAction);
                    newMenu.add(newWebResourceAction);
                    newMenu.add(newWordDocAction);
                    newMenu.add(newXmlDocAction);
                    newMenu.add(newYamlAction);
                    newMenu.add(new Separator());
                }

                newMenu.add(new Separator("Other"));
                IWorkbenchAction otherAction = ActionFactory.NEW
                        .create(getViewSite().getWorkbenchWindow());
                otherAction.setId(MdwMenuManager.MDW_MENU_PREFIX + "new.other");
                otherAction.setText("Other...");
                newMenu.add(otherAction);
            }
        };
    }

    private ActionGroup createImportActionGroup() {
        importMenu = new MenuManager("Import", MdwPlugin.getImageDescriptor("icons/import.gif"),
                MdwMenuManager.MDW_MENU_PREFIX + "menu.import");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                importMenu.removeAll();
                IStructuredSelection selection = getSelection();
                WorkflowElement element = (WorkflowElement) selection.getFirstElement();
                if (!importMenuApplies(selection))
                    return;

                if (importProjectApplies(selection))
                    importMenu.add(importProjectAction);

                if (importPackageApplies(selection))
                    importMenu.add(importPackageAction);

                if (importVcsApplies(selection))
                    importMenu.add(importVcsAction);

                if (importProcessApplies(selection)) {
                    if (element instanceof WorkflowProcess) {
                        importProcessAction.setId(
                                MdwMenuManager.MDW_MENU_PREFIX + "import.new.process.version");
                        importProcessAction.setText("New Process Version...");
                    }
                    else {
                        importProcessAction
                                .setId(MdwMenuManager.MDW_MENU_PREFIX + "import.process");
                        importProcessAction.setText("Process...");
                    }
                    importMenu.add(importProcessAction);
                }

                if (importWorkflowAssetApplies(selection)) {
                    if (element instanceof WorkflowAsset) {
                        WorkflowAsset asset = (WorkflowAsset) element;
                        // menu item text and icon are dynamic
                        importWorkflowAssetAction
                                .setId(MdwMenuManager.MDW_MENU_PREFIX + "import.new.asset.version");
                        importWorkflowAssetAction
                                .setText("New " + asset.getTitle() + " Version...");
                        importWorkflowAssetAction.setImageDescriptor(
                                MdwPlugin.getImageDescriptor("icons/" + asset.getIcon()));
                    }
                    else {
                        importWorkflowAssetAction
                                .setId(MdwMenuManager.MDW_MENU_PREFIX + "import.asset");
                        importWorkflowAssetAction.setText("Asset...");
                        importWorkflowAssetAction
                                .setImageDescriptor(MdwPlugin.getImageDescriptor("icons/doc.gif"));
                    }
                    importMenu.add(importWorkflowAssetAction);
                }

                if (importAttributesApplies(selection)) {
                    List<IAction> importAttrsActions = getImportAttributeActions(selection);
                    if (!importAttrsActions.isEmpty()) {
                        MenuManager attributesMenu = new MenuManager("Attributes",
                                MdwPlugin.getImageDescriptor("icons/attribute.gif"),
                                MdwMenuManager.MDW_MENU_PREFIX + "menu.import.attributes");
                        attributesMenu.removeAll();
                        for (IAction action : importAttrsActions)
                            attributesMenu.add(action);
                        importMenu.add(attributesMenu);
                    }
                }

                if (importTaskTemplateApplies(selection))
                    importMenu.add(importTaskTemplateAction);

                importMenu.add(new Separator("Other"));
                IWorkbenchAction otherAction = ActionFactory.IMPORT
                        .create(getViewSite().getWorkbenchWindow());
                otherAction.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.other");
                otherAction.setText("Other...");
                importMenu.add(otherAction);
            }

        };
    }

    private ActionGroup createExportActionGroup() {
        exportMenu = new MenuManager("Export", MdwPlugin.getImageDescriptor("icons/export.gif"),
                MdwMenuManager.MDW_MENU_PREFIX + "menu.export");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                exportMenu.removeAll();
                IStructuredSelection selection = getSelection();

                if (exportPackageApplies(selection))
                    exportMenu.add(exportPackageAction);

                if (exportProcessApplies(selection))
                    exportMenu.add(exportProcessAction);

                if (exportWorkflowAssetApplies(selection)) {
                    WorkflowAsset asset = (WorkflowAsset) selection.getFirstElement();
                    // menu item text and icon are dynamic
                    exportWorkflowAssetAction
                            .setId(MdwMenuManager.MDW_MENU_PREFIX + "export.to.file");
                    exportWorkflowAssetAction.setText(asset.getTitle() + " to File...");
                    exportWorkflowAssetAction.setImageDescriptor(
                            MdwPlugin.getImageDescriptor("icons/" + asset.getIcon()));
                    exportMenu.add(exportWorkflowAssetAction);
                }

                if (exportAttributesApplies(selection)) {
                    List<IAction> exportAttrsActions = getExportAttributeActions(selection);
                    if (!exportAttrsActions.isEmpty()) {
                        MenuManager attributesMenu = new MenuManager("Attributes",
                                MdwPlugin.getImageDescriptor("icons/attribute.gif"),
                                MdwMenuManager.MDW_MENU_PREFIX + "menu.export.attributes");
                        attributesMenu.removeAll();
                        for (IAction action : exportAttrsActions)
                            attributesMenu.add(action);
                        exportMenu.add(attributesMenu);
                    }
                }

                if (exportTaskTemplatesApplies(selection))
                    exportMenu.add(exportTaskTemplateAction);

                exportMenu.add(new Separator("Other"));
                IWorkbenchAction otherAction = ActionFactory.EXPORT
                        .create(getViewSite().getWorkbenchWindow());
                otherAction.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.other");
                otherAction.setText("Other...");
                exportMenu.add(otherAction);
            }
        };
    }

    private ActionGroup createRunActionGroup() {
        runMenu = new MenuManager("Run", MdwPlugin.getImageDescriptor("icons/run.gif"),
                MdwMenuManager.MDW_MENU_PREFIX + "menu.run");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                runMenu.removeAll();
                IStructuredSelection selection = getSelection();

                if (runOnServerApplies(selection))
                    runMenu.add(runOnServerAction);
                if (runTestsApplies(selection))
                    runMenu.add(runTestsAction);
            }
        };
    }

    private ActionGroup createWebActionGroup() {
        webMenu = new MenuManager("Web", MdwPlugin.getImageDescriptor("icons/webtools.gif"),
                MdwMenuManager.MDW_MENU_PREFIX + "menu.web");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                webMenu.removeAll();
                IStructuredSelection selection = getSelection();
                if (webLaunchApplies(selection)) {
                    if (selection.getFirstElement() instanceof WorkflowElement) {
                        if (((WorkflowElement) selection.getFirstElement()).getProject()
                                .checkRequiredVersion(5, 5)) {
                            webMenu.add(mdwHubAction);
                        }
                        else {
                            webMenu.add(taskManagerAction);
                            webMenu.add(webToolsAction);
                        }
                    }
                }
            }
        };
    }

    private ActionGroup createFormatActionGroup() {
        formatMenu = new MenuManager("Format", null,
                MdwMenuManager.MDW_MENU_PREFIX + "menu.format");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                formatMenu.removeAll();
                IStructuredSelection selection = getSelection();

                WorkflowProject project = ((WorkflowElement) selection.getFirstElement())
                        .getProject();

                if (formatFunctionTestResultsApplies(selection))
                    formatMenu.add(formatFunctionTestResultsAction);
                java.io.File resultsFile = project.getFunctionTestResultsFile();
                formatFunctionTestResultsAction
                        .setEnabled(resultsFile != null && resultsFile.exists());
                if (formatLoadTestResultsApplies(selection))
                    formatMenu.add(formatLoadTestResultsAction);
                resultsFile = project.getLoadTestResultsFile();
                formatLoadTestResultsAction.setEnabled(resultsFile != null && resultsFile.exists());
            }
        };
    }

    private ActionGroup createServerActionGroup() {
        serverMenu = new MenuManager("Server", MdwPlugin.getImageDescriptor("icons/server.gif"),
                MdwMenuManager.MDW_MENU_PREFIX + "menu.server");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                serverMenu.removeAll();
                IStructuredSelection selection = getSelection();

                if (serverActionApplies(selection)) {
                    WorkflowProject project = (WorkflowProject) selection.getFirstElement();

                    if (project.isUserAuthorizedForSystemAdmin())
                        serverMenu.add(refreshCachesAction);
                    if (!project.isRemote() || project.isUserAuthorizedForSystemAdmin()) {
                        stubServerAction
                                .setChecked(project.getDesignerProxy().isStubServerRunning());
                        serverMenu.add(stubServerAction);
                        logWatcherAction.setChecked(DesignerProxy.isLogWatcherRunning());
                        serverMenu.add(logWatcherAction);
                    }

                    if (!MdwPlugin.isRcp()) {
                        if (!serverMenu.isEmpty())
                            serverMenu.add(new Separator("VM"));
                        serverMenu.add(visualVmAction);
                        serverMenu.add(jconsoleAction);
                    }
                }
            }
        };
    }

    private ActionGroup createSwingActionGroup() {
        swingMenu = new MenuManager("Swing Tools", null,
                MdwMenuManager.MDW_MENU_PREFIX + "menu.swing.tools");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                swingMenu.removeAll();
                IStructuredSelection selection = getSelection();

                if (swingLaunchApplies(selection)) {
                    WorkflowProject project = (WorkflowProject) selection.getFirstElement();
                    MdwSettings settings = MdwPlugin.getSettings();

                    if (settings.isSwingLaunchEventManager()
                            && project.isUserAuthorizedForSystemAdmin())
                        swingMenu.add(eventManagerAction);
                    if (settings.isSwingLaunchThreadPoolManager()
                            && project.isUserAuthorizedForSystemAdmin())
                        swingMenu.add(threadPoolManagerAction);
                }
            }
        };
    }

    private IAction createOpenAction() {
        IAction action = new Action() {
            public void run() {
                if (!openApplies(getSelection()))
                    return;

                for (Object item : getSelection().toList()) {
                    if (!actionHandler.open((WorkflowElement) item))
                        view.expand(item);
                }
            }
        };
        action.setActionDefinitionId("org.eclipse.jdt.ui.edit.text.java.open.editor");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "open");
        action.setText("Open");
        getActionBars().setGlobalActionHandler("org.eclipse.ui.navigator.Open", action);
        return action;
    }

    private IAction createPropertiesAction() {
        IAction action = new Action() {
            public void run() {
                actionHandler.showPropertiesView();
                view.setFocus();
            }
        };
        ImageDescriptor propsImageDesc = MdwPlugin.getImageDescriptor("icons/properties.gif");
        action.setImageDescriptor(propsImageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "show.properties");
        action.setText("Show Properties");

        return action;
    }

    private IAction createIncrementVersionAction() {
        IAction action = new Action() {
            public void run() {
                if (incrementVersionApplies(getSelection()))
                    actionHandler
                            .incrementVersion((WorkflowElement) getSelection().getFirstElement());
                view.setFocus();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "increment.version");
        action.setText("Increment Version");

        return action;
    }

    private IAction createSetVersionAction() {
        IAction action = new Action() {
            public void run() {
                if (setVersionApplies(getSelection()))
                    actionHandler.setVersion((WorkflowElement) getSelection().getFirstElement());
                view.setFocus();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "set.version");
        action.setText("Set Version");

        return action;
    }

    private IAction createTagVersionAction() {
        IAction action = new Action() {
            public void run() {
                if (tagVersionApplies(getSelection()))
                    actionHandler.tagVersion(selectionToElementArray(getSelection()));
                view.setFocus();
            }
        };
        ImageDescriptor propsImageDesc = MdwPlugin.getImageDescriptor("icons/package.gif");
        action.setImageDescriptor(propsImageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "tag.version");
        action.setText("Tag Version...");

        return action;
    }

    private IAction createCopyAction() {
        IAction action = new Action() {
            public void run() {
                if (!copyApplies(getSelection()))
                    return;

                view.setFocus();

                Object[] items = getSelection().toArray();
                WorkflowElement[] elements = new WorkflowElement[items.length];
                for (int i = 0; i < items.length; i++) {
                    elements[i] = (WorkflowElement) items[i];
                }
                actionHandler.copy(elements, view.getClipboard());
            }
        };
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        action.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        action.setActionDefinitionId("org.eclipse.ui.edit.copy");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "copy");
        action.setText("Copy");
        getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), action);
        return action;
    }

    private IAction createPasteAction() {
        IAction action = new Action() {
            public void run() {
                if (!pasteApplies(getSelection()))
                    return;

                view.setFocus();

                actionHandler.paste(view.getClipboard(), view.getDropTarget(),
                        (WorkflowElement) getSelection().getFirstElement());
            }
        };
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        action.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        action.setActionDefinitionId("org.eclipse.ui.edit.paste");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "paste");
        action.setText("Paste");
        getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), action);
        return action;
    }

    private IAction createDeleteAction() {

        IAction action = new Action() {
            public void run() {
                if (!deleteApplies(getSelection()))
                    return;

                view.setFocus();

                Object[] items = getSelection().toArray();
                WorkflowElement[] elements = new WorkflowElement[items.length];
                boolean includesArchived = false;
                for (int i = 0; i < items.length; i++) {
                    elements[i] = (WorkflowElement) items[i];
                    if (elements[i].isArchived())
                        includesArchived = true;
                }

                actionHandler.delete(elements);
                view.select(null);
                getContext().setSelection(new StructuredSelection());

                if (includesArchived) // TODO archived elements are not being
                                      // listened to
                    view.refreshItem(elements[0].getProject());
            }
        };
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        action.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        action.setActionDefinitionId("org.eclipse.ui.edit.delete");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "delete");
        action.setText("Delete");
        getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), action);
        return action;
    }

    private IAction createRenameAction() {
        IAction action = new Action() {
            public void run() {
                if (!renameApplies(getSelection()))
                    return;

                view.setFocus();

                actionHandler.rename((WorkflowElement) getSelection().getFirstElement());
            }
        };
        action.setActionDefinitionId("org.eclipse.ui.edit.rename");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "rename");
        action.setText("Rename...");
        getActionBars().setGlobalActionHandler(ActionFactory.RENAME.getId(), action);
        return action;
    }

    private IAction createImportProjectAction() {
        IAction action = new Action() {
            public void run() {
                if (!importProjectApplies(getSelection()))
                    return;
                actionHandler.imporT(WorkflowProject.class,
                        (WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/remote_project.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.projecs");
        action.setText("Project(s)...");
        return action;
    }

    private IAction createImportPackageAction() {
        IAction action = new Action() {
            public void run() {
                if (!importPackageApplies(getSelection()))
                    return;

                WorkflowElement selection = (WorkflowElement) getSelection().getFirstElement();
                WorkflowProject workflowProject = selection.getProject();
                boolean authorized = (workflowProject.isFilePersist()
                        && !workflowProject.isRemote())
                        || workflowProject.getDesignerDataModel()
                                .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN);
                if (!authorized) {
                    MessageDialog.openError(getViewSite().getShell(), "Package Import",
                            "You are not authorized to import into project: "
                                    + workflowProject.getName());
                    return;
                }
                actionHandler.imporT(WorkflowPackage.class, selection);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/package.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.packages");
        action.setText("Package(s)...");
        return action;
    }

    private IAction createImportVcsAction() {
        IAction action = new Action() {
            public void run() {
                if (!importVcsApplies(getSelection()))
                    return;

                WorkflowElement selection = (WorkflowElement) getSelection().getFirstElement();
                WorkflowProject workflowProject = selection.getProject();
                boolean authorized = workflowProject.getDesignerDataModel()
                        .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN);
                if (!authorized) {
                    MessageDialog.openError(getViewSite().getShell(), "Package Import",
                            "You are not authorized to import into project: "
                                    + workflowProject.getName());
                    return;
                }
                actionHandler.remoteImportVcs(selection.getProject());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/gitrepo.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "assets.from.vcs");
        action.setText("Assets from VCS...");
        return action;
    }

    private IAction createImportProcessAction() {
        IAction action = new Action() {
            public void run() {
                if (!importProcessApplies(getSelection()))
                    return;
                actionHandler.imporT(WorkflowProcess.class,
                        (WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.process");
        action.setText("Process...");
        return action;
    }

    private IAction createImportTaskTemplateAction() {
        IAction action = new Action() {
            public void run() {
                if (!importTaskTemplateApplies(getSelection()))
                    return;
                WorkflowElement selection = (WorkflowElement) getSelection().getFirstElement();
                WorkflowProject workflowProject = selection.getProject();
                if (!workflowProject.isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_DESIGN)) {
                    MessageDialog.openError(getViewSite().getShell(), "Task Template Import",
                            "You are not authorized to import into project: "
                                    + workflowProject.getName());
                    return;
                }
                actionHandler.imporT(TaskTemplate.class, selection);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/task.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.tasks");
        action.setText("Task Templates...");
        return action;
    }

    private IAction createImportWorkflowAssetAction() {
        IAction action = new Action() {
            public void run() {
                if (!importWorkflowAssetApplies(getSelection()))
                    return;
                actionHandler.imporT(WorkflowAsset.class,
                        (WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/doc.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "new.asset.version");
        action.setText("New Version...");
        return action;
    }

    private IAction createExportPackageAction() {
        IAction action = new Action() {
            public void run() {
                if (!exportPackageApplies(getSelection()))
                    return;

                view.setFocus();

                actionHandler.export(WorkflowPackage.class, getSelection());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/package.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.packages");
        action.setText("Package(s)...");
        return action;
    }

    private List<IAction> getExportAttributeActions(IStructuredSelection selection) {
        List<IAction> exportAttributesActions = new ArrayList<IAction>();
        if (selection != null && selection.getFirstElement() instanceof WorkflowElement) {
            // bam
            IAction action = new Action() {
                public void run() {
                    if (!exportAttributesApplies(getSelection()))
                        return;
                    view.setFocus();
                    actionHandler.exportAttributes(WorkAttributeConstant.BAM_ATTR_PREFIX,
                            (WorkflowElement) getSelection().getFirstElement());
                }
            };
            action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.monitoring");
            action.setText("Monitoring...");
            exportAttributesActions.add(action);

            // simulation
            action = new Action() {
                public void run() {
                    if (!exportAttributesApplies(getSelection()))
                        return;
                    view.setFocus();
                    actionHandler.exportAttributes(WorkAttributeConstant.SIMULATION_ATTR_PREFIX,
                            (WorkflowElement) getSelection().getFirstElement());
                }
            };
            action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.simulation");
            action.setText("Simulation...");
            exportAttributesActions.add(action);

            // pagelet-driven attributes
            WorkflowProject project = ((WorkflowElement) selection.getFirstElement()).getProject();
            List<PageletTab> pageletTabs = project.getPageletTabs();
            if (pageletTabs != null) {
                for (PageletTab pageletTab : pageletTabs) {
                    final String prefix = pageletTab.getOverrideAttributePrefix();
                    action = new Action() {
                        public void run() {
                            if (!exportAttributesApplies(getSelection()))
                                return;
                            view.setFocus();
                            actionHandler.exportAttributes(prefix,
                                    (WorkflowElement) getSelection().getFirstElement());
                        }
                    };
                    action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.attributes." + prefix);
                    action.setText(pageletTab.getLabel() + "...");
                    exportAttributesActions.add(action);
                }
            }
        }

        return exportAttributesActions;
    }

    private List<IAction> getImportAttributeActions(IStructuredSelection selection) {
        List<IAction> importAttributesActions = new ArrayList<IAction>();

        if (selection != null && selection.getFirstElement() instanceof WorkflowElement) {

            // bam
            IAction action = new Action() {
                public void run() {
                    if (!importAttributesApplies(getSelection()))
                        return;
                    view.setFocus();
                    actionHandler.importAttributes(WorkAttributeConstant.BAM_ATTR_PREFIX,
                            (WorkflowElement) getSelection().getFirstElement());
                }
            };
            action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.monitoring");
            action.setText("Monitoring...");
            importAttributesActions.add(action);

            // simulation
            action = new Action() {
                public void run() {
                    if (!importAttributesApplies(getSelection()))
                        return;
                    view.setFocus();
                    actionHandler.importAttributes(WorkAttributeConstant.SIMULATION_ATTR_PREFIX,
                            (WorkflowElement) getSelection().getFirstElement());
                }
            };
            action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.simulation");
            action.setText("Simulation...");
            importAttributesActions.add(action);

            // pagelet-driven attributes
            WorkflowProject project = ((WorkflowElement) selection.getFirstElement()).getProject();
            List<PageletTab> pageletTabs = project.getPageletTabs();
            if (pageletTabs != null) {
                for (PageletTab pageletTab : pageletTabs) {
                    final String prefix = pageletTab.getOverrideAttributePrefix();
                    action = new Action() {
                        public void run() {
                            if (!importAttributesApplies(getSelection()))
                                return;
                            view.setFocus();
                            actionHandler.importAttributes(prefix,
                                    (WorkflowElement) getSelection().getFirstElement());
                        }
                    };
                    action.setId(MdwMenuManager.MDW_MENU_PREFIX + "import.attributes." + prefix);
                    action.setText(pageletTab.getLabel() + "...");
                    importAttributesActions.add(action);
                }
            }
        }

        return importAttributesActions;
    }

    private IAction createExportProcessAction() {
        IAction action = new Action() {
            public void run() {
                if (!exportProcessApplies(getSelection()))
                    return;

                view.setFocus();

                actionHandler.export(WorkflowProcess.class, getSelection());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.process");
        action.setText("Process...");
        return action;
    }

    private IAction createExportTaskTemplateAction() {
        IAction action = new Action() {
            public void run() {
                if (!exportTaskTemplatesApplies(getSelection()))
                    return;

                view.setFocus();

                actionHandler.export(TaskTemplate.class, getSelection());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/task.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.tasks");
        action.setText("Task Templates...");
        return action;
    }

    private IAction createSwingLaunchAction(final String title, final String icon,
            final String formName, final String dataInit) {
        IAction action = new Action() {
            public void run() {
                if (!swingLaunchApplies(getSelection()))
                    return;

                WorkflowProject workflowProject = (WorkflowProject) getSelection()
                        .getFirstElement();
                boolean is55 = workflowProject.checkRequiredVersion(5, 5);
                String handlerPkg = is55 ? "com.centurylink.mdw.listener.formaction."
                        : "com.qwest.mdw.listener.formaction.";
                String listenerPackage = is55 ? "com.centurylink.mdw.swingtools/" : "";
                actionHandler.launchSwing(workflowProject, title,
                        formName == null ? null : listenerPackage + formName,
                        handlerPkg + dataInit);
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "swing." + title);
        action.setText(title);
        if (icon != null) {
            ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/" + icon);
            action.setImageDescriptor(imageDesc);
        }
        return action;
    }

    private IAction createExportWorkflowAssetAction() {
        IAction action = new Action() {
            public void run() {
                if (!exportWorkflowAssetApplies(getSelection()))
                    return;
                actionHandler.export(WorkflowAsset.class, getSelection());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/doc.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.asset.to.file");
        action.setText("To File...");
        return action;
    }

    private IAction createExportAsAction() {
        IAction action = new Action() {
            public void run() {
                if (!exportAsApplies(getSelection()))
                    return;

                view.setFocus();

                Object element = getSelection().getFirstElement();
                actionHandler.exportAs((WorkflowElement) element);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/export_as.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "export.as");
        action.setText("Export As...");
        return action;
    }

    private IAction createRunAction() {
        IAction action = new Action() {
            public void run() {
                if (!runApplies(getSelection()))
                    return;

                view.setFocus();

                Object element = getSelection().getFirstElement();
                if (element instanceof AutomatedTestSuite || element instanceof AutomatedTestCase)
                    actionHandler.test(getSelection());
                else
                    actionHandler.run((WorkflowElement) element);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/run.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "run");
        action.setText("Run...");
        return action;
    }

    private IAction createRunOnServerAction() {
        IAction action = new Action() {
            public void run() {
                if (!runOnServerApplies(getSelection()))
                    return;
                view.setFocus();
                actionHandler.run((WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/run_on_server.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "run.on.server");
        action.setText("Run on Server");
        return action;
    }

    private IAction createRunTestsAction() {
        IAction action = new Action() {
            public void run() {
                if (!runTestsApplies(getSelection()))
                    return;
                view.setFocus();
                actionHandler.test(getSelection());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/testrun.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "run.tests");
        action.setText("Run Tests...");
        return action;
    }

    private IAction createRunFromStartPageAction() {
        IAction action = new Action() {
            public void run() {
                if (!runFromPageApplies(getSelection()))
                    return;

                view.setFocus();

                Object element = getSelection().getFirstElement();
                if (element instanceof AutomatedTestSuite || element instanceof AutomatedTestCase)
                    actionHandler.test(getSelection());
                else if (element instanceof File
                        && ((File) element).getParent() instanceof AutomatedTestCase
                        && TestCase.LEGACY_TEST_CASE_FILENAMES.values()
                                .contains(((File) element).getName()))
                    actionHandler.test(getSelection());
                else
                    actionHandler.runFromPage((WorkflowElement) element);
            }
        };

        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/run.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "run.start.page");
        action.setText("Run Start Page...");
        return action;
    }

    private IAction createDebugAction() {
        IAction action = new Action() {
            public void run() {
                if (!debugApplies(getSelection()))
                    return;

                view.setFocus();

                Object element = getSelection().getFirstElement();
                if (element instanceof AutomatedTestSuite || element instanceof AutomatedTestCase)
                    actionHandler.debugTest(getSelection());
                else
                    actionHandler.debug((WorkflowElement) element);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/debug.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "debug");
        action.setText("Debug...");
        return action;
    }

    private IAction createInstancesAction() {
        IAction action = new Action() {
            public void run() {
                if (!showInstancesApplies(getSelection()))
                    return;
                view.setFocus();
                actionHandler.showInstances((WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/list.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "view.instances");
        action.setText("View Instances");
        return action;
    }

    private IAction createMyTasksAction() {
        IAction action = new Action() {
            public void run() {
                if (!myTasksApplies(getSelection()))
                    return;
                view.setFocus();
                actionHandler.showInstances((WorkflowElement) getSelection().getFirstElement());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/list.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "my.tasks");
        action.setText("My Tasks");
        return action;
    }

    private IAction createCollapseAllAction() {
        IAction action = new Action() {
            public void run() {
                view.handleCollapseAll();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "collapse.all");
        action.setText("Collapse All");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/collapseall.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createRefreshAction(final boolean viewRefresh) {
        IAction action = new Action() {
            public void run() {
                if (viewRefresh)
                    view.handleRefresh();
                else if (refreshApplies(getSelection()))
                    actionHandler.refresh((WorkflowElement) getSelection().getFirstElement());
            }
        };
        action.setText(viewRefresh ? "Refresh View" : "Refresh");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + (viewRefresh ? "view.refresh" : "refresh"));
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/refresh.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createRefreshCachesAction() {
        IAction action = new Action() {
            public void run() {
                actionHandler.serverAction((WorkflowElement) getSelection().getFirstElement(),
                        WorkflowElementActionHandler.REFRESH_CACHES);
            }
        };
        action.setText("Refresh Caches");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "refresh.caches");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/refr_cache.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createUpdateAction() {
        IAction action = new Action() {
            public void run() {
                if (updateApplies(getSelection())) {
                    WorkflowElement workflowElement = (WorkflowElement) getSelection()
                            .getFirstElement();
                    actionHandler.update(workflowElement);
                }
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "update.to.latest");
        action.setText("Update to Latest");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/sync.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createStubServerAction() {
        IAction action = new Action("Stub Server", Action.AS_CHECK_BOX) {
            public void run() {
                WorkflowProject project = (WorkflowProject) getSelection().getFirstElement();
                actionHandler.serverAction(project, WorkflowElementActionHandler.STUB_SERVER);
                project.fireElementChangeEvent(ChangeType.STATUS_CHANGE,
                        project.getDesignerProxy().isStubServerRunning());
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/stubserv.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createLogWatcherAction() {
        IAction action = new Action("Log Watcher", Action.AS_CHECK_BOX) {
            public void run() {
                WorkflowProject project = (WorkflowProject) getSelection().getFirstElement();
                actionHandler.serverAction(project, WorkflowElementActionHandler.LOG_WATCHER);
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/logwatch.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createSortToolbarAction() {
        IAction action = new Action() {
            public void run() {
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(PreferenceConstants.PREFS_SORT_PACKAGE_CONTENTS_A_TO_Z,
                        isChecked());
                view.handleApply();
            }
        };

        action.setText("Sort Package Contents");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "sort.package.contents");
        ImageDescriptor sortImageDesc = MdwPlugin.getImageDescriptor("icons/sort.gif");
        action.setImageDescriptor(sortImageDesc);
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        action.setChecked(
                prefsStore.getBoolean(PreferenceConstants.PREFS_SORT_PACKAGE_CONTENTS_A_TO_Z));
        return action;
    }

    private IAction createFilterToolbarAction() {
        IAction action = new Action() {
            public void run() {
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                List<String> options = new ArrayList<String>();
                options.add(PreferenceConstants.PREFS_FILTER_PROCESSES_IN_PEX);
                options.add(PreferenceConstants.PREFS_FILTER_WORKFLOW_ASSETS_IN_PEX);
                options.add(PreferenceConstants.PREFS_FILTER_EVENT_HANDLERS_IN_PEX);
                options.add(PreferenceConstants.PREFS_SHOW_ACTIVITY_IMPLEMENTORS_IN_PEX);
                options.add(PreferenceConstants.PREFS_FILTER_TASK_TEMPLATES_IN_PEX);
                options.add(PreferenceConstants.PREFS_FILTER_ARCHIVED_ITEMS_IN_PEX);

                List<String> selected = new ArrayList<String>();
                for (String option : options) {
                    boolean show = !prefsStore.getBoolean(option);
                    if (option.startsWith("Show"))
                        show = !show; // means default is filtered
                    if (show)
                        selected.add(option);
                }
                ListSelectionDialog lsd = new ListSelectionDialog(view.getSite().getShell(),
                        options, new IStructuredContentProvider() {
                            public Object[] getElements(Object inputElement) {
                                return ((List<?>) inputElement).toArray(new String[0]);
                            }

                            public void inputChanged(Viewer viewer, Object oldInput,
                                    Object newInput) {
                            }

                            public void dispose() {
                            }
                        }, new ILabelProvider() {
                            public void addListener(ILabelProviderListener listener) {
                            }

                            public void removeListener(ILabelProviderListener listener) {
                            }

                            public boolean isLabelProperty(Object element, String property) {
                                return false;
                            }

                            public void dispose() {
                            }

                            public Image getImage(Object element) {
                                return null;
                            }

                            public String getText(Object element) {
                                if (PreferenceConstants.PREFS_FILTER_PROCESSES_IN_PEX
                                        .equals(element))
                                    return "Processes";
                                else if (PreferenceConstants.PREFS_FILTER_WORKFLOW_ASSETS_IN_PEX
                                        .equals(element))
                                    return "Assets";
                                else if (PreferenceConstants.PREFS_FILTER_EVENT_HANDLERS_IN_PEX
                                        .equals(element))
                                    return "Event Handlers";
                                else if (PreferenceConstants.PREFS_SHOW_ACTIVITY_IMPLEMENTORS_IN_PEX
                                        .equals(element))
                                    return "Activity Implementors";
                                else if (PreferenceConstants.PREFS_FILTER_TASK_TEMPLATES_IN_PEX
                                        .equals(element))
                                    return "Task Templates";
                                else if (PreferenceConstants.PREFS_FILTER_ARCHIVED_ITEMS_IN_PEX
                                        .equals(element))
                                    return "Archived Items";
                                else
                                    return null;
                            }
                        }, "Workflow elements to display in Process Explorer");
                lsd.setTitle("Show Package Contents");
                lsd.setInitialSelections(selected.toArray(new String[0]));
                int res = lsd.open();
                if (res != Dialog.CANCEL) {
                    Object[] results = (Object[]) lsd.getResult();
                    for (String option : options) {
                        boolean show = false;
                        for (Object result : results) {
                            if (option.equals(result))
                                show = true;
                        }
                        if (option.startsWith("Filter"))
                            show = !show; // default is to show
                        prefsStore.setValue(option, show);
                    }
                    view.handleApply();
                }
            }
        };

        action.setText("Filter Package Contents");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "filter.package.contents");
        ImageDescriptor sortImageDesc = MdwPlugin.getImageDescriptor("icons/filter.gif");
        action.setImageDescriptor(sortImageDesc);
        return action;
    }

    private IAction createNewAction(final Class<? extends WorkflowElement> elementClass,
            String label, String icon) {
        IAction action = new Action() {
            public void run() {
                if (!createApplies(elementClass, getSelection()))
                    return;
                actionHandler.create(elementClass,
                        (WorkflowElement) getSelection().getFirstElement());
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "new." + label);
        action.setText(label);
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/" + icon);
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createSearchAction() {
        IAction action = new Action() {
            public void run() {
                if (!searchApplies(getSelection()))
                    return;

                Object[] items = getSelection().toArray();
                WorkflowElement[] elements = new WorkflowElement[items.length];
                for (int i = 0; i < items.length; i++)
                    elements[i] = (WorkflowElement) items[i];

                actionHandler.search(elements);
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "search");
        action.setText("Search");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/search.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createFindCallersAction() {
        IAction action = new Action() {
            public void run() {
                if (!findCallersApplies(getSelection()))
                    return;

                actionHandler.findCallers((WorkflowElement) getSelection().getFirstElement());
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "find.calling.processes");
        action.setText("Find Calling Processes");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createShowProcessHierarchyAction() {
        IAction action = new Action() {
            public void run() {
                if (!showProcessHierarchyApplies(getSelection()))
                    return;

                actionHandler.showHierarchy((WorkflowElement) getSelection().getFirstElement());
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "show.process.hierarchy");
        action.setText("Show Process Hierarchy");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/hierarchy.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createCompareResultsAction() {
        IAction action = new Action() {
            @SuppressWarnings("restriction")
            public void run() {
                if (compareResultsApplies(getSelection())) {
                    WorkflowProject project = ((WorkflowElement) getSelection().getFirstElement())
                            .getProject();
                    try {
                        project.getProjectFolder(
                                project.getTestResultsPath(AutomatedTestCase.FUNCTION_TEST))
                                .refreshLocal(IResource.DEPTH_INFINITE, null);
                        Object[] items = new Object[2];
                        if (getSelection().getFirstElement() instanceof AutomatedTestResults) {
                            AutomatedTestResults expectedResults = (AutomatedTestResults) getSelection()
                                    .getFirstElement();
                            if (project.isFilePersist()) {
                                items[0] = project
                                        .getProjectFile(expectedResults.getVcsAssetPath());
                            }
                            else {
                                expectedResults.openTempFile(new NullProgressMonitor());
                                items[0] = expectedResults
                                        .getTempFile(expectedResults.getTempFolder());
                            }
                            String actualResultsPath = expectedResults.getPackage().getName() + "/"
                                    + expectedResults.getName();
                            items[1] = project.getProjectFile(
                                    project.getTestResultsPath(AutomatedTestCase.FUNCTION_TEST)
                                            + "/" + actualResultsPath);
                        }
                        else if (getSelection()
                                .getFirstElement() instanceof LegacyExpectedResults) {
                            LegacyExpectedResults expectedResult = (LegacyExpectedResults) getSelection()
                                    .getFirstElement();
                            items[0] = expectedResult.getExpectedResult();
                            items[1] = expectedResult.getActualResult();
                        }
                        if (items[1] == null || !((IFile) items[1]).exists()) {
                            MessageDialog.openWarning(view.getSite().getShell(), "No Results",
                                    "Unable to locate results file: "
                                            + ((IFile) items[1]).getLocation().toString());
                            return;
                        }
                        StructuredSelection compareSelection = new StructuredSelection(items);
                        ResultsCompareAction compareAction = new ResultsCompareAction(
                                compareSelection);
                        compareAction.run(compareSelection);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "Compare Test Results", project);
                    }
                }
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "compare.results");
        action.setText("Compare Results");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/compare.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createOpenInstanceAction() {
        IAction action = new Action() {
            public void run() {
                if (openInstanceApplies(getSelection())) {
                    WorkflowProject project = ((WorkflowElement) getSelection().getFirstElement())
                            .getProject();
                    try {
                        Long procInstId = null;
                        if (getSelection().getFirstElement() instanceof AutomatedTestResults) {
                            AutomatedTestResults expectedResults = (AutomatedTestResults) getSelection()
                                    .getFirstElement();
                            procInstId = expectedResults.getActualProcessInstanceId();
                        }
                        else if (getSelection()
                                .getFirstElement() instanceof LegacyExpectedResults) {
                            LegacyExpectedResults expectedResult = (LegacyExpectedResults) getSelection()
                                    .getFirstElement();
                            java.io.File resultsFile = expectedResult.getActualResultFile();
                            TestFile testFile = new TestFile(null, resultsFile.getPath());
                            testFile.load();
                            TestFileLine line1 = testFile.getLines().get(0);
                            procInstId = new Long(line1.getWord(3));
                        }

                        if (procInstId == null) {
                            MessageDialog.openWarning(view.getSite().getShell(), "No Results",
                                    "Unable to locate results file.");
                            return;
                        }
                        ProcessInstanceVO procInst = project.getDataAccess()
                                .getProcessInstance(procInstId);
                        Long processId = procInst.getProcessId();
                        ProcessVO procVO = project.getProcess(processId).getProcessVO();
                        if (procVO == null)
                            PluginMessages.uiError("Unable to locate process: " + processId,
                                    "Open Process Instance", project);
                        WorkflowProcess instance = new WorkflowProcess(project, procVO);
                        instance.setProcessInstance(procInst);
                        actionHandler.open(instance);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "Open Process Instance", project);
                    }
                }
            }
        };
        action.setText("Open Process Instance");
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "open.process.instance");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createFormatFunctionTestResultsAction() {
        IAction action = new Action() {
            public void run() {
                if (formatFunctionTestResultsApplies(getSelection())) {
                    WorkflowElement element = (WorkflowElement) getSelection().getFirstElement();
                    actionHandler.formatResults(element.getProject(),
                            AutomatedTestCase.FUNCTION_TEST);
                }
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/auto_test.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "format.function.test.results");
        action.setText("Function Tests Results");
        return action;
    }

    private IAction createFormatLoadTestResultsAction() {
        IAction action = new Action() {
            public void run() {
                if (formatLoadTestResultsApplies(getSelection())) {
                    WorkflowElement element = (WorkflowElement) getSelection().getFirstElement();
                    actionHandler.formatResults(element.getProject(), AutomatedTestCase.LOAD_TEST);
                }
            }
        };
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/load_test.gif");
        action.setImageDescriptor(imageDesc);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "format.load.test.results");
        action.setText("Load Test Results");
        return action;
    }

    private IAction createTaskManagerAction() {
        IAction action = new Action() {
            public void run() {
                if (webLaunchApplies(getSelection())) {
                    if (getSelection().getFirstElement() instanceof WorkflowProject) {
                        WorkflowProject project = (WorkflowProject) getSelection()
                                .getFirstElement();
                        WebLaunchActions.getLaunchAction(project, WebApp.TaskManager)
                                .launch(project);
                    }
                    else if (getSelection().getFirstElement() instanceof WorkflowPackage) {
                        WorkflowPackage packageVersion = (WorkflowPackage) getSelection()
                                .getFirstElement();
                        WebLaunchActions
                                .getLaunchAction(packageVersion.getProject(), WebApp.TaskManager)
                                .launch(packageVersion);
                    }
                }
            }
        };
        WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(null, WebApp.TaskManager);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "launch.task.manager");
        action.setText(launchAction.getLabel());
        action.setImageDescriptor(launchAction.getImageDescriptor());
        return action;
    }

    private IAction createMdwHubAction() {
        IAction action = new Action() {
            public void run() {
                if (webLaunchApplies(getSelection())) {
                    if (getSelection().getFirstElement() instanceof WorkflowProject) {
                        WorkflowProject project = (WorkflowProject) getSelection()
                                .getFirstElement();
                        WebLaunchActions.getLaunchAction(project, WebApp.MdwHub).launch(project);
                    }
                    else if (getSelection().getFirstElement() instanceof WorkflowPackage) {
                        WorkflowPackage packageVersion = (WorkflowPackage) getSelection()
                                .getFirstElement();
                        WebLaunchActions.getLaunchAction(packageVersion.getProject(), WebApp.MdwHub)
                                .launch(packageVersion);
                    }
                }
            }
        };
        WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(null, WebApp.MdwHub);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "launch.hub");
        action.setText(launchAction.getLabel());
        action.setImageDescriptor(launchAction.getImageDescriptor());
        return action;
    }

    private IAction createWebToolsAction() {
        IAction action = new Action() {
            public void run() {
                if (webLaunchApplies(getSelection())) {
                    if (getSelection().getFirstElement() instanceof WorkflowProject) {
                        WorkflowProject project = (WorkflowProject) getSelection()
                                .getFirstElement();
                        WebLaunchActions.getLaunchAction(project, WebApp.WebTools).launch(project);
                    }
                    else if (getSelection().getFirstElement() instanceof WorkflowPackage) {
                        WorkflowPackage pkg = (WorkflowPackage) getSelection().getFirstElement();
                        WebLaunchActions.getLaunchAction(pkg.getProject(), WebApp.WebTools)
                                .launch(pkg);
                    }
                }
            }
        };
        WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(null, WebApp.WebTools);
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "launch.web.tools");
        action.setText(launchAction.getLabel());
        action.setImageDescriptor(launchAction.getImageDescriptor());
        return action;
    }

    private IAction createUnlockAction() {
        IAction action = new Action() {
            public void run() {
                if (unlockApplies(getSelection())) {
                    WorkflowProject project = (WorkflowProject) getSelection().getFirstElement();
                    WorkflowProjectManager.getInstance().makeLocal(project);
                    project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE,
                            project.getMdwVcsRepository());
                    MessageDialog.openInformation(getViewSite().getShell(),
                            "Remote Project Unlocked", project.getName()
                                    + " has been unlocked.  Please close any open assets and refresh.");
                }
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "unlock");
        action.setText("Unlock");

        return action;
    }

    private IViewSite getViewSite() {
        return view.getViewSite();
    }

    private IActionBars getActionBars() {
        return getViewSite().getActionBars();
    }

    private IStructuredSelection getSelection() {
        if (getContext() == null
                || WorkflowProjectManager.getInstance().getWorkflowProjects().isEmpty())
            return new StructuredSelection();

        return (IStructuredSelection) getContext().getSelection();
    }

    public boolean showPropertiesApplies(IStructuredSelection selection) {
        if (selection.size() != 1)
            return false;

        Object element = selection.getFirstElement();
        if (!(element instanceof WorkflowElement))
            return false;
        if (element instanceof AutomatedTestCase)
            return !((AutomatedTestCase) element).isLegacy();
        if (element instanceof WorkflowProject || element instanceof WorkflowPackage
                || element instanceof WorkflowProcess || element instanceof ExternalEvent
                || element instanceof WorkflowAsset || element instanceof AutomatedTestCase) {
            return true;
        }

        return false;
    }

    public boolean incrementVersionApplies(IStructuredSelection selection) {
        if (selection.size() != 1)
            return false;
        if (!(selection.getFirstElement() instanceof WorkflowPackage))
            return false;

        WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
        if (packageVersion.isDefaultPackage() || packageVersion.isArchived()
                || !packageVersion.isUserAuthorized(UserRoleVO.ASSET_DESIGN))
            return false;

        return true;
    }

    public boolean setVersionApplies(IStructuredSelection selection) {
        if (selection.size() != 1)
            return false;
        if (!(selection.getFirstElement() instanceof WorkflowPackage))
            return false;

        WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
        if (packageVersion.isDefaultPackage() || packageVersion.isArchived()
                || !packageVersion.isUserAuthorized(UserRoleVO.ASSET_DESIGN))
            return false;

        return true;
    }

    public boolean tagVersionApplies(IStructuredSelection selection) {
        boolean allowArchived = MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES);

        for (Object item : selection.toList()) {
            if (!(item instanceof WorkflowPackage))
                return false;
            WorkflowPackage pkg = (WorkflowPackage) item;
            if (pkg.isDefaultPackage() || (pkg.isArchived() && !allowArchived)) {
                return false;
            }
            if (!pkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                return false;
            }
        }
        return true;
    }

    public boolean copyApplies(IStructuredSelection selection) {
        for (Object item : selection.toList()) {
            WorkflowElement workflowElement = (WorkflowElement) item;
            if (!(workflowElement instanceof WorkflowProcess)
                    && !(workflowElement instanceof ExternalEvent)
                    && !(workflowElement instanceof WorkflowAsset))
                return false;
            if (workflowElement instanceof AutomatedTestCase)
                return !((AutomatedTestCase) workflowElement).isLegacy();
        }
        return true;
    }

    public boolean pasteApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowElement) {
                WorkflowElement element = (WorkflowElement) selection.getFirstElement();
                WorkflowPackage pkg = element.getPackage();
                if (pkg != null) {
                    if (!pkg.isArchived() && pkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN))
                        return true;
                }
            }

        }

        return false;
    }

    public boolean isSomethingToPaste(IStructuredSelection selection) {
        if (selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowElement element = (WorkflowElement) selection.getFirstElement();
            WorkflowPackage destPackage = element.getPackage();

            if (destPackage != null) {
                Object clipboardContents = view.getClipboard()
                        .getContents(TextTransfer.getInstance());
                if (clipboardContents instanceof String) {
                    String clipString = (String) clipboardContents;
                    if (clipString != null && (clipString.startsWith("Process~")
                            || clipString.startsWith("ExternalEvent~")
                            || clipString.startsWith("WorkflowAsset~"))) {
                        ProcessExplorerDropTarget dropTarget = view.getDropTarget();
                        return (dropTarget != null
                                && dropTarget.isValidDrop(clipString, destPackage, DND.DROP_COPY));
                    }
                }
            }
        }
        return false;
    }

    public boolean deleteApplies(IStructuredSelection selection) {
        boolean deleteArchived = MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES);

        for (Object item : selection.toList()) {
            WorkflowElement workflowElement = (WorkflowElement) item;
            if (workflowElement.isArchived() && !deleteArchived) {
                return false;
            }
            if (workflowElement instanceof Folder
                    || workflowElement instanceof AutomatedTestSuite) {
                return false;
            }
            if (!(workflowElement instanceof WorkflowProject)
                    && !workflowElement.isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                return false;
            }
        }
        return true;
    }

    public boolean renameApplies(IStructuredSelection selection) {
        if (selection.size() != 1)
            return false;

        Object element = selection.getFirstElement();
        if (!(element instanceof WorkflowElement) || ((WorkflowElement) element).isArchived())
            return false;

        WorkflowElement workflowElement = (WorkflowElement) element;
        if (element instanceof Folder || element instanceof AutomatedTestSuite)
            return false;
        if (element instanceof AutomatedTestCase)
            return !((AutomatedTestCase) element).isLegacy();
        if (element instanceof File || element instanceof LegacyExpectedResults)
            return false;
        return workflowElement.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
    }

    public boolean importPackageApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived())
            return false;

        if (!(element instanceof WorkflowProject))
            return false;

        WorkflowProject project = (WorkflowProject) element;

        if (!project.isInitialized())
            return false;

        if (project.isRemote() && project.checkRequiredVersion(6))
            return false;

        if (project.isFilePersist() && !project.isRemote())
            return true; // local file persist can always import (even Git)

        return project.getDesignerDataModel().userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN);
    }

    public boolean importVcsApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived())
            return false;

        if (!(element instanceof WorkflowProject))
            return false;

        WorkflowProject project = (WorkflowProject) element;

        if (!project.isInitialized())
            return false;

        if (!project.isGitVcs() || !project.isRemote())
            return false;

        return project.getDesignerDataModel().userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN);
    }

    public boolean importProjectApplies(IStructuredSelection selection) {
        return true;
    }

    public boolean importProcessApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived())
            return false;

        if (element.getProject().isRemote() && element.getProject().checkRequiredVersion(6))
            return false;

        if (element instanceof WorkflowPackage) {
            WorkflowPackage pkg = (WorkflowPackage) element;
            return !pkg.isDefaultPackage() && pkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
        }
        else if (element instanceof WorkflowProcess) {
            WorkflowProcess proc = (WorkflowProcess) element;
            return !proc.isInDefaultPackage()
                    && proc.getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN);
        }

        return false;
    }

    public boolean importWorkflowAssetApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived())
            return false;

        if (element.getProject().isRemote() && element.getProject().checkRequiredVersion(6))
            return false;

        if (element instanceof WorkflowPackage) {
            WorkflowPackage pkg = (WorkflowPackage) element;
            return !pkg.isDefaultPackage() && pkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
        }
        else if (element instanceof WorkflowAsset) {
            WorkflowAsset asset = (WorkflowAsset) element;
            return !asset.isInDefaultPackage() && asset.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
        }

        return false;
    }

    public boolean importAttributesApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived() || !element.getProject().checkRequiredVersion(5, 2))
            return false;

        if (element.getProject().isRemote() && element.getProject().checkRequiredVersion(6))
            return false;

        if (element instanceof WorkflowPackage) {
            WorkflowPackage pkg = (WorkflowPackage) element;
            return !pkg.isDefaultPackage() && pkg.getProject().getDataAccess()
                    .getDesignerDataModel().userHasRole(pkg.getGroup(), UserRoleVO.ASSET_DESIGN);
        }
        else if (element instanceof WorkflowProcess) {
            WorkflowProcess proc = (WorkflowProcess) element;
            return !proc.isInDefaultPackage() && proc.isInRuleSet()
                    && proc.getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN);
        }

        return false;
    }

    public boolean importTaskTemplateApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (element.isArchived() || !element.getProject().isFilePersist())
            return false;

        if (element.getProject().isRemote() && element.getProject().checkRequiredVersion(6))
            return false;

        if (!(element instanceof WorkflowPackage))
            return false;

        WorkflowPackage pkg = (WorkflowPackage) element;
        return !pkg.isDefaultPackage() && pkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
    }

    public boolean exportApplies(IStructuredSelection selection) {
        return  exportPackageApplies(selection)
                || exportProcessApplies(selection) || exportWorkflowAssetApplies(selection)
                || exportAttributesApplies(selection) || exportTaskTemplatesApplies(selection);
    }

    public boolean exportPackageApplies(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        if (!(element instanceof WorkflowElement))
            return false;
        WorkflowProject project = ((WorkflowElement) element).getProject();
        if (selection.size() > 1 && !project.getProject().checkRequiredVersion(5, 5))
            return false;

        for (Object item : selection.toList()) {
            if (!(item instanceof WorkflowPackage))
                return false;
            WorkflowPackage pkg = (WorkflowPackage) item;
            if (pkg.isDefaultPackage() || !pkg.getProject().equals(project))
                return false;
        }
        return true;
    }

    public boolean exportProcessApplies(IStructuredSelection selection) {
        return selection.size() == 1 && (selection.getFirstElement() instanceof WorkflowProcess);
    }

    public boolean exportWorkflowAssetApplies(IStructuredSelection selection) {
        return selection.size() == 1 && (selection.getFirstElement() instanceof WorkflowAsset)
                && !((WorkflowAsset) selection.getFirstElement()).getProject().isFilePersist();
    }

    public boolean exportAttributesApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement element = (WorkflowElement) selection.getFirstElement();
        if (!element.getProject().checkRequiredVersion(5, 2))
            return false;
        if (element instanceof WorkflowProcess)
            return ((WorkflowProcess) element).isInRuleSet() && !element.isInDefaultPackage();
        else if (element instanceof WorkflowPackage)
            return !((WorkflowPackage) element).isDefaultPackage();

        return false;
    }

    public boolean exportTaskTemplatesApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowPackage))
            return false;

        WorkflowPackage pkg = (WorkflowPackage) selection.getFirstElement();
        return pkg.getProject().isFilePersist();
    }

    public boolean exportAsApplies(IStructuredSelection selection) {
        return selection.size() == 1 && (selection.getFirstElement() instanceof WorkflowProcess);
    }

    // more specific run actions for submenu
    public boolean runOnServerApplies(IStructuredSelection selection) {
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
            return !workflowProject.isOsgi() && !workflowProject.isRemote();
        }
        return false;
    }

    public boolean runTestsApplies(IStructuredSelection selection) {
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowElement element = (WorkflowElement) selection.getFirstElement();
            if (element instanceof WorkflowProject)
                return ((WorkflowProject) element)
                        .isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION);
            if (element.getProject().isFilePersist() && element instanceof WorkflowPackage)
                return ((WorkflowPackage) element).isUserAuthorized(UserRoleVO.PROCESS_EXECUTION);
        }
        return false;
    }

    public boolean runMenuApplies(IStructuredSelection selection) {
        return runOnServerApplies(selection) || runTestsApplies(selection);
    }

    // top-level run action
    public boolean runApplies(IStructuredSelection selection) {
        if (selection.size() > 1) {
            Boolean legacy = null;
            WorkflowPackage testPkg = null;
            for (Object o : selection.toArray()) {
                if (!(o instanceof AutomatedTestCase))
                    return false;
                // make sure they're all in the same package or legacy suite
                AutomatedTestCase testCase = (AutomatedTestCase) o;
                if (legacy == null)
                    legacy = testCase.isLegacy();
                if (legacy.booleanValue() != testCase.isLegacy())
                    return false;
                if (!testCase.isLegacy()) {
                    if (testPkg == null)
                        testPkg = testCase.getPackage();
                    if (!testPkg.equals(testCase.getPackage()))
                        return false;
                }
            }
            return ((AutomatedTestCase) selection.getFirstElement()).getProject()
                    .isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION);
        }

        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement workflowElement = (WorkflowElement) selection.getFirstElement();

        if (workflowElement instanceof Report)
            return true;

        if (workflowElement instanceof Page)
            return true;

        if (workflowElement instanceof Template
                && ((Template) workflowElement).getLanguage().equals(RuleSetVO.VELOCITY))
            return true;

        if (workflowElement instanceof WorkflowProcess || workflowElement instanceof ExternalEvent)
            return workflowElement.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION);

        if (workflowElement instanceof AutomatedTestSuite
                || workflowElement instanceof AutomatedTestCase) {
            if (workflowElement.getProject() == null || !workflowElement.getProject()
                    .isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION))
                return false;
            WorkflowPackage pkg = workflowElement.getPackage();
            if (pkg != null && !pkg.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION))
                return false;
            try {
                AutomatedTestView testView = (AutomatedTestView) MdwPlugin.getActivePage()
                        .showView("mdw.views.launch.automatedTest");
                return !testView.isLocked();
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Menu", workflowElement.getProject());
            }
        }

        return false;
    }

    public boolean runFromPageApplies(IStructuredSelection selection) {

        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement workflowElement = (WorkflowElement) selection.getFirstElement();

        if (workflowElement instanceof WorkflowProcess)
            return workflowElement.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION);

        return false;
    }

    public boolean debugApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowElement))
            return false;

        WorkflowElement workflowElement = (WorkflowElement) selection.getFirstElement();

        if (workflowElement instanceof AutomatedTestSuite
                || workflowElement instanceof AutomatedTestCase) {
            WorkflowProject prj = workflowElement.getProject();
            if (prj == null || !prj.checkRequiredVersion(6, 0) || !prj.isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION))
                return false;
            WorkflowPackage pkg = workflowElement.getPackage();
            if (pkg != null && !pkg.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION))
                return false;
            try {
                AutomatedTestView testView = (AutomatedTestView) MdwPlugin.getActivePage()
                        .showView("mdw.views.launch.automatedTest");
                return !testView.isLocked();
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Menu", workflowElement.getProject());
            }
        }

        if (workflowElement instanceof WorkflowProcess || workflowElement instanceof ExternalEvent)
            return !MdwPlugin.isRcp()
                    && workflowElement.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION);
        else
            return false;
    }

    public boolean showInstancesApplies(IStructuredSelection selection) {
        return selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProcess;
    }

    public boolean myTasksApplies(IStructuredSelection selection) {
        return selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject;
    }

    public boolean searchApplies(IStructuredSelection selection) {
        return selection.size() == 1 && (selection.getFirstElement() instanceof WorkflowProject
                || selection.getFirstElement() instanceof WorkflowPackage);
    }

    public boolean findCallersApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowProcess))
            return false;
        WorkflowProject project = ((WorkflowProcess) selection.getFirstElement()).getProject();
        return project.isPureMdw52();
    }

    public boolean showProcessHierarchyApplies(IStructuredSelection selection) {
        if (selection.size() != 1 || !(selection.getFirstElement() instanceof WorkflowProcess))
            return false;
        WorkflowProject project = ((WorkflowProcess) selection.getFirstElement()).getProject();
        return project.isFilePersist(); // otherwise it's too slow
    }

    public boolean createApplies(Class<? extends WorkflowElement> elementClass,
            IStructuredSelection selection) {
        if (selection.size() > 1)
            return false;

        if (WorkflowProject.class.equals(elementClass)
                || WorkflowElement.class.equals(elementClass))
            return true; // create a new local project

        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowElement) {
                WorkflowElement workflowElement = (WorkflowElement) selection.getFirstElement();
                if (!workflowElement.getProject()
                        .isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_DESIGN))
                    return false;
            }
            return true;
        }

        return false;
    }

    public boolean openApplies(IStructuredSelection selection) {
        return true; // open is always applicable
    }

    public boolean refreshApplies(IStructuredSelection selection) {
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject)
            return true;

        if (selection.size() == 1 && (selection.getFirstElement() instanceof AutomatedTestSuite
                || selection.getFirstElement() instanceof AutomatedTestCase))
            return true;

        return false;
    }

    public boolean updateApplies(IStructuredSelection selection) {
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowPackage) {
            WorkflowPackage pkgVersion = (WorkflowPackage) selection.getFirstElement();
            return !pkgVersion.isArchived() && !pkgVersion.isDefaultPackage();
        }

        return false;
    }

    public boolean webLaunchApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowProject)
                return true;
            else if (selection.getFirstElement() instanceof WorkflowPackage) {
                WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
                return packageVersion.getProject().checkRequiredVersion(5, 1);
            }
        }

        return false;
    }

    public boolean serverActionApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowProject) {
                WorkflowProject project = (WorkflowProject) selection.getFirstElement();
                if (project.isUserAuthorizedForSystemAdmin()
                        || project.isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION))
                    return true;
            }
        }
        return false;
    }

    public boolean swingLaunchApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowProject)
                return true;
        }
        return false;
    }

    public boolean newMenuApplies(IStructuredSelection selection) {
        if (selection.size() == 0) {
            return true; // need to be able to create new workflow projects
        }
        else if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowElement element = (WorkflowElement) selection.getFirstElement();
            if (element.isArchived())
                return false;
            else if (element instanceof Folder)
                return false;
            else if (element instanceof AutomatedTestSuite)
                return false;
            else if (element instanceof AutomatedTestCase)
                return !((AutomatedTestCase) element).isLegacy();
            else if (element instanceof LegacyExpectedResults)
                return false;
            else if (element instanceof File)
                return false;
            else
                return true;
        }
        return false;
    }

    public boolean importMenuApplies(IStructuredSelection selection) {
        if (selection.size() == 0) {
            return true; // need to be able to import workflow projects
        }
        else if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowElement) {
            WorkflowElement element = (WorkflowElement) selection.getFirstElement();
            if (element.isArchived())
                return false;
            else if (element instanceof Folder)
                return false;
            else if (element instanceof AutomatedTestSuite)
                return false;
            else if (element instanceof AutomatedTestCase)
                return !((AutomatedTestCase) element).isLegacy();
            else if (element instanceof LegacyExpectedResults)
                return false;
            else if (element instanceof File)
                return false;
            else
                return true;
        }
        return false;
    }

    public boolean formatMenuApplies(IStructuredSelection selection) {
        return formatFunctionTestResultsApplies(selection)
                || formatLoadTestResultsApplies(selection);
    }

    public boolean formatFunctionTestResultsApplies(IStructuredSelection selection) {
        // enablement is governed by existance of test results
        return selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject;
    }

    public boolean formatLoadTestResultsApplies(IStructuredSelection selection) {
        // enablement is governed by existance of test results
        return selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject;
    }

    public boolean testApplies(IStructuredSelection selection) {
        return selection.size() == 1 && selection.getFirstElement() instanceof WorkflowProject;
    }

    public boolean compareResultsApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof AutomatedTestResults) {
                AutomatedTestResults expectedResults = (AutomatedTestResults) selection
                        .getFirstElement();
                return expectedResults.getActualResults().exists();
            }
            else if (selection.getFirstElement() instanceof LegacyExpectedResults) {
                LegacyExpectedResults expectedResult = (LegacyExpectedResults) selection
                        .getFirstElement();
                return expectedResult.getActualResult().exists();
            }
        }
        return false;
    }

    public boolean openInstanceApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof AutomatedTestResults) {
                AutomatedTestResults expectedResults = (AutomatedTestResults) selection
                        .getFirstElement();
                return expectedResults.getActualResults().exists();
            }
            else if (selection.getFirstElement() instanceof LegacyExpectedResults) {
                LegacyExpectedResults expectedResult = (LegacyExpectedResults) selection
                        .getFirstElement();
                return expectedResult.getActualResult().exists();
            }
        }
        return false;
    }

    public boolean unlockApplies(IStructuredSelection selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof WorkflowProject) {
                WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
                return workflowProject.isFilePersist() && workflowProject.isRemote();
            }
        }
        return false;
    }

    @SuppressWarnings("restriction")
    class ResultsCompareAction extends org.eclipse.compare.internal.CompareAction {
        public ResultsCompareAction(ISelection selection) {
            // prime the pump
            super.isEnabled(selection);
            setActivePart(compareResultsAction, view.getViewSite().getPart());
        }
    }

    private WorkflowElement[] selectionToElementArray(IStructuredSelection selection) {
        Object[] items = selection.toArray();
        WorkflowElement[] elements = new WorkflowElement[items.length];
        for (int i = 0; i < items.length; i++)
            elements[i] = (WorkflowElement) items[i];
        return elements;
    }
}
