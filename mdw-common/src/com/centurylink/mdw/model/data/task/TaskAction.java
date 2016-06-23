/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.task;

import java.io.Serializable;
import java.util.Arrays;

import com.centurylink.mdw.model.value.user.UserRoleVO;

/**
 * Task Action model object.  Task actions are no longer constrained to a constant
 * set of values declared here.  Non-standard actions are governed by the result codes
 * of the outgoing transitions from the task creation activity.
 *
 */
public class TaskAction implements Serializable,Comparable<TaskAction> {

	private static final long serialVersionUID = 1L;
	// standard task actions
    public static final String CREATE = "Create";
    public static final String ASSIGN = "Assign";
    public static final String CLAIM = "Claim";
    public static final String RELEASE = "Release";
    public static final String CANCEL = "Cancel";
    public static final String COMPLETE = "Complete";
    public static final String RETRY = "Retry";
    public static final String FORWARD = "Forward";
    public static final String ABORT = "Abort";
    public static final String WORK = "Work";
    public static final String SAVE = "Save";
    public static final String CALLBACK = "Callback";	// for making a request/response call to the engine

    public static final String[] STANDARD_ACTIONS = { CREATE, ASSIGN, CLAIM, RELEASE, CANCEL, COMPLETE, RETRY, FORWARD, ABORT, WORK, SAVE };

    private boolean dynamic;
    private String taskActionName;
	private Long taskActionId;
	private UserRoleVO[] userRoles;
	private String alias;
	private boolean requireComment;
	private String outcome;

	public String getAlias() { return alias; }
	public void setAlias(String s) { alias = s; }

	public boolean isRequireComment() { return requireComment; }
	public void setRequireComment(boolean b) { this.requireComment = b; }

    /**
     * Override standard action outcome
     */
    public String getOutcome() { return outcome; }
    public void setOutcome(String s) { this.outcome = s; }

    public boolean isDynamic() { return dynamic; }
    public void setDynamic(boolean b) { dynamic = b; }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof TaskAction))
            return false;
        TaskAction taskAction = (TaskAction) o;
        if (taskAction.getTaskActionName() == null)
            return getTaskActionName() == null;
        return (taskAction.getTaskActionName().equals(getTaskActionName()));
    }

    public int compareTo(TaskAction other)
    {
      return this.getLabel().compareTo(other.getLabel());
    }

    /**
     * Returns the TaskActionName of  the user
     * @return String
     *
     */
    public String getTaskActionName() {
        return taskActionName;
    }

    public String getLabel() {
        return alias == null ? taskActionName : alias;
    }

    /**
     * Sets the TaskActionName of the USer
     * @param pTaskActionName
     */
    public void setTaskActionName(String pTaskActionName){
        this.taskActionName = pTaskActionName;
    }
	public Long getTaskActionId() {
		return taskActionId;
	}
	public void setTaskActionId(Long taskActionId) {
		this.taskActionId = taskActionId;
	}
	public UserRoleVO[] getUserRoles() {
		return userRoles;
	}
	public void setUserRoles(UserRoleVO[] userRoles) {
		this.userRoles = userRoles;
	}

	public static String fixCase(String action) {
	    if (action == null)
	        return null;
	    if (action.equalsIgnoreCase(TaskAction.CANCEL) || action.equalsIgnoreCase(TaskAction.RETRY) || action.equalsIgnoreCase(TaskAction.FORWARD))
	        return action.substring(0, 1).toUpperCase() + action.substring(1).toLowerCase();
	    else
	        return action;
	}

	public static boolean isStandardAction(String action) {
	    for (String stdAction : STANDARD_ACTIONS) {
	        if (stdAction.equals(action))
	            return true;
	    }
	    return false;
	}

	/**
	 * TODO: Drive this from rules in MDWTaskActions.xml (requires major refactoring -- use JAXB).
	 */
	public static boolean isCompleting(String action) {
	    return COMPLETE.equals(action) || !isStandardAction(action);
	}

    /**
     * Checked if the passed in RoleId is mapped to this TaskAction
     * @param pRoleId
     * @return boolean results
     */
    public boolean isRoleMapped(Long pRoleId){
       if(userRoles == null || userRoles.length == 0){
            return false;
        }
        for(int i=0; i<userRoles.length; i++){
            if(userRoles[i].getId().longValue() == pRoleId.longValue()){
                return true;
            }
        }
        return false;
    }

	/**
		 *
		 * @return
		 * @author
		 */
    public String toString() {
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("TaskActionVO[");
    	buffer.append("taskActionId = ").append(taskActionId);
    	buffer.append(" taskActionName = ").append(taskActionName);
    	if (userRoles == null) {
    		buffer.append(" userRoles = ").append("null");
    	} else {
    		buffer.append(" userRoles = ").append(
    				Arrays.asList(userRoles).toString());
    	}
    	buffer.append("]");
    	return buffer.toString();
    }



}
