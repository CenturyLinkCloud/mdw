/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.designer.pages.DesignerPage.PersistType;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.ProcessCanvasWrapper;
import com.centurylink.mdw.plugin.designer.WorkflowSelectionProvider;
import com.centurylink.mdw.plugin.designer.dialogs.ArchivedProcessSaveDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ProcessSaveDialog;
import com.centurylink.mdw.plugin.designer.editors.ZoomLevelMenuCreator.Zoomable;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.views.ProcessInstanceListView;
import com.centurylink.mdw.plugin.designer.views.ToolboxView;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessEditor extends WorkflowElementEditor
        implements Zoomable, ElementChangeListener {
    private WorkflowSelectionProvider selectionProvider;
    private ProcessEditorContextListener contextPartListener;
    private WorkflowEditorPartListener toolboxViewPartListener;

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public WorkflowElement getElement() {
        return process;
    }

    private ProcessCanvasWrapper processCanvasWrapper;

    public ProcessCanvasWrapper getProcessCanvasWrapper() {
        return processCanvasWrapper;
    }

    // support view
    private ToolboxView toolboxView;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        if (input instanceof WorkflowProcess) {
            setInput(input);
            process = (WorkflowProcess) input;
        }
        else if (input instanceof FileEditorInput) {
            IFile file = ((FileEditorInput) input).getFile();
            String processName = file.getName().substring(0,
                    file.getName().length() - (file.getFileExtension().length() + 1));
            WorkflowProject project = WorkflowProjectManager.getInstance()
                    .getWorkflowProject(file.getProject());
            project.getDesignerProxy(); // force initialization
            process = project.getProcess(processName);
            setInput(process);
        }

        process.addElementChangeListener(this);
        selectionProvider = new WorkflowSelectionProvider(process);
        site.setSelectionProvider(selectionProvider);
        if (process.hasInstanceInfo())
            setPartName(
                    process.getProcessInstance().getId().toString() + " - " + process.getName());
        else
            setPartName(process.getLabel());
    }

    /**
     * The Eclipse workbench title bar.
     */
    @Override
    public String getTitleToolTip() {
        return process.getFullPathLabel();
    }

    @Override
    public void createPartControl(Composite parent) {
        processCanvasWrapper = new ProcessCanvasWrapper(parent, process);
        processCanvasWrapper.setSelectionProvider(selectionProvider);
        processCanvasWrapper.addDirtyStateListener(this);
        processCanvasWrapper.populate();

        toolboxView = ToolboxView.show();
        if (toolboxView != null) {
            toolboxViewPartListener = toolboxView.getEditorListener(process);
            getSite().getPage().addPartListener(toolboxViewPartListener);
        }

        contextPartListener = new ProcessEditorContextListener(getProcess());
        getSite().getPage().addPartListener(contextPartListener);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
                MdwPlugin.getPluginId() + ".process_editor_help");
    }

    @Override
    public void dispose() {
        super.dispose();
        if (process != null)
            process.removeElementChangeListener(this);
        if (processCanvasWrapper != null)
            processCanvasWrapper.dispose();
        if (contextPartListener != null)
            getSite().getPage().removePartListener(contextPartListener);
        if (toolboxView != null)
            getSite().getPage().removePartListener(toolboxViewPartListener);
    }

    public boolean isForProcessInstance() {
        return processCanvasWrapper.isInstance();
    }

    public boolean isLaunchAllowed() {
        return getProcess().getPackage() != null
                && getProcess().getPackage().isUserAuthorized(UserRoleVO.PROCESS_EXECUTION);
    }

    public boolean isDebugAllowed() {
        return isLaunchAllowed() && !MdwPlugin.isRcp();
    }

    public boolean isLockAllowed() {
        return processCanvasWrapper.isLockAllowed();
    }

    public void toggleProcessLock(boolean lock) {
        processCanvasWrapper.toggleProcessLock(lock);
    }

    public boolean isRecordChanges() {
        return processCanvasWrapper.isRecordChanges();
    }

    public void setRecordChanges(boolean record) {
        processCanvasWrapper.setRecordChanges(record);
    }

    public void commitChanges() {
        if (MessageDialog.openConfirm(getSite().getShell(), "Commit Changes",
                "Commit recorded changes?"))
            processCanvasWrapper.commitChanges();
    }

    public void launchProcess(boolean debug) {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        if (debug)
            actionHandler.debug(process);
        else
            actionHandler.run(process);
    }

    public void showInstances() {
        try {
            IViewPart viewPart = getSite().getPage()
                    .showView("mdw.views.designer.process.instance.list");
            if (viewPart != null) {
                ProcessInstanceListView instancesView = (ProcessInstanceListView) viewPart;
                instancesView.setProcess(getProcess());
            }
        }
        catch (PartInitException ex) {
            PluginMessages.log(ex);
        }
    }

    public void openProcessDefinition() {
        processCanvasWrapper.openProcessDefinition(new WorkflowProcess(process));
    }

    public void updateCanvasBackground() {
        processCanvasWrapper.updateCanvasBackground();
    }

    public void setCanvasLinkStyle(String linkShape) {
        processCanvasWrapper.setLinkStyle(linkShape);
    }

    /**
     * Unlike link style, node style is saved with the process, so instead of
     * being global, node style is remembered for each opened process.
     */
    public void setCanvasNodeStyle(String nodeShape) {
        processCanvasWrapper.setNodeStyle(nodeShape);
    }

    public String getNodeIdType() {
        return getProcessCanvasWrapper().getNodeIdType();
    }

    public void setNodeIdType(String nodeIdType) {
        getProcessCanvasWrapper().setNodeIdType(nodeIdType);
    }

    public boolean isShowToolTips() {
        return getProcessCanvasWrapper().isShowToolTips();
    }

    public void setShowToolTips(boolean showToolTips) {
        getProcessCanvasWrapper().setShowToolTips(showToolTips);
    }

    public void exportAs() {
        processCanvasWrapper.exportAs(getSite().getShell());
    }

    private ProcessEditorActionBarContributor getActionBarContributor() {
        return (ProcessEditorActionBarContributor) getEditorSite().getActionBarContributor();
    }

    public void refresh() {
        processCanvasWrapper.refresh();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (!saveActiveScriptEditors())
            return;

        if (overrideAttributesDirty()) {
            int overrideSaveOutcome = promptToSaveOverrideAttributes();
            if (overrideSaveOutcome == ISaveablePart2.YES)
                saveOverrideAttributes();
            else if (overrideSaveOutcome == ISaveablePart2.NO)
                clearOverrideAttributesDirtyState();

            if (overrideSaveOutcome == ISaveablePart2.CANCEL)
                return;
        }

        if (checkLatest()) {
            ProcessSaveDialog saveDialog = new ProcessSaveDialog(getSite().getShell(), getProcess(),
                    false);
            int result = saveDialog.open();
            if (result == ProcessSaveDialog.CANCEL)
                return;

            int version = saveDialog.getVersion();
            if (result == ProcessSaveDialog.FORCE_UPDATE) {
                if (!forceUpdateProcess()) {
                    saveDialog.setAllowForceUpdate(false);
                    result = saveDialog.open();
                    version = saveDialog.getVersion();
                    if (result == ProcessSaveDialog.CANCEL)
                        return;
                }
            }
            if (result == ProcessSaveDialog.OVERWRITE)
                saveProcess(PersistType.UPDATE, version);
            if (result == ProcessSaveDialog.NEW_VERSION)
                saveProcess(PersistType.NEW_VERSION, version);
        }
        else {
            ArchivedProcessSaveDialog saveDialog = new ArchivedProcessSaveDialog(
                    getSite().getShell(), process, false);
            int result = saveDialog.open();
            if (result == ProcessSaveDialog.CANCEL)
                return;
            else if (result == ProcessSaveDialog.FORCE_UPDATE)
                forceUpdateProcess();
        }

        getSite().getPage().activate(this);
    }

    private boolean checkLatest() {
        boolean isLatest = getProcess().isLatest();
        if (!isLatest && !DesignerProxy.isArchiveEditAllowed()) {
            String msg = getProcess().getLabel()
                    + " is flagged as archived and yet is editable without the required system property.  "
                    + "Process will be temporarily dearchived to support save and avoid losing work.  Please report this error to MDW Support.";
            PluginMessages.uiError(msg, "Save Process", getProcess().getProject());
            isLatest = true;
        }
        return isLatest;
    }

    private void saveProcess(PersistType persistType, int version) {
        processCanvasWrapper.saveProcess(persistType, version);

        // in case version was incremented
        setPartName(processCanvasWrapper.getLabel());
        firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);

        refreshActions();

        // notify listeners that properties may have changed
        getProcess().fireElementChangeEvent(ChangeType.PROPERTIES_CHANGE, null);
    }

    private boolean forceUpdateProcess() {
        boolean allowed = processCanvasWrapper.forceUpdateProcess()
                .getStatus() != RunnerStatus.DISALLOW;

        refreshActions();
        // notify listeners that properties may have changed
        getProcess().fireElementChangeEvent(ChangeType.PROPERTIES_CHANGE, null);

        return allowed;
    }

    public void refreshActions() {
        if (!isForProcessInstance()) {
            getActionBarContributor().getLockAction().setChecked(getProcess().isLockedToUser());
            getActionBarContributor().getLockAction().setEnabled(isLockAllowed());
            getActionBarContributor().getLockAction()
                    .setText(isLockAllowed() ? "Lock/Unlock Process" : "");
        }
    }

    @Override
    public boolean isDirty() {
        if (isForProcessInstance())
            return false;

        return super.isDirty();
    }

    public boolean overrideAttributesDirty() {
        return getProcess().isAnyOverrideAttributeDirty()
                || getProcess().isAnyAttributeOwnerDirty();
    }

    public Long getProcessId() {
        return process.getId();
    }

    public void remove() {
        closeActiveScriptEditors(false);
        processCanvasWrapper.remove();

        if (overrideAttributesDirty()) {
            int overrideSaveOutcome = promptToSaveOverrideAttributes();
            if (overrideSaveOutcome == ISaveablePart2.YES)
                saveOverrideAttributes();
            else
                clearOverrideAttributesDirtyState();
        }
    }

    private List<IEditorPart> activeScriptEditors = new ArrayList<IEditorPart>();

    public void addActiveScriptEditor(final IEditorPart editor) {
        if (!activeScriptEditors.contains(editor)) {
            activeScriptEditors.add(editor);
            editor.getSite().getPage().addPartListener(new IPartListener2() {
                public void partClosed(IWorkbenchPartReference partRef) {
                    removeActiveScriptEditor(editor);
                }

                public void partActivated(IWorkbenchPartReference partRef) {
                }

                public void partBroughtToTop(IWorkbenchPartReference partRef) {
                }

                public void partDeactivated(IWorkbenchPartReference partRef) {
                }

                public void partOpened(IWorkbenchPartReference partRef) {
                }

                public void partHidden(IWorkbenchPartReference partRef) {
                }

                public void partVisible(IWorkbenchPartReference partRef) {
                }

                public void partInputChanged(IWorkbenchPartReference partRef) {
                }
            });
        }
    }

    public void removeActiveScriptEditor(IEditorPart editor) {
        activeScriptEditors.remove(editor);
    }

    private boolean closeActiveScriptEditors(boolean promptForSave) {
        boolean proceed = true;
        List<IEditorPart> copy = new ArrayList<IEditorPart>(activeScriptEditors);
        for (IEditorPart scriptEditor : copy) {
            if (scriptEditor != null) {
                if (!scriptEditor.getSite().getPage().closeEditor(scriptEditor, promptForSave))
                    proceed = false;
            }
        }
        return proceed;
    }

    private boolean saveActiveScriptEditors() {
        boolean proceed = true;
        for (IEditorPart scriptEditor : activeScriptEditors) {
            if (scriptEditor != null) {
                if (scriptEditor.isDirty()
                        && !scriptEditor.getSite().getPage().saveEditor(scriptEditor, true))
                    proceed = false;
            }
        }
        return proceed;
    }

    /**
     * Compare return value with ISaveablePart2.CANCEL/NO instead of
     * ProcessSaveDialog.CANCEL/DONT_SAVE. These are exactly the opposite of
     * what you want (inherited from Dialog.CANCEL).
     */
    public int promptToSaveOnClose() {
        getSite().getShell().setFocus();
        closeActiveScriptEditors(true);
        getSite().getShell().setFocus();

        if (checkLatest()) {
            ProcessSaveDialog saveDialog = new ProcessSaveDialog(getSite().getShell(), getProcess(),
                    true);
            int result = saveDialog.open();
            if (result == ProcessSaveDialog.DONT_SAVE) {
                return ISaveablePart2.NO;
            }
            else if (result == ProcessSaveDialog.CANCEL) {
                return ISaveablePart2.CANCEL;
            }
            else {
                int version = saveDialog.getVersion();
                if (result == ProcessSaveDialog.FORCE_UPDATE) {
                    if (!forceUpdateProcess()) {
                        saveDialog.setAllowForceUpdate(false);
                        result = saveDialog.open();
                        version = saveDialog.getVersion();
                        if (result == ProcessSaveDialog.CANCEL)
                            return ISaveablePart2.CANCEL;
                    }
                }

                if (result == ProcessSaveDialog.OVERWRITE)
                    saveProcess(PersistType.UPDATE, version);
                if (result == ProcessSaveDialog.NEW_VERSION)
                    saveProcess(PersistType.NEW_VERSION, version);
            }
        }
        else {
            ArchivedProcessSaveDialog saveDialog = new ArchivedProcessSaveDialog(
                    getSite().getShell(), process, true);
            int result = saveDialog.open();
            if (result == ProcessSaveDialog.DONT_SAVE)
                return ISaveablePart2.NO;
            else if (result == ProcessSaveDialog.CANCEL)
                return ISaveablePart2.CANCEL;
            else if (result == ProcessSaveDialog.FORCE_UPDATE)
                forceUpdateProcess();
        }

        return ISaveablePart2.CANCEL; // least dangerous
    }

    private int promptToSaveOverrideAttributes() {
        String msg = getProcess().getName() + " has unsaved override attributes.  Save changes?";
        String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
        MessageDialog attrsSaveDialog = new MessageDialog(getSite().getShell(), "Save Attributes",
                null, msg, MessageDialog.QUESTION, buttons, 0);
        return attrsSaveDialog.open();
    }

    private void saveOverrideAttributes() {
        getProcess().getProject().getDesignerProxy().saveAllOverrideAttributes(getProcess());
        clearOverrideAttributesDirtyState();
    }

    private void clearOverrideAttributesDirtyState() {
        getProcess().clearOverrideAttributesDirty();
        getProcess().clearAttributeOwnersDirty();
    }

    public void notifyNameChange() {
        setPartName(processCanvasWrapper.getLabel());
        firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
    }

    @Override
    public void setFocus() {
        processCanvasWrapper.setFocus();
    }

    public int getZoomLevel() {
        return processCanvasWrapper.getZoomLevel();
    }

    public void setZoomLevel(int zoomLevel) {
        processCanvasWrapper.setZoomLevel(zoomLevel);
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getChangeType().equals(ChangeType.RENAME)
                || ece.getChangeType().equals(ChangeType.VERSION_CHANGE)) {
            notifyNameChange();
            processCanvasWrapper.repaint();
        }
    }

    @Override
    public boolean isSaveAsAllowed() {
        return getProcess().getPackage() != null
                && getProcess().isUserAuthorized(UserRoleVO.ASSET_DESIGN);
    }

    @Override
    public void doSaveAs() {
        if (!saveActiveScriptEditors())
            return;

        processCanvasWrapper.saveProcessAs();
    }
}
