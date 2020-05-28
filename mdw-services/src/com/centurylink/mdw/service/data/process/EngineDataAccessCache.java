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
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.SQLException;
import java.util.*;

public class EngineDataAccessCache implements EngineDataAccess {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static EngineDataAccessCache memoryOnlyInstance;    // for regular processes only

    private static final int CACHE_OFF = 0;
    private static final int CACHE_ON = 1;
    private static final int CACHE_ONLY = 2;

    protected EngineDataAccessDB edadb;

    private Map<Long,ProcessInstance> procInstCache;      // used when persist_variable is false
    private Map<Long,Document> documentCache;                // for local document
    private Map<Long,ActivityInstance> activityInstCache;
    private Map<String,EventInstance> eventInstCache;

    private int performanceLevel;
    private int cacheProcess;
    private int cacheActivityTransition;        // only support CACHE_OFF and CACHE_ONLY
    private int cacheVariable;
    private int cacheDocument;
    private int cacheEvent;                    // only support CACHE_OFF and CACHE_ONLY

    private static long next_id = 1000;

    private EngineDataAccessCache(boolean forServiceProcess, int performanceLevel) {
        this.performanceLevel = performanceLevel;
        setCacheFlags(forServiceProcess, performanceLevel);
        if (cacheProcess != CACHE_OFF)
            procInstCache = new HashMap<>();
        if (cacheDocument != CACHE_OFF)
            documentCache = new HashMap<>();
        if (cacheActivityTransition == CACHE_ONLY)
            activityInstCache = new HashMap<>();
        if (cacheEvent == CACHE_ONLY)
            eventInstCache = new HashMap<>();
        if (cacheProcess != CACHE_ONLY || cacheDocument != CACHE_ONLY && cacheEvent != CACHE_ONLY)
            edadb = new EngineDataAccessDB();
    }

    public DatabaseAccess getDatabaseAccess() {
        return edadb == null ? null : edadb.getDatabaseAccess();
    }

    /**
     * Returns null for perf level 9.
     */
    public DocumentDbAccess getDocumentDbAccess() {
        return edadb == null ? null : edadb.getDocumentDbAccess();
    }

    private Long getNextInternalId() {
        return next_id++;
    }

    public int getPerformanceLevel() {
        return performanceLevel;
    }

    public Document loadDocument(Long documentId, boolean forUpdate) throws DataAccessException, SQLException {
        Document upToDateDoc = null;
        if (edadb == null)  { // is the case for CACHE_ONLY
            EngineDataAccessDB db = new EngineDataAccessDB();
            TransactionWrapper tw = null;
            try {
                tw = db.startTransaction();
                upToDateDoc = db.loadDocument(documentId, forUpdate);
            }
            finally {
                if (tw != null)
                    db.stopTransaction(tw);
            }
        }
        else {
            upToDateDoc = edadb.getDocument(documentId, forUpdate);
        }
        // Also update the cache
        if (upToDateDoc!=null)
            documentCache.put(documentId, upToDateDoc);

        return upToDateDoc;
    }

    public synchronized Document getDocument(Long documentId, boolean forUpdate) throws SQLException {
        Document docvo = null;
        if (cacheDocument ==CACHE_OFF) {
            docvo = edadb.getDocument(documentId, forUpdate);
        } else if (cacheDocument ==CACHE_ONLY) {
            docvo = documentCache.get(documentId);
            if (docvo == null) {  // Could be a pass-by-reference document from non-cache-only parent process
                try {
                    docvo = loadDocument(documentId, forUpdate);
                }
                catch (DataAccessException e) {
                    throw new SQLException(e);
                }
            }
        } else {
            if (forUpdate) {
                docvo = edadb.getDocument(documentId, true);
                if (docvo!=null) documentCache.put(documentId, docvo);
            } else {
                docvo = documentCache.get(documentId);
                if (docvo==null) {
                    docvo = edadb.getDocument(documentId, forUpdate);
                    if (docvo!=null) documentCache.put(documentId, docvo);
                }
            }
        }
        return docvo;
    }

    /**
     * Update the content (actual document object) bound to the given
     * document reference object.
     *
     * @param docid
     * @param content
     * @throws DataAccessException
     */
    public synchronized void updateDocumentContent(Long docid, String content) throws SQLException {
        if (cacheDocument ==CACHE_OFF) {
            edadb.updateDocumentContent(docid, content);
        } else if (cacheDocument ==CACHE_ONLY) {
            Document docvo = documentCache.get(docid);
            if (docvo!=null) docvo.setContent(content);
        } else {
            edadb.updateDocumentContent(docid, content);
            Document docvo = documentCache.get(docid);
            if (docvo!=null) docvo.setContent(content);
        }
    }

    /**
     * Update document information (everything but document content itself).
     * The method will update only the arguments that have non-null values.
     * @param docvo
     * @throws SQLException
     */
    public synchronized void updateDocumentInfo(Document docvo) throws SQLException {
        if (cacheDocument ==CACHE_OFF) {
            edadb.updateDocumentInfo(docvo);
        } else if (cacheDocument ==CACHE_ONLY) {
            Document docvo0 = documentCache.get(docvo.getDocumentId());
            docvo0.setDocumentType(docvo.getDocumentType());
            docvo0.setOwnerId(docvo.getOwnerId());
            docvo0.setOwnerType(docvo.getOwnerType());
        } else {
            edadb.updateDocumentInfo(docvo);
            Document docvo0 = documentCache.get(docvo.getDocumentId());
            if (docvo0!=docvo) {
                docvo0.setDocumentType(docvo.getDocumentType());
                docvo0.setOwnerId(docvo.getOwnerId());
                docvo0.setOwnerType(docvo.getOwnerType());
            }
        }
    }

    public synchronized ProcessInstance getProcessInstance(Long processInstId)
            throws SQLException {
        ProcessInstance pi = null;
        if (cacheProcess ==CACHE_OFF) {
            pi = edadb.getProcessInstance(processInstId);
        } else if (cacheProcess ==CACHE_ONLY) {
            pi = procInstCache.get(processInstId);
        } else {
            pi = procInstCache.get(processInstId);
            if (pi==null) {
                pi = edadb.getProcessInstance(processInstId);
                if (pi!=null) procInstCache.put(processInstId, pi);
            }
        }
        return pi;
    }

    public synchronized Long createProcessInstance(ProcessInstance procinst) throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            edadb.createProcessInstance(procinst);
        } else if (cacheProcess ==CACHE_ONLY) {
            procinst.setId(getNextInternalId());
            procInstCache.put(procinst.getId(), procinst);
        } else {
            edadb.createProcessInstance(procinst);
            procInstCache.put(procinst.getId(), procinst);
        }
        return procinst.getId();
    }


    public synchronized Long createVariableInstance(VariableInstance var, Long processInstId)
            throws SQLException {
        if (cacheVariable ==CACHE_OFF) {
            edadb.createVariableInstance(var, processInstId);
        } else if (cacheVariable ==CACHE_ONLY) {
            var.setInstanceId(getNextInternalId());
            ProcessInstance pi = procInstCache.get(processInstId);
            VariableInstance var0 = pi.getVariable(var.getName());
            if (var0==null) pi.getVariables().add(var);
        } else {
            edadb.createVariableInstance(var, processInstId);
            ProcessInstance pi = procInstCache.get(processInstId);
            if (pi!=null) {
                List<VariableInstance> vars = pi.getVariables();
                if (vars==null) {
                    // this is possible only when the process instance
                    // is not currently loaded, such as when called in setParameterValue/3
                    vars = new ArrayList<VariableInstance>();
                    vars.add(var);
                } else {
                    VariableInstance var0 = pi.getVariable(var.getName());
                    if (var0==null) vars.add(var);
                }
            }
        }
        return var.getInstanceId();
    }

    public synchronized void updateVariableInstance(VariableInstance var) throws SQLException {
        if (cacheVariable ==CACHE_OFF) {
            edadb.updateVariableInstance(var);
        } else if (cacheVariable ==CACHE_ONLY) {
            // updated already
        } else {
            edadb.updateVariableInstance(var);
        }
    }

    public synchronized Long createDocument(Document docvo) throws SQLException {
        return createDocument(docvo, null);
    }

    public synchronized Long createDocument(Document docvo, Package pkg) throws SQLException {
        if (cacheDocument ==CACHE_OFF) {
            edadb.createDocument(docvo, pkg);
        } else if (cacheDocument ==CACHE_ONLY) {
            docvo.setDocumentId(getNextInternalId());
            documentCache.put(docvo.getDocumentId(), docvo);
        } else {
            edadb.createDocument(docvo);
            documentCache.put(docvo.getDocumentId(), docvo);
        }
        return docvo.getDocumentId();
    }

    public void addDocumentToCache(Document docvo) {
        documentCache.put(docvo.getDocumentId(), docvo);
    }

    public synchronized Long createTransitionInstance(TransitionInstance vo) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            vo.setTransitionInstanceID(getNextInternalId());
        } else {
            edadb.createTransitionInstance(vo);
        }
        return vo.getTransitionInstanceID();
    }

    public synchronized void completeTransitionInstance(Long transInstId, Long actInstId) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            // no cache for transition instance
        } else {
            edadb.completeTransitionInstance(transInstId, actInstId);
        }
    }

    public synchronized Long createActivityInstance(ActivityInstance vo) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            vo.setId(getNextInternalId());
            activityInstCache.put(vo.getId(), vo);
        } else {
            edadb.createActivityInstance(vo);
        }
        return vo.getId();
    }

    // was WorkManager.failActivityInstance and WorkManager.completeActivityInstance
    // used to append status message when failure only, and override when success.
    // changed to append in all cases
    public synchronized void setActivityInstanceStatus(ActivityInstance actInst,
            Integer status, String statusMessage)
            throws SQLException {
        if (cacheActivityTransition == CACHE_ONLY) {
            actInst.setStatusCode(status);
            actInst.setMessage(statusMessage);
        } else {
            edadb.setActivityInstanceStatus(actInst, status, statusMessage);
        }
    }

    public synchronized void setProcessInstanceStatus(Long processInstId, Integer status) throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            edadb.setProcessInstanceStatus(processInstId, status);
        } else if (cacheProcess ==CACHE_ONLY) {
            ProcessInstance pi = procInstCache.get(processInstId);
            if (status.equals(WorkStatus.STATUS_PENDING_PROCESS)) {
                status = WorkStatus.STATUS_IN_PROGRESS;
            }
            pi.setStatusCode(status);
        } else {
            edadb.setProcessInstanceStatus(processInstId, status);
            if (status.equals(WorkStatus.STATUS_PENDING_PROCESS))
                status = WorkStatus.STATUS_IN_PROGRESS;
            ProcessInstance pi = procInstCache.get(processInstId);
            if (pi!=null) pi.setStatusCode(status);
        }
    }

    public synchronized void setProcessCompletionTime(ProcessInstance pi) throws SQLException {
        if (cacheProcess == CACHE_OFF) {
            edadb.setProcessCompletionTime(pi);
        }
        else if (cacheProcess == CACHE_ONLY) {
            ProcessInstance cachepi = procInstCache.get(pi.getId());
            cachepi.setCompletionTime(pi.getCompletionTime());
        }
        else {
            edadb.setProcessCompletionTime(pi);
            ProcessInstance cachepi = procInstCache.get(pi.getId());
            if (cachepi != null)
                cachepi.setCompletionTime(pi.getCompletionTime());
        }
    }

    public synchronized void setActivityCompletionTime(ActivityInstance ai) throws SQLException {
        if (cacheProcess != CACHE_ONLY) {
            edadb.setActivityCompletionTime(ai);
        }
    }

    public synchronized void cancelTransitionInstances(Long procInstId, String comment,
            Long transId) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            // TODO what to do with this?
        } else {
            edadb.cancelTransitionInstances(procInstId, comment, transId);
        }
    }

    public synchronized int countTransitionInstances(Long pProcInstId, Long pWorkTransId)
            throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            return 0;    // TODO check if this is okay
        } else {
            return edadb.countTransitionInstances(pProcInstId, pWorkTransId);
        }
    }

    public int countActivityInstances(Long procInstId, Long activityId)
            throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            int count = 0;
            List<ActivityInstance> actInstances = new ArrayList<>();
            for (ActivityInstance actInst : activityInstCache.values()) {
                if (!actInst.getActivityId().equals(activityId)) continue;
                if (!actInst.getProcessInstanceId().equals(procInstId)) continue;
                actInstances.add(actInst);
            }
            Collections.sort(actInstances, (ActivityInstance o1, ActivityInstance o2) ->
                    o1.getId().compareTo(o2.getId()));
            for (ActivityInstance actInst : actInstances) {
                if (actInst.getStatusCode() == WorkStatus.STATUS_FAILED)
                    count++;
                else if (actInst.getStatusCode() == WorkStatus.STATUS_COMPLETED)
                    count = 0;
            }
            return count;
        } else {
            return edadb.countActivityInstances(procInstId, activityId);
        }
    }

    public synchronized void determineCompletedTransitions(Long pProcInstId,
            List<Transition> transitions) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            // TODO - how to implement this?
        } else {
            edadb.determineCompletedTransitions(pProcInstId, transitions);
        }
    }

    public synchronized ActivityInstance getActivityInstance(Long actInstId)
            throws DataAccessException, SQLException {
        ActivityInstance actInst;
        if (cacheActivityTransition ==CACHE_ONLY) {
            actInst = activityInstCache.get(actInstId);
        } else {
            actInst = edadb.getActivityInstance(actInstId);
        }
        return actInst;
    }

    public synchronized List<ActivityInstance> getActivityInstances(Long actId,
            Long procInstId, boolean activeOnly, boolean isSynchActivity)
            throws SQLException {
        List<ActivityInstance> ret;
        if (cacheActivityTransition ==CACHE_ONLY) {
            ret = new ArrayList<ActivityInstance>();
            for (ActivityInstance actInst : activityInstCache.values()) {
                if (!actInst.getActivityId().equals(actId)) continue;
                if (!actInst.getProcessInstanceId().equals(procInstId)) continue;
                if (activeOnly) {
                    if (isSynchActivity && actInst.getStatusCode()==WorkStatus.STATUS_COMPLETED
                            || actInst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS.intValue()
                            || actInst.getStatusCode()==WorkStatus.STATUS_WAITING.intValue()
                            || actInst.getStatusCode()==WorkStatus.STATUS_HOLD.intValue())
                        ret.add(actInst);
                } else ret.add(actInst);
            }
        } else {
            ret = edadb.getActivityInstances(actId, procInstId, activeOnly, isSynchActivity);
        }
        return ret;
    }

    public synchronized List<ProcessInstance> getChildProcessInstances(Long procInstId)
            throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            return edadb.getChildProcessInstances(procInstId);
        } else if (cacheProcess ==CACHE_ONLY) {
            return new ArrayList<ProcessInstance>();    // TODO implement this
        } else {
            return edadb.getChildProcessInstances(procInstId);
        }
    }

    public synchronized List<VariableInstance> getProcessInstanceVariables(
            Long processInstanceId) throws DataAccessException, SQLException {
        ProcessInstance pi = null;
        List<VariableInstance> vars = null;
        if (cacheVariable ==CACHE_OFF) {
            vars = edadb.getProcessInstanceVariables(processInstanceId);
        } else if (cacheVariable ==CACHE_ONLY) {
            pi = procInstCache.get(processInstanceId);
            vars = pi.getVariables();
        } else {
            pi = procInstCache.get(processInstanceId);
            if (pi==null) {
                pi = edadb.getProcessInstance(processInstanceId);
                procInstCache.put(processInstanceId, pi);
            }
            vars = pi.getVariables();
            if (vars==null) {
                vars = edadb.getProcessInstanceVariables(processInstanceId);
                pi.setVariables(vars);
            }
        }
        return vars;
    }

    public synchronized List<ProcessInstance> getProcessInstances(Long procId,
            String ownerType, Long ownerId) throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            return edadb.getProcessInstances(procId, ownerType, ownerId);
        } else if (cacheProcess ==CACHE_ONLY) {
            return new ArrayList<ProcessInstance>();    // TODO implement this, but this may not be needed
        } else {
            return edadb.getProcessInstances(procId, ownerType, ownerId);
        }
    }

    public synchronized VariableInstance getVariableInstance(Long varInstId) throws SQLException {
        if (cacheVariable == CACHE_OFF) {
            return edadb.getVariableInstance(varInstId);
        } else if (cacheVariable == CACHE_ONLY) {
            return null;
        } else {
            return edadb.getVariableInstance(varInstId);
        }
    }

    public synchronized VariableInstance getVariableInstance(Long procInstId,
            String varname) throws SQLException {
        VariableInstance var = null;
        if (cacheVariable ==CACHE_OFF) {
            var = edadb.getVariableInstance(procInstId, varname);
        } else if (cacheVariable ==CACHE_ONLY) {
            ProcessInstance pi = procInstCache.get(procInstId);
            var = pi.getVariable(varname);
        } else {
            ProcessInstance pi = procInstCache.get(procInstId);
            if (pi!=null && pi.getVariables()!=null)
                var = pi.getVariable(varname);
            else var = edadb.getVariableInstance(procInstId, varname);
        }
        return var;
    }

    public synchronized Integer lockActivityInstance(Long actInstId) throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            ActivityInstance actInst = activityInstCache.get(actInstId);
            return actInst.getStatusCode();
        } else {
            return edadb.lockActivityInstance(actInstId);
        }
    }

    public synchronized Integer lockProcessInstance(Long procInstId) throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            return edadb.lockProcessInstance(procInstId);
        } else if (cacheProcess ==CACHE_ONLY) {
            ProcessInstance procInst = procInstCache.get(procInstId);
            return procInst.getStatusCode();
        } else {
            return edadb.lockProcessInstance(procInstId);
        }
    }

    @Override
    public List<ProcessInstance> getProcessInstancesByMasterRequestId(String masterRequestId) throws SQLException {
        if (edadb != null)
            return edadb.getProcessInstancesByMasterRequestId(masterRequestId, null);

        return null;
    }

    public synchronized EventInstance lockEventInstance(String eventName) throws SQLException {
        if (cacheEvent ==CACHE_ONLY) {
            return eventInstCache.get(eventName);
        } else {
            return edadb.lockEventInstance(eventName);
        }
    }

    public synchronized Long recordEventLog(String name, String category,
            String subCategory, String source, String ownerType, Long ownerId,
            String user, String modUser, String comments) throws SQLException {
        if (cacheEvent ==CACHE_ONLY) {
            return null;
        } else {
            return edadb.recordEventLog(name, category, subCategory,
                    source, ownerType, ownerId, user, modUser, comments);
        }
    }

    public synchronized List<EventWaitInstance> recordEventArrive(String eventName, Long documentId)
            throws SQLException {
        if (cacheEvent == CACHE_ONLY) {
            EventInstance eventInst = eventInstCache.get(eventName);
            if (eventInst == null) {
                eventInst = new EventInstance();
                eventInst.setEventName(eventName);
                eventInst.setDocumentId(documentId);
                eventInst.setCreateDate(new Date());
                eventInst.setStatus(EventInstance.STATUS_ARRIVED);
                eventInst.setWaiters(new ArrayList<EventWaitInstance>());
                eventInstCache.put(eventName, eventInst);
                return new ArrayList<EventWaitInstance>();
            }
            else {
                if (eventInst.getStatus().equals(EventInstance.STATUS_WAITING)) {
                    eventInstCache.remove(eventName);
                    return eventInst.getWaiters();
                }
                else if (eventInst.getStatus().equals(EventInstance.STATUS_WAITING_MULTIPLE)) {
                    eventInst.setDocumentId(documentId);
                    eventInst.setStatus(EventInstance.STATUS_CONSUMED);
                    return eventInst.getWaiters();
                }
                else {
                    throw new SQLException(
                            "The event is already recorded and in status " + eventInst.getStatus());
                }
            }
        }
        else {
            return edadb.recordEventArrive(eventName, documentId);
        }
    }

    public synchronized Long recordEventWait(String eventName, boolean multipleRecepients,
            int preserveSeconds, Long actInstId, String compCode)
            throws SQLException {
        if (cacheEvent ==CACHE_ONLY) {
            EventInstance eventInst = eventInstCache.get(eventName);
            Long documentId;
            if (eventInst==null) {
                eventInst = new EventInstance();
                eventInst.setEventName(eventName);
                eventInst.setCreateDate(new Date());
                eventInst.setStatus(multipleRecepients?EventInstance.STATUS_WAITING_MULTIPLE:EventInstance.STATUS_WAITING);
                eventInst.setWaiters(new ArrayList<EventWaitInstance>());
                eventInstCache.put(eventName, eventInst);
                documentId = null;
            } else {
                if (eventInst.getStatus().equals(EventInstance.STATUS_WAITING)) {
                    if (multipleRecepients) {
                        throw new SQLException("The event has been waited by a single recepient");
                    } else {
                        eventInst.getWaiters().clear();    // deregister existing waiters
                    }
                    documentId = null;
                } else if (eventInst.getStatus().equals(EventInstance.STATUS_WAITING_MULTIPLE)) {
                    if (!multipleRecepients) {
                        throw new SQLException("The event has been waited by multiple recepients");
                    }
                    documentId = null;
                } else if (eventInst.getStatus().equals(EventInstance.STATUS_ARRIVED)) {
                    if (multipleRecepients) {
                        eventInst.setStatus(EventInstance.STATUS_CONSUMED);
                        documentId = eventInst.getDocumentId();
                    } else {
                        eventInstCache.remove(eventName);
                        documentId = eventInst.getDocumentId();
                    }
                } else if (eventInst.getStatus().equals(EventInstance.STATUS_CONSUMED)) {
                    if (multipleRecepients) {
                        documentId = eventInst.getDocumentId();
                    } else {
                        throw new SQLException("The event has been waited by multiple recepients");
                    }
                } else {        // STATUS_FLAG
                    throw new SQLException("The event is already recorded as a FLAG");
                }
            }
            EventWaitInstance eventWaitInst = new EventWaitInstance();
            eventWaitInst.setActivityInstanceId(actInstId);
            eventWaitInst.setCompletionCode(compCode);
            eventWaitInst.setMessageDocumentId(documentId);
            eventInst.getWaiters().add(eventWaitInst);
            return documentId;
        } else {
            return edadb.recordEventWait(eventName, multipleRecepients,
                    preserveSeconds, actInstId, compCode);
        }
    }

    public synchronized void removeEventWaitForActivityInstance(Long activityInstanceId, String reason)
            throws SQLException {
        if (cacheEvent ==CACHE_ONLY) {
            for (String eventName : eventInstCache.keySet()) {
                EventInstance eventInst = eventInstCache.get(eventName);
                for (EventWaitInstance ewi : eventInst.getWaiters()) {
                    // TODO implement this
                }
            }
        } else {
            edadb.removeEventWaitForActivityInstance(activityInstanceId, reason);
        }
    }

    public synchronized void removeEventWaitForProcessInstance(Long processInstanceId)
            throws SQLException {
        if (cacheEvent ==CACHE_ONLY) {
            // TODO implement this
        } else {
            edadb.removeEventWaitForProcessInstance(processInstanceId);
        }
    }

    public synchronized void setProcessInstanceStartTime(Long processInstanceId) throws SQLException {
        if (cacheProcess == CACHE_OFF) {
            edadb.setProcessInstanceStartTime(processInstanceId);
        } else if (cacheProcess == CACHE_ONLY) {
            ProcessInstance processInstance = procInstCache.get(processInstanceId);
            processInstance.setStartDate(new Date());
        } else {
            edadb.setProcessInstanceStartTime(processInstanceId);
            ProcessInstance procInst = procInstCache.get(processInstanceId);
            if (procInst != null)
                procInst.setStartDate(new Date(DatabaseAccess.getCurrentTime()));
        }
    }

    public synchronized void setProcessInstanceCompletionCode(Long procInstId,
            String completionCode) throws SQLException {
        if (cacheProcess ==CACHE_OFF) {
            edadb.setProcessInstanceCompletionCode(procInstId, completionCode);
        } else if (cacheProcess ==CACHE_ONLY) {
            ProcessInstance procInst = procInstCache.get(procInstId);
            procInst.setCompletionCode(completionCode);
        } else {
            edadb.setProcessInstanceCompletionCode(procInstId, completionCode);
            ProcessInstance procInst = procInstCache.get(procInstId);
            if (procInst!=null) procInst.setCompletionCode(completionCode);
        }
    }

    public synchronized void updateActivityInstanceEndTime(Long actInstId, Date endtime)
            throws SQLException {
        if (cacheActivityTransition ==CACHE_ONLY) {
            ActivityInstance actInst = activityInstCache.get(actInstId);
            actInst.setEndDate(new Date());
        } else {
            edadb.updateActivityInstanceEndTime(actInstId, endtime);
        }
    }



    private void setCacheFlags(boolean forServiceProcess, int performanceLevel) {
        if (performanceLevel >= 7) {
            this.cacheActivityTransition = CACHE_ONLY;
            this.cacheVariable = CACHE_ONLY;
            this.cacheDocument = CACHE_ONLY;
            this.cacheEvent = CACHE_ONLY;
        } else if (performanceLevel >= 5) {    // for regular processes, 3 and 5 are the same
            this.cacheActivityTransition = CACHE_OFF;
            this.cacheVariable = forServiceProcess?CACHE_ONLY:CACHE_ON;
            this.cacheDocument = forServiceProcess?CACHE_ONLY:CACHE_ON;
            // this.cache_event = forServiceProcess?CACHE_ONLY:CACHE_OFF;
            this.cacheEvent = CACHE_OFF;
        } else if (performanceLevel >= 3) {
            this.cacheActivityTransition = CACHE_OFF;
            this.cacheVariable = CACHE_ON;
            this.cacheDocument = CACHE_ON;
            // this.cache_event = forServiceProcess?CACHE_ONLY:CACHE_OFF;
            this.cacheEvent = CACHE_OFF;
        } else {
            this.cacheActivityTransition = CACHE_OFF;
            this.cacheVariable = CACHE_OFF;
            this.cacheDocument = CACHE_OFF;
            this.cacheEvent = CACHE_OFF;
        }
        if (cacheActivityTransition == CACHE_OFF && cacheVariable == CACHE_OFF)
            cacheProcess = CACHE_OFF;
        else if (cacheActivityTransition == CACHE_ONLY && cacheVariable == CACHE_ONLY)
            cacheProcess = CACHE_ONLY;
        else cacheProcess = CACHE_ON;
    }

    public static EngineDataAccessCache getInstance(boolean forServiceProcess, int performanceLevel) {
        EngineDataAccessCache edac;
        if (forServiceProcess) {
            edac = new EngineDataAccessCache(forServiceProcess, performanceLevel);
        } else {
            if (performanceLevel>=9) {
                if (memoryOnlyInstance == null)
                    memoryOnlyInstance = new EngineDataAccessCache(forServiceProcess, performanceLevel);
                edac = memoryOnlyInstance;
            } else edac = new EngineDataAccessCache(forServiceProcess, performanceLevel);
        }
        return edac;
    }

    public void persistInternalEvent(String eventId, String message)
            throws SQLException {
        edadb.persistInternalEvent(eventId, message);
    }

    public int deleteEventInstance(String eventName) throws SQLException {
        return edadb.deleteEventInstance(eventName);
    }

    public Long getRequestCompletionTime(String ownerType, Long ownerId) throws SQLException {
        return edadb == null || cacheDocument == CACHE_ONLY ? null : edadb.getRequestCompletionTime(ownerType, ownerId);
    }

    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws SQLException {
        if (edadb != null) edadb.setElapsedTime(ownerType, instanceId, elapsedTime);
    }

    public TransactionWrapper startTransaction() throws DataAccessException {
        if (edadb!=null) return edadb.startTransaction();
        else return null;
    }

    public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
        if (edadb!=null) edadb.stopTransaction(transaction);
    }
}
