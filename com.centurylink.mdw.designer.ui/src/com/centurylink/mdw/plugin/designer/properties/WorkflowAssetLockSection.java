/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.DateConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class WorkflowAssetLockSection extends PropertySection
        implements IFilter, ElementChangeListener {
    private WorkflowAsset workflowAsset;

    public WorkflowAsset getWorkflowAsset() {
        return workflowAsset;
    }

    private PropertyEditor lockUserPropertyEditor;
    private PropertyEditor modDatePropertyEditor;
    private PropertyEditor lockButtonPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        if (workflowAsset != null)
            workflowAsset.removeElementChangeListener(this);

        workflowAsset = (WorkflowAsset) selection;
        workflowAsset.addElementChangeListener(this);

        lockUserPropertyEditor.setElement(workflowAsset);
        lockUserPropertyEditor.setValue(workflowAsset.getLockingUser());

        modDatePropertyEditor.setElement(workflowAsset);
        modDatePropertyEditor.setValue(workflowAsset.getModifyDate());

        lockButtonPropertyEditor.setElement(workflowAsset);
        lockButtonPropertyEditor.setLabel(workflowAsset.isLockedToUser() ? "Unlock" : "Lock");
        lockButtonPropertyEditor.setEnabled(
                workflowAsset.isLockedToUser() || (workflowAsset.getLockingUser() == null
                        && workflowAsset.isUserAuthorized(UserRoleVO.ASSET_DESIGN)));
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        workflowAsset = (WorkflowAsset) selection;

        // lock user read-only text field
        lockUserPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
        lockUserPropertyEditor.setLabel("Locked By");
        lockUserPropertyEditor.setWidth(150);
        lockUserPropertyEditor.setReadOnly(true);
        lockUserPropertyEditor.render(composite);

        // lock date read-only text field
        modDatePropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
        modDatePropertyEditor.setLabel("Lock Date");
        modDatePropertyEditor.setWidth(150);
        modDatePropertyEditor.setValueConverter(new DateConverter());
        modDatePropertyEditor.setReadOnly(true);
        modDatePropertyEditor.render(composite);

        // lock button
        lockButtonPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_BUTTON);
        lockButtonPropertyEditor.setLabel("");
        lockButtonPropertyEditor.setWidth(65);
        lockButtonPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                toggleLock();
            }
        });
        lockButtonPropertyEditor.render(composite);
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getElement().equals(workflowAsset)) {
            if (ece.getChangeType().equals(ChangeType.PROPERTIES_CHANGE)) {
                WorkflowAsset changedVersion = (WorkflowAsset) ece.getElement();
                if (!lockUserPropertyEditor.getValue().equals(changedVersion.getLockingUser())) {
                    lockUserPropertyEditor.setValue(changedVersion.getLockingUser());
                    modDatePropertyEditor.setValue(changedVersion.getModifyDate());
                    lockButtonPropertyEditor
                            .setLabel(changedVersion.isLockedToUser() ? "Unlock" : "Lock");
                }
                notifyLabelChange();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (workflowAsset != null)
            workflowAsset.removeElementChangeListener(this);
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowAsset))
            return false;

        WorkflowAsset wa = (WorkflowAsset) toTest;
        if (wa.getProject().isFilePersist() || wa.isArchived())
            return false;

        return true;
    }

    private void toggleLock() {
        final boolean lock = !workflowAsset.isLockedToUser();
        final IEditorPart editor = workflowAsset.findOpenEditor();
        if (editor != null && editor.isDirty() && !lock)
            getPart().getSite().getPage().closeEditor(editor, true);

        final StringBuffer errorMessage = new StringBuffer();

        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                try {
                    String lockMessage = workflowAsset.getProject().getDesignerProxy()
                            .toggleWorkflowAssetLock(workflowAsset, lock);
                    if (lockMessage != null && lockMessage.length() > 0) {
                        errorMessage.append(lockMessage);
                    }
                    else {
                        if (editor != null) {
                            getPart().getSite().getPage().closeEditor(editor, false);
                            workflowAsset.openFile(new NullProgressMonitor());
                        }
                        if (lock) {
                            getDesignerProxy().loadWorkflowAsset(workflowAsset);
                            modDatePropertyEditor.setValue(workflowAsset.getModifyDate());
                            lockUserPropertyEditor.setValue(workflowAsset.getLockingUser());
                            lockButtonPropertyEditor.setLabel("Unlock");
                        }
                        else {
                            workflowAsset.setLockingUser(null);
                            modDatePropertyEditor.setValue("");
                            lockUserPropertyEditor.setValue("");
                            lockButtonPropertyEditor.setLabel("Lock");
                        }
                    }
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    errorMessage.append(ex.getMessage());
                }
            }
        });

        if (errorMessage.toString().length() > 0)
            PluginMessages.uiError(errorMessage.toString(), "Lock/Unlock Resource",
                    workflowAsset.getProject());
    }
}