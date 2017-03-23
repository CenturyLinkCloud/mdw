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

import java.util.List;

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.AttributeValueChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.TemplatedNotifier;

/**
 * For 5.5 this section is replaced by TaskTemplateEditor.
 */
public class TaskNoticesSection extends DesignSection implements IFilter {
    private Activity activity;
    private NoticesValueChangeListener noticesValueChangeListener = new NoticesValueChangeListener();

    /**
     * Override to reflect newly-added process variables.
     */
    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;
        initializeNotices(activity);
        super.setSelection(selection);
        activity.addAttributeValueChangeListener(noticesValueChangeListener);
    }

    private void initializeNotices(Activity activity) {
        String attrVal = activity.getAttribute(TaskAttributeConstant.NOTICES);
        if (StringHelper.isEmpty(attrVal) || attrVal.equals("$DefaultNotices")) {
            attrVal = activity.getProject().checkRequiredVersion(5, 5) ? TaskVO.getDefaultNotices()
                    : TaskVO.getDefaultCompatibilityNotices();
            activity.setAttribute(TaskAttributeConstant.NOTICES, attrVal);
        }
    }

    @Override
    public boolean selectForSection(PropertyEditor propertyEditor) {
        return PropertyEditor.SECTION_NOTICES.equals(propertyEditor.getSection());
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;

        if (activity.isForProcessInstance())
            return false;

        if (activity.isManualTask()) {
            PropertyEditorList propEditorList = new PropertyEditorList(activity);
            for (PropertyEditor propertyEditor : propEditorList) {
                // return true if any widgets specify Notices section
                if (PropertyEditor.SECTION_NOTICES.equals(propertyEditor.getSection()))
                    return true;
            }
        }

        return false;
    }

    class NoticesValueChangeListener extends AttributeValueChangeListener {
        public NoticesValueChangeListener() {
            super("Notices");
        }

        @Override
        public void attributeValueChanged(String newValue) {
            int columnCount = StringHelper
                    .delimiterColumnCount(newValue.substring(0, newValue.indexOf(";")), ",", "\\,"); // to
                                                                                                     // maintain
                                                                                                     // compatibility
                                                                                                     // for
                                                                                                     // Notices
                                                                                                     // with/without
                                                                                                     // asset
                                                                                                     // version
            int notifierCol = columnCount > 3 ? 3 : 2;

            List<String[]> rows = StringHelper.parseTable(newValue, ',', ';', columnCount);
            boolean changed = false;
            for (String[] row : rows) {
                if (!StringHelper.isEmpty(row[1]) && StringHelper.isEmpty(row[notifierCol])) {
                    if (activity.getProject().checkRequiredVersion(5, 5))
                        row[notifierCol] = TemplatedNotifier.DEFAULT_NOTIFIER_IMPL;
                    else // old notifier impl
                        row[notifierCol] = "com.qwest.mdw.workflow.task.observer.TaskEmailNotifier";
                    changed = true;
                }
                else if (StringHelper.isEmpty(row[1]) && !StringHelper.isEmpty(row[notifierCol])) {
                    row[notifierCol] = "";
                    changed = true;
                }
            }
            if (changed) {
                for (AttributeVO attribute : activity.getAttributes()) {
                    if (attribute.getAttributeName().equals("Notices")) {
                        attribute.setAttributeValue(StringHelper.serializeTable(rows));
                    }
                }
                TaskNoticesSection.super.setSelection(activity);
            }
        }
    }
}
