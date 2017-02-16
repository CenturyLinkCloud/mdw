/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class SubProcessInstancesSection extends PropertySection implements IFilter {
    // activity or embedded subprocess
    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    private TableEditor tableEditor;
    private List<ColumnSpec> columnSpecs;

    private SubProcessInstanceContentProvider contentProvider;
    private SubProcessInstanceLabelProvider labelProvider;

    public void setSelection(WorkflowElement selection) {
        this.element = selection;

        tableEditor.setElement(element);

        if (element instanceof Activity)
            tableEditor.setValue(((Activity) element).getSubProcessInstances());
        else if (element instanceof EmbeddedSubProcess)
            tableEditor.setValue(((EmbeddedSubProcess) element).getSubProcessInstances());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        this.element = selection;

        tableEditor = new TableEditor(element, TableEditor.TYPE_TABLE);
        tableEditor.setReadOnly(true);

        if (columnSpecs == null)
            columnSpecs = createColumnSpecs();
        tableEditor.setColumnSpecs(columnSpecs);

        if (contentProvider == null)
            contentProvider = new SubProcessInstanceContentProvider();
        tableEditor.setContentProvider(contentProvider);

        if (labelProvider == null)
            labelProvider = new SubProcessInstanceLabelProvider();
        tableEditor.setLabelProvider(labelProvider);

        tableEditor.render(composite);

        // double-click
        tableEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                if (!(element instanceof EmbeddedSubProcess))
                    openSubProcessInstance((ProcessInstanceVO) newValue);
            }
        });

        // right-click menu
        tableEditor.getTable().addListener(SWT.MenuDetect, new Listener() {
            public void handleEvent(Event event) {
                tableEditor.getTable().setMenu(element instanceof EmbeddedSubProcess ? null
                        : createContextMenu(getShell()));
            }
        });
    }

    private Menu createContextMenu(Shell shell) {
        Menu menu = new Menu(shell, SWT.POP_UP);

        StructuredSelection selection = (StructuredSelection) tableEditor.getTableViewer()
                .getSelection();
        if (selection.size() == 1 && selection.getFirstElement() instanceof ProcessInstanceVO) {
            final ProcessInstanceVO processInstanceInfo = (ProcessInstanceVO) selection
                    .getFirstElement();

            // view
            MenuItem procInstItem = new MenuItem(menu, SWT.PUSH);
            procInstItem.setText("View Subprocess Instance");
            ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
            procInstItem.setImage(imageDesc.createImage());
            procInstItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openSubProcessInstance(processInstanceInfo);
                }
            });
        }

        return menu;
    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec instanceIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID",
                "instanceId");
        instanceIdColSpec.width = 100;
        columnSpecs.add(instanceIdColSpec);

        ColumnSpec nameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "name");
        nameColSpec.width = 250;
        columnSpecs.add(nameColSpec);

        ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
        statusColSpec.width = 100;
        columnSpecs.add(statusColSpec);

        ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start",
                "startDate");
        startDateColSpec.width = 150;
        columnSpecs.add(startDateColSpec);

        ColumnSpec endDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "End", "endDate");
        endDateColSpec.width = 150;
        columnSpecs.add(endDateColSpec);

        return columnSpecs;
    }

    class SubProcessInstanceContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<ProcessInstanceVO> rows = (List<ProcessInstanceVO>) inputElement;
            return rows.toArray(new ProcessInstanceVO[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class SubProcessInstanceLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            ProcessInstanceVO processInstanceInfo = (ProcessInstanceVO) element;

            switch (columnIndex) {
            case 0:
                return processInstanceInfo.getId().toString();
            case 1:
                return processInstanceInfo.getProcessName();
            case 2:
                return WorkStatuses.getWorkStatuses().get(processInstanceInfo.getStatusCode());
            case 3:
                if (processInstanceInfo.getStartDate() == null)
                    return "";
                return processInstanceInfo.getStartDate();
            case 4:
                if (processInstanceInfo.getEndDate() == null)
                    return "";
                return processInstanceInfo.getEndDate();
            default:
                return null;
            }
        }
    }

    private void openSubProcessInstance(ProcessInstanceVO processInstanceInfo) {
        // create a new instance for a new editor
        ProcessVO subprocess = new ProcessVO();
        subprocess.setProcessId(processInstanceInfo.getProcessId());
        subprocess.setProcessName(processInstanceInfo.getProcessName());
        WorkflowProcess toOpen = new WorkflowProcess(element.getProject(), subprocess);
        toOpen.setPackage(element.getProject().getProcessPackage(subprocess.getId()));
        toOpen.setProcessInstance(processInstanceInfo);

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(toOpen, "mdw.editors.process");
        }
        catch (PartInitException ex) {
            PluginMessages.uiError(getShell(), ex, "Open SubProcess Instances",
                    element.getProject());
        }
    }

    /**
     * For IFilter interface.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowElement))
            return false;

        WorkflowElement workflowElement = (WorkflowElement) toTest;

        if (workflowElement instanceof Activity) {
            Activity activity = (Activity) workflowElement;
            if (activity.isSubProcessInvoke() || activity.isManualTask())
                return activity.hasSubProcessInstances();
            else
                return false;
        }
        else if (workflowElement instanceof EmbeddedSubProcess) {
            return workflowElement.hasInstanceInfo();
        }

        return false;
    }

}
