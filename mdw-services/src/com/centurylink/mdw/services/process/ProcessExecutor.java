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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.transaction.SystemException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.TransactionUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ProcessExecutor {

    private ProcessExecutorImpl engineImpl;

    public ProcessExecutor(EngineDataAccess edao,
            InternalMessenger internalMessenger, boolean forServiceProcess) {
        engineImpl = new ProcessExecutorImpl(edao, internalMessenger, forServiceProcess);
    }

    ProcessExecutor(ProcessExecutorImpl engineImpl) {
        this.engineImpl = engineImpl;
    }

    ///////////////////////////////////////
    // methods about process instances
    ///////////////////////////////////////

    public ProcessInstance createProcessInstance(Long processId, String ownerType,
            Long ownerId, String secondaryOwnerType, Long secondaryOwnerId,
            String masterRequestId, Map<String,String> parameters)
    throws ProcessException, DataAccessException {
        String label = null;
        Process procVO = ProcessCache.getProcess(processId);
        if (procVO != null) {
            label = procVO.getLabel();
            Package pkg = PackageCache.getProcessPackage(processId);
            if (pkg != null && !pkg.isDefaultPackage())
                label = pkg.getLabel() + "/" + label;
        }
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createProcessInstance(processId, ownerType,
                    ownerId, secondaryOwnerType, secondaryOwnerId,
                    masterRequestId, parameters, label);
        } finally {
            stopTransaction(transaction);
        }
    }

    public ProcessInstance getProcessInstance(Long procInstId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getProcessInstance(procInstId);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to load process instance:" + procInstId, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void startProcessInstance(ProcessInstance processInst, int delay)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.startProcessInstance(processInst, delay);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void handleProcessFinish(InternalEvent event)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.handleProcessFinish(event);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws DataAccessException,ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateProcessInstanceStatus(pProcInstId, status);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void abortProcessInstance(InternalEvent event)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.abortProcessInstance(event);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setProcessInstanceCompletionCode(procInstId, completionCode);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to set process instance completion code", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    ///////////////////////////////////////
    // methods about activity instances
    ///////////////////////////////////////

    public ActivityInstance getActivityInstance(Long pActivityInstId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getActivityInstance(pActivityInstId);
        } catch (SQLException e) {
            throw new ProcessException(0, "Failed to get activity instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void executeActivityInstance(BaseActivity activity)
    throws ActivityException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            if (activity.getTimer() != null) {
                activity.executeTimed(this);
            } else {
                activity.execute(this);
            }
        } catch (DataAccessException e) {
            throw new ActivityException(-1, "Failed to execute activity", e);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                throw new ActivityException(-1, "Fail to stop transaction in execute()", e);
            }
        }
    }

    public void failActivityInstance(InternalEvent event,
            ProcessInstance processInst, Long activityId, Long activityInstId,
            BaseActivity activity, Throwable cause) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.failActivityInstance(event,
                    processInst, activityId, activityInstId,
                    activity, cause);
        } catch (DataAccessException e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Exception thrown during failActivityInstance", e);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Fail to stop transaction in failActivityInstance", e);
            }
        }
    }

    public ActivityRuntime prepareActivityInstance(
            InternalEvent event, ProcessInstance procInst)
            throws ProcessException, DataAccessException, ServiceLocatorException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.prepareActivityInstance(event, procInst);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void cancelActivityInstance(
            ActivityInstance actInst, ProcessInstance procinst, String pStatusMsg)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.cancelActivityInstance(actInst, procinst, pStatusMsg);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void holdActivityInstance(ActivityInstance actInst, Long procId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.holdActivityInstance(actInst, procId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public CompletionCode finishActivityInstance(BaseActivity activity,
            ProcessInstance pi, ActivityInstance ai, InternalEvent event, boolean bypassWait)
            throws ProcessException,ActivityException, DataAccessException, ServiceLocatorException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.finishActivityInstance(activity, pi, ai, event, bypassWait);
        } finally {
            stopTransaction(transaction);
        }
    }

    public ActivityRuntime resumeActivityPrepare(ProcessInstance procInst,
            InternalEvent event, boolean resumeOnHold) throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityPrepare(procInst, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean resumeActivityExecute(
            ActivityRuntime ar, InternalEvent event, boolean resumeOnHold)
        throws ActivityException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityExecute(ar, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityFinish(ActivityRuntime ar, boolean finished,
            InternalEvent event, boolean resumeOnHold)
            throws ProcessException,DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityFinish(ar, finished, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityException(ProcessInstance procInst, Long actInstId,
            BaseActivity activity, Throwable cause) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityException(procInst, actInstId, activity, cause);
        } catch (DataAccessException e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Failed to process resume activity exception", e);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Fail to stop transaction in resumeActivityException", e);
            }
        }
    }

    public void setActivityInstanceStatus(ActivityInstance actInst,
            Integer status, String status_message)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setActivityInstanceStatus(actInst, status, status_message);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to set activity instance status", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateActivityInstanceEndTime(Long actInstId, Date endtime)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateActivityInstanceEndTime(actInstId, endtime);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to update activity end time", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().countActivityInstances(procInstId, activityId, statuses);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to count activity instances", e);
        } finally {
            stopTransaction(transaction);
        }
    }


    ///////////////////////////////////////
    // methods about transition instances
    ///////////////////////////////////////

    /**
     * Creates a new instance of the WorkTransationInstance entity
     *
     * @param pWokTransId
     * @param pProcessInstId
     * @return WorkTransitionInstance object
     */
    public TransitionInstance createTransitionInstance(Transition transition, Long pProcessInstId)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createTransitionInstance(transition, pProcessInstId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void createTransitionInstances(ProcessInstance processInstanceVO,
            List<Transition> transitions, Long fromActivityInstanceId)
          throws ProcessException, DataAccessException{
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.createTransitionInstances(processInstanceVO, transitions, fromActivityInstanceId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void determineCompletedTransitions(Long pProcInstId, List<Transition> transitions)
       throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().determineCompletedTransitions(pProcInstId, transitions);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to determine completed transitions", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    ///////////////////////////////////////
    // methods about variable instances and documents
    ///////////////////////////////////////

    public void updateDocumentInfo(DocumentReference docref,
            String documentType, String ownerType, Long ownerId, Integer statusCode, String statusMessage) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentInfo(docref, documentType, ownerType, ownerId, statusCode, statusMessage);
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance createVariableInstance(ProcessInstance pi,
            String varname, Object value)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createVariableInstance(pi, varname, value);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to create variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateVariableInstance(VariableInstance var)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateVariableInstance(var);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to update variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance getVariableInstance(Long procInstId, String varname)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getVariableInstance(procInstId, varname);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to get variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Document getDocument(DocumentReference docref, boolean forUpdate)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDocument(docref, forUpdate);
        } finally {
            stopTransaction(transaction);
        }
    }

    /**
     * Always goes to the database.
     */
    public Document loadDocument(DocumentReference docref, boolean forUpdate)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.loadDocument(docref, forUpdate);
        } finally {
            stopTransaction(transaction);
        }
    }

    public DocumentReference createDocument(String type, String ownerType, Long ownerId, Object doc)
    throws DataAccessException {
        return createDocument(type, ownerType, ownerId, null, null, doc);
    }

    public DocumentReference createDocument(String type, String ownerType, Long ownerId, Integer statusCode, String statusMessage, Object doc)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createDocument(type, ownerType, ownerId, statusCode, statusMessage, doc);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateDocumentContent(DocumentReference docref, Object doc, String type, Package pkg)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentContent(docref, doc, type, pkg);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void addDocumentToCache(Document docvo) {
        if (engineImpl.getDataAccess() instanceof EngineDataAccessCache) {
            ((EngineDataAccessCache)engineImpl.getDataAccess()).addDocumentToCache(docvo);
        }
    }

    ///////////////////////////////////////
    // methods about event instances, event wait instances, event notifications
    ///////////////////////////////////////

    public void cancelEventWaitInstances(Long activityInstanceId)
        throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.cancelEventWaitInstances(activityInstanceId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean deleteInternalEvent(String eventName) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.deleteInternalEvent(eventName);
        } catch (Exception e) {
            throw new DataAccessException(0, "Failed to delete internal event" + eventName, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstance(
            Long actInstId, String pEventName, String compCode,
            boolean pRecurring, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstance(actInstId,
                    pEventName, compCode, pRecurring, notifyIfArrived);
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstances(Long actInstId,
            String[] pEventNames, String[] pWakeUpEventTypes,
            boolean[] pEventOccurances, boolean notifyIfArrived, boolean reregister)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstances(actInstId,
                    pEventNames, pWakeUpEventTypes, pEventOccurances, notifyIfArrived, reregister);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void removeEventWaitForActivityInstance(Long activityInstanceId, String reason)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().removeEventWaitForActivityInstance(activityInstanceId, reason);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to remove event wait for activity instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Integer notifyProcess(String pEventName, Long pEventInstId,
                String message, int delay)
    throws DataAccessException, EventException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.notifyProcess(pEventName, pEventInstId, message, delay);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to notify process of event arrival", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void sendInternalEvent(InternalEvent event)
        throws MdwException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.sendInternalEvent(event);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void sendDelayedInternalEvent(InternalEvent event, int delaySeconds, String msgid, boolean isUpdate)
        throws MdwException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.sendDelayedInternalEvent(event, delaySeconds, msgid, isUpdate);
        } finally {
            stopTransaction(transaction);
        }
    }

    //////////////////////////////////////////////////
    // miscellaneous methods with transaction wrapper
    /////////////////////////////////////////////////////

    public String getSynchronousProcessResponse(Long procInstId, String varname)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getServiceProcessResponse(procInstId, varname);
        } catch (Exception e) {
            throw new DataAccessException(0, "Failed to get value for variable " + varname, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Map<String, String> getOutPutParameters(Long procInstId, Long procId)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getOutputParameters(procInstId, procId);
        } catch (Exception e) {
            throw new DataAccessException(0, "Failed to get output parameters:" + procInstId, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    /**
     * For running your own code inside a transaction.
     * To be used in execute() method when transactions are not turned on globally.
     * @param runnable
     * @throws ProcessException
     * @throws DataAccessException
     */
    public void runInTransaction(Runnable runnable)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            runnable.run();
        } finally {
            stopTransaction(transaction);
        }
    }

    //////////////////////////////////////////////////
    // Methods that are not transaction wrappers
    //////////////////////////////////////////////////

    /**
     * This method must be called within the same transaction scope (namely engine is already started
     * @param actInstId
     * @throws DataAccessException
     */
    public Integer lockActivityInstance(Long actInstId)
       throws DataAccessException {
        try {
            if (!isInTransaction()) throw
                new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockActivityInstance(actInstId);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to lock activity instance", e);
        }
    }

    /**
     * this method must be called within the same transaction scope (namely engine is already started
     * @param actInstId
     * @throws DataAccessException
     */
    public Integer lockProcessInstance(Long procInstId)
       throws DataAccessException {
        try {
            if (!isInTransaction()) throw
                new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockProcessInstance(procInstId);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to lock process instance", e);
        }
    }

    InternalMessenger getInternalMessenger() {
        return engineImpl.getInternalMessenger();
    }

    public int getPerformanceLevel() {
        return engineImpl.getDataAccess().getPerformanceLevel();
    }

    public boolean isInMemory() {
        return engineImpl.isInMemory();
    }

    public boolean isInService() {
        return engineImpl.isInService();
    }

    public DatabaseAccess getDatabaseAccess() throws SQLException {
        DatabaseAccess db = engineImpl.getDatabaseAccess();
        if (db==null) throw new SQLException("Engine database is not open");
        return db;
    }

    public TransactionWrapper startTransaction() throws DataAccessException {
        return engineImpl.getDataAccess().startTransaction();
    }

    public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
        engineImpl.getDataAccess().stopTransaction(transaction);
    }

    private boolean isInTransaction() {
        try {
            return TransactionUtil.getInstance().isInTransaction();
        } catch (SystemException e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Fail to check if inside transaction - treated as not", e);
            return false;
        }
    }

    /**
     * Notify registered ProcessMonitors.
     */
    public void notifyMonitors(ProcessInstance processInstance, String event) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.notifyMonitors(processInstance, event);
        }
        catch (Exception ex) {
            // do not stop processing due to notification failure
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
        }
        finally {
            try {
                stopTransaction(transaction);
            }
            catch (DataAccessException ex) {
                LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            }
        }
    }

}
