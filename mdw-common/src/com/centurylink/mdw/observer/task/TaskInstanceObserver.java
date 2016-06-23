/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;


import com.centurylink.mdw.common.exception.ObserverException;

import java.io.Serializable;

/**
 * Interface that defines the structure for all the classes for which
 * the data will be fetched from a diff source
 *
 */
@Deprecated
public interface TaskInstanceObserver extends Serializable{

    /**
     * Method that notifies the observer that the TaskInstance has been created
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @throws ObserverException
     */
    public void notifyCreation(Long pTaskInstId, Long pTaskInstOwnerId)
     throws ObserverException;

     /**
	  * Method that notifies the observer that the TaskInstance has changed
	  * @param pTaskInstId
	  * @param pTaskInstOwnerId
	  * @throws ObserverException
	  */
	  public void notifyDataChange(Long pTaskInstId, Long pTaskInstOwnerId)
     throws ObserverException;

     /**
     * Method that notifies the observer that the TaskInstance has been claimed
     * @param pTaskInstId
     * @param pOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
     */
    public void notifyAssigned(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

      /**
     * Method that notifies the observer that the TaskInstance has been cancelled
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @throws ObserverException
     */
    public void notifyCancellation(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

      /**
     * Method that notifies the observer that the TaskInstance has been claimed
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
     */
    public void notifyCompletion(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

    /**
     * Method that notifies the observer that the TaskInstance has been released
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
     */
    public void notifyRelease(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

    /**
     * Method that notifies the observer that the TaskInstance has been forwarded
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
     */
    public void notifyForward(Long taskInstId, Long taskInstOwnerId, Integer previousStatus, Integer previousState, String destination, String previousGroups, Long previousAssigneeUserId)
     throws ObserverException;

    /**
     * Method that notifies the observer that the TaskInstance has been set to In Progress
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
     */
    public void notifyInProgress(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

      /**
     * Method that notifies the observer that the TaskInstance has been put on hold
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
      */
    public void notifyHold(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

      /**
     * Method that notifies the observer that the TaskInstance has been moved to alert state
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
      */
    public void notifyAlert(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;

      /**
     * Method that notifies the observer that the TaskInstance has moved to jeopardy state
     * @param pTaskInstId
     * @param pTaskInstOwnerId
     * @param pPrevStatus
     * @param pPrevState
     * @throws ObserverException
      */
    public void notifyJeopardy(Long pTaskInstId, Long pTaskInstOwnerId, Integer pPrevStatus, Integer pPrevState)
     throws ObserverException;
}
