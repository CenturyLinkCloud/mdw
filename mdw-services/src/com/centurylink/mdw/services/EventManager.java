/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.data.monitor.CertifiedMessage;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.model.data.monitor.UnscheduledEvent;
import com.centurylink.mdw.model.value.event.EventInstanceVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.services.event.ServiceHandler;
import com.centurylink.mdw.services.event.WorkflowHandler;

public interface EventManager {

    public void createAuditLog(UserActionVO userAction)
    throws DataAccessException, EventException;

    /**
     * @param pEventName
     * @param pEventCategory
     * @param pEventSubCat
     * @param pEventSource
     * @param pEventOwner
     * @param pEventOwnerId
     * @param user
     * @param comments
     * @return id
     */
    public Long createEventLog(String pEventName, String pEventCategory, String pEventSubCat, String pEventSource,
        String pEventOwner, Long pEventOwnerId, String user, String modUser, String comments)
    throws DataAccessException, EventException;

    public List<EventLog> getEventLogs(String pEventName, String pEventSource,
    	    String pEventOwner, Long pEventOwnerId) throws DataAccessException;


    /**
     * This method is helping ListenerHelper and external event handler to get
     * a transaction scope.
     * @param clsname
     * @param request
     * @param metainfo
     * @return
     * @throws Exception
     */
    public String processExternalEvent(String clsname, String request, Map<String,String> metainfo)
	throws Exception;

    ////////////////////////////////////////////
    // notify/create process instances when receiving external events
    // and invoke synchronous process
    ////////////////////////////////////////////

    /**
     * Method that notifies the event wait instances based on the passed in params
     *
     * @param pEventName
     * @param pEventSource
     * @param pEventOwner
     * @param pEventOwnerId
     * @param pCompCode
     * @param pParams
     * @return Boolean Status
     */
    public Integer notifyProcess(String pEventName, Long pEventInstId, String message, int delay)
    throws DataAccessException, EventException;

    /**
     * Creates the process instance
     *
     * @param pProcessId
     * @param pProcessOwner
     * @param pProcessOwnerId
     * @param pSecondaryOwner
     * @param pSecondaryOwnerId
     * @return new WorkInstance
     */
    public ProcessInstanceVO createProcessInstance(Long pProcessId, String pProcessOwner,
        Long pProcessOwnerId, String pSecondaryOwner, Long pSecondaryOwnerId, String pMasterRequestId)
    throws ProcessException, DataAccessException;

    ////////////////////////////////////////////
    // get/set variable instances and documents
    ////////////////////////////////////////////

    /**
     * get variable instance by its ID
     */
    public VariableInstanceInfo getVariableInstance(Long varInstId)
    throws DataAccessException;

    /**
     * Get variable instance for a process instance.
     * The method does not take care of embedded process, so the caller needs to pass
     * the parent process instance id when looking for variables for embedded process
     */
    public VariableInstanceInfo getVariableInstance(Long procInstId, String name)
    throws DataAccessException;

    /**
     * updates the variable instance.
     * The method takes care of document variables, for which the data need to be
     * real data.
     * The method does not take care of embedded process, so the caller needs to pass
     * the parent process instance id when looking for variables for embedded process
     *
     * @param pVarInstId
     * @param value
     */
    public void updateVariableInstance(Long pVarInstId, Object value)
    throws ProcessException, com.centurylink.mdw.common.exception.DataAccessException;

    /**
     * Set the variable instance value.
     * The method does not take care of document variables, for which you must
     * pass in DocumentReferenceObject.
     * The method does not take care of embedded process, so the caller needs to pass
     * the parent process instance id when looking for variables for embedded process
     *
     * @param procInstId
     * @param name
     * @param value
     * @return
     * @throws DataAccessException
     */
    public VariableInstanceInfo setVariableInstance(Long procInstId, String name, Object value)
    throws DataAccessException;

    public List<VariableInstanceInfo> getProcessInstanceVariables(Long procInstId)
    throws DataAccessException;

    public DocumentVO getDocumentVO(Long documentId)
    throws DataAccessException;

    public void updateDocumentContent(Long docid, Object doc, String type)
    throws DataAccessException;

    public void updateDocumentInfo(Long docid, String documentType, String ownerType, Long ownerId)
    throws DataAccessException;

    @Deprecated
    public Long createDocument(String type, String ownerType, Long ownerId, Object doc)
    throws DataAccessException;

    public Long createDocument(String type, String ownerType, Long ownerId, Object doc, PackageVO pkg)
    throws DataAccessException;

    ////////////////////////////////////////////
    // send delay events
    ////////////////////////////////////////////

    public void sendDelayEventsToWaitActivities(String masterRequestId)
	throws DataAccessException, ProcessException;


    public void retryActivity(Long activityId, Long activityInstId)
    throws DataAccessException, ProcessException;

    /**
     * skip the activity by sending an activity finish event, with the given completion code.
     * The status of the activity instance is c
     * @param activityId
     * @param activityInstId
     * @param completionCode
     * @throws DataAccessException
     * @throws ProcessException
     */
    public void skipActivity(Long activityId, Long activityInstId, String completionCode)
    throws DataAccessException, ProcessException;

    ////////////////////////////////////////////
    // get process/activity/transition instances and update process instance status
    ////////////////////////////////////////////

    /**
     * Returns the transition instance object by ID
     *
     * @param transInstId
     * @return transition instance object
     */
    public WorkTransitionInstanceVO getWorkTransitionInstance(Long transInstId)
    throws DataAccessException, ProcessException;

    /**
     * Returns the ActivityInstance identified by the passed in Id
     *
     * @param pActivityInstId
     * @return ActivityInstance
     */
    public ActivityInstanceVO getActivityInstance(Long pActivityInstId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the activity instances by process name, activity logical ID, and  master request ID.
     *
     * @param processName
     * @param activityLogicalId
     * @param masterRequestId
     * @return the list of activity instances. If the process definition or the activity
     * 		with the given logical ID is not found, null
     * 		or no such activity instances are found, an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    public List<ActivityInstanceVO> getActivityInstances(String masterRequestId,
    		String processName, String activityLogicalId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the Process instance object identified by the passed in Id
     *
     * @param procInstId
     * @return ProcessInstanceVO
     */
    public ProcessInstanceVO getProcessInstance(Long procInstId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the ProcessInstance and associated variable  identified by the passed in Id
     *
     * @param pProcInstId
     * @param loadVariables
     * @return ProcessInstance
     */
    public ProcessInstanceVO getProcessInstance(Long procInstId, boolean loadVariables)
    throws ProcessException, DataAccessException;

    /**
     * Returns Document VO
     * @param forUpdate
     * @throws com.centurylink.mdw.common.exception.DataAccessException
     */
    public DocumentVO getDocument(DocumentReference docRef, boolean forUpdate)
    throws ProcessException, com.centurylink.mdw.common.exception.DataAccessException;

    /**
     * Returns the process instances by process name and master request ID.
     *
     * @param processName
     * @param masterRequestId
     * @return the list of process instances. If the process definition is not found, null
     * 		is returned; if process definition is found but no process instances are found,
     * 		an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    public List<ProcessInstanceVO> getProcessInstances(String masterRequestId, String processName)
    throws ProcessException, DataAccessException;

    /**
     * Returns the process instance based on the passed in params
     *
     * @param pProcessId Unique Identifier for the process
     * @param pOwner Owner type of the process instance
     * @param pOwnerId Id for the owner
     * @return Process instance object
     */
    public List<ProcessInstanceVO> getProcessInstances(Long pProcessId, String pOwner, Long pOwnerId)
    throws ProcessException, DataAccessException;

    /**
     * Update the status of the process instance. Used by task manager.
     * @param pProcInstId
     * @param status
     * @throws ProcessException
     * @throws DataAccessException
     */
    public void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws ProcessException, DataAccessException;

    ////////////////////////////////////////////
    // find/get process definition
    ////////////////////////////////////////////

    /**
     * get the process definition by process ID
     * @param pProcessId process ID
     * @return process definition
     */
    public ProcessVO getProcessVO(Long pProcessId)
    throws DataAccessException, ProcessException;

    /**
     * Find the process definition ID by process name and version.
     * Throws DataAccessException when the process is not found
     * @param name
     * @param version
     * @return ID of the process
     * @throws DataAccessException
     */
    public Long findProcessId(String name, int version)
    throws DataAccessException;

    ////
    //// the followings are for scheduled events (timer tasks and long-delayed internal messages)
    ////

    /**
     * Load all internal event and scheduled jobs before cutoff time.
     * If cutoff time is null, load only unscheduled events
     */
    public List<ScheduledEvent> getScheduledEventList(Date cutoffTime)
    throws DataAccessException;

    /**
     * Load all internal events older than the specified time up to a max of batchSize.
     */
    public List<UnscheduledEvent> getUnscheduledEventList(Date olderThan, int batchSize)
    throws DataAccessException;

    public void offerScheduledEvent(ScheduledEvent event)
    throws DataAccessException;

    public void processScheduledEvent(String eventName, Date now)
    throws DataAccessException;

    public boolean processUnscheduledEvent(String eventName);

    public List<UnscheduledEvent> processInternalEvents(List<UnscheduledEvent> eventList);

    ////
    //// the followings are for certified messages
    ////
    public List<CertifiedMessage> getCertifiedMessageList()
    throws DataAccessException;

    public void recordCertifiedMessage(CertifiedMessage message)
    throws DataAccessException;

    public boolean deliverCertifiedMessage(CertifiedMessage message,
    		int ackTimeout, int maxTries, int retryInterval);

    public boolean consumeCertifiedMessage(String messageId)
    throws DataAccessException;

    public void updateCertifiedMessageStatus(String msgid, Integer status)
    throws DataAccessException;

    ////
    //// for event management GUI
    ////

    public void createEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds)
    throws DataAccessException;

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds)
    throws DataAccessException;

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds, String comments)
    throws DataAccessException;

    public void createEventWaitInstance(String eventName,
            Long actInstId,  String compCode)
    throws DataAccessException;

    public int getTableRowCount(String tableName, String whereClause)
    throws DataAccessException;

    public List<String[]> getTableRowList(String tableName, Class<?>[] types, String[] fields,
    		String whereClause, String orderby, boolean descending, int startRow, int rowCount)
    throws DataAccessException;

    public int deleteTableRow(String tableName, String fieldName, Object fieldValue)
    throws DataAccessException;

    public void createTableRow(String tableName, String[] fieldNames, Object[] fieldValues)
	throws DataAccessException;

    public int updateTableRow(String tableName, String keyName, Object keyValue,
		String[] fieldNames, Object[] fieldValues)
    throws DataAccessException;

    //
    // miscellaneous
    //

    public void setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
    throws DataAccessException;

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
    throws DataAccessException;

    public Map<String,String> getAttributes(String ownerType, Long ownerId)
    throws DataAccessException;

    public void sendInternalEvent(String message)
    throws ProcessException;

    public EventInstanceVO getEventInstance(String eventName)
    throws DataAccessException;

    /**
     * Register a service handler to respond to MDW listener requests.
     * The handlers's getProtocol() and getPath() methods are used to uniquely
     * identify the types of requests that it responds to.
     */
    public void registerServiceHandler(ServiceHandler handler)
    throws EventException;

    public void unregisterServiceHandler(ServiceHandler handler)
    throws EventException;

    public ServiceHandler getServiceHandler(String protocol, String path)
    throws EventException;

    /**
     * Register a workflow handler to respond to activity triggers.
     * The handlers's getAsset() and getParameters() methods are used to uniquely identify
     * the types of flows the handler responds to.
     * Note: asset should include the workflow package (eg: MyPackage/MyCamelRoute.xml).
     */
    public void registerWorkflowHandler(WorkflowHandler handler)
    throws EventException;

    public void unregisterWorkflowHandler(WorkflowHandler handler)
    throws EventException;

    public WorkflowHandler getWorkflowHandler(String asset, Map<String,String> parameters)
    throws EventException;

    public ProcessVO findProcessByProcessInstanceId(Long processInstanceId)
    throws DataAccessException, ProcessException;

}
