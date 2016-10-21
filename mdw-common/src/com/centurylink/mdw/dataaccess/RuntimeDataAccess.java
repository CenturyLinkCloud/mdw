/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.variable.DocumentVO;

public interface RuntimeDataAccess {

    boolean hasProcessInstances(Long processId)
    throws DataAccessException;

    ProcessInstanceVO getProcessInstanceBase(Long procInstId)
    throws DataAccessException;

    ProcessInstanceVO getProcessInstanceAll(Long procInstId)
    throws DataAccessException;

    /**
     * Same as getProcessInstanceAll(), except returns null if not found.
     */
    ProcessInstanceVO getProcessInstance(Long instanceId) throws DataAccessException;

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

    List<ProcessInstanceVO> getProcessInstanceList(String owner, String secondaryOwner, Long secondaryOwnerId, String orderBy)
    throws DataAccessException;

    DocumentVO getDocument(Long documentId)
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
