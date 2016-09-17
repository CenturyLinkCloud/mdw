/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;

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

    ProcessInstanceVO getProcessInstanceForSecondary(String pSecOwner, Long pSecOwnerId)
    throws DataAccessException;

    ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId, Long eventInstId)
    throws DataAccessException;

    public String getExternalEventDetails(Long externalEventId)
    throws DataAccessException;

    List<TaskInstanceVO> getTaskInstancesForProcessInstance(Long processInstId)
    throws DataAccessException;

    ProcessInstanceVO getCauseForTaskInstance(Long pTaskInstanceId)
    throws DataAccessException;

    DocumentVO getDocument(Long documentId)
    throws DataAccessException;

    public List<DocumentVO> findDocuments(Long procInstId, String type, String searchKey1, String searchKey2,
            String ownerType, Long ownerId, Date createDateStart, Date createDateEnd, String orderByClause)
    throws DataAccessException;

    public void updateVariableInstance(VariableInstanceInfo var)
    throws DataAccessException;

    public void updateDocumentContent(Long documentId, String content)
    throws DataAccessException;

    public List<Long> findTaskInstance(Long taskId, String masterRequestId)
    throws DataAccessException;

    public List<EventLog> getEventLogs(String pEventName, String pEventSource, String pEventOwner, Long pEventOwnerId)
    throws DataAccessException;

    public List<TaskActionVO> getUserTaskActions(String[] groups, Date startDate)
    throws DataAccessException;

    public ProcessInstanceVO getProcessInstanceForCalling(Long procInstId)
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
