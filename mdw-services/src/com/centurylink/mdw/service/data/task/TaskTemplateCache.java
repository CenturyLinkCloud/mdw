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
package com.centurylink.mdw.service.data.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.AssetRefCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.AssetRefConverter;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Caches task definitions.
 */
public class TaskTemplateCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static List<TaskTemplate> taskVoCache = new ArrayList<TaskTemplate>();
    private static Map<String,TaskTemplate> templateVersions = new HashMap<String,TaskTemplate>();

    private static final Object lock = new Object();

    public void initialize(Map<String,String> params) {}

    public int getCacheSize() {
        return taskVoCache.size();
    }

    public void clearCache() {
        taskVoCache.clear();
        templateVersions.clear();
    }

    public void loadCache() throws CachingException {
        load();
    }

    private synchronized void load() throws CachingException {
        List<TaskTemplate> taskVoCacheTemp = new ArrayList<TaskTemplate>();
        try {
            List<TaskTemplate> taskTemplates = DataAccess.getProcessLoader().getTaskTemplates();
            for (TaskTemplate taskTemplate : taskTemplates)
                taskVoCacheTemp.add(taskTemplate);
            synchronized(lock) {
                taskVoCache = taskVoCacheTemp;
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    public synchronized void refreshCache() throws CachingException {
        // clearCache();
        loadCache();
    }

    /**
     * Return the latest task for the given name.
     */
    public static TaskTemplate getTemplateForName(String taskName) {
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskTemplate task = taskVoCache.get(i);
            if (task.getTaskName().equals(taskName)) {
                return task;
            }
        }
        return null;
    }

    public static TaskTemplate getTaskTemplate(Long taskId) {
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskTemplate task = taskVoCache.get(i);
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        // Look in Git history using ASSET_REF
        AssetRef ref = AssetRefCache.getAssetRef(taskId);
        if (ref != null) {
            try {
                TaskTemplate template = AssetRefConverter.getTaskTemplate(ref);
                if (template != null) {
                    taskVoCache.add(template);
                    return template;
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * Return the latest task id for the given logical ID
     */
    public static TaskTemplate getTaskTemplate(String logicalId) {
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskTemplate task = taskVoCache.get(i);
            if (logicalId.equals(task.getLogicalId())) {
               return task;
            }
        }
        return null;
    }

    /**
     * Get the latest task template that matched the assetVersionSpec.
     * Uses the task logicalId to match to the taskVoCache, so if the logical ID
     * in the matching asset is not unique, then the latest template with this logicalId
     * will be returned regardless of assetVersionSpec.
     * So CHANGE THE LOGICAL_ID if you want in-flight tasks to use a different template.
     * @throws Exception
     */
    public static TaskTemplate getTaskTemplate(AssetVersionSpec assetVersionSpec) throws Exception {
        TaskTemplate taskTemplate = templateVersions.get(assetVersionSpec.toString());
        if (taskTemplate == null) {
            if (assetVersionSpec.getPackageName() != null) {
                List<Package> pkgVOs = PackageCache.getAllPackages(assetVersionSpec.getPackageName());
                for (Package pkgVO : pkgVOs) {
                    for (TaskTemplate template : pkgVO.getTaskTemplates()) {
                        if (assetVersionSpec.getName().equals(template.getName())) {
                            if (template.meetsVersionSpec(assetVersionSpec.getVersion()) && (taskTemplate == null || template.getVersion() > taskTemplate.getVersion()))
                                taskTemplate = template;
                        }
                    }
                }
            }
            // If didn't find, check ASSET_REF DB table to retrieve from git history
            if (taskTemplate == null && !assetVersionSpec.getVersion().equals("0")) {
                AssetRef ref = AssetRefCache.getAssetRef(assetVersionSpec);
                if (ref != null) {
                    taskTemplate = AssetRefConverter.getTaskTemplate(ref);
                    if (taskTemplate != null)
                        taskVoCache.add(taskTemplate);
                }
            }
            if (taskTemplate != null)
                templateVersions.put(assetVersionSpec.toString(), taskTemplate);
        }
        return taskTemplate;
    }

    public static List<TaskTemplate> getTaskTemplatesForCategory(int categoryId) throws DataAccessException {
        TaskCategory taskCategory = null;
        for (TaskCategory category : DataAccess.getBaselineData().getTaskCategories().values()) {
            if (category.getId().longValue() == categoryId) {
                taskCategory = category;
                break;
            }
        }
        List<TaskTemplate> tasks = new ArrayList<TaskTemplate>();
        if (taskCategory != null) {
            synchronized (lock) {
                for (TaskTemplate taskVO : taskVoCache) {
                    if (taskCategory.getCode().equals(taskVO.getTaskCategory()))
                        tasks.add(taskVO);
                }
            }
        }
        return tasks;
    }

    public static List<TaskTemplate> getTaskTemplates() {
        return taskVoCache;
    }
}
