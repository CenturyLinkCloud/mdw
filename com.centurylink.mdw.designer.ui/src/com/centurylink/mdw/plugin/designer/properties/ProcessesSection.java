/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

public class ProcessesSection extends DesignSection implements IFilter {
    @Override
    public boolean selectForSection(PropertyEditor propertyEditor) {
        return PropertyEditor.SECTION_PROCESSES.equals(propertyEditor.getSection());
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (activity.isForProcessInstance())
            return false;

        PropertyEditorList propEditorList = new PropertyEditorList(activity);
        for (PropertyEditor propertyEditor : propEditorList) {
            // return true if any widgets specify PROCESSES section
            if (PropertyEditor.SECTION_PROCESSES.equals(propertyEditor.getSection()))
                return true;
        }

        return false;
    }
}
