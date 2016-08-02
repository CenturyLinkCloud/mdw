/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class MyTasksView extends ListView
{
  public MyTasksView()
  {
    super("myTasks");
  }

  protected List<ColumnSpec> createColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();
    
    ColumnSpec taskInstIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Task ID", "taskInstanceId");
    taskInstIdColSpec.width = 100;
    columnSpecs.add(taskInstIdColSpec);
    
    ColumnSpec taskNameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "taskName");
    taskNameColSpec.width = 180;
    columnSpecs.add(taskNameColSpec);

    ColumnSpec masterRequestColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Master Request ID", "orderId");
    masterRequestColSpec.width = 180;
    columnSpecs.add(masterRequestColSpec);

    ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
    statusColSpec.width = 100;
    columnSpecs.add(statusColSpec);

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start", "startDateAsDate");
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
  public Object[] getElements()
  {
    return getProject().getDesignerProxy().getMyTasks().toArray();
  }
  
}
