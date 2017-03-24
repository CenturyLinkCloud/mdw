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
package com.centurylink.mdw.workflow.activity.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.sync.SyncExpressionEvaluator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractWait;

/**
 * Synchronization gateway activity.
 */
@Tracked(LogLevel.TRACE)
public class SynchronizationActivity extends AbstractWait implements com.centurylink.mdw.activity.types.SynchronizationActivity {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final char UNDERSCORE = '_';

    private boolean isSynchronized;

     /**
     * Executes the controlled activity
     * @throws ActivityException
     */
    public  void  execute() throws ActivityException{
        isSynchronized = checkIfSynchronized();
        if (!isSynchronized) {
            EventWaitInstance received = registerWaitEvents(false, true);
            if (received!=null)
                resume(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
        }
    }

    protected boolean checkIfSynchronized() throws ActivityException {
        String syncExpression = getAttributeValue(WorkAttributeConstant.SYNC_EXPRESSION);
        boolean yes;
        try {
            ProcessExecutor engine = getEngine();
            Process procdef = getProcessDefinition();
            Map<String,String> idToEscapedName = new HashMap<String,String>();    // for backward compatibility
            List<Transition> incomingTransitions =
                getIncomingTransitions(procdef, this.getActivityId(), idToEscapedName);
            engine.determineCompletedTransitions(getProcessInstanceId(), incomingTransitions);
            if (StringHelper.isEmpty(syncExpression)) {
                yes = true;
                for (Transition one : incomingTransitions) {
                    if (!one.getEventType().equals(EventType.FINISH)) {
                        yes = false;
                    }
                }
            } else {
                String[] syncedActivityIds = new String[incomingTransitions.size()];
                List<String> completedActivities = new ArrayList<String>();
                for (int i=0; i<syncedActivityIds.length; i++) {
                    Transition sync = incomingTransitions.get(i);
                    syncedActivityIds[i] = sync.getCompletionCode();
                    if (sync.getEventType().equals(EventType.FINISH))
                        completedActivities.add(syncedActivityIds[i]);
                }
                SyncExpressionEvaluator syncExpressionEval =
                    new SyncExpressionEvaluator(syncedActivityIds, syncExpression, idToEscapedName);
                yes = syncExpressionEval.evaluate(completedActivities, getParameters());
            }
            return yes;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Replaces space characters in the activity name with underscores.
     * @param rawActivityName
     * @return the escaped activity name
     */
    private String escape(String rawActivityName) {
        boolean lastIsUnderScore = false;
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<rawActivityName.length(); i++) {
            char ch = rawActivityName.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch);
                lastIsUnderScore = false;
            } else if (!lastIsUnderScore) {
                sb.append(UNDERSCORE);
                lastIsUnderScore = true;
            }
        }
        return sb.toString();
    }

    /**
     * reuse WorkTransitionVO for sync info
     */
    private List<Transition> getIncomingTransitions(Process procdef, Long activityId,
            Map<String,String> idToEscapedName) {
        List<Transition> incomingTransitions = new ArrayList<Transition>();
        for (Transition trans : procdef.getTransitions()) {
            if (trans.getToWorkId().equals(activityId)) {
                Transition sync = new Transition();
                sync.setWorkTransitionId(trans.getWorkTransitionId());
                Activity act = procdef.getActivityVO(trans.getFromWorkId());
                String logicalId = act.getLogicalId();
                // id to escaped name map is for backward compatibility
                idToEscapedName.put(logicalId, escape(act.getActivityName()));
                sync.setCompletionCode(logicalId);
                incomingTransitions.add(sync);
            }
        }
        return incomingTransitions;
    }

    /**
     * synchronize a condition and returns the result of the evaluation.
     * @return boolean
     */
     public boolean isSynchronized() throws ActivityException {
         return this.isSynchronized;
     }

    private boolean resume(String message, String completionCode) throws ActivityException {
        // process non-sync message
        this.setReturnCode(completionCode);
        processOtherMessage(message);
        handleEventCompletionCode();
        return true;
    }

    /**
     * This method is invoked to process a received event (other than an incoming transition is made).
     * You will need to override this method to customize processing of the event.
     *
     * The default method does nothing.
     *
     * The status of the activity after processing the event is configured in the designer, which
     * can be either Hold or Waiting.
     *
     * When you override this method, you can optionally set different completion
     * code from those configured in the designer by calling setReturnCode().
     *
     * @param messageString the entire message content of the external event (from document table)
     * @throws ActivityException
     */
    protected void processOtherMessage(String messageString)
        throws ActivityException {
    }

    private boolean isOtherEvent(InternalEvent eventMessageDoc) {
        return OwnerType.DOCUMENT.equals(eventMessageDoc.getSecondaryOwnerType());
    }

    public final boolean resume(InternalEvent eventMessageDoc)
            throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            Integer status = super.lockActivityInstance();
            if (!status.equals(WorkStatus.STATUS_WAITING)) {
                this.setReturnCode("true");        // this is only used when !isSynchronized
                return false;
            } else if (isOtherEvent(eventMessageDoc)) {
                 String messageString = this.getMessageFromEventMessage(eventMessageDoc);
                 return resume(messageString, eventMessageDoc.getCompletionCode());
            } else {
                this.setReturnCode(null);
                isSynchronized = this.checkIfSynchronized();
                if (isSynchronized) {
                    try {
                        // Need to set status complete earlier, so that other threads can see it.
                        // will be set to complete status again in completeActivityInstance(), but that is fine
                        this.getEngine().setActivityInstanceStatus(this.getActivityInstance(),
                                WorkStatus.STATUS_COMPLETED, null);
                    } catch (Exception e) {
                        super.logexception("Failed to set activity instance status complete in Synchronization activity", e);
                    }
                    super.deregisterEvents();
                }
                return isSynchronized;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean resumeWaiting(InternalEvent eventMessageDoc)
            throws ActivityException {
        // check if it is synchronized at this time
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            super.lockActivityInstance();
            isSynchronized = this.checkIfSynchronized();
            if (isSynchronized) return true;
            EventWaitInstance received = registerWaitEvents(true, true);
            if (received!=null) {
                 boolean done = resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                         received.getCompletionCode());
                 if (done) eventMessageDoc.setCompletionCode(this.getReturnCode());
                 return done;
             } else {
                 return false;
             }
        } finally {
            stopTransaction(transaction);
        }
    }

    @Override
    public boolean needSuspend() {
        return !isSynchronized;
    }


}