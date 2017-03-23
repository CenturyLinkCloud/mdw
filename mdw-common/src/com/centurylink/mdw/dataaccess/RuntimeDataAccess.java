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
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;

public interface RuntimeDataAccess {

    boolean hasProcessInstances(Long processId)
    throws DataAccessException;

    ProcessInstance getProcessInstanceBase(Long procInstId)
    throws DataAccessException;

    ProcessInstance getProcessInstanceAll(Long procInstId)
    throws DataAccessException;

    /**
     * Same as getProcessInstanceAll(), except returns null if not found.
     */
    ProcessInstance getProcessInstance(Long instanceId) throws DataAccessException;

    int deleteProcessInstancesForProcess(Long processId)
    throws DataAccessException;

    int deleteProcessInstances(List<Long> processInstanceIds)
    throws DataAccessException;

    ProcessList getProcessInstanceList(Map<String,String> criteria, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException;

    /**
     * Include variable values in criteria.
     */
    ProcessList getProcessInstanceList(Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException;

    /**
     * Include variables in returned list, and use variable values criteria in selecting.
     */
    ProcessList getProcessInstanceList(Map<String,String> criteria, List<String> variableNames, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException;

    List<ProcessInstance> getProcessInstanceList(String owner, String secondaryOwner, Long secondaryOwnerId, String orderBy)
    throws DataAccessException;

    Document getDocument(Long documentId)
    throws DataAccessException;

    public List<EventLog> getEventLogs(String pEventName, String pEventSource, String pEventOwner, Long pEventOwnerId)
    throws DataAccessException;

    /**
     * Returns the top-level linked process in the call chain for the specified instance.
     * Downstream calls include all routes, whereas upstream calls include only the specific instance stack.
     */
    public LinkedProcessInstance getProcessInstanceCallHierarchy(Long processInstanceId)
    throws DataAccessException;

    public ActivityList getActivityInstanceList(Query query)
    throws DataAccessException;
}
