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
package com.centurylink.mdw.model.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkStatuses {
    private static Map<Integer, String> workStatuses = new HashMap<>();
    private static Map<String, Integer> nameToCode = new HashMap<>();

    static {
        for (int i = 0; i < WorkStatus.allStatusCodes.length; i++) {
            workStatuses.put(WorkStatus.allStatusCodes[i], WorkStatus.allStatusNames[i]);
            nameToCode.put(WorkStatus.allStatusNames[i], WorkStatus.allStatusCodes[i]);
        }
    }

    public static Map<Integer, String> getWorkStatuses() {
        return workStatuses;
    }

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
