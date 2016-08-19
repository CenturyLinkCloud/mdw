/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.task.EngineAccess;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public abstract class FormServerCommon {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected void loadDocument(FormDataDocument datadoc, Long docid)
		throws DataAccessException, MbengException {
		EventManager eventManager = ServiceLocator.getEventManager();
		DocumentVO docvo = eventManager.getDocumentVO(docid);
		datadoc.load(docvo.getContent());
	}

    /**
     * The method is invoked to
     * @param datadoc
     * @param taskInst
     * @throws Exception
     */
    protected void loadTaskData(FormDataDocument datadoc, TaskInstanceVO taskInst)
		throws Exception {
    	if (OwnerType.DOCUMENT.equals(taskInst.getSecondaryOwnerType())) {
    		loadDocument(datadoc, taskInst.getSecondaryOwnerId());
    	} else if (OwnerType.USER.equals(taskInst.getOwnerType())) {
    		// owner is USER and secondary owner is null indicates temporary task
    		datadoc.load(taskInst.getActivityMessage());
    	}
    }

    protected void saveTaskData(TaskInstanceVO taskInst, FormDataDocument datadoc)
    throws DataAccessException {
    	if (OwnerType.DOCUMENT.equals(taskInst.getSecondaryOwnerType())) {
			EventManager eventManager = ServiceLocator.getEventManager();
			eventManager.updateDocumentContent(taskInst.getSecondaryOwnerId(),
					datadoc.format(), FormDataDocument.class.getName());
		} else if (OwnerType.USER.equals(taskInst.getOwnerType())) {
    		// owner is USER and secondary owner is null indicates temporary task
			taskInst.setActivityMessage(datadoc.format());
		}
    }

    public String logtag(Long taskInstId, FormSession mdw) {
    	return "T" + taskInstId + "." + mdw.getCuid();
    }

    protected void clear_error_and_init(FormDataDocument datadoc) {
    	datadoc.clearErrors();
    	datadoc.setMetaValue(FormDataDocument.META_PROMPT, null);
    	datadoc.setMetaValue(FormDataDocument.META_INITIALIZATION, null);
    }

    protected void createDataDocumentFromInitCall(FormDataDocument datadoc,
    		String inputref, Map<String,String> params) throws Exception {
		String action = inputref;
		for (String name : params.keySet()) {
			if (name.equals(FormConstants.URLARG_INPUTREF)) continue;
			String value = params.get(name);
			if (action.equals(inputref)) action += "?";
			else action += "&";
			action += name + "=" + value;
		}
		datadoc.setAttribute(FormDataDocument.ATTR_ACTION, action);
		String req = datadoc.format();
		String resp;
		if (logger.isDebugEnabled()) logger.debug("inputref request: " + req);
		boolean internal=true;
		if (internal) {
			Map<String,String> metaInfo = new HashMap<String,String>();
			metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_INTERNAL);
			ListenerHelper helper = new ListenerHelper();
			resp = helper.processEvent(req, metaInfo);
		} else {
			String mdwweburl = ApplicationContext.getServicesUrl();
			HttpHelper httpHelper = new HttpHelper(new URL(mdwweburl + "/Services"));
			resp = httpHelper.post(req);
		}
		if (logger.isDebugEnabled()) logger.debug("inputref response: " + resp);
		if (resp==null) {
			throw new Exception("Failed to load inputref " + inputref);
		}
		datadoc.load(resp);
		if (!"FORMDATA".equals(datadoc.getRootNode().getKind())) {
			logger.severe("Loaded inputref is not correct: " + resp);
			throw new Exception("Data loaded is not a FORMDATA");
		}
    }

    /**
     * The method is invoked to create initial data document when a get/post
     * is made using URL directly and it is not a submission from
     * existing web pages.
     *
     * @param request
     * @param mdw
     * @param formname
     * @return
     */
    protected FormDataDocument createDataDocument(Map<String,String> params,
    		FormSession mdw, String formname) {
    	String inputxml = params.get(FormConstants.URLARG_INPUTXML);
		String inputdoc = params.get(FormConstants.URLARG_INPUTDOC);
		String inputref = params.get(FormConstants.URLARG_INPUTREF);
		FormDataDocument datadoc = new FormDataDocument();
		if (inputxml != null) {			// xml content is specified - this must come from a POST
			try {
				datadoc.load(inputxml);
			} catch (Exception e) {
				logger.severeException("Cannot load document", e);
			}
		} else if (inputdoc!=null) {	// document ID of the xml is specified - load from database
			try {
				loadDocument(datadoc, new Long(inputdoc));
            } catch (Exception e) {
                logger.severeException("Cannot load document", e);
            }
		} else if (inputref!=null && (inputref.length()==0 || inputref.equals("copy"))){
			// get a copy of the data document of referenced (typically parent) window
			String parent_taskinstid = params.get(FormConstants.URLARG_PARENT);
			TaskInstanceVO parentTaskInst = mdw.getWindow(new Long(parent_taskinstid)).getTaskInstance();
			try {
				loadTaskData(datadoc, parentTaskInst);
				clear_error_and_init(datadoc);
			} catch (Exception e) {
				logger.severeException("Cannot parse document", e);
			}
		} else if (inputref!=null) {	// really a reference
			try {
				createDataDocumentFromInitCall(datadoc, inputref, params);
			} catch (Exception e) {
				logger.severeException("Cannot load document", e);
				datadoc.addError("Cannot load document - exception " + e.getMessage());
			}
		} else {
            // no input document specified - convert all arguments to data document element
			try {
				for (String name : params.keySet()) {
					String value = params.get(name);
					datadoc.setValue(name, value);
				}
			} catch (Exception e) {
				logger.severeException("Cannot load document", e);
				datadoc.addError("Cannot load document - exception " + e.getMessage());
			}

		}
		datadoc.setMetaValue(FormDataDocument.META_FORM, formname);
		return datadoc;
    }

    protected FormDataDocument performAction(FormSession mdw, CallURL callurl,
    		FormDataDocument datadoc, TaskInstanceVO taskInst) {
        String act = callurl.getAction();
        FormDataDocument datadocReturn;
        if (act.startsWith(FormConstants.SPECIAL_ACTION_PREFIX)) {
        	datadocReturn = performSpecialAction(mdw, callurl, datadoc, taskInst);
        	if (datadocReturn==null) datadocReturn = processGeneralAction(datadoc, mdw, callurl, taskInst);
        } else {
        	datadocReturn = processGeneralAction(datadoc, mdw, callurl, taskInst);
        }
        return datadocReturn;
    }

    protected FormDataDocument performSpecialAction(FormSession mdw, CallURL callurl,
    		FormDataDocument datadoc, TaskInstanceVO taskInst) {
    	Long taskInstId = taskInst.getTaskInstanceId();
        String act = callurl.getAction();
    	if (act.equals(FormConstants.ACTION_REPAINT)) {
    		// do nothing
        } else if (act.equals(FormConstants.ACTION_LIST_PICK)) {
        	// already handled by update_model
        } else if (act.equals(FormConstants.ACTION_WINDOW)) {
        	String formname = callurl.getParameter(FormConstants.URLARG_FORMNAME);
        	show_window(formname, taskInstId, callurl.getParameters(), datadoc, mdw);
        } else if (act.equals(FormConstants.ACTION_PAGE)) {
        	String newFormName = callurl.getParameter(FormConstants.URLARG_FORMNAME);
        	String inputref = callurl.getParameter(FormConstants.URLARG_INPUTREF);
        	if (inputref!=null) {
        		try {
            		createDataDocumentFromInitCall(datadoc, inputref, callurl.getParameters());
            	} catch (Exception ex) {
            		logger.exception(logtag(taskInstId,mdw), "Exception in initializing page data", ex);
            		datadoc.addError("Exception: " + ex.getMessage());
            	}
        	}
        	datadoc.setAttribute(FormDataDocument.ATTR_FORM, newFormName);
        } else if (act.equals(FormConstants.ACTION_COMPLETE_TASK)) {
        	String taskAction = callurl.getParameter(FormConstants.URLARG_ACTION);
        	try {
				if (TaskAction.RETRY.equalsIgnoreCase(taskAction))
					processTaskCloseAction(datadoc, mdw, callurl, TaskAction.RETRY,
						taskInst, callurl.getParameter(FormConstants.URLARG_COMMENT), false);
				else processTaskCloseAction(datadoc, mdw, callurl, TaskAction.COMPLETE,
						taskInst, callurl.getParameter(FormConstants.URLARG_COMMENT), false);
	    	} catch (Exception ex) {
	    		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
	    		datadoc.addError("Exception: " + ex.getMessage());
			}
        } else if (act.equals(FormConstants.ACTION_CANCEL_TASK)) {
        	String taskAction = callurl.getParameter(FormConstants.URLARG_ACTION);
			try {
				if (TaskAction.ABORT.equalsIgnoreCase(taskAction))
					processTaskCloseAction(datadoc, mdw, callurl, TaskAction.ABORT,
							taskInst, callurl.getParameter(FormConstants.URLARG_COMMENT), false);
				else processTaskCloseAction(datadoc, mdw, callurl, TaskAction.CANCEL,
						taskInst, callurl.getParameter(FormConstants.URLARG_COMMENT), false);
	    	} catch (Exception ex) {
	    		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
	    		datadoc.addError("Exception: " + ex.getMessage());
			}
        } else if (act.equals(FormConstants.ACTION_ASSIGN_TASK)) {
        	try {
        		processTaskAssignment(mdw, callurl.getParameters(), taskInstId, datadoc);
        	} catch (Exception ex) {
        		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
        		datadoc.addError("Exception: " + ex.getMessage());
        	}
        } else if (act.equals(FormConstants.ACTION_SAVE_TASK)) {
        	processTaskSaveAction(datadoc, mdw, taskInstId);
        } else if (act.equals(FormConstants.ACTION_GET_ASYNC_RESPONSE)) {
        	datadoc = getAsyncResponse(datadoc, mdw);
        } else if (act.equals(FormConstants.ACTION_ACT_AS)) {
        	String actAsName = callurl.getParameter(FormConstants.URLARG_NAME);
        	try {
        		UserVO actAs;
        		if (StringHelper.isEmpty(actAsName) || actAsName.equalsIgnoreCase(mdw.getRealUser().getCuid())) actAs = null;
        		else {
        			actAs = UserGroupCache.getUser(actAsName);
        			if (actAs==null) throw new UserException("User does not exist: " + actAsName);
        			UserGroupVO[] groups = actAs.getWorkgroups();
        			mdw.setActAsUser(null);	// so that it checks the right of the real user below
        			boolean allowed = false;
        			for (int i=0; i<groups.length&&!allowed; i++) {
        				allowed = mdw.hasRole(groups[i].getName(), UserRoleVO.SUPERVISOR);
        			}
        			if (!allowed) throw new UserException("You are not a supervisor for " + actAsName);
        		}
        		mdw.setActAsUser(actAs);
        	} catch (CachingException e) {
        		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", e);
        		datadoc.addError("Exception: " + e.getMessage());
        	} catch (UserException e) {
        		datadoc.addError(e.getMessage());
			}
        } else datadoc = null;
    	return datadoc;
    }

    /**
     * complete/cancel general task instance
     * @param datadoc
     * @param mdw
     * @param action
     * @param taskAction
     * @param taskInstId
     */
    protected void processTaskCloseAction(FormDataDocument datadoc,
    		FormSession mdw, CallURL callurl, String taskAction,
    		TaskInstanceVO taskInst, String comment,
    		boolean isClassicTask) throws Exception {
    	Long taskInstId = taskInst.getTaskInstanceId();
    	FormWindow win = mdw.getWindow(taskInstId);
    	if (isClassicTask) {
    		// set task as completed/cancelled
			TaskManager taskManager = ServiceLocator.getTaskManager();
        	Long userId = mdw.getActAsUser().getId();
        	taskManager.performActionOnTaskInstance(taskAction, taskInstId, userId, null, comment, null, true);
        	// refresh cached task instance
        	if (win!=null) {
        		taskInst = TaskManagerAccess.getInstance().getTaskInstance(taskInstId);
        		win.setTaskInstance(taskInst);
        	} // else can happen for regression tester entry
    	} else {
    		// save data to document
    		if (taskAction.equalsIgnoreCase(TaskAction.COMPLETE)) saveTaskData(taskInst, datadoc);
    		// set task as completed/cancelled
    		TaskManagerAccess.getInstance().closeTaskInstance(taskInst, taskAction, mdw.getCuid(), comment);
    		// refresh cached task instance
    		if (win!=null) {
    			taskInst = TaskManagerAccess.getInstance().getTaskInstance(taskInstId);
    			win.setTaskInstance(taskInst);
    		}
    		// notify process engine
    		datadoc.setAttribute(FormDataDocument.ATTR_ACTION, callurl.toString());
    		String eventName = datadoc.getAttribute(FormDataDocument.ATTR_ID);
    		(new EngineAccess()).sendMessageToEngine(datadoc.format(), eventName,
	        			taskInstId, taskInst.getOwnerApplicationName());
    	}
    }

    protected void processTaskAssignment(FormSession mdw, Map<String,String> params, Long taskInstId,
    		FormDataDocument datadoc) throws Exception {
    	String taskAction = params.get(FormConstants.URLARG_ACTION);
		TaskInstanceVO taskInst = mdw.getWindow(taskInstId).getTaskInstance();
    	saveTaskData(taskInst, datadoc);
    	Long userId = mdw.getActAsUser().getId();
    	Long assigneeId;	// MDW user ID
		String comment = params.get(FormConstants.URLARG_COMMENT);
    	String destination;
    	if (taskInst.isDetailOnly()) {
    		FormDataDocument reqdoc = new FormDataDocument();
    		reqdoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
    				taskInst.getAssociatedTaskInstanceId().toString());
    		reqdoc.setMetaValue(FormDataDocument.META_USER, mdw.getCuid());
    		reqdoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_ASSIGN_TASK);
    		reqdoc.setValue("TaskAction", taskAction);
        	if (comment!=null) reqdoc.setValue("Comment", comment);
        	if (taskAction.equalsIgnoreCase(TaskAction.ASSIGN)) {
        		String cuid = params.get(FormConstants.URLARG_PROMPT_INPUT);
        		reqdoc.setValue("AssigneeCuid", "self".equals(cuid)?mdw.getCuid():cuid);
        	} else if (taskAction.equalsIgnoreCase(TaskAction.FORWARD)) {
        		reqdoc.setValue("GroupName",params.get(FormConstants.URLARG_PROMPT_INPUT));
        	}
        	String response = TaskManagerAccess.getInstance().notifySummaryTaskManager(reqdoc);
        	String errmsg = FormDataDocument.parseSimpleResponse(response);
			if (errmsg!=null) throw new TaskException(errmsg);
    	} else {
	    	if (taskAction.equalsIgnoreCase(TaskAction.ASSIGN)) {
	    		String assigneeCuid = params.get(FormConstants.URLARG_PROMPT_INPUT);
				if (assigneeCuid==null || assigneeCuid.length()==0)
					throw new TaskException("No assignee is specified");
				else if (assigneeCuid.equals(FormDataDocument.ASSIGN_STATUS_SELF)) assigneeCuid = mdw.getCuid();
				UserVO assignee = UserGroupCache.getUser(assigneeCuid);
				if (assignee==null) throw new TaskException(assigneeCuid + " is not a valid user");
				if (!userCanWorkOn(assignee, taskInst)) {
					if (assignee.getId().equals(userId))
						throw new TaskException("You are not authorized to work on this task");
					else throw new TaskException("The user is not authorized to work on this task");
				}
				assigneeId = assignee.getId();
				destination = null;
	    	} else if (taskAction.equalsIgnoreCase(TaskAction.CLAIM)) {
				UserVO assignee = UserGroupCache.getUser(userId);
				if (!userCanWorkOn(assignee, taskInst)) {
					throw new TaskException("You are not authorized to work on this task");
				}
	    		assigneeId = userId;
	    		destination = null;
	    	} else if (taskAction.equalsIgnoreCase(TaskAction.RELEASE)) {
	    		assigneeId = null;
	    		destination = null;
	    	} else if (taskAction.equalsIgnoreCase(TaskAction.WORK)) {
	    		assigneeId = null;
	    		destination = null;
	    	} else if (taskAction.equalsIgnoreCase(TaskAction.FORWARD)) {
	    		assigneeId = null;
				destination = params.get(FormConstants.URLARG_PROMPT_INPUT);
	    	} else throw new TaskException("Unsupported task assignment action " + taskAction);
			TaskManager taskManager = ServiceLocator.getTaskManager();
    		taskManager.performActionOnTaskInstance(taskAction, taskInst.getTaskInstanceId(),
    				userId, assigneeId, comment, destination, false);
    	}
    	// refresh cached task instance
    	taskInst = TaskManagerAccess.getInstance().getTaskInstance(taskInstId);
    	mdw.getWindow(taskInstId).setTaskInstance(taskInst);
    }

    private boolean userCanWorkOn(UserVO user, TaskInstanceVO taskInst)
			throws TaskException, DataAccessException {
		List<String> groups;
		if (taskInst.isTemplateBased()) groups = taskInst.getGroups();
		else {
			TaskVO task = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
			groups = task.getUserGroups();
		}
		for (String grp : groups) {
			for (UserGroupVO g : user.getWorkgroups()) {
				if (grp.equals(g.getName())) return true;
			}
		}
		return false;
    }

    protected void processResponseMessage(FormDataDocument datadoc, FormSession mdw,
    		TaskInstanceVO taskInst) throws Exception {
		String additionalAction = datadoc.getAttribute(FormDataDocument.ATTR_ACTION);
		CallURL callurl = additionalAction==null?null:new CallURL(additionalAction);
		Long taskInstId = taskInst.getTaskInstanceId();
		if (callurl==null) {
			// do nothing
		} else if (callurl.getAction().equals(FormConstants.ACTION_PROMPT)) {
			// prompt will generated from FormDataDocument.META_PROMPT
		} else if (callurl.getAction().equals(FormConstants.ACTION_DIALOG)) {
	    	String formName = callurl.getParameter(FormConstants.URLARG_FORMNAME);
			show_dialog(formName, taskInstId, callurl.getParameters(), datadoc, mdw);
		} else if (callurl.getAction().equals(FormConstants.ACTION_WINDOW)) {
	    	String formName = callurl.getParameter(FormConstants.URLARG_FORMNAME);
			show_window(formName, taskInstId, callurl.getParameters(), datadoc, mdw);
//		} else if (callurl.getAction().equals(FormConstants.ACTION_OK)) {	// now on client side
//			String message = datadoc.getValue(FormDataDocument.META_PROMPT);
//			handle_OK(mdw, message);
//		} else if (callurl.getAction().equals(FormConstants.ACTION_CANCEL)) {	// now on client
//			String message = datadoc.getValue(FormDataDocument.META_PROMPT);
//			handle_CANCEL(mdw, message);
		} else if (callurl.getAction().startsWith(FormConstants.ACTION_LOGON)) {
			String cuid = callurl.getParameter(FormConstants.URLARG_CUID);
			if (cuid==null || cuid.length()==0) cuid = "unknown";
        	mdw.setUser(TaskManagerAccess.getInstance().getUserAuthorization(cuid, false));	// convert to LDAP filter???
        	logger.info(logtag(taskInstId,mdw), "User logged on - cuid " + cuid);
		} else if (callurl.getAction().equals(FormConstants.ACTION_LOGOFF)) {
			logger.info(logtag(taskInstId,mdw), "User logged off - cuid " + mdw.getCuid());
    		mdw.logoff();
		} else if (callurl.getAction().equals(FormConstants.ACTION_COMPLETE_TASK)) {
			String action = FormConstants.ACTION_COMPLETE_TASK;
			this.processTaskCloseAction(datadoc, mdw, new CallURL(action), TaskAction.COMPLETE,
					taskInst, null, false);
		} else {
			// do nothing
		}
    }

    private String checkForNonFormDataError(FormDataDocument datadoc) {
    	if (!datadoc.getRootNode().getKind().equals(FormDataDocument.KIND_FORMDATA)) {
    		MbengNode node = datadoc.getRootNode().getFirstChild();
    		String errmsg = null;
    		while (node!=null && errmsg==null) {
    			if (node.getKind().contains("StatusMessage")) {
    				errmsg = node.getValue();
    			}
    			node = node.getNextSibling();
    		}
    		if (errmsg==null) errmsg = "Unknown error from engine";
    		return errmsg;
		} else return null;
    }

    protected FormDataDocument processGeneralAction(FormDataDocument datadoc, FormSession mdw,
    		CallURL callurl, TaskInstanceVO taskInst) {
    	String timeoutSpec = callurl.getParameter(FormConstants.URLARG_TIMEOUT);
//    	Long taskInstId = datadoc.getTaskInstanceId();
    	Long taskInstId = taskInst.getTaskInstanceId();
        String origFormName = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
    	try {
    		datadoc.setAttribute(FormDataDocument.ATTR_ACTION, callurl.toString());
//    		TaskInstanceVO taskInst = mdw.getWindow(taskInstId).getTaskInstance();
    		if ("async".equals(timeoutSpec)) {
    			datadoc.setAttribute(FormDataDocument.ATTR_ENGINE_CALL_STATUS, "WAITING");
    			this.saveTaskData(taskInst, datadoc);
    			(new EngineAccess()).callEngineAsync(datadoc, taskInst.getOwnerApplicationName());
    		} else {
    			int timeoutSeconds = timeoutSpec==null?120:Integer.parseInt(timeoutSpec);
    			FormDataDocument datadoc1 = (new EngineAccess()).callEngine(datadoc, timeoutSeconds,
		    			taskInst.getOwnerApplicationName());
		    	String errmsg = checkForNonFormDataError(datadoc1);
		    	if (errmsg!=null) throw new Exception(errmsg);
		    	datadoc = datadoc1;	// set here so that non-formdata error message will not override old datadoc
		    	saveTaskData(taskInst, datadoc);
	     		if (!datadoc.hasErrors()) {
	     			processResponseMessage(datadoc, mdw, taskInst);
	     			if (datadoc.getAttribute(FormDataDocument.ATTR_FORM)==null)
	     				datadoc.setAttribute(FormDataDocument.ATTR_FORM, origFormName);
	     		} else datadoc.setAttribute(FormDataDocument.ATTR_FORM, origFormName);
    		}
    	} catch (Exception ex) {
    		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
    		datadoc.addError("Exception: " + ex.getMessage());
    		datadoc.setAttribute(FormDataDocument.ATTR_FORM, origFormName);
    	}
		return datadoc;
    }

    protected void processTaskSaveAction(FormDataDocument datadoc, FormSession mdw, Long taskInstId) {
    	try {
    		// save data to document
    		TaskInstanceVO taskInst = mdw.getWindow(taskInstId).getTaskInstance();
	        saveTaskData(taskInst, datadoc);
    	} catch (Exception ex) {
    		logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
    		datadoc.addError("Exception: " + ex.getMessage());
    	}
    }

    private FormDataDocument getAsyncResponse(FormDataDocument datadoc, FormSession mdw) {
    	Long taskInstId = datadoc.getTaskInstanceId();
		TaskInstanceVO taskInst = mdw.getWindow(taskInstId).getTaskInstance();
		try {
			if (taskInst==null) throw new Exception("There is no task instance");
			FormDataDocument datadoc2 = new FormDataDocument();
	        loadTaskData(datadoc2, taskInst);
	        String av = datadoc2.getAttribute(FormDataDocument.ATTR_ENGINE_CALL_STATUS);
	        if ("DONE".equals(av)) {
	        	datadoc2.setAttribute(FormDataDocument.ATTR_ENGINE_CALL_STATUS, null);
	        	this.saveTaskData(taskInst, datadoc2);
	        	if (logger.isDebugEnabled()) {
	        		logger.debug(logtag(taskInstId,mdw), "Call Engine Async Resposne: "
	        				+ datadoc2.format());
	        	}
	        }
	        datadoc = datadoc2;
	    } catch (Exception ex) {
			logger.exception(logtag(taskInstId,mdw), "Exception in performing form action", ex);
			datadoc.addError("Exception: " + ex.getMessage());
		}
        return datadoc;
    }

    abstract protected void show_dialog(String formname,
    		Long taskInstId, Map<String,String> params, FormDataDocument datadoc, FormSession mdw);

    abstract protected void show_window(String formname,
    		Long taskInstId, Map<String,String> params, FormDataDocument datadoc, FormSession mdw);

}
