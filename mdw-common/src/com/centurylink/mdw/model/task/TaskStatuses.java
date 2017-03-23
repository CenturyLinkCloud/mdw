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