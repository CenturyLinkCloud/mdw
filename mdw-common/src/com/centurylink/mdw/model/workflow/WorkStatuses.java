/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkStatuses
{
  private static Map<Integer,String> workStatuses = new HashMap<Integer,String>();
  private static Map<String,Integer> nameToCode = new HashMap<String,Integer>();

  static
  {
    for (int i = 0; i < WorkStatus.allStatusCodes.length; i++)
    {
      workStatuses.put(WorkStatus.allStatusCodes[i], WorkStatus.allStatusNames[i]);
      nameToCode.put(WorkStatus.allStatusNames[i], WorkStatus.allStatusCodes[i]);
    }
  }

  public static Map<Integer,String> getWorkStatuses() { return workStatuses; }

  public static String getName(Integer code) {
      String name = workStatuses.get(code);
      if (WorkStatus.STATUSNAME_PENDING_PROCESS.equals(name))
          return WorkStatus.STATUSNAME_PENDING;
      else if (WorkStatus.STATUSNAME_CANCELLED.equals(name))
          return WorkStatus.STATUSNAME_CANCELED;
      else
          return name;
  }
  public static Integer getCode(String name) {
      if (WorkStatus.STATUSNAME_PENDING.equals(name))
          return WorkStatus.STATUS_PENDING_PROCESS;
      else if (WorkStatus.STATUSNAME_CANCELED.equals(name))
          return WorkStatus.STATUS_CANCELLED;
      else
        return nameToCode.get(name);
  }
}
