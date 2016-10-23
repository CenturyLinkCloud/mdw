/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.event.InternalEvent;


/**
 * This interface must be implemented by all activities that is suspendible
 * (may be put in WAITING status). This includes timer wait, event wait,
 * synchronization, process invocation, synchronous JMS/MQ adapters, and manual tasks.
 *
 * The activity will be resumed from some external event and the engine will
 * invoke the resume() method defined here.
 */
public interface SuspendibleActivity {

    /**
     * The method is invoked by the engine right after the activity is executed,
     * to determine whether to put the activity instance in waiting status.
     * Not all suspendible activities need to be suspended. For example, asynchronous process
     * invocation, event wait when the event is already arrived, etc.
     * @return true if the activity instance really needs to be suspended (put
     * in WAITING status).
     * @throws ActivityException
     */
    public boolean needSuspend()
    throws ActivityException;

    /**
     * The method is invoked when the wait activity is resumed.
     *
     * The resume may be a result of receiving external events,
     * either after or before the registration for wait.
     * For MDW 4 style event wait, the secondary owner is always EXTERNAL_EVENT_INSTANCE,
     * so you can access the external message received by calling getExternalEventInstanceDetails().
     * For MDW 3 style events, the secondary owner is not an external event instance,
     * and the external message is not directly accessible (the main problem we made the change).
     *
     * The method should normally return true, which causes
     * the activity to finish. In case further wait is needed,
     * the activity should register event and return false.
     *
     * When the method returns true, the engine will complete the activity and move on.
     * By default, the transition is determined based on null completion code and
     * FINISH event type. You can change the completion code by using the method
     * this.setReturnCode(). You can also change the event type from FINISH
     * to ABORT, CORRECT and DELAY, by prepending the completion code with
     * "ABORT:", "CORRECT:" and "DELAY:", respectively. Examples are
     * "ABORT:my-return-code" and "CORRECT:".
     *

     * @return true normally, false when the activity need to be put back in waiting status
     *
     *
     */
    boolean resume(InternalEvent eventMessageDoc)
    throws ActivityException;

    /**
     * The method is invoked when the wait activity was put on hold status
     * (due to time out for event wait activities, or receiving non-task events
     * for manual task activities).
     *
     * The method normally should put the activity back to wait status.
     * It also needs to check if the events it is waiting for are already arrived,
     * in which case it should invoke resume() to handle it.
     *
     * The method should return true if there is no need to wait any more (event
     * already arrived and handled), or return false to stay waiting status.
     *
     * The event message in this case does not contain any useful information
     * for resuming waiting,
     * but information there may be needed to construct the parameter calling resume()
     * in case the event is already arrived.
     *
     */
    boolean resumeWaiting(InternalEvent event)
    throws ActivityException;

}
