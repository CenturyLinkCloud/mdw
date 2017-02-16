/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

/**
 * Special section for task variables. For 5.5 this section is replaced by
 * TaskTemplateEditor.
 */
public class TaskVariablesSection extends DesignSection implements IFilter {
    private Activity activity;

    /**
     * Override to reflect newly-added process variables
     */
    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;
        reconcileProcessVariables(activity);
        super.setSelection(selection);
    }

    private void reconcileProcessVariables(Activity activity) {
        String attrVal = activity.getAttribute("Variables");
        attrVal = TaskVO.updateVariableInString(attrVal, activity.getProcess().getVariables());
        activity.setAttribute("Variables", attrVal);
    }

    @Override
    public boolean selectForSection(PropertyEditor propertyEditor) {
        return PropertyEditor.SECTION_VARIABLES.equals(propertyEditor.getSection());
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;

        if (!activity.isManualTask())
            return false;

        if (activity.isForProcessInstance())
            return false;

        PropertyEditorList propEditorList = new PropertyEditorList(activity);
        for (PropertyEditor propertyEditor : propEditorList) {
            // return true if any widgets specify Variables section
            if (PropertyEditor.SECTION_VARIABLES.equals(propertyEditor.getSection()))
                return true;
        }

        return false;
    }
}
