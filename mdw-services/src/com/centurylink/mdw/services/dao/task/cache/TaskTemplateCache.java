/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.task.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;

/**
 * Caches a shallow version of all the TaskVOs.  When a particular TaskVO
 * is requested, its full blown version is populated just-in-time.
 */
public class TaskTemplateCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static List<TaskVO> taskVoCache = new ArrayList<TaskVO>();
    private static Map<String,TaskVO> templateVersions = new HashMap<String,TaskVO>();

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
        List<TaskVO> taskVoCacheTemp = new ArrayList<TaskVO>();
        try {
    		TaskManager taskMgr = ServiceLocator.getTaskManager();
    		TaskVO[] shallowTaskVos = taskMgr.getShallowTaskVOs();
            for (TaskVO taskVO : shallowTaskVos)
                taskVoCacheTemp.add(taskVO);
            synchronized(lock) {
                taskVoCache = taskVoCacheTemp;
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    public synchronized void refreshCache() throws CachingException {
        // clearCache();
        loadCache();
    }

    /**
     * Return the latest task for the given name.
     */
    public static TaskVO getTaskTemplate(String taskName) {
        TaskVO taskVo = null;
        int idxToReplace = -1;
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskVO task = taskVoCache.get(i);
            if (task.getTaskName().equals(taskName)) {
                taskVo = task;
                if (taskVo.isShallow()) {
                    taskVo = loadTaskTemplate(taskVo.getTaskId());
                    if (taskVo != null)   // Would be null whenever exception occurs trying to pull full version of TaskVO
                        idxToReplace = i;
                }
                break;
            }
        }
        if (idxToReplace >= 0) {
            synchronized (lock) {
                taskVoCache.set(idxToReplace, taskVo);
            }
        }
        return taskVo;
    }

    /**
     * Returns a full-blown TaskVO.  If the cached version is
     * shallow, performs the query to fully populate it.
     */
    public static TaskVO getTaskTemplate(Long taskId) {
        TaskVO taskVo = null;
        int idxToReplace = -1;
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskVO task = taskVoCache.get(i);
            if (task.getTaskId().equals(taskId)) {
                taskVo = task;
                if (taskVo.isShallow()) {
                    taskVo = loadTaskTemplate(taskVo.getTaskId());
                    if (taskVo != null)   // Would be null whenever exception occurs trying to pull full version of TaskVO
                        idxToReplace = i;
                }
                break;
            }
        }
        if (idxToReplace >= 0) {
            synchronized (lock) {
                taskVoCache.set(idxToReplace, taskVo);
            }
        }
        return taskVo;
    }

    private static TaskVO loadTaskTemplate(Long taskId) {
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            return taskMgr.getTaskVO(taskId);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Return the latest task id for the given logical ID
     */
    public static TaskVO getTaskTemplate(String sourceAppName, String logicalId) {
        if (sourceAppName!=null && !sourceAppName.equals(TaskInstanceVO.DETAILONLY) && !sourceAppName.equals(ApplicationContext.getApplicationName())) {
        	int k = sourceAppName.indexOf('@');
        	if (k > 0) {
        	  String app = sourceAppName.substring(0, k);
        	  if (!app.equals(ApplicationContext.getApplicationName()))
                  logicalId = app + ":" + logicalId;
        	}
        	else logicalId = sourceAppName + ":" + logicalId;
        }
        TaskVO taskVo = null;
        int idxToReplace = -1;
        for (int i = 0; i < taskVoCache.size(); i++) {
            TaskVO task = taskVoCache.get(i);
            if (logicalId.equals(task.getLogicalId())) {
                taskVo = task;
                if (taskVo.isShallow()) {
                    taskVo = loadTaskTemplate(taskVo.getTaskId());
                    if (taskVo != null)    // Would be null whenever exception occurs trying to pull full version of TaskVO
                        idxToReplace = i;
                }
                break;
            }
        }
        if (idxToReplace >= 0) {
            synchronized (lock) {
                taskVoCache.set(idxToReplace, taskVo);
            }
        }
        return taskVo;
    }

    /**
     * Get the latest task template that matched the assetVersionSpec.
     * Uses the task logicalId to match to the taskVoCache, so if the logical ID
     * in the matching asset is not unique, then the latest template with this logicalId
     * will be returned regardless of assetVersionSpec.
     * So CHANGE THE LOGICAL_ID if you want in-flight tasks to use a different template.
     */
    public static TaskVO getTaskTemplate(AssetVersionSpec assetVersionSpec) throws CachingException {
        TaskVO taskTemplate = templateVersions.get(assetVersionSpec.toString());
        if (taskTemplate == null) {
            if (assetVersionSpec.getPackageName() != null) {
                List<PackageVO> pkgVOs = PackageVOCache.getAllPackageVOs(assetVersionSpec.getPackageName());
                for (PackageVO pkgVO : pkgVOs) {
                    for (TaskVO template : pkgVO.getTaskTemplates()) {
                        if (assetVersionSpec.getName().equals(template.getName())) {
                            if (template.meetsVersionSpec(assetVersionSpec.getVersion()) && (taskTemplate == null || template.getVersion() > taskTemplate.getVersion()))
                                taskTemplate = template;
                        }
                    }
                }
            }
            if (taskTemplate != null)
                templateVersions.put(assetVersionSpec.toString(), taskTemplate);
        }
        return taskTemplate;
    }

    public static List<TaskVO> getTaskTemplatesForCategory(int categoryId) throws DataAccessException {
        TaskManager taskMgr = ServiceLocator.getTaskManager();
        TaskCategory taskCategory = null;
        for (TaskCategory category : taskMgr.getTaskCategories()) {
            if (category.getId().longValue() == categoryId) {
                taskCategory = category;
                break;
            }
        }
        List<TaskVO> tasks = new ArrayList<TaskVO>();
        if (taskCategory != null) {
            synchronized (lock) {
                for (TaskVO taskVO : taskVoCache) {
                    if (taskCategory.getCode().equals(taskVO.getTaskCategory()))
                        tasks.add(taskVO);
                }
            }
        }
        return tasks;
    }

    public static List<TaskVO> getTaskTemplates() {
        return taskVoCache;
    }
}
