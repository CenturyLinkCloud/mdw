/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.status;

import java.util.HashMap;
import java.util.Map;

public class GlobalApplicationStatus {
    public static final String ONLINE = "Available";

    public static final String OFFLINE = "Offline";

    public static final String SUMMARY_TASK_APPNAME = "SummaryTaskManager";

    private static GlobalApplicationStatus sysStatusSingleton;

    private Map<String, String> systemStatusMap;

    public Map<String, String> getSystemStatusMap() {
        return systemStatusMap;
    }

    public void setSystemStatusMap(Map<String, String> systemStatusMap) {
        this.systemStatusMap = systemStatusMap;
    }

    public GlobalApplicationStatus() {
        systemStatusMap = new HashMap<String, String>();
    }

    public static GlobalApplicationStatus getInstance() {
        if (sysStatusSingleton == null) {
            sysStatusSingleton = new GlobalApplicationStatus();
        }
        return sysStatusSingleton;
    }

    public void setAppStatus(String systemName, String status) {
        this.getSystemStatusMap().put(systemName, status);
    }

    public boolean getDetailTaskManagersStatus() {
        if (systemStatusMap.isEmpty())
            return true;
        for (String key : systemStatusMap.keySet()) {
            if (OFFLINE.equals(systemStatusMap.get(key)))
                return false;
        }
        return true;
    }

    public String getOfflineSystemMsg() {
        StringBuffer msg = new StringBuffer("Unable to process the request. The detail task manager(s) are offline: ");
        for (String key : systemStatusMap.keySet()) {
            if (OFFLINE.equals(systemStatusMap.get(key))) {
                msg.append(key).append(" ");
            }
        }
        return msg.toString();
    }
}
