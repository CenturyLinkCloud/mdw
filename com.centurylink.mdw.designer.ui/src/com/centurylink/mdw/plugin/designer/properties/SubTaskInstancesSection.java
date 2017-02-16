/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ITableLabelProvider;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;

public class SubTaskInstancesSection extends TaskInstancesSection implements IFilter {
    public void setSelection(WorkflowElement selection) {
        setActivity((Activity) selection);
        setTaskInstances(getActivity().getSubTaskInstances());
    }

    @Override
    protected List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = super.createColumnSpecs();
        ColumnSpec masterInstColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Master Task Inst.",
                "masterInstanceId");
        masterInstColSpec.width = 100;
        columnSpecs.add(0, masterInstColSpec);
        return columnSpecs;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        return new SubTaskInstanceLabelProvider();
    }

    class SubTaskInstanceLabelProvider extends TaskInstanceLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            TaskInstanceVO taskInstanceVO = (TaskInstanceVO) element;
            ColumnSpec colspec = getColumnSpecs().get(columnIndex);
            if (colspec.property.equals("masterInstanceId"))
                return taskInstanceVO.getMasterTaskInstanceId() == null ? ""
                        : taskInstanceVO.getMasterTaskInstanceId().toString();
            else
                return super.getColumnText(element, columnIndex);
        }
    }

    /**
     * For IFilter interface.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (activity.hasInstanceInfo() && activity.isManualTask())
            return activity.getSubTaskInstances() != null;
        return false;
    }

}
