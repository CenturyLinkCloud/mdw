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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.TableModelUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;

public class PreferencesSection extends PropertySection implements IFilter {
    private WorkflowProject project;

    public WorkflowProject getProject() {
        return project;
    }

    private TableEditor noticesEditor;
    private PropertyEditor intervalEditor;

    public void setSelection(WorkflowElement selection) {
        project = (WorkflowProject) selection;

        noticesEditor.setElement(project);
        List<String> selectedNotices = project.getNoticeChecks();
        noticesEditor.setValue(getSelectedNoticeRows(selectedNotices));
        intervalEditor.setValue(project.getNoticeCheckInterval());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        project = (WorkflowProject) selection;

        createNoticesTable();
        noticesEditor.render(composite, false);

        intervalEditor = new PropertyEditor(selection, PropertyEditor.TYPE_SPINNER);
        intervalEditor.setLabel("Check Interval (mins)");
        intervalEditor.setWidth(50);
        intervalEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                project.setNoticeCheckInterval(Integer.parseInt((String) newValue));
            }
        });
        intervalEditor.render(composite);
        intervalEditor.setMinValue(1);
        intervalEditor.setMaxValue(60);
        intervalEditor.setElement(project);
        intervalEditor.setValue(project.getNoticeCheckInterval());
    }

    private void createNoticesTable() {
        noticesEditor = new TableEditor(null, TableEditor.TYPE_TABLE);
        noticesEditor.setLabel("Desktop Notices");

        // colspecs
        List<ColumnSpec> colSpecs = new ArrayList<ColumnSpec>();
        ColumnSpec selectionColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX, "", "selected");
        selectionColSpec.width = 50;
        colSpecs.add(selectionColSpec);
        ColumnSpec extensionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Task Outcome",
                "outcome");
        extensionColSpec.width = 150;
        extensionColSpec.readOnly = true;
        colSpecs.add(extensionColSpec);

        noticesEditor.setColumnSpecs(colSpecs);
        noticesEditor.setHorizontalSpan(3);
        noticesEditor.setWidth(300);
        noticesEditor.setModelUpdater(new TableModelUpdater() {
            public Object create() {
                return null;
            }

            @SuppressWarnings("rawtypes")
            public void updateModelValue(List tableValue) {
                List<String> selectedNotices = new ArrayList<String>();
                for (Action outcome : UserActionVO.NOTIFIABLE_TASK_ACTIONS) {
                    for (Object rowObj : tableValue) {
                        DefaultRowImpl row = (DefaultRowImpl) rowObj;
                        if (outcome.toString().equals(row.getColumnValue(1))) {
                            if (((Boolean) row.getColumnValue(0)).booleanValue())
                                selectedNotices.add(outcome.toString());
                            else
                                selectedNotices.remove(outcome.toString());
                        }
                    }
                }
                project.setNoticeChecks(selectedNotices);
            }
        });
    }

    private List<DefaultRowImpl> getSelectedNoticeRows(List<String> selectedNotices) {
        List<DefaultRowImpl> tableRows = new ArrayList<DefaultRowImpl>();
        for (Action outcome : UserActionVO.NOTIFIABLE_TASK_ACTIONS) {
            String[] rowData = new String[2];
            if (selectedNotices.contains(outcome.toString()))
                rowData[0] = "true";
            else
                rowData[0] = "false";
            rowData[1] = outcome.toString();
            DefaultRowImpl row = noticesEditor.new DefaultRowImpl(rowData);
            tableRows.add(row);
        }
        return tableRows;
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProject))
            return false;

        return true;
    }
}
