/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.service.data.activity;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches activities according to their implementor.
 */
@RegisteredService(value=CacheService.class)
public class ActivityCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<String,List<Activity>> activities = new HashMap<>();

    public static List<Activity> getActivities(String implementor) throws DataAccessException {
        return getActivities(implementor, false);
    }

    /**
     * Find activities in process defs with the specified implementor class.
     */
    public static List<Activity> getActivities(String implementor, boolean withArchived) throws DataAccessException {
        synchronized (ActivityCache.class) {
            List<Activity> matchingActivities = activities.get(implementor);
            if (matchingActivities == null) {
                matchingActivities = loadActivities(implementor, withArchived);
                activities.put(implementor, matchingActivities);
            }
            return matchingActivities;
        }
    }

    private static List<Activity> loadActivities(String implementor, boolean withArchived) throws DataAccessException {
        List<Activity> loadedActivities = new ArrayList<>();
        long before = System.currentTimeMillis();
        List<Process> processes;
        if (withArchived)
            processes = ProcessCache.getAllProcesses();
        else
            processes = DataAccess.getProcessLoader().getProcessList(false);
        for (Process process : processes) {
            process = ProcessCache.getProcess(process.getId());
            for (Activity activity : process.getActivities()) {
                if (implementor.equals(activity.getImplementor())) {
                    activity.setPackageName(process.getPackageName());
                    activity.setProcessName(process.getName());
                    activity.setProcessVersion(process.getVersionString());
                    activity.setProcessId(process.getId());
                    loadedActivities.add(activity);
                }
            }
        }
        long elapsed = System.currentTimeMillis() - before;
        logger.debug(implementor + " activities loaded in " + elapsed + " ms");
        return loadedActivities;
    }

    @Override
    public void refreshCache() {
        // cache is lazily loaded
        clearCache();
    }

    @Override
    public void clearCache() {
        synchronized (ActivityCache.class) {
            activities.clear();
        }
    }
}
