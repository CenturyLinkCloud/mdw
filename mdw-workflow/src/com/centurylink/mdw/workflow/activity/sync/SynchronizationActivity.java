/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.services.process.ProcessExecuter;
import com.centurylink.mdw.synchronization.SyncExpressionEvaluator;
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
    		EventWaitInstanceVO received = registerWaitEvents(false, true);
    		if (received!=null)
    			resume(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
    	}
    }

    protected boolean checkIfSynchronized() throws ActivityException {
    	String syncExpression = getAttributeValue(WorkAttributeConstant.SYNC_EXPRESSION);
    	boolean yes;
        try {
        	ProcessExecuter engine = getEngine();
        	ProcessVO procdef = getProcessDefinition();
    		Map<String,String> idToEscapedName = new HashMap<String,String>();	// for backward compatibility
        	List<WorkTransitionVO> incomingTransitions =
        		getIncomingTransitions(procdef, this.getActivityId(), idToEscapedName);
        	engine.determineCompletedTransitions(getProcessInstanceId(), incomingTransitions);
        	if (StringHelper.isEmpty(syncExpression)) {
        		yes = true;
        		for (WorkTransitionVO one : incomingTransitions) {
        			if (!one.getEventType().equals(EventType.FINISH)) {
        				yes = false;
        			}
        		}
        	} else {
        		String[] syncedActivityIds = new String[incomingTransitions.size()];
        		List<String> completedActivities = new ArrayList<String>();
        		for (int i=0; i<syncedActivityIds.length; i++) {
        			WorkTransitionVO sync = incomingTransitions.get(i);
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
    private List<WorkTransitionVO> getIncomingTransitions(ProcessVO procdef, Long activityId,
    		Map<String,String> idToEscapedName) {
    	List<WorkTransitionVO> incomingTransitions = new ArrayList<WorkTransitionVO>();
        for (WorkTransitionVO trans : procdef.getTransitions()) {
            if (trans.getToWorkId().equals(activityId)) {
            	WorkTransitionVO sync = new WorkTransitionVO();
            	sync.setWorkTransitionId(trans.getWorkTransitionId());
            	ActivityVO act = procdef.getActivityVO(trans.getFromWorkId());
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

    private boolean isOtherEvent(InternalEventVO eventMessageDoc) {
    	return OwnerType.DOCUMENT.equals(eventMessageDoc.getSecondaryOwnerType());
    }

	public final boolean resume(InternalEventVO eventMessageDoc)
			throws ActivityException {
		TransactionWrapper transaction = null;
		try {
			transaction = startTransaction();
	    	Integer status = super.lockActivityInstance();
	    	if (!status.equals(WorkStatus.STATUS_WAITING)) {
	    		this.setReturnCode("true");		// this is only used when !isSynchronized
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

	public boolean resumeWaiting(InternalEventVO eventMessageDoc)
			throws ActivityException {
		// check if it is synchronized at this time
		TransactionWrapper transaction = null;
		try {
			transaction = startTransaction();
	    	super.lockActivityInstance();
	    	isSynchronized = this.checkIfSynchronized();
	    	if (isSynchronized) return true;
			EventWaitInstanceVO received = registerWaitEvents(true, true);
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