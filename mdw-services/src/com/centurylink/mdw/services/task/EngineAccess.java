/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.MiniEncrypter;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskType;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.observer.task.RemoteNotifier;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.task.factory.TaskInstanceNotifierFactory;
import com.qwest.mbeng.MbengNode;

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
				Long docid = eventManager.createDocument(FormDataDocument.class
						.getName(), 0L, OwnerType.TASK_INSTANCE, taskInstId, null,
						null, message);
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
			if (taskInst.isLocal()) {
                TaskRuntimeContext taskRuntime = taskManager.getTaskRuntimeContext(taskInst);
                for (String notifierSpec : notifierSpecs) {
                    try {
                        TaskNotifier notifier =notifierFactory.getNotifier(notifierSpec, processInstId);
                        if (notifier != null && !(notifier instanceof RemoteNotifier)) {
                            notifier.sendNotice(taskRuntime, action, outcome);
                        }
                    }
				    catch (Exception ex) {
				        // don't let one notifier failure prevent others from processing
				        logger.severeException(ex.getMessage(), ex);
				    }
				}
        	} else {
        		FormDataDocument msgdoc = new FormDataDocument();
        		msgdoc.setMetaValue(FormDataDocument.META_ACTION, FormConstants.ACTION_TASK_NOTIFICATION);
        		if (taskInst.getAssociatedTaskInstanceId()!=null)
            		msgdoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, taskInst.getAssociatedTaskInstanceId().toString());
        		else msgdoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, taskInst.getTaskInstanceId().toString());
        		msgdoc.setValue("Outcome", outcome);
        		MbengNode notifierTable = msgdoc.setTable("Notifiers");
        		for (String notifierSpec : notifierSpecs) {
        			msgdoc.addEntry(notifierTable, notifierSpec);
        		}
        		// populate task instance data that may be needed in notification
        		msgdoc.setMetaValue(FormDataDocument.META_TASK_NAME, taskInst.getTaskName());
        		msgdoc.setMetaValue(FormDataDocument.META_TASK_ASSIGNEE, taskInst.getTaskClaimUserCuid());
        		msgdoc.setMetaValue(FormDataDocument.META_TASK_DUE_DATE, StringHelper.dateToString(taskInst.getDueDate()));
        		msgdoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, taskInst.getOwnerId().toString());
        		msgdoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, taskInst.getOrderId());
        		msgdoc.setValue("TaskInstanceUrl", taskInst.getTaskInstanceUrl());
        		TaskVO task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
        		msgdoc.setMetaValue(FormDataDocument.META_TASK_LOGICAL_ID, task.getLogicalId());
        		MbengNode groupTable = msgdoc.setTable("Groups");
        		for (String group : taskInst.getGroups()) {
        			msgdoc.addEntry(groupTable, group);
        		}
        		// send message
        		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(taskInst.getOwnerApplicationName());
        		msgbroker.sendMessage(msgdoc.format());
        	}
		} catch (Exception e) {
			logger.severeException("Failed to send email notification for task instance "
					+ taskInst.getTaskInstanceId(), e);
		}
    }

	/**
	 * Import a task template from engine.
	 *
	 * @param ownerApplication engine logical name, optionally with engine URL
	 * @param logicalId logical ID of the task template to be imported.
	 * @return newly imported task template.
	 * @throws Exception
	 */
	public TaskVO importTask(String ownerApplication, String logicalId) throws Exception {
		String applName;
		int k = ownerApplication.indexOf('@');
		if (k>0) applName = ownerApplication.substring(0,k);
		else applName = ownerApplication;
		FormDataDocument calldoc = new FormDataDocument();
		calldoc.setMetaValue(FormDataDocument.META_ACTION, FormConstants.ACTION_GET_TASK_TEMPLATE);
		calldoc.setMetaValue(FormDataDocument.META_TASK_LOGICAL_ID, logicalId);
		int timeoutSeconds = 30;
		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(ownerApplication);
		String response = msgbroker.invoke(calldoc.format(), timeoutSeconds);
		calldoc.load(response);
		if (calldoc.hasErrors()) throw new DataAccessException(calldoc.getErrors().get(0));
		TaskVO task = new TaskVO();
		task.setLogicalId(applName + ":" + logicalId);
		task.setTaskName(calldoc.getValue(TaskActivity.ATTRIBUTE_TASK_NAME));
		task.setComment(calldoc.getValue(TaskAttributeConstant.DESCRIPTION));
		task.setTaskCategory(calldoc.getValue(TaskActivity.ATTRIBUTE_TASK_CATEGORY));
		task.setFormName(calldoc.getValue(TaskAttributeConstant.FORM_NAME));
		task.setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
		task.setAttribute(TaskAttributeConstant.NOTICES, calldoc.getValue(TaskAttributeConstant.NOTICES));
		task.setAttribute(TaskAttributeConstant.ALERT_INTERVAL, calldoc.getValue(TaskAttributeConstant.ALERT_INTERVAL));
		task.setAttribute(TaskAttributeConstant.AUTO_ASSIGN, calldoc.getValue(TaskAttributeConstant.AUTO_ASSIGN));
		task.setAttribute(TaskAttributeConstant.TASK_SLA, calldoc.getValue(TaskAttributeConstant.TASK_SLA));
		task.setAttribute(TaskAttributeConstant.NOTICE_GROUPS, calldoc.getValue(TaskAttributeConstant.NOTICE_GROUPS));
		task.setAttribute(TaskAttributeConstant.RECIPIENT_EMAILS, calldoc.getValue(TaskAttributeConstant.RECIPIENT_EMAILS));
		task.setAttribute(TaskAttributeConstant.CC_GROUPS, calldoc.getValue(TaskAttributeConstant.CC_GROUPS));
		task.setAttribute(TaskAttributeConstant.CC_EMAILS, calldoc.getValue(TaskAttributeConstant.CC_EMAILS));
		task.setAttribute(TaskAttributeConstant.INDICES, calldoc.getValue(TaskAttributeConstant.INDICES));
		task.setAttribute(TaskAttributeConstant.SERVICE_PROCESSES, calldoc.getValue(TaskAttributeConstant.SERVICE_PROCESSES));
		task.setUserGroupsFromString(calldoc.getValue(TaskActivity.ATTRIBUTE_TASK_GROUPS));
		task.setTaskCategory(calldoc.getValue(TaskActivity.ATTRIBUTE_TASK_CATEGORY));
		TaskManager taskManager = ServiceLocator.getTaskManager();
		Long taskId = taskManager.createTask(task, true);
		task.setTaskId(taskId);
		return task;
	}

	/**
	 * Import a resource from engine.
	 *
	 * @param ownerApplication
	 * @param resourceName
	 * @param resourceLanguage
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public RuleSetVO importResource(String ownerApplication, String resourceName, String resourceLanguage,
			int version) throws Exception {
		RuleSetVO resource = RuleSetCache.getRuleSet(resourceName, resourceLanguage, version);
		StringBuffer request = new StringBuffer();
		request.append("<_mdw_get_resource>");
		request.append("<name>").append(resourceName).append("</name>");
		request.append("<language>").append(resourceLanguage).append("</language>");
		if (version>0) request.append("<version>").append(version).append("</version>");
		request.append("</_mdw_get_resource>");
		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(ownerApplication);
		String response = msgbroker.invoke(request.toString(), 30);
		if (response==null) throw new DataAccessException("Null response received");
		if (response.startsWith("ERROR:")) throw new DataAccessException(response.substring(6));
		if (resource==null) {
			resource = new RuleSetVO();
			resource.setName(resourceName);
			resource.setLanguage(resourceLanguage);
			resource.setId(0L);
			resource.setVersion(version>0?version:1);
		} else if (version==0) {
			version = resource.getVersion()+1;
			resource = new RuleSetVO();
			resource.setName(resourceName);
			resource.setLanguage(resourceLanguage);
			resource.setId(0L);
			resource.setVersion(version);
		} // else simply override existing version
		resource.setRuleSet(response);
		TaskManager taskManager = ServiceLocator.getTaskManager();
    	Long id = taskManager.saveResource(resource);
		resource.setId(id);
		(new RuleSetCache()).clearCache();
		return resource;
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

    public String getDatabaseCredential(String serverSpec) throws Exception {
		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(serverSpec);
		String dbinfo = msgbroker.invoke("<_mdw_database_credential>encrypted</_mdw_database_credential>", 30);
		if (dbinfo==null) throw new Exception("Faield to find database info");
		if (dbinfo.startsWith("###")) dbinfo = MiniEncrypter.decrypt(dbinfo.substring(3));
		if (!dbinfo.startsWith("jdbc:")) throw new Exception("Faield to find database info");
		return dbinfo;
    }

}
