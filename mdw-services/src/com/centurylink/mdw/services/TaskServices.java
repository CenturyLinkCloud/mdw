/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.Task;
import com.centurylink.mdw.common.service.types.TaskAction;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.task.TaskCount;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.task.SubTask;

public interface TaskServices {
    /**
     * Create a manual task instance that points to a remote task.
     * The base URL for accessing the remote task is included as the "appUrl" parameter.
     *
     * @param task
     * @param parameters
     */
    public void createTask(Task task, Map<String,Object> parameters) throws ServiceException;

    /**
     * Update a task instance.
     *
     * @param task
     * @param parameters
     */
    public void updateTask(Task task, Map<String,Object> parameters) throws ServiceException;

    /**
     * Perform the designated action on a task instance.
     *
     * @param taskAction
     * @param parameters
     */
    public void performActionOnTask(TaskAction taskAction, Map<String,Object> parameters) throws ServiceException;

    /**
     * Returns all the tasks assigned to a particular user.
     */
    public TaskList getUserTasks(String cuid) throws TaskException, DataAccessException;

    /**
     * Returns tasks associated with the specified user's workgroups.
     */
    public TaskList getWorkgroupTasks(String cuid, Query query) throws TaskException, UserException, DataAccessException;

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException;

    public Map<String,String> getIndexes(Long taskInstanceId) throws DataAccessException;

    public TaskInstanceVO createCustomTaskInstance(String logicalId, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId) throws TaskException, DataAccessException;

    public TaskInstanceVO createAutoFormTaskInstance(String logicalId, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, FormDataDocument formDoc) throws TaskException, DataAccessException;

    public TaskInstanceVO getInstance(Long instanceId) throws DataAccessException;

    /**
     * Returns a map of name to runtime Value.
     */
    public Map<String,Value> getValues(Long instanceId) throws ServiceException;

    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException;

    public void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
    throws TaskException, DataAccessException;

    public void createSubTasks(List<SubTask> subTaskList, TaskRuntimeContext masterTaskContext)
    throws TaskException, DataAccessException;

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException;

    public List<TaskCount> getTopThroughputTasks(String aggregateBy, Query query) throws ServiceException;

    public Map<Date,List<TaskCount>> getTaskInstanceBreakdown(Query query) throws ServiceException;

}
