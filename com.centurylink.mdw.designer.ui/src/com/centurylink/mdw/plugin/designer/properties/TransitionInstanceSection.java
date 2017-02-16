/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.model.data.work.WorkTransitionStatus;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

public class TransitionInstanceSection extends PropertySection implements IFilter {
    private Transition transition;

    public Transition getTransition() {
        return transition;
    }

    private TableEditor tableEditor;
    private List<ColumnSpec> columnSpecs;

    private TransitionInstanceContentProvider contentProvider;
    private TransitionInstanceLabelProvider labelProvider;

    public void setSelection(WorkflowElement selection) {
        transition = (Transition) selection;

        tableEditor.setElement(transition);
        tableEditor.setValue(transition.getInstances());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        transition = (Transition) selection;
        tableEditor = new TableEditor(transition, TableEditor.TYPE_TABLE);
        tableEditor.setReadOnly(true);

        if (columnSpecs == null)
            columnSpecs = createColumnSpecs();
        tableEditor.setColumnSpecs(columnSpecs);

        if (contentProvider == null)
            contentProvider = new TransitionInstanceContentProvider();
        tableEditor.setContentProvider(contentProvider);

        if (labelProvider == null)
            labelProvider = new TransitionInstanceLabelProvider();
        tableEditor.setLabelProvider(labelProvider);

        tableEditor.render(composite);

    }

    private List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec instanceIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID",
                "instanceId");
        instanceIdColSpec.width = 100;
        columnSpecs.add(instanceIdColSpec);

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

    class TransitionInstanceContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<WorkTransitionInstanceVO> rows = (List<WorkTransitionInstanceVO>) inputElement;
            return rows.toArray(new WorkTransitionInstanceVO[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class TransitionInstanceLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            WorkTransitionInstanceVO transitionInstanceVO = (WorkTransitionInstanceVO) element;

            switch (columnIndex) {
            case 0:
                return transitionInstanceVO.getTransitionInstanceID().toString();
            case 1:
                String status = null;
                for (int i = 0; i < WorkTransitionStatus.allStatusCodes.length; i++) {
                    if (WorkTransitionStatus.allStatusCodes[i]
                            .equals(transitionInstanceVO.getStatusCode()))
                        status = WorkTransitionStatus.allStatusNames[i];
                }
                return status;
            case 2:
                if (transitionInstanceVO.getStartDate() == null)
                    return "";
                return transitionInstanceVO.getStartDate();
            case 3:
                if (transitionInstanceVO.getEndDate() == null)
                    return "";
                return transitionInstanceVO.getEndDate();
            default:
                return null;
            }
        }
    }

    /**
     * For IFilter interface.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Transition))
            return false;

        transition = (Transition) toTest;
        return transition.hasInstanceInfo();
    }
}