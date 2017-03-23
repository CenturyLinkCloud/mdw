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
package com.centurylink.mdw.plugin.project.extensions;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.TableModelUpdater;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ExtensionModulesTable {
    private WorkflowProject workflowProject;

    public ExtensionModulesTable(WorkflowProject workflowProject) {
        this.workflowProject = workflowProject;
    }

    private List<ExtensionModule> selectedModules = new ArrayList<ExtensionModule>();

    public List<ExtensionModule> getSelectedModules() {
        return selectedModules;
    }

    public void setSelectedModules(List<ExtensionModule> list) {
        this.selectedModules = list;
        List<DefaultRowImpl> tableRows = new ArrayList<DefaultRowImpl>();
        for (ExtensionModule module : WorkflowProjectManager.getInstance()
                .getAvailableExtensions(workflowProject)) {
            String[] rowData = new String[3];
            if (selectedModules.contains(module))
                rowData[0] = "true";
            else
                rowData[0] = "false";
            rowData[1] = module.getName();
            rowData[2] = module.getDescription();
            DefaultRowImpl row = tableEditor.new DefaultRowImpl(rowData);
            tableRows.add(row);
        }

        tableEditor.setValue(tableRows);
    }

    private TableEditor tableEditor;

    public TableEditor getTableEditor() {
        return tableEditor;
    }

    public TableEditor create() {
        tableEditor = new TableEditor(null, TableEditor.TYPE_TABLE);

        // colspecs
        List<ColumnSpec> colSpecs = new ArrayList<ColumnSpec>();
        ColumnSpec selectionColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX, "", "import");
        selectionColSpec.width = 80;
        colSpecs.add(selectionColSpec);
        ColumnSpec extensionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Extension",
                "extension");
        extensionColSpec.width = 150;
        extensionColSpec.readOnly = true;
        colSpecs.add(extensionColSpec);
        ColumnSpec descriptionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Description",
                "description");
        descriptionColSpec.readOnly = true;
        descriptionColSpec.width = 350;
        colSpecs.add(descriptionColSpec);

        tableEditor.setColumnSpecs(colSpecs);
        tableEditor.setFillWidth(true);
        tableEditor.setModelUpdater(new TableModelUpdater() {
            public Object create() {
                return null;
            }

            @SuppressWarnings("rawtypes")
            public void updateModelValue(List tableValue) {
                selectedModules = new ArrayList<ExtensionModule>();
                for (ExtensionModule module : WorkflowProjectManager.getInstance()
                        .getAvailableExtensions(workflowProject)) {
                    for (Object rowObj : tableValue) {
                        DefaultRowImpl row = (DefaultRowImpl) rowObj;
                        if (module.getName().equals(row.getColumnValue(1))) {
                            if (((Boolean) row.getColumnValue(0)).booleanValue())
                                selectedModules.add(module);
                            else
                                selectedModules.remove(module);
                        }
                    }
                }
            }
        });

        return tableEditor;
    }
}
