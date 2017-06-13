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
package com.centurylink.mdw.services.process;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.FinishActivity;
import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.activity.types.SynchronizationActivity;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.model.workflow.TransitionStatus;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.OfflineMonitorTrigger;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.CollectionUtil;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.util.timer.TrackingTimer;

class ProcessExecutorImpl {

    protected static StandardLogger logger;

    protected EngineDataAccess edao;
    private InternalMessenger internalMessenger;
    private final boolean inService;

    ProcessExecutorImpl(EngineDataAccess edao,
            InternalMessenger internalMessenger, boolean forServiceProcess) {
        logger = LoggerUtil.getStandardLogger();
        this.edao = edao;
        inService = forServiceProcess;
        this.internalMessenger = internalMessenger;
    }

    private String logtag(Long procId, Long procInstId, Long actId, Long actInstId) {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(procId);
        sb.append(".");
        sb.append(procInstId);
        sb.append(" a");
        sb.append(actId);
        sb.append(".");
        sb.append(actInstId);
        return sb.toString();
    }

    private String logtag(Long procId, Long procInstId, String masterRequestId) {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(procId);
        sb.append(".");
        sb.append(procInstId);
        sb.append(" m.");
        sb.append(masterRequestId);
        return sb.toString();
    }

    private String logtag(Long procId, Long procInstId, TransitionInstance transInst) {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(procId);
        sb.append(".");
        sb.append(procInstId);
        sb.append(" t");
        sb.append(transInst.getTransitionID());
        sb.append(".");
        sb.append(transInst.getTransitionInstanceID());
        return sb.toString();
    }

    final EngineDataAccess getDataAccess() {
        return edao;
    }

    final DatabaseAccess getDatabaseAccess() {
        return edao.getDatabaseAccess();
    }

    ActivityInstance createActivityInstance(Long pActivityId, Long procInstId)
    throws ProcessException, SQLException, DataAccessException {
        ActivityInstance ai = new ActivityInstance();
        ai.setActivityId(pActivityId);
        ai.setProcessInstanceId(procInstId);
        ai.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
        edao.createActivityInstance(ai);
        return ai;
    }

    TransitionInstance createTransitionInstance(
            Transition transition, Long processInstId)
        throws DataAccessException {
        try {
            TransitionInstance transInst = new TransitionInstance();
            transInst.setTransitionID(transition.getWorkTransitionId());
            transInst.setProcessInstanceID(processInstId);
            transInst.setStatusCode(TransitionStatus.STATUS_INITIATED);
            transInst.setDestinationID(transition.getToWorkId());
            edao.createTransitionInstance(transInst);
            return transInst;
        } catch (SQLException e) {
            throw new DataAccessException(0, e.getMessage(), e);
        }
    }

    VariableInstance createVariableInstance(ProcessInstance pi,
            String varname, Object value)
    throws SQLException,DataAccessException {
        Process processVO = this.getMainProcessDefinition(pi);
        Variable variableVO = processVO.getVariable(varname);
        if (variableVO==null) {
            throw new DataAccessException("Variable "
                    + varname + " is not defined for process " + processVO.getProcessId());
        }
        VariableInstance var = new VariableInstance();
        var.setName(variableVO.getVariableName());
        var.setVariableId(variableVO.getVariableId());
        var.setType(variableVO.getVariableType());
        if (value instanceof String)
            var.setStringValue((String)value);
        else
            var.setData(value);
        if (pi.isEmbedded() || !pi.getProcessId().equals(processVO.getProcessId()))
            edao.createVariableInstance(var, pi.getOwnerId());
        else
            edao.createVariableInstance(var, pi.getId());
        return var;
    }

    DocumentReference createDocument(String type, String ownerType, Long ownerId,
            Object doc) throws DataAccessException {
        return createDocument(type, ownerType, ownerId, null, null, doc);
    }

    DocumentReference createDocument(String type, String ownerType, Long ownerId,
            Integer statusCode, String statusMessage, Object doc) throws DataAccessException {
        DocumentReference docref = null;
        try {
            Document docvo = new Document();
            if (doc instanceof String)
                docvo.setContent((String)doc);
            else
                docvo.setObject(doc, type);
            docvo.setDocumentType(type);
            docvo.setOwnerType(ownerType);
            docvo.setOwnerId(ownerId);
            docvo.setStatusCode(statusCode);
            docvo.setStatusMessage(statusMessage);
            edao.createDocument(docvo);
            docref = new DocumentReference(docvo.getDocumentId());
        } catch (Exception e) {
            throw new DataAccessException(0, e.getMessage(), e);
        }
        return docref;
    }

    Document getDocument(DocumentReference docref, boolean forUpdate) throws DataAccessException {
        try {
            return edao.getDocument(docref.getDocumentId(), forUpdate);
        } catch (SQLException e) {
            throw new DataAccessException(-1, e.getMessage(), e);
        }
    }

    /**
     * Does not work for remote documents
     */
    Document loadDocument(DocumentReference docref, boolean forUpdate)
        throws DataAccessException {
        try {
            return edao.loadDocument(docref.getDocumentId(), forUpdate);
        } catch (SQLException e) {
            throw new DataAccessException(-1, e.getMessage(), e);
        }
    }

    void updateDocumentContent(DocumentReference docref, Object doc, String type, Package pkg) throws DataAccessException {
        try {
            Document docvo = edao.getDocument(docref.getDocumentId(), false);
            if (doc instanceof String)
                docvo.setContent((String)doc);
            else
                docvo.setObject(doc, type);
            edao.updateDocumentContent(docvo.getDocumentId(), docvo.getContent(pkg));
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to update document content", e);
        }
    }

    private List<VariableInstance> convertParameters(Map<String,String> eventParams,
            Process processVO, Long procInstId) throws ProcessException, DataAccessException {
        List<VariableInstance> vars = new ArrayList<VariableInstance>();
        if (eventParams == null || eventParams.isEmpty()) {
            return vars;
        }
        for (String varname : eventParams.keySet()) {
            Variable variableVO = processVO.getVariable(varname);
            if (variableVO==null) {
                String msg = "there is no variable named " + varname
                    + " in process with ID " + processVO.getProcessId()
                    + " for parameter binding";
                throw new ProcessException(msg);
            }
            VariableInstance var = new VariableInstance();
            var.setName(variableVO.getVariableName());
            var.setVariableId(variableVO.getVariableId());
            var.setType(variableVO.getVariableType());
            String value = eventParams.get(varname);
            if (value!=null && value.length()>0) {
                if (VariableTranslator.isDocumentReferenceVariable(getPackage(processVO), var.getType())) {
                    if (value.startsWith("DOCUMENT:")) var.setStringValue(value);
                    else {
                        DocumentReference docref = this.createDocument(var.getType(),
                                OwnerType.PROCESS_INSTANCE, procInstId, value);
                        var.setData(docref);
                    }
                } else var.setStringValue(value);
                vars.add(var);    // only create variable instances when value is not null
            }
            // vars.add(var);    // if we put here, we create variables regardless if value is null
        }

        return vars;
    }

    /**
     * Create a process instance. The status is PENDING_PROCESS
     * @param processVO
     * @param eventMessageDoc
     * @return
     * @throws ProcessException
     * @throws DataAccessException
     */
    ProcessInstance createProcessInstance(Long processId, String ownerType,
            Long ownerId, String secondaryOwnerType, Long secondaryOwnerId,
            String masterRequestId, Map<String,String> parameters, String label)
    throws ProcessException, DataAccessException
    {
        ProcessInstance pi;
        try {
            Process processVO;
            if (ownerType.equals(OwnerType.MAIN_PROCESS_INSTANCE)) {
                ProcessInstance parentPi = getDataAccess().getProcessInstance(ownerId);
                Process parentProcdef = ProcessCache.getProcess(parentPi.getProcessId());
                processVO = parentProcdef.getSubProcessVO(processId);
                pi = new ProcessInstance(parentPi.getProcessId(), processVO.getProcessName());
                pi.setComment(processId.toString());
            } else {
                processVO = ProcessCache.getProcess(processId);
                pi = new ProcessInstance(processId, processVO.getProcessName());
            }
            pi.setOwner(ownerType);
            pi.setOwnerId(ownerId);
            pi.setSecondaryOwner(secondaryOwnerType);
            pi.setSecondaryOwnerId(secondaryOwnerId);
            pi.setMasterRequestId(masterRequestId);
            pi.setStatusCode(WorkStatus.STATUS_PENDING_PROCESS);
            if (label != null)
                pi.setComment(label);
            edao.createProcessInstance(pi);
//            if (parameters!=null)    // do not check this, as below will initialize variables array
            createVariableInstancesFromEventMessage(pi, parameters);
        } catch (SQLException e) {
            throw new DataAccessException(-1, e.getMessage(), e);
        }
        return pi;
    }

    private void createVariableInstancesFromEventMessage(ProcessInstance pi,
            Map<String,String> parameters) throws ProcessException, DataAccessException, SQLException {
        Process processVO = getProcessDefinition(pi);
        pi.setVariables(convertParameters(parameters, processVO, pi.getId()));
        for (VariableInstance var : pi.getVariables()) {
            edao.createVariableInstance(var, pi.getId());
        }
    }

    void updateDocumentInfo(DocumentReference docref, String documentType, String ownerType,
            Long ownerId, Integer statusCode, String statusMessage) throws DataAccessException {
        try {
            Document docvo = edao.getDocument(docref.getDocumentId(), false);
            if (documentType != null)
                docvo.setDocumentType(documentType);
            if (ownerType != null)
                docvo.setOwnerType(ownerType);
            if (ownerId != null)
                docvo.setOwnerId(ownerId);
            if (statusCode != null)
                docvo.setStatusCode(statusCode);
            if (statusMessage != null)
                docvo.setStatusMessage(statusMessage);
            edao.updateDocumentInfo(docvo);
        } catch (SQLException e) {
            throw new DataAccessException(-1, e.getMessage(), e);
        }
    }

    void cancelEventWaitInstances(Long activityInstanceId)
    throws DataAccessException {
        try {
            getDataAccess().removeEventWaitForActivityInstance(activityInstanceId, "Cancel due to timeout");
        } catch (Exception e) {
            throw new DataAccessException(0, "Failed to cancel event waits", e);
        }
    }

    String getServiceProcessResponse(Long procInstId, String varname)
        throws DataAccessException {
        try {
            VariableInstance varinst;
            if (varname==null) {
                varinst = getDataAccess().getVariableInstance(procInstId, VariableConstants.RESPONSE);
                if (varinst==null) varinst = getDataAccess().getVariableInstance(procInstId, VariableConstants.MASTER_DOCUMENT);
                if (varinst==null) varinst = getDataAccess().getVariableInstance(procInstId, VariableConstants.REQUEST);
            } else {
                varinst = getDataAccess().getVariableInstance(procInstId, varname);
            }
            if (varinst==null) return null;
            if (varinst.isDocument()) {
                Document docvo = getDocument((DocumentReference)varinst.getData(), false);
                return docvo.getContent(null);
            } else return varinst.getStringValue();
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to get value for variable " + varname, e);
        }
    }

    void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws DataAccessException,ProcessException {
        try {
            getDataAccess().setProcessInstanceStatus(pProcInstId, status);
            if (status.equals(WorkStatus.STATUS_COMPLETED) ||
                status.equals(WorkStatus.STATUS_CANCELLED) ||
                status.equals(WorkStatus.STATUS_FAILED)) {
                getDataAccess().removeEventWaitForProcessInstance(pProcInstId);
            }
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to update process instance status", e);
        }
    }

    protected Process getProcessDefinition(ProcessInstance procinst) {
        Process procdef = ProcessCache.getProcess(procinst.getProcessId());
        if (procinst.isEmbedded())
            procdef = procdef.getSubProcessVO(new Long(procinst.getComment()));
        return procdef;
    }

    protected Process getMainProcessDefinition(ProcessInstance procinst)
        throws DataAccessException, SQLException {
        Process procdef = ProcessCache.getProcess(procinst.getProcessId());
        if (procinst.isEmbedded()) {
            procinst = edao.getProcessInstance(procinst.getOwnerId());
            procdef = ProcessCache.getProcess(procinst.getProcessId());
        }
        return procdef;
    }

    boolean deleteInternalEvent(String eventName)
    throws DataAccessException {
        try {
            int count = getDataAccess().deleteEventInstance(eventName);
            return count > 0;
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to delete internal event" + eventName, e);
        }
    }

    InternalMessenger getInternalMessenger() {
        return internalMessenger;
    }

    ///////////// create process instance

    /**
     * Handles the work Transitions for the passed in collection of Items
     *
     * @param processInst
     * @param transitions
     * @param eventMessageDoc
     */
    void createTransitionInstances(ProcessInstance processInstanceVO,
            List<Transition> transitions, Long fromActInstId)
           throws ProcessException,DataAccessException {
        TransitionInstance transInst;
        for (Transition transition : transitions) {
            try {
                if (tooManyMaxTransitionInstances(transition, processInstanceVO.getId())) {
                    // Look for a error transition at this time
                    // In case we find it, raise the error event
                    // Otherwise do not do anything
                    handleWorkTransitionError(processInstanceVO, transition.getWorkTransitionId(), fromActInstId);
                } else {
                    transInst = createTransitionInstance(transition, processInstanceVO.getId());
                    String tag = logtag(processInstanceVO.getProcessId(),
                            processInstanceVO.getId(), transInst);
                    logger.info(tag, "Transition initiated from " + transition.getFromWorkId() + " to " + transition.getToWorkId());

                    InternalEvent jmsmsg;
                    int delay = 0;
                    jmsmsg = InternalEvent.createActivityStartMessage(
                            transition.getToWorkId(), processInstanceVO.getId(),
                            transInst.getTransitionInstanceID(), processInstanceVO.getMasterRequestId(),
                            transition.getLabel());
                    delay = transition.getTransitionDelay();
                    String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + processInstanceVO.getId()
                            + "start" + transition.getToWorkId() + "by" + transInst.getTransitionInstanceID();
                    if (delay>0) this.sendDelayedInternalEvent(jmsmsg, delay, msgid, false);
                    else sendInternalEvent(jmsmsg);
                }
            } catch (SQLException ex) {
                throw new ProcessException(-1, ex.getMessage(), ex);
            } catch (MdwException ex) {
                throw new ProcessException(-1, ex.getMessage(), ex);
            }
        }
    }

    private boolean tooManyMaxTransitionInstances(Transition trans, Long pProcessInstId)
        throws SQLException {
        if (inService) return false;
        String retryAttribVal = trans.getAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT);
        int retryCount = retryAttribVal==null?-1:Integer.parseInt(retryAttribVal);
        if (retryCount<0) return false;
        int count = edao.countTransitionInstances(pProcessInstId, trans.getWorkTransitionId());
        if (count>0 && count >= retryCount) {
            String msg = "Transition " + trans.getWorkTransitionId()
            + " not made - exceeded allowed retry count of " + retryCount;
            // log as exception since this message is often overlooked
            logger.severeException(msg, new ProcessException(msg));
            return true;
        } else return false;
    }

    private void handleWorkTransitionError(ProcessInstance processInstVO, Long workTransitionId,
            Long fromActInstId) throws ProcessException, DataAccessException, SQLException
    {
        edao.setProcessInstanceStatus(processInstVO.getId(), WorkStatus.STATUS_WAITING);
        Process processVO = getMainProcessDefinition(processInstVO);
        Process embeddedProcdef = processVO.findEmbeddedProcess(EventType.ERROR, null);
        while (embeddedProcdef==null && processInstVO.getOwner().equals(OwnerType.PROCESS_INSTANCE)) {
            processInstVO = edao.getProcessInstance(processInstVO.getOwnerId());
            processVO = getMainProcessDefinition(processInstVO);
            embeddedProcdef = processVO.findEmbeddedProcess(EventType.ERROR, null);
        }
        if (embeddedProcdef == null) {
            logger.warn("Error subprocess does not exist. Transition failed. TransitionId-->"
                    + workTransitionId + " ProcessInstanceId-->" + processInstVO.getId());
            return;
        }
        String tag = logtag(processInstVO.getProcessId(),processInstVO.getId(),processInstVO.getMasterRequestId());
        logger.info(tag, "Transition to error subprocess " + embeddedProcdef.getProcessQualifiedName());
        String secondaryOwnerType;
        Long secondaryOwnerId;
        if (fromActInstId==null || fromActInstId.longValue()==0L) {
            secondaryOwnerType = OwnerType.WORK_TRANSITION;
            secondaryOwnerId = workTransitionId;
        } else {
            secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
            secondaryOwnerId = fromActInstId;
        }
        String ownerType = OwnerType.MAIN_PROCESS_INSTANCE;
        ProcessInstance procInst = createProcessInstance(embeddedProcdef.getProcessId(),
                ownerType, processInstVO.getId(), secondaryOwnerType, secondaryOwnerId,
                processInstVO.getMasterRequestId(), null, null);
        startProcessInstance(procInst, 0);
    }

    /**
     * Starting a process instance, which has been created already.
     * The method sets the status to "In Progress",
     * find the start activity, and sends an internal message to start the activity
     *
     * @param processInstanceVO
     */
    void startProcessInstance(ProcessInstance processInstanceVO, int delay)
      throws ProcessException {

        try {
            Process processVO = getProcessDefinition(processInstanceVO);
            edao.setProcessInstanceStatus(processInstanceVO.getId(), WorkStatus.STATUS_PENDING_PROCESS);
            // setProcessInstanceStatus will really set to STATUS_IN_PROGRESS - hint to set START_DT as well
            if (logger.isInfoEnabled()) {
                logger.info(logtag(processInstanceVO.getProcessId(), processInstanceVO.getId(),
                        processInstanceVO.getMasterRequestId()),
                        WorkStatus.LOGMSG_PROC_START + " - " + processVO.getProcessQualifiedName()
                        + (processInstanceVO.isEmbedded() ?
                                (" (embedded process " + processVO.getProcessId() + ")") :
                                ("/" + processVO.getVersionString())));
            }
            notifyMonitors(processInstanceVO, WorkStatus.LOGMSG_PROC_START);
            // get start activity ID
            Long startActivityId;
            if (processInstanceVO.isEmbedded()) {
                edao.setProcessInstanceStatus(processInstanceVO.getId(), WorkStatus.STATUS_PENDING_PROCESS);
                startActivityId = processVO.getStartActivity().getActivityId();
            } else {
                Activity startActivity = processVO.getStartActivity();
                if (startActivity == null) {
                    throw new ProcessException("WorkTransition has not been defined for START event! ProcessID = " + processVO.getProcessId());
                }
                startActivityId = startActivity.getActivityId();
            }
            InternalEvent event = InternalEvent.createActivityStartMessage(
                    startActivityId, processInstanceVO.getId(),
                    null, processInstanceVO.getMasterRequestId(),
                    EventType.EVENTNAME_START + ":");
            if (delay>0) {
                String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + processInstanceVO.getId()
                    + "start" + startActivityId;
                this.sendDelayedInternalEvent(event, delay, msgid, false);
            } else sendInternalEvent(event);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ProcessException(ex.getMessage());
        }
    }

    ///// execute activity

    /**
     * determine if activity needs to wait (such as synchronous
     * process invocation, wait activity, synchronization)
     *
     * @param activity
     * @param activityInstanceId
     * @param eventMessageDoc
     * @return whether to wait
     */
    private boolean activityNeedsWait(GeneralActivity activity)
            throws ActivityException {
        if (activity instanceof SuspendibleActivity)
            return ((SuspendibleActivity) activity).needSuspend();
        return false;
    }

    /**
     * Reports the error status of the activity instance to the activity manager
     *
     * @param eventMessageDoc
     * @param activityInstId
     * @param processInstId
     * @param activity
     * @param cause
     */
    void failActivityInstance(InternalEvent event,
            ProcessInstance processInst, Long activityId, Long activityInstId,
            BaseActivity activity, Throwable cause) {

        String tag = logtag(processInst.getProcessId(), processInst.getId(), activityId, activityInstId);
        logger.severeException("Failed to execute activity - " + cause.getClass().getName(), cause);
        String compCode = null;
        String statusMsg = buildStatusMessage(cause);
        try {
            ActivityInstance actInstVO = null;
            if (activity != null && activityInstId != null) {
                activity.setReturnMessage(statusMsg);
                actInstVO = edao.getActivityInstance(activityInstId);
                failActivityInstance(actInstVO, statusMsg, processInst, tag, cause.getClass().getName());
                compCode = activity.getReturnCode();
            }
            if (!AdapterActivity.COMPCODE_AUTO_RETRY.equals(compCode)) {
                DocumentReference docRef = createActivityExceptionDocument(processInst, actInstVO, activity, cause);
                InternalEvent outgoingMsg =
                    InternalEvent.createActivityErrorMessage(activityId, activityInstId, processInst.getId(), compCode,
                        event.getMasterRequestId(), statusMsg.length() > 2000 ? statusMsg.substring(0, 1999) : statusMsg, docRef.getDocumentId());
                sendInternalEvent(outgoingMsg);
            }
        }
        catch (Exception ex) {
            logger.severeException("Exception thrown during failActivityInstance", ex);
        }
    }

    private String buildStatusMessage(Throwable t) {
        if (t == null)
            return "";
        StringBuffer message = new StringBuffer(t.toString());
        String v = PropertyManager.getProperty("MDWFramework.WorkflowEngine/ActivityStatusMessage.ShowStackTrace");
        boolean includeStackTrace = !"false".equalsIgnoreCase(v);
        if (includeStackTrace) {
            // get the root cause
            Throwable cause = t;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (t != cause)
                message.append("\nCaused by: " + cause);
            for (StackTraceElement element : cause.getStackTrace()) {
                message.append("\n").append(element.toString());
            }
        }

        if (message.length() > 4000) {
            return message.toString().substring(0, 3998);
        }

        return message.toString();
    }

    void cancelActivityInstance(ActivityInstance actInst,
            ProcessInstance procinst, String statusMsg) {
        String logtag = this.logtag(procinst.getProcessId(), procinst.getId(), actInst.getActivityId(), actInst.getId());
        try {
            this.cancelActivityInstance(actInst, statusMsg, procinst, logtag);
        } catch (Exception e) {
            logger.severeException("Exception thrown during canceActivityInstance", e);
        }
    }

    void holdActivityInstance(ActivityInstance actInst, Long procId) {
        String logtag = this.logtag(procId, actInst.getProcessInstanceId(), actInst.getActivityId(), actInst.getId());
        try {
            this.holdActivityInstance(actInst, logtag);
        } catch (Exception e) {
            logger.severeException("Exception thrown during canceActivityInstance", e);
        }
    }

    private ActivityInstance waitForActivityDone(ActivityInstance actInst)
        throws DataAccessException, InterruptedException, SQLException {
        int max_retry = 10;
        int retry_interval = 2;
        int count = 0;
        while (count<max_retry && actInst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS.intValue()) {
            logger.debug("wait for synch activity to finish: " + actInst.getId());
            Thread.sleep(retry_interval*1000);
            actInst = getDataAccess().getActivityInstance(actInst.getId());
            count++;
        }
        return actInst;
    }

    ActivityRuntime prepareActivityInstance(InternalEvent event, ProcessInstance procInst)
            throws DataAccessException, ProcessException, ServiceLocatorException {
        try {
            // for asynch engine, procInst is always null
            ActivityRuntime ar = new ActivityRuntime();
            Long activityId = event.getWorkId();
            Long workTransInstanceId = event.getTransitionInstanceId();

            // check if process instance is still alive
            ar.procinst = procInst;
            if (WorkStatus.STATUS_CANCELLED.equals(ar.procinst.getStatusCode())
                    || WorkStatus.STATUS_COMPLETED.equals(ar.procinst.getStatusCode())) {
                ar.startCase = ActivityRuntime.STARTCASE_PROCESS_TERMINATED;
                return ar;
            }

            Process processVO = getProcessDefinition(ar.procinst);
            Activity actVO = processVO.getActivityVO(activityId);
            Package pkg = PackageCache.getProcessPackage(getMainProcessDefinition(procInst).getId());
            try {
                GeneralActivity activity = pkg.getActivityImplementor(actVO);
                ar.activity = (BaseActivity)activity;
            } catch (Throwable e) {
                String logtag = this.logtag(procInst.getProcessId(), procInst.getId(), activityId, 0L);
                logger.exception(logtag, "Failed to create activity implementor instance", e);
                ar.activity = null;
            }
            boolean isSynchActivity = ar.activity!=null && ar.activity instanceof SynchronizationActivity;
            if (isSynchActivity) getDataAccess().lockProcessInstance(procInst.getId());

            List<ActivityInstance> actInsts;
            if (this.inService) actInsts = null;
            else actInsts = getDataAccess().getActivityInstances(activityId, procInst.getId(), true, isSynchActivity);
            if (actInsts==null || actInsts.isEmpty()) {
                // create activity instance and prepare it
                ar.actinst = createActivityInstance(activityId, procInst.getId());
                prepareActivitySub(processVO, actVO, ar.procinst, ar.actinst,
                        workTransInstanceId, event, ar.activity);
                if (ar.activity==null) {
                    logger.severe("Failed to load the implementor class or create instance: " + actVO.getImplementorClassName());
                    ar.startCase = ActivityRuntime.STARTCASE_ERROR_IN_PREPARE;
                } else {
                    ar.startCase = ActivityRuntime.STARTCASE_NORMAL;
                    // notify registered monitors
                    ar.activity.notifyMonitors(WorkStatus.LOGMSG_START);
                }
            } else if (isSynchActivity) {
                ar.actinst = actInsts.get(0);
                   if (ar.actinst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS.intValue())
                    ar.actinst = waitForActivityDone(ar.actinst);
                if (ar.actinst.getStatusCode()==WorkStatus.STATUS_WAITING.intValue()) {
                    if (workTransInstanceId!=null && workTransInstanceId.longValue()>0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_WAITING;
                } else if (ar.actinst.getStatusCode()==WorkStatus.STATUS_HOLD.intValue()) {
                    if (workTransInstanceId!=null && workTransInstanceId.longValue()>0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_WAITING;
                } else {    // completed - possible when there are OR conditions
                    if (workTransInstanceId!=null && workTransInstanceId.longValue()>0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_COMPLETE;
                }
            } else {
                ActivityInstance onHoldActInst = null;
                for (ActivityInstance actInst : actInsts) {
                    if (actInst.getStatusCode()==WorkStatus.STATUS_HOLD.intValue()) {
                        onHoldActInst = actInst;
                        break;
                    }
                }
                if (onHoldActInst!=null) {
                    if (workTransInstanceId!=null && workTransInstanceId.longValue()>0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, onHoldActInst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_RESUME_WAITING;
                    ar.actinst = onHoldActInst;
                } else {    // WAITING or IN_PROGRESS
                    ar.startCase = ActivityRuntime.STARTCASE_INSTANCE_EXIST;
                }
            }
            return ar;
        } catch (SQLException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } catch (NamingException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } catch (MdwException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    private void prepareActivitySub(Process processVO, Activity actVO,
            ProcessInstance pi, ActivityInstance ai, Long pWorkTransInstId,
            InternalEvent event, BaseActivity activity)
    throws DataAccessException, SQLException, NamingException, MdwException, ServiceLocatorException {


        if (logger.isInfoEnabled())
            logger.info(logtag(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId()),
                    WorkStatus.LOGMSG_START + " - " + actVO.getActivityName());

        if (pWorkTransInstId!=null && pWorkTransInstId.longValue()!=0)
            edao.completeTransitionInstance(pWorkTransInstId, ai.getId());

        if (activity==null) {
            edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_FAILED, "Failed to instantiate activity implementor");
            return;
            // note cannot throw exception here, as when implementor is not defined,
            // the error handling itself will throw exception. We failed the activity outright here.
        }
        Class<?> implClass = activity.getClass();
        TrackingTimer activityTimer = null;
        Tracked t = implClass.getAnnotation(Tracked.class);
        if (t != null) {
            String logTag = logtag(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId());
            activityTimer = new TrackingTimer(logTag, actVO.getImplementorClassName(), t.value());
        }

        List<VariableInstance> vars;
        if (processVO.isEmbeddedProcess())
            vars = edao.getProcessInstanceVariables(pi.getOwnerId());
        else
            vars = edao.getProcessInstanceVariables(pi.getId());

        event.setWorkInstanceId(ai.getId());

        activity.prepare(actVO, pi, ai, vars, pWorkTransInstId,
                event.getCompletionCode(), activityTimer, new ProcessExecutor(this));
            // prepare Activity to update SLA Instance
            // now moved to EventWaitActivity
        return;
    }

    private void removeActivitySLA(ActivityInstance ai, ProcessInstance procInst) {
        Process procdef = getProcessDefinition(procInst);
        Activity actVO = procdef.getActivityVO(ai.getActivityId());
        int sla_seconds = actVO==null?0:actVO.getSlaSeconds();
        if (sla_seconds > 0) {
            ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
            try {
                eventQueue.unscheduleEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+ai.getId());
            } catch (Exception e) {
                if (logger.isDebugEnabled()) logger.debugException("Failed to unschedule SLA", e);
            }
        }
    }

    private void failActivityInstance(ActivityInstance ai, String statusMsg,
            ProcessInstance procinst, String logtag, String abbrStatusMsg)
    throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_FAILED, statusMsg);
        removeActivitySLA(ai, procinst);
        if (logger.isInfoEnabled())
            logger.info(logtag, WorkStatus.LOGMSG_FAILED + " - " + abbrStatusMsg);
    }

    private void completeActivityInstance(ActivityInstance ai, String compcode,
            ProcessInstance procInst, String logtag)
    throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_COMPLETED, compcode);
        removeActivitySLA(ai, procInst);
        if (logger.isInfoEnabled())
            logger.info(logtag, WorkStatus.LOGMSG_COMPLETE + " - completion code "
                    + (compcode==null?"null":("'"+compcode+"'")));

    }

    private void cancelActivityInstance(ActivityInstance ai, String statusMsg,
            ProcessInstance procInst, String logtag)
    throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_CANCELLED, statusMsg);
        removeActivitySLA(ai, procInst);
        if (logger.isInfoEnabled())
            logger.info(logtag, WorkStatus.LOGMSG_CANCELLED + " - " + statusMsg);
    }

    private void holdActivityInstance(ActivityInstance ai, String logtag)
    throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_HOLD, null);
        if (logger.isInfoEnabled()) logger.info(logtag, WorkStatus.LOGMSG_HOLD);
    }

    private void suspendActivityInstance(ActivityInstance ai, String logtag, String additionalMsg)
    throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_WAITING, null);
        if (logger.isInfoEnabled()) {
            if (additionalMsg!=null) logger.info(logtag, WorkStatus.LOGMSG_SUSPEND + " - " + additionalMsg);
            else logger.info(logtag, WorkStatus.LOGMSG_SUSPEND);
        }
    }

    CompletionCode finishActivityInstance(BaseActivity activity,
            ProcessInstance pi, ActivityInstance ai, InternalEvent event, boolean bypassWait)
            throws DataAccessException, ProcessException, ActivityException, ServiceLocatorException {
        try {
            if (activity.getTimer() != null)
                activity.getTimer().start("Finish Activity");

            // Step 3  get and parse completion code
            boolean mayNeedWait = !bypassWait && activityNeedsWait(activity);
            String origCompCode = activity.getReturnCode();
            CompletionCode compCode = new CompletionCode();
            compCode.parse(origCompCode);
            Integer actInstStatus = compCode.getActivityInstanceStatus();
            if (actInstStatus==null && mayNeedWait) actInstStatus = WorkStatus.STATUS_WAITING;
            String logtag = logtag(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId());

            // Step 3a if activity not successful
            if (compCode.getEventType().equals(EventType.ERROR)) {
                failActivityInstance(ai, activity.getReturnMessage(),
                        pi, logtag, activity.getReturnMessage());
                if (!AdapterActivity.COMPCODE_AUTO_RETRY.equals(compCode.getCompletionCode())) {
                    DocumentReference docRef = createActivityExceptionDocument(pi, ai, activity, new ActivityException("Activity failed: " + ai.getId()));
                    InternalEvent outmsg = InternalEvent.createActivityErrorMessage(ai.getActivityId(),
                        ai.getId(), pi.getId(), compCode.getCompletionCode(),
                        event.getMasterRequestId(), activity.getReturnMessage(), docRef.getDocumentId());
                    sendInternalEvent(outmsg);
                }
            }

            // Step 3b if activity needs to wait
            else if (mayNeedWait && actInstStatus!=null && !actInstStatus.equals(WorkStatus.STATUS_COMPLETED)) {
                if (actInstStatus.equals(WorkStatus.STATUS_HOLD)) {
                    holdActivityInstance(ai, logtag);
                    InternalEvent outmsg = InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                } else if (actInstStatus.equals(WorkStatus.STATUS_WAITING) &&
                        (compCode.getEventType().equals(EventType.ABORT) || compCode.getEventType().equals(EventType.CORRECT)
                                || compCode.getEventType().equals(EventType.ERROR))) {
                    suspendActivityInstance(ai, logtag, null);
                    InternalEvent outmsg =  InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
                else if (actInstStatus.equals(WorkStatus.STATUS_CANCELLED)) {
                    cancelActivityInstance(ai, compCode.getCompletionCode(), pi,  logtag);
                    InternalEvent outmsg =  InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
                else {
                    suspendActivityInstance(ai, logtag, null);
                }
            }

            // Step 3c. otherwise, activity is successful and complete it
            else {
                completeActivityInstance(ai, origCompCode, pi, logtag);
                // notify registered monitors
                activity.notifyMonitors(WorkStatus.LOGMSG_COMPLETE);

                if (activity instanceof FinishActivity) {
                    String compcode = ((FinishActivity)activity).getProcessCompletionCode();
                    boolean noNotify = ((FinishActivity)activity).doNotNotifyCaller();
                    completeProcessInstance(pi, compcode, noNotify);
                    List<ProcessInstance> subProcessInsts = getDataAccess().getProcessInstances(pi.getProcessId(), OwnerType.MAIN_PROCESS_INSTANCE, pi.getId());
                    for (ProcessInstance subProcessInstanceVO : subProcessInsts) {
                        if (!subProcessInstanceVO.getStatusCode().equals(WorkStatus.STATUS_COMPLETED) &&
                                !subProcessInstanceVO.getStatusCode().equals(WorkStatus.STATUS_CANCELLED))
                        completeProcessInstance(subProcessInstanceVO, compcode, noNotify);
                    }
                } else {
                    InternalEvent outmsg = InternalEvent.createActivityNotifyMessage(ai,
                            compCode.getEventType(), event.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
            }
            return compCode;    // not used by asynch engine
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } finally {
            if (activity.getTimer() != null)
                activity.getTimer().stopAndLogTiming();
        }
    }

    ///////////// process finish

    void handleProcessFinish(InternalEvent event) throws ProcessException
    {
        try {
            String ownerType = event.getOwnerType();
            String secondaryOwnerType = event.getSecondaryOwnerType();
            if (!OwnerType.ACTIVITY_INSTANCE.equals(secondaryOwnerType)) {
                // top level processes (non-remote) or ABORT embedded processes
                ProcessInstance pi = edao.getProcessInstance(event.getWorkInstanceId());
                Process subProcVO = getProcessDefinition(pi);
                if (pi.isEmbedded()) {
                    subProcVO.getSubProcessVO(event.getWorkId());
                    String embeddedProcType = subProcVO.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
                    if (ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS.equals(embeddedProcType)) {
                        Long parentProcInstId = event.getOwnerId();
                        pi = edao.getProcessInstance(parentProcInstId);
                        this.cancelProcessInstanceTree(pi);
                        if (logger.isInfoEnabled()) {
                            logger.info(logtag(pi.getProcessId(), pi.getId(), pi.getMasterRequestId()),
                                    "Process cancelled");
                        }
                        InternalEvent procFinishMsg = InternalEvent.createProcessFinishMessage(pi);
                        if (OwnerType.ACTIVITY_INSTANCE.equals(pi.getSecondaryOwner())) {
                            procFinishMsg.setSecondaryOwnerType(pi.getSecondaryOwner());
                            procFinishMsg.setSecondaryOwnerId(pi.getSecondaryOwnerId());
                        }
                        this.sendInternalEvent(procFinishMsg);
                    }
                }
            } else if (ownerType.equals(OwnerType.PROCESS_INSTANCE)
                    || ownerType.equals(OwnerType.MAIN_PROCESS_INSTANCE)) {
                // local process call or call to error/correction/delay handler
                Long activityInstId = event.getSecondaryOwnerId();
                ActivityInstance actInst = edao.getActivityInstance(activityInstId);
                ProcessInstance procInst = edao.getProcessInstance(actInst.getProcessInstanceId());
                BaseActivity cntrActivity = prepareActivityForResume(event,procInst, actInst);
                if (cntrActivity!=null) {
                    resumeProcessInstanceForSecondaryOwner(event, cntrActivity);
                }    // else the process is completed/cancelled
            }
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    private void handleResumeOnHold(GeneralActivity cntrActivity, ActivityInstance actInst,
            ProcessInstance procInst)
        throws DataAccessException, MdwException {
        try {
            InternalEvent event = InternalEvent.createActivityNotifyMessage(actInst,
                    EventType.RESUME, procInst.getMasterRequestId(), actInst.getStatusCode()==WorkStatus.STATUS_COMPLETED? "Completed" : null);
            boolean finished = ((SuspendibleActivity)cntrActivity).resumeWaiting(event);
            this.resumeActivityFinishSub(actInst, (BaseActivity)cntrActivity, procInst,
                    finished, true);
        } catch (Exception e) {
//          throw new ProcessException(-1, e.getMessage(), e);
            logger.severeException("Resume failed", e);
            String statusMsg = "activity failed during resume";
            try {
                String logtag = logtag(procInst.getProcessId(), procInst.getId(),
                        actInst.getActivityId(), actInst.getId());
                failActivityInstance(actInst, statusMsg, procInst, logtag, statusMsg);
            } catch (SQLException e1) {
                throw new DataAccessException(-1, e1.getMessage(), e1);
            }
            DocumentReference docRef = createActivityExceptionDocument(procInst, actInst, (BaseActivity)cntrActivity, e);
            InternalEvent event = InternalEvent.createActivityErrorMessage(
                    actInst.getActivityId(), actInst.getId(), procInst.getId(), null,
                    procInst.getMasterRequestId(), statusMsg, docRef.getDocumentId());
            this.sendInternalEvent(event);
        }
    }

     /**
     * Resumes the process instance for the secondary owner
     *
     * @param childInst child process instance
     * @param masterReqId
     */
    private void resumeProcessInstanceForSecondaryOwner(InternalEvent event,
            BaseActivity cntrActivity) throws Exception {
        Long actInstId = event.getSecondaryOwnerId();
        ActivityInstance actInst = edao.getActivityInstance(actInstId);
        String masterRequestId = event.getMasterRequestId();
//            Long parentInstId = eventMessageDoc.getEventMessage().getWorkOwnerId();
        Long parentInstId = actInst.getProcessInstanceId();
        ProcessInstance parentInst = edao.getProcessInstance(parentInstId);
        String logtag = logtag(parentInst.getProcessId(), parentInstId, actInst.getActivityId(), actInstId);
        boolean isEmbeddedProcess;
        if (event.getOwnerType().equals(OwnerType.MAIN_PROCESS_INSTANCE)) isEmbeddedProcess = true;
        else if (event.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
            try {
                Process subprocdef = ProcessCache.getProcess(event.getWorkId());
                isEmbeddedProcess = subprocdef.isEmbeddedProcess();
            } catch (Exception e1) {
                // can happen when the subprocess is remote
                logger.info(logtag,
                        "subprocess definition cannot be found - treat it as a remote process - id "
                        + event.getWorkId());
                isEmbeddedProcess = false;
            }
        } else isEmbeddedProcess = false;    // including the case the subprocess is remote
        String compCode = event.getCompletionCode();
        if (isEmbeddedProcess) {
            // mark parent process instance in progress
            edao.setProcessInstanceStatus(parentInst.getId(), WorkStatus.STATUS_IN_PROGRESS);
            if (logger.isInfoEnabled())
                logger.info(logtag,    "Activity resumed from embedded subprocess, which returns completion code " + compCode);
            CompletionCode parsedCompCode = new CompletionCode();
            parsedCompCode.parse(event.getCompletionCode());
            Transition outgoingWorkTransVO = null;
            if (compCode==null || parsedCompCode.getEventType().equals(EventType.RESUME)) {        // default behavior
                if (actInst.getStatusCode()==WorkStatus.STATUS_HOLD ||
                        actInst.getStatusCode()==WorkStatus.STATUS_COMPLETED) {
                    handleResumeOnHold(cntrActivity, actInst, parentInst);
                } else if (actInst.getStatusCode()==WorkStatus.STATUS_FAILED) {
                    completeActivityInstance(actInst, compCode, parentInst, logtag);
                    // notify registered monitors
                    cntrActivity.notifyMonitors(WorkStatus.LOGMSG_FAILED);

                    InternalEvent jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.FINISH, masterRequestId, null);
                    sendInternalEvent(jmsmsg);
                } else {
                    // other status simply ignore
                }
            } else if (parsedCompCode.getEventType().equals(EventType.ABORT)) {    // TaskAction.ABORT and TaskAction.CANCEL
                String comment = actInst.getMessage() + "  \nException handler returns " + compCode;
                if (actInst.getStatusCode()!=WorkStatus.STATUS_COMPLETED) {
                    cancelActivityInstance(actInst, comment, parentInst, logtag);
                }
                    if (parsedCompCode.getCompletionCode()!=null && parsedCompCode.getCompletionCode().startsWith("process"))    {// TaskAction.ABORT
                        boolean invoke_abort_handler = true;
                        if (invoke_abort_handler) {
                            InternalEvent outgoingMsg = InternalEvent.createActivityNotifyMessage(actInst,
                                    EventType.ABORT, parentInst.getMasterRequestId(), null);
                            sendInternalEvent(outgoingMsg);
                        } else {
                            completeProcessInstance(parentInst, EventType.EVENTNAME_ABORT, false);
                        }
                    }
            } else if (parsedCompCode.getEventType().equals(EventType.START)) {        // TaskAction.RETRY
                String comment = actInst.getMessage() +
                    "  \nException handler returns " + compCode;
                if (actInst.getStatusCode()!=WorkStatus.STATUS_COMPLETED) {
                    cancelActivityInstance(actInst, comment, parentInst, logtag);
                }
                retryActivity(parentInst, actInst.getActivityId(), null, masterRequestId);
            } else {    // event type must be FINISH
                if (parsedCompCode.getCompletionCode()!=null)
                    outgoingWorkTransVO = findTaskActionWorkTransition(parentInst, actInst, parsedCompCode.getCompletionCode());
                if (actInst.getStatusCode()!=WorkStatus.STATUS_COMPLETED) {
                    completeActivityInstance(actInst, compCode, parentInst, logtag);
                    cntrActivity.notifyMonitors(WorkStatus.LOGMSG_COMPLETE);
                }
                InternalEvent jmsmsg;
                int delay = 0;
                if (outgoingWorkTransVO != null) {
                    // is custom action (RESUME), transition accordingly
                    TransitionInstance workTransInst = createTransitionInstance(outgoingWorkTransVO, parentInstId);
                    jmsmsg = InternalEvent.createActivityStartMessage(
                            outgoingWorkTransVO.getToWorkId(), parentInstId,
                              workTransInst.getTransitionInstanceID(), masterRequestId,
                              outgoingWorkTransVO.getLabel());
                    delay = outgoingWorkTransVO.getTransitionDelay();
                } else {
                    jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.FINISH, masterRequestId, null);
                }
                if (delay>0) {
                    String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + parentInstId
                        + "start" + outgoingWorkTransVO.getToWorkId();
                    sendDelayedInternalEvent(jmsmsg, delay, msgid, false);
                } else sendInternalEvent(jmsmsg);
            }
        } else {    // must be InvokeProcessActivity
            if (actInst.getStatusCode()==WorkStatus.STATUS_WAITING || actInst.getStatusCode()==WorkStatus.STATUS_HOLD) {
                boolean isSynchronized = ((InvokeProcessActivity)cntrActivity).resume(event);
                if (isSynchronized) {   // all subprocess instances terminated
                    // mark parent process instance in progress
                    edao.setProcessInstanceStatus(parentInst.getId(), WorkStatus.STATUS_IN_PROGRESS);
                    // complete the activity and send activity FINISH message
                    CompletionCode parsedCompCode = new CompletionCode();
                    parsedCompCode.parse(event.getCompletionCode());
                    if (parsedCompCode.getEventType().equals(EventType.ABORT)) {
                        cancelActivityInstance(actInst, "Subprocess is cancelled", parentInst, logtag);
                    } else {
                        completeActivityInstance(actInst, compCode, parentInst, logtag);
                        cntrActivity.notifyMonitors(WorkStatus.LOGMSG_COMPLETE);
                    }
                    InternalEvent jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.FINISH, masterRequestId, compCode);
                    sendInternalEvent(jmsmsg);
                }  else {
                    // multiple instances and not all terminated - do nothing
                    logger.info(logtag, "Activity continue suspend - not all child processes have completed");
                }
            } else {  // status is COMPLETED or others
                // do nothing - asynchronous subprocess call
                logger.info(logtag, "Activity not waiting for subprocess - asynchronous subprocess call");
            }
        }
    }

    private void completeProcessInstance(ProcessInstance procInst) throws Exception {
        edao.setProcessInstanceStatus(procInst.getId(), WorkStatus.STATUS_COMPLETED);
        if (!inService) {
            edao.removeEventWaitForProcessInstance(procInst.getId());
            this.cancelTasksOfProcessInstance(procInst);
        }
    }

    /**
     *
     * @param processInstVO
     * @param processVO
     * @param pMessage
     */
    private void completeProcessInstance(ProcessInstance processInst, String completionCode, boolean noNotify)
    throws Exception {

        Process processVO = getProcessDefinition(processInst);
        InternalEvent retMsg = InternalEvent.createProcessFinishMessage(processInst);

        if (OwnerType.ACTIVITY_INSTANCE.equals(processInst.getSecondaryOwner())) {
            retMsg.setSecondaryOwnerType(processInst.getSecondaryOwner());
            retMsg.setSecondaryOwnerId(processInst.getSecondaryOwnerId());
        }

        if (completionCode==null) completionCode = processInst.getCompletionCode();
        if (completionCode!=null) retMsg.setCompletionCode(completionCode);

        boolean isCancelled = false;
        if (completionCode==null) {
            completeProcessInstance(processInst);
        } else if (processVO.isEmbeddedProcess()) {
             completeProcessInstance(processInst);
            retMsg.setCompletionCode(completionCode);
        } else {
            CompletionCode parsedCompCode = new CompletionCode();
            parsedCompCode.parse(completionCode);
            if (parsedCompCode.getEventType().equals(EventType.ABORT)) {
                this.cancelProcessInstanceTree(processInst);
                isCancelled = true;
            } else if (parsedCompCode.getEventType().equals(EventType.FINISH)) {
                completeProcessInstance(processInst);
                if (parsedCompCode.getCompletionCode()!=null) {
                    completionCode = parsedCompCode.getCompletionCode();
                    retMsg.setCompletionCode(completionCode);
                } else completionCode = null;
            } else {
                completeProcessInstance(processInst);
                retMsg.setCompletionCode(completionCode);
            }
        }
        if (!noNotify) sendInternalEvent(retMsg);
        if (logger.isInfoEnabled()) {
            logger.info(logtag(processVO.getProcessId(), processInst.getId(), processInst.getMasterRequestId()),
                    (isCancelled?WorkStatus.LOGMSG_PROC_CANCEL:WorkStatus.LOGMSG_PROC_COMPLETE) + " - " + processVO.getProcessQualifiedName()
                    + (isCancelled?"":completionCode==null?" completion code is null":(" completion code = "+completionCode)));
        }
        notifyMonitors(processInst, WorkStatus.LOGMSG_PROC_COMPLETE);
    }

    /**
     * Look up the appropriate work transition for an embedded exception handling subprocess.
     * @param parentInstance the parent process
     * @param activityInstance the activity in the main process
     * @param taskAction the selected task action
     * @return the matching work transition, if found
     */
    private Transition findTaskActionWorkTransition(ProcessInstance parentInstance,
            ActivityInstance activityInstance, String taskAction) {
        if (taskAction == null)
            return null;

        Process processVO = getProcessDefinition(parentInstance);
        Transition workTransVO = processVO.getWorkTransition(activityInstance.getActivityId(), EventType.RESUME, taskAction);
        if (workTransVO == null) {
            // try upper case
            workTransVO = processVO.getWorkTransition(activityInstance.getActivityId(), EventType.RESUME, taskAction.toUpperCase());
        }
        if (workTransVO == null) {
            workTransVO = processVO.getWorkTransition(activityInstance.getActivityId(), EventType.FINISH, taskAction);
        }
        return workTransVO;
    }

    private void retryActivity(ProcessInstance procInst, Long actId,
            String completionCode, String masterRequestId)
            throws DataAccessException, SQLException, MdwException {
        // make sure any other activity instances are closed
        List<ActivityInstance> activityInstances = edao.getActivityInstances(actId, procInst.getId(),
                true, false);
        for (ActivityInstance actInst :  activityInstances) {
            if (actInst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS.intValue()
                        || actInst.getStatusCode()==WorkStatus.STATUS_PENDING_PROCESS.intValue()) {
                String logtag = logtag(procInst.getProcessId(), procInst.getId(),
                        actId, actInst.getId());
                failActivityInstance(actInst, "Retry Activity Action",
                        procInst, logtag, "Retry Activity Action");
            }
        }
        // start activity again
        InternalEvent event = InternalEvent.createActivityStartMessage(actId,
                procInst.getId(), null, masterRequestId, EventType.EVENTNAME_START);
        sendInternalEvent(event);
    }

    /////////////// activity resume

    private boolean validateProcessInstance(ProcessInstance processInst) {
        Integer status = processInst.getStatusCode();
        if (WorkStatus.STATUS_CANCELLED.equals(status)) {
            logger.info("ProcessInstance has been cancelled. ProcessInstanceId = " + processInst.getId());
            return false;
        } else if (WorkStatus.STATUS_COMPLETED.equals(status)) {
            logger.info("ProcessInstance has been completed. ProcessInstanceId = " + processInst.getId());
            return false;
        } else return true;
    }


    private BaseActivity prepareActivityForResume(InternalEvent event,
                ProcessInstance procInst, ActivityInstance actInst)
    throws DataAccessException, SQLException
    {
        Long actId = actInst.getActivityId();
        Long procInstId = actInst.getProcessInstanceId();

        if (!validateProcessInstance(procInst)) {
            if (logger.isInfoEnabled())
                logger.info(logtag(procInst.getProcessId(), procInstId, actId, actInst.getId()),
                    "Activity would resume, but process is no longer alive");
            return null;
        }
        if (logger.isInfoEnabled())
            logger.info(logtag(procInst.getProcessId(), procInstId, actId, actInst.getId()), "Activity to resume");

        Process processVO = getProcessDefinition(procInst);
        Activity actVO = processVO.getActivityVO(actId);

        TrackingTimer activityTimer = null;
        try {
            // use design-time package
            Package pkg = PackageCache.getProcessPackage(getMainProcessDefinition(procInst).getId());

            BaseActivity cntrActivity = (BaseActivity)pkg.getActivityImplementor(actVO);
            Tracked t = cntrActivity.getClass().getAnnotation(Tracked.class);
            if (t != null) {
                String logTag = logtag(procInst.getProcessId(), procInst.getId(), actId, actInst.getId());
                activityTimer = new TrackingTimer(logTag, cntrActivity.getClass().getName(), t.value());
                activityTimer.start("Prepare Activity for Resume");
            }
            List<VariableInstance> vars = processVO.isEmbeddedProcess()?
                    edao.getProcessInstanceVariables(procInst.getOwnerId()):
                        edao.getProcessInstanceVariables(procInstId);
            // procInst.setVariables(vars);     set inside edac method
            Long workTransitionInstId = event.getTransitionInstanceId();
            cntrActivity.prepare(actVO, procInst, actInst, vars, workTransitionInstId,
                    event.getCompletionCode(), activityTimer, new ProcessExecutor(this));
            return cntrActivity;
        } catch (Exception e) {
            logger.severeException("Unable to instantiate implementer " + actVO.getImplementorClassName(), e);
            return null;
        }
        finally {
            if (activityTimer != null) {
                activityTimer.stopAndLogTiming();
            }
        }
    }

    private boolean isProcessInstanceResumable(ProcessInstance pInstance) {
        int statusCd = pInstance.getStatusCode().intValue();
        if (statusCd == WorkStatus.STATUS_COMPLETED.intValue()) {
            return false;
        } else if (statusCd == WorkStatus.STATUS_CANCELLED.intValue()) {
            return false;
        }
        return true;
    }

    ActivityRuntime resumeActivityPrepare(ProcessInstance procInst,
            InternalEvent event, boolean resumeOnHold)
            throws ProcessException, DataAccessException {
        Long actInstId = event.getWorkInstanceId();
        try {
            ActivityRuntime ar = new ActivityRuntime();
            ar.startCase = ActivityRuntime.RESUMECASE_NORMAL;
            ar.actinst = edao.getActivityInstance(actInstId);
            ar.procinst = procInst;
            if (!this.isProcessInstanceResumable(ar.procinst)) {
                ar.startCase = ActivityRuntime.RESUMECASE_PROCESS_TERMINATED;
                logger.info(logtag(ar.procinst.getProcessId(), ar.procinst.getId(),
                            ar.actinst.getActivityId(), actInstId),
                        "Cannot resume activity instance as the process is completed/canceled");
                return ar;
            }
            if (!resumeOnHold && ar.actinst.getStatusCode()!=WorkStatus.STATUS_WAITING.intValue()) {
                logger.info(logtag(ar.procinst.getProcessId(), ar.procinst.getId(),
                        ar.actinst.getActivityId(), actInstId),
                    "Cannot resume activity instance as it is not waiting any more");
                ar.startCase = ActivityRuntime.RESUMECASE_ACTIVITY_NOT_WAITING;
                return ar;
            }
            ar.activity = prepareActivityForResume(event, ar.procinst, ar.actinst);
            if (resumeOnHold) event.setEventType(EventType.RESUME);
            else event.setEventType(EventType.FINISH);
            return ar;
        } catch (SQLException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    private void resumeActivityFinishSub(ActivityInstance actinst,
            BaseActivity activity, ProcessInstance procinst,
            boolean finished, boolean resumeOnHold)
        throws DataAccessException, SQLException, MdwException {
        String logtag = logtag(procinst.getProcessId(),procinst.getId(),
                actinst.getActivityId(),actinst.getId());
        if (finished) {
            CompletionCode completionCode = new CompletionCode();
            completionCode.parse(activity.getReturnCode());
            if (WorkStatus.STATUS_HOLD.equals(completionCode.getActivityInstanceStatus())) {
                holdActivityInstance(actinst, logtag);
            } else if (WorkStatus.STATUS_WAITING.equals(completionCode.getActivityInstanceStatus())) {
                suspendActivityInstance(actinst, logtag, "continue suspend");
            } else if (WorkStatus.STATUS_CANCELLED.equals(completionCode.getActivityInstanceStatus())) {
                cancelActivityInstance(actinst, "Cancelled upon resume", procinst, logtag);
            } else if (WorkStatus.STATUS_FAILED.equals(completionCode.getActivityInstanceStatus())) {
                failActivityInstance(actinst, "Failed upon resume", procinst, logtag, activity.getReturnMessage());
            } else {    // status is null or Completed
                completeActivityInstance(actinst, completionCode.toString(), procinst, logtag);
                // notify registered monitors
                activity.notifyMonitors(WorkStatus.LOGMSG_COMPLETE);
            }
            InternalEvent event = InternalEvent.createActivityNotifyMessage(actinst,
                    completionCode.getEventType(), procinst.getMasterRequestId(),
                    completionCode.getCompletionCode());
            sendInternalEvent(event);
        } else {
            if (resumeOnHold) {
                suspendActivityInstance(actinst, logtag, "resume waiting after hold");
            } else {
                if (logger.isInfoEnabled()) logger.info(logtag, "continue suspend");
            }
        }
    }

    void resumeActivityFinish(ActivityRuntime ar,
            boolean finished, InternalEvent event, boolean resumeOnHold)
            throws DataAccessException, ProcessException {
        try {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().start("Resume Activity Finish");
            this.resumeActivityFinishSub(ar.actinst, ar.activity, ar.procinst,
                    finished, resumeOnHold);
        } catch (SQLException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } catch (MdwException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } finally {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().stopAndLogTiming();
        }
    }

    boolean resumeActivityExecute(ActivityRuntime ar,
            InternalEvent event, boolean resumeOnHold) throws ActivityException {
        boolean finished;
        try {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().start("Resume Activity");
            if (resumeOnHold) finished = ((SuspendibleActivity)ar.activity).resumeWaiting(event);
            else finished = ((SuspendibleActivity)ar.activity).resume(event);
        }
        finally {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().stopAndLogTiming();
        }
        return finished;
    }

    Map<String, String> getOutputParameters(Long procInstId, Long procId) throws SQLException,
            ProcessException, DataAccessException {
        Process subprocDef = ProcessCache.getProcess(procId);
        Map<String, String> params = new HashMap<String, String>();
        boolean passDocContent = (isInService() && getDataAccess().getPerformanceLevel() >= 5) || getDataAccess().getPerformanceLevel() >= 9 ;  // DHO  (if not serviceProc then lvl9)
        for (Variable var : subprocDef.getVariables()) {
            if (var.getVariableCategory().intValue() == Variable.CAT_OUTPUT
                    || var.getVariableCategory().intValue() == Variable.CAT_INOUT) {
                VariableInstance vio = getDataAccess()
                        .getVariableInstance(procInstId,
                                var.getVariableName());
                if (vio != null) {
                    if (passDocContent && vio.isDocument()) {
                        Document docvo = getDocument((DocumentReference)vio.getData(), false);
                        if (docvo != null)
                            params.put(var.getVariableName(), docvo.getContent(getPackage(subprocDef)));
                    }
                    else {
                        params.put(var.getVariableName(), vio.getStringValue());
                    }
                }
            }
        }
        return params;
    }

    void resumeActivityException(
            ProcessInstance procInst,
            Long actInstId, BaseActivity activity, Throwable cause) {
        String compCode = null;
        try {
            String statusMsg = buildStatusMessage(cause);
            ActivityInstance actInst = edao.getActivityInstance(actInstId);
            String logtag = logtag(procInst.getProcessId(), procInst.getId(),
                    actInst.getActivityId(), actInst.getId());
            failActivityInstance(actInst, statusMsg, procInst, logtag, "Exception in resume");
            if (activity==null || !AdapterActivity.COMPCODE_AUTO_RETRY.equals(activity.getReturnCode())) {
                Throwable th = cause == null ? new ActivityException("Resume activity: " + actInstId) : cause;
                DocumentReference docRef = createActivityExceptionDocument(procInst, actInst, activity, th);
                InternalEvent outgoingMsg = InternalEvent.createActivityErrorMessage(
                            actInst.getActivityId(), actInst.getId(),
                            procInst.getId(), compCode, procInst.getMasterRequestId(),
                            statusMsg, docRef.getDocumentId());
                sendInternalEvent(outgoingMsg);
            }
        }
        catch (Exception e) {
            logger.severeException("\n\n*****Failed in handleResumeException*****\n", e);
        }
    }

    //////// handle process abort

    /**
     * Abort a single process instance by process instance ID,
     * or abort potentially multiple (but typically one) process instances
     * by process ID and owner ID.
     *
     * @param pMessage
     * @param pProcessInst
     * @param pCause
     * @throws ProcessHandlerException
     *
     */
    void abortProcessInstance(InternalEvent event)
      throws ProcessException {
        Long processId = event.getWorkId();
        String processOwner = event.getOwnerType();
        Long processOwnerId = event.getOwnerId();
        Long processInstId = event.getWorkInstanceId();
        try {
            if (processInstId!=null && processInstId.longValue()!=0L) {
                ProcessInstance pi = edao.getProcessInstance(processInstId);
                cancelProcessInstanceTree(pi);
                if (logger.isInfoEnabled()) {
                    logger.info(logtag(pi.getProcessId(), pi.getId(), pi.getMasterRequestId()),
                            "Process cancelled");
                }
            } else {
                List<ProcessInstance> coll = edao.getProcessInstances(processId, processOwner, processOwnerId);
                if (CollectionUtil.isEmpty(coll)) {
                    logger.info("No Process Instances for the Process and Owner");
                    return;
                }
                for (ProcessInstance pi : coll) {
                    // there really should have only one
                    cancelProcessInstanceTree(pi);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ProcessException(ex.getMessage());
        }
    }

    /**
     * Cancels the process instance as well as all descendant process instances.
     *
     * The method deregister associated event wait instances.
     *
     * @param pProcessInstId
     * @return new WorkInstance
     * @throws SQLException
     * @throws ProcessException
     */
    private void cancelProcessInstanceTree(ProcessInstance pi)
    throws Exception {
        if (pi.getStatusCode().equals(WorkStatus.STATUS_COMPLETED) ||
                pi.getStatusCode().equals(WorkStatus.STATUS_CANCELLED)) {
            throw new ProcessException("ProcessInstance is not in a cancellable state");
        }
        List<ProcessInstance> childInstances = edao.getChildProcessInstances(pi.getId());
        for (ProcessInstance child : childInstances) {
            if (!child.getStatusCode().equals(WorkStatus.STATUS_COMPLETED)
                    && !child.getStatusCode().equals(WorkStatus.STATUS_CANCELLED)) {
                this.cancelProcessInstanceTree(child);
            } else {
                logger.info("Descendent ProcessInstance in not in a cancellable state. ProcessInstanceId="
                    + child.getId());
            }
        }
        this.cancelProcessInstance(pi);
    }

    /**
     * Cancels a single process instance.
     * It cancels all active transition instances, all event wait instances,
     * and sets the process instance into canceled status.
     *
     * The method does not cancel task instances
     *
     * @param pProcessInst
     * @return new WorkInstance
     */
    private void cancelProcessInstance(ProcessInstance pProcessInst)
        throws Exception {
        edao.cancelTransitionInstances(pProcessInst.getId(),
                "ProcessInstance has been cancelled.", null);
        edao.setProcessInstanceStatus(pProcessInst.getId(), WorkStatus.STATUS_CANCELLED);
        edao.removeEventWaitForProcessInstance(pProcessInst.getId());
        this.cancelTasksOfProcessInstance(pProcessInst);
    }

    /////////////////////// other

    private void cancelTasksOfProcessInstance(ProcessInstance procInst)
    throws NamingException, JMSException, SQLException, ServiceLocatorException, MdwException {
        List<ProcessInstance> processInstanceList =
            edao.getChildProcessInstances(procInst.getId());
        List<Long> procInstIds = new ArrayList<Long>();
        procInstIds.add(procInst.getId());
        for (ProcessInstance pi : processInstanceList) {
            Process pidef = getProcessDefinition(pi);
            if (pidef.isEmbeddedProcess()) procInstIds.add(pi.getId());
        }
        ServiceLocator.getTaskManager().cancelTasksForProcessInstances(procInstIds);
    }

    EventWaitInstance createEventWaitInstance(Long actInstId,
            String pEventName, String compCode,
            boolean pRecurring, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        return createEventWaitInstance(actInstId, pEventName, compCode, pRecurring, notifyIfArrived, false);
    }

    EventWaitInstance createEventWaitInstance(
            Long actInstId, String pEventName, String compCode,
            boolean pRecurring, boolean notifyIfArrived, boolean isBroadcast)
    throws DataAccessException, ProcessException {
        try {
            String FINISH = EventType.getEventTypeName(EventType.FINISH);
            if (compCode==null||compCode.length()==0) compCode = FINISH;
            EventWaitInstance ret = null;
            Long documentId = null;
            if (isBroadcast) {
                documentId = edao.recordBroadcastEventWait(pEventName,
                        3600,       // TODO set this value in designer!
                        actInstId, compCode);
                if (logger.isInfoEnabled()) {
                    logger.info("registered event wait event='"
                            + pEventName + "' actInst=" + actInstId
                            + " as broadcast-waiting");
                }
            }
            else {
                documentId = edao.recordEventWait(pEventName,
                        !pRecurring,
                        3600,       // TODO set this value in designer!
                        actInstId, compCode);
                if (logger.isInfoEnabled()) {
                    logger.info("registered event wait event='"
                            + pEventName + "' actInst=" + actInstId
                            + (pRecurring?" as recurring":"as non-recurring"));
                }
            }
            if (documentId!=null) {
                if (logger.isInfoEnabled()) {
                    logger.info((notifyIfArrived?"notify":"return") +
                            " event before registration: event='"
                            + pEventName + "' actInst=" + actInstId);
                }
                if (notifyIfArrived) {
                    if (compCode.equals(FINISH)) compCode = null;
                    ActivityInstance actInst = edao.getActivityInstance(actInstId);
                    resumeActivityInstance(actInst, compCode, documentId, null, 0);
                    edao.removeEventWaitForActivityInstance(actInstId, "activity notified");
                } else {
                    edao.removeEventWaitForActivityInstance(actInstId, "activity to notify is returned");
                }
                ret = new EventWaitInstance();
                ret.setMessageDocumentId(documentId);
                ret.setCompletionCode(compCode);
                Document docvo = edao.getDocument(documentId, true);
                edao.updateDocumentInfo(docvo);
            }
            return ret;
        } catch (MdwException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        } catch (SQLException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }

    }

    EventWaitInstance createEventWaitInstances(Long actInstId,
            String[] pEventNames, String[] pWakeUpEventTypes,
            boolean[] pEventOccurances, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        return createEventWaitInstances(actInstId, pEventNames, pWakeUpEventTypes, pEventOccurances, notifyIfArrived, false);
    }

   /**
     * Method that creates the event log based on the passed in params
     *
     * @param pEventNames
     * @param pEventSources
     * @param pEventOwner
     * @param pEventOwnerId
     * @param pWorkTransInstId
     * @param pEventTypes
     * @param pEventOccurances
     * @param pDeRegisterSiblings
     * @return EventWaitInstance
     * @throws SQLException
     * @throws ProcessException
     */
    EventWaitInstance createEventWaitInstances(Long actInstId,
            String[] pEventNames, String[] pWakeUpEventTypes,
            boolean[] pEventOccurances, boolean notifyIfArrived, boolean isBroadcast)
    throws DataAccessException, ProcessException {

        try {
            EventWaitInstance ret = null;
            Long documentId = null;
            String pCompCode = null;
            int i;
            for (i=0; i < pEventNames.length; i++) {
                pCompCode = pWakeUpEventTypes[i];
                if (isBroadcast) {
                    documentId = edao.recordBroadcastEventWait(pEventNames[i],
                            3600,       // TODO set this value in designer!
                            actInstId, pWakeUpEventTypes[i]);
                    if (logger.isInfoEnabled()) {
                        logger.info("registered event wait event='"
                                + pEventNames[i] + "' actInst=" + actInstId
                                + " as broadcast-waiting");
                    }
                }
                else {
                    documentId = edao.recordEventWait(pEventNames[i],
                            !pEventOccurances[i],
                            3600,       // TODO set this value in designer!
                            actInstId, pWakeUpEventTypes[i]);
                    if (logger.isInfoEnabled()) {
                        logger.info("registered event wait event='"
                                + pEventNames[i] + "' actInst=" + actInstId
                                + (pEventOccurances[i]?" as recurring":"as non-recurring"));
                    }
                }
                   if (documentId!=null) break;
            }
            if (documentId!=null) {
                if (logger.isInfoEnabled()) {
                    logger.info((notifyIfArrived?"notify":"return") +
                            " event before registration: event='"
                            + pEventNames[i] + "' actInst=" + actInstId);
                }
                if (pCompCode!=null && pCompCode.length()==0) pCompCode = null;
                if (notifyIfArrived) {
                    ActivityInstance actInst = edao.getActivityInstance(actInstId);
                    resumeActivityInstance(actInst, pCompCode, documentId, null, 0);
                    edao.removeEventWaitForActivityInstance(actInstId, "activity notified");
                } else {
                    edao.removeEventWaitForActivityInstance(actInstId, "activity to notify is returned");
                }
                ret = new EventWaitInstance();
                ret.setMessageDocumentId(documentId);
                ret.setCompletionCode(pCompCode);
                Document docvo = edao.getDocument(documentId, true);
                edao.updateDocumentInfo(docvo);
            }
            return ret;
        } catch (SQLException e) {
            throw new ProcessException(-1, e.getMessage(), e);
           } catch (MdwException e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    EventWaitInstance createBroadcastEventWaitInstances(Long actInstId,
            String[] pEventNames, String[] pWakeUpEventTypes,
            boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        return createEventWaitInstances(actInstId, pEventNames, pWakeUpEventTypes, null, notifyIfArrived, true);
    }

    Integer broadcast(String pEventName, Long pEventInstId,
            String message, int delay) throws DataAccessException, EventException, SQLException {
        return notifyProcess(pEventName, pEventInstId, message, delay, true);
    }

    Integer notifyProcess(String pEventName, Long pEventInstId,
            String message, int delay)
    throws DataAccessException, EventException, SQLException {
        return notifyProcess(pEventName, pEventInstId, message, delay, false);
    }

    Integer notifyProcess(String pEventName, Long pEventInstId,
                    String message, int delay, boolean isBroadcast)
    throws DataAccessException, EventException, SQLException {
        List<EventWaitInstance> waiters = null;
        if (isBroadcast)
            waiters = edao.recordBroadcastEventArrive(pEventName, pEventInstId);
        else
            waiters = edao.recordEventArrive(pEventName, pEventInstId);

        if (waiters!=null) {
            boolean hasFailures = false;
            try {
                for (EventWaitInstance inst : waiters) {
                    String pCompCode = inst.getCompletionCode();
                    if (pCompCode!=null && pCompCode.length()==0) pCompCode = null;
                    if (logger.isInfoEnabled()) {
                        logger.info("notify event after registration: event='"
                                + pEventName + "' actInst=" + inst.getActivityInstanceId());
                    }
                    ActivityInstance actInst = edao.getActivityInstance(inst.getActivityInstanceId());
                    if (actInst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS.intValue()) {
                        // assuming it is a service process waiting for message
                        JSONObject json = new JsonObject();
                        json.put("ACTION", "NOTIFY");
                        json.put("CORRELATION_ID", pEventName);
                        json.put("MESSAGE", message);
                        internalMessenger.broadcastMessage(json.toString());
                    } else {
                        resumeActivityInstance(actInst, pCompCode, pEventInstId, message, delay);
                    }
                    // deregister wait instances
                    edao.removeEventWaitForActivityInstance(inst.getActivityInstanceId(), "activity notified");
                    if (pEventInstId!=null && pEventInstId.longValue()>0) {
                        Document docvo = edao.getDocument(pEventInstId, true);
                        edao.updateDocumentInfo(docvo);
                    }
                }
            } catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new EventException(ex.getMessage());
            }
            if (hasFailures) return EventInstance.RESUME_STATUS_PARTIAL_SUCCESS;
            else return EventInstance.RESUME_STATUS_SUCCESS;
        } else return EventInstance.RESUME_STATUS_NO_WAITERS;
    }

    private boolean isProcessInstanceProgressable(ProcessInstance pInstance) {

        int statusCd = pInstance.getStatusCode().intValue();
        if (statusCd == WorkStatus.STATUS_COMPLETED.intValue()) {
            return false;
        } else if (statusCd == WorkStatus.STATUS_CANCELLED.intValue()) {
            return false;
        } else if (statusCd == WorkStatus.STATUS_HOLD.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * Sends a RESUME internal event to resume the activity instance.
     *
     * This may be called in the following cases:
     *   1) received an external event (including the case the message is received before registration)
     *       In this case, the argument message is populated.
     *   2) when register even wait instance, and the even has already arrived. In this case
     *       the argument message null.
     *
     * @param pProcessInstId
     * @param pCompletionCode
     */
    private void resumeActivityInstance(ActivityInstance actInst,
            String pCompletionCode, Long documentId, String message, int delay)
    throws DataAccessException, MdwException, SQLException {
        ProcessInstance pi = edao.getProcessInstance(actInst.getProcessInstanceId());
        if (!this.isProcessInstanceResumable(pi)) {
            logger.info("ProcessInstance in NOT resumable. ProcessInstanceId:" + pi.getId());
        }
        InternalEvent outgoingMsg = InternalEvent.
            createActivityNotifyMessage(actInst, EventType.RESUME,
                    pi.getMasterRequestId(), pCompletionCode);
        if (documentId!=null) {        // should be always true
            outgoingMsg.setSecondaryOwnerType(OwnerType.DOCUMENT);
            outgoingMsg.setSecondaryOwnerId(documentId);
        }
        if (message!=null && message.length()<2500) {
            outgoingMsg.addParameter("ExternalEventMessage", message);
        }
        if (this.isProcessInstanceProgressable(pi)) {
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
        }
        if (delay>0) this.sendDelayedInternalEvent(outgoingMsg, delay,
                ScheduledEvent.INTERNAL_EVENT_PREFIX+actInst.getId(), false);
        else this.sendInternalEvent(outgoingMsg);
    }

    void sendInternalEvent(InternalEvent event) throws MdwException {
        internalMessenger.sendMessage(event, edao);
    }

    void sendDelayedInternalEvent(InternalEvent event, int delaySeconds, String msgid, boolean isUpdate)
        throws MdwException {
        internalMessenger.sendDelayedMessage(event, delaySeconds, msgid, isUpdate, edao);
    }

    boolean isInService() {
        return inService;
    }

    boolean isInMemory() {
        if (null != edao && edao.getPerformanceLevel() >= 9)
            return true;
        return false;
    }

    /**
     * Notify registered ProcessMonitors.
     */
    public void notifyMonitors(ProcessInstance processInstance, String event) throws SQLException, DataAccessException {
        // notify registered monitors
        List<ProcessMonitor> monitors = MonitorRegistry.getInstance().getProcessMonitors();
        if (!monitors.isEmpty()) {
            Process processVO = getMainProcessDefinition(processInstance);
            Package pkg = PackageCache.getProcessPackage(processVO.getId());
            Map<String, Object> vars = new HashMap<String, Object>();
            if (processInstance.getVariables() != null) {
                for (VariableInstance var : processInstance.getVariables()) {
                    Object value = var.getData();
                    if (value instanceof DocumentReference) {
                        try {
                            Document docVO = getDocument((DocumentReference) value, false);
                            value = docVO == null ? null : docVO.getObject(var.getType(), pkg);
                        }
                        catch (DataAccessException ex) {
                            logger.severeException(ex.getMessage(), ex);
                        }
                    }
                    vars.put(var.getName(), value);
                }
            }
            ProcessRuntimeContext runtimeContext = new ProcessRuntimeContext(pkg, processVO, processInstance, vars);

            for (ProcessMonitor monitor : monitors) {
                try {
                    if (monitor instanceof OfflineMonitor) {
                        @SuppressWarnings("unchecked")
                        OfflineMonitor<ProcessRuntimeContext> processOfflineMonitor = (OfflineMonitor<ProcessRuntimeContext>) monitor;
                        new OfflineMonitorTrigger<ProcessRuntimeContext>(processOfflineMonitor, runtimeContext).fire(event);
                    }
                    else {
                        if (WorkStatus.LOGMSG_PROC_START.equals(event)) {
                            Map<String, Object> updated = monitor.onStart(runtimeContext);
                            if (updated != null) {
                                for (String varName : updated.keySet()) {
                                    if (processInstance.getVariables() == null)
                                        processInstance.setVariables(new ArrayList<VariableInstance>());
                                    Variable varVO = processVO.getVariable(varName);
                                    if (varVO == null || !varVO.isInput())
                                        throw new ProcessException("Process '" + processVO.getFullLabel() + "' has no such input variable defined: " + varName);
                                    if (processInstance.getVariable(varName) != null)
                                        throw new ProcessException("Process '" + processVO.getFullLabel() + "' input variable already populated: " + varName);
                                    if (VariableTranslator.isDocumentReferenceVariable(runtimeContext.getPackage(), varVO.getVariableType())) {
                                        DocumentReference docRef = createDocument(varVO.getVariableType(), OwnerType.VARIABLE_INSTANCE, new Long(0),
                                                updated.get(varName));
                                        VariableInstance varInst = createVariableInstance(processInstance, varName, docRef);
                                        updateDocumentInfo(docRef, processVO.getVariable(varInst.getName()).getVariableType(), OwnerType.VARIABLE_INSTANCE,
                                                varInst.getInstanceId(), null, null);
                                        processInstance.getVariables().add(varInst);
                                    }
                                    else {
                                        VariableInstance varInst = createVariableInstance(processInstance, varName, updated.get(varName));
                                        processInstance.getVariables().add(varInst);
                                    }
                                }
                            }
                        }
                        else if (WorkStatus.LOGMSG_PROC_COMPLETE.equals(event)) {
                            monitor.onFinish(runtimeContext);
                        }
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
    }

    private DocumentReference createActivityExceptionDocument(ProcessInstance processInst,
            ActivityInstance actInstVO, BaseActivity activityImpl, Throwable th) throws DataAccessException {
        ActivityException actEx;
        if (th instanceof ActivityException) {
            actEx = (ActivityException) th;
        }
        else {
            if (th instanceof MdwException)
                actEx = new ActivityException(((MdwException)th).getCode(), th.toString(), th.getCause());
            else
                actEx = new ActivityException(th.toString(), th.getCause());
            actEx.setStackTrace(th.getStackTrace());
        }

        // populate activity context
        if (actInstVO != null) {
            Process process = getProcessDefinition(processInst);
            Package pkg = getPackage(process);
            if (pkg != null)
                processInst.setPackageName(pkg.getName());
            Activity activity = process.getActivityVO(actInstVO.getActivityId());
            ActivityRuntimeContext runtimeContext = new ActivityRuntimeContext(pkg, process, processInst, activity, actInstVO);
            // TODO option to suppress variables
            for (Variable var : process.getVariables()) {
                try {
                    runtimeContext.getVariables().put(var.getName(), activityImpl.getVariableValue(var.getName()));
                }
                catch (ActivityException ex) {
                    logger.severeException(activityImpl.logtag() + ex.getMessage(), ex);
                }
            }

            actEx.setRuntimeContext(runtimeContext);
        }

        return createDocument(Exception.class.getName(), OwnerType.ACTIVITY_INSTANCE, actInstVO.getId(), actEx);
    }

    private Package getPackage(Process process) {
        if (process.getPackageName() == null)
            return null;
        else
            return PackageCache.getPackage(process.getPackageName());
    }
}
