/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.attribute.AttributeDefinition;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.CompletionCode;
import com.centurylink.mdw.services.task.WaitingForMe;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.qwest.mbeng.MbengNode;


public abstract class CompositeSynchronousAdapterBase extends PoolableAdapterBase implements SuspendibleActivity {

	protected static final String PROP_RESPONSE_TIMEOUT = "ResponseTimeout";
	private String stub_response;

	@Override
	public void execute() throws ActivityException {
		int response_timeout = this.getTimeoutForRealResponse();
		// stubbing is supported in service processes for real responses,
		// but for acknowledgment in regular processes (waitAsynchronously)
		if (this.waitAsynchronously()) {
			registerWaitEvents(false, false);
    		if (response_timeout > 0) {
    			if (logger.isDebugEnabled()) logger.debug("Set timeout " + response_timeout + " seconds");
        		InternalEvent delayMsg = InternalEvent.createActivityDelayMessage(getActivityInstance(),
        				this.getMasterRequestId());
        		try {
					getEngine().sendDelayedInternalEvent(delayMsg, response_timeout,
							ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId()+"timeout", false);
				} catch (MDWException e) {
					throw new ActivityException(0, "Failed to set timeout", e);
				}
    		}
			super.execute();
		} else {
			if (getEngine().getPerformanceLevel()<=6) {
				registerWaitEvents(false, false);
			}
			List<String[]> eventSpecs = this.getWaitEventSpecs();
			if (eventSpecs.isEmpty()) throw new ActivityException("No event is registered to listen to response");
			String[] eventNames = new String[eventSpecs.size()];
			for (int i=0; i<eventNames.length; i++) {
				eventNames[i] = translatePlaceHolder(eventSpecs.get(i)[0]);
			}
			super.execute();
			if (isStubbing) {
				processResponse(stub_response);
			} else {
				WaitingForMe waiter = new WaitingForMe(eventNames);
				if (response_timeout<=0) response_timeout = 120;
				// do not want to occupy a thread indefinitely when timeout value is not specified
				String responseData = waiter.waitForEvent(response_timeout);
				if (responseData==null) {
					throw new ActivityException("Activity times out after waiting " + response_timeout
							+ " seconds for response");
				}
				processResponse(responseData);
			}
		}
	}

	/**
	 * When the method returns true, the activity is put in waiting status, and events
	 * are registered for receiving asynchronous responses through standard event wait/registration
	 * mechanism.
	 * When the method returns false, the activity is waiting directly in the threads,
	 * and the event arrival must be notified using event handler NotifySynchronousAdapterHandler.
	 *
	 * The default implementation of the method returns true for regular processes, and false
	 * for service processes. If you override this method, you can return false for regular processes,
	 * but should never return true for service processes.
	 *
	 * @return see above
	 */
	protected boolean waitAsynchronously() {
		return !getEngine().isInService();
	}

	@Override
	public final boolean needSuspend() throws ActivityException {
		return waitAsynchronously();
	}

	/**
	 * The method returns the timeout value in seconds for waiting for real responses,
	 * whereas the method getTimeoutForResponse() returns timeout for acknowledgment
	 * (only when the underlying protocol for sending request is fundamentally synchronous
	 * such as web services).
	 *
	 * @return see above
	 */
    protected int getTimeoutForRealResponse() {
    	String timeout_s = null;
    	int timeout;
    	try {
    		timeout_s = this.getAttributeValueSmart(PROP_RESPONSE_TIMEOUT);
			timeout = timeout_s==null?-1:Integer.parseInt(timeout_s);
		} catch (NumberFormatException e) {
			logger.severeException("Cannot parse timeout value " + timeout_s, e);
			timeout = -1;
		} catch (PropertyException e) {
			logger.severeException("Cannot read timeout attribute " + PROP_RESPONSE_TIMEOUT, e);
			timeout = -1;
		}
		return timeout;
    }


	/**
	 * Called when receiving acknowledgment. It is overridden so that it does not
	 * save the acknowledgment in response variable, and really does nothing.
	 *
	 * You can override this method again to check out valid acknowledgment, for instance.
	 */
	@Override
	public void onSuccess(String response)
	throws ActivityException, ConnectionException, AdapterException {
		if (isStubbing) stub_response = response;
	}

	@Override
	public final boolean resume(InternalEvent eventMessageDoc)
			throws ActivityException {
		String responseData = getMessageFromEventMessage(eventMessageDoc);
		if (!StringHelper.isEmpty(responseData) && doLogging()) logMessage(responseData, true);
		setReturnCodeAndExitStatus(eventMessageDoc.getCompletionCode());
		return processResponse(responseData);
	}

	// copied from WaitActivity
    private String getMessageFromEventMessage(InternalEvent eventMessageDoc)
		throws ActivityException {
		if (eventMessageDoc.getParameters()!=null) {
			String msg = eventMessageDoc.getParameters().get("ExternalEventMessage");
			if (msg!=null) return msg;
		}
		Long extEventInstId = eventMessageDoc.getSecondaryOwnerId();
		return this.getExternalEventInstanceDetails(extEventInstId);
	}

	// copied from EventWaitActivity
    private void setReturnCodeAndExitStatus(String code) {
    	CompletionCode compcode = new CompletionCode();
    	compcode.parse(code);
    	if (compcode.getEventType().equals(EventType.FINISH)) {
    		setReturnCode(compcode.getCompletionCode());
    	} else {
    		setReturnCode(compcode.getEventTypeName() + ":" +
    				(compcode.getCompletionCode()==null?"":compcode.getCompletionCode()));
    	}
    }

    /**
     * Not currently implemented for listening to unsolicited events.
     * If we do need that, copy the code from event wait activity.
     */
    public boolean resumeWaiting(InternalEvent eventMessageDoc) throws ActivityException {
    	return false;
    }

	@Override
	protected final boolean canBeSynchronous() {
		return true;
	}

	@Override
	protected final boolean canBeAsynchronous() {
		return false;
	}

	// copied from ControlledWaitActivityImpl.
    private List<String[]> getWaitEventSpecs() {
        String attVal = this.getAttributeValue(WorkAttributeConstant.WAIT_EVENT_NAMES);
        if (attVal==null) return new ArrayList<String[]>();
        return StringHelper.parseTable(attVal, ',', ';', 3);
    }

	// copied from ControlledWaitActivityImpl.
	protected EventWaitInstance registerWaitEvents(boolean reregister, boolean check_if_arrvied)
		throws ActivityException {
		List<String[]> eventSpecs = this.getWaitEventSpecs();
		if (eventSpecs.isEmpty()) return null;
		String[] eventNames = new String[eventSpecs.size()];
		String[] eventCompletionCodes = new String[eventSpecs.size()];
		boolean[] eventOccurances = new boolean[eventSpecs.size()];
		for (int i=0; i<eventNames.length; i++) {
			eventNames[i] = translatePlaceHolder(eventSpecs.get(i)[0]);
			eventCompletionCodes[i] = eventSpecs.get(i)[1];
			if (StringHelper.isEmpty(eventCompletionCodes[i]))
				eventCompletionCodes[i] = EventType.EVENTNAME_FINISH;
			String eventOccur = eventSpecs.get(i)[2];
			eventOccurances[i] = (eventOccur==null || eventOccur.length()==0
					|| eventOccur.equalsIgnoreCase("true"));
		}
		try {
			EventWaitInstance received = getEngine().createEventWaitInstances(
					this.getActivityInstanceId(),
					eventNames,
					eventCompletionCodes,
					eventOccurances, !check_if_arrvied);
			return received;
		} catch (Exception ex) {
			super.logexception(ex.getMessage(), ex);
			throw new ActivityException(ex.getMessage());
		}
	}

    /**
     * You may override this method to process real response messages received asynchronously.
     *
     * The default method binds the response message to the response variable if it is specified,
     * and returns true.
     *
     * When the method returns true, the engine will complete the activity and move on.
     * By default, the transition is determined based on the completion code specified
     * in the event wait registration. You can override that by using the method
     * this.setReturnCode().
     *
     * If the method returns false, the activity will keep waiting for additional messages.
     * The work flow does not transition away and the completion code is ignored.
     *
     * When waitAsynchronously() returns false (for service processes if you do not
     * override that method), the return value is ignored and assumed to be true.
     *
     * @return see above
     * @param response the actual response message
     * @throws ActivityException
     */
    protected boolean processResponse(String response)
    	throws ActivityException {
        String varname = this.getAttributeValue(RESPONSE_VARIABLE);
        if (varname==null) return true;
        String vartype = this.getParameterType(varname);
        if (VariableTranslator.isDocumentReferenceVariable(vartype))
            this.setParameterValueAsDocument(varname, vartype, response);
        else this.setParameterValue(varname, response);
    	return true;
    }

    protected void getDescriptorCommon(AttributeDefinition attrdef) {
    	MbengNode node;
    	attrdef.defineDROPDOWN(REQUEST_VARIABLE, "Request Variable",  AttributeDefinition.SOURCE_VARIABLES, 120);
    	attrdef.defineDROPDOWN(RESPONSE_VARIABLE, "Response Variable", AttributeDefinition.SOURCE_VARIABLES, 120);
    	attrdef.defineBoolean(DO_LOGGING, "Log req/resp", true);
        node = attrdef.defineTABLE(WorkAttributeConstant.WAIT_EVENT_NAMES, "Events");
        attrdef.defineTEXTColumn(node, "Event Name");
        attrdef.defineTEXTColumn(node, "Completion Code");
        attrdef.defineBOOLEANColumn(node, "Recurring", false);
        attrdef.defineTEXT(PROP_MAX_TRIES, "Max tries", 60);
        attrdef.defineTEXT(PROP_RETRY_INTERVAL, "Retry Interval (sec)", 80);
        attrdef.defineTEXT(PROP_TIMEOUT, "Acknowledgment Timeout (sec)", 80);
        attrdef.defineTEXT(PROP_RESPONSE_TIMEOUT, "esponse Timeout (sec)", 80);
        attrdef.defineDROPDOWN(WorkAttributeConstant.STATUS_AFTER_TIMEOUT, "Status after timeout",
        		new String[]{WorkStatus.STATUSNAME_CANCELLED, WorkStatus.STATUSNAME_WAITING},
        		WorkStatus.STATUSNAME_CANCELLED);
        attrdef.defineHYPERLINK(REQUEST_XSD, "Request XSD");
        attrdef.defineHYPERLINK(RESPONSE_XSD, "Response XSD");
        attrdef.defineHYPERLINKstatic("/MDWWeb/doc/CompositeSynchronousAdapter.html",
        			"Composit Synchronous Adaptor Activity Help");
    }

}
