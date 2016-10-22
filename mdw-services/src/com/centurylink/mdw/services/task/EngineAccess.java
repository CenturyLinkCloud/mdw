/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.task.factory.TaskInstanceNotifierFactory;

/**
 * This class provides (summary/detail) task manager access to engine.
 *
 */
public class EngineAccess {

	private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static int call_engine_method = -1;

	public void sendMessageToEngine(String message, String eventName,
			Long taskInstId, String ownerApplName) throws Exception {

		boolean internal = ownerApplName == null;
		if (internal) { // use internal calls notify processes directly
    		if (call_engine_method<0) {
    			call_engine_method = PropertyManager.getIntegerProperty(PropertyNames.MDW_TASKMGR_CALLENGINE_METHOD, 3);
    		}
    		if (call_engine_method==1 || call_engine_method==2) {
				EventManager eventManager = ServiceLocator.getEventManager();
				Long docid = eventManager.createDocument(FormDataDocument.class.getName(),
						OwnerType.TASK_INSTANCE, taskInstId, message);
				eventManager.notifyProcess(eventName, docid, message, 0);
    		} else {
    			IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
    			msgbroker.sendMessage(message);
    		}
		} else {
			IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(ownerApplName);
			msgbroker.sendMessage(message);
		}
	}

	public void callEngineAsync(FormDataDocument datadoc, String ownerApplName) throws Exception {
		String request = datadoc.format();
		if (logger.isDebugEnabled()) {
			logger.debug("Call Engine Async Request: " + request);
		}
		// cannot use sendMessageToEngine, as its 'internal' alternative
		// does not work when the action is not directed to a task activity
		// instance
		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(ownerApplName);
		msgbroker.sendMessage(request);
	}

    /**
     * Call back to engine synchronously (namely waiting for the response).
     * @param request request data to be sent to the engine, in String (XML or JSON)
     * @param timeoutSeconds time out waiting for engine to respond, in seconds
     * @param ownerApplName the engine application name when the task manager is hosted remotely. For local task manager,
     * 		this should be null.
     * @return the response data received from the engine, as a string (XML, JSON, etc)
     * @throws Exception connection related exception. Data exception should typically
     * 		included by the engine in response form data document itself w/o throwing an exception here.
     */
    public String callEngine(String request, int timeoutSeconds, String ownerApplName) throws Exception {
        long start_time = 0L;
        if (logger.isDebugEnabled()) {
        	logger.debug("Call Engine Request: " + request);
        	start_time = System.currentTimeMillis();
        }
        String response;

        if (ownerApplName == null) {		// internal
    		if (call_engine_method<0) {
    			call_engine_method = PropertyManager.getIntegerProperty(PropertyNames.MDW_TASKMGR_CALLENGINE_METHOD, 3);
    		}
    		if (call_engine_method==1 || call_engine_method==2) {		// Call event handler directly
        		// this does not save request/response in DOCUMENT
    			EventManager eventManager = ServiceLocator.getEventManager();
        		Map<String,String> metainfo = new HashMap<String,String>();
        		metainfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_INTERNAL);
        		metainfo.put(Listener.METAINFO_DOCUMENT_ID, "0");
        		response = eventManager.processExternalEvent("com.centurylink.mdw.listener.FormEventHandler",
        					request, metainfo);
        	} else if (call_engine_method==4) {		// use restful service; does not support remote task manager
        		String mdwweburl = ApplicationContext.getServicesUrl();
        		HttpHelper httpHelper = new HttpHelper(new URL(mdwweburl + "/Services"));
        		int connectTimeOut = 15;	// should this be configurable?
        		int readTimeOut = timeoutSeconds;
        		response = httpHelper.post(request, connectTimeOut, readTimeOut);
        	} else {		// determined by intra-mdw messenger
            	IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
            	response = msgbroker.invoke(request, timeoutSeconds);
        	}
        } else {
        	IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(ownerApplName);
        	response = msgbroker.invoke(request, timeoutSeconds);
        }
        if (logger.isDebugEnabled()) {
        	double time = (System.currentTimeMillis() - start_time)/1000.0;
        	logger.debug("Call Engine Resposne [" + time + "s]: " + response);
        }
    	return response;
    }

    /**
     * Call back to engine synchronously (namely waiting for the response).
     * @param datadoc request data to be sent to the engine, as a form data document
     * @param timeoutSeconds time out waiting for engine to respond, in seconds
     * @param ownerApplName owner applica
     * @param ownerApplName the engine application name when the task manager is hosted remotely. For local task manager,
     * 		this should be null.
     * @throws Exception connection related exception. Data exception should typically
     * 		included by the engine in response form data document itself w/o throwing an exception here.
     */
    public FormDataDocument callEngine(FormDataDocument datadoc, int timeoutSeconds,
    		String ownerApplName) throws Exception {
        String response = callEngine(datadoc.formatJson(), timeoutSeconds, ownerApplName);
        datadoc = new FormDataDocument();
        datadoc.load(response);
        /* the response may not be JSON, at least can receive error messages like:
        	<xs:MDWStatusMessage xmlns:xs="http://mdw.qwest.com/XMLSchema">
        	  <StatusCode>-1</StatusCode>
        	  <xs:StatusMessage>com.centurylink.enstel.mdw.formaction.ViewOrder</xs:StatusMessage>
        	</xs:MDWStatusMessage> */
    	return datadoc;
    }

    /**
     * Instruct engine to send an email notification.
     *
     * @param taskInst
     * @param outcome
     */
    public void sendNotification(TaskInstanceVO taskInst, String action, String outcome) {
        try {
            TaskInstanceNotifierFactory notifierFactory = TaskInstanceNotifierFactory.getInstance();
            List<String> notifierSpecs = new ArrayList<String>();
            Long processInstId = OwnerType.PROCESS_INSTANCE.equals(taskInst.getOwnerType()) ? taskInst.getOwnerId() : null;
            notifierSpecs = notifierFactory.getNotifierSpecs(taskInst.getTaskId(), processInstId, outcome);
        	if (notifierSpecs == null || notifierSpecs.isEmpty()) return;
			TaskManager taskManager = ServiceLocator.getTaskManager();
			taskManager.getTaskInstanceAdditionalInfo(taskInst);
            TaskRuntimeContext taskRuntime = taskManager.getTaskRuntimeContext(taskInst);
            for (String notifierSpec : notifierSpecs) {
                try {
                    TaskNotifier notifier =notifierFactory.getNotifier(notifierSpec, processInstId);
                    if (notifier != null) {
                        notifier.sendNotice(taskRuntime, action, outcome);
                    }
                }
			    catch (Exception ex) {
			        // don't let one notifier failure prevent others from processing
			        logger.severeException(ex.getMessage(), ex);
			    }
			}
		} catch (Exception e) {
			logger.severeException("Failed to send email notification for task instance "
					+ taskInst.getTaskInstanceId(), e);
		}
    }

	public String getRemoteProperty(String serverSpec, String propertyName) throws Exception {
		StringBuffer calldoc = new StringBuffer();
		calldoc.append("<_mdw_property>");
		calldoc.append(propertyName);
		calldoc.append("</_mdw_property>");
		int timeout = 30;
		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(serverSpec);
		return msgbroker.invoke(calldoc.toString(), timeout);
	}
}
