/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.cache.impl.WebPageCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.listener.http.FormServerCommon;
import com.centurylink.mdw.listener.http.FormSession;
import com.centurylink.mdw.listener.http.FormWindow;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.qwest.mbeng.MbengException;

@Deprecated
public class MDWBase extends FormServerCommon implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final String JSF_WINID = "form:" + FormConstants.URLARG_WINID;

    private FormSession mdwsession;

    // the following are working variables only used during a single 6-phase life cycle
    // Any concurrence issue of doing this?
    private FormWindow newWindow;
    private String skin;
    private boolean toRefresh;
    private long startTime;
    private Map<String,String> params;

    public MDWBase() {
    	mdwsession = null;
    }

    public FormWindow getWindow() {
    	if (newWindow!=null) return newWindow;
        String winid = params==null?null:params.get(JSF_WINID);
        // in case form does not have JSF_WINID defined, return first window
        if (winid==null) return mdwsession.getFirstWindow();
        return mdwsession.getWindow(new Long(winid));
    }

    private synchronized FormWindow createNewWindow(Map<String,String> params) {
    	// first delete closed windows
    	mdwsession.deleteClosedWindows();
    	// now create new window
    	FormWindow window = new FormWindow(null, null);
    	Long taskInstId = initializeWindow(window, params);
        if (taskInstId!=null) {
        	if (logger.isInfoEnabled())
        		logger.info(logtag(taskInstId,mdwsession), "New window created");
        	mdwsession.putWindow(taskInstId, window);
        }
        return window;
    }

    private synchronized FormWindow createNewWindow(TaskInstanceVO taskInst, FormDataDocument datadoc) {
    	FormWindow window = new FormWindow(taskInst, datadoc);
    	if (logger.isInfoEnabled())
    		logger.info(logtag(taskInst.getTaskInstanceId(),mdwsession), "New window created");
    	mdwsession.putWindow(taskInst.getTaskInstanceId(), window);
        return window;
    }

    public FormDataDocument getData() {
        return getWindow().getData();
    }

    public void setData(FormDataDocument dataDoc) {
        getWindow().setData(dataDoc);
    }

    public String getFormName() {
    	FormDataDocument datadoc = getWindow().getData();
        return datadoc==null?null:datadoc.getMetaValue(FormDataDocument.META_FORM);
    }

    public String getCuid() {
    	return mdwsession.getCuid();
    }

    public void loadDocument(FormDataDocument datadoc, Long docid)
    throws DataAccessException, MbengException
    {
		EventManager eventManager = RemoteLocator.getEventManager();
		DocumentVO docvo = eventManager.getDocumentVO(docid);
		datadoc.load(docvo.getContent());
    }

	public String getSkin() {
        if (skin==null) {
            skin = PropertyManager.getProperty("mdw.richfaces.skin");
            if (skin==null) skin = "blueSky";    // darkX, glassX, laguna
        }
        return skin;
	}

	public void setSkin(String skin) {
		this.skin = skin;
	}

	public boolean isToRefresh() {
		boolean ret = toRefresh;
		toRefresh = false;
		return ret;
	}

	public void setToRefresh(boolean v) {
		toRefresh = v;
	}

	private boolean isDbForms() {
	    return mdwsession!=null;
	}

	private void setDbForms(boolean dbForms) {
		if (dbForms) {
			if (mdwsession==null) {
				AuthenticatedUser user = (AuthenticatedUser)FacesVariableUtil.getValue("authenticatedUser");
				mdwsession = new FormSession(user);
				logger.info("New session for user: " + mdwsession.getCuid());
			}
		} else {
			mdwsession = null;
		}
	}

    public void clearCache() {
    	(new WebPageCache()).clearCache();
    	(new RuleSetCache()).clearCache();
    }

    public String getFacelet(String formName) throws DataAccessException, MbengException {
    	if (formName.startsWith("jsf:")) formName = formName.substring(4);
    	String facelet;
    	RuleSetVO ruleSetVO = RuleSetCache.getRuleSet(formName, RuleSetVO.FACELET, 0);
        if (ruleSetVO == null)
            throw new DataAccessException("Facelet not found for form: " + formName);
        facelet = ruleSetVO.getRuleSet();
        return facelet;
    }

	public TaskInstanceVO getTaskInstance() {
		return getWindow().getTaskInstance();
	}

	public void setTaskInstance(TaskInstanceVO taskInstance, FormDataDocument datadoc) {
		// TODO check with Don how this is used
		if (taskInstance!=null) {
			setDbForms(true);
			FormWindow win = mdwsession.getWindow(taskInstance.getTaskInstanceId());
			if (win==null) win = this.createNewWindow(taskInstance, datadoc);
		}
	}

	public void beforeAllPhases(FacesContext facesContext) {
		String path = facesContext.getExternalContext().getRequestServletPath();
		setDbForms(path!= null && path.endsWith("form.jsf"));
		if (!isDbForms()) return;

		startTime = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			startTime = System.currentTimeMillis();
			logger.debug("Phases processing started");
		}
		toRefresh = false;
		newWindow = null;
		params = facesContext.getExternalContext().getRequestParameterMap();

        FormWindow window = getWindow();
        if (window==null) {
        	String winid = params.get(JSF_WINID);
        	if (winid!=null) {		// session expired
        		logger.info("Session Expired - CUID - " + getCuid());
	        	try
	        	{
	        		String sessionTimeoutForm = PropertyManager.getProperty(PropertyNames.MDW_WEB_SESSION_TIMEOUT);
	        		if (sessionTimeoutForm!=null && sessionTimeoutForm.length()>0) {
	        			String taskMgrUrl = ApplicationContext.getTaskManagerUrl();
	        			int k = taskMgrUrl.lastIndexOf("/");
	        			facesContext.getExternalContext().redirect(taskMgrUrl.substring(k)
	        					+ "/form.jsf?formName="+sessionTimeoutForm+"&prompt=Session Expired");
	        		} else {
	        		    logger.severe("HTTP Session Expired.");
	        			UIError uiError = new UIError("HTTP Session Expired.");
	        			FacesVariableUtil.setValue("error", uiError);
	//              	  FacesVariableUtil.setRequestAttrValue("error", uiError);
	        			facesContext.getExternalContext().redirect("error.jsf");
	        		}
	        	} catch (IOException ex) {
	        		logger.severeException(ex.getMessage(), ex);
	        	}
	        	facesContext.responseComplete();
        	} else {			// initial launch from URL (can be sub window)
        		if (newWindow==null) {
        			newWindow = createNewWindow(params);
        		}
//        		window = newWindow;
        	}
        } else {
        	window.clearErrors();
        }
	}

	public void beforeRenderResponse(FacesContext facesContext) {
		if (!isDbForms()) return;

		FormWindow window = getWindow();
		// set JSF validation messages
		Iterator<FacesMessage> jsfMsgs = facesContext.getMessages();
		while (jsfMsgs.hasNext())
        {
			FacesMessage jsfMsg = jsfMsgs.next();
			window.addError(jsfMsg.getDetail());
        }
	}

	public void afterAllPhases() {
		if (!isDbForms()) return;

		FormWindow window = getWindow();
		if (window==null) {
			logger.info("Session expired for user " + getCuid());
		}
		newWindow = null;
		params = null;
		if (logger.isDebugEnabled()) {
			double duration = (System.currentTimeMillis()-startTime)/1000.0;
			logger.debug("Phases processing finished [" + duration + "s]");
		}
	}

	/**
	 * logicalUrl is like nav.xhtml?class=com.centurylink.mdw.formaction.MyAction&tablename=tttt&pageno=6
	 * return string is formName to navigate to
	 */
	public void performAction(String logicalUrl) {
		FormWindow win = getWindow();
		if (logger.isDebugEnabled())
    		logger.debug(logtag(win.getId(),mdwsession), "Form action invoked: '" + logicalUrl + "'");
	    CallURL callurl = new CallURL(logicalUrl);
    	String action = callurl.getParameter("class");
    	if (action!=null) {
    		callurl.getParameters().remove("class");
    		callurl.setAction(action);
    	}
    	FormDataDocument datadoc = win.getData();
        if (action==null || action.length()==0) {
            datadoc.addError("action not specified");
        } else {
        	datadoc = super.performAction(mdwsession, callurl, datadoc, win.getTaskInstance());
        	win.setData(datadoc);
        }
	}

    @Override
    protected FormDataDocument performSpecialAction(FormSession mdw, CallURL callurl,
    		FormDataDocument datadoc, TaskInstanceVO taskInst) {
        String act = callurl.getAction();
        if (act.startsWith(FormConstants.ACTION_SKIN)) {
        	String skinname = callurl.getParameter("skin");
        	if (skinname!=null) {
        		setSkin(skinname);
        		setToRefresh(true);
        	}
        	return datadoc;
        } else return super.performSpecialAction(mdw, callurl, datadoc, taskInst);
    }

    private Long initializeWindow(FormWindow win, Map<String,String> params) {
		String refresh_string = params.get(FormConstants.URLARG_REFRESH);
		boolean refresh = refresh_string!=null && refresh_string.equalsIgnoreCase("true") || isToRefresh();
		if (refresh) clearCache();
		String formName = params.get(FormConstants.URLARG_FORMNAME);
//		if (formName.startsWith("jsf:")) formName = formName.substring(4);
		String taskinstid = params.get(FormConstants.URLARG_TASK_INSTANCE_ID);
		Long taskInstId;
		FormDataDocument datadoc;
		try {
			taskInstId = new Long(taskinstid);
			TaskManager taskManager = RemoteLocator.getTaskManager();
			TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstId);
			taskManager.getTaskInstanceAdditionalInfo(taskInst);
			EventManager eventManager = RemoteLocator.getEventManager();
			DocumentVO docvo = eventManager.getDocumentVO(taskInst.getSecondaryOwnerId());
			datadoc = new FormDataDocument();
			datadoc.load(docvo.getContent());
			win.setData(datadoc);
			win.setTaskInstance(taskInst);
		} catch (Exception e) {
			logger.severeException("Failed to load task data", e);
			taskInstId = null;
		}
		return taskInstId;
	}

	@Override
	protected void show_dialog(String formname, Long taskInstId,
			Map<String, String> params, FormDataDocument datadoc, FormSession mdw) {
		// TODO implement show_dialog for jsf page
	}

	@Override
	protected void show_window(String formname, Long taskInstId,
			Map<String, String> params, FormDataDocument datadoc, FormSession mdw) {
		// TODO implement show_dialog for jsf page
	}

	// TODO menu action?
    // TODO check how to use JavaScript functions as actions
}
