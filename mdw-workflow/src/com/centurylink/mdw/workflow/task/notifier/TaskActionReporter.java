/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.notifier;

import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.observer.task.TaskInstanceObserver;

/**
 * Convenience base class provides no-op defaults for all the notify methods.
 *
 */
public abstract class TaskActionReporter implements TaskInstanceObserver{

    private static final long serialVersionUID = 2160364868210292321L;

    public void notifyAlert(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {

    }

    public void notifyAssigned(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyCancellation(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyCompletion(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyCreation(Long taskInstId, Long taskInstOwnerId)
	    throws ObserverException {
    }

    public void notifyDataChange(Long taskInstId, Long taskInstOwnerId)
	    throws ObserverException {
    }

    public void notifyHold(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyJeopardy(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyRelease(Long taskInstId, Long taskInstOwnerId,
	    Integer prevStatus, Integer prevState) throws ObserverException {
    }

    public void notifyInProgress(Long taskInstId, Long taskInstOwnerId,
        Integer prevStatus, Integer prevState) throws ObserverException {
    }
    
    public void notifyForward(Long taskInstId, Long taskInstOwnerId, Integer previousStatus, 
        Integer previousState, String destination, String previousWorkgroups, Long previousAssigneeUserId) throws ObserverException {
    }
    
}
