/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.common.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.RemoteAccess;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.activity.TextNoteVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

/**
 * Value object representing a process definition.
 */
public class ProcessVO extends RuleSetVO {

    public static final String TRANSITION_ON_NULL = "Matches Null Return Code";
    public static final String TRANSITION_ON_DEFAULT = "Acts as Default";

    public static final Integer PROCESS_TYPE_CONCRETE = new Integer(1);
    public static final Integer PROCESS_TYPE_ALIAS = new Integer(2);		// for MDW 5, this means process def is stored in RULE_SET

    public static final String OLD_START_ACTIVITY_BASE_CLASS = "com.qwest.mdw.workflow.activity.types.StartActivity";

    private List<ExternalEventVO> externalEvents;
    private List<VariableVO> variables;
    private List<WorkTransitionVO> transitions;
    private List<ProcessVO> subProcesses;
    private List<ActivityVO> activities;
    private List<TextNoteVO> textNotes;
    private List<AttributeVO> attributes;
    private List<ActivityImplementorVO> implementors;
    private List<Long> deletedTransitions;  // for designer's use only
    private String remoteServer;            // for remote server logical name; null for local
    private boolean isInRuleSet;

    public ProcessVO() {
    	setLanguage(RuleSetVO.PROCESS);
        deletedTransitions = null;
        remoteServer = null;
        isInRuleSet = false;
    }

    public ProcessVO(Long id) {
        this();
        setId(id);
    }

    public ProcessVO(Long pProcessId, String pPrName, String pDesc, List<ExternalEventVO> externalEvents) {
    	setLanguage(RuleSetVO.PROCESS);
    	this.setId(pProcessId);
    	this.setName(pPrName);
    	this.setComment(pDesc);
        this.externalEvents = externalEvents;
        deletedTransitions = null;
        remoteServer = null;
        isInRuleSet = false;
    }

    public ProcessVO(ProcessVO cloneFrom) {
        super(cloneFrom);
        setExternalEvents(cloneFrom.getExternalEvents());
        setVariables(cloneFrom.getVariables());
        setTransitions(cloneFrom.getTransitions());
        setSubProcesses(cloneFrom.getSubProcesses());
        setActivities(cloneFrom.getActivities());
        setAttributes(cloneFrom.getAttributes());
        setImplementors(cloneFrom.getImplementors());
        setTextNotes(cloneFrom.getTextNotes());
        clearDeletedTransitions();
        isInRuleSet = cloneFrom.isInRuleSet;
        overrideAttributesApplied = cloneFrom.overrideAttributesApplied();
    }

    public void set(List<AttributeVO> pAttribs,  List<VariableVO> pVariables, List<WorkTransitionVO> pWorkTrans,
                List<ProcessVO> pSubProcesses, List<ActivityVO> pActivities){
        this.activities = pActivities;
        this.attributes = pAttribs;
        this.transitions = pWorkTrans;
        this.variables = pVariables;
        this.subProcesses = pSubProcesses;
    }

    /**
     * checks if the passed in workId is
     * Activity
     * @return boolean status
     */
    public boolean isWorkActivity(Long pWorkId) {

        if(this.activities == null){
            return false;
        }
        for(int i=0; i<activities.size(); i++){
            if(pWorkId.longValue() == activities.get(i).getActivityId().longValue()){
                return true;
            }
        }
        return false;
    }

    /**
     * returns the process VO identified by the passed in work id
     * It is also possible the sub process is referencing self.
     * @param pWorkId
     */
    public ProcessVO getSubProcessVO(Long pWorkId) {
        if (this.getId().equals(pWorkId)) return this;
        if(this.subProcesses == null) return null;
        for (ProcessVO ret : subProcesses) {
            if (ret.getProcessId().equals(pWorkId)) return ret;
        }
        return null;
    }

    /**
     * Returns the Activity VO
     * @param pWorkId
     * @returb ActivityVO
     */
     public ActivityVO getActivityVO(Long pWorkId) {
         if(this.activities == null){
            return null;
        }
        for(int i=0; i<activities.size(); i++){
            if(pWorkId.longValue() == activities.get(i).getActivityId().longValue()){
                return activities.get(i);
            }
        }
        return null;

    }

     /**
     * Returns the WorkTransitionVO
     * @param pWorkTransId
     * @returb WorkTransitionVO
     */
     public WorkTransitionVO getWorkTransitionVO(Long pWorkTransId) {
         if(this.transitions == null){
            return null;
        }
        for(int i=0; i<transitions.size(); i++){
            if(pWorkTransId.longValue() == transitions.get(i).getWorkTransitionId().longValue()){
                return transitions.get(i);
            }
        }
        return null;

    }

    /**
	 * @return the activities
	 */
	public List<ActivityVO> getActivities() {
		return activities;
	}

	/**
	 * @param activities the activities to set
	 */
	public void setActivities(List<ActivityVO> activities) {
		this.activities = activities;
	}

	public boolean hasDynamicJavaActivity() {
	    if (activities != null) {
	        for (ActivityVO activity : activities) {
	            if (activity.getImplementorClassName() != null && activity.getImplementorClassName().endsWith("DynamicJavaActivity"))
	                return true;
	        }
	    }
	    return false;
	}

	public List<TextNoteVO> getTextNotes() {
		return textNotes;
	}

	public void setTextNotes(List<TextNoteVO> v) {
		this.textNotes = v;
	}

    /**
	 * @return the attributes
	 */
	public List<AttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<AttributeVO> attributes) {
		this.attributes = attributes;
	}


	/**
	 * @return the processId
	 */
	public Long getProcessId() {
		return getId();
	}

	/**
	 * @param processId the processId to set
	 */
	public void setProcessId(Long processId) {
		setId(processId);
	}

	/**
	 * @return the processName
	 */
	public String getProcessName() {
		return getName();
	}

	/**
	 * @param processName the processName to set
	 */
	public void setProcessName(String processName) {
		setName(processName);
	}

    /**
	 * @return the processDescription
	 */
	public String getProcessDescription() {
		return this.getComment();
	}

	/**
	 * @param processDescription the processDescription to set
	 */
	public void setProcessDescription(String processDescription) {
		this.setComment(processDescription);
	}

	/**
	 * @return the subProcesses
	 */
	public List<ProcessVO> getSubProcesses() {
	    return this.subProcesses;
	}

	/**
	 * @param subProcesses the subProcesses to set
	 */
	public void setSubProcesses(List<ProcessVO> pSubProcesses) {
	    this.subProcesses = pSubProcesses;
	}

	/**
	 * @return the transitions
	 */
	public List<WorkTransitionVO> getTransitions() {
		return transitions;
	}

	/**
	 * @param transitions the transitions to set
	 */
	public void setTransitions(List<WorkTransitionVO> transitions) {
		this.transitions = transitions;
	}

	/**
	 * @return the variables
	 */
	public List<VariableVO> getVariables() {
		return variables;
	}

	public VariableVO getVariable(String varName) {
	    for (VariableVO var : variables) {
	        if (var.getVariableName().equals(varName))
	            return var;
	    }
	    return null; // not found
	}

	public VariableVO getVariable(Long id) {
	    for (VariableVO var : variables) {
	        if (var.getVariableId().equals(id))
	            return var;
	    }
	    return null; // not found
	}

	/**
	 * @param variables the variables to set
	 */
	public void setVariables(List<VariableVO> variables) {
		this.variables = variables;
	}

	public void addLocalVariable(VariableTypeVO variableType, String variableName) {
	    if (getVariable(variableName) == null) {
	        VariableVO varVO = new VariableVO();
            varVO.setVariableName(variableName);
            varVO.setVariableType(variableType.getVariableType());
            varVO.setVariableCategory(VariableVO.CAT_LOCAL);
	        variables.add(varVO);
	    }
	}

    /**
	 * @return the externalEvents
	 */
	public List<ExternalEventVO> getExternalEvents() {
		return this.externalEvents;
	}

	/**
	 * @param externalEvents the external events to set
	 */
	public void setExternalEvents(List<ExternalEventVO> externalEvents) {
		this.externalEvents = externalEvents;
	}

    public List<ActivityImplementorVO> getImplementors(){
        return this.implementors;
    }
    public void setImplementors(List<ActivityImplementorVO> imps){
        this.implementors = imps;
    }
    private ActivityImplementorVO getImplementor(ActivityVO activity) {
        if (implementors != null) {
            for (ActivityImplementorVO impl : implementors) {
                if (impl.getImplementorClassName().equals(activity.getImplementorClassName()))
                    return impl;
            }
        }
        return null;
    }

    public void addSubProcess(ProcessVO pSubProc){
        if(this.subProcesses == null) this.subProcesses = new ArrayList<ProcessVO>();
        this.subProcesses.add(pSubProc);
    }

    /**
     * Method that searches for the process at different levels
     * @param pSubProc
     */
     public ProcessVO searchSubProcess(ProcessVO pSubProc){
         if(this.equals(pSubProc)) return this;
         if(this.subProcesses == null)return null;
         for (ProcessVO vo : subProcesses) {
             ProcessVO result = vo.searchSubProcess(pSubProc);
             if(result != null) return result;
         }
         return null;
    }

    /**
     * Removes the sub process
     * @param pSubProc
     */
    public void removeSubProcess(ProcessVO pSubProc){
        if(this.equals(pSubProc)) return;
        if(this.subProcesses == null) return;
        this.subProcesses.remove(pSubProc);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProcessVO)) return false;
        ProcessVO pVO = (ProcessVO)obj;
        if (getId().longValue() != pVO.getProcessId().longValue()) return false;
        if (remoteServer==null) return pVO.remoteServer==null;
        else return remoteServer.equals(pVO.remoteServer);
    }

    public boolean equals(Long id, String server) {
        if (id.longValue()!=getId().longValue()) return false;
        if (remoteServer==null) return server==null;
        else return remoteServer.equals(server);
    }

    /**
     * Finds one work transition for this process matching the specified parameters
     * @param fromWorkId
     * @param eventType
     * @param completionCode
     * @return the work transition value object (or null if not found)
     */
    public WorkTransitionVO getWorkTransition(Long fromWorkId, Integer eventType, String completionCode) {
        WorkTransitionVO ret = null;
        for (WorkTransitionVO workTransitionVO : getTransitions()) {
            if (workTransitionVO.getFromWorkId().equals(fromWorkId)
                    && workTransitionVO.match(eventType, completionCode)) {
                if (ret == null) ret = workTransitionVO;
                else {
                    throw new IllegalStateException("Multiple matching work transitions when one expected:\n"
                            + " processId: " + getProcessId() + " fromWorkId: " + fromWorkId + " eventType: "
                            + eventType + "compCode: " + completionCode);
                }
            }
        }
        return ret;
    }

    public Integer getEventType() {
    	String subtype = getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
    	if (subtype==null) return EventType.ERROR;
    	else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS))
            return EventType.ERROR;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS))
            return EventType.ABORT;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_CORRECT_PROCESS))
            return EventType.CORRECT;
        else if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_DELAY_PROCESS))
            return EventType.DELAY;
        else return EventType.ERROR;
    }

    public ProcessVO findEmbeddedProcess(Integer eventType, String completionCode) {
    	if (this.subProcesses==null) return null;
    	for (ProcessVO subproc : subProcesses) {
    		if (eventType.equals(subproc.getEventType())) {
    			String entrycode = subproc.getAttribute(WorkAttributeConstant.ENTRY_CODE);
    			if (StringHelper.isEmpty(entrycode)) {
    				if (StringHelper.isEmpty(completionCode)) return subproc;
    			} else {
    				if (entrycode.equals(completionCode)) return subproc;
    			}
    		}
    	}
    	for (ProcessVO subproc : subProcesses) {
    		if (eventType.equals(subproc.getEventType())) {
    			String entrycode = subproc.getAttribute(WorkAttributeConstant.ENTRY_CODE);
    			if (StringHelper.isEmpty(entrycode)) return subproc;
    		}
    	}
    	return null;
    }

    /**
     * Finds the work transitions from the given activity
     * that match the event type and completion code.
     * A DEFAULT completion code matches any completion
     * code if and only if there is no other matches
     *
     * @param fromWorkId
     * @param eventType
     * @parame completionCode
     * @return the matching work transition value objects
     */
    public List<WorkTransitionVO> getWorkTransitions(Long fromWorkId, Integer eventType, String completionCode) {
    	List<WorkTransitionVO> allTransitions = getAllWorkTransitions(fromWorkId);
    	List<WorkTransitionVO> returnSet = getWorkTransitions(allTransitions, eventType, completionCode);
    	if (returnSet.size()>0) return returnSet;
    	// look for default transition
        boolean noLabelIsDefault = getTransitionWithNoLabel().equals(TRANSITION_ON_DEFAULT);
        if (noLabelIsDefault) returnSet = getWorkTransitions(allTransitions, eventType, null);
        else returnSet = getWorkTransitions(allTransitions, eventType, ActivityResultCodeConstant.RESULT_DEFAULT);
        if (returnSet.size()>0) return returnSet;
    	// look for resume transition
        if (eventType.equals(EventType.FINISH)) {
        	returnSet = new ArrayList<WorkTransitionVO>();
        	for (WorkTransitionVO trans : allTransitions) {
        		if (trans.getEventType().equals(EventType.RESUME)) returnSet.add(trans);
        	}
        }
        return returnSet;
    }

    public List<WorkTransitionVO> getAllWorkTransitions(Long fromWorkId) {
        List<WorkTransitionVO> allTransitions = new ArrayList<WorkTransitionVO>();
        for (WorkTransitionVO workTransitionVO : getTransitions()) {
            if (workTransitionVO.getFromWorkId().equals(fromWorkId)) {
                allTransitions.add(workTransitionVO);
            }
        }
        return allTransitions;
    }

    private List<WorkTransitionVO> getWorkTransitions(List<WorkTransitionVO> all,
    		Integer eventType, String compcode) {
    	List<WorkTransitionVO> set = new ArrayList<WorkTransitionVO>();
    	for (WorkTransitionVO trans : all) {
    		if (trans.match(eventType, compcode)) set.add(trans);
    	}
    	return set;
    }

    /**
     * Find a work transition based on its id value
     * @param workTransitionId
     * @return the matching transition VO, or null if not found
     */
    public WorkTransitionVO getWorkTransition(Long workTransitionId) {
        for (WorkTransitionVO workTransitionVO : getTransitions()) {
            if (workTransitionVO.getWorkTransitionId().equals(workTransitionId))
                return workTransitionVO;
        }
        return null; // not found
    }

    /**
     * Returns the value of a process attribute.
     * @param attrname
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public String getAttribute(String attrname) {
        return AttributeVO.findAttribute(attributes, attrname);
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param attrname
     * @param value
     */
    public void setAttribute(String attrname, String value) {
        if (attributes==null) attributes = new ArrayList<AttributeVO>();
        AttributeVO.setAttribute(attributes, attrname, value);
    }

    public List<Long> getDeletedTransitions() {
        return deletedTransitions;
    }

    public void clearDeletedTransitions() {
        deletedTransitions = null;
    }

    public void addDeletedTransitions(Long workTransID) {
        if(null == deletedTransitions)
            deletedTransitions = new ArrayList<Long>();
        deletedTransitions.add(workTransID);
    }

    @Override
    public ProcessVO getNextVersion() {
        return (ProcessVO)super.getNextVersion();
    }

    @Override
    public ProcessVO getPrevVersion() {
        return (ProcessVO)super.getPrevVersion();
    }

    public String getNewVersionString(boolean major) {
        int version = getNewVersion(major);
        return version/1000 + "." + version%1000;
    }

    public String getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(String v) {
        remoteServer = v;
    }

    public boolean isRemote() {
        return remoteServer!=null;
    }

    public String getRemoteName() {
        return getProcessName() + RemoteAccess.REMOTE_NAME_DELIMITER + remoteServer;
    }

    public boolean isEmbeddedProcess() {
        String procVisibility = getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
        return (procVisibility != null && procVisibility.equals(ProcessVisibilityConstant.EMBEDDED));
    }

    public boolean isEmbeddedExceptionHandler() {
        if (!isEmbeddedProcess()) return false;
        String subtype = getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
        return (subtype==null || subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS));
    }

    public ProcessVO findEmbeddedProcess(String type) {
        if (subProcesses==null) return null;
        for (ProcessVO subproc : subProcesses) {
            if (ProcessVisibilityConstant.EMBEDDED.equals(
                    subproc.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY))) {
                String subtype = subproc.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
                if (subtype==null) subtype = ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS;
                if (type.equals(subtype)) return subproc;
            }
        }
        return null;
    }

    public List<ActivityVO> getUpstreamActivities(Long activityId) {
        List<ActivityVO> upstreamActivities = new ArrayList<ActivityVO>();
        for (WorkTransitionVO workTransitionVO : getTransitions()) {
            if (workTransitionVO.getToWorkId().equals(activityId))
                upstreamActivities.add(getActivityVO(workTransitionVO.getFromWorkId()));
        }
        if (getSubProcesses() != null) {
            for (ProcessVO embeddedSubProc : getSubProcesses()) {
                for (WorkTransitionVO workTransitionVO : embeddedSubProc.getTransitions()) {
                    if (workTransitionVO.getToWorkId().equals(activityId))
                        upstreamActivities.add(embeddedSubProc.getActivityVO(workTransitionVO.getFromWorkId()));
                }
            }
        }
        return upstreamActivities;
    }

    public ActivityVO getActivity(String activityName) {
        for (ActivityVO activityVO : getActivities()) {
            if (activityVO.getActivityName().equals(activityName))
                return activityVO;
        }
        return null;
    }

    public ActivityVO getActivityByLogicalId(String logicalId) {
        for (ActivityVO activityVO : getActivities()) {
            if (activityVO.getLogicalId().equals(logicalId))
                return activityVO;
        }
        return null;
    }

    /**
     * Also searches subprocs.
     */
    public ActivityVO getActivityById(String logicalId) {
        for (ActivityVO activityVO : getActivities()) {
            if (activityVO.getLogicalId().equals(logicalId)) {
                activityVO.setProcessName(getName());
                return activityVO;
            }
        }
        for (ProcessVO subProc : this.subProcesses) {
            for (ActivityVO activityVO : subProc.getActivities()) {
                if (activityVO.getLogicalId().equals(logicalId)) {
                    activityVO.setProcessName(getName() + ":" + subProc.getName());
                    return activityVO;
                }
            }
        }

        return null;
    }

    public String getTransitionWithNoLabel() {
        String v = this.getAttribute(WorkAttributeConstant.TRANSITION_WITH_NO_LABEL);
        return (v==null)?TRANSITION_ON_NULL:v;
    }

    public String getProcessType() {
        String v = this.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
        return (v==null)?ProcessVisibilityConstant.REGULAR:v;
    }

	public String getLabel() {
	    return getProcessName() + " v" + getVersionString();
	}

	public static int versionFromString(String v) {
		int k = v.indexOf('.');
		int version;
		if (k>0) {
			version = Integer.parseInt(v.substring(0,k))*1000
				+ Integer.parseInt(v.substring(k+1));
		} else version = Integer.parseInt(v);
		return version;
	}

	public static String versionToString(int version) {
		return version/1000 + "." + version%1000;
	}

	public void removeDeletedTransitions() {
    	List<WorkTransitionVO> toDelete = new ArrayList<WorkTransitionVO>();
		for (WorkTransitionVO trans : this.transitions) {
			Changes changes = new Changes(trans.getAttributes());
			if (changes.getChangeType()==Changes.DELETE) {
				toDelete.add(trans);
			}
		}
		for (WorkTransitionVO trans : toDelete) {
			this.transitions.remove(trans);
		}
		if (this.getSubProcesses()==null) return;
		for (ProcessVO subproc : this.getSubProcesses()) {
			toDelete.clear();
			for (WorkTransitionVO trans : subproc.transitions) {
				Changes changes = new Changes(trans.getAttributes());
				if (changes.getChangeType()==Changes.DELETE) {
					toDelete.add(trans);
				}
			}
			for (WorkTransitionVO trans : toDelete) {
				subproc.transitions.remove(trans);
			}
		}
    }

	public boolean isLoaded() {
		return activities!=null;
	}

	public int getPerformanceLevel() {
        String v = this.getAttribute(WorkAttributeConstant.PERFORMANCE_LEVEL);
        try {
			return (v==null)?0:Integer.parseInt(v);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Force conversion to new process vo format:
	 * Differences:
	 *   - process_id is a rule_set_id
	 *   - activity_id/transition_id are logical ids
	 *   - variable id are not used (set to 0)
	 *   - transitions from main/sub process using 0L as from
	 *   - variable name/type are from variable instance comments field
	 *
	 */
	public void convertToUseLogicalIds() {
		if (isInRuleSet) return;
		isInRuleSet = true;
		HashMap<Long,Long> actIdMap = new HashMap<Long,Long>();
		HashMap<Long,Long> subprocIdMap = new HashMap<Long,Long>();
		Long newId;
		if (this.subProcesses!=null) {
			for (ProcessVO subproc : subProcesses) {
				newId = new Long(subproc.getAttribute(WorkAttributeConstant.LOGICAL_ID).substring(1));
				subprocIdMap.put(subproc.getProcessId(), newId);
				subproc.setProcessId(newId);
				for (ActivityVO act : subproc.activities) {
					newId = new Long(act.getLogicalId().substring(1));
					actIdMap.put(act.getActivityId(), newId);
					act.setActivityId(newId);
				}
				for (WorkTransitionVO trans : subproc.transitions) {
					if (trans.getFromWorkId().equals(subproc.getProcessId())) {	// hidden start transition
						trans.setFromWorkId(0L);
						trans.setWorkTransitionId(0L);
						trans.setToWorkId(actIdMap.get(trans.getToWorkId()));
					} else {
						newId = new Long(trans.getLogicalId().substring(1));
						trans.setWorkTransitionId(newId);
						trans.setFromWorkId(actIdMap.get(trans.getFromWorkId()));
						trans.setToWorkId(actIdMap.get(trans.getToWorkId()));
					}
				}
			}
		}
		for (ActivityVO act : activities) {
			newId = new Long(act.getLogicalId().substring(1));
			actIdMap.put(act.getActivityId(), newId);
			act.setActivityId(newId);
		}
		for (WorkTransitionVO trans : transitions) {
			if (trans.getFromWorkId().equals(getProcessId())) {	// hidden transitions
				trans.setFromWorkId(0L);
				trans.setWorkTransitionId(0L);
				if (trans.getEventType().equals(EventType.START))
					trans.setToWorkId(actIdMap.get(trans.getToWorkId()));
				else trans.setToWorkId(subprocIdMap.get(trans.getToWorkId()));
			} else {
				newId = new Long(trans.getLogicalId().substring(1));
				trans.setWorkTransitionId(newId);
				trans.setFromWorkId(actIdMap.get(trans.getFromWorkId()));
				trans.setToWorkId(actIdMap.get(trans.getToWorkId()));
			}
		}
		for (VariableVO var : variables) {
			var.setVariableId(0L);
		}
	}

	public boolean isInRuleSet() {
		return this.isInRuleSet;
	}

	public void setInRuleSet(boolean v) {
		this.isInRuleSet = v;
	}

	public void remove(ProcessVO processVO) {
        ProcessVO prev = null;
        while ((prev = getPrevVersion()) != null) {
            if (prev.equals(processVO)) {
                if (prev.getPrevVersion() != null) {
                    prev.getPrevVersion().setNextVersion(prev.getNextVersion());
                }
                if (prev.getNextVersion() != null) {
                    prev.getNextVersion().setPrevVersion(prev.getPrevVersion());
                }
                break;
            }
        }
        ProcessVO next = null;
        while ((next = getNextVersion()) != null) {
            if (next.equals(processVO)) {
                if (next.getPrevVersion() != null) {
                    next.getPrevVersion().setNextVersion(next.getNextVersion());
                }
                if (getNextVersion() != null) {
                    getNextVersion().setPrevVersion(next.getPrevVersion());
                }
                break;
            }
        }
	}

    public String getRenderingEngine() {
        String rendering = getAttribute(WorkAttributeConstant.RENDERING_ENGINE);
        if (rendering == null)
            rendering = PropertyManager.getProperty(PropertyNames.MDW_DEFAULT_RENDERING_ENGINE);
        if (rendering == null)
            rendering = WorkAttributeConstant.HTML5_RENDERING;
        return rendering;
    }

    public boolean isCompatibilityRendering() {
        return WorkAttributeConstant.COMPATIBILITY_RENDERING.equals(getRenderingEngine());
    }

    public ActivityVO getStartActivity() {
        if (isInRuleSet()) {
            for (ActivityVO activity : getActivities()) {
                ActivityImplementorVO impl = getImplementor(activity);
                if (impl != null && impl.isStart())
                    return activity;
            }
            // revert to old logic of assuming first activity in ruleset is start
            return getActivities().get(0);
        }
        else {
            WorkTransitionVO startTransition = getWorkTransition(getProcessId(), EventType.START, null);
            if (startTransition == null) {
                // at design time getTransitions() does not include process start hidden link - so infer start activity
                for (ActivityVO act : getActivities()) {
                    ActivityImplementorVO impl = getImplementor(act);
                    if (impl != null && (StartActivity.class.equals(impl.getBaseClassName()) || OLD_START_ACTIVITY_BASE_CLASS.equals(impl.getBaseClassName())))
                        return act;
                }
                // for backward compatibility only - fall back to activity icon shape
                for (ActivityVO act : getActivities()) {
                    ActivityImplementorVO impl = getImplementor(act);
                    if (impl != null && "shape:start".equals(impl.getIconName()))
                        return act;
                }
                return null;
            }
            else
                return getActivityVO(startTransition.getToWorkId());
        }
    }

    public List<ActivityVO> getDownstreamActivities(ActivityVO activity) {
        List<ActivityVO> downstreamActivities = new ArrayList<ActivityVO>();
        for (WorkTransitionVO transition : getAllWorkTransitions(activity.getActivityId()))
            downstreamActivities.add(getActivityVO(transition.getToWorkId()));
        return downstreamActivities;
    }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }

    public void applyOverrideAttributes(Map<String,String> overrideAttrs) {
        if (overrideAttrs != null) {
            for (String name : overrideAttrs.keySet()) {
                if (name.startsWith(WorkAttributeConstant.OVERRIDE_QUALIFIER)) {
                    int colon = name.indexOf(':');
                    String attrname = name.substring(colon + 1);
                    Long id = new Long(name.substring(WorkAttributeConstant.OVERRIDE_QUALIFIER.length(), colon));
                    if (name.startsWith(WorkAttributeConstant.OVERRIDE_TRANSITION)) {
                        WorkTransitionVO wt = getWorkTransition(id);
                        if (wt == null && getSubProcesses() != null) {
                            for (ProcessVO subproc : getSubProcesses()) {
                                wt = subproc.getWorkTransition(id);
                                if (wt != null)
                                    break;
                            }
                        }
                        if (wt != null)
                            wt.setAttribute(attrname, overrideAttrs.get(name));
                    }
                    else if (name.startsWith(WorkAttributeConstant.OVERRIDE_SUBPROC)) {
                        ProcessVO subproc = getSubProcessVO(id);
                        if (subproc != null)
                            subproc.setAttribute(attrname, overrideAttrs.get(name));
                    }
                    else {
                        ActivityVO act = getActivityVO(id);
                        if (act == null && getSubProcesses() != null) {
                            for (ProcessVO subproc : getSubProcesses()) {
                                act = subproc.getActivityVO(id);
                                if (act != null)
                                    break;
                            }
                        }
                        if (act != null)
                            act.setAttribute(attrname, overrideAttrs.get(name));
                    }
                }
                else {
                    setAttribute(name, overrideAttrs.get(name));
                }
            }
        }
        overrideAttributesApplied = true;
    }

    public Map<String,String> getOverrideAttributes() {
        if (!isInRuleSet())
            throw new UnsupportedOperationException("Override attributes not supported for non-asset process " + getLabel());

        Map<String,String> overrideAttrs = new HashMap<String,String>();
        if (getAttributes() != null) {
            for (AttributeVO attr : getAttributes()) {
                if (WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName()))
                    overrideAttrs.put(attr.getAttributeName(), attr.getAttributeValue());
            }
        }
        if (getActivities() != null) {
            for (ActivityVO activity : getActivities())
                overrideAttrs.putAll(getSubOverrideAttributes(activity.getAttributes(), OwnerType.ACTIVITY, activity.getActivityId()));
        }
        if (getTransitions() != null) {
            for (WorkTransitionVO transition : getTransitions())
                overrideAttrs.putAll(getSubOverrideAttributes(transition.getAttributes(), OwnerType.WORK_TRANSITION, transition.getWorkTransitionId()));
        }
        if (getSubProcesses() != null) {
            for (ProcessVO subproc : getSubProcesses()) {
                overrideAttrs.putAll(getSubOverrideAttributes(subproc.getAttributes(), OwnerType.PROCESS, subproc.getId()));
                if (subproc.getActivities() != null) {
                    for (ActivityVO activity : subproc.getActivities())
                        overrideAttrs.putAll(getSubOverrideAttributes(activity.getAttributes(), OwnerType.ACTIVITY, activity.getActivityId()));
                }
                if (subproc.getTransitions() != null) {
                    for (WorkTransitionVO trans : subproc.getTransitions())
                        overrideAttrs.putAll(getSubOverrideAttributes(trans.getAttributes(), OwnerType.WORK_TRANSITION, trans.getWorkTransitionId()));
                }
            }
        }

        return overrideAttrs;
    }

    private Map<String,String> getSubOverrideAttributes(List<AttributeVO> attrs, String subType, Long subId) {
        Map<String,String> overrideAttrs = new HashMap<String,String>();
        if (attrs != null) {
            for (AttributeVO attr : attrs) {
                if (WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName())) {
                    String name = WorkAttributeConstant.getOverrideAttributeName(attr.getAttributeName(), subType, String.valueOf(subId));
                    overrideAttrs.put(name, attr.getAttributeValue());
                }
            }
        }
        return overrideAttrs;
    }

    public void removeEmptyAndOverrideAttributes() {
        setAttributes(removeEmptyAndOverrideAttrs(getAttributes()));
        if (getActivities() != null) {
            for (ActivityVO activity : getActivities())
                activity.setAttributes(removeEmptyAndOverrideAttrs(activity.getAttributes()));
        }
        if (getTransitions() != null) {
            for (WorkTransitionVO trans : getTransitions())
                trans.setAttributes(removeEmptyAndOverrideAttrs(trans.getAttributes()));
        }
        if (getSubProcesses() != null) {
            for (ProcessVO embedded : getSubProcesses())
                embedded.removeEmptyAndOverrideAttributes();
        }
    }

    /**
     * Also removes these junk attributes: REFERENCED_ACTIVITIES, REFERENCED_PROCESSES.
     */
    private List<AttributeVO> removeEmptyAndOverrideAttrs(List<AttributeVO> attrs) {
        if (attrs == null)
            return null;
        List<AttributeVO> toKeep = new ArrayList<AttributeVO>();
        for (AttributeVO attr : attrs) {
            if (attr.getAttributeName() != null && attr.getAttributeName().trim().length() > 0 &&
                    attr.getAttributeValue() != null && attr.getAttributeValue().trim().length() > 0 &&
                    !WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName()) &&
                    !attr.getAttributeName().equals("REFERENCED_ACTIVITIES") &&
                    !attr.getAttributeName().equals("REFERENCED_PROCESSES")) {
                toKeep.add(attr);
            }
        }
        return toKeep;
    }

    public void removeEmptyAttributes() {
        setAttributes(removeEmptyAttrs(getAttributes()));
        if (getActivities() != null) {
            for (ActivityVO activity : getActivities())
                activity.setAttributes(removeEmptyAttrs(activity.getAttributes()));
        }
        if (getTransitions() != null) {
            for (WorkTransitionVO trans : getTransitions())
                trans.setAttributes(removeEmptyAttrs(trans.getAttributes()));
        }
        if (getSubProcesses() != null) {
            for (ProcessVO embedded : getSubProcesses())
                embedded.removeEmptyAttributes();
        }
    }

    private List<AttributeVO> removeEmptyAttrs(List<AttributeVO> attrs) {
        if (attrs == null)
            return null;
        List<AttributeVO> toKeep = new ArrayList<AttributeVO>();
        for (AttributeVO attr : attrs) {
            if (attr.getAttributeValue() != null && attr.getAttributeValue().trim().length() > 0)
                toKeep.add(attr);
        }
        return toKeep;
    }

    private boolean overrideAttributesApplied;
    public boolean overrideAttributesApplied() { return overrideAttributesApplied; }
}
