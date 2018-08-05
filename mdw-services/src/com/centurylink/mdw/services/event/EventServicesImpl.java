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
package com.centurylink.mdw.services.event;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.monitor.UnscheduledEvent;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.InternalEventDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

public class EventServicesImpl implements EventServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void createAuditLog(UserAction userAction) throws DataAccessException, EventException {
        String name = userAction.getAction().equals(Action.Other) ? userAction.getExtendedAction() : userAction.getAction().toString();
        String comment = userAction.getDescription();
        if (userAction.getAction() == UserAction.Action.Forward)
            comment = comment == null ? userAction.getDestination() : comment + " > " + userAction.getDestination();
        String modUser = null;
        if (userAction.getAction() == UserAction.Action.Assign)
            modUser = userAction.getDestination();
        createEventLog(name, EventLog.CATEGORY_AUDIT, "User Action",
            userAction.getSource(), userAction.getEntity().toString(), userAction.getEntityId(), userAction.getUser(), modUser, comment);
    }

    /**
     * Method that creates the event log based on the passed in params
     *
     * @param pEventName
     * @param pEventCat
     * @param pEventSource
     * @param pEventOwner
     * @param pEventOwnerId
     * @return EventLog
     */
    public Long createEventLog(String pEventName, String pEventCategory, String pEventSubCat, String pEventSource,
        String pEventOwner, Long pEventOwnerId, String user, String modUser, String comments)
    throws DataAccessException, EventException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            Long id = edao.recordEventLog(pEventName, pEventCategory, pEventSubCat,
                    pEventSource, pEventOwner, pEventOwnerId, user, modUser, comments);
            return id;
        } catch (SQLException e) {
            edao.rollbackTransaction(transaction);
            throw new EventException("Failed to create event log", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public Integer notifyProcess(String pEventName, Long pEventInstId,
            String message, int delay)
    throws DataAccessException, EventException {
        EngineDataAccess edao = new EngineDataAccessDB();
        InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
        return engine.notifyProcess(pEventName, pEventInstId, message, delay);
    }

    /**
     * Method that returns distinct event log sources
     *
     * @return String[]
     */
    public String[] getDistinctEventLogEventSources()
    throws DataAccessException, EventException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getDistinctEventLogEventSources();
        } catch (SQLException e) {
            throw new EventException("Failed to notify events", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * create or update variable instance value.
     * This does not take care of checking for embedded processes.
     * For document variables, the value must be DocumentReference, not the document content
     */
    public VariableInstance setVariableInstance(Long procInstId, String name, Object value)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            VariableInstance varInst = edao.getVariableInstance(procInstId, name);
            if (varInst != null) {
                if (value instanceof String)
                    varInst.setStringValue((String)value);
                else
                    varInst.setData(value);
                edao.updateVariableInstance(varInst);
            } else {
                if (value != null) {
                    Process process = ProcessCache.getProcess(edao.getProcessInstance(procInstId).getProcessId());
                    Variable variable = process.getVariable(name);
                    if (variable == null) {
                        throw new DataAccessException("Variable " + name + " is not defined for process " + process.getId());
                    }

                    varInst = new VariableInstance();
                    varInst.setName(name);
                    varInst.setVariableId(variable.getId());
                    varInst.setType(variable.getType());
                    if (value instanceof String) varInst.setStringValue((String)value);
                    else varInst.setData(value);

                    edao.createVariableInstance(varInst, procInstId);
                } else varInst = null;
            }
            return varInst;
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to set variable value", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public VariableInstance getVariableInstance(Long varInstId)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getVariableInstance(varInstId);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Fail to get variable instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public VariableInstance getVariableInstance(Long procInstId, String name)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getVariableInstance(procInstId, name);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Fail to get variable instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public List<VariableInstance> getProcessInstanceVariables(Long procInstId)
        throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getProcessInstanceVariables(procInstId);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get process instance variables", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws ProcessException, com.centurylink.mdw.dataaccess.DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.setProcessInstanceStatus(pProcInstId, status);
            if (status.equals(WorkStatus.STATUS_COMPLETED) ||
                status.equals(WorkStatus.STATUS_CANCELLED) ||
                status.equals(WorkStatus.STATUS_FAILED)) {
                edao.removeEventWaitForProcessInstance(pProcInstId);
            }
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to update process instance status", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * This is for regression tester only.
     * @param masterRequestId
     */
    public void sendDelayEventsToWaitActivities(String masterRequestId)
            throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            List<ProcessInstance> procInsts = edao.getProcessInstancesByMasterRequestId(masterRequestId, null);
            for (ProcessInstance pi : procInsts) {
                List<ActivityInstance> actInsts = edao.getActivityInstancesForProcessInstance(pi.getId());
                for (ActivityInstance ai : actInsts) {
                    if (ai.getStatusCode()==WorkStatus.STATUS_WAITING.intValue()) {
                        InternalEvent event = InternalEvent.createActivityDelayMessage(ai,
                                masterRequestId);
                        this.sendInternalEvent(event, edao);
                    }
                }
            }
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to send delay event wait activities runtime", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }


    private void sendInternalEvent(InternalEvent pMsg, EngineDataAccess edao) throws ProcessException {
        InternalMessenger msgbroker = MessengerFactory.newInternalMessenger();
        msgbroker.sendMessage(pMsg, edao);
    }

    public void retryActivity(Long activityId, Long activityInstId)
    throws DataAccessException, ProcessException {
        CodeTimer timer = new CodeTimer("WorkManager.retryActivity", true);
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ActivityInstance ai = edao.getActivityInstance(activityInstId);
            Long procInstId = ai.getProcessInstanceId();
            ProcessInstance pi = edao.getProcessInstance(procInstId);
            if (!this.isProcessInstanceResumable(pi)) {
                logger.info("ProcessInstance in NOT resumable. ProcessInstanceId:" + pi.getId());
                timer.stopAndLogTiming("NotResumable");
                throw new ProcessException("The process instance is not resumable");
            }
            InternalEvent outgoingMsg = InternalEvent.createActivityStartMessage(
                    activityId, procInstId, null, pi.getMasterRequestId(), ActivityResultCodeConstant.RESULT_RETRY);
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
            this.sendInternalEvent(outgoingMsg, edao);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } catch (MdwException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        timer.stopAndLogTiming("");
    }

    public void skipActivity(Long activityId, Long activityInstId, String completionCode)
    throws DataAccessException, ProcessException {
        CodeTimer timer = new CodeTimer("WorkManager.skipActivity", true);
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ActivityInstance ai = edao.getActivityInstance(activityInstId);
            Long procInstId = ai.getProcessInstanceId();
            ProcessInstance pi = edao.getProcessInstance(procInstId);
            if (!this.isProcessInstanceResumable(pi)) {
                logger.info("ProcessInstance in NOT resumable. ProcessInstanceId:" + pi.getId());
                timer.stopAndLogTiming("NotResumable");
                throw new ProcessException("The process instance is not resumable");
            }

            Integer eventType;
            if (completionCode!=null) {
                int k = completionCode.indexOf(':');
                if (k<0) {
                    eventType = EventType.getEventTypeFromName(completionCode);
                    if (eventType!=null) completionCode = null;
                    else {
                        if (completionCode.length()==0) completionCode = null;
                        eventType = EventType.FINISH;
                    }
                } else {
                    String eventName = completionCode.substring(0,k);
                    eventType = EventType.getEventTypeFromName(eventName);
                    if (eventType!=null) {
                        completionCode = completionCode.substring(k+1);
                        if (completionCode.length()==0) completionCode = null;
                    } else eventType = EventType.FINISH;
                }
            } else {
                eventType = EventType.FINISH;
            }

            InternalEvent outgoingMsg = InternalEvent.
                createActivityNotifyMessage(ai, eventType,
                        pi.getMasterRequestId(), completionCode);
            this.sendInternalEvent(outgoingMsg, edao);
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } catch (MdwException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        timer.stopAndLogTiming("");
    }

    /**
     * Checks if the process inst is resumable
     *
     * @param pInstance
     * @return boolean status
     */
    private boolean isProcessInstanceResumable(ProcessInstance pInstance) {

        int statusCd = pInstance.getStatusCode().intValue();
        if (statusCd == WorkStatus.STATUS_COMPLETED.intValue()) {
            return false;
        } else if (statusCd == WorkStatus.STATUS_CANCELLED.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * Returns the ProcessInstance identified by the passed in Id
     *
     * @param pProcInstId
     * @return ProcessInstance
     */
    public ProcessInstance getProcessInstance(Long procInstId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getProcessInstance(procInstId);

        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to get process instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Returns the process instances by process name and master request ID.
     *
     * @param processName
     * @param masterRequestId
     * @return the list of process instances. If the process definition is not found, null
     *         is returned; if process definition is found but no process instances are found,
     *         an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    @Override
    public List<ProcessInstance> getProcessInstances(String masterRequestId, String processName)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            Process procdef = ProcessCache.getProcess(processName, 0);
            if (procdef==null) return null;
            transaction = edao.startTransaction();
            return edao.getProcessInstancesByMasterRequestId(masterRequestId, procdef.getId());
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Returns the activity instances by process name, activity logical ID, and  master request ID.
     *
     * @param processName
     * @param activityLogicalId
     * @param masterRequestId
     * @return the list of activity instances. If the process definition or the activity
     *         with the given logical ID is not found, null
     *         is returned; if process definition is found but no process instances are found,
     *         or no such activity instances are found, an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    public List<ActivityInstance> getActivityInstances(String masterRequestId, String processName, String activityLogicalId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            Process procdef = ProcessCache.getProcess(processName, 0);
            if (procdef==null) return null;
            Activity actdef = procdef.getActivityByLogicalId(activityLogicalId);
            if (actdef==null) return null;
            transaction = edao.startTransaction();
            List<ActivityInstance> actInstList = new ArrayList<ActivityInstance>();
            List<ProcessInstance> procInstList =
                edao.getProcessInstancesByMasterRequestId(masterRequestId, procdef.getId());
            if (procInstList.size()==0) return actInstList;
            for (ProcessInstance pi : procInstList) {
                List<ActivityInstance> actInsts = edao.getActivityInstances(actdef.getId(), pi.getId(), false, false);
                for (ActivityInstance ai : actInsts) {
                    actInstList.add(ai);
                }
            }
            return actInstList;
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Returns the ActivityInstance identified by the passed in Id
     *
     * @param pActivityInstId
     * @return ActivityInstance
     */
    public ActivityInstance getActivityInstance(Long pActivityInstId)
    throws ProcessException, DataAccessException {
        ActivityInstance ai;
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ai = edao.getActivityInstance(pActivityInstId);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to get activity instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        return ai;
    }

    /**
     * Returns the WorkTransitionVO based on the passed in params
     *
     * @param pId
     * @return WorkTransitionINstance
     */
    public TransitionInstance getWorkTransitionInstance(Long pId)
    throws DataAccessException, ProcessException {
        TransitionInstance wti;
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            wti = edao.getWorkTransitionInstance(pId);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to get work transition instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        return wti;
    }

    public void updateDocumentContent(Long docid, Object doc, String type, Package pkg)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            Document docvo = edao.getDocument(docid, false);
            if (doc instanceof String) docvo.setContent((String)doc);
            else docvo.setObject(doc, type);
            edao.updateDocumentContent(docvo.getDocumentId(), docvo.getContent(pkg));
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to update document content", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void updateDocumentInfo(Long docid, String documentType,
            String ownerType, Long ownerId) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            Document docvo = edao.getDocument(docid, false);
            if (documentType != null)
                docvo.setDocumentType(documentType);
            if (ownerType != null) {
                if (!ownerType.equalsIgnoreCase(docvo.getOwnerType()))
                    edao.getDocumentDbAccess().updateDocumentDbOwnerType(docvo, ownerType);
                docvo.setOwnerType(ownerType);
            }
            if (ownerId != null)
                docvo.setOwnerId(ownerId);
            edao.updateDocumentInfo(docvo);
        }
        catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to update document content", e);
        }
        finally {
            edao.stopTransaction(transaction);
        }
    }

    public Long createDocument(String type, String ownerType, Long ownerId, Object doc, Package pkg)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            Document docvo = new Document();
            if (doc instanceof Response) {
                String statusMsg = ((Response)doc).getStatusMessage() != null ? ((Response)doc).getStatusMessage() : "";
                docvo.setStatusCode(((Response)doc).getStatusCode());
                docvo.setStatusMessage(statusMsg.length() > 1000 ? statusMsg.substring(0, 1000) : statusMsg);
                docvo.setContent(((Response)doc).getContent());
            }
            else if (doc instanceof String)
                docvo.setContent((String)doc);
            else
                docvo.setObject(doc, type);
            docvo.setDocumentType(type);
            docvo.setOwnerType(ownerType);
            docvo.setOwnerId(ownerId);
            Long docid = edao.createDocument(docvo, pkg);
            return docid;
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to create document", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public Document getDocumentVO(Long docid)
    throws DataAccessException {
        try {
            DatabaseAccess db = new DatabaseAccess(null);
            RuntimeDataAccess da = DataAccess.getRuntimeDataAccess(db);
            return da.getDocument(docid);
        } catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
    }

    public List<EventLog> getEventLogs(String pEventName, String pEventSource,
            String pEventOwner, Long pEventOwnerId) throws DataAccessException {
        try {
            DatabaseAccess db = new DatabaseAccess(null);
            RuntimeDataAccess da = DataAccess.getRuntimeDataAccess(db);
            return da.getEventLogs(pEventName, pEventSource, pEventOwner, pEventOwnerId);
        } catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
    }

    public EventInstance getEventInstance(String eventName) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getEventInstance(eventName);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get event instance", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public List<ScheduledEvent> getScheduledEventList(Date cutoffTime) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getScheduledEventList(cutoffTime);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get scheduled event list", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public List<UnscheduledEvent> getUnscheduledEventList(Date olderThan, int batchSize) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getUnscheduledEventList(olderThan, batchSize);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get unscheduled event list", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void offerScheduledEvent(ScheduledEvent event)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.offerScheduledEvent(event);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException(23000, "The event is already scheduled", e);
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                throw new DataAccessException(23000, "The event is already scheduled", e);
                // for unknown reason (may be because of different Oracle driver - ojdbc14),
                // when running under Tomcat, contraint violation does not throw SQLIntegrityConstraintViolationException
                // 23000 is ANSI/SQL standard SQL State for constraint violation
                // Alternatively, we can use e.getErrorCode()==1 for Oracle (ORA-00001)
                // or e.getErrorCode()==1062 for MySQL
            } else {
                throw new DataAccessException(-1, "Failed to create scheduled event", e);
            }
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void processScheduledEvent(String eventName, Date now)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ScheduledEvent event = edao.lockScheduledEvent(eventName);
            Date currentScheduledTime = event==null?null:event.getScheduledTime();
            ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
            boolean processed = queue.processEventInEjb(eventName, event, now, edao);
            if (processed)  {
                if (event.isScheduledJob()) {
                    edao.recordScheduledJobHistory(event.getName(), currentScheduledTime,
                            ApplicationContext.getServer().toString());
                }
                if (event.getScheduledTime()==null) edao.deleteEventInstance(event.getName());
                else edao.updateEventInstance(event.getName(), null, null,
                        event.getScheduledTime(), null, null, 0, null);
            }     // else do nothing - may be processed by another server
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to process scheduled event", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public boolean processUnscheduledEvent(String eventName) {
        TransactionWrapper transaction = null;
        boolean processed = false;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ScheduledEvent event = edao.lockScheduledEvent(eventName);
            if (event!=null) {
                ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
                InternalEventDriver command = new InternalEventDriver(null, event.getMessage());
                if (thread_pool.execute(ThreadPoolProvider.WORKER_SCHEDULER, event.getName(), command)) {
                    String query = "delete from EVENT_INSTANCE where EVENT_NAME=?";
                    transaction.getDatabaseAccess().runUpdate(query, event.getName());
                    processed = true;
                }
            }
        } catch (Exception e) {
            logger.severeException("Failed to process unscheduled event " + eventName, e);
            // do not rollback - that may cause the event being processed again and again
            processed = false;
        } finally {
            try {
                edao.stopTransaction(transaction);
            } catch (DataAccessException e) {
                logger.severeException("Failed to process unscheduled event " + eventName, e);
                // do not rollback - that may cause the event being processed again and again
                processed = false;
            }
        }
        return processed;
        // return true when the message is successfully sent; when false, release reserved connection
    }

    public List<UnscheduledEvent> processInternalEvents(List<UnscheduledEvent> eventList) {
        List<UnscheduledEvent> returnList = new ArrayList<UnscheduledEvent>();
        ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
        for (UnscheduledEvent one : eventList) {
            if (EventInstance.ACTIVE_INTERNAL_EVENT.equals(one.getReference())) {
                InternalEventDriver command = new InternalEventDriver(one.getName(), one.getMessage());
                if (!thread_pool.execute(ThreadPoolProvider.WORKER_SCHEDULER, one.getName(), command)) {
                    String msg = ThreadPoolProvider.WORKER_SCHEDULER + " has no thread available for Unscheduled event: " + one.getName() + " message:\n" + one.getMessage();
                    // make this stand out
                    logger.warnException(msg, new Exception(msg));
                    logger.info(thread_pool.currentStatus());
                    returnList.add(one);
                }
            }
            else
                returnList.add(one);
        }
        return returnList;
    }



    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds)
    throws DataAccessException {
        updateEventInstance(eventName, documentId, status, consumeDate, auxdata, reference, preserveSeconds, null);
    }

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds, String comments)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.updateEventInstance(eventName, documentId, status, consumeDate, auxdata, reference, preserveSeconds, comments);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to retrieve event instance list", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.setAttribute(ownerType, ownerId, attrname, attrvalue);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to set attribute for " + ownerType + ": " + ownerId, e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.setAttributes(ownerType, ownerId, attributes);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to set attributes for " + ownerType + ": " + ownerId, e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    private static List<ServiceHandler> serviceHandlers = new ArrayList<ServiceHandler>();

    public void registerServiceHandler(ServiceHandler handler) throws EventException {
        if (!serviceHandlers.contains(handler))
            serviceHandlers.add(handler);
    }

    public void unregisterServiceHandler(ServiceHandler handler) throws EventException {
        serviceHandlers.remove(handler);
    }

    public ServiceHandler getServiceHandler(String protocol, String path) throws EventException {
        for (ServiceHandler serviceHandler : serviceHandlers) {
            if (protocol.equals(serviceHandler.getProtocol())
                && ((path == null && serviceHandler.getPath() == null)
                     || (path != null && path.equals(serviceHandler.getPath()))) ) {
                return serviceHandler;
            }
        }
        return null;
    }

    private static List<WorkflowHandler> workflowHandlers = new ArrayList<WorkflowHandler>();

    public void registerWorkflowHandler(WorkflowHandler handler) throws EventException {
        if (!workflowHandlers.contains(handler))
            workflowHandlers.add(handler);
    }

    public void unregisterWorkflowHandler(WorkflowHandler handler) throws EventException {
        workflowHandlers.remove(handler);
    }

    public WorkflowHandler getWorkflowHandler(String asset, Map<String,String> parameters) throws EventException {
        for (WorkflowHandler workflowHandler : workflowHandlers) {
            if (asset.equals(workflowHandler.getAsset())) {
                if (parameters == null) {
                    if (workflowHandler.getParameters() == null)
                        return workflowHandler;
                    else
                        continue;
                }
                else if (workflowHandler.getParameters() == null) {
                    continue;
                }
                boolean match = true;
                for (String paramName : parameters.keySet()) {
                    if (!(parameters.get(paramName).equals(workflowHandler.getParameters().get(paramName)))) {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return workflowHandler;
            }
        }
        return null;
    }

    @Override
    public Process findProcessByProcessInstanceId(Long processInstanceId) throws DataAccessException,
            ProcessException {
        ProcessInstance processInst = getProcessInstance(processInstanceId);
        if (processInst.isEmbedded()) {
            processInst = getProcessInstance(processInst.getOwnerId());
        }
        Process process = processInst != null ? ProcessCache.getProcess(processInst.getProcessId()) : null;
        return process;
    }
}
