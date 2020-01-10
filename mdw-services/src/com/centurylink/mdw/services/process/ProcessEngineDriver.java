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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.activity.types.SuspendableActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.WorkflowException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.TransactionUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessEngineDriver {

    public static final int DEFAULT_PERFORMANCE_LEVEL = 3;

    private static Integer default_performance_level_regular;
    private static Integer default_performance_level_service;
    private static String useTransactionOnExecute = "not_loaded";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private EngineLogger engineLogger;

    private Exception lastException;    // used by service process to throw exception back to event handler
    private Long mainProcessInstanceId;    // used by service process to remember main process instance ID which caller may query
    private int eventConsumeRetrySleep;

    public ProcessEngineDriver() {
        if (default_performance_level_regular == null)
            loadDefaultPerformanceLevel();
        eventConsumeRetrySleep = PropertyManager.getIntegerProperty(PropertyNames.MDW_INTERNAL_EVENT_CONSUME_RETRY_SLEEP, 2);
        engineLogger = new EngineLogger(logger, DEFAULT_PERFORMANCE_LEVEL);  // perf level to be updated in processEvents()
    }

    /**
     * Checks whether the process instance has been canceled or completed
     * @return false if process has been canceled or completed
     */
    private boolean processInstanceIsActive(ProcessInstance processInst) {
        Integer status = processInst.getStatusCode();
        if (WorkStatus.STATUS_CANCELLED.equals(status)) {
            logger.info("ProcessInstance has been cancelled. ProcessInstanceId = " + processInst.getId());
            return false;
        } else if (WorkStatus.STATUS_COMPLETED.equals(status)) {
            logger.info("ProcessInstance has been completed. ProcessInstanceId = " + processInst.getId());
            return false;
        } else return true;
    }

    private String[] getStackTrace() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        String[] ret = new String[stack.length];
        for (int i=0; i<stack.length; i++) {
            ret[i] = stack[i].getClassName() + ":" + stack[i].getMethodName() + " at " +
                stack[i].getFileName() + ", line " + stack[i].getLineNumber();
        }
        return ret;
    }

    private boolean isRecursiveCall(ProcessInstance originatingInstance,
            Process processVO, Long embeddedProcId) {
        if (processVO.getId().equals(originatingInstance.getProcessId())) {
            if (originatingInstance.getOwner().equals(OwnerType.MAIN_PROCESS_INSTANCE)) {
                return embeddedProcId.toString().equals(originatingInstance.getComment());
            } else return false;
        } else return false;
    }

    /**
     * Handles an event for a process or activity
     */
    private void handleInheritedEvent(ProcessExecutor engine, ProcessInstance processInstVO,
            Process processVO, InternalEvent messageDoc, Integer eventType)
      throws ProcessException {
        try {
            if (logger.isInfoEnabled()) {
                if (messageDoc.isProcess())  {
                    // Process SLA (Delay handler)
                    String msg =  "Inherited Event - type=" + eventType + ", compcode=" + messageDoc.getCompletionCode();
                    engineLogger.info(processVO.getId(), processInstVO.getId(), processInstVO.getMasterRequestId(), msg);
                }
                else {
                    String tag = EngineLogger.logtag(processVO.getId(), processInstVO.getId(), messageDoc.getWorkId(), messageDoc.getWorkInstanceId());
                    String msg = "Inherited Event - type=" + eventType + ", compcode=" + messageDoc.getCompletionCode();
                    engineLogger.info(tag, processInstVO.getId(), null, msg);
                }
            }
            engine.notifyMonitors(processInstVO, InternalLogMessage.PROCESS_ERROR);
            String compCode = messageDoc.getCompletionCode();
            ProcessInstance originatingInstance = processInstVO;
            Process embeddedHandlerProc = processVO.findSubprocess(eventType, compCode);
            while (embeddedHandlerProc == null && processInstVO.getOwner().equals(OwnerType.PROCESS_INSTANCE)) {
                processInstVO = engine.getProcessInstance(processInstVO.getOwnerId());
                processVO = getProcessDefinition(processInstVO);
                embeddedHandlerProc = processVO.findSubprocess(eventType, compCode);
            }

            if (embeddedHandlerProc != null) {
                if (isRecursiveCall(originatingInstance, processVO, embeddedHandlerProc.getId())) {
                    logger.warn("Invoking embedded process recursively - not allowed: " + embeddedHandlerProc.getName());
                }
                else {
                    Long secondaryOwnerId = null;
                    String secondaryOwnerType = null;
                    if (!eventType.equals(EventType.ABORT)) {
                        Long secondOwnerId = messageDoc.getWorkInstanceId();
                        if (secondOwnerId != null && secondOwnerId > 0) {
                            if (!messageDoc.isProcess()) {
                                secondaryOwnerId = secondOwnerId;
                                secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
                            }
                            // Update the Process Variable "exception" with the exception handler's triggering Activity exception
                            if (processVO.getVariable("exception") != null &&
                                    messageDoc.getSecondaryOwnerId() != null && messageDoc.getSecondaryOwnerId() > 0) {
                                VariableInstance exceptionVar = processInstVO.getVariable("exception");
                                if (exceptionVar == null)
                                    engine.createVariableInstance(processInstVO, "exception", new DocumentReference(messageDoc.getSecondaryOwnerId()));
                                else
                                    engine.updateVariableInstance(exceptionVar);
                            }
                        }
                        else {
                            if (eventType.equals(EventType.ERROR)) {
                                logger.warn("Creating fallout embedded process without activity instance as secondary owner");
                                logger.warn("--- completion code " + messageDoc.getCompletionCode());
                                logger.warn("--- trans inst ID " + messageDoc.getTransitionInstanceId());
                                logger.warn("--- work ID " + messageDoc.getWorkId());
                                String[] stack = this.getStackTrace();
                                for (int i = 0; i < stack.length; i++) {
                                    logger.warn("--- stack " + i + ": " + stack[i]);
                                }
                            }
                            messageDoc.setSecondaryOwnerType(null);
                        }
                    }
                    else {
                        messageDoc.setSecondaryOwnerType(null);
                    }
                    String ownerType = OwnerType.MAIN_PROCESS_INSTANCE;
                    ProcessInstance procInst = engine.createProcessInstance(
                            embeddedHandlerProc.getId(), ownerType, processInstVO.getId(),
                            secondaryOwnerType, secondaryOwnerId, processInstVO.getMasterRequestId(), null);
                    engine.startProcessInstance(procInst, 0);
                }
            }
            else {
                // try package-level handler
                Process packageHandlerProc = null;
                // TODO ugly test to avoid dup errors for service subproc invokes
                if (messageDoc.getStatusMessage() == null || !messageDoc.getStatusMessage().startsWith("com.centurylink.mdw.activity.ActivityException: At least one subprocess is not completed\n"))
                    packageHandlerProc = getPackageHandler(processInstVO, eventType);
                if (packageHandlerProc != null) {
                    Map<String,String> params = new HashMap<>();
                    Variable exVar = packageHandlerProc.getVariable("exception");
                    if (exVar == null || !exVar.isInput()) {
                        logger.warn("Handler proc " + packageHandlerProc.getQualifiedLabel() + " does not declare input var: 'exception'");
                    }
                    else {
                        params.put("exception", new DocumentReference(messageDoc.getSecondaryOwnerId()).toString());
                    }
                    if (packageHandlerProc.isService()) {
                        invokeService(packageHandlerProc.getId(), OwnerType.ERROR,
                                messageDoc.getSecondaryOwnerId(),
                                originatingInstance.getMasterRequestId(), null, params, null, 0,
                                messageDoc.isProcess() ? OwnerType.PROCESS_INSTANCE : OwnerType.ACTIVITY_INSTANCE, messageDoc.getWorkInstanceId(), null);
                    }
                    else {
                        startProcess(packageHandlerProc.getId(), originatingInstance.getMasterRequestId(), OwnerType.ERROR,
                                messageDoc.getSecondaryOwnerId(), params, messageDoc.isProcess() ? OwnerType.PROCESS_INSTANCE : OwnerType.ACTIVITY_INSTANCE, messageDoc.getWorkInstanceId(), null);
                    }
                }
                else if (eventType.equals(EventType.ABORT)) {
                    // abort the root process instance
                    InternalEvent event = InternalEvent.createProcessAbortMessage(processInstVO.getId());
                    engine.abortProcessInstance(event);
                }
                else {
                    logger.info("Transition has not been defined for event of type " + eventType);
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ProcessException(ex.getMessage());
        }
    }

    /**
     * Finds the relevant package handler for a master process instance.
     */
    private Process getPackageHandler(ProcessInstance masterInstance, Integer eventType) {
        Process process = getProcessDefinition(masterInstance);
        Process handler = getPackageHandler(process.getPackageName(), eventType);
        if (handler != null && handler.getName().equals(process.getName())) {
            logger.warn("Package handler recursion is not allowed. "
                    + "Define an embedded handler in package handler: " + handler.getLabel());
        }
        return handler;
    }

    private Process getPackageHandler(String packageName, Integer eventType) {
        String handlerProcName = EventType.getHandlerName(eventType);
        if (handlerProcName != null) {
            Process packageHandlerProc = null;
            if (PackageCache.getPackage(packageName) != null) {
                packageHandlerProc = ProcessCache.getProcess(packageName + "/" + handlerProcName);
                if (packageHandlerProc == null)  // try lower case
                    packageHandlerProc = ProcessCache.getProcess(packageName + "/" + handlerProcName.toLowerCase());
            }
            if (packageHandlerProc == null && packageName.indexOf('.') > 0) {
                packageHandlerProc = getPackageHandler(packageName.substring(0, packageName.lastIndexOf('.')), eventType);
            }
            return packageHandlerProc;
        }
        return null;
    }

    private void retryActivityStartWhenInstanceExists(ProcessExecutor engine,
            InternalEvent event, ProcessInstance pi) {
        int count = event.getDeliveryCount();
        String av = PropertyManager.getProperty(PropertyNames.MDW_ACTIVITY_ACTIVE_MAX_RETRY);
        int max_retry = 5;
        if (av!=null) {
            // delay some seconds to avoid race condition
            try {
                max_retry = Integer.parseInt(av);
                if (max_retry<0) max_retry = 0;
                else if (max_retry>20) max_retry = 20;
            } catch (Exception e) {
            }
        }
        int initial_delay = 5;
        if (count < max_retry) {
            int delayInSeconds = initial_delay;
            count++;
            event.setDeliveryCount(count);
            for (int i = 0; i < count; i++)
                delayInSeconds = delayInSeconds * 2;
            String tag = EngineLogger.logtag(pi.getProcessId(), pi.getId(), event.getWorkId(), 0L);
            String msg = "Active instance exists, retry in " + delayInSeconds + " seconds";
            engineLogger.info(tag,pi.getId(), null, msg);
            try {
                String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + pi.getId() + "start" + event.getWorkId();
                engine.sendDelayedInternalEvent(event, delayInSeconds, msgid, false);
            } catch (MdwException e) {
                tag = EngineLogger.logtag(pi.getProcessId(), pi.getId(), event.getWorkId(), 0L);
                msg = "Failed to send retry jms message";
                engineLogger.error(tag, pi.getId(), null, msg, new Exception(msg));
            }
        } else {
            // only log exception w/o creating a fall out task. Do we need to?
            String tag = EngineLogger.logtag(pi.getProcessId(),pi.getId(),event.getWorkId(),0L);
            String msg = "Active instance exists - fail after " + max_retry + " retries";
            engineLogger.error(tag, pi.getId(), null, msg, new Exception(msg));
        }
    }

    private void resumeActivity(ProcessExecutor engine, InternalEvent event,
            ProcessInstance procInst, boolean resumeOnHold) {
        Long actInstId = event.getWorkInstanceId();
        ActivityRuntime ar = null;
        try {
            ar = engine.resumeActivityPrepare(procInst, event, resumeOnHold);
            if (ar.getStartCase()!=ActivityRuntime.RESUMECASE_NORMAL) return;
            boolean finished;
            if (ar.getActivity() instanceof SuspendableActivity) {
                if ("true".equalsIgnoreCase(useTransactionOnExecute)) {
                    finished = engine.resumeActivityExecute(ar, event, resumeOnHold);
                } else {
                    if (resumeOnHold) finished = ((SuspendableActivity)ar.activity).resumeWaiting(event);
                    else finished = ((SuspendableActivity)ar.activity).resume(event);
                }
            } else finished = true;
            engine.resumeActivityFinish(ar, finished, event, resumeOnHold);
        } catch (Exception e) {
            logger.error("Resume failed", e);
            lastException = e;
            engine.resumeActivityException(procInst, actInstId,
                    ar==null?null:ar.getActivity(), e);
        }
    }

    private void executeActivity(ProcessExecutor engine, InternalEvent event, ProcessInstance procInst) {
        ActivityRuntime ar = null;
        try {
            // Step 1. check, create and prepare activity instance
            ar = engine.prepareActivityInstance(event, procInst);
            switch (ar.getStartCase()) {
            case ActivityRuntime.STARTCASE_PROCESS_TERMINATED:
                logger.info("ProcessInstance is already terminated. ProcessInstanceId = "
                        + ar.getProcessInstance().getId());
                break;
            case ActivityRuntime.STARTCASE_ERROR_IN_PREPARE:
                // error already reported
                break;
            case ActivityRuntime.STARTCASE_INSTANCE_EXIST:
                retryActivityStartWhenInstanceExists(engine, event, ar.getProcessInstance());
                break;
            case ActivityRuntime.STARTCASE_SYNCH_COMPLETE:
                String msg = "The synchronization activity is already completed";
                engineLogger.info(ar.getProcessInstance().getProcessId(), ar.getProcessInstance().getId(),
                        ar.getActivityInstance().getActivityId(), ar.getActivityInstance().getId(), msg);
                break;
            case ActivityRuntime.STARTCASE_SYNCH_HOLD:
                String message = "The synchronization activity is on-hold - ignore incoming transition";
                engineLogger.info(ar.getProcessInstance().getProcessId(), ar.getProcessInstance().getId(),
                        ar.getActivityInstance().getActivityId(), ar.getActivityInstance().getId(), message);
                break;
            case ActivityRuntime.STARTCASE_SYNCH_WAITING:
                event.setWorkInstanceId(ar.getActivityInstance().getId());
                event.setEventType(EventType.RESUME);
                resumeActivity(engine, event, procInst, false);
                break;
            case ActivityRuntime.STARTCASE_RESUME_WAITING:
                event.setWorkInstanceId(ar.getActivityInstance().getId());
                event.setEventType(EventType.RESUME);
                resumeActivity(engine, event, procInst, true);
                break;
            case ActivityRuntime.STARTCASE_NORMAL:
            default:
                // Step 2. invoke execute() of the activity
                String resCode = ar.activity.notifyMonitors(InternalLogMessage.ACTIVITY_EXECUTE);
                if (resCode == null || resCode.equals("(EXECUTE_ACTIVITY)")) {
                    if (ar.getActivity() instanceof StartActivity) {
                        engine.setProcessInstanceStartTime(ar.getProcessInstance().getId());
                    }
                    // proceed with normal activity execution
                    if ("not_loaded".equals(useTransactionOnExecute)) {
                        useTransactionOnExecute = PropertyManager.getProperty(PropertyNames.MDW_ENGINE_USE_TRANSACTION);
                    }
                    if ("true".equalsIgnoreCase(useTransactionOnExecute))
                        engine.executeActivityInstance(ar.getActivity());
                    else {
                        if (ar.getActivity().getTimer() != null)
                            ar.getActivity().executeTimed(engine);
                        else
                            ar.getActivity().execute(engine);
                    }
                }
                else {
                    // bypass execution due to monitor
                    if (!"null".equals(resCode))
                        ar.getActivity().setReturnCode(resCode);
                    if (ar.getActivity() instanceof SuspendableActivity) {
                        engine.finishActivityInstance(ar.getActivity(), ar.getProcessInstance(), ar.getActivityInstance(), event, true);
                        return;
                    }
                }
                // Step 3. finish activity (complete, suspend or others) or process
                engine.finishActivityInstance(ar.getActivity(),
                        ar.getProcessInstance(), ar.getActivityInstance(), event, false);
                break;

            }
        } catch (Exception ex) {
            lastException = ex;
            engine.failActivityInstance(event, procInst,
                    event.getWorkId(),        // act ID
                    (ar==null || ar.getActivityInstance()==null) ? 0L : ar.getActivityInstance().getId(),
                    ar==null?null:ar.getActivity(), ex);
        }
    }

    private void handleDelay(ProcessExecutor engine, InternalEvent event,
            ProcessInstance processInstance) throws Exception {
        if (!processInstanceIsActive(processInstance)) return;

        ActivityInstance ai = null;
        if (OwnerType.SLA.equals(event.getSecondaryOwnerType())) {
            // new way to handle SLA through JMS message delay rather than timer demon
            Long actInstId = event.getWorkInstanceId();
            ai = engine.getActivityInstance(actInstId);
            if (ai.getStatusCode() != WorkStatus.STATUS_WAITING) {
                // ignore the message when the status is not waiting.
                return;
            }
            String msg = "Activity in waiting status times out";
            engineLogger.info(processInstance.getProcessId(), processInstance.getId(), ai.getActivityId(), actInstId, msg);

            Integer actInstStatus = WorkStatus.STATUS_CANCELLED;
            Process procdef = getProcessDefinition(processInstance);
            Activity activity = procdef.getActivity(ai.getActivityId());
            String status = activity.getAttribute(WorkAttributeConstant.STATUS_AFTER_TIMEOUT);
            if (status!=null) {
                for (int i=0; i<WorkStatus.allStatusNames.length; i++) {
                    if (status.equalsIgnoreCase(WorkStatus.allStatusNames[i])) {
                        actInstStatus = WorkStatus.allStatusCodes[i];
                        break;
                    }
                }
            }
            if (!actInstStatus.equals(WorkStatus.STATUS_WAITING)) {
                // deregister event wait instances when status to be set is COMPLETED or HOLD;
                 engine.cancelEventWaitInstances(ai.getId());
            }

            if (actInstStatus.equals(WorkStatus.STATUS_CANCELLED)) {
                engine.cancelActivityInstance(ai, processInstance, "Cancelled due to time out");
            } else if (actInstStatus.equals(WorkStatus.STATUS_HOLD)) {
                engine.holdActivityInstance(ai, processInstance.getProcessId());
            } // else keep in WAITING status
        }

        Process processVO = getProcessDefinition(processInstance);
        List<Transition> workTransitionVOs = processVO.getTransitions(event.getWorkId(),
                EventType.DELAY, event.getCompletionCode());
        if (workTransitionVOs != null && !workTransitionVOs.isEmpty()) {
            engine.createTransitionInstances(processInstance, workTransitionVOs,
                    event.isProcess()?null:event.getWorkInstanceId());
        } else {
            if (ai != null) {
                // This creates the exception document used by Package level Delay handler
                event.setSecondaryOwnerType(OwnerType.ERROR);
                event.setSecondaryOwnerId(engine.createActivityExceptionDocument(processInstance, ai, null, new ActivityException("Activity timeout")).getDocumentId());
            }
            handleInheritedEvent(engine, processInstance, processVO, event, EventType.DELAY);
        }
    }

    private ProcessInstance findProcessInstance(ProcessExecutor engine,
            InternalEvent event) throws ProcessException, DataAccessException {
        Long procInstId;
        if (event.isProcess()) procInstId = event.getWorkInstanceId();    // can be null or populated for process start message
        else procInstId = event.getOwnerId();
        if (procInstId==null) return null;        // must be process start event
        return engine.getProcessInstance(procInstId);
    }

    /**
     * Executes the flow
     */
    public void processEvents(String msgid, String textMessage) {
        try {
            if (logger.isDebugEnabled())
                logger.debug("executeFlow: " + textMessage);
            InternalEvent event = new InternalEvent(textMessage);

            // a. find the process instance (looking for memory only first, then regular)
            Long procInstId;
            ProcessInstance procInst;
            if (event.isProcess()) {
                if (event.getEventType().equals(EventType.FINISH)) {
                    procInstId = null;    // not needed, and for remote process returns, will not be able to find it
                } else {
                    procInstId = event.getWorkInstanceId();
                }
            } else {
                procInstId = event.getOwnerId();
            }
            if (procInstId != null) {
                EngineDataAccess temp_edao = EngineDataAccessCache.getInstance(false, 9);
                procInst = temp_edao.getProcessInstance(procInstId);
                if (procInst == null) {
                    TransactionWrapper transaction = null;
                    EngineDataAccessDB edbao = new EngineDataAccessDB();
                    try {
                        transaction = edbao.startTransaction();
                        procInst = edbao.getProcessInstance(procInstId);
                    }
                    catch (SQLException ex) {
                        if (("Failed to load process instance: " + procInstId).equals(ex.getMessage())) {
                            if (ApplicationContext.isDevelopment()) {
                                logger.error("Unable to load process instance id=" + procInstId + ".  Was this instance deleted?");
                                return;
                            } else {
                                throw ex;
                            }
                        }
                        throw ex;
                    }
                    finally {
                        edbao.stopTransaction(transaction);
                    }
                }
            } else {
                procInst = null;
            }

            // b. now determine performance level here
            int performance_level;
            if (procInst == null) {        // must be process start message
                if (event.isProcess() && event.getEventType().equals(EventType.START)) {
                    Process procdef = getProcessDefinition(event.getWorkId());
                    if (procdef == null)
                        throw new WorkflowException("Unable to load process id " + event.getWorkId() + " for " + msgid);
                    performance_level = procdef.getPerformanceLevel();
                } else {
                    performance_level = 0;
                }
            } else {
                Process procdef = getProcessDefinition(procInst.getProcessId());
                if (procdef == null) {
                    String msg = "Unable to load process id " + procInst.getProcessId() + " (instance id=" + procInst.getId() + ") for " + msgid;
                    if (ApplicationContext.isDevelopment()) {
                        // referential integrity not always enforced for VCS assets
                        if (PropertyManager.getBooleanProperty(PropertyNames.MDW_INTERNAL_EVENT_DEV_CLEANUP, true)) {
                            logger.error(msg + " (event will be deleted)");
                            EngineDataAccess edao = EngineDataAccessCache.getInstance(false, default_performance_level_regular);
                            InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
                            ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
                            engine.deleteInternalEvent(msgid);
                            return;
                        }
                        else {
                            logger.error(msg);
                        }
                    }
                    else {
                        throw new WorkflowException(msg);
                    }
                }
                performance_level = procdef.getPerformanceLevel();
            }
            if (performance_level <= 0)
                performance_level = default_performance_level_regular;
            engineLogger.setPerformanceLevel(performance_level);

            // c. create engine
            EngineDataAccess edao = EngineDataAccessCache.getInstance(false, performance_level);
            InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
            ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
            if (msgid != null) {
                boolean success = engine.deleteInternalEvent(msgid);
                if (!success) {
                    // retry two times to get around race condition (internal event inserted
                    // into EVENT_INSTANCE table but not committed yet)
                    int retries = 0;
                    while (!success && retries < 2) {
                        logger.debug("Failed to consume internal event " + msgid + " - retry in 2 seconds");
                        Thread.sleep(eventConsumeRetrySleep * 1000L);
                        retries++;
                        success = engine.deleteInternalEvent(msgid);
                    }
                }
                if (!success) {
                    logger.warn("Fail to consume internal event " + msgid + " - assuming the event is already processed by another thread");
                    return;    // already processed;
                }
            }
            if (performance_level >= 9)
                msgBroker.setCacheOption(InternalMessenger.CACHE_ONLY);
            else if (performance_level >= 3)
                msgBroker.setCacheOption(InternalMessenger.CACHE_ON);
            // d. process event(s)
            if (performance_level >= 3) {
                // TODO cache proc inst
                processEvent(engine, event, procInst);
                while ((event = msgBroker.getNextMessageFromQueue(engine)) != null) {
                    procInst = this.findProcessInstance(engine, event);
                    processEvent(engine, event, procInst);
                }
            } else {
                processEvent(engine, event, procInst);
            }
        } catch (XmlException e) {
            logger.error("Unparsable xml message: " + textMessage, e);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void processEvent(ProcessExecutor engine, InternalEvent event, ProcessInstance procInst) {
        try {
            if (event.isProcess()) {
                if (event.getEventType().equals(EventType.START)) {
                    if (procInst == null) {
                        procInst = engine.createProcessInstance(
                                event.getWorkId(), event.getOwnerType(), event.getOwnerId(),
                                event.getSecondaryOwnerType(), event.getSecondaryOwnerId(),
                                event.getMasterRequestId(), event.getParameters());
                    }
                    engine.startProcessInstance(procInst, 0);
                } else if (event.getEventType().equals(EventType.FINISH)) {
                    // do not check status - process is already in completed status
                    engine.handleProcessFinish(event);
                } else if (event.getEventType().equals(EventType.ABORT)) {
                    if (!processInstanceIsActive(procInst)) return;
                    engine.abortProcessInstance(event);
                } else if (event.getEventType().equals(EventType.DELAY)) {
                    if (!processInstanceIsActive(procInst)) return;
                    event.setSecondaryOwnerType(OwnerType.ERROR);
                    event.setSecondaryOwnerId(engine.createProcessExceptionDocument(procInst, new ProcessException("Process SLA timeout")).getDocumentId());
                    handleInheritedEvent(engine, procInst, getProcessDefinition(procInst), event, EventType.DELAY);
                }
            } else {
                if (!processInstanceIsActive(procInst))
                    return;
                if (event.getEventType().equals(EventType.START)) {
                    this.executeActivity(engine, event, procInst);
                } else if (event.getEventType().equals(EventType.RESUME)) {
                    resumeActivity(engine, event, procInst, false);
                } else if (event.getEventType().equals(EventType.DELAY)) {
                    handleDelay(engine, event, procInst);
                } else {
                    Process processVO = getProcessDefinition(procInst);
                    procInst.setProcessName(processVO.getName());
                    List<Transition> workTransitionVOs = processVO.getTransitions(event.getWorkId(),
                            event.getEventType(), event.getCompletionCode());
                    if (workTransitionVOs != null && !workTransitionVOs.isEmpty()) {
                        engine.createTransitionInstances(procInst, workTransitionVOs,
                                event.isProcess()?null:event.getWorkInstanceId());
                    } else if (event.getEventType().equals(EventType.FINISH)) {
                            // do nothing
                    } else if (event.getEventType().equals(EventType.ERROR)) {
                        if (!processVO.isEmbeddedExceptionHandler()) {
                            engine.updateProcessInstanceStatus(procInst.getId(), WorkStatus.STATUS_WAITING);
                            handleInheritedEvent(engine, procInst, processVO, event, EventType.ERROR);
                        } else {
                            logger.info("Error occurred inside an error handler!!!");
                        }
                    } else if (event.getEventType().equals(EventType.CORRECT)) {
                        handleInheritedEvent(engine, procInst, processVO, event, EventType.CORRECT);
                    } else if (event.getEventType().equals(EventType.ABORT)) {
                        handleInheritedEvent(engine, procInst, processVO, event, EventType.ABORT);
                    }
                }
            }
        } catch (Throwable ex) {
            logger.error("Fatal exception in executeFlow - cannot generate fallout task", ex);
        }
        finally {
            // Check for any non-stopped transactions (i.e. locked "for update" document rows)
            TransactionUtil transUtil = TransactionUtil.getInstance();
            if (transUtil.getTransaction() != null) {
                try {
                    TransactionWrapper transaction = new TransactionWrapper();
                    engine.stopTransaction(transaction);  // This will stop transaction and close DB connection
                }
                catch (Throwable ex) {
                    logger.error("Fatal exception stopping transaction - cannot close DB connection and stop transaction", ex);
                }
            }
        }
    }

    private void addDocumentToCache(ProcessExecutor engine, Long docid, String type, String content) {
        if (content != null) {
            if (docid == 0L) {
                try {
                    engine.createDocument(type, OwnerType.LISTENER_REQUEST, 0L, content);
                } catch (DataAccessException e) {
                    // should never happen, as this is cache only
                }
            } else {
                Document docvo = new Document();
                docvo.setContent(content);
                docvo.setDocumentId(docid);
                docvo.setDocumentType(type);
                engine.addDocumentToCache(docvo);
            }
        }
    }

    /**
     * Invoke a real-time service process.
     * @return the service response
     */
    public String invokeService(Long processId, String ownerType,
            Long ownerId, String masterRequestId, String masterRequest,
            Map<String,String> parameters, String responseVarName, Map<String,String> headers) throws Exception {
        return invokeService(processId, ownerType, ownerId, masterRequestId, masterRequest, parameters, responseVarName, 0, null, null, headers);
    }

    /**
     * Invoke a service (synchronous) process. The method cannot be used
     * if the process is not a service process.
     * Performance level:
     *    0 - to be determined by global property or process attribute, which will set the level to one of the following
     *    9 - all cache options CACHE_ONLY
     *    5 - CACHE_OFF for activity/transition, CACHE_ONLY for variable/document, CACHE_ON for internal event queue
     *    3 - CACHE_OFF for activity/transition, CACHE_ON for variable/document, CACHE_ON for internal event queue
     *    1 - CACHE_OFF for activity/transition, CACHE_OFF for variable/document, CACHE_OFF for internal event queue
     *
     * @param processId ID of the process definition
     * @param ownerType Owner of the Service Process - DOCUMENT or PROCESS_INSTANCE
     * @param ownerId owner ID of the request event
     * @param masterRequestId master request ID
     * @param masterRequest content of the request event
     * @param parameters Input parameter bindings for the process instance to be created
     * @param responseVarName the name of the variable where the response is to be obtained.
     *         If you leave this null, the response will be taken from "response"
     *         if one is defined, and null otherwise
     * @param performance_level the performance level to be used to run the process.
     *         When a 0 is passed in, the default performance level for service processes will be used,
     *         unless the performance level attribute is configured for the process.
     * @return response message, which is obtained from the variable named ie responseVarName
     *      of the process.
     */
    public String invokeService(Long processId, String ownerType,
            Long ownerId, String masterRequestId, String masterRequest,
            Map<String,String> parameters, String responseVarName, int performance_level,
            String secondaryOwnerType, Long secondaryOwnerId, Map<String,String> headers) throws Exception {
        Process process = getProcessDefinition(processId);
        Package pkg = getPackage(process);
        long startMilli = System.currentTimeMillis();
        if (performance_level <= 0)
            performance_level = process.getPerformanceLevel();
        if (performance_level <= 0)
            performance_level = default_performance_level_service;
        EngineDataAccess edao = EngineDataAccessCache.getInstance(true, performance_level);
        InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        msgBroker.setCacheOption(InternalMessenger.CACHE_ONLY);
        ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, true);
        engineLogger.setPerformanceLevel(performance_level);
        if (performance_level >= 5) {
            if (OwnerType.DOCUMENT.equals(ownerType))
                addDocumentToCache(engine, ownerId, XmlObject.class.getName(), masterRequest);
            if (parameters != null) {
                for (String key : parameters.keySet()) {
                    String value = parameters.get(key);
                    if (value != null && value.startsWith("DOCUMENT:")) {
                        DocumentReference docRef = new DocumentReference(parameters.get(key));
                        if (!docRef.getDocumentId().equals(ownerId)) {
                            Document docVO = engine.loadDocument(docRef, false);
                            if (docVO != null) {
                                String docContent = docVO.getContent(pkg);
                                if (docContent != null)
                                    addDocumentToCache(engine, docRef.getDocumentId(), docVO.getDocumentType(), docContent);
                            }
                        }
                    }
                }
            }
        }
        ProcessInstance mainProcessInst = executeServiceProcess(engine, processId,
                ownerType, ownerId, masterRequestId, parameters, secondaryOwnerType, secondaryOwnerId, headers);
        boolean completed = mainProcessInst.getStatusCode().equals(WorkStatus.STATUS_COMPLETED);
        if (headers != null)
            headers.put(Listener.METAINFO_MDW_PROCESS_INSTANCE_ID, mainProcessInst.getId().toString());
        String resp = completed ? engine.getSynchronousProcessResponse(mainProcessInst.getId(), responseVarName):null;
        long stopMilli = System.currentTimeMillis();
        logger.info("Synchronous process executed in " +
                ((stopMilli-startMilli)/1000.0) + " seconds at performance level " + performance_level);
        if (completed)
            return resp;
        if (lastException == null)
            throw new Exception("Process instance not completed");
        throw lastException;
    }

    /**
     * Called internally by invoke subprocess activities to call service processes as
     * subprocesses of regular processes.
     * @return map of output parameters (can be empty hash, but not null);
     */
    public Map<String,String> invokeServiceAsSubprocess(Long processId,
            Long parentProcInstId, String masterRequestId, Map<String,String> parameters,
            int performance_level) throws Exception
    {
        long startMilli = System.currentTimeMillis();
        if (performance_level <= 0)
            performance_level = getProcessDefinition(processId).getPerformanceLevel();
        if (performance_level <=0 )
            performance_level = default_performance_level_service;
        EngineDataAccess edao = EngineDataAccessCache.getInstance(true, performance_level);
        engineLogger.setPerformanceLevel(performance_level);
        InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        msgBroker.setCacheOption(InternalMessenger.CACHE_ONLY);
        ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, true);
        ProcessInstance mainProcessInst = executeServiceProcess(engine, processId,
                OwnerType.PROCESS_INSTANCE, parentProcInstId, masterRequestId, parameters, null, null, null);
           boolean completed = mainProcessInst.getStatusCode().equals(WorkStatus.STATUS_COMPLETED);
        Map<String,String> resp = completed?engine.getOutPutParameters(mainProcessInst.getId(), processId):null;
        long stopMilli = System.currentTimeMillis();
        logger.info("Synchronous process executed in " +
                ((stopMilli-startMilli)/1000.0) + " seconds at performance level " + performance_level);
        if (completed)
            return resp;
        if (lastException == null)
            throw new Exception("Process instance not completed");
        throw lastException;
    }

    /**
     * execute service process using asynch engine
     */
    private ProcessInstance executeServiceProcess(ProcessExecutor engine, Long processId,
            String ownerType, Long ownerId, String masterRequestId, Map<String,String> parameters,
            String secondaryOwnerType, Long secondaryOwnerId, Map<String,String> headers) throws Exception {
        Process procdef = getProcessDefinition(processId);
        Long startActivityId = procdef.getStartActivity().getId();
        if (masterRequestId == null)
            masterRequestId = genMasterRequestId();
        ProcessInstance mainProcessInst = engine.createProcessInstance(
                processId, ownerType, ownerId, secondaryOwnerType, secondaryOwnerId,
                masterRequestId, parameters);
        mainProcessInstanceId = mainProcessInst.getId();
        engine.updateProcessInstanceStatus(mainProcessInst.getId(), WorkStatus.STATUS_PENDING_PROCESS);
        if (OwnerType.DOCUMENT.equals(ownerType) && ownerId != 0L) {
            setOwnerDocumentProcessInstanceId(engine, ownerId, mainProcessInst.getId(), masterRequestId);
            bindRequestVariable(procdef, ownerId, engine, mainProcessInst);
        }
        if (headers != null) {
            bindRequestHeadersVariable(procdef, headers, engine, mainProcessInst);
        }
        String msg = InternalLogMessage.PROCESS_START.message + " - " + procdef.getQualifiedName() + "/" + procdef.getVersionString();
        engineLogger.info(processId, mainProcessInst.getId(), masterRequestId, msg);
        engine.notifyMonitors(mainProcessInst, InternalLogMessage.PROCESS_START);
        // setProcessInstanceStatus will really set to STATUS_IN_PROGRESS - hint to set START_DT as well
        InternalEvent event = InternalEvent.createActivityStartMessage(startActivityId,
                mainProcessInst.getId(), 0L, masterRequestId, EventType.EVENTNAME_START);
        InternalMessenger msgBroker = engine.getInternalMessenger();
        lastException = null;
        processEvent(engine, event, mainProcessInst);
        while ((event = msgBroker.getNextMessageFromQueue(engine)) != null) {
            ProcessInstance procInst = this.findProcessInstance(engine, event);
            processEvent(engine, event, procInst);
        }
        mainProcessInst = engine.getProcessInstance(mainProcessInst.getId());
        return mainProcessInst;
    }

    public Long getMainProcessInstanceId() {
        return mainProcessInstanceId;
    }

    private void setOwnerDocumentProcessInstanceId(ProcessExecutor engine,
            Long msgDocId, Long procInstId, String masterRequestId) {
        // update document's OWNER_ID with the processInstanceId (OWNER_TYPE will stay LISTENER_REQUEST)
        try {
            if (msgDocId != 0L)
                engine.updateDocumentInfo(new DocumentReference(msgDocId),
                        null, null, procInstId, null, null);
        } catch (Exception e) {
            // this is possible for race condition - document was just created
            logger.warn("Failed to update document for process instance id");
        }
    }

    private void bindRequestVariable(Process procdef,
            Long reqdocId, ProcessExecutor engine, ProcessInstance pi)
    throws DataAccessException {
        Variable requestVO = procdef.getVariable(VariableConstants.REQUEST);
        if (requestVO==null) return;
        int cat = requestVO.getVariableCategory();
        String vartype = requestVO.getType();
        if (cat != Variable.CAT_INPUT && cat != Variable.CAT_INOUT)
            return;
        if (!VariableTranslator.isDocumentReferenceVariable(getPackage(procdef), vartype))
            return;
        List<VariableInstance> viList = pi.getVariables();
        if (viList != null) {
            for (VariableInstance vi : viList) {
                if (vi.getName().equals(VariableConstants.REQUEST))
                    return;
            }
        }
        DocumentReference docref = new DocumentReference(reqdocId);
        engine.createVariableInstance(pi, VariableConstants.REQUEST, docref);
    }

    private void bindRequestHeadersVariable(Process procdef, Map<String,String> headers,
            ProcessExecutor engine, ProcessInstance pi) throws DataAccessException {
        Variable headersVO = procdef.getVariable(VariableConstants.REQUEST_HEADERS);
        if (headersVO == null)
            return;
        int cat = headersVO.getVariableCategory();
        String vartype = headersVO.getType();
        if (cat != Variable.CAT_INPUT && cat != Variable.CAT_INOUT)
            return;
        List<VariableInstance> viList = pi.getVariables();
        if (viList != null) {
            for (VariableInstance vi : viList) {
                if (vi.getName().equals(VariableConstants.REQUEST_HEADERS))
                    return;
            }
        }

        if (vartype.equals(Map.class.getName())) {
            engine.createVariableInstance(pi, VariableConstants.REQUEST_HEADERS, headers);
        }
        else if (vartype.equals("java.util.Map<String,String>") || vartype.equals(Object.class.getName())) {
            DocumentReference docRef = engine.createDocument(vartype, OwnerType.VARIABLE_INSTANCE, 0L, headers);
            VariableInstance varInst = engine.createVariableInstance(pi, VariableConstants.REQUEST_HEADERS, docRef);
            engine.updateDocumentInfo(docRef, null, null, varInst.getInstanceId(), null, null);
        }
        else {
            logger.info("Implicit requestHeaders supports variable types " + Map.class.getName() + " or " + Object.class.getName());
        }
    }

    /**
     * Start a process.
     * @param processId
     * @param masterRequestId
     * @param ownerType
     * @param ownerId
     * @param vars Input parameter bindings for the process instance to be created
     * @param headers
     * @return the process instance ID
     */
    public Long startProcess(Long processId, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> vars, Map<String,String> headers) throws Exception {
        return startProcess(processId, masterRequestId, ownerType, ownerId, vars, null, null, headers);
    }

    /**
     * Starting a regular process.
     * @param processId ID of the process to be started
     * @param masterRequestId
     * @param ownerType
     * @param ownerId
     * @param vars Input parameter bindings for the process instance to be created
     * @return Process instance ID
     */
    public Long startProcess(Long processId, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> vars,
            String secondaryOwnerType, Long secondaryOwnerId, Map<String,String> headers) throws Exception {
        Process procdef = getProcessDefinition(processId);
        int performance_level = procdef.getPerformanceLevel();
        if (performance_level <= 0)
            performance_level = default_performance_level_regular;
        EngineDataAccess edao = EngineDataAccessCache.getInstance(false, performance_level);
        engineLogger.setPerformanceLevel(performance_level);
        InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        // do not set internal messenger with cache options, as this engine does not process it directly - Unless PL 9
        if (performance_level >= 9)
            msgBroker.setCacheOption(InternalMessenger.CACHE_ONLY);
        if (masterRequestId == null)
            masterRequestId = genMasterRequestId();
        ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
        ProcessInstance processInst = engine.createProcessInstance(processId,
                ownerType, ownerId, secondaryOwnerType, secondaryOwnerId,
                masterRequestId, vars);
        if (ownerType.equals(OwnerType.DOCUMENT) && ownerId != 0L) {
            setOwnerDocumentProcessInstanceId(engine, ownerId, processInst.getId(), masterRequestId);
            bindRequestVariable(procdef, ownerId, engine, processInst);
        }
        if (headers != null) {
            bindRequestHeadersVariable(procdef, headers, engine, processInst);
        }
        // Delay for ensuring document document content is available for the processing thread
        // It is also needed to ensure the message is really sent, instead of cached
        int delay = PropertyManager.getIntegerProperty(PropertyNames.MDW_PROCESS_LAUNCH_DELAY, 2);
        engine.startProcessInstance(processInst, delay);
        return processInst.getId();
    }

    private void loadDefaultPerformanceLevel() {
        String pv = PropertyManager.getProperty(PropertyNames.MDW_PERFORMANCE_LEVEL_REGULAR);
        if (pv != null)
            default_performance_level_regular = Integer.parseInt(pv);
        else
            default_performance_level_regular = DEFAULT_PERFORMANCE_LEVEL;
        pv = PropertyManager.getProperty(PropertyNames.MDW_PERFORMANCE_LEVEL_SERVICE);
        if (pv != null)
            default_performance_level_service = Integer.parseInt(pv);
        else
            default_performance_level_service = DEFAULT_PERFORMANCE_LEVEL;
    }

    private Process getProcessDefinition(Long processId) {
        return ProcessCache.getProcess(processId);
    }

    private Process getProcessDefinition(ProcessInstance procinst) {
        Process procdef = null;
        if (procinst.getProcessInstDefId() > 0L)
            procdef = ProcessCache.getProcessInstanceDefiniton(procinst.getProcessId(), procinst.getProcessInstDefId());
        if (procdef == null)
            procdef = ProcessCache.getProcess(procinst.getProcessId());
        if (procinst.isEmbedded())
            procdef = procdef.getSubProcess(new Long(procinst.getComment()));
        return procdef;
    }

    private Package getPackage(String packageName) {
        return PackageCache.getPackage(packageName);
    }

    private Package getPackage(Process process) {
        if (process.getPackageName() == null)
            return null;
        else
            return getPackage(process.getPackageName());
    }

    private String genMasterRequestId() {
        return Long.toHexString(System.nanoTime());
    }
}