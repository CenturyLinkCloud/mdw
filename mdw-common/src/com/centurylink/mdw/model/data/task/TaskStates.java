/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.task;

import java.util.HashMap;
import java.util.Map;

public class TaskStates {
    private static Map<Integer,String> taskStates = new HashMap<Integer,String>();
    
    static {
      for (int i = 0; i < TaskState.allTaskStateCodes.length; i++)
      {
        taskStates.put(TaskState.allTaskStateCodes[i], TaskState.allTaskStateNames[i]);
      }
    }
    
    public static Map<Integer,String> getTaskStates() { return taskStates; }

}
