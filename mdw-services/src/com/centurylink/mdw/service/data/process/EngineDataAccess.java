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
package com.centurylink.mdw.service.data.process;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.util.TransactionWrapper;

public interface EngineDataAccess {

    /////// Process methods

    Long createProcessInstance(ProcessInstance procinst)
    throws DataAccessException,SQLException;

    void setProcessElapsedTime(ProcessInstance pi)
    throws SQLException;

    ProcessInstance getProcessInstance(Long procInstId)
    throws DataAccessException,SQLException;

    void setProcessInstanceStatus(Long procInstId, Integer status)
    throws DataAccessException,SQLException;

    void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
    throws SQLException;

    List<ProcessInstance> getProcessInstances(Long procId, String ownerType, Long ownerId)
    throws SQLException;

    List<ProcessInstance> getChildProcessInstances(Long procInstId)
    throws SQLException;

    Integer lockProcessInstance(Long procInstId)
    throws SQLException;

    /////// activity methods

    ActivityInstance getActivityInstance(Long actInstId)
    throws DataAccessException,SQLException;

    Long createActivityInstance(ActivityInstance act)
    throws DataAccessException,SQLException;

    void setActivityInstanceStatus(ActivityInstance actInst, Integer status, String status_message)
    throws DataAccessException,SQLException;

    List<ActivityInstance> getActivityInstances(Long actId, Long procInstId,
            boolean activeOnly, boolean isSynchActivity)
    throws SQLException;

    void updateActivityInstanceEndTime(Long actInstId, Date endtime)
    throws SQLException;

    int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
    throws SQLException;

    Integer lockActivityInstance(Long actInstId)
    throws SQLException;

    /////// transition methods

    Long createTransitionInstance(TransitionInstance vo)
    throws DataAccessException,SQLException;

    void completeTransitionInstance(Long transInstId, Long actInstId)
    throws DataAccessException,SQLException;

    void cancelTransitionInstances(Long procInstId, String comment, Long transId)
    throws SQLException;

    int countTransitionInstances(Long pProcInstId, Long pWorkTransId)
    throws SQLException;

    void determineCompletedTransitions(Long pProcInstId, List<Transition> transitions)
    throws SQLException;

    /////// variables

    Long createVariableInstance(VariableInstance var, Long processInstId)
    throws DataAccessException,SQLException;

    VariableInstance getVariableInstance(Long varInstId)
    throws DataAccessException,SQLException;

    VariableInstance getVariableInstance(Long procInstId, String varname)
    throws SQLException;

    void updateVariableInstance(VariableInstance var)
    throws DataAccessException,SQLException;

    List<VariableInstance> getProcessInstanceVariables(Long processInstanceId)
    throws DataAccessException,SQLException;

    /////// documents

    /**
     * Always goes to the database.
     */
    Document loadDocument(Long documentId, boolean forUpdate)
    throws DataAccessException, SQLException;

    Document getDocument(Long documentId, boolean forUpdate)
    throws DataAccessException,SQLException;

    Long createDocument(Document docvo)
    throws DataAccessException,SQLException;

    Long createDocument(Document docvo, Package pkg)
    throws DataAccessException,SQLException;

    void updateDocumentContent(Long documentId, String content)
    throws DataAccessException,SQLException;

    void updateDocumentInfo(Document docvo)
    throws SQLException;

    /////// events

    void removeEventWaitForActivityInstance(Long activityInstanceId, String reason)
    throws SQLException;

    void removeEventWaitForProcessInstance(Long processInstanceId)
    throws SQLException;

    Long recordEventWait(String eventName, boolean multipleRecepients,
            int preserveSeconds, Long actInstId, String compCode)
    throws SQLException;

    List<EventWaitInstance> recordEventArrive(String eventName, Long documentId)
    throws SQLException;

    Long recordEventLog(String name, String category, String subCategory,
            String source, String ownerType, Long ownerId, String user, String modUser, String comments)
    throws SQLException;

    boolean recordEventFlag(String eventName, int preserveSeconds)
    throws SQLException;

    void persistInternalEvent(String eventId, String message)
    throws SQLException;

    int deleteEventInstance(String eventName)
    throws SQLException;

    EventInstance lockEventInstance(String eventName)
    throws SQLException;

    /////// transaction

    TransactionWrapper startTransaction()
    throws DataAccessException;

    void stopTransaction(TransactionWrapper transaction)
    throws DataAccessException;

    /////// miscellaneous

    DatabaseAccess getDatabaseAccess();

    int getPerformanceLevel();

    void updateDocumentMongoCollection(Document docvo, String newOwnerType);
}
