/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ToolboxFilterDialog extends TrayDialog {
    private WorkflowProject workflowProject;
    private List<String> suppressedImplementors;

    public List<String> getSuppressedImplementors() {
        return suppressedImplementors;
    }

    private CheckboxTreeViewer treeViewer;

    public ToolboxFilterDialog(Shell shell, WorkflowProject workflowProject,
            List<String> suppressedImpls) {
        super(shell);
        this.workflowProject = workflowProject;
        this.suppressedImplementors = suppressedImpls;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Show Toolbox Items");
        composite.getShell().setImage(workflowProject.getDefaultPackage().getIconImage());

        createMessageArea(composite);
        createPackageTree(composite);
        createSelectButtons(composite);
        return (composite);
    }

    protected Label createMessageArea(Composite composite) {
        Label label = new Label(composite, SWT.NONE);
        label.setText("Activity Implementors to display in Toolbox View");
        label.setFont(composite.getFont());
        GridData data = new GridData(GridData.FILL_BOTH);
        label.setLayoutData(data);
        return label;
    }

    private void createPackageTree(Composite parent) {
        treeViewer = new CheckboxTreeViewer(parent,
                SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        treeViewer.setContentProvider(new ViewContentProvider());
        treeViewer.setLabelProvider(new ViewLabelProvider());
        treeViewer.setCheckStateProvider(new ViewCheckStateProvider());
        treeViewer.setInput(workflowProject.getTopLevelUserVisiblePackages());
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 400;
        treeViewer.getTree().setLayoutData(data);
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                boolean checked = event.getChecked();
                if (event.getElement() instanceof WorkflowPackage) {
                    WorkflowPackage pkg = (WorkflowPackage) event.getElement();
                    for (ActivityImpl impl : pkg.getActivityImpls()) {
                        if (checked)
                            suppressedImplementors.remove(impl.getImplClassName());
                        else if (!suppressedImplementors.contains(impl.getImplClassName()))
                            suppressedImplementors.add(impl.getImplClassName());
                    }
                    treeViewer.refresh();
                }
                else if (event.getElement() instanceof ActivityImpl) {
                    ActivityImpl impl = (ActivityImpl) event.getElement();
                    if (checked)
                        suppressedImplementors.remove(impl.getImplClassName());
                    else if (!suppressedImplementors.contains(impl.getImplClassName()))
                        suppressedImplementors.add(impl.getImplClassName());
                    treeViewer.refresh();
                }
            }
        });
        ColumnViewerToolTipSupport.enableFor(treeViewer);
    }

    private void createSelectButtons(Composite parent) {
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_SPACING);
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

        Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID,
                "Select All", false);
        selectButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                suppressedImplementors = new ArrayList<String>();
                treeViewer.refresh();
            }
        });

        Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID,
                "Deselect All", false);
        deselectButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                suppressedImplementors = new ArrayList<String>();
                for (WorkflowPackage pkg : workflowProject.getTopLevelUserVisiblePackages()) {
                    for (ActivityImpl impl : pkg.getActivityImpls()) {
                        if (!suppressedImplementors.contains(impl.getImplClassName()))
                            suppressedImplementors.add(impl.getImplClassName());
                    }
                }
                treeViewer.refresh();
            }
        });
    }

    class ViewContentProvider implements ITreeContentProvider {
        public Object[] getElements(Object inputElement) {
            return ((List<?>) inputElement).toArray(new WorkflowPackage[0]);
        }

        public boolean hasChildren(Object element) {
            if (element instanceof WorkflowPackage) {
                WorkflowPackage pkg = (WorkflowPackage) element;
                return pkg.getActivityImpls() != null && !pkg.getActivityImpls().isEmpty();
            }
            else {
                return false;
            }
        }

        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof WorkflowPackage) {
                WorkflowPackage pkg = (WorkflowPackage) parentElement;
                return pkg.getActivityImpls() == null ? null
                        : pkg.getActivityImpls().toArray(new ActivityImpl[0]);
            }
            else {
                return null;
            }
        }

        public Object getParent(Object element) {
            return null;
        }

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }
    }

    class ViewLabelProvider extends ColumnLabelProvider {
        public String getText(Object element) {
            if (element instanceof WorkflowPackage) {
                return (((WorkflowPackage) element).getName());
            }
            else if (element instanceof ActivityImpl) {
                ActivityImpl impl = (ActivityImpl) element;
                return impl.getLabel();
            }
            else {
                return super.getText(element);
            }
        }

        public String getToolTipText(Object element) {
            if (element instanceof ActivityImpl)
                return ((ActivityImpl) element).getImplClassName();
            else
                return super.getToolTipText(element);
        }
    }

    class ViewCheckStateProvider implements ICheckStateProvider {
        public boolean isChecked(Object element) {
            if (element instanceof WorkflowPackage) {
                WorkflowPackage pkg = (WorkflowPackage) element;
                boolean noneChecked = true;
                for (ActivityImpl impl : pkg.getActivityImpls()) {
                    if (!suppressedImplementors.contains(impl.getImplClassName())) {
                        noneChecked = false;
                        break;
                    }
                }
                return !noneChecked;
            }
            else if (element instanceof ActivityImpl) {
                ActivityImpl impl = (ActivityImpl) element;
                return !suppressedImplementors.contains(impl.getImplClassName());
            }
            else {
                return false;
            }
        }

        public boolean isGrayed(Object element) {
            if (element instanceof WorkflowPackage) {
                WorkflowPackage pkg = (WorkflowPackage) element;
                boolean allChecked = true;
                boolean noneChecked = true;
                for (ActivityImpl impl : pkg.getActivityImpls()) {
                    if (suppressedImplementors.contains(impl.getImplClassName()))
                        allChecked = false;
                    else
                        noneChecked = false;
                }
                return !allChecked && !noneChecked;
            }
            else {
                return false;
            }
        }
    }

}
