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
package com.centurylink.mdw.plugin.designer;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.pages.FlowchartPage;
import com.centurylink.mdw.designer.runtime.ProcessStatusPane;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.views.ToolboxView;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.search.ProcessSearchQuery;
import com.centurylink.mdw.plugin.search.SearchQuery;
import com.centurylink.mdw.plugin.search.SearchResultsPage;
import com.centurylink.mdw.plugin.swing.support.EmbeddedSwingComposite;

/**
 * SWT wrapper for the designer toolbox swing component pane. This is a
 * singleton since there is only one toolbox view.
 */
public class ToolboxWrapper extends DesignerPanelWrapper
        implements AWTEventListener, ElementChangeListener {
    public static final String VIEW_ID = "mdw.views.designer.toolbox";

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public void setProcess(WorkflowProcess process) {
        if (process != null)
            process.getProject().removeElementChangeListener(this);
        this.process = process;
        process.getProject().addElementChangeListener(this);
    }

    private static ToolboxWrapper instance;
    private ActivityImpl toolboxSelection;

    public ActivityImpl getToolboxSelection() {
        return toolboxSelection;
    }

    private FlowchartPage flowchartPage;

    public FlowchartPage getFlowchartPage() {
        return flowchartPage;
    }

    public void setFlowchartPage(FlowchartPage flowchartPage) {
        this.flowchartPage = flowchartPage;
    }

    private ToolboxWrapper() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
    }

    private Map<String, List<ActivityImpl>> projectDirtyImpls = new HashMap<String, List<ActivityImpl>>();

    public boolean isDirty() {
        List<ActivityImpl> dirtyImpls = getDirtyImpls();
        return isPopulated() && dirtyImpls != null && !dirtyImpls.isEmpty();
    }

    public void setDirty(ActivityImpl impl) {
        List<ActivityImpl> dirtyImpls = getDirtyImpls();
        if (dirtyImpls == null)
            dirtyImpls = new ArrayList<ActivityImpl>();
        if (!dirtyImpls.contains(impl))
            dirtyImpls.add(impl);
        projectDirtyImpls.put(getProject().getLabel(), dirtyImpls);
    }

    public void clearDirty() {
        projectDirtyImpls.remove(getProject().getLabel());
    }

    public void dirtyStateChanged(boolean dirty) {
        if (dirty)
            setDirty(getToolboxSelection());
        else
            projectDirtyImpls.remove(getProject().getName());

        super.dirtyStateChanged(dirty);
    }

    public List<ActivityImpl> getDirtyImpls() {
        return projectDirtyImpls.get(getProject().getLabel());
    }

    public static ToolboxWrapper getInstance() {
        if (instance == null)
            instance = new ToolboxWrapper();

        return instance;
    }

    public void populate() {
        if (getFlowchartPage() != null)
            filter();
        super.populate();
    }

    public boolean isPopulated() {
        return getFlowchartPage() != null;
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
                JPanel toolbarPane = null;
                if (getFlowchartPage() == null) {
                    toolbarPane = new JPanel();
                }
                else if (isInstance()) {
                    toolbarPane = new ProcessStatusPane(null);
                }
                else {
                    getFlowchartPage().nodepane.reload(isSortAtoZ(),
                            getProject().getDataAccess().getSchemaVersion());
                    toolbarPane = getFlowchartPage().nodepane;
                }
                JScrollPane scrollPane = new JScrollPane(toolbarPane);
                scrollPane.setHorizontalScrollBarPolicy(
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setBackground(Color.GRAY);
                return scrollPane;
            }
        };
    }

    /**
     * Handles Swing events (running on the AWT UI thread).
     */
    public void eventDispatched(AWTEvent event) {
        if (getFlowchartPage() == null || !event.getSource().equals(getFlowchartPage().nodepane))
            return;

        if (event.getID() == MouseEvent.MOUSE_RELEASED) {
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.isPopupTrigger()
                    || (PluginUtil.isMac() && mouseEvent.getButton() == 3)) {
                getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        final Menu menu = buildPopupMenu();
                        if (menu != null) {
                            getEmbeddedSwingComposite().setMenu(menu);
                            menu.setVisible(true);
                        }
                    }
                });
                mouseEvent.consume();
            }
        }
        else if (event.getID() == MouseEvent.MOUSE_CLICKED) {
            // don't propagate double-click events
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.getClickCount() > 1)
                mouseEvent.consume();
        }
        else if (event.getID() == MouseEvent.MOUSE_PRESSED
                || event.getID() == MouseEvent.MOUSE_DRAGGED) {
            // set the current selection
            MouseEvent mouseEvent = (MouseEvent) event;
            handleSelection(mouseEvent.getX(), mouseEvent.getY());
            final boolean doubleClick = mouseEvent.getClickCount() > 1;

            getDisplay().asyncExec(new Runnable() {
                public void run() {
                    setViewFocus();
                    getSelectionProvider().setSelection(toolboxSelection);
                    if (doubleClick)
                        showPropertiesView();
                }
            });
        }
    }

    private void handleSelection(int mouseX, int mouseY) {
        int idx = getFlowchartPage().nodepane.nodeAt(mouseX, mouseY);
        if (idx != -1) {
            Object obj = getDesignerProxy().getNodeMetaInfo().get(idx);
            if (obj instanceof ActivityImplementorVO) {
                ActivityImplementorVO activityImplVO = (ActivityImplementorVO) obj;
                toolboxSelection = getProject()
                        .getActivityImpl(activityImplVO.getImplementorClassName());
                if (!toolboxSelection.isUserAuthorized(UserRoleVO.ASSET_DESIGN))
                    toolboxSelection.setReadOnly(true);
            }
        }
        toolboxSelection.addDirtyStateListener(this);
    }

    /**
     * Updates the UI to reflect the model.
     */
    public void update() {
        filter();
        getFlowchartPage().nodepane.reload(isSortAtoZ(),
                getProject().getDataAccess().getSchemaVersion());
        clearDirty();
    }

    /**
     * Applies filters for suppressed implementors.
     */
    public void filter() {
        try {
            List<String> suppressed = getProject().getSuppressedActivityImplementors();
            for (ActivityImplementorVO impl : getProject().getDataAccess().getDesignerDataModel()
                    .getActivityImplementors())
                impl.setShowInToolbox(
                        !suppressed.contains(impl.getImplementorClassName()) && !impl.isHidden());
        }
        catch (IOException ex) {
            PluginMessages.uiError(ex, "Filter Implementors", getProject());
        }
    }

    /**
     * Reloads from the database.
     */
    public void refresh() {
        try {
            IRunnableWithProgress loader = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    ProgressMonitor progressMonitor = new SwtProgressMonitor(monitor);
                    progressMonitor
                            .start("Loading Activity Implementors for " + getProject().getLabel());
                    progressMonitor.progress(25);
                    getProject().reloadActivityImplementors();
                    progressMonitor.done();
                }
            };
            ProgressMonitorDialog progMonDlg = new MdwProgressMonitorDialog(
                    Display.getCurrent().getActiveShell());
            progMonDlg.run(true, false, loader);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Refresh Implementors", getProject());
        }
        update();
        clearDirty();
    }

    private boolean isSortAtoZ() {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        return prefsStore.getBoolean(PreferenceConstants.PREFS_SORT_TOOLBOX_A_TO_Z);
    }

    public void setViewFocus() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart viewPart = page.findView(VIEW_ID);
            if (viewPart != null)
                page.activate(viewPart);
        }
    }

    public boolean isInstance() {
        if (getProcess() == null)
            return false;
        return getProcess().hasInstanceInfo();
    }

    public DesignerProxy getDesignerProxy() {
        return getProject().getDesignerProxy();
    }

    public PluginDataAccess getDataAccess() {
        return getProject().getDataAccess();
    }

    public WorkflowProject getProject() {
        return process.getProject();
    }

    /**
     * Needs to run on the SWT UI thread.
     */
    private Menu buildPopupMenu() {
        if (toolboxSelection == null)
            return null;

        Menu menu = new Menu(getParent().getShell(), SWT.POP_UP);

        if (toolboxSelection != null
                && toolboxSelection.isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
            MenuItem newItem = new MenuItem(menu, SWT.PUSH);
            newItem.setText("New...");
            ImageDescriptor newImageDesc = MdwPlugin.getImageDescriptor("icons/genact_wiz.gif");
            newItem.setImage(newImageDesc.createImage());
            newItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    newImpl();
                }
            });

            // delete
            if (!toolboxSelection.isPseudoProcessActivity()) {
                MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
                deleteItem.setText("Delete");
                ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
                deleteItem.setImage(deleteImageDesc.createImage());
                deleteItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        deleteImpl();
                    }
                });
            }
        }

        // properties
        MenuItem propsItem = new MenuItem(menu, SWT.PUSH);
        propsItem.setText("Show Properties");
        ImageDescriptor propsImageDesc = MdwPlugin.getImageDescriptor("icons/properties.gif");
        propsItem.setImage(propsImageDesc.createImage());
        propsItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showPropertiesView();
            }
        });

        // view impl source
        if (!toolboxSelection.isPseudoProcessActivity()) {
            MenuItem sourceItem = new MenuItem(menu, SWT.PUSH);
            sourceItem.setText("View Source");
            ImageDescriptor sourceImageDesc = MdwPlugin.getImageDescriptor("icons/java.gif");
            sourceItem.setImage(sourceImageDesc.createImage());
            sourceItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    PanelBusyIndicator pbi = new PanelBusyIndicator(getDisplay(),
                            getFlowchartPage().nodepane);
                    try {
                        pbi.busyWhile(new Runnable() {
                            public void run() {
                                toolboxSelection.getProject()
                                        .viewSource(toolboxSelection.getImplClassName());
                            }
                        });
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(ex, "View Source", getProject());
                    }
                }
            });
        }

        // search for references
        MenuItem refsItem = new MenuItem(menu, SWT.PUSH);
        refsItem.setText("Search for Usages");
        ImageDescriptor refsImageDesc = MdwPlugin.getImageDescriptor("icons/references.gif");
        refsItem.setImage(refsImageDesc.createImage());
        refsItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                searchReferences(toolboxSelection);
            }
        });

        return menu;
    }

    private void deleteImpl() {
        IViewPart viewPart = getViewPart();
        if (viewPart != null)
            ((ToolboxView) viewPart).handleDelete();
    }

    private void newImpl() {
        IViewPart viewPart = getViewPart();
        if (viewPart != null)
            ((ToolboxView) viewPart).handleNew();
    }

    private IViewPart getViewPart() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null)
            return page.findView(VIEW_ID);
        return null;
    }

    private void searchReferences(ActivityImpl activityImpl) {
        List<WorkflowProject> projects = new ArrayList<WorkflowProject>();
        projects.add(getProject());
        Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
        ProcessSearchQuery searchQuery = new ProcessSearchQuery(projects,
                SearchQuery.SearchType.CONTAINING_ENTITY, "*", true, shell);
        searchQuery.setContainedEntityId(activityImpl.getId());
        searchQuery.setContainedEntityName(activityImpl.getImplClassName());

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
                    "Search for usages cancelled.");
        }
        catch (Exception ex) {
            PluginMessages.uiError(shell, ex, "Search for Usages", getProject());
        }
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getElement() instanceof ActivityImpl && getFlowchartPage() != null) // must
                                                                                    // be
                                                                                    // visible
            update(); // reflect any changes
    }
}
