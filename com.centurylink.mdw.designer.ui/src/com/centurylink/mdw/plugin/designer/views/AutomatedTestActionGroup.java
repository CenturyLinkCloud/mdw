/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

import com.centurylink.mdw.designer.testing.TestFile;
import com.centurylink.mdw.designer.testing.TestFileLine;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.MdwMenuManager;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutomatedTestActionGroup extends ActionGroup {
    private AutomatedTestView view;
    private WorkflowElementActionHandler actionHandler;

    public WorkflowElementActionHandler getActionHandler() {
        return actionHandler;
    }

    private IAction rerunAction;

    public IAction getRerunAction() {
        return rerunAction;
    }

    private IAction rerunSelectionAction;

    public IAction getRerunSelectionAction() {
        return rerunSelectionAction;
    }

    private IAction stopAction;

    public IAction getStopAction() {
        return stopAction;
    }

    private ActionGroup formatActionGroup;
    private MenuManager formatMenu;
    private IAction formatFunctionTestResultsAction;

    public IAction getFormatFunctionTestResultsAction() {
        return formatFunctionTestResultsAction;
    }

    private IAction formatLoadTestResultsAction;

    public IAction getFormatLoadTestResultsAction() {
        return formatLoadTestResultsAction;
    }

    private IAction compareResultsAction;

    public IAction getCompareResultsAction() {
        return compareResultsAction;
    }

    private IAction openProcessInstanceAction;

    public IAction getOpenProcessInstanceAction() {
        return openProcessInstanceAction;
    }

    private IAction openTestCase;

    public IAction getOpenTestCaseAction() {
        return openTestCase;
    }

    private IAction openExpectedAction;

    public IAction getOpenExpectedAction() {
        return openExpectedAction;
    }

    public AutomatedTestActionGroup(AutomatedTestView view) {
        this.view = view;

        rerunAction = createRerunAction();
        enableRerunAction(false);
        rerunSelectionAction = createRerunSelectionAction();
        stopAction = createStopAction();
        enableStopAction(false);

        formatActionGroup = createFormatActionGroup();
        formatFunctionTestResultsAction = createFormatFunctionTestResultsAction();
        formatLoadTestResultsAction = createFormatLoadTestResultsAction();
        enableFormatFunctionTestResults(false);
        enableFormatLoadTestResults(false);
        openTestCase = createOpenTestCaseAction();
        openExpectedAction = createOpenExpectedAction();
        compareResultsAction = createCompareResultsAction();
        openProcessInstanceAction = createOpenProcessInstanceAction();

        actionHandler = new WorkflowElementActionHandler();
    }

    public void enableRerunAction(boolean enabled) {
        rerunAction.setEnabled(enabled);
    }

    public void enableStopAction(boolean enabled) {
        stopAction.setEnabled(enabled);
    }

    public void enableFormatFunctionTestResults(boolean enabled) {
        formatFunctionTestResultsAction.setEnabled(enabled);
    }

    public void enableFormatLoadTestResults(boolean enabled) {
        formatLoadTestResultsAction.setEnabled(enabled);
    }

    private IAction createRerunAction() {
        IAction action = new Action() {
            public void run() {
                view.handleRerun();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "rerun.tests");
        action.setText("Rerun Test(s)");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/rerun.gif");
        action.setImageDescriptor(imageDesc);
        ImageDescriptor disabledImageDesc = MdwPlugin
                .getImageDescriptor("icons/rerun_disabled.gif");
        action.setDisabledImageDescriptor(disabledImageDesc);
        return action;
    }

    private IAction createRerunSelectionAction() {
        IAction action = new Action() {
            public void run() {
                view.handleRerunSelection();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "rerun.selected.tests");
        action.setText("Rerun Test(s)");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/rerun.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createStopAction() {
        IAction action = new Action() {
            public void run() {
                view.handleStop();
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "stop.tests");
        action.setText("Stop Test(s)");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/stop.gif");
        action.setImageDescriptor(imageDesc);
        ImageDescriptor disabledImageDesc = MdwPlugin.getImageDescriptor("icons/stop_disabled.gif");
        action.setDisabledImageDescriptor(disabledImageDesc);
        return action;
    }

    private ActionGroup createFormatActionGroup() {
        formatMenu = new MenuManager("Format", null, MdwMenuManager.MDW_MENU_PREFIX + "format");
        return new ActionGroup() {
            @Override
            public void fillContextMenu(IMenuManager menu) {
                formatMenu.removeAll();
                WorkflowProject project = view.getTestSuite().getProject();
                formatMenu.add(formatFunctionTestResultsAction);
                java.io.File resultsFile = project.getFunctionTestResultsFile();
                formatFunctionTestResultsAction
                        .setEnabled(resultsFile != null && resultsFile.exists());
                formatMenu.add(formatLoadTestResultsAction);
                resultsFile = project.getLoadTestResultsFile();
                formatLoadTestResultsAction.setEnabled(resultsFile != null && resultsFile.exists());
            }
        };
    }

    private IAction createFormatFunctionTestResultsAction() {
        IAction action = new Action() {
            public void run() {
                actionHandler.formatResults(view.getTestSuite().getProject(),
                        AutomatedTestCase.FUNCTION_TEST);
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "format.function.test.results");
        action.setText("Format Function Test Results");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/auto_test.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createFormatLoadTestResultsAction() {
        IAction action = new Action() {
            public void run() {
                actionHandler.formatResults(view.getTestSuite().getProject(),
                        AutomatedTestCase.LOAD_TEST);
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "format.load.test.results");
        action.setText("Format Load Test Results");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/load_test.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createOpenTestCaseAction() {
        IAction action = new Action() {
            public void run() {
                if (openTestCaseApplies(view.getSelectedItem()))
                    actionHandler.open(view.getSelectedItem());
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "open.test.case");
        action.setText("Open Test Case");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/test.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createOpenExpectedAction() {
        IAction action = new Action() {
            public void run() {
                if (openExpectedApplies(view.getSelectedItem()))
                    actionHandler.open(view.getSelectedItem());
            }
        };
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "open.expected.results");
        action.setText("Open Expected Results");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/result.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createCompareResultsAction() {
        IAction action = new Action() {
            @SuppressWarnings("restriction")
            public void run() {
                if (compareResultsApplies(view.getSelectedItem())) {
                    WorkflowProject project = ((WorkflowElement) view.getSelectedItem())
                            .getProject();
                    try {
                        project.getProjectFolder(
                                project.getTestResultsPath(AutomatedTestCase.FUNCTION_TEST))
                                .refreshLocal(IResource.DEPTH_INFINITE, null);
                        Object[] items = new Object[2];
                        if (view.getSelectedItem() instanceof AutomatedTestResults) {
                            AutomatedTestResults expectedResults = (AutomatedTestResults) view
                                    .getSelectedItem();
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
                        else if (view.getSelectedItem() instanceof LegacyExpectedResults) {
                            LegacyExpectedResults expectedResult = (LegacyExpectedResults) view
                                    .getSelectedItem();
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

    private IAction createOpenProcessInstanceAction() {
        IAction action = new Action() {
            public void run() {
                if (openProcessInstanceApplies(view.getSelectedItem())) {
                    WorkflowProject project = ((WorkflowElement) view.getSelectedItem())
                            .getProject();
                    try {
                        Long procInstId = null;
                        if (view.getSelectedItem() instanceof AutomatedTestResults) {
                            AutomatedTestResults expectedResults = (AutomatedTestResults) view
                                    .getSelectedItem();
                            procInstId = expectedResults.getActualProcessInstanceId();
                        }
                        else if (view.getSelectedItem() instanceof LegacyExpectedResults) {
                            LegacyExpectedResults expectedResult = (LegacyExpectedResults) view
                                    .getSelectedItem();
                            File resultsFile = expectedResult.getActualResultFile();
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
        action.setId(MdwMenuManager.MDW_MENU_PREFIX + "open.process.instance");
        action.setText("Open Process Instance");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        IToolBarManager toolbar = actionBars.getToolBarManager();
        toolbar.add(new GroupMarker("mdw.toolbox.group"));
        toolbar.add(rerunAction);
        toolbar.add(stopAction);
        toolbar.add(formatFunctionTestResultsAction);
        toolbar.add(formatLoadTestResultsAction);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        final WorkflowElement selection = view.getSelectedItem();
        if (selection == null)
            return;

        // open commands
        if (openTestCaseApplies(selection))
            menu.add(openTestCase);

        // rerun tests
        if (rerunApplies(selection))
            menu.add(rerunSelectionAction);

        // open expected
        if (openExpectedApplies(selection))
            menu.add(openExpectedAction);

        // compare results with expected
        if (compareResultsApplies(selection))
            menu.add(compareResultsAction);

        // open process instance
        if (openProcessInstanceApplies(selection))
            menu.add(openProcessInstanceAction);

        // format
        if (formatMenuApplies(selection)) {
            menu.add(formatMenu);
            formatActionGroup.fillContextMenu(menu);
        }
    }

    public boolean openTestCaseApplies(WorkflowElement selection) {
        if (selection instanceof AutomatedTestCase)
            return true;
        else
            return false;
    }

    public boolean rerunApplies(WorkflowElement selection) {
        if (selection instanceof AutomatedTestSuite) {
            AutomatedTestSuite testSuite = (AutomatedTestSuite) selection;
            return !testSuite.isRunning();
        }
        else if (selection instanceof WorkflowPackage) {
            return !view.getTestSuite().isRunning();
        }
        else if (selection instanceof AutomatedTestCase) {
            AutomatedTestCase testCase = (AutomatedTestCase) selection;
            return !testCase.getTestSuite().isRunning();
        }
        return false;
    }

    public boolean openExpectedApplies(WorkflowElement selection) {
        return (selection instanceof AutomatedTestResults
                || selection instanceof LegacyExpectedResults);
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
                return !expectedResult.getTestCase().isRunning()
                        && expectedResult.getActualResult().exists();
            }
        }
        return false;
    }

    public boolean openProcessInstanceApplies(WorkflowElement selection) {
        if (selection.size() == 1) {
            if (selection.getFirstElement() instanceof AutomatedTestResults) {
                AutomatedTestResults expectedResults = (AutomatedTestResults) selection
                        .getFirstElement();
                return expectedResults.getActualResults().exists();
            }
            else if (selection.getFirstElement() instanceof LegacyExpectedResults) {
                LegacyExpectedResults expectedResult = (LegacyExpectedResults) selection
                        .getFirstElement();
                return !expectedResult.getTestCase().isRunning()
                        && expectedResult.getActualResult().exists();
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

    public boolean formatMenuApplies(IStructuredSelection selection) {
        return selection.size() == 1 && selection.getFirstElement() instanceof AutomatedTestSuite;
    }
}