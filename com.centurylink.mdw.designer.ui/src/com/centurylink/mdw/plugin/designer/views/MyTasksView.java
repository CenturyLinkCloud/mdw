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
package com.centurylink.mdw.plugin.designer.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class MyTasksView extends ListView {
    public MyTasksView() {
        super("myTasks");
    }

    protected List<ColumnSpec> createColumnSpecs() {
        List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec taskInstIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Task ID",
                "taskInstanceId");
        taskInstIdColSpec.width = 100;
        columnSpecs.add(taskInstIdColSpec);

        ColumnSpec taskNameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "taskName");
        taskNameColSpec.width = 180;
        columnSpecs.add(taskNameColSpec);

        ColumnSpec masterRequestColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT,
                "Master Request ID", "orderId");
        masterRequestColSpec.width = 180;
        columnSpecs.add(masterRequestColSpec);

        ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
        statusColSpec.width = 100;
        columnSpecs.add(statusColSpec);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start",
                "startDateAsDate");
        startDateColSpec.width = 150;
        startDateColSpec.dateFormat = df;

        columnSpecs.add(startDateColSpec);

        ColumnSpec dueDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Due", "dueDate");
        dueDateColSpec.width = 150;
        dueDateColSpec.dateFormat = df;
        columnSpecs.add(dueDateColSpec);

        return columnSpecs;
    }

    @Override
    public Object[] getElements() {
        return getProject().getDesignerProxy().getMyTasks().toArray();
    }

}
