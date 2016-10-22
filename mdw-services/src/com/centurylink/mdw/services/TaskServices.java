/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskCount;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.task.SubTask;

public interface TaskServices {

    /**
     * Returns tasks (optionally associated with the specified user's workgroups).
     */
    public TaskList getTasks(Query query, String cuid) throws ServiceException;

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException;

    public Map<String,String> getIndexes(Long taskInstanceId) throws DataAccessException;

    public TaskInstanceVO createCustomTaskInstance(AssetVersionSpec spec, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId) throws TaskException, DataAccessException, CachingException;

    public TaskInstanceVO createAutoFormTaskInstance(AssetVersionSpec spec, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, FormDataDocument formDoc) throws TaskException, DataAccessException, CachingException;

    public TaskInstanceVO getInstance(Long instanceId) throws DataAccessException;

    /**
     * Returns a map of name to runtime Value.
     */
    public Map<String,Value> getValues(Long instanceId) throws ServiceException;

    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException;

    /**
     * Create an ad-hoc manual task instance.
     * @return the newly-created instance id
     */
    public Long createTask(String userCuid, String logicalId) throws ServiceException;

    public void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
    throws TaskException, DataAccessException;

    public void createSubTasks(List<SubTask> subTaskList, TaskRuntimeContext masterTaskContext)
    throws TaskException, DataAccessException;

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException;

    public List<TaskVO> getTaskTemplates(Query query) throws ServiceException;

    public List<TaskCount> getTopThroughputTasks(String aggregateBy, Query query) throws ServiceException;

    public Map<Date,List<TaskCount>> getTaskInstanceBreakdown(Query query) throws ServiceException;

    public TaskRuntimeContext getRuntimeContext(Long instanceId) throws ServiceException;

    public void performTaskAction(TaskActionVO taskAction, Query query) throws ServiceException;

    /**
     * Update a task instance.
     */
    public void updateTask(String userCuid, TaskInstanceVO taskInstance) throws ServiceException;
}
