/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.model.data.task.TaskState;

public class TaskAdvisories extends TaskStates
{
  public List<SelectItem> list()
  {
    List<SelectItem> list = new ArrayList<SelectItem>();
    list.add(new SelectItem("-1", "[Not Invalid]"));
    list.add(new SelectItem("0", ""));
    list.add(new SelectItem(TaskState.STATE_ALERT.toString(), "Alert"));
    list.add(new SelectItem(TaskState.STATE_JEOPARDY.toString(), "Jeopardy"));
    list.add(new SelectItem(TaskState.STATE_INVALID.toString(), "Invalid"));
    return list;
  }
}
