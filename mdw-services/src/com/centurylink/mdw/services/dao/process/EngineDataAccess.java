/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.process;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.event.EventInstanceVO;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public interface EngineDataAccess {

	/////// Process methods

	Long createProcessInstance(ProcessInstanceVO procinst)
	throws DataAccessException,SQLException;

	ProcessInstanceVO getProcessInstance(Long procInstId)
    throws DataAccessException,SQLException;

	void setProcessInstanceStatus(Long procInstId, Integer status)
    throws DataAccessException,SQLException;

	void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
	throws SQLException;

	List<ProcessInstanceVO> getProcessInstances(Long procId, String ownerType, Long ownerId)
    throws SQLException;

	List<ProcessInstanceVO> getChildProcessInstances(Long procInstId)
    throws SQLException;

    Integer lockProcessInstance(Long procInstId)
    throws SQLException;

	/////// activity methods

	ActivityInstanceVO getActivityInstance(Long actInstId)
	throws DataAccessException,SQLException;

	Long createActivityInstance(ActivityInstanceVO act)
	throws DataAccessException,SQLException;

	void setActivityInstanceStatus(ActivityInstanceVO actInst, Integer status, String status_message)
    throws DataAccessException,SQLException;

	List<ActivityInstanceVO> getActivityInstances(Long actId, Long procInstId,
			boolean activeOnly, boolean isSynchActivity)
	throws SQLException;

	void updateActivityInstanceEndTime(Long actInstId, Date endtime)
	throws SQLException;

	int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
	throws SQLException;

	Integer lockActivityInstance(Long actInstId)
	throws SQLException;

	/////// transition methods

	Long createTransitionInstance(WorkTransitionInstanceVO vo)
    throws DataAccessException,SQLException;

	void completeTransitionInstance(Long transInstId, Long actInstId)
	throws DataAccessException,SQLException;

	void cancelTransitionInstances(Long procInstId, String comment, Long transId)
    throws SQLException;

	int countTransitionInstances(Long pProcInstId, Long pWorkTransId)
    throws SQLException;

	void determineCompletedTransitions(Long pProcInstId, List<WorkTransitionVO> transitions)
	throws SQLException;

	/////// variables

	Long createVariableInstance(VariableInstanceInfo var, Long processInstId)
    throws DataAccessException,SQLException;

	VariableInstanceInfo getVariableInstance(Long varInstId)
	throws DataAccessException,SQLException;

	VariableInstanceInfo getVariableInstance(Long procInstId, String varname)
	throws SQLException;

	void updateVariableInstance(VariableInstanceInfo var)
	throws DataAccessException,SQLException;

	List<VariableInstanceInfo> getProcessInstanceVariables(Long processInstanceId)
    throws DataAccessException,SQLException;

	/////// documents

	/**
	 * Always goes to the database.
	 */
	DocumentVO loadDocument(Long documentId, boolean forUpdate)
	throws DataAccessException, SQLException;

	DocumentVO getDocument(Long documentId, boolean forUpdate)
	throws DataAccessException,SQLException;

	Long createDocument(DocumentVO docvo)
	throws DataAccessException,SQLException;

    Long createDocument(DocumentVO docvo, PackageVO pkg)
    throws DataAccessException,SQLException;

    void updateDocumentContent(Long documentId, String content)
	throws DataAccessException,SQLException;

	void updateDocumentInfo(DocumentVO docvo)
	throws SQLException;

	List<DocumentVO> findDocuments(Long procInstId, String type, String searchKey1, String searchKey2,
            String ownerType, Long ownerId, Date createDateStart, Date createDateEnd, String orderByClause)
    throws SQLException;

	/////// events

	void removeEventWaitForActivityInstance(Long activityInstanceId, String reason)
	throws SQLException;

	void removeEventWaitForProcessInstance(Long processInstanceId)
	throws SQLException;

	Long recordEventWait(String eventName, boolean multipleRecepients,
            int preserveSeconds, Long actInstId, String compCode)
	throws SQLException;

	List<EventWaitInstanceVO> recordEventArrive(String eventName, Long documentId)
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

    EventInstanceVO lockEventInstance(String eventName)
    throws SQLException;

    /////// transaction

	TransactionWrapper startTransaction()
	throws DataAccessException;

	void stopTransaction(TransactionWrapper transaction)
	throws DataAccessException;

	/////// miscellaneous

	DatabaseAccess getDatabaseAccess();

	int getPerformanceLevel();

}
