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

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class OutputSection extends PropertySection implements IFilter {
    private AutomatedTestCase testCase;

    public AutomatedTestCase getTestCase() {
        return testCase;
    }

    private PropertyEditor outputEditor;

    public void setSelection(WorkflowElement selection) {
        testCase = (AutomatedTestCase) selection;

        outputEditor.setElement(testCase);
        File outputFile = testCase.getOutputFile();
        if (outputFile == null || !outputFile.exists()) {
            outputEditor.setValue((String) null);
        }
        else {
            try {
                outputEditor.setValue(new String(PluginUtil.readFile(outputFile)));
            }
            catch (IOException ex) {
                PluginMessages.log(ex);
                outputEditor.setValue(ex.toString());
            }
        }
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        testCase = (AutomatedTestCase) selection;

        outputEditor = new PropertyEditor(selection, PropertyEditor.TYPE_TEXT);
        outputEditor.setMultiLine(true);
        outputEditor.setSpan(4);
        outputEditor.setIndent(5);
        outputEditor.setWidth(600);
        outputEditor.setHeight(175);
        outputEditor.render(composite);
        outputEditor.setEditable(false);
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof AutomatedTestCase))
            return false;

        return true;
    }
}
