/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

import com.centurylink.mdw.dataaccess.version4.DBMappingUtil;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.User;
import com.centurylink.mdw.plugin.actions.MdwMenuManager;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.WorkflowSelectionProvider;
import com.centurylink.mdw.plugin.designer.dialogs.LoginDialog;
import com.centurylink.mdw.plugin.designer.editors.WorkflowElementEditor;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.TabbedPropertySheetPage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessExplorerView extends ViewPart
        implements ITabbedPropertySheetPageContributor, IMenuListener {
    public static final String VIEW_ID = "mdw.views.designer.processes";

    private TreeViewer treeViewer;
    private WorkflowSelectionProvider selectionProvider;
    private Menu contextMenu;
    private Clipboard clipboard;

    public Clipboard getClipboard() {
        return clipboard;
    }

    private ProcessExplorerDragSource dragSource;
    private ProcessExplorerDropTarget dropTarget;

    public ProcessExplorerDropTarget getDropTarget() {
        return dropTarget;
    }

    private ProcessExplorerActionGroup actionGroup;
    private ProcessExplorerContextListener contextListener;

    @Override
    public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent) {
            protected void handleTreeExpand(TreeEvent event) {
                if (event.item.getData() instanceof WorkflowProject) {
                    final WorkflowProject project = (WorkflowProject) event.item.getData();
                    if (project.isFilePersist() && !project.isRemote()) {
                        // authentication not needed
                        project.setUser(new User(System.getProperty("user.name")));
                    }
                    else {
                        // user authentication
                        Boolean authenticated = project.isAuthenticated();
                        if (authenticated == null) { // has to be in a separate
                                                     // thread to take effect
                            event.item.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    treeViewer.collapseToLevel(project, TreeViewer.ALL_LEVELS);
                                }
                            });
                            return;
                        }
                        if (!authenticated) {
                            LoginDialog loginDialog = new LoginDialog(MdwPlugin.getShell(),
                                    project);
                            int res = loginDialog.open();
                            if (res == Dialog.CANCEL || !project.isAuthenticated()) { // has
                                                                                      // to
                                                                                      // be
                                                                                      // in
                                                                                      // a
                                                                                      // separate
                                                                                      // thread
                                                                                      // to
                                                                                      // take
                                                                                      // effect
                                event.item.getDisplay().asyncExec(new Runnable() {
                                    public void run() {
                                        treeViewer.collapseToLevel(project, TreeViewer.ALL_LEVELS);
                                    }
                                });
                                return;
                            }
                        }
                        super.handleTreeExpand(event);
                        return;
                    }
                }
                super.handleTreeExpand(event);
            }
        };

        ProcessExplorerContentProvider contentProvider = new ProcessExplorerContentProvider();
        WorkflowProjectManager.getInstance().addElementChangeListener(contentProvider); // for
                                                                                        // project
                                                                                        // adds/deletes
        treeViewer.setContentProvider(contentProvider);
        treeViewer.setLabelProvider(new ProcessExplorerLabelProvider());
        treeViewer.setInput(WorkflowProjectManager.getInstance().getWorkflowProjects());
        treeViewer.collapseAll();

        clipboard = new Clipboard(parent.getDisplay());

        // action group
        actionGroup = new ProcessExplorerActionGroup(this);
        actionGroup.fillActionBars(getViewSite().getActionBars());

        // context menu
        MenuManager menuMgr = new MdwMenuManager("Process Explorer");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this);
        contextMenu = menuMgr.createContextMenu(treeViewer.getTree());
        treeViewer.getTree().setMenu(contextMenu);
        getSite().registerContextMenu(menuMgr, treeViewer);

        treeViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                for (Object item : getSelection().toList()) {
                    if (!actionGroup.getActionHandler().open((WorkflowElement) item))
                        expand(item);
                }
            }
        });
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged((IStructuredSelection) event.getSelection());
            }
        });

        // drag and drop support
        Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
        dragSource = new ProcessExplorerDragSource(treeViewer);
        treeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, transfers, dragSource);
        dropTarget = new ProcessExplorerDropTarget(treeViewer);
        treeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, transfers, dropTarget);

        contextListener = new ProcessExplorerContextListener();
        getSite().getPage().addPartListener(contextListener);

        MdwPlugin.getPluginWorkbench().getHelpSystem().setHelp(parent,
                MdwPlugin.getPluginId() + ".process_explorer_help");
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        selectionProvider = new WorkflowSelectionProvider(null);
        site.setSelectionProvider(selectionProvider);
    }

    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    public void select(WorkflowElement element) {
        treeViewer.setSelection(element);
        if (element == null)
            handleSelectionChanged(new StructuredSelection());
    }

    public void expand(Object item) {
        treeViewer.expandToLevel(item, 1);
        showPropertiesView();
    }

    public void handleRefresh() {
        BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
            public void run() {
                treeViewer.collapseAll();
                WorkflowProjectManager projectMgr = WorkflowProjectManager.getInstance();
                projectMgr.refresh();
                treeViewer.setInput(projectMgr.getWorkflowProjects());
            }
        });

        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        for (WorkflowProject project : WorkflowProjectManager.getInstance().getWorkflowProjects()) {
            if (!project.isFilePersist())
                actionHandler.syncOpenEditors(project);
        }
    }

    public void refreshTree() {
        treeViewer.refresh();
    }

    public void handleApply() {
        Object[] expandedElements = treeViewer.getExpandedElements();
        treeViewer.setInput(WorkflowProjectManager.getInstance().getWorkflowProjects());
        treeViewer.setExpandedElements(expandedElements);
    }

    public void handleCollapseAll() {
        treeViewer.collapseAll();
    }

    protected void handleSelectionChanged(IStructuredSelection selection) {
        List<?> list = selection.toList();
        if (list.size() == 0)
            return;

        ActionContext actionContext = new ActionContext(selection);
        actionGroup.setContext(actionContext);

        // show the properties for this item
        Object item = list.get(0);
        if (item instanceof WorkflowElement) {
            WorkflowElement workflowElement = (WorkflowElement) item;
            selectionProvider.setSelection(workflowElement);

            // set the schema owner static qualifier
            if (workflowElement.getProject() != null)
                DBMappingUtil.setSchemaOwner(
                        workflowElement.getProject().getMdwDataSource().getSchemaOwner());
        }
    }

    public void refreshItem(WorkflowElement workflowElement) {
        treeViewer.refresh(workflowElement, true);
    }

    @SuppressWarnings({ "rawtypes", "restriction", "unchecked" })
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class)
            return new TabbedPropertySheetPage(this);
        if (type == IContextProvider.class)
            return org.eclipse.jdt.internal.ui.util.JavaUIHelp.getHelpContextProvider(this,
                    MdwPlugin.getPluginId() + ".process_explorer_help");

        return super.getAdapter(type);
    }

    public String getContributorId() {
        return "mdw.tabbedprops.contributor"; // see plugin.xml
    }

    private IStructuredSelection getSelection() {
        return (IStructuredSelection) treeViewer.getSelection();
    }

    public void menuAboutToShow(IMenuManager menuManager) {
        actionGroup.fillContextMenu(menuManager);
    }

    protected void showPropertiesView() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.showView("org.eclipse.ui.views.PropertySheet");
        }
        catch (PartInitException ex) {
            PluginMessages.log(ex);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (treeViewer != null && treeViewer.getTree() != null)
            treeViewer.getTree().dispose();
        if (contextMenu != null)
            contextMenu.dispose();
        if (clipboard != null)
            clipboard.dispose();
    }

    class ProcessExplorerContextListener implements IPartListener2 {
        public void activated(WorkflowElementEditor editor) {
        };

        public void broughtToTop(WorkflowElementEditor editor) {
        };

        public void closed(WorkflowElementEditor editor) {
        };

        public void hidden(WorkflowElementEditor editor) {
        };

        public void visible(WorkflowElementEditor editor) {
        };

        public void opened(WorkflowElementEditor editor) {
        };

        public void deactivated(WorkflowElementEditor editor) {
        };

        public void partActivated(IWorkbenchPartReference partRef) {
            refreshTree();
        }

        public void partBroughtToTop(IWorkbenchPartReference partRef) {
            refreshTree();
        }

        public void partClosed(IWorkbenchPartReference partRef) {
        }

        public void partDeactivated(IWorkbenchPartReference partRef) {
        }

        public void partHidden(IWorkbenchPartReference partRef) {
        }

        public void partInputChanged(IWorkbenchPartReference partRef) {
        }

        public void partOpened(IWorkbenchPartReference partRef) {
        }

        public void partVisible(IWorkbenchPartReference partRef) {
        }
    }
}