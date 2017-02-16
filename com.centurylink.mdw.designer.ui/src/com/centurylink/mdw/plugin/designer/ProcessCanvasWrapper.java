/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.display.TextNote;
import com.centurylink.mdw.designer.pages.CanvasCommon;
import com.centurylink.mdw.designer.pages.DesignerCanvas;
import com.centurylink.mdw.designer.pages.DesignerPage.PersistType;
import com.centurylink.mdw.designer.pages.FlowchartPage;
import com.centurylink.mdw.designer.runtime.ProcessInstancePage;
import com.centurylink.mdw.designer.utils.GraphClipboard;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerResult;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.dialogs.ActivityInstanceDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ActivityInstanceDialog.Mode;
import com.centurylink.mdw.plugin.designer.dialogs.ExportAsDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ProcessSaveAsDialog;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.Note;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ArtifactEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.DocumentationEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.JavaEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.ScriptEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.TransformEditorValueProvider;
import com.centurylink.mdw.plugin.designer.storage.DocumentStorage;
import com.centurylink.mdw.plugin.designer.storage.StorageEditorInput;
import com.centurylink.mdw.plugin.designer.views.ProcessInstanceListView;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.swing.support.EmbeddedSwingComposite;

/**
 * Wraps the designer canvas swing component.
 */
public class ProcessCanvasWrapper extends DesignerPanelWrapper implements AWTEventListener {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public void setProcess(WorkflowProcess processVersion) {
        this.process = processVersion;
    }

    private WorkflowElement designerCanvasSelection;

    public WorkflowElement getCanvasSelection() {
        return designerCanvasSelection;
    }

    private ProcessInstancePage processInstancePage;

    public ProcessInstancePage getProcessInstancePage() {
        return processInstancePage;
    }

    public void setProcessInstancePage(ProcessInstancePage piPage) {
        this.processInstancePage = piPage;
    }

    private FlowchartPage flowchartPage;

    public FlowchartPage getFlowchartPage() {
        return flowchartPage;
    }

    public void setFlowchartPage(FlowchartPage flowchartPage) {
        this.flowchartPage = flowchartPage;
    }

    private WorkflowElementActionHandler actionHandler;

    private boolean dirty;
    private int zoomLevel = 100;
    private boolean allowInPlaceLabelEditing = false;
    private Color readOnlyBackgroundColor = new Color(248, 248, 248);

    public ProcessCanvasWrapper(Composite parent, WorkflowProcess processVersion) {
        super(parent);
        this.process = processVersion;
        this.flowchartPage = getDesignerProxy().newFlowchartPage();
        designerCanvasSelection = processVersion;
        actionHandler = new WorkflowElementActionHandler();
        Toolkit.getDefaultToolkit().addAWTEventListener(this,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.WINDOW_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        updateCanvasBackground();
    }

    /**
     * Creates the wrapped Swing component.
     * 
     * @param parent
     *            the parent SWT part
     * @return the SWT composite with the embedded Swing component
     */
    public EmbeddedSwingComposite createEmbeddedSwingComposite(Composite parent) {
        return new EmbeddedSwingComposite(parent, SWT.NONE) {
            @Override
            protected JComponent createSwingComponent() {
                CanvasCommon canvas = null;

                if (isInstance()) {
                    getFrame().setCursor(
                            java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                    canvas = getDesignerProxy().loadProcessInstance(getProcess(),
                            getProcessInstancePage());
                    getProcess().setReadOnly(
                            getProject().getPersistType() == WorkflowProject.PersistType.Database);
                    getFrame().setCursor(
                            java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
                else {
                    canvas = getDesignerProxy().loadProcess(getProcess(), getFlowchartPage());
                }

                // initialize canvas settings from preferences
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                String nodeIdType = prefsStore
                        .getString(PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE);
                if (!isInstance() && !nodeIdType.isEmpty() && !getNodeIdType().equals(nodeIdType))
                    setNodeIdType(nodeIdType);
                FlowchartPage.showtip = !prefsStore
                        .getBoolean(PreferenceConstants.PREFS_DESIGNER_SUPPRESS_TOOLTIPS);

                JScrollPane scrollPane = new JScrollPane(canvas);
                scrollPane.setHorizontalScrollBarPolicy(
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setVerticalScrollBarPolicy(
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                return scrollPane;
            }
        };
    }

    public CanvasCommon getCanvas() {
        if (isInstance())
            return getProcessInstancePage().canvas;
        else
            return getFlowchartPage().canvas;
    }

    public void loadProcess() {
        populate();
    }

    public void populate() {
        MdwSettings settings = MdwPlugin.getSettings();
        allowInPlaceLabelEditing = settings.isInPlaceLabelEditing();
        RGB rgb = settings.getReadOnlyBackground();
        readOnlyBackgroundColor = new Color(rgb.red, rgb.green, rgb.blue);

        if (isInstance())
            processInstancePage = getDesignerProxy().newProcessInstancePage();
        super.populate();

        designerCanvasSelection = getProcess();
        designerCanvasSelection.addDirtyStateListener(this);
    }

    public void refresh() {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    refreshCanvas();
                }
            });

            refreshActions();
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Refresh Instance", getProject());
        }
    }

    /**
     * Without PanelBusyIndicator.
     */
    public void refreshNoPbi() {
        refreshCanvas();
        refreshActions();
    }

    protected void refreshActions() {
        ProcessEditor processEditor = (ProcessEditor) MdwPlugin.getActivePage()
                .findEditor(getProcess());
        if (processEditor != null)
            processEditor.refreshActions();
    }

    /**
     * From AWT Event Thread.
     */
    protected void refreshCanvas() {
        if (isInstance()) {
            getDesignerProxy().loadProcessInstance(getProcess(), getProcessInstancePage());
            getProcessInstancePage().canvas.repaint();
        }
        else {
            getFlowchartPage().frame.getDataModel()
                    .removeProcessGraph(getFlowchartPage().getProcess());
            getDesignerProxy().loadProcess(getProcess(), getFlowchartPage());
            getFlowchartPage().canvas.repaint();
            updateCanvasBackground();
            getProcess().fireElementChangeEvent(ElementChangeEvent.ChangeType.PROPERTIES_CHANGE,
                    null);
        }
    }

    public WorkflowProject getProject() {
        return process.getProject();
    }

    public void repaint() {
        if (isInstance())
            getProcessInstancePage().canvas.repaint();
        else
            getFlowchartPage().canvas.repaint();
    }

    public void updateCanvasBackground() {
        if (isInstance() || (!getProcess().isReadOnly()
                && (getProcess().getProject().isFilePersist() || getProcess().isLockedToUser())))
            getFlowchartPage().canvas.setBackground(Color.WHITE);
        else
            getFlowchartPage().canvas.setBackground(readOnlyBackgroundColor);
    }

    public void setLinkStyle(String linkType) {
        getFlowchartPage().setLinkStyle(linkType);
    }

    public String getNodeIdType() {
        if (this.isInstance())
            return Node.ID_NONE;
        return getFlowchartPage().getProcess().getNodeIdType();
    }

    public void setNodeIdType(String nodeIdType) {
        String previous = getFlowchartPage().getProcess().getNodeIdType();
        if (!(previous.equals(nodeIdType))) {
            getFlowchartPage().getProcess().setNodeIdType(nodeIdType);
            repaint();
        }
    }

    public boolean isShowToolTips() {
        if (isInstance())
            return true;
        else
            return FlowchartPage.showtip;
    }

    public void setShowToolTips(boolean showToolTips) {
        boolean previous = FlowchartPage.showtip;
        FlowchartPage.showtip = showToolTips;
        if (!(previous == FlowchartPage.showtip))
            repaint();
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
        if (isInstance())
            getProcessInstancePage().setZoomLevel(zoomLevel);
        else
            getFlowchartPage().setZoomLevel(zoomLevel);
    }

    public void setNodeStyle(String nodeType) {
        getFlowchartPage().setNodeStyle(nodeType);
    }

    public void exportAs(Shell shell) {
        ExportAsDialog exportAsDialog = new ExportAsDialog(shell, getProcess(), flowchartPage);
        exportAsDialog.open();
    }

    public void remove() {
        if (!isInstance()) {
            // TODO remove old designer data model
            DesignerDataModel model = getProject().getDataAccess().getDesignerDataModel();
            Graph graph = model.findProcessGraph(getProcess().getProcessVO());
            if (graph != null)
                model.removeProcessGraph(graph);
            getProcess().revert();
        }

        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        if (designerCanvasSelection != null)
            designerCanvasSelection.removeDirtyStateListener(this);
    }

    public String getLabel() {
        return getProcess().getLabel();
    }

    public void saveProcess(PersistType persistType, int version) {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean validate = prefsStore
                .getBoolean(PreferenceConstants.PREFS_ENFORCE_PROCESS_VALIDATION_RULES);
        boolean keepLocked = prefsStore
                .getBoolean(PreferenceConstants.PREFS_KEEP_PROCESSES_LOCKED_WHEN_SAVING);

        // save the process
        getDesignerProxy().saveProcessWithProgress(getProcess(), getFlowchartPage(), persistType,
                version, validate, keepLocked);
        dirtyStateChanged(getFlowchartPage().getProcess().dirtyLevel > 0);

        if (persistType.equals(PersistType.NEW_VERSION))
            getProcess().fireElementChangeEvent(ChangeType.VERSION_CHANGE,
                    getProcess().getVersionString());

        // so that we don't lose edits to previous canvas selections
        setEditorFocus();
        designerCanvasSelection = getProcess();
        getSelectionProvider().setSelection(designerCanvasSelection);

        getProcess().sync();

        updateCanvasBackground(); // reflect locking changes
    }

    public RunnerResult forceUpdateProcess() {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean validate = prefsStore
                .getBoolean(PreferenceConstants.PREFS_ENFORCE_PROCESS_VALIDATION_RULES);
        boolean keepLocked = prefsStore
                .getBoolean(PreferenceConstants.PREFS_KEEP_PROCESSES_LOCKED_WHEN_SAVING);

        RunnerResult result = getDesignerProxy().forceUpdateProcessWithProgress(getProcess(),
                getFlowchartPage(), validate, keepLocked);

        dirtyStateChanged(getFlowchartPage().getProcess().dirtyLevel > 0);

        // so that we don't lose edits to previous canvas selections
        setEditorFocus();
        designerCanvasSelection = getProcess();
        getSelectionProvider().setSelection(designerCanvasSelection);

        getProcess().sync();

        if (result.getStatus() == RunnerStatus.SUCCESS)
            updateCanvasBackground();

        return result;
    }

    public void cutSelection() {
        DesignerCanvas canvas = getFlowchartPage() == null ? null : getFlowchartPage().canvas;
        if (canvas != null) {
            // simulate ctrl-x
            // KeyEvent e = new KeyEvent(canvas, KeyEvent.KEY_RELEASED,
            // System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK,
            // KeyEvent.VK_X, (char)0x78);
            // getFlowchartPage().canvas.keyPressed(e);

            // since designer canvas has nonstandard mnemonics for ctrl-x, use
            // the following
            copySelection();
            deleteSelection();
        }
    }

    public void copySelection() {
        DesignerCanvas canvas = getFlowchartPage() == null ? null : getFlowchartPage().canvas;
        if (canvas != null) {
            // simulate ctrl-c
            KeyEvent e = new KeyEvent(canvas, KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                    KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_C, (char) 0x63);
            getFlowchartPage().canvas.keyReleased(e);
        }
    }

    public void addNote(int x, int y) {
        TextNote note = getFlowchartPage().getProcess().addTextNote(x, y, "");
        getFlowchartPage().canvas.setSelectedObject(note);
        getFlowchartPage().canvas.requestFocus();
        getFlowchartPage().canvas.repaint();
    }

    public void pasteSelection() {
        DesignerCanvas canvas = getFlowchartPage() == null ? null : getFlowchartPage().canvas;
        if (canvas != null) {
            // simulate ctrl-v
            KeyEvent e = new KeyEvent(canvas, KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                    KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_V, (char) 0x76);
            getFlowchartPage().canvas.keyReleased(e);
        }
    }

    public void deleteSelection() {
        DesignerCanvas canvas = getFlowchartPage() == null ? null : getFlowchartPage().canvas;
        if (canvas != null) {
            // simulate del pressed
            KeyEvent e = new KeyEvent(canvas, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                    KeyEvent.VK_DELETE, (char) 0x7f);
            getFlowchartPage().canvas.keyPressed(e);
        }
    }

    public void saveProcessAs() {
        ProcessSaveAsDialog newProcessDialog = new ProcessSaveAsDialog(getParent().getShell(),
                getProcess());
        if (newProcessDialog.open() != Dialog.OK)
            return; // cancelled

        String newName = newProcessDialog.getNewName();
        WorkflowPackage newPkg = getProject().getDefaultPackage();
        if (!StringHelper.isEmpty(newProcessDialog.getPackageName()))
            newPkg = getProject().getPackage(newProcessDialog.getPackageName());

        flowchartPage.getProcess().blankOutTaskLogicalId();
        flowchartPage.getProcess().save_temp_vars();

        getDesignerProxy().saveProcessAs(getProcess(), newPkg, newName, false);
        WorkflowProcess newProc = getProject().getProcess(newName);
        if (newProc != null) {
            // update the process explorer tree
            newProc.sync();
            newProc.addElementChangeListener(getProject());
            newProc.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newProc);

            // update the canvas editor
            openProcessDefinition(newProc);
            IWorkbenchPage page = MdwPlugin.getActivePage();
            if (page != null) {
                ProcessEditor processEditor = (ProcessEditor) page.findEditor(getProcess());
                if (processEditor != null)
                    page.closeEditor(processEditor, false);
            }
        }
    }

    /**
     * Handles Swing events (running on the AWT UI thread).
     */
    public void eventDispatched(AWTEvent event) {
        // check for relevance and consistency
        if (isInstance()) {
            if (getProcessInstancePage() == null
                    || !event.getSource().equals(getProcessInstancePage().canvas)
                    || getProcessInstancePage().getProcess() == null)
                return;
        }
        else {
            if (!event.getSource().equals(getFlowchartPage().canvas)
                    || getFlowchartPage().getProcess() == null)
                return;
        }

        if (event.getID() == MouseEvent.MOUSE_RELEASED) {
            final MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.isPopupTrigger()
                    || (PluginUtil.isMac() && mouseEvent.getButton() == 3)) {
                getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        final Menu menu = buildPopupMenu(mouseEvent.getX(), mouseEvent.getY());
                        if (menu != null) {
                            getEmbeddedSwingComposite().setMenu(menu);
                            menu.setVisible(true);
                        }
                    }
                });
                mouseEvent.consume();
            }
            if (!isInstance() && !allowInPlaceLabelEditing
                    && (getFlowchartPage().canvas.getAnchor() == -2
                            || designerCanvasSelection instanceof Note))
                mouseEvent.consume(); // no in-place editing unless specified
        }
        else if (event.getID() == MouseEvent.MOUSE_CLICKED) {
            // don't propagate double-click events
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.getClickCount() > 1)
                mouseEvent.consume();
        }
        else if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
            MouseEvent mouseEvent = (MouseEvent) event;
            // ignore drag events for instances and read-only
            if (isInstance() || (process.isReadOnly() && flowchartPage.canvas.getMarquee() == null))
                mouseEvent.consume();
        }
        else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            // set the current selection
            MouseEvent mouseEvent = (MouseEvent) event;
            final boolean doubleClick = mouseEvent.getClickCount() > 1;

            if (doubleClick)
                mouseEvent.consume(); // don't select twice
            else
                handleSelection(mouseEvent);

            if (!isInstance() && !allowInPlaceLabelEditing
                    && getFlowchartPage().canvas.getAnchor() == -2)
                mouseEvent.consume(); // no in-place editing unless specified

            final boolean isSubProcessActivity = designerCanvasSelection instanceof Activity
                    && ((Activity) designerCanvasSelection).isSubProcessInvoke();
            final boolean isScriptActivity = designerCanvasSelection instanceof Activity
                    && ((Activity) designerCanvasSelection).isScript();

            getDisplay().asyncExec(new Runnable() {
                public void run() {
                    setEditorFocus();
                    getSelectionProvider().setSelection(designerCanvasSelection);
                    if (doubleClick) {
                        showPropertiesView();
                        if (!isInstance() && MdwPlugin.getDefault().getPreferenceStore().getBoolean(
                                PreferenceConstants.PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS)) {
                            if (isSubProcessActivity) {
                                Activity subProcessActivity = (Activity) designerCanvasSelection;
                                openSubProcess(subProcessActivity);
                            }
                            else if (isScriptActivity) {
                                Activity scriptActivity = (Activity) designerCanvasSelection;
                                openScript(scriptActivity);
                            }
                        }
                    }
                }
            });
        }
        else if (event.getID() == KeyEvent.KEY_PRESSED && !allowInPlaceLabelEditing) {
            KeyEvent keyEvent = (KeyEvent) event;

            int keycode = keyEvent.getKeyCode();
            if (keycode != KeyEvent.VK_DELETE && keycode != KeyEvent.VK_UP
                    && keycode != KeyEvent.VK_DOWN && keycode != KeyEvent.VK_LEFT
                    && keycode != KeyEvent.VK_RIGHT) {
                keyEvent.consume();
            }
        }
        else if (event.getID() == KeyEvent.KEY_RELEASED) {
            KeyEvent keyEvent = (KeyEvent) event;

            // override ctrl-x since canvas uses non-standard mnemonics
            if (keyEvent.isControlDown() && keyEvent.getKeyCode() == KeyEvent.VK_X) {
                cutSelection();
                keyEvent.consume();
            }
        }

        if (!isInstance()) {
            // check for effect on dirtiness
            boolean newDirty = getFlowchartPage().getProcess().dirtyLevel > 0;
            if (newDirty != dirty) {
                dirty = newDirty;
                getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        fireDirtyStateChanged(dirty);
                    }
                });
            }
        }
    }

    private void handleSelection(MouseEvent mouseEvent) {
// if (designerCanvasSelection != null)
// designerCanvasSelection.removeDirtyStateListener(this);

        Graph process = null;
        if (isInstance())
            process = getProcessInstancePage().getProcess();
        else
            process = getFlowchartPage().getProcess();

        int mouseX = mouseEvent.getX();
        int mouseY = mouseEvent.getY();
        if (process.zoom != 100) {
            mouseX = mouseX * 100 / process.zoom;
            mouseY = mouseY * 100 / process.zoom;
        }

        Object obj = null;
        if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED) {
            if (isInstance())
                obj = getProcessInstancePage().canvas.objectAt(process, mouseX, mouseY,
                        getProcessInstancePage().canvas.getGraphics());
            else
                obj = getFlowchartPage().canvas.objectAt(process, mouseX, mouseY,
                        getFlowchartPage().canvas.getGraphics());
        }
        else {
            obj = process.objectAt(mouseX, mouseY,
                    isInstance() ? getProcessInstancePage().canvas.getGraphics()
                            : getFlowchartPage().canvas.getGraphics());
        }

        if (obj != null) {
            DesignerProxy designerProxy = getProcess().getProject().getDesignerProxy();
            // create the appropriate DesignerCanvasSelection
            if (obj instanceof Node) {
                ActivityImpl actImpl = getProcess().getProject()
                        .getActivityImpl(((Node) obj).nodet.getImplementorClassName());
                Activity activity = new Activity((Node) obj, getProcess(), actImpl);
                if (isInstance()) {
                    activity.setProcessInstance(getProcess().getProcessInstance());
                    List<ActivityInstanceVO> activityInstances = getProcess().getProcessInstance()
                            .getActivityInstances(activity.getId());
                    if (activityInstances.size() == 0
                            && getProcess().getEmbeddedSubProcessInstances() != null) {
                        // try embedded subprocess instances
                        for (ProcessInstanceVO embeddedSubProcessInstance : getProcess()
                                .getEmbeddedSubProcessInstances())
                            activityInstances.addAll(embeddedSubProcessInstance
                                    .getActivityInstances(activity.getId()));
                    }
                    activity.setInstances(activityInstances);
                    if (activity.isManualTask()) {
                        activity.setTaskInstances(
                                getProcess().getMainTaskInstances(activity.getId()));
                        activity.setSubTaskInstances(
                                getProcess().getSubTaskInstances(activity.getId()));
                    }
                    if (activity.isSubProcessInvoke()) {
                        // TODO: load subprocess instances when process instance
                        // id loaded (like manual task instances above)
                        List<ProcessInstanceVO> subProcessInstances = designerProxy
                                .getSubProcessInstances(getProcess(), activity);
                        if (subProcessInstances.size() == 0
                                && getProcess().getEmbeddedSubProcessInstances() != null) {
                            for (ProcessInstanceVO embeddedSubProcessInstance : getProcess()
                                    .getEmbeddedSubProcessInstances())
                                subProcessInstances.addAll(designerProxy.getSubProcessInstances(
                                        embeddedSubProcessInstance, activity));
                        }
                        activity.setSubProcessInstances(subProcessInstances);
                    }
                }
                else {
                    if (activity.getLogicalId() == null)
                        activity.setLogicalId(process.generateLogicalId("A"));
                    if (Node.ID_SEQUENCE.equals(getNodeIdType()) && activity.getSequenceId() == 0)
                        process.assignSequenceIds();
                }
                designerCanvasSelection = activity;
            }
            else if (obj instanceof Link) {
                Transition transition = new Transition((Link) obj, getProcess());
                if (isInstance())
                    transition.setInstances(getProcess().getProcessInstance()
                            .getTransitionInstances(transition.getId()));
                designerCanvasSelection = transition;
            }
            else if (obj instanceof SubGraph) {
                EmbeddedSubProcess embeddedSubProcess = new EmbeddedSubProcess((SubGraph) obj,
                        getProcess());
                if (isInstance()) {
                    embeddedSubProcess.setSubProcessInstances(((SubGraph) obj).getInstances());
                }
                designerCanvasSelection = embeddedSubProcess;
            }
            else if (obj instanceof TextNote) {
                TextNote textNote = (TextNote) obj;
                Note note = new Note(textNote, getProcess());
                designerCanvasSelection = note;
            }
            else {
                designerCanvasSelection = getProcess();
            }
        }
        else {
            designerCanvasSelection = getProcess();
        }
        designerCanvasSelection.addDirtyStateListener(this);
    }

    /**
     * Needs to run on the SWT UI thread.
     */
    private Menu buildPopupMenu(final int x, final int y) {
        if (popupMenu != null)
            popupMenu.dispose();

        popupMenu = new Menu(getParent().getShell(), SWT.POP_UP);

        if (!isInstance()) {
            if (designerCanvasSelection instanceof WorkflowProcess) {
                if (process.getPackage() != null
                        && process.isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                    // save as
                    MenuItem saveAsItem = new MenuItem(popupMenu, SWT.PUSH);
                    saveAsItem.setText("Save as...");
                    ImageDescriptor propsImageDescForSave = MdwPlugin
                            .getImageDescriptor("icons/save.gif");
                    saveAsItem.setImage(propsImageDescForSave.createImage());
                    saveAsItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            saveProcessAs();
                        }
                    });
                }

                MenuItem exportAsItem = new MenuItem(popupMenu, SWT.PUSH);
                exportAsItem.setText("Export as...");
                ImageDescriptor exportAsDesc = MdwPlugin.getImageDescriptor("icons/export_as.gif");
                exportAsItem.setImage(exportAsDesc.createImage());
                exportAsItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        exportAs(ProcessCanvasWrapper.this.getDisplay().getActiveShell());
                    }
                });
            }
            if (!process.isReadOnly()) {
                if (designerCanvasSelection instanceof WorkflowProcess) {
                    if (process.isInRuleSet()) {
                        // notes
                        MenuItem noteItem = new MenuItem(popupMenu, SWT.PUSH);
                        noteItem.setText("Add Note");
                        ImageDescriptor noteImageDesc = MdwPlugin
                                .getImageDescriptor("icons/doc.gif");
                        noteItem.setImage(noteImageDesc.createImage());
                        noteItem.addSelectionListener(new SelectionAdapter() {
                            public void widgetSelected(SelectionEvent e) {
                                addNote(x, y);
                            }
                        });
                    }

                    // paste
                    MenuItem pasteItem = new MenuItem(popupMenu, SWT.PUSH);
                    pasteItem.setText("Paste\tCtrl+V");
                    ImageDescriptor pasteImageDesc = MdwPlugin
                            .getImageDescriptor("icons/paste.gif");
                    pasteItem.setImage(pasteImageDesc.createImage());
                    pasteItem.setEnabled(GraphClipboard.getInstance().get() != null);
                    pasteItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            pasteSelection();
                        }
                    });
                }
                else {
                    // cut
                    MenuItem cutItem = new MenuItem(popupMenu, SWT.PUSH);
                    cutItem.setText("Cut\tCtrl+X");
                    ImageDescriptor cutImageDesc = MdwPlugin.getImageDescriptor("icons/cut.gif");
                    cutItem.setImage(cutImageDesc.createImage());
                    cutItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            cutSelection();
                        }
                    });
                }
            }

            if (!(designerCanvasSelection instanceof WorkflowProcess)) {
                // copy
                MenuItem copyItem = new MenuItem(popupMenu, SWT.PUSH);
                copyItem.setText("Copy\tCtrl+C");
                ImageDescriptor copyImageDesc = MdwPlugin.getImageDescriptor("icons/copy.gif");
                copyItem.setImage(copyImageDesc.createImage());
                copyItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        copySelection();
                    }
                });

                // delete
                if (!process.isReadOnly()) {
                    MenuItem deleteItem = new MenuItem(popupMenu, SWT.PUSH);
                    deleteItem.setText("Delete\tDel");
                    ImageDescriptor deleteImageDesc = MdwPlugin
                            .getImageDescriptor("icons/delete.gif");
                    deleteItem.setImage(deleteImageDesc.createImage());
                    deleteItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            deleteSelection();
                        }
                    });
                }
            }
        }

        // properties
        MenuItem propsItem = new MenuItem(popupMenu, SWT.PUSH);
        propsItem.setText("Show Properties");
        ImageDescriptor propsImageDesc = MdwPlugin.getImageDescriptor("icons/properties.gif");
        propsItem.setImage(propsImageDesc.createImage());
        propsItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showPropertiesView();
            }
        });

        // documentation
        if (!isInstance()) {
            MenuItem docsItem = new MenuItem(popupMenu, SWT.PUSH);
            docsItem.setText("Documentation");
            ImageDescriptor docsImageDesc = MdwPlugin.getImageDescriptor("icons/word.gif");
            docsItem.setImage(docsImageDesc.createImage());
            docsItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openDocumentation();
                }
            });
        }

        // process definition xml
        if (!isInstance() && (designerCanvasSelection instanceof WorkflowProcess)
                && getProcess().isInRuleSet()) {
            MenuItem defXmlItem = new MenuItem(popupMenu, SWT.PUSH);
            defXmlItem.setText("Definition Doc");
            ImageDescriptor docsImageDesc = MdwPlugin.getImageDescriptor("icons/doc.gif");
            defXmlItem.setImage(docsImageDesc.createImage());
            defXmlItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openDefinitionXml();
                }
            });
        }

        // open subprocess
        boolean isSubProcessActivity = designerCanvasSelection instanceof Activity
                && ((Activity) designerCanvasSelection).isSubProcessInvoke();
        if (!isInstance() && isSubProcessActivity) {
            final Activity subProcessActivity = (Activity) designerCanvasSelection;
            if (!subProcessActivity.isHeterogeneousSubProcInvoke()) {
                MenuItem subProcItem = new MenuItem(popupMenu, SWT.PUSH);
                subProcItem.setText("Open Subprocess");
                ImageDescriptor subProcImageDesc = MdwPlugin
                        .getImageDescriptor("icons/designer.gif");
                subProcItem.setImage(subProcImageDesc.createImage());
                subProcItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        openSubProcess(subProcessActivity);
                    }
                });
            }
        }

        // edit script
        boolean isScript = designerCanvasSelection instanceof Activity
                && ((Activity) designerCanvasSelection).isScript();
        if (!isInstance() && isScript) {
            final Activity scriptActivity = (Activity) designerCanvasSelection;
            MenuItem editScriptItem = new MenuItem(popupMenu, SWT.PUSH);
            String artifactTitle = "Script";
            if (scriptActivity.isRule())
                artifactTitle = "Rule";
            else if (TransformEditorValueProvider.isTransformActivity(scriptActivity))
                artifactTitle = "Transform";
            else if (scriptActivity.getAttribute("Rule") != null)
                artifactTitle += " *";
            if (process.isReadOnly())
                editScriptItem.setText("View " + artifactTitle);
            else
                editScriptItem.setText("Edit " + artifactTitle);
            ImageDescriptor scriptImageDesc = MdwPlugin.getImageDescriptor("icons/script.gif");
            editScriptItem.setImage(scriptImageDesc.createImage());
            editScriptItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openScript(scriptActivity);
                }
            });
        }

        // edit java
        boolean isJava = designerCanvasSelection instanceof Activity
                && ((Activity) designerCanvasSelection).isDynamicJava();
        if (!isInstance() && isJava) {
            final Activity javaActivity = (Activity) designerCanvasSelection;
            MenuItem editJavaItem = new MenuItem(popupMenu, SWT.PUSH);
            String artifactTitle = "Java";
            if (process.isReadOnly())
                editJavaItem.setText("View " + artifactTitle);
            else
                editJavaItem.setText("Edit " + artifactTitle);
            ImageDescriptor javaImageDesc = MdwPlugin.getImageDescriptor("icons/java.gif");
            editJavaItem.setImage(javaImageDesc.createImage());
            editJavaItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openJava(javaActivity);
                }
            });
        }

        // open task instance or process instance
        if (isInstance() && designerCanvasSelection instanceof Activity) {
            final Activity activity = (Activity) designerCanvasSelection;
            if (activity.isManualTask()) {
                if (activity.getTaskInstances() != null && !activity.getTaskInstances().isEmpty()) {
                    MenuItem viewTaskItem = new MenuItem(popupMenu, SWT.PUSH);
                    viewTaskItem.setText("View Task");
                    viewTaskItem.setImage(
                            MdwPlugin.getImageDescriptor("icons/taskmgr.gif").createImage());
                    viewTaskItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            TaskInstanceVO taskInstanceVO = null;
                            for (TaskInstanceVO taskInstance : activity.getTaskInstances()) {
                                if (taskInstanceVO == null || taskInstance
                                        .getTaskInstanceId() > taskInstanceVO.getTaskInstanceId())
                                    taskInstanceVO = taskInstance;
                            }
                            boolean assigned = activity.getProject().getUser().getUsername()
                                    .equals(taskInstanceVO.getTaskClaimUserCuid());
                            String taskInstParams = activity.getProject().getTaskInstancePath(
                                    taskInstanceVO.getTaskInstanceId(), assigned);
                            WorkflowPackage packageVersion = activity.getPackage();
                            String packageParam = packageVersion.isDefaultPackage() ? ""
                                    : "&packageName=" + packageVersion.getName();
                            WebApp webapp = activity.getProject().checkRequiredVersion(5, 5)
                                    ? WebApp.MdwHub : WebApp.TaskManager;
                            WebLaunchActions.getLaunchAction(activity.getProject(), webapp)
                                    .launch(activity.getProject(), taskInstParams + packageParam);
                        }
                    });
                }
            }
            else if (activity.isSubProcessInvoke()) {
                if (activity.getSubProcessInstances() != null
                        && !activity.getSubProcessInstances().isEmpty()
                        && activity.getSubProcessInstances().size() < 50) {
                    MenuItem subMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                    subMenuItem.setText("SubProcesses");
                    subMenuItem.setImage(
                            MdwPlugin.getImageDescriptor("icons/processfolder.gif").createImage());
                    Menu subMenu = new Menu(subMenuItem);
                    subMenuItem.setMenu(subMenu);
                    for (final ProcessInstanceVO subProcInst : activity.getSubProcessInstances()) {
                        MenuItem subProcItem = new MenuItem(subMenu, SWT.PUSH);
                        subProcItem.setText("Instance " + subProcInst.getId());
                        subProcItem.setImage(
                                MdwPlugin.getImageDescriptor("icons/process.gif").createImage());
                        subProcItem.addSelectionListener(new SelectionAdapter() {
                            public void widgetSelected(SelectionEvent e) {
                                openProcessInstance(subProcInst);
                            }
                        });
                    }
                }
            }
        }

        // launch process, lock, show instances, find calling processes
        if (!isInstance() && designerCanvasSelection instanceof WorkflowProcess) {
            if (designerCanvasSelection.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)) {
                MenuItem runItem = new MenuItem(popupMenu, SWT.PUSH);
                runItem.setText("Run...");
                ImageDescriptor runImageDesc = MdwPlugin.getImageDescriptor("icons/run.gif");
                runItem.setImage(runImageDesc.createImage());
                runItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        launchProcess((WorkflowProcess) designerCanvasSelection, false);
                    }
                });
                MenuItem runPageItem = new MenuItem(popupMenu, SWT.PUSH);
                runPageItem.setText("Run Start Page...");
                ImageDescriptor runPageImageDesc = MdwPlugin.getImageDescriptor("icons/run.gif");
                runPageItem.setImage(runPageImageDesc.createImage());
                runPageItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        launchProcessPage((WorkflowProcess) designerCanvasSelection);
                    }
                });

                if (!MdwPlugin.isRcp()) {
                    MenuItem debugItem = new MenuItem(popupMenu, SWT.PUSH);
                    debugItem.setText("Debug...");
                    ImageDescriptor debugImageDesc = MdwPlugin
                            .getImageDescriptor("icons/debug.gif");
                    debugItem.setImage(debugImageDesc.createImage());
                    debugItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            launchProcess((WorkflowProcess) designerCanvasSelection, true);
                        }
                    });
                }
            }

            if (isLockAllowed()) {
                MenuItem lockItem = new MenuItem(popupMenu, SWT.PUSH);
                lockItem.setText(getProcess().isLockedToUser() ? "Unlock Process" : "Lock Process");
                ImageDescriptor lockImageDesc = MdwPlugin.getImageDescriptor("icons/lock.gif");
                lockItem.setImage(lockImageDesc.createImage());
                lockItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        toggleProcessLock(
                                !((WorkflowProcess) designerCanvasSelection).isLockedToUser());
                    }
                });
            }

            if (getProcess().getProject().isPureMdw52()) {
                MenuItem callersItem = new MenuItem(popupMenu, SWT.PUSH);
                callersItem.setText("Find Calling Processes");
                ImageDescriptor callersImageDesc = MdwPlugin
                        .getImageDescriptor("icons/process.gif");
                callersItem.setImage(callersImageDesc.createImage());
                callersItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        actionHandler.findCallers(designerCanvasSelection);
                    }
                });

                if (getProject().isFilePersist()) {
                    MenuItem hierarchyItem = new MenuItem(popupMenu, SWT.PUSH);
                    hierarchyItem.setText("Process Hierarchy");
                    ImageDescriptor hierarchyImageDesc = MdwPlugin
                            .getImageDescriptor("icons/hierarchy.gif");
                    hierarchyItem.setImage(hierarchyImageDesc.createImage());
                    hierarchyItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            actionHandler.showHierarchy(designerCanvasSelection);
                        }
                    });
                }
            }

            MenuItem instancesItem = new MenuItem(popupMenu, SWT.PUSH);
            instancesItem.setText("View Instances");
            ImageDescriptor instancesImageDesc = MdwPlugin.getImageDescriptor("icons/list.gif");
            instancesItem.setImage(instancesImageDesc.createImage());
            instancesItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    showProcessInstances((WorkflowProcess) designerCanvasSelection);
                }
            });
        }

        // launch activity
        if (!isInstance() && designerCanvasSelection instanceof Activity) {
            if (((Activity) designerCanvasSelection).getProcess()
                    .isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)) {
                MenuItem runItem = new MenuItem(popupMenu, SWT.PUSH);
                runItem.setText("Run from Here...");
                ImageDescriptor runImageDesc = MdwPlugin.getImageDescriptor("icons/run.gif");
                runItem.setImage(runImageDesc.createImage());
                runItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        launchProcess((Activity) designerCanvasSelection, false);
                    }
                });

                if (!MdwPlugin.isRcp()) {
                    MenuItem debugItem = new MenuItem(popupMenu, SWT.PUSH);
                    debugItem.setText("Debug from Here...");
                    ImageDescriptor debugImageDesc = MdwPlugin
                            .getImageDescriptor("icons/debug.gif");
                    debugItem.setImage(debugImageDesc.createImage());
                    debugItem.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            launchProcess((Activity) designerCanvasSelection, true);
                        }
                    });
                }
            }
        }

        // refresh
        if (designerCanvasSelection instanceof WorkflowProcess) {
            MenuItem refreshItem = new MenuItem(popupMenu, SWT.PUSH);
            refreshItem.setText("Refresh");
            ImageDescriptor refreshImageDesc = MdwPlugin.getImageDescriptor("icons/refresh.gif");
            refreshItem.setImage(refreshImageDesc.createImage());
            refreshItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    refresh();
                }
            });
        }

        // process definition and open calling process instance
        if (isInstance() && designerCanvasSelection instanceof WorkflowProcess) {
            final WorkflowProcess processInstVersion = (WorkflowProcess) designerCanvasSelection;

            MenuItem procDefItem = new MenuItem(popupMenu, SWT.PUSH);
            procDefItem.setText("Process Definition");
            ImageDescriptor procDefImageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
            procDefItem.setImage(procDefImageDesc.createImage());
            procDefItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openProcessDefinition(new WorkflowProcess(processInstVersion));
                }
            });

            if (OwnerType.PROCESS_INSTANCE
                    .equals(processInstVersion.getProcessInstance().getOwner())) {
                MenuItem callingInstanceItem = new MenuItem(popupMenu, SWT.PUSH);
                callingInstanceItem.setText("Calling Process Instance");
                ImageDescriptor callerImageDesc = MdwPlugin.getImageDescriptor("icons/launch.gif");
                callingInstanceItem.setImage(callerImageDesc.createImage());
                callingInstanceItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        openCallingInstance();
                    }
                });
            }

            MenuItem instanceHierarchyItem = new MenuItem(popupMenu, SWT.PUSH);
            instanceHierarchyItem.setText("Instance Hierarchy");
            ImageDescriptor hierarchyImageDesc = MdwPlugin
                    .getImageDescriptor("icons/hierarchy.gif");
            instanceHierarchyItem.setImage(hierarchyImageDesc.createImage());
            instanceHierarchyItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    actionHandler.showHierarchy(designerCanvasSelection);
                }
            });
        }

        // retry and skip
        if (isInstance() && designerCanvasSelection instanceof Activity
                && ((Activity) designerCanvasSelection).hasInstanceInfo()
                && ((Activity) designerCanvasSelection).getProcess()
                        .isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)) {
            MenuItem retryItem = new MenuItem(popupMenu, SWT.PUSH);
            retryItem.setText("Retry");
            ImageDescriptor retryImageDesc = MdwPlugin.getImageDescriptor("icons/retry.gif");
            retryItem.setImage(retryImageDesc.createImage());
            retryItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
                    try {
                        pbi.busyWhile(new Runnable() {
                            public void run() {
                                Activity activity = (Activity) designerCanvasSelection;
                                Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
                                ActivityInstanceVO actInstVO = activity.getInstances()
                                        .get(activity.getInstances().size() - 1);
                                ActivityInstanceDialog actInstDlg = new ActivityInstanceDialog(
                                        shell, activity, actInstVO, Mode.RETRY);
                                actInstDlg.open();
                            }
                        });
                    }
                    catch (InvocationTargetException ex) {
                        PluginMessages.uiError(ex, "Retry Activity", getProject());
                    }
                }
            });

            MenuItem skipItem = new MenuItem(popupMenu, SWT.PUSH);
            skipItem.setText("Proceed");
            ImageDescriptor skipImageDesc = MdwPlugin.getImageDescriptor("icons/skip.gif");
            skipItem.setImage(skipImageDesc.createImage());
            skipItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
                    try {
                        pbi.busyWhile(new Runnable() {
                            public void run() {
                                Activity activity = (Activity) designerCanvasSelection;
                                Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
                                ActivityInstanceVO actInstVO = activity.getInstances()
                                        .get(activity.getInstances().size() - 1);
                                ActivityInstanceDialog actInstDlg = new ActivityInstanceDialog(
                                        shell, activity, actInstVO, Mode.SKIP);
                                actInstDlg.open();
                            }
                        });
                    }
                    catch (InvocationTargetException ex) {
                        PluginMessages.uiError(ex, "Skip Activity", getProject());
                    }
                }
            });
        }

        // view owning document
        if (isInstance() && designerCanvasSelection instanceof WorkflowProcess
                && (OwnerType.DOCUMENT.equals(getProcess().getProcessInstance().getOwner())
                        || OwnerType.TESTER.equals(getProcess().getProcessInstance().getOwner()))) {
            MenuItem docItem = new MenuItem(popupMenu, SWT.PUSH);
            docItem.setText("View Owning Document");
            ImageDescriptor docImageDesc = MdwPlugin.getImageDescriptor("icons/doc.gif");
            docItem.setImage(docImageDesc.createImage());
            docItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
                    try {
                        pbi.busyWhile(new Runnable() {

                            public void run() {
                                IStorage storage = new DocumentStorage(getProject(),
                                        new DocumentReference(
                                                getProcess().getProcessInstance().getOwnerId(),
                                                null));
                                final IStorageEditorInput input = new StorageEditorInput(storage);
                                final IWorkbenchPage page = MdwPlugin.getActivePage();
                                if (page != null) {
                                    try {
                                        page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
                                    }
                                    catch (PartInitException ex) {
                                        PluginMessages.uiError(ex, "View Document", getProject());
                                    }
                                }
                            }
                        });
                    }
                    catch (InvocationTargetException ex) {
                        PluginMessages.uiError(ex, "View Document", getProject());
                    }
                }
            });
        }

        return popupMenu;
    }

    public DesignerProxy getDesignerProxy() {
        return getProcess().getProject().getDesignerProxy();
    }

    public boolean isInstance() {
        return getProcess().hasInstanceInfo();
    }

    private void openSubProcess(final Activity subprocessActivity) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    WorkflowProject workflowProject = subprocessActivity.getProject();

                    String subProcName = subprocessActivity
                            .getAttribute(WorkAttributeConstant.PROCESS_NAME);
                    if (subProcName == null) {
                        MessageDialog.openError(getDisplay().getActiveShell(), "Open Subprocess",
                                "Please select a subprocess");
                        return;
                    }
                    String subProcVer = subprocessActivity
                            .getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                    WorkflowProcess subProcess = new AssetLocator(subprocessActivity,
                            AssetLocator.Type.Process).getProcessVersion(
                                    new AssetVersionSpec(subProcName, subProcVer));
                    if (subProcess == null) {
                        PluginMessages.uiError(getDisplay().getActiveShell(),
                                "SubProcess not found: " + subProcName + " v" + subProcVer,
                                "Open SubProcess", workflowProject);
                        return;
                    }

                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage();
                    try {
                        page.openEditor(subProcess, "mdw.editors.process");
                    }
                    catch (PartInitException ex) {
                        PluginMessages.uiError(getDisplay().getActiveShell(), ex, "Open SubProcess",
                                workflowProject);
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open SubProcess", getProject());
        }
    }

    private void openProcessInstance(ProcessInstanceVO processInstanceInfo) {
        // create a new instance for a new editor
        ProcessVO subprocess = new ProcessVO();
        subprocess.setProcessId(processInstanceInfo.getProcessId());

        try {
            ProcessVO def = getDesignerProxy().getDesignerDataAccess()
                    .getProcessDefinition(subprocess.getProcessId());
            subprocess.setProcessName(def.getName());
            subprocess.setVersion(def.getVersion());
            WorkflowProcess toOpen = new WorkflowProcess(getProject(), subprocess);
            toOpen.setPackage(getProject().getProcessPackage(subprocess.getId()));
            toOpen.setProcessInstance(processInstanceInfo);
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage();
            page.openEditor(toOpen, "mdw.editors.process");
        }
        catch (Exception ex) {
            PluginMessages.uiError(getDisplay().getActiveShell(), ex, "Open SubProcess Instances",
                    getProject());
        }
    }

    private void openScript(final Activity scriptOrRuleActivity) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    ArtifactEditorValueProvider artifactValueProvider = null;
                    if (TransformEditorValueProvider.isTransformActivity(scriptOrRuleActivity))
                        artifactValueProvider = new TransformEditorValueProvider(
                                scriptOrRuleActivity);
                    else
                        artifactValueProvider = new ScriptEditorValueProvider(scriptOrRuleActivity);

                    ArtifactEditor artifactEditor = new ArtifactEditor(scriptOrRuleActivity,
                            artifactValueProvider, null);
                    artifactEditor.openTempFile(new NullProgressMonitor());
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open Script", getProject());
        }
    }

    private void openDocumentation() {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    ArtifactEditorValueProvider artifactValueProvider = new DocumentationEditorValueProvider(
                            designerCanvasSelection);
                    ArtifactEditor artifactEditor = new ArtifactEditor(designerCanvasSelection,
                            artifactValueProvider, null);
                    artifactEditor.openTempFile(new NullProgressMonitor());
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open Documentation", getProject());
        }
    }

    private void openDefinitionXml() {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    try {
                        WorkflowProject project = process.getProject();
                        IStorage storage = new DocumentStorage(project, process.getLabel(),
                                project.getDataAccess().loadRuleSet(process.getId()).getRuleSet());
                        IStorageEditorInput input = new StorageEditorInput(storage);
                        IWorkbenchPage page = MdwPlugin.getActivePage();
                        if (page != null)
                            page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "View Definition", process.getProject());
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open Script", getProject());
        }
    }

    private void openJava(final Activity javaActivity) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    ArtifactEditorValueProvider artifactValueProvider = new JavaEditorValueProvider(
                            javaActivity);
                    ArtifactEditor artifactEditor = new ArtifactEditor(javaActivity,
                            artifactValueProvider, null);
                    artifactEditor.openTempFile(new NullProgressMonitor());
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open Java", getProject());
        }
    }

    public void openProcessDefinition(final WorkflowProcess processVersion) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    // create a new instance for a new editor
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage();
                    try {
                        page.openEditor(processVersion, "mdw.editors.process");
                    }
                    catch (PartInitException ex) {
                        PluginMessages.uiError(getDisplay().getActiveShell(), ex, "Open Process",
                                processVersion.getProject());
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Open Process", getProject());
        }
    }

    public void launchProcess(final WorkflowProcess processVersion, final boolean debug) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    if (debug)
                        actionHandler.debug(processVersion);
                    else
                        actionHandler.run(processVersion);
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Launch Process", getProject());
        }
    }

    public void launchProcessPage(final WorkflowProcess processVersion) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    actionHandler.runFromPage(processVersion);
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Launch Process", getProject());
        }
    }

    public void launchProcess(final Activity activity, final boolean debug) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    if (debug)
                        actionHandler.debug(activity);
                    else
                        actionHandler.run(activity);
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Launch Process", getProject());
        }
    }

    public boolean isLockAllowed() {
        return getProcess().getPackage() != null
                && getProcess().isUserAuthorized(UserRoleVO.ASSET_DESIGN)
                && (getProcess().getNextVersion() == null || DesignerProxy.isArchiveEditAllowed())
                && !getProcess().isRemote()
                && (getProcess().isLockedToUser() || getProcess().getLockingUser() == null);
    }

    public void toggleProcessLock(final boolean lock) {
        boolean savePrompted = false;

        IWorkbenchPage page = MdwPlugin.getActivePage();
        if (page != null) {
            ProcessEditor processEditor = (ProcessEditor) page.findEditor(getProcess());
            if (processEditor != null && processEditor.isDirty()) {
                int result = processEditor.promptToSaveOnClose();
                savePrompted = true;
                if (result == ISaveablePart2.CANCEL)
                    return;
            }
        }

        final StringBuffer errorMessage = new StringBuffer();
        BusyIndicator.showWhile(getDisplay(), new Runnable() {
            public void run() {
                try {
                    String lockMessage = getDesignerProxy().toggleProcessLock(getProcess(), lock);
                    if (lockMessage != null && lockMessage.length() > 0)
                        errorMessage.append(lockMessage);
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    errorMessage.append(ex.getMessage());
                }
            }
        });

        if (savePrompted)
            refreshNoPbi(); // avoid thread deadlock due to SWT/AWT contention
        else
            refresh();

        if (errorMessage.toString().length() > 0)
            PluginMessages.uiMessage(errorMessage.toString(), "Lock/Unlock Process",
                    getProcess().getProject(), PluginMessages.INFO_MESSAGE);
    }

    public boolean isRecordChanges() {
        return getFlowchartPage().isRecordChange();
    }

    public void setRecordChanges(boolean record) {
        getFlowchartPage().setRecordChange(record);
    }

    public void commitChanges() {
        getFlowchartPage().commitChanges();
        fireDirtyStateChanged(true);
    }

    private void showProcessInstances(final WorkflowProcess processVersion) {
        PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
        try {
            pbi.busyWhile(new Runnable() {
                public void run() {
                    try {
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage();
                        IViewPart viewPart = page
                                .showView("mdw.views.designer.process.instance.list");
                        if (viewPart != null) {
                            ProcessInstanceListView instancesView = (ProcessInstanceListView) viewPart;
                            instancesView.setProcess(getProcess());
                        }
                    }
                    catch (PartInitException ex) {
                        PluginMessages.uiError(ex, "Show Instances", processVersion.getProject());
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(ex, "Show Instances", getProject());
        }
    }

    public void openCallingInstance() {
        if (isInstance()) {
            PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(), getCanvas());
            try {
                pbi.busyWhile(new Runnable() {
                    public void run() {
                        // create a new instance for a new editor
                        Long callerId = getProcess().getProcessInstance().getOwnerId();
                        PluginDataAccess dataAccess = getProject().getDataAccess();
                        ProcessInstanceVO procInst = dataAccess.getProcessInstance(callerId);

                        ProcessVO subprocess = new ProcessVO();
                        subprocess.setProcessId(procInst.getProcessId());
                        subprocess.setProcessName(procInst.getProcessName());
                        WorkflowProcess toOpen = new WorkflowProcess(getProject(), subprocess);
                        toOpen.setPackage(getProject().getProcessPackage(subprocess.getId()));

                        toOpen.setProcessInstance(procInst);
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage();
                        try {
                            page.openEditor(toOpen, "mdw.editors.process");
                            page.showView("org.eclipse.ui.views.PropertySheet");
                        }
                        catch (PartInitException ex) {
                            PluginMessages.uiError(ex, "Open Calling Instance", getProject());
                        }
                    }
                });
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Open Calling Instance", getProject());
            }
        }
    }

    public void setEditorFocus() {
        IWorkbenchPage page = MdwPlugin.getActivePage();
        if (page != null) {
            ProcessEditor processEditor = (ProcessEditor) page.findEditor(getProcess());
            if (processEditor != null)
                page.activate(processEditor);
        }
    }
}
