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

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

/**
 * General activity design tab section.
 */
public class DesignSection extends PropertySection implements IFilter {
    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    protected PropertyEditorList propertyEditors;

    private Label warningLabel;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;

        if (propertyEditors != null) {
            for (PropertyEditor propertyEditor : propertyEditors) {
                propertyEditor.dispose();
            }
        }
        if (warningLabel != null) {
            warningLabel.dispose();
        }

        if (activity.getActivityImpl().getAttrDescriptionXml() == null) {
            warningLabel = new Label(composite, SWT.NONE);
            warningLabel.setText("Please configure this attribute's parameters.");
        }
        else {
            propertyEditors = new PropertyEditorList(activity);
            for (PropertyEditor propertyEditor : propertyEditors) {
                if (selectForSection(propertyEditor)) {
                    preRender(propertyEditor);
                    propertyEditor.render(composite);
                    propertyEditor.setValue(activity);
                    if (!propertyEditor.getType().equals(PropertyEditor.TYPE_LINK)) {
                        propertyEditor.setEditable(
                                !(propertyEditor.isReadOnly() || activity.isReadOnly()));
                    }
                }
            }
        }

        composite.layout(true);
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        // widget creation is deferred until setSelection()
    }

    /**
     * Opportunity to dynamically populate property editors before render.
     * 
     * @param activity
     * @param propertyEditors
     */
    protected void preRender(PropertyEditor propertyEditor) {
        // do nothing by default
    }

    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }

    /**
     * Not shown for script activities since they have the Script tab. Also,
     * subprocesses have separate SubProcessDesignSection (except InvokeHetero).
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (activity.isForProcessInstance())
            return false;

        if (activity.isScript() || activity.isExpressionEval() || activity.isDynamicJava()
                || activity.isOsgiAdapter())
            return false;

        return true;
    }

    public boolean selectForSection(PropertyEditor propertyEditor) {
        if (propertyEditor.getSection() != null)
            return false;

        if ("DocumentVariables".equals(propertyEditor.getSource()))
            return !showScriptSection(activity);

        return true;
    }
}