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
package com.centurylink.mdw.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskAggregate;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.task.types.TaskList;

public interface TaskServices {

    /**
     * Returns tasks (optionally associated with the specified user's workgroups).
     */
    TaskList getTasks(Query query, String cuid) throws ServiceException;
    TaskList getTasks(Query query) throws ServiceException;

    Map<String,String> getIndexes(Long taskInstanceId) throws ServiceException;
    void updateIndexes(Long taskInstanceId, Map<String,String> indexes) throws ServiceException;

    TaskInstance getInstance(Long instanceId) throws ServiceException;

    /**
     * Returns a map of name to runtime Value.
     */
    Map<String,Value> getValues(Long instanceId) throws ServiceException;

    void applyValues(Long instanceId, Map<String,String> values) throws ServiceException;

    TaskInstance createTask(Long taskId, String masterRequestId, Long procInstId,
            String secOwner, Long secOwnerId, String name, String comments) throws ServiceException, DataAccessException;
    /**
     * Create an ad-hoc manual task instance.
     * @return the newly-created instance
     */
    TaskInstance createTask(String logicalId, String userCuid, String title, String comments, Instant due) throws ServiceException;

    void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
    throws ServiceException, DataAccessException;

    TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException;

    List<TaskTemplate> getTaskTemplates(Query query) throws ServiceException;

    List<TaskAggregate> getTopTasks(Query query) throws ServiceException;

    TreeMap<Date,List<TaskAggregate>> getTaskBreakdown(Query query) throws ServiceException;

    TaskRuntimeContext getContext(Long instanceId) throws ServiceException;
    TaskRuntimeContext getContext(TaskInstance taskInstance) throws ServiceException;

    void performTaskAction(UserTaskAction taskAction) throws ServiceException;
    TaskInstance performAction(Long taskInstanceId, String action, String userCuid, String assigneeCuid, String comment,
            String destination, boolean notifyEngine) throws ServiceException;

    List<EventLog> getHistory(Long taskInstanceId) throws ServiceException;

    /**
     * Update a task instance.
     */
    void updateTask(String userCuid, TaskInstance taskInstance) throws ServiceException;

    List<TaskInstance> getTaskInstancesForProcess(Long processInstanceId)
            throws ServiceException, DataAccessException;
    void cancelTaskInstancesForProcess(Long processInstanceId)
            throws ServiceException, DataAccessException;

    void cancelTaskForActivity(Long activityInstanceId) throws ServiceException, DataAccessException;

    TaskInstance getTaskInstanceForActivity(Long activityInstanceId)
            throws ServiceException, DataAccessException;

    List<String> getGroupsForTaskInstance(TaskInstance taskInstance)
            throws DataAccessException, ServiceException;

    /**
     * Only allowable task actions based on instance status are returned (defined in mdw-task-actions.xml).
     * However, if query "custom" filter is true all custom actions are returned regardless of allowable.
     * TODO: custom actions should always be allowed unless they appear in mdw-task-actions.xml to restrict.
     */
    List<TaskAction> getActions(Long instanceId, String userCuid, Query query) throws ServiceException;

    void updateTaskInstanceState(Long taskInstId, boolean isAlert)
            throws DataAccessException, ServiceException;

    Map<String, List<TaskTemplate>> getTaskTemplatesByPackage(Query query) throws ServiceException;

    void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws ServiceException;
}
