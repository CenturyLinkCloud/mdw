/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.process;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.event.EventInstanceVO;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class EngineDataAccessCache implements EngineDataAccess {

	protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static EngineDataAccessCache memoryOnlyInstance;	// for regular processes only

    private static final int CACHE_OFF = 0;
    private static final int CACHE_ON = 1;
    private static final int CACHE_ONLY = 2;

    protected EngineDataAccessDB edadb;

    private Map<Long,ProcessInstanceVO> procInstCache;      // used when persist_variable is false
    private Map<Long,DocumentVO> documentCache;				// for local document
    private Map<Long,ActivityInstanceVO> activityInstCache;
    private Map<String,EventInstanceVO> eventInstCache;

    private int performance_level;
    private int cache_process;
    private int cache_activity_transition;		// only support CACHE_OFF and CACHE_ONLY
    private int cache_variable;
    private int cache_document;
    private int cache_event;					// only support CACHE_OFF and CACHE_ONLY

    private static long next_id = 1000;

	private EngineDataAccessCache(boolean forServiceProcess, int performance_level) {
		this.performance_level = performance_level;
		this.setCacheFlags(forServiceProcess, performance_level);
    	if (cache_process!=CACHE_OFF) procInstCache = new HashMap<Long,ProcessInstanceVO>();
    	if (cache_document!=CACHE_OFF) documentCache = new HashMap<Long,DocumentVO>();
    	if (cache_activity_transition==CACHE_ONLY) activityInstCache = new HashMap<Long,ActivityInstanceVO>();
    	if (cache_event==CACHE_ONLY) eventInstCache = new HashMap<String,EventInstanceVO>();
    	if (cache_process!=CACHE_ONLY || cache_document!=CACHE_ONLY && cache_event!=CACHE_ONLY)
    		edadb = new EngineDataAccessDB();
	}

	public DatabaseAccess getDatabaseAccess() {
    	return edadb.getDatabaseAccess();
	}

    private Long getNextInternalId() {
        return next_id++;
    }

    public int getPerformanceLevel() {
    	return performance_level;
    }

    public DocumentVO loadDocument(Long documentId, boolean forUpdate) throws DataAccessException, SQLException {
        if (edadb == null)  { // is the case for CACHE_ONLY
            EngineDataAccessDB db = new EngineDataAccessDB();
            TransactionWrapper tw = null;
            try {
                tw = db.startTransaction();
                return db.loadDocument(documentId, forUpdate);
            }
            finally {
                if (tw != null)
                  db.stopTransaction(tw);
            }
        }
        else
            return edadb.getDocument(documentId, forUpdate);
    }

    public synchronized DocumentVO getDocument(Long documentId, boolean forUpdate) throws SQLException {
        DocumentVO docvo = null;
        if (cache_document==CACHE_OFF) {
        	docvo = edadb.getDocument(documentId, forUpdate);
        } else if (cache_document==CACHE_ONLY) {
        	docvo = documentCache.get(documentId);
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
     * @param docref
     * @param doc
     * @throws DataAccessException
     */
    public synchronized void updateDocumentContent(Long docid, String content) throws SQLException {
    	if (cache_document==CACHE_OFF) {
    		edadb.updateDocumentContent(docid, content);
        } else if (cache_document==CACHE_ONLY) {
    		DocumentVO docvo = documentCache.get(docid);
    		if (docvo!=null) docvo.setContent(content);
        } else {
        	edadb.updateDocumentContent(docid, content);
    		DocumentVO docvo = documentCache.get(docid);
    		if (docvo!=null) docvo.setContent(content);
        }
    }

    /**
     * Update document information (everything but document content itself).
     * The method will update only the arguments that have non-null values.
     * @param docref
     * @param processInstId
     * @param documentType
     * @param ownerType
     * @param ownerId
     * @param searchKey1
     * @param searchKey2
     * @throws DataAccessException
     */
    public synchronized void updateDocumentInfo(DocumentVO docvo) throws SQLException {
    	if (cache_document==CACHE_OFF) {
    		edadb.updateDocumentInfo(docvo);
        } else if (cache_document==CACHE_ONLY) {
    		DocumentVO docvo0 = documentCache.get(docvo.getDocumentId());
    		docvo0.setDocumentType(docvo.getDocumentType());
    		docvo0.setOwnerId(docvo.getOwnerId());
    		docvo0.setOwnerType(docvo.getOwnerType());
    		docvo0.setProcessInstanceId(docvo.getProcessInstanceId());
        } else {
    		edadb.updateDocumentInfo(docvo);
    		DocumentVO docvo0 = documentCache.get(docvo.getDocumentId());
    		if (docvo0!=docvo) {
    			docvo0.setDocumentType(docvo.getDocumentType());
        		docvo0.setOwnerId(docvo.getOwnerId());
        		docvo0.setOwnerType(docvo.getOwnerType());
        		docvo0.setProcessInstanceId(docvo.getProcessInstanceId());
    		}
        }
    }

    public synchronized ProcessInstanceVO getProcessInstance(Long processInstId)
		    throws SQLException {
    	ProcessInstanceVO pi = null;
    	if (cache_process==CACHE_OFF) {
    		pi = edadb.getProcessInstance(processInstId);
    	} else if (cache_process==CACHE_ONLY) {
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

    public synchronized Long createProcessInstance(ProcessInstanceVO procinst) throws SQLException {
    	if (cache_process==CACHE_OFF) {
    		edadb.createProcessInstance(procinst);
    	} else if (cache_process==CACHE_ONLY) {
    		procinst.setId(getNextInternalId());
    		procInstCache.put(procinst.getId(), procinst);
    	} else {
    		edadb.createProcessInstance(procinst);
    		procInstCache.put(procinst.getId(), procinst);
    	}
    	return procinst.getId();
    }

    public synchronized Long createVariableInstance(VariableInstanceInfo var, Long processInstId)
            throws SQLException {
    	if (cache_variable==CACHE_OFF) {
    		edadb.createVariableInstance(var, processInstId);
    	} else if (cache_variable==CACHE_ONLY) {
    		var.setInstanceId(getNextInternalId());
    		ProcessInstanceVO pi = procInstCache.get(processInstId);
    		VariableInstanceInfo var0 = pi.getVariable(var.getName());
    		if (var0==null) pi.getVariables().add(var);
    	} else {
    		edadb.createVariableInstance(var, processInstId);
    		ProcessInstanceVO pi = procInstCache.get(processInstId);
    		if (pi!=null) {
    			List<VariableInstanceInfo> vars = pi.getVariables();
    			if (vars==null) {
    				// this is possible only when the process instance
    				// is not currently loaded, such as when called in setParameterValue/3
    				vars = new ArrayList<VariableInstanceInfo>();
    				vars.add(var);
    			} else {
    				VariableInstanceInfo var0 = pi.getVariable(var.getName());
    				if (var0==null) vars.add(var);
    			}
    		}
    	}
    	return var.getInstanceId();
    }

    public synchronized void updateVariableInstance(VariableInstanceInfo var) throws SQLException {
    	if (cache_variable==CACHE_OFF) {
    		edadb.updateVariableInstance(var);
    	} else if (cache_variable==CACHE_ONLY) {
    		// updated already
    	} else {
    		edadb.updateVariableInstance(var);
    	}
    }

    public synchronized Long createDocument(DocumentVO docvo) throws SQLException {
        return createDocument(docvo, null);
    }

    public synchronized Long createDocument(DocumentVO docvo, PackageVO pkg) throws SQLException {
    	if (cache_document==CACHE_OFF) {
    		edadb.createDocument(docvo, pkg);
        } else if (cache_document==CACHE_ONLY) {
        	docvo.setDocumentId(getNextInternalId());
        	documentCache.put(docvo.getDocumentId(), docvo);
        } else {
        	edadb.createDocument(docvo);
        	documentCache.put(docvo.getDocumentId(), docvo);
        }
    	return docvo.getDocumentId();
    }

    public void addDocumentToCache(DocumentVO docvo) {
    	documentCache.put(docvo.getDocumentId(), docvo);
    }

    public synchronized Long createTransitionInstance(WorkTransitionInstanceVO vo) throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		vo.setTransitionInstanceID(getNextInternalId());
    	} else {
    		edadb.createTransitionInstance(vo);
    	}
    	return vo.getTransitionInstanceID();
    }

    public synchronized void completeTransitionInstance(Long transInstId, Long actInstId) throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		// no cache for transition instance
    	} else {
    		edadb.completeTransitionInstance(transInstId, actInstId);
    	}
    }

    public synchronized Long createActivityInstance(ActivityInstanceVO vo) throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
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
    public synchronized void setActivityInstanceStatus(ActivityInstanceVO actInst,
    		Integer status, String statusMessage)
            throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		actInst.setStatusCode(status);
    		actInst.setStatusMessage(statusMessage);
    	} else {
    		edadb.setActivityInstanceStatus(actInst, status, statusMessage);
    	}
    }

    public synchronized void setProcessInstanceStatus(Long processInstId, Integer status) throws SQLException {
    	if (cache_process==CACHE_OFF) {
    		edadb.setProcessInstanceStatus(processInstId, status);
    	} else if (cache_process==CACHE_ONLY) {
			ProcessInstanceVO pi = procInstCache.get(processInstId);
			if (status.equals(WorkStatus.STATUS_PENDING_PROCESS)) {
	            status = WorkStatus.STATUS_IN_PROGRESS;
	            pi.setStartDate(StringHelper.dateToString(new Date()));
			}
			pi.setStatusCode(status);
    	} else {
    		edadb.setProcessInstanceStatus(processInstId, status);
    		if (status.equals(WorkStatus.STATUS_PENDING_PROCESS))
	            status = WorkStatus.STATUS_IN_PROGRESS;
			ProcessInstanceVO pi = procInstCache.get(processInstId);
    		if (pi!=null) pi.setStatusCode(status);
    	}
    }

	public synchronized void cancelTransitionInstances(Long procInstId, String comment,
			Long transId) throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		// TODO what to do with this?
    	} else {
			edadb.cancelTransitionInstances(procInstId, comment, transId);
    	}
	}

	public synchronized int countTransitionInstances(Long pProcInstId, Long pWorkTransId)
			throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		return 0;	// TODO check if this is okay
    	} else {
    		return edadb.countTransitionInstances(pProcInstId, pWorkTransId);
    	}
	}

	public int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
	throws SQLException {
		if (cache_activity_transition==CACHE_ONLY) {
			int count = 0;
			for (ActivityInstanceVO actInst : activityInstCache.values()) {
				if (!actInst.getDefinitionId().equals(activityId)) continue;
				if (!actInst.getOwnerId().equals(procInstId)) continue;
				for (Integer s : statuses) {
					if (s.intValue()==actInst.getStatusCode()) {
						count++;
						break;
					}
				}
	        }
			return count;
    	} else {
    		return edadb.countActivityInstances(procInstId, activityId, statuses);
    	}
	}

	public synchronized void determineCompletedTransitions(Long pProcInstId,
			List<WorkTransitionVO> transitions) throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
    		// TODO - how to implement this?
    	} else {
    		edadb.determineCompletedTransitions(pProcInstId, transitions);
    	}
	}

	public synchronized List<DocumentVO> findDocuments(Long procInstId, String type,
			String searchKey1, String searchKey2, String ownerType,
			Long ownerId, Date createDateStart, Date createDateEnd,
			String orderByClause) throws SQLException {
		return edadb.findDocuments(procInstId, type, searchKey1, searchKey2,
				ownerType, ownerId, createDateStart, createDateEnd, orderByClause);
		// TODO handle CACHE_ON and CACHE_ONLY
	}

	public synchronized ActivityInstanceVO getActivityInstance(Long actInstId)
			throws DataAccessException, SQLException {
		ActivityInstanceVO actInst;
    	if (cache_activity_transition==CACHE_ONLY) {
    		actInst = activityInstCache.get(actInstId);
    	} else {
    		actInst = edadb.getActivityInstance(actInstId);
    	}
    	return actInst;
	}

	public synchronized List<ActivityInstanceVO> getActivityInstances(Long actId,
			Long procInstId, boolean activeOnly, boolean isSynchActivity)
			throws SQLException {
		List<ActivityInstanceVO> ret;
    	if (cache_activity_transition==CACHE_ONLY) {
    		ret = new ArrayList<ActivityInstanceVO>();
    		for (ActivityInstanceVO actInst : activityInstCache.values()) {
    			if (!actInst.getDefinitionId().equals(actId)) continue;
    			if (!actInst.getOwnerId().equals(procInstId)) continue;
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

	public synchronized List<ProcessInstanceVO> getChildProcessInstances(Long procInstId)
			throws SQLException {
		if (cache_process==CACHE_OFF) {
			return edadb.getChildProcessInstances(procInstId);
    	} else if (cache_process==CACHE_ONLY) {
    		return new ArrayList<ProcessInstanceVO>();	// TODO implement this
    	} else {
    		return edadb.getChildProcessInstances(procInstId);
    	}
	}

	public synchronized List<VariableInstanceInfo> getProcessInstanceVariables(
			Long processInstanceId) throws DataAccessException, SQLException {
		ProcessInstanceVO pi = null;
		List<VariableInstanceInfo> vars = null;
    	if (cache_variable==CACHE_OFF) {
    		vars = edadb.getProcessInstanceVariables(processInstanceId);
    	} else if (cache_variable==CACHE_ONLY) {
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

	public synchronized List<ProcessInstanceVO> getProcessInstances(Long procId,
			String ownerType, Long ownerId) throws SQLException {
    	if (cache_process==CACHE_OFF) {
    		return edadb.getProcessInstances(procId, ownerType, ownerId);
    	} else if (cache_process==CACHE_ONLY) {
    		return new ArrayList<ProcessInstanceVO>();	// TODO implement this, but this may not be needed
    	} else {
    		return edadb.getProcessInstances(procId, ownerType, ownerId);
    	}
	}

	public synchronized VariableInstanceInfo getVariableInstance(Long varInstId)
			throws DataAccessException, SQLException {
    	if (cache_variable==CACHE_OFF) {
    		return edadb.getVariableInstance(varInstId);
    	} else if (cache_variable==CACHE_ONLY) {
    		return null;	// TODO implement this - but may not be needed (used by designer/task manager only
    	} else {
    		return edadb.getVariableInstance(varInstId);
    	}
	}

	public synchronized VariableInstanceInfo getVariableInstance(Long procInstId,
			String varname) throws SQLException {
		VariableInstanceInfo var = null;
    	if (cache_variable==CACHE_OFF) {
			var = edadb.getVariableInstance(procInstId, varname);
    	} else if (cache_variable==CACHE_ONLY) {
    		ProcessInstanceVO pi = procInstCache.get(procInstId);
    		var = pi.getVariable(varname);
    	} else {
    		ProcessInstanceVO pi = procInstCache.get(procInstId);
    		if (pi!=null && pi.getVariables()!=null)
    			var = pi.getVariable(varname);
    		else var = edadb.getVariableInstance(procInstId, varname);
    	}
		return var;
	}

	public synchronized Integer lockActivityInstance(Long actInstId) throws SQLException {
		if (cache_activity_transition==CACHE_ONLY) {
			ActivityInstanceVO actInst = activityInstCache.get(actInstId);
			return actInst.getStatusCode();
		} else {
			return edadb.lockActivityInstance(actInstId);
		}
	}

	public synchronized Integer lockProcessInstance(Long procInstId) throws SQLException {
		if (cache_process==CACHE_OFF) {
			return edadb.lockProcessInstance(procInstId);
		} else if (cache_process==CACHE_ONLY) {
			ProcessInstanceVO procInst = procInstCache.get(procInstId);
			return procInst.getStatusCode();
		} else {
			return edadb.lockProcessInstance(procInstId);
		}
	}

	public synchronized EventInstanceVO lockEventInstance(String eventName) throws SQLException {
		if (cache_event==CACHE_ONLY) {
			return eventInstCache.get(eventName);
		} else {
			return edadb.lockEventInstance(eventName);
		}
	}

	public synchronized Long recordEventLog(String name, String category,
			String subCategory, String source, String ownerType, Long ownerId,
			String user, String modUser, String comments) throws SQLException {
		if (cache_event==CACHE_ONLY) {
			return null;
		} else {
			return edadb.recordEventLog(name, category, subCategory,
					source, ownerType, ownerId, user, modUser, comments);
		}
	}

	public synchronized List<EventWaitInstanceVO> recordEventArrive(String eventName,
			Long documentId) throws SQLException {
		if (cache_event==CACHE_ONLY) {
			EventInstanceVO eventInst = eventInstCache.get(eventName);
			if (eventInst==null) {
				eventInst = new EventInstanceVO();
				eventInst.setEventName(eventName);
				eventInst.setDocumentId(documentId);
				eventInst.setCreateDate(new Date());
				eventInst.setStatus(EventInstanceVO.STATUS_ARRIVED);
				eventInst.setWaiters(new ArrayList<EventWaitInstanceVO>());
				eventInstCache.put(eventName, eventInst);
				return new ArrayList<EventWaitInstanceVO>();
			} else {
				if (eventInst.getStatus().equals(EventInstanceVO.STATUS_WAITING)) {
					eventInstCache.remove(eventName);
	                return eventInst.getWaiters();
	            } else if (eventInst.getStatus().equals(EventInstanceVO.STATUS_WAITING_MULTIPLE)) {
	            	eventInst.setDocumentId(documentId);
	                eventInst.setStatus(EventInstanceVO.STATUS_CONSUMED);
	                return eventInst.getWaiters();
	            } else {
	                throw new SQLException("The event is already recorded and in status "
	                        + eventInst.getStatus());
	            }
			}
		} else {
			return edadb.recordEventArrive(eventName, documentId);
		}
	}

	public synchronized Long recordEventWait(String eventName, boolean multipleRecepients,
			int preserveSeconds, Long actInstId, String compCode)
			throws SQLException {
		if (cache_event==CACHE_ONLY) {
			EventInstanceVO eventInst = eventInstCache.get(eventName);
			Long documentId;
			if (eventInst==null) {
				eventInst = new EventInstanceVO();
				eventInst.setEventName(eventName);
				eventInst.setCreateDate(new Date());
				eventInst.setStatus(multipleRecepients?EventInstanceVO.STATUS_WAITING_MULTIPLE:EventInstanceVO.STATUS_WAITING);
				eventInst.setWaiters(new ArrayList<EventWaitInstanceVO>());
				eventInstCache.put(eventName, eventInst);
				documentId = null;
			} else {
				if (eventInst.getStatus().equals(EventInstanceVO.STATUS_WAITING)) {
					if (multipleRecepients) {
						throw new SQLException("The event has been waited by a single recepient");
					} else {
						eventInst.getWaiters().clear();    // deregister existing waiters
					}
					documentId = null;
				} else if (eventInst.getStatus().equals(EventInstanceVO.STATUS_WAITING_MULTIPLE)) {
					if (!multipleRecepients) {
						throw new SQLException("The event has been waited by multiple recepients");
					}
					documentId = null;
				} else if (eventInst.getStatus().equals(EventInstanceVO.STATUS_ARRIVED)) {
					if (multipleRecepients) {
		                eventInst.setStatus(EventInstanceVO.STATUS_CONSUMED);
						documentId = eventInst.getDocumentId();
					} else {
						eventInstCache.remove(eventName);
						documentId = eventInst.getDocumentId();
					}
				} else if (eventInst.getStatus().equals(EventInstanceVO.STATUS_CONSUMED)) {
					if (multipleRecepients) {
						documentId = eventInst.getDocumentId();
					} else {
						throw new SQLException("The event has been waited by multiple recepients");
					}
				} else {        // STATUS_FLAG
					throw new SQLException("The event is already recorded as a FLAG");
				}
			}
			EventWaitInstanceVO eventWaitInst = new EventWaitInstanceVO();
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
		if (cache_event==CACHE_ONLY) {
			for (String eventName : eventInstCache.keySet()) {
				EventInstanceVO eventInst = eventInstCache.get(eventName);
				for (EventWaitInstanceVO ewi : eventInst.getWaiters()) {
					// TODO implement this
				}
			}
		} else {
			edadb.removeEventWaitForActivityInstance(activityInstanceId, reason);
		}
	}

	public synchronized void removeEventWaitForProcessInstance(Long processInstanceId)
			throws SQLException {
		if (cache_event==CACHE_ONLY) {
			// TODO implement this
		} else {
			edadb.removeEventWaitForProcessInstance(processInstanceId);
		}
	}

	public synchronized boolean recordEventFlag(String eventName, int preserveInterval)
			throws SQLException {
		if (cache_event==CACHE_ONLY) {
			EventInstanceVO eventInst = eventInstCache.get(eventName);
			if (eventInst==null) {
				eventInst = new EventInstanceVO();
				eventInst.setEventName(eventName);
				eventInst.setCreateDate(new Date());
				eventInst.setStatus(EventInstanceVO.STATUS_FLAG);
				eventInstCache.put(eventName, eventInst);
				return false;
			} else {
				if (eventInst.getStatus().equals(EventInstanceVO.STATUS_FLAG)) {
	                return true;
	            } else {
	                throw new SQLException("The event is already recorded but not a FLAG");
	            }
			}
		} else {
			return edadb.recordEventFlag(eventName, preserveInterval);
		}
	}

	public synchronized void setProcessInstanceCompletionCode(Long procInstId,
			String completionCode) throws SQLException {
    	if (cache_process==CACHE_OFF) {
    		edadb.setProcessInstanceCompletionCode(procInstId, completionCode);
    	} else if (cache_process==CACHE_ONLY) {
			ProcessInstanceVO procInst = procInstCache.get(procInstId);
			procInst.setCompletionCode(completionCode);
    	} else {
    		edadb.setProcessInstanceCompletionCode(procInstId, completionCode);
    		ProcessInstanceVO procInst = procInstCache.get(procInstId);
    		if (procInst!=null) procInst.setCompletionCode(completionCode);
    	}
	}

	public synchronized void updateActivityInstanceEndTime(Long actInstId, Date endtime)
			throws SQLException {
    	if (cache_activity_transition==CACHE_ONLY) {
			ActivityInstanceVO actInst = activityInstCache.get(actInstId);
			actInst.setEndDate(StringHelper.dateToString(new Date()));
    	} else {
    		edadb.updateActivityInstanceEndTime(actInstId, endtime);
    	}
	}



	private void setCacheFlags(boolean forServiceProcess, int performance_level) {
		if (performance_level>=7) {
			this.cache_activity_transition = CACHE_ONLY;
			this.cache_variable = CACHE_ONLY;
			this.cache_document = CACHE_ONLY;
			this.cache_event = CACHE_ONLY;
		} else if (performance_level>=5) {	// for regular processes, 3 and 5 are the same
			this.cache_activity_transition = CACHE_OFF;
			this.cache_variable = forServiceProcess?CACHE_ONLY:CACHE_ON;
			this.cache_document = forServiceProcess?CACHE_ONLY:CACHE_ON;
			// this.cache_event = forServiceProcess?CACHE_ONLY:CACHE_OFF;
			this.cache_event = CACHE_OFF;
		} else if (performance_level>=3) {
			this.cache_activity_transition = CACHE_OFF;
			this.cache_variable = CACHE_ON;
			this.cache_document = CACHE_ON;
			// this.cache_event = forServiceProcess?CACHE_ONLY:CACHE_OFF;
			this.cache_event = CACHE_OFF;
		} else {
			this.cache_activity_transition = CACHE_OFF;
			this.cache_variable = CACHE_OFF;
			this.cache_document = CACHE_OFF;
			this.cache_event = CACHE_OFF;
		}
		if (cache_activity_transition==CACHE_OFF && cache_variable==CACHE_OFF)
			cache_process = CACHE_OFF;
		else if (cache_activity_transition==CACHE_ONLY && cache_variable==CACHE_ONLY)
			cache_process = CACHE_ONLY;
		else cache_process = CACHE_ON;
	}

	public static EngineDataAccessCache getInstance(boolean forServiceProcess, int performance_level) {
		EngineDataAccessCache edac;
		if (forServiceProcess) {
			edac = new EngineDataAccessCache(forServiceProcess, performance_level);
		} else {
			if (performance_level>=9) {
				if (memoryOnlyInstance==null) memoryOnlyInstance =
					new EngineDataAccessCache(forServiceProcess, performance_level);
				edac = memoryOnlyInstance;
			} else edac = new EngineDataAccessCache(forServiceProcess, performance_level);
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

	public TransactionWrapper startTransaction() throws DataAccessException {
		if (edadb!=null) return edadb.startTransaction();
		else return null;
	}

	public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
		if (edadb!=null) edadb.stopTransaction(transaction);
	}

}
