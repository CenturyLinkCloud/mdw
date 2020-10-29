package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.DocumentDbAccess;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.util.TransactionWrapper;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface EngineDataAccess {

    Long createProcessInstance(ProcessInstance procinst)
    throws DataAccessException,SQLException;

    void setProcessCompletionTime(ProcessInstance pi)
    throws SQLException;

    void setActivityCompletionTime(ActivityInstance ai)
    throws SQLException;

    ProcessInstance getProcessInstance(Long procInstId)
    throws DataAccessException,SQLException;

    void setProcessInstanceStatus(Long procInstId, Integer status)
    throws DataAccessException,SQLException;

    void setProcessInstanceStartTime(Long processInstanceId)
    throws SQLException;

    void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
    throws SQLException;

    List<ProcessInstance> getProcessInstances(Long procId, String ownerType, Long ownerId)
    throws SQLException;

    List<ProcessInstance> getChildProcessInstances(Long procInstId)
    throws SQLException;

    Integer lockProcessInstance(Long procInstId)
    throws SQLException;

    List<ProcessInstance> getProcessInstancesByMasterRequestId(String masterRequestId)
    throws SQLException;

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

    int countActivityInstances(Long procInstId, Long activityId)
    throws SQLException;

    Integer lockActivityInstance(Long actInstId)
    throws SQLException;

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

    Long createVariableInstance(VariableInstance var, Long processInstId, Package pkg)
    throws DataAccessException,SQLException;

    VariableInstance getVariableInstance(Long varInstId)
    throws DataAccessException,SQLException;

    VariableInstance getVariableInstance(Long procInstId, String varname)
    throws SQLException;

    void updateVariableInstance(VariableInstance var, Package pkg)
    throws DataAccessException,SQLException;

    List<VariableInstance> getProcessInstanceVariables(Long processInstanceId)
    throws DataAccessException,SQLException;

    /**
     * Always goes to the database.
     */
    Document loadDocument(Long documentId, boolean forUpdate)
    throws DataAccessException, SQLException;

    Document getDocument(Long documentId, boolean forUpdate)
    throws DataAccessException, SQLException;

    Long createDocument(Document doc, Package pkg)
    throws DataAccessException, SQLException;

    void updateDocumentContent(Long documentId, String content)
    throws DataAccessException, SQLException;

    void updateDocumentInfo(Document doc)
    throws SQLException;

    Long getRequestCompletionTime(String ownerType, Long ownerId)
    throws SQLException;

    void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime)
    throws SQLException;

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

    void persistInternalEvent(String eventId, String message)
    throws SQLException;

    int deleteEventInstance(String eventName)
    throws SQLException;

    EventInstance lockEventInstance(String eventName)
    throws SQLException;

    TransactionWrapper startTransaction()
    throws DataAccessException;

    void stopTransaction(TransactionWrapper transaction)
    throws DataAccessException;

    DatabaseAccess getDatabaseAccess();
    DocumentDbAccess getDocumentDbAccess();

    int getPerformanceLevel();
}
