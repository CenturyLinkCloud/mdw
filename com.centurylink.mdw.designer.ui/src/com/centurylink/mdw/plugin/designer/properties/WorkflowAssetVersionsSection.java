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
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
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

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.dialogs.AttributeDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;

public class WorkflowAssetVersionsSection extends PropertySection
        implements IFilter, ElementChangeListener {
    private WorkflowAsset workflowAsset;

    public WorkflowAsset getWorkflowAsset() {
        return workflowAsset;
    }

    private TableEditor tableEditor;

    public void setSelection(WorkflowElement selection) {
        if (workflowAsset != null)
            workflowAsset.removeElementChangeListener(this);
        workflowAsset = (WorkflowAsset) selection;
        workflowAsset.addElementChangeListener(this);

        tableEditor.setElement(workflowAsset);
        setTable();
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        workflowAsset = (WorkflowAsset) selection;

        tableEditor = new TableEditor(workflowAsset, TableEditor.TYPE_TABLE);

        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec packageColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Package", "package");
        packageColSpec.width = 160;
        columnSpecs.add(packageColSpec);
        ColumnSpec versionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Version", "version");
        versionColSpec.width = 60;
        columnSpecs.add(versionColSpec);
        ColumnSpec idColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "ID", "id");
        idColSpec.width = 65;
        columnSpecs.add(idColSpec);
        ColumnSpec createDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Created",
                "createDate");
        createDateColSpec.width = 110;
        columnSpecs.add(createDateColSpec);
        ColumnSpec userColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "User", "user");
        userColSpec.width = 75;
        columnSpecs.add(userColSpec);
        ColumnSpec commentsColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Comments",
                "comments");
        commentsColSpec.width = 200;
        columnSpecs.add(commentsColSpec);
        ColumnSpec lockedToColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Locked To",
                "lockedTo");
        lockedToColSpec.width = 75;
        columnSpecs.add(lockedToColSpec);
        ColumnSpec modDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Last Modified",
                "modDate");
        modDateColSpec.width = 110;
        columnSpecs.add(modDateColSpec);

        tableEditor.setColumnSpecs(columnSpecs);
        // tableEditor.setFillWidth(true);
        tableEditor.setReadOnly(true);

        tableEditor.setContentProvider(new AssetVersionContentProvider());
        tableEditor.setLabelProvider(new AssetVersionLabelProvider());

        tableEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                openAsset((WorkflowAsset) newValue);
            }
        });

        tableEditor.render(composite);

        // right-click menu
        tableEditor.getTable().addListener(SWT.MenuDetect, new Listener() {
            public void handleEvent(Event event) {
                tableEditor.getTable()
                        .setMenu(createContextMenu(tableEditor.getTable().getShell()));
            }
        });
    }

    private Menu createContextMenu(Shell shell) {
        Menu menu = new Menu(shell, SWT.POP_UP);

        final StructuredSelection selection = (StructuredSelection) tableEditor.getTableViewer()
                .getSelection();
        // open
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowAsset) {
            final WorkflowAsset asset = (WorkflowAsset) selection.getFirstElement();

            MenuItem openItem = new MenuItem(menu, SWT.PUSH);
            openItem.setText("Open");
            openItem.setImage(workflowAsset.getIconImage());
            openItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openAsset(asset);
                }
            });
        }
        // view comments
        if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowAsset) {
            final WorkflowAsset asset = (WorkflowAsset) selection.getFirstElement();

            MenuItem viewCommentsItem = new MenuItem(menu, SWT.PUSH);
            viewCommentsItem.setText("View Comments");
            ImageDescriptor viewCommentsImageDesc = MdwPlugin.getImageDescriptor("icons/view.gif");
            viewCommentsItem.setImage(viewCommentsImageDesc.createImage());
            viewCommentsItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    openViewCommentsDialog(asset);
                }
            });
        }
        // delete
        if (!selection.isEmpty() && !workflowAsset.getProject().isProduction()
                && workflowAsset.isUserAuthorized(UserRoleVO.ASSET_DESIGN)
                && (selection.size() == 1
                        && ((WorkflowAsset) selection.getFirstElement()).isLatestVersion()
                        || MdwPlugin.getSettings().isAllowDeleteArchivedProcesses())) {
            MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
            deleteItem.setText("Delete...");
            ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
            deleteItem.setImage(deleteImageDesc.createImage());
            deleteItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (selection.size() >= 1
                            && selection.getFirstElement() instanceof WorkflowAsset) {
                        WorkflowAsset[] assets = new WorkflowAsset[selection.size()];
                        int idx = 0;
                        for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
                            assets[idx] = (WorkflowAsset) iter.next();
                            idx++;
                        }
                        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
                        actionHandler.delete(assets);
                        boolean removedSelected = false;
                        for (WorkflowAsset a : assets) {
                            if (a.equals(workflowAsset))
                                removedSelected = true;
                        }
                        if (removedSelected) {
                            WorkflowAsset sel = null;
                            for (WorkflowAsset toSel : workflowAsset.getAllVersions()) {
                                if (!toSel.equals(workflowAsset)) {
                                    sel = toSel;
                                    break;
                                }
                            }
                            workflowAsset.fireElementChangeEvent(ChangeType.ELEMENT_DELETE, null);
                            setSelection(sel);
                        }
                        else {
                            // just remove and refresh
                            for (WorkflowAsset a : assets)
                                workflowAsset.getProject().removeWorkflowAsset(a);
                            setSelection(workflowAsset);
                        }
                    }
                }
            });
        }

        return menu;
    }

    class AssetVersionContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<WorkflowAsset> rows = (List<WorkflowAsset>) inputElement;
            return rows.toArray(new WorkflowAsset[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class AssetVersionLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0)
                return workflowAsset.getPackageIconImage();
            else if (columnIndex == 1)
                return workflowAsset.getIconImage();
            else
                return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            WorkflowAsset asset = (WorkflowAsset) element;

            switch (columnIndex) {
            case 0:
                return asset.getPackageLabel();
            case 1:
                return asset.getVersionLabel();
            case 2:
                return asset.getIdLabel();
            case 3:
                return asset.getFormattedCreateDate();
            case 4:
                if (asset.getCreateUser() == null)
                    return "";
                return asset.getCreateUser();
            case 5:
                if (asset.getRevisionComment() != null)
                    return asset.getRevisionComment().replaceAll("\\\n", " ~ ");
                if (asset.getComment() == null)
                    return "";
                return asset.getComment().replaceAll("\\\n", " ~ ");
            case 6:
                if (asset.getLockingUser() == null)
                    return "";
                return asset.getLockingUser();
            case 7:
                return asset.getFormattedModifyDate();
            default:
                return null;
            }
        }
    }

    private void openAsset(WorkflowAsset asset) {
        asset.openFile(new NullProgressMonitor());
    }

    private void openViewCommentsDialog(WorkflowAsset asset) {
        // just use the attribute dialog
        String comment = asset.getRevisionComment();
        if (comment == null)
            comment = asset.getComment();
        AttributeVO attr = new AttributeVO(asset.getLabel(), comment);
        AttributeDialog dialog = new AttributeDialog(getShell(), attr);
        dialog.open();
    }

    /**
     * Show this section for def docs only for >= R5.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowAsset))
            return false;

        WorkflowAsset asset = (WorkflowAsset) toTest;
        return asset.getProject().checkRequiredVersion(5, 0);
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getElement().equals(workflowAsset)) {
            ChangeType type = ece.getChangeType();
            if (type == ChangeType.VERSION_CHANGE || type == ChangeType.PROPERTIES_CHANGE
                    || type == ChangeType.SETTINGS_CHANGE || type == ChangeType.ELEMENT_DELETE
                    || type == ChangeType.ELEMENT_CREATE) {
                notifyLabelChange();
                tableEditor.setElement(workflowAsset);
                setTable();
            }
        }
    }

    private void setTable() {
        List<WorkflowAsset> versions = workflowAsset.getAllVersions();
        if (!tableEditor.getTable().isDisposed()) {
            tableEditor.setValue(versions);
            for (int i = 0; i < versions.size(); i++) {
                if (versions.get(i).getVersion() == workflowAsset.getVersion()) {
                    tableEditor.getTable().select(i);
                    tableEditor.getTable().showSelection();
                }
            }
        }
    }
}
