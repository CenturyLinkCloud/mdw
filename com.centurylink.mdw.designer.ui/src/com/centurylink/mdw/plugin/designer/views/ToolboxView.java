/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.NewWizard;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.ToolboxWrapper;
import com.centurylink.mdw.plugin.designer.WorkflowSelectionProvider;
import com.centurylink.mdw.plugin.designer.dialogs.ToolboxFilterDialog;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.editors.WorkflowEditorPartListener;
import com.centurylink.mdw.plugin.designer.editors.WorkflowElementEditor;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.TabbedPropertySheetPage;
import com.centurylink.mdw.plugin.designer.wizards.ImportPackageWizard;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

@SuppressWarnings("restriction")
public class ToolboxView extends ViewPart
        implements ITabbedPropertySheetPageContributor, DirtyStateListener {
    public static final String VIEW_ID = "mdw.views.designer.toolbox";

    private ToolboxWrapper toolboxWrapper;
    private WorkflowSelectionProvider selectionProvider;
    private ToolboxViewActionGroup actionGroup;

    public void setInput(ToolboxWrapper toolboxWrapper) {
        this.toolboxWrapper = toolboxWrapper;
        toolboxWrapper.populate();
        actionGroup.enableToolbarActions(toolboxWrapper.isPopulated());
        updateViewTitle();
        setViewTitleTooltip();
        actionGroup.getDeleteAction().setEnabled(false);
    }

    public boolean isDirty() {
        return toolboxWrapper.isDirty();
    }

    @Override
    public void createPartControl(Composite parent) {
        toolboxWrapper.setParent(parent);
        toolboxWrapper.populate();

        actionGroup = new ToolboxViewActionGroup(this);
        IActionBars actionBars = getViewSite().getActionBars();
        actionGroup.fillActionBars(actionBars);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
                MdwPlugin.getPluginId() + ".toolbox_help");
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        toolboxWrapper = ToolboxWrapper.getInstance();
        selectionProvider = new WorkflowSelectionProvider(toolboxWrapper.getProcess());
        selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection() instanceof ActivityImpl) {
                    ActivityImpl impl = (ActivityImpl) event.getSelection();
                    actionGroup.getDeleteAction().setEnabled(event.getSelection() != null
                            && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
                }
                else {
                    actionGroup.getDeleteAction().setEnabled(false);
                }
            }
        });
        site.setSelectionProvider(selectionProvider);
        toolboxWrapper.setSelectionProvider(selectionProvider);
        toolboxWrapper.addDirtyStateListener(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        toolboxWrapper.dispose();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class)
            return new TabbedPropertySheetPage(this);
        else if (type == ISelectionProvider.class)
            return ToolboxWrapper.class;

        return super.getAdapter(type);
    }

    public void dirtyStateChanged(boolean dirty) {
        if (toolboxWrapper.getToolboxSelection() != null)
            actionGroup.getSaveAction().setEnabled(dirty && toolboxWrapper.getToolboxSelection()
                    .isUserAuthorized(UserRoleVO.ASSET_DESIGN));
        updateViewTitle();
    }

    private void updateViewTitle() {
        if (toolboxWrapper.isInstance())
            setPartName("Legend");
        else
            setPartName(isDirty() ? "*Toolbox" : "Toolbox");
        firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
    }

    private void setViewTitleTooltip() {
        if (toolboxWrapper.isInstance())
            setTitleToolTip("Workflow Element Color Coding");
        else
            setTitleToolTip("Toolbox" + (toolboxWrapper.isPopulated()
                    ? " for " + toolboxWrapper.getProject().getName() : ""));
    }

    public String getContributorId() {
        return "mdw.tabbedprops.contributor"; // see plugin.xml
    }

    public ActivityImpl getSelection() {
        return toolboxWrapper.getToolboxSelection();
    }

    public boolean isSelection() {
        return getSelection() != null;
    }

    public void handleSort(boolean aToZ) {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        prefsStore.setValue(PreferenceConstants.PREFS_SORT_TOOLBOX_A_TO_Z, aToZ);
        toolboxWrapper.update();
        ;
    }

    public void handleRefresh() {
        boolean refresh = true;
        if (isDirty()) {
            String confMsg = "Refresh will overwrite unsaved activity implementors. Continue?";
            refresh = MessageDialog.openConfirm(getSite().getShell(), "Confirm Refresh", confMsg);
        }
        if (refresh) {
            toolboxWrapper.refresh();
            dirtyStateChanged(false);
            actionGroup.getDeleteAction().setEnabled(false);
        }
    }

    public void handleFilter() {
        if (isDirty()) {
            MessageDialog.openWarning(getSite().getShell(), "Please Save",
                    "Please save or abandon outstanding changes before filtering");
            return;
        }

        WorkflowProject project = getProject();
        try {
            ToolboxFilterDialog filterDlg = new ToolboxFilterDialog(getSite().getShell(), project,
                    project.getSuppressedActivityImplementors());
            int res = filterDlg.open();
            if (res == Dialog.OK) {
                project.setSuppressedActivityImplementors(filterDlg.getSuppressedImplementors());
                toolboxWrapper.update();
                actionGroup.getDeleteAction().setEnabled(false);
            }
        }
        catch (IOException ex) {
            PluginMessages.uiError(ex, "Filter Implementors", project);
        }
    }

    public void handleSave() {
        boolean save = true;
        if (toolboxWrapper.getProject().isProduction()) {
            String confMsg = "Save activity implementors in Production environment?";
            save = MessageDialog.openConfirm(getSite().getShell(), "Confirm Save", confMsg);
        }
        if (save) {
            List<ActivityImpl> dirtyImpls = toolboxWrapper.getDirtyImpls();
            List<String> dirtyImplNames = new ArrayList<String>();
            for (ActivityImpl dirtyImpl : dirtyImpls)
                dirtyImplNames.add(dirtyImpl.getImplClassName());
            int res = PluginMessages.uiList(getSite().getShell(),
                    "The following modified activity implementors will be saved",
                    "Save Implementors", dirtyImplNames);
            if (res == Dialog.CANCEL)
                return;

            getDesignerProxy().saveActivityImpls(dirtyImpls);

            toolboxWrapper.clearDirty();
            dirtyStateChanged(false);
            actionGroup.getDeleteAction().setEnabled(false);
            toolboxWrapper.update(); // redraw to reflect the change
        }
    }

    public void handleNew() {
        if (isDirty()) {
            MessageDialog.openWarning(getSite().getShell(), "Please Save",
                    "Please save or abandon outstanding changes before creating a new implementor");
            return;
        }
        else {
            IWorkbench workbench = PlatformUI.getWorkbench();
            NewWizard wizard = new NewWizard();
            wizard.setCategoryId("mdw.codegen.activity");
            wizard.init(workbench, new StructuredSelection(getProject()));

            IDialogSettings workbenchSettings = IDEWorkbenchPlugin.getDefault().getDialogSettings();
            IDialogSettings wizardSettings = workbenchSettings.getSection("NewWizardAction");
            if (wizardSettings == null)
                wizardSettings = workbenchSettings.addNewSection("NewWizardAction");
            wizardSettings.put("NewWizardSelectionPage.STORE_SELECTED_ID",
                    "mdw.codegen.general.activity");
            wizard.setDialogSettings(wizardSettings);
            wizard.setForcePreviousAndNextButtons(true);

            WizardDialog dialog = new WizardDialog(null, wizard);
            dialog.create();
            if (dialog.open() != Dialog.CANCEL)
                toolboxWrapper.update();
        }
    }

    public void handleDiscover() {
        if (isDirty()) {
            MessageDialog.openWarning(getSite().getShell(), "Please Save",
                    "Please save or abandon outstanding changes before discovering new assets");
            return;
        }
        else {
            IWorkbench workbench = PlatformUI.getWorkbench();
            ImportPackageWizard wiz = new ImportPackageWizard();
            wiz.init(workbench, new StructuredSelection(getProject()));
            WizardDialog dlg = new WizardDialog(getSite().getShell(), wiz);
            if (dlg.open() != Dialog.CANCEL)
                toolboxWrapper.update();
        }
    }

    public void handleDelete() {
        ActivityImpl activityImpl = getSelection();
        if (activityImpl == null) {
            MessageDialog.openWarning(getSite().getShell(), "No Selection",
                    "Please select an Activity Implementor To Delete");
            return;
        }
        if (isDirty()) {
            MessageDialog.openWarning(getSite().getShell(), "Please Save",
                    "Please save or abandon outstanding changes before deleting");
            return;
        }
        else {
            new WorkflowElementActionHandler().delete(new WorkflowElement[] { activityImpl });
            toolboxWrapper.update();
            dirtyStateChanged(false);
            actionGroup.getDeleteAction().setEnabled(false);
        }
    }

    private DesignerProxy getDesignerProxy() {
        return toolboxWrapper.getDesignerProxy();
    }

    public WorkflowProject getProject() {
        return toolboxWrapper.getProject();
    }

    public static ToolboxView show() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            return (ToolboxView) page.showView("mdw.views.designer.toolbox");
        }
        catch (PartInitException ex) {
            PluginMessages.log(ex);
            return null;
        }
    }

    @Override
    public void setFocus() {
        toolboxWrapper.setFocus();
    }

    public WorkflowEditorPartListener getEditorListener(WorkflowProcess processVersion) {
        return new ProcessEditorListener(processVersion);
    }

    class ProcessEditorListener extends WorkflowEditorPartListener {
        ProcessEditorListener(WorkflowProcess processVersion) {
            super(processVersion);
        }

        @Override
        public void activated(WorkflowElementEditor editor) {
        }

        @Override
        public void closed(WorkflowElementEditor editor) {
        }

        @Override
        public void opened(WorkflowElementEditor editor) {
        }

        @Override
        public void deactivated(WorkflowElementEditor editor) {
        }

        @Override
        public void inputChanged(WorkflowElementEditor editor) {
        }

        @Override
        public void broughtToTop(WorkflowElementEditor editor) {
            updateToolboxWrapper((ProcessEditor) editor);
        }

        @Override
        public void hidden(WorkflowElementEditor editor) {
            if (!editor.isVisible())
                return; // already processed
            ToolboxWrapper toolboxWrapper = ToolboxWrapper.getInstance();
            if (toolboxWrapper.getFlowchartPage() != null) {
                toolboxWrapper.setFlowchartPage(null);
                toolboxWrapper.getSelectionProvider().clearSelection();

                setInput(toolboxWrapper);
            }
            editor.setVisible(false);
        }

        @Override
        public void visible(WorkflowElementEditor editor) {
            if (editor.isVisible())
                return; // already processed
            updateToolboxWrapper((ProcessEditor) editor);
            editor.setVisible(true);
        }

        private void updateToolboxWrapper(ProcessEditor editor) {
            ToolboxWrapper toolboxWrapper = ToolboxWrapper.getInstance();
            toolboxWrapper.setFlowchartPage(editor.getProcessCanvasWrapper().getFlowchartPage());
            toolboxWrapper.setProcess((WorkflowProcess) editor.getElement());
            setInput(toolboxWrapper);
        }
    }
}