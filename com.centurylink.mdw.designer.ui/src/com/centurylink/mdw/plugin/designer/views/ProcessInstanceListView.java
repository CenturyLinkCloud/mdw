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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.designer.pages.CanvasCommon;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.PanelBusyIndicator;
import com.centurylink.mdw.plugin.designer.dialogs.ProcessInstanceFilterDialog;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.ProcessInstanceSort;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.storage.DocumentStorage;
import com.centurylink.mdw.plugin.designer.storage.StorageEditorInput;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessInstanceListView extends ViewPart {
    public static final String VIEW_ID = "mdw.views.designer.process.instance.list";
    private WorkflowProject workflowProject;
    private WorkflowProcess processVersion;
    private ProcessInstanceActionGroup actionGroup;
    private List<ColumnSpec> columnSpecs;
    private String[] columnProps;

    private Combo projectCombo;
    private Combo processCombo;
    private Combo versionCombo;
    private Label instanceCountLabel;
    private Table processInstanceTable;

    private TableViewer tableViewer;

    public TableViewer getTableViewer() {
        return tableViewer;
    }

    private ProcessInstanceContentProvider contentProvider;
    private ProcessInstanceLabelProvider labelProvider;

    public void setProcess(WorkflowProcess processVersion) {
        this.processVersion = processVersion;
        workflowProject = processVersion.getProject();
        projectCombo.select(projectCombo.indexOf(workflowProject.getName()));
        refreshProcesses();
        String processName = processVersion.getName();
        processCombo.select(processCombo.indexOf(processName));
        refreshVersions();
        String version = processVersion.getVersionString();
        versionCombo.select(versionCombo.indexOf(version));
        resetPageIndex();
        clearVariables();
        refreshTable();
    }

    @Override
    public void createPartControl(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout
        GridLayout gl = new GridLayout();
        gl.numColumns = 10;
        composite.setLayout(gl);

        createSelectionControls(composite);

        columnSpecs = createColumnSpecs();
        createTable(composite);
        tableViewer = createTableViewer(processInstanceTable);
        contentProvider = new ProcessInstanceContentProvider();
        tableViewer.setContentProvider(contentProvider);
        labelProvider = new ProcessInstanceLabelProvider();
        tableViewer.setLabelProvider(labelProvider);

        actionGroup = new ProcessInstanceActionGroup(this);
        IActionBars actionBars = getViewSite().getActionBars();
        actionGroup.fillActionBars(actionBars);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
                MdwPlugin.getPluginId() + ".process_instance_help");
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
    }

    @Override
    public void setFocus() {

    }

    public void refresh() {
        // refreshProjects();
        refreshProcesses();
        refreshVersions();
        resetPageIndex();
        refreshTable();
    }

    public void filter() {
        if (processVersion == null)
            return;

        ProcessInstanceFilterDialog filterDialog = new ProcessInstanceFilterDialog(
                getSite().getShell(), processVersion, contentProvider.getFilter());
        if (filterDialog.open() == Dialog.OK) {
            contentProvider.setFilter(filterDialog.getFilter());
            resetPageIndex();

            String filterVersion = filterDialog.getFilter().getProcess();
            if (filterVersion != null && !filterVersion.equals(versionCombo.getText().trim())) {
                // version was changed in filter
                versionCombo.setText(filterVersion);
                processVersion = getProcessVersion(processVersion, filterVersion);
            }
            refreshTable();
        }
    }

    private WorkflowProcess getProcessVersion(WorkflowProcess processVersion, String version) {
        if (version.length() == 0) {
            WorkflowProcess pv = new WorkflowProcess(workflowProject,
                    new ProcessVO(processVersion.getProcessVO()));
            pv.getProcessVO().setProcessId(0L); // indicates exclude version
                                                // from criteria
            pv.setPackage(processVersion.getPackage());
            return pv;
        }
        else {
            ProcessVO processVO = workflowProject.getDataAccess()
                    .getProcess(processVersion.getName(), version);
            WorkflowProcess pv = new WorkflowProcess(workflowProject, processVO);
            pv.setPackage(processVersion.getPackage());
            return pv;
        }
    }

    public void pageDown() {
        long count = contentProvider.getInstanceCount();
        int pageSize = contentProvider.getFilter().getPageSize();
        long pages = count / pageSize;
        if (count % pageSize > 0)
            pages++;
        if (contentProvider.getPageIndex() == pages)
            return;
        contentProvider.setPageIndex(contentProvider.getPageIndex() + 1);
        refreshTable();
    }

    public void pageUp() {
        if (contentProvider.getPageIndex() == 1)
            return;
        contentProvider.setPageIndex(contentProvider.getPageIndex() - 1);
        refreshTable();
    }

    public void refreshTable() {
        BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
            public void run() {
                contentProvider.setInstanceInfo(null); // force refresh
                tableViewer.setInput(processVersion);
                long count = contentProvider.getInstanceCount().longValue();
                int pageIdx = contentProvider.getPageIndex().intValue();
                int pageSize = contentProvider.getFilter().getPageSize().intValue();

                String info = null;
                if (count == 0) {
                    info = "No matching results found";
                }
                else {
                    int base = (pageIdx - 1) * pageSize;

                    int first = base + 1;
                    long last = count > (base + pageSize) ? first + pageSize - 1
                            : base + count % pageSize;

                    info = "Showing: " + first + " to " + last + " of " + count;
                }
                instanceCountLabel.setText(info);
                instanceCountLabel.pack();
            }
        });
    }

    private void resetPageIndex() {
        contentProvider.setPageIndex(1);
    }

    private void clearVariables() {
        contentProvider.getFilter().getVariableValues().clear();
    }

    private void createSelectionControls(Composite parent) {
        new Label(parent, SWT.NONE).setText("Workflow Project:");
        projectCombo = new Combo(parent, SWT.DROP_DOWN);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 160;
        projectCombo.setLayoutData(gd);
        refreshProjects();
        projectCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String projectName = projectCombo.getText().trim();
                workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(projectName);
                processVersion = null;
                refreshProcesses();
                processCombo.select(0);
                refreshVersions();
                versionCombo.select(0);
                resetPageIndex();
                clearVariables();
                refreshTable();
            }
        });
        projectCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String project = projectCombo.getText().trim();
                if (project.length() > 0 && projectCombo.indexOf(project) == -1) {
                    processVersion = null;
                    refreshProcesses();
                    processCombo.select(0);
                    refreshVersions();
                    versionCombo.select(0);
                    resetPageIndex();
                    clearVariables();
                    refreshTable();
                }
            }
        });

        new Label(parent, SWT.NONE).setText("    "); // spacer

        new Label(parent, SWT.NONE).setText("Process:");
        processCombo = new Combo(parent, SWT.DROP_DOWN);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 160;
        processCombo.setLayoutData(gd);
        processCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String selected = processCombo.getText().trim();
                if (selected.length() == 0) {
                    processVersion = null;
                    refreshVersions();
                    versionCombo.select(0);
                }
                else {
                    ProcessVO processVO = workflowProject.getDesignerProxy()
                            .getLatestProcessVO(selected);
                    processVersion = new WorkflowProcess(workflowProject, processVO);
                    refreshVersions();
                    versionCombo.select(1);
                }
                resetPageIndex();
                clearVariables();
                refreshTable();
            }
        });
        processCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String process = processCombo.getText().trim();
                if (process.length() > 0 && processCombo.indexOf(process) == -1) {
                    processVersion = null;
                    refreshVersions();
                    versionCombo.select(0);
                    resetPageIndex();
                    clearVariables();
                    refreshTable();
                }
            }
        });

        new Label(parent, SWT.NONE).setText("    "); // spacer

        new Label(parent, SWT.NONE).setText("Version:");
        versionCombo = new Combo(parent, SWT.DROP_DOWN);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 50;
        versionCombo.setLayoutData(gd);
        versionCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                // clear out filter version
                if (contentProvider.getFilter() != null)
                    contentProvider.getFilter().setProcess(null);

                String version = versionCombo.getText().trim();
                if (version.isEmpty() && processVersion.getId() == 0)
                    return; // avoid re-retrieve
                processVersion = getProcessVersion(processVersion, version);

                resetPageIndex();
                refreshTable();
            }
        });
        versionCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String version = versionCombo.getText().trim();
                if (version.length() > 0 && versionCombo.indexOf(version) == -1) {
                    processVersion = null;
                    resetPageIndex();
                    refreshTable();
                }
            }
        });

        new Label(parent, SWT.NONE).setText("    "); // spacer

        instanceCountLabel = new Label(parent, SWT.NONE);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        instanceCountLabel.setLayoutData(gd);
        instanceCountLabel.setText("Showing: ");
    }

    private void refreshProjects() {
        projectCombo.removeAll();
        projectCombo.add("");
        List<WorkflowProject> workflowProjects = WorkflowProjectManager.getInstance()
                .getWorkflowProjects();
        if (workflowProjects != null && workflowProjects.size() > 0) {
            for (WorkflowProject workflowProject : workflowProjects)
                projectCombo.add(workflowProject.getName());
            if (workflowProject == null)
                projectCombo.select(0);
            else
                projectCombo.setText(workflowProject.getName());
        }
    }

    private void refreshProcesses() {
        BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
            public void run() {
                processCombo.removeAll();
                processCombo.add("");
                if (workflowProject != null) {
                    List<ProcessVO> processVOs = workflowProject.getDataAccess()
                            .getProcesses(false);
                    for (ProcessVO processVO : processVOs)
                        processCombo.add(processVO.getProcessName());
                    if (processVersion != null)
                        processCombo.setText(processVersion.getName());
                }
            }
        });
    }

    private void refreshVersions() {
        versionCombo.removeAll();
        versionCombo.add("");
        if (processVersion != null) {
            for (WorkflowProcess pv : processVersion.getAllProcessVersions()) {
                versionCombo.add(pv.getVersionString());
            }
            versionCombo.setText(processVersion.getVersionString());
        }
    }

    private void createTable(Composite parent) {
        int style = SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;

        processInstanceTable = new Table(parent, style);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 10;
        processInstanceTable.setLayoutData(gridData);
        processInstanceTable.setLinesVisible(true);
        processInstanceTable.setHeaderVisible(true);

        for (int i = 0; i < columnSpecs.size(); i++) {
            ColumnSpec colSpec = columnSpecs.get(i);
            int styles = SWT.LEFT;
            if (colSpec.readOnly)
                style = style | SWT.READ_ONLY;
            TableColumn column = new TableColumn(processInstanceTable, styles, i);
            column.setText(colSpec.label);
            column.setWidth(colSpec.width);
            column.setResizable(colSpec.resizable);
            column.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    TableColumn sortColumn = tableViewer.getTable().getSortColumn();
                    TableColumn currentColumn = (TableColumn) e.widget;
                    int direction = tableViewer.getTable().getSortDirection();
                    if (sortColumn == currentColumn) {
                        direction = direction == SWT.UP ? SWT.DOWN : SWT.UP;
                    }
                    else {
                        tableViewer.getTable().setSortColumn(currentColumn);
                        direction = SWT.DOWN;
                    }
                    tableViewer.getTable().setSortDirection(direction);

                    ProcessInstanceSort sort = contentProvider.getSort();
                    sort.setSort(currentColumn.getText());
                    sort.setAscending(direction == SWT.UP);
                    refreshTable();
                }
            });
        }

        // double-click
        processInstanceTable.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                ProcessInstanceVO processInstanceInfo = (ProcessInstanceVO) e.item.getData();
                handleOpen(processInstanceInfo);
            }
        });

        // right-click menu
        processInstanceTable.addListener(SWT.MenuDetect, new Listener() {
            public void handleEvent(Event event) {
                processInstanceTable.setMenu(createContextMenu(processInstanceTable.getShell()));
            }
        });

        // auto-adjust column width
        processInstanceTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                int tableWidth = processInstanceTable.getBounds().width;
                int cumulative = 0;
                TableColumn[] tableColumns = processInstanceTable.getColumns();
                for (int i = 0; i < tableColumns.length; i++) {
                    if (i == tableColumns.length - 1)
                        tableColumns[i].setWidth(tableWidth - cumulative - 25);
                    cumulative += tableColumns[i].getWidth();
                }
            }
        });

    }

    private TableViewer createTableViewer(Table table) {
        TableViewer tableViewer = new TableViewer(table);
        tableViewer.setUseHashlookup(true);
        tableViewer.setColumnProperties(columnProps);

        return tableViewer;
    }

    CanvasCommon canvas = null;

    private void handleOpen(final ProcessInstanceVO processInstanceInfo) {
        BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
            public void run() {
                // create a new instance for a new editor
                final WorkflowProcess toOpen = new WorkflowProcess(processVersion);
                toOpen.setProcessVO(processVersion.getProcessVO());
                toOpen.setProcessInstance(processInstanceInfo);
                IEditorPart editor = MdwPlugin.getActivePage().getActiveEditor();
                if (editor instanceof ProcessEditor) {
                    ProcessEditor processEditor = (ProcessEditor) editor;
                    CanvasCommon canvas = processEditor.getProcessCanvasWrapper().getCanvas();
                    PanelBusyIndicator pbi = new PanelBusyIndicator(
                            getSite().getShell().getDisplay(), canvas);
                    try {
                        pbi.busyWhile(new Runnable() {
                            public void run() {
                                openInstance(toOpen);
                            }
                        });
                    }
                    catch (InvocationTargetException ex) {
                        PluginMessages.uiError(ex, "Open Process Instance", workflowProject);
                    }
                }
                else {
                    // no editor panel to show busy on
                    openInstance(toOpen);
                }
            }
        });
    }

    private void openInstance(WorkflowProcess toOpen) {
        try {
            IWorkbenchPage page = MdwPlugin.getActivePage();
            page.openEditor(toOpen, "mdw.editors.process");
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Open Process Instance", workflowProject);
        }
    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec processInstColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID",
                "id");
        processInstColSpec.width = 100;
        columnSpecs.add(processInstColSpec);

        ColumnSpec masterRequestColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT,
                "Master Request ID", "masterRequestId");
        masterRequestColSpec.width = 180;
        columnSpecs.add(masterRequestColSpec);

        ColumnSpec ownerColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Owner", "owner");
        ownerColSpec.width = 100;
        columnSpecs.add(ownerColSpec);

        ColumnSpec ownerIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Owner ID", "ownerId");
        ownerIdColSpec.width = 100;
        columnSpecs.add(ownerIdColSpec);

        ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "statusCode");
        statusColSpec.width = 100;
        columnSpecs.add(statusColSpec);

        ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start",
                "startDate");
        startDateColSpec.width = 150;
        columnSpecs.add(startDateColSpec);

        ColumnSpec endDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "End", "endDate");
        endDateColSpec.width = 150;
        columnSpecs.add(endDateColSpec);

        columnProps = new String[columnSpecs.size()];
        for (int i = 0; i < columnSpecs.size(); i++) {
            columnProps[i] = columnSpecs.get(i).property;
        }

        return columnSpecs;
    }

    private Menu createContextMenu(Shell shell) {
        Menu menu = new Menu(shell, SWT.POP_UP);

        final StructuredSelection selection = (StructuredSelection) getTableViewer().getSelection();
        if (selection.size() == 1 && selection.getFirstElement() instanceof ProcessInstanceVO) {
            final ProcessInstanceVO processInstanceInfo = (ProcessInstanceVO) selection
                    .getFirstElement();

            // open instance
            MenuItem openItem = new MenuItem(menu, SWT.PUSH);
            openItem.setText("Open");
            ImageDescriptor openImageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
            openItem.setImage(openImageDesc.createImage());
            openItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    handleOpen(processInstanceInfo);
                }
            });

            // owning document
            if (OwnerType.DOCUMENT.equals(processInstanceInfo.getOwner())
                    || OwnerType.TESTER.equals(processInstanceInfo.getOwner())) {
                MenuItem docItem = new MenuItem(menu, SWT.PUSH);
                docItem.setText("View Owning Document");
                ImageDescriptor docImageDesc = MdwPlugin.getImageDescriptor("icons/doc.gif");
                docItem.setImage(docImageDesc.createImage());
                docItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        IStorage storage = new DocumentStorage(workflowProject,
                                new DocumentReference(processInstanceInfo.getOwnerId(), null));
                        final IStorageEditorInput input = new StorageEditorInput(storage);
                        final IWorkbenchPage page = MdwPlugin.getActivePage();
                        if (page != null) {
                            BusyIndicator.showWhile(getSite().getShell().getDisplay(),
                                    new Runnable() {
                                        public void run() {
                                            try {
                                                page.openEditor(input,
                                                        "org.eclipse.ui.DefaultTextEditor");
                                            }
                                            catch (PartInitException ex) {
                                                PluginMessages.uiError(ex, "View Document",
                                                        workflowProject);
                                            }
                                        }
                                    });
                        }
                    }
                });
            }

            // instance hierarchy
            MenuItem hierarchyItem = new MenuItem(menu, SWT.PUSH);
            hierarchyItem.setText("Instance Hierarchy");
            ImageDescriptor hierarchyImageDesc = MdwPlugin
                    .getImageDescriptor("icons/hierarchy.gif");
            hierarchyItem.setImage(hierarchyImageDesc.createImage());
            hierarchyItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    WorkflowProcess pv = new WorkflowProcess(processVersion);
                    pv.setProcessVO(processVersion.getProcessVO());
                    pv.setProcessInstance(processInstanceInfo);
                    new WorkflowElementActionHandler().showHierarchy(pv);
                }
            });
        }

        // delete
        if (!selection.isEmpty() && !processVersion.getProject().isProduction()
                && processVersion.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)) {
            MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
            deleteItem.setText("Delete...");
            ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
            deleteItem.setImage(deleteImageDesc.createImage());
            deleteItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (selection.size() == 1
                            && selection.getFirstElement() instanceof ProcessInstanceVO) {
                        ProcessInstanceVO pii = (ProcessInstanceVO) selection.getFirstElement();
                        if (MessageDialog.openConfirm(getSite().getShell(), "Confirm Delete",
                                "Delete process instance ID: " + pii.getId()
                                        + " for workflow project '"
                                        + processVersion.getProject().getName() + "'?")) {
                            List<ProcessInstanceVO> instances = new ArrayList<ProcessInstanceVO>();
                            instances.add((ProcessInstanceVO) selection.getFirstElement());
                            handleDelete(instances);
                        }
                    }
                    else {
                        if (MessageDialog.openConfirm(getSite().getShell(), "Confirm Delete",
                                "Delete selected process instances for workflow project '"
                                        + processVersion.getProject().getName() + "'?")) {
                            List<ProcessInstanceVO> instances = new ArrayList<ProcessInstanceVO>();
                            for (Object instance : selection.toArray()) {
                                if (instance instanceof ProcessInstanceVO)
                                    instances.add((ProcessInstanceVO) instance);
                            }
                            handleDelete(instances);
                        }
                    }
                }
            });
        }

        return menu;
    }

    private void handleDelete(List<ProcessInstanceVO> processInstances) {
        processVersion.getProject().getDesignerProxy().deleteProcessInstances(processInstances);
        refresh();
    }
}
