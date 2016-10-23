/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.task;

import java.util.HashMap;
import java.util.Map;

public class TaskStatuses
{
  private static Map<Integer,String> taskStatuses = new HashMap<Integer,String>();

  static {
    for (int i = 0; i < TaskStatus.allStatusCodes.length; i++)
    {
      taskStatuses.put(TaskStatus.allStatusCodes[i], TaskStatus.allStatusNames[i]);
    }
  }

  public static Map<Integer,String> getTaskStatuses() { return taskStatuses; }

  public static String getName(Integer code) {
      String name = taskStatuses.get(code);
      if (TaskStatus.STATUSNAME_CANCELLED.equals(name))
          return TaskStatus.STATUSNAME_CANCELED;
      else
          return name;
  }
  public static Integer getCode(String name) {
      if (TaskStatus.STATUSNAME_CANCELED.equals(name))
          return TaskStatus.STATUS_CANCELLED;
      else
        return TaskStatus.getStatusCodeForName(name);
  }
}