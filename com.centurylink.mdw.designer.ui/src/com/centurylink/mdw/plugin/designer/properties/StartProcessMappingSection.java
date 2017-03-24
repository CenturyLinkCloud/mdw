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
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.MappingEditor;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class StartProcessMappingSection extends PropertySection implements IFilter {
    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    private MappingEditor mappingEditor;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;

        mappingEditor.setElement(activity);
        mappingEditor.setMappedVariables(getInputVariables());
        mappingEditor.setValueAttr("Parameters");
        mappingEditor.initValue();
        mappingEditor.setEditable(
                !activity.isReadOnly() && !mappingEditor.getMappedVariables().isEmpty());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        activity = (Activity) selection;

        mappingEditor = new MappingEditor(activity);
        mappingEditor.setOwningProcess(activity.getProcess());
        mappingEditor.setMappedVariables(getInputVariables());
        mappingEditor.setVariableColumnLabel("Input Variable");
        mappingEditor.setBindingColumnLabel("Binding Expression");

        mappingEditor.render(composite);
    }

    private Map<Integer, VariableVO> getInputVariables() {
        Map<Integer, VariableVO> inputVariables = new TreeMap<Integer, VariableVO>();
        List<VariableVO> inputVariableVOs = activity.getProcess().getInputVariables();
        for (int i = 0; i < inputVariableVOs.size(); i++) {
            VariableVO inputVariableVO = (VariableVO) inputVariableVOs.get(i);
            inputVariables.put(new Integer(i), inputVariableVO);
        }
        return inputVariables;
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        activity = (Activity) toTest;

        if (activity.isForProcessInstance())
            return false;

        return activity.isProcessStart();
    }
}