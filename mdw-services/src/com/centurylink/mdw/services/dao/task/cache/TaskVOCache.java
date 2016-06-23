/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.task.cache;

import java.util.Collection;
import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.task.TaskVO;

/**
 * @deprecated @use TaskTemplateCache
 */
@Deprecated
public class TaskVOCache {

    public static List<TaskVO> getAllTasks() throws DataAccessException {
        return TaskTemplateCache.getTaskTemplates();
    }

    public static Collection<TaskVO> getTaskVOs() {
        return TaskTemplateCache.getTaskTemplates();
    }

    public static Long getTaskId(String taskName) {
        TaskVO taskVo = TaskTemplateCache.getTaskTemplate(taskName);
        return taskVo == null ? null : taskVo.getTaskId();
    }

    public static Long getTaskId(String sourceAppName, String logicalId) {
        TaskVO taskVo = TaskTemplateCache.getTaskTemplate(sourceAppName, logicalId);
        return taskVo == null ? null : taskVo.getTaskId();
    }

    public static List<TaskVO> getTasksForCategory(int categoryId) throws DataAccessException {
        return TaskTemplateCache.getTaskTemplatesForCategory(categoryId);
    }

    public static TaskVO getTaskVO(Long taskId) {
        return TaskTemplateCache.getTaskTemplate(taskId);
    }

    public static String getTaskName(Long id) {
        TaskVO taskVo = TaskTemplateCache.getTaskTemplate(id);
        return taskVo == null ? null : taskVo.getTaskName();
    }
}
