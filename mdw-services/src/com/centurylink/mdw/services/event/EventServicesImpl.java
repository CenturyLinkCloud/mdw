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

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.event.EventDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * @param pEventName event name
     * @param pEventCategory event category
     * @param pEventSource event source
     * @param pEventOwner event owner
     * @param pEventOwnerId event owner id
     * @return EventLog
     */
    public Long createEventLog(String pEventName, String pEventCategory, String pEventSubCat, String pEventSource,
            String pEventOwner, Long pEventOwnerId, String user, String modUser, String comments)
            throws DataAccessException, EventException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.recordEventLog(pEventName, pEventCategory, pEventSubCat,
                    pEventSource, pEventOwner, pEventOwnerId, user, modUser, comments);
        } catch (SQLException e) {
            edao.rollbackTransaction(transaction);
            throw new EventException("Failed to create event log", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public Integer notifyProcess(String eventName, Long docId, String message, int delay)
            throws DataAccessException, EventException {
        EngineDataAccess edao = new EngineDataAccessDB();
        InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
        return engine.notifyProcess(eventName, docId, message, delay);
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
                    ProcessInstance procInst = edao.getProcessInstance(procInstId);
                    Process process = null;
                    if (procInst.getInstanceDefinitionId() > 0L)
                        process = ProcessCache.getInstanceDefinition(procInst.getProcessId(), procInst.getInstanceDefinitionId());
                    if (process == null)
                        process = ProcessCache.getProcess(procInst.getProcessId());
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
                }
            }
            return varInst;
        } catch (SQLException | IOException e) {
            throw new DataAccessException(-1, "Failed to set " + name + " on instance " + procInstId, e);
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

    /**
     * This is for regression tester only.
     * @param masterRequestId master request id.
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
                    if (ai.getStatusCode()==WorkStatus.STATUS_WAITING) {
                        InternalEvent event = InternalEvent.createActivityDelayMessage(ai,
                                masterRequestId);
                        sendInternalEvent(event, edao);
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

    public void cancelProcess(Long processInstanceId) throws DataAccessException, ServiceException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            try {
                ProcessInstance pi = edao.getProcessInstance(processInstanceId);
                if (!isProcessInstanceResumable(pi))
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Process not cancelable: " + pi.getId());
                InternalEvent internalEvent = InternalEvent.createProcessAbortMessage(pi);
                sendInternalEvent(internalEvent, edao);
            }
            catch (SQLException ex) {
                throw new ServiceException(ServiceException.NOT_FOUND, ex.getMessage());
            }
        } catch (ProcessException ex) {
            throw new ServiceException(0, "Failed to cancel process: " + processInstanceId, ex);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void retryActivity(Long activityId, Long activityInstId)
            throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ActivityInstance ai = edao.getActivityInstance(activityInstId);
            Long procInstId = ai.getProcessInstanceId();
            ProcessInstance pi = edao.getProcessInstance(procInstId);
            if (!isProcessInstanceResumable(pi)) {
                logger.info("ProcessInstance in NOT resumable. ProcessInstanceId:" + pi.getId());
                throw new ProcessException("The process instance is not resumable");
            }
            InternalEvent outgoingMsg = InternalEvent.createActivityStartMessage(
                    activityId, procInstId, null, pi.getMasterRequestId(), ActivityResultCodeConstant.RESULT_RETRY);
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
            sendInternalEvent(outgoingMsg, edao);
        } catch (SQLException | MdwException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
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
            if (!isProcessInstanceResumable(pi)) {
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
            edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_CANCELLED, null);
            sendInternalEvent(outgoingMsg, edao);
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
        } catch (SQLException | MdwException e) {
            throw new ProcessException(0, "Failed to remove event waits", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        timer.stopAndLogTiming("");
    }

    /**
     * Checks if the process inst is resumable
     *
     * @param pInstance process instance
     * @return boolean status
     */
    private boolean isProcessInstanceResumable(ProcessInstance pInstance) {
        int statusCd = pInstance.getStatusCode();
        if (statusCd == WorkStatus.STATUS_COMPLETED) {
            return false;
        } else return statusCd != WorkStatus.STATUS_CANCELLED;
    }

    /**
     * Returns the ProcessInstance identified by the passed in Id
     *
     * @param procInstId process instance id
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
     * @param processName process name.
     * @param masterRequestId master request id.
     * @return the list of process instances. If the process definition is not found, null
     *         is returned; if process definition is found but no process instances are found,
     *         an empty list is returned.
     * @throws ProcessException process exception.
     * @throws DataAccessException data access exception.
     */
    @Override
    public List<ProcessInstance> getProcessInstances(String masterRequestId, String processName)
            throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            Process procdef = ProcessCache.getProcess(processName);
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
     * @param processName process name.
     * @param activityLogicalId activity logical id.
     * @param masterRequestId master request id.
     * @return the list of activity instances. If the process definition or the activity
     *         with the given logical ID is not found, null
     *         is returned; if process definition is found but no process instances are found,
     *         or no such activity instances are found, an empty list is returned.
     * @throws ProcessException process exception.
     * @throws DataAccessException data access exception.
     */
    public List<ActivityInstance> getActivityInstances(String masterRequestId, String processName, String activityLogicalId)
            throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            Process procdef = ProcessCache.getProcess(processName);
            if (procdef == null)
                return null;
            Activity actdef = procdef.getActivity(activityLogicalId, false);
            if (actdef == null)
                return null;
            transaction = edao.startTransaction();
            List<ActivityInstance> actInstList = new ArrayList<>();
            List<ProcessInstance> procInstList =
                    edao.getProcessInstancesByMasterRequestId(masterRequestId, procdef.getId());
            if (procInstList.size() == 0)
                return actInstList;
            for (ProcessInstance pi : procInstList) {
                List<ActivityInstance> actInsts = edao.getActivityInstances(actdef.getId(), pi.getId(), false, false);
                actInstList.addAll(actInsts);
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
     * @param pActivityInstId activity instance id
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
     * @param pId WorkTransitionInstance id
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
        return createDocument(type, ownerType, ownerId, doc, pkg, null);
    }

    public Long createDocument(String type, String ownerType, Long ownerId, Object doc, Package pkg, String path)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            Document docvo = new Document();
            if (doc instanceof Response) {
                Response response = (Response)doc;
                String statusMsg = response.getStatusMessage() != null ? response.getStatusMessage() : "";
                docvo.setStatusCode(response.getStatusCode());
                docvo.setStatusMessage(statusMsg.length() > 1000 ? statusMsg.substring(0, 1000) : statusMsg);
                if (path == null)
                    path = response.getPath();
                docvo.setContent(((Response)doc).getContent());
            }
            else if (doc instanceof String)
                docvo.setContent((String)doc);
            else
                docvo.setObject(doc, type);
            docvo.setDocumentType(type);
            docvo.setOwnerType(ownerType);
            docvo.setOwnerId(ownerId);
            docvo.setPath(path);
            return edao.createDocument(docvo, pkg);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to create document", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public List<EventLog> getEventLogs(String eventName, String eventSource,
            String eventOwner, Long ownerId) throws ServiceException {
        EventDataAccess dataAccess = new EventDataAccess();
        try {
            return dataAccess.getEventLogs(eventName, eventSource, eventOwner, ownerId);
        } catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
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
            throw new DataAccessException(-1, "Failed to update event instance: " + eventName, e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void deleteEventInstance(String eventName) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.deleteEventInstance(eventName);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete event instance: " + eventName, e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    public void deleteEventWaitInstance(String eventName) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.removeEventWait(eventName);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete event wait instance: " + eventName, e);
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

    private static List<ServiceHandler> serviceHandlers = new ArrayList<>();

    public void registerServiceHandler(ServiceHandler handler) {
        if (!serviceHandlers.contains(handler))
            serviceHandlers.add(handler);
    }

    public void unregisterServiceHandler(ServiceHandler handler) {
        serviceHandlers.remove(handler);
    }

    public ServiceHandler getServiceHandler(String protocol, String path) {
        for (ServiceHandler serviceHandler : serviceHandlers) {
            if (protocol.equals(serviceHandler.getProtocol())
                    && ((path == null && serviceHandler.getPath() == null)
                    || (path != null && path.equals(serviceHandler.getPath()))) ) {
                return serviceHandler;
            }
        }
        return null;
    }

    private static List<WorkflowHandler> workflowHandlers = new ArrayList<>();

    public void registerWorkflowHandler(WorkflowHandler handler) {
        if (!workflowHandlers.contains(handler))
            workflowHandlers.add(handler);
    }

    public void unregisterWorkflowHandler(WorkflowHandler handler) {
        workflowHandlers.remove(handler);
    }

    public WorkflowHandler getWorkflowHandler(String asset, Map<String,String> parameters) {
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
    public Process findProcessByProcessInstanceId(Long processInstanceId)
            throws DataAccessException, ProcessException, IOException {
        ProcessInstance processInst = getProcessInstance(processInstanceId);
        if (processInst.isEmbedded()) {
            processInst = getProcessInstance(processInst.getOwnerId());
        }
        Process process = null;
        if (processInst != null) {
            if (processInst.getInstanceDefinitionId() > 0L)
                process = ProcessCache.getInstanceDefinition(processInst.getProcessId(), processInst.getInstanceDefinitionId());
            if (process == null)
                process = ProcessCache.getProcess(processInst.getProcessId());
        }
        return process;
    }

}
