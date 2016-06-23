/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.AuthUtils;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.MiniEncrypter;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.form.ResourceLoader;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskStatuses;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.task.EngineAccess;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;

/**
 * This class is the server side for handling
 * forms implemented as JSPs. The class
 * is put here instead of mdw-web so that
 * the source can be visible in MDW applications
 * to help debugging.
 */

public class FormServer extends FormServerCommon implements ResourceLoader {

    private static String FormServerClass = null;

    public static FormServer getInstance() {
    	if (FormServerClass==null) {
        	String v = PropertyManager.getProperty(PropertyNames.MDW_FORM_SERVER);
        	FormServerClass = (v==null)?"":v;
    	}
    	FormServer server;
    	if (FormServerClass.equals("")) server = new FormServer();
    	else {
    		server = (FormServer)ApplicationContext.getClassInstance(FormServerClass);
    		if (server==null) server = new FormServer();
    	}
    	return server;
    }

    protected HtmlGenerator getHtmlGenerator() {
    	return new HtmlGenerator();
    }

    private static class HtmlMDWSession extends FormSession {
    	String privileges;
    	HttpSession session;
    	HtmlMDWSession(AuthenticatedUser user) {
    		super(user);
    		privileges = user.getGroupsAndRolesAsString();
    	}
    	@Override
    	public void logoff() {
    		super.logoff();
        	session.removeAttribute("MDWSession");
    	}
    	@Override
    	public void setActAsUser(UserVO actAsUser) {
    		super.setActAsUser(actAsUser);
    		privileges = getActAsUser().getGroupsAndRolesAsString();
    	}
    }

    private void refreshCache() {
		(new RuleSetCache()).clearCache();
		logger.debug("RuleSetCache cleared");
	}

    /**
     * Load resource from cache, files (local override), database, and internal source
     * (included in MDWListener.jar or MDWImplCore.jar sub-directory resource)
     */
    @Override
    public RuleSetVO getResource(String name, String language, int version)
    		throws DataAccessException {
    	// load from cache, file system (local override) and database
    	RuleSetVO resource = RuleSetCache.getRuleSet(name, language, version);
    	if (resource!=null) return resource;
    	// load from internal source (MDWListener.jar/MDWImplCore.jar/MDWDesigner.jar)
    	InputStream is0 = null;
    	try {
    		resource = new RuleSetVO();
			resource.setLanguage(language);
			String filename = name + resource.getFileExtension();
    		String path = "/resource/" + filename;
    		is0 = this.getClass().getResourceAsStream(path);
    		if (is0==null && resource.isImage()) {
    			path = "/resource/images/" + filename;
        		is0 = this.getClass().getResourceAsStream(path);
        		if (is0==null) {
        			path = "/images/" + filename;
            		is0 = this.getClass().getResourceAsStream(path);
        		}
    		}
    		if (is0!=null) {
        		byte buffer[] = FileHelper.readFromResourceStream(is0);
        		resource.setName(name);
    			resource.setId(-1L);
    			if (resource.isBinary())
    				resource.setRuleSet(resource.encode(buffer));
    			else resource.setRuleSet(new String(buffer));
    			if (logger.isDebugEnabled()) logger.debug("Got resource internally: " + path);
    			return resource;
    		}
    	} catch (Exception e) {
    		// do nothing
    	}
    	return null;
    }

    @Override
    public String getProperty(String packageName, String propertyName) {
    	return PackageVOCache.getPackage(packageName).getProperty(propertyName);
    }

	private RuleSetVO getFormDocument(String formname) throws DataAccessException, MbengException {
		RuleSetVO resource;
		if (formname.startsWith("html:")) {
			resource = this.getResource(formname.substring(5), RuleSetVO.HTML, 0);
			if (resource==null) throw new DataAccessException("cannot find form definition document - " + formname);
			return resource;
		} else if (formname.endsWith(".html")) {
			resource = this.getResource(formname.substring(0, formname.length()-5), RuleSetVO.HTML, 0);
			if (resource==null) throw new DataAccessException("cannot find form definition document - " + formname);
			return resource;
		} else if (formname.startsWith("extjs:")) {
			resource = this.getResource(formname.substring(6), RuleSetVO.JAVASCRIPT, 0);
			if (resource==null) throw new DataAccessException("cannot find form definition document - " + formname);
			return resource;
		} else {
			resource = getResource(formname, RuleSetVO.FORM, 0);
			if (resource==null) throw new DataAccessException("cannot find form definition document - " + formname);
			Object compiled = resource.getCompiledObject();
			if (compiled==null) {
				FormatDom fmter = new FormatDom();
				DomDocument formdoc = new DomDocument();
				fmter.load(formdoc, resource.getRuleSet());
				resource.setCompiledObject(formdoc);
			}
		}
		return resource;
	}

	/**
	 * This version is used by regression tester
	 * @param taskInstanceId
	 * @param updates
	 * @throws ServletException
	 */
    public void process_form(Long taskInstId, String formAction,
    		Map<String,String> updates, String cuid) throws Exception {
    	// prepare
    	UserManager userMgr = ServiceLocator.getUserManager();
    	AuthenticatedUser authuser = userMgr.loadUser(cuid);
    	HtmlMDWSession mdw = new HtmlMDWSession(authuser);
    	TaskManager taskManager = ServiceLocator.getTaskManager();
    	TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstId);
    	// load model (datadoc)
    	FormDataDocument datadoc = new FormDataDocument();
    	super.loadTaskData(datadoc, taskInst);
    	// clear error, initialization, and prompt
    	clear_error_and_init(datadoc);
    	// update model
    	datadoc.setMetaValue(FormDataDocument.ATTR_ACTION, formAction);
    	for (String name : updates.keySet()) {
    		String value = updates.get(name);
    		datadoc.setValue(name, value);
    	}
    	// perform action
    	datadoc = performAction(mdw, new CallURL(formAction), datadoc, taskInst);
    	// save back model
    	populateTaskInstanceStandardData(datadoc, taskInst, mdw);
    	super.saveTaskData(taskInst, datadoc);
    }

    public void process_form(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    	try {
			String formdatastr = request.getParameter("mdw_formdata");
    		HtmlMDWSession mdw = this.getMDWSession(request, response, formdatastr!=null, null);
    		if (mdw==null) return;	// displayed log-in or session expired page
			String formname;
			FormDataDocument datadoc;
			RuleSetVO formdoc;
    		TaskInstanceVO taskInst;
    		Long taskInstId;
    		FormWindow window;

			datadoc = new FormDataDocument();
			datadoc.loadJson(formdatastr);
			formname = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
			String action = datadoc.getAttribute(FormDataDocument.ATTR_ACTION);
			formdoc = this.getFormDocument(formname);
			taskInstId = datadoc.getTaskInstanceId();
   			if (logger.isDebugEnabled())
	    		logger.debug(logtag(taskInstId,mdw), "json from browser: " + formdatastr);
   			window = mdw.getWindow(taskInstId);
   			taskInst = window.getTaskInstance();
	    	// clear error, initialization, and prompt
	        clear_error_and_init(datadoc);
			if (action==null || action.length()==0) {
	            datadoc.addError("action not specified");
			} else {
				try {
					CallURL callurl = new CallURL(action);
					// update model (datadoc) on the server-side
					this.update_model_server(datadoc, request, taskInstId, mdw);
					datadoc = performAction(mdw, callurl, datadoc, taskInst);
					String formname2 = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
					if (formname2 !=null && !formname.equals(formname2)) {
						try {
							RuleSetVO formdoc2 = this.getFormDocument(formname2);
							formdoc = formdoc2;
						} catch (Exception e) {
							logger.exception(logtag(taskInstId,mdw), "Failed to load form definition", e);
							datadoc.addError("cannot find form definition document: " + formname2);
						}
					}
				} catch (Exception e) {
					String message = "Illegal action " + action;
					logger.exception(logtag(taskInstId,mdw), message, e);
					datadoc.addError(message + ": " + e.getMessage());
				}
			}

    		taskInst = window.getTaskInstance();		// taskInst may be changed by performAction
    		// cannot use mdw.getWindow(taskInstId).getTaskInstance(), as that will not work after log-off
    		populateTaskInstanceStandardData(datadoc, taskInst, mdw);
    		String html;
    		datadoc.complyJson();
    		HtmlGenerator htmlGenerator = this.getHtmlGenerator();
    		String summaryTaskManagerUrl = TaskManagerAccess.getInstance().getSummaryTaskManagerUrl();
    		if (formdoc.getLanguage().equalsIgnoreCase(RuleSetVO.HTML)) {
        		html = htmlGenerator.generate(formname, formdoc.getRuleSet(),
        				datadoc, this, summaryTaskManagerUrl);
    		} else {
    			html = htmlGenerator.generate(formname, (DomDocument)formdoc.getCompiledObject(),
    				datadoc, this, summaryTaskManagerUrl);
    		}
    		response.setContentType("text/html");
    		response.getOutputStream().print(html);
    	} catch (Exception ex) {
    		logger.severeException(ex.getMessage(), ex);
	    	response.setContentType("text/html");
	    	try {
				ServletOutputStream out = response.getOutputStream();
				out.print("<html><head><title>Error</title></head><body><h1>Error</h1><pre>\n");
				ex.printStackTrace(new PrintStream(out));
				out.print("</pre></body></html>\n");
			} catch (IOException e) {
				throw new ServletException(ex.getMessage(), ex);
			}
    	}
    }

    private void display_error_response(String title,
    		String message, HttpServletResponse response) throws IOException {
    	String result = "<html><head><title>Error</title></head><body><h1>" +
			title  +  "</h1>" + message + "</body></html>";
    	response.setContentType("text/html");
    	response.getOutputStream().print(result);
    }

    private HtmlMDWSession getMDWSession(HttpServletRequest request) {
    	HttpSession session = request.getSession(false);
    	if (session==null) return null;
		HtmlMDWSession mdw = (HtmlMDWSession)session.getAttribute("MDWSession");
		return mdw;
    }

    private HtmlMDWSession getMDWSession(HttpServletRequest request, HttpServletResponse response,
    		boolean hasSubmittedData, String foreignSessionId)
    		throws IOException, GeneralSecurityException {
    	if (foreignSessionId!=null) {
        	HttpSession session = request.getSession(true);
        	foreignSessionId = MiniEncrypter.decrypt(foreignSessionId);
        	// System.out.println("@@@@ get foreign session: " + foreignSessionId);
        	String[] parsed = foreignSessionId.split("@");
        	String cuid = parsed[0];
        	AuthenticatedUser authuser =
        		TaskManagerAccess.getInstance().getUserAuthorization(cuid, false);
			session.setAttribute("authenticatedUser", authuser);
			HtmlMDWSession mdw = new HtmlMDWSession(authuser);
			session.setAttribute("MDWSession", mdw);
    		mdw.session = session;
    		return mdw;
    	}
    	HttpSession session = request.getSession(false);
    	if (session==null) {	// only when not using clear trust/ldap filter
			int sessionTimeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_WEB_SESSION_TIMEOUT, 1800);
    		session = request.getSession(true);
    		session.setMaxInactiveInterval(sessionTimeout);
    	}
		HtmlMDWSession mdw = (HtmlMDWSession)session.getAttribute("MDWSession");
		if (mdw==null) {
			if (hasSubmittedData) {		// this indicates the session expired
				display_error_response("Session Expired", "Your session has expired", response);
				return null;
			}
			AuthenticatedUser authuser = (AuthenticatedUser) session.getAttribute("authenticatedUser");
			if (authuser==null) {	// only possible when not filtered
				authuser = getAuthenticatedUser(request,response);
				if (authuser==null) return null;	// displayed login page
				session.setAttribute("authenticatedUser", authuser);
			} else {
				// this overrides is for taking care of remote user manager
				authuser = TaskManagerAccess.getInstance().getUserAuthorization(authuser.getCuid(), true);
				session.setAttribute("authenticatedUser", authuser);
			}
			mdw = new HtmlMDWSession(authuser);
			session.setAttribute("MDWSession", mdw);
		}
		mdw.session = session;
		return mdw;
    }

    private AuthenticatedUser getAuthenticatedUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	AuthenticatedUser authuser;
    	String user = request.getParameter("user");
		String pass = request.getParameter("pass");
		if (user!=null && pass!=null) {
			try {
				AuthUtils.ldapAuthenticate(user, pass);
				authuser = TaskManagerAccess.getInstance().getUserAuthorization(user, false);
			} catch (Exception e) {
				String msg = e.getMessage();
				display_authentication_page(request, msg, response);
				authuser = null;
    	    }
		} else {
			display_authentication_page(request, null, response);
			authuser = null;
		}
		return authuser;
    }

    private void display_authentication_page(HttpServletRequest request,
    		String errmsg, HttpServletResponse response) throws IOException {
    	response.setContentType("text/html");
    	StringBuffer html = new StringBuffer();
    	html.append("<html>\n");
    	html.append("<head>\n");
    	html.append("<title>Authenticate</title>\n");
    	html.append("</head>\n");
    	html.append("<body bgcolor='#ffc0c0'>\n");
    	html.append("<h1>Task Manager Authentication</h1>\n");
    	if (errmsg!=null) {
    		html.append("<font color='red'><b>" + errmsg + "</b></font>\n");
    	}
    	String requestURI = request.getRequestURI();
        if (requestURI.endsWith("/form")) {
        	html.append("<form action='form' method='post'>\n");
        } else {	// is "/task"
        	html.append("<form action='task' method='post'>\n");
        }
    	Enumeration<?> paramNames = request.getParameterNames();
    	while (paramNames.hasMoreElements()) {
    		String name = (String)paramNames.nextElement();
    		String value = request.getParameter(name);
    		html.append("<input type='hidden' name='").append(name);
    		html.append("' value='").append(value).append("'>\n");
    	}
    	html.append("<center>\n");
    	html.append("<table border='1'>\n");
    	html.append("<tr><td>User Name</td><td><input type='text' name='user'/></td></tr>\n");
    	html.append("<tr><td>Password</td><td><input type='password' name='pass'/></td></tr>\n");
    	html.append("<tr><td colspan='2' align='center'><input type='submit' value='Log In'></td></tr>\n");
    	html.append("</table>\n");
    	html.append("</center>\n");
    	html.append("</form>\n");
    	html.append("<p>\n");
    	html.append("<i>This authentication page is for development environment only,\n");
    	html.append("ClearTrust filter will kick in instead in production/test environments.</i>\n");
    	html.append("</body>\n");
    	html.append("</html>\n");
    	response.setContentType("text/html");
    	response.getOutputStream().print(html.toString());
    }

    private void populateTaskInstanceStandardData(FormDataDocument datadoc, TaskInstanceVO taskInst,
    		HtmlMDWSession mdw)
    {
    	if (taskInst.getOwnerType().equals(OwnerType.USER)) {
	    	datadoc.setMetaValue(FormDataDocument.META_USER, mdw.getCuid());
            datadoc.setMetaValue(FormDataDocument.META_PRIVILEGES, mdw.privileges);
            datadoc.setMetaValue(FormDataDocument.META_TASK_ASSIGN_STATUS, FormDataDocument.ASSIGN_STATUS_SELF);
            return;
		}
    	String v = taskInst.getStartDate();
    	if (v!=null && v.length()>10) v = v.substring(0,10);
    	else v = "";
    	datadoc.setMetaValue(FormDataDocument.META_TASK_START_DATE, v);
    	v = taskInst.getEndDate();
    	if (v!=null && v.length()>10) v = v.substring(0,10);
    	else v = "";
    	datadoc.setMetaValue(FormDataDocument.META_TASK_END_DATE, v);
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	Date dd = taskInst.getDueDate();
    	if (dd!=null) v = df.format(dd);
    	else v = "";
    	datadoc.setMetaValue(FormDataDocument.META_TASK_DUE_DATE, v);
    	v = TaskStatuses.getTaskStatuses().get(taskInst.getStatusCode());
    	datadoc.setMetaValue(FormDataDocument.META_TASK_STATUS, v);
    	datadoc.setMetaValue(FormDataDocument.META_TASK_ASSIGNEE, taskInst.getTaskClaimUserCuid());
    	datadoc.setMetaValue(FormDataDocument.META_TASK_NAME, taskInst.getTaskName());
    	String taskinstid = taskInst.getTaskInstanceId().toString();
    	datadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, taskinstid);
    	datadoc.setMetaValue(FormDataDocument.META_TASK_OWNER_APPL, taskInst.getOwnerApplicationName());
    	datadoc.setMetaValue(FormDataDocument.META_TITLE, "Task " + taskinstid);
    	datadoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, taskInst.getMasterRequestId());
    	datadoc.setMetaValue(FormDataDocument.META_USER, mdw.getCuid());
    	datadoc.setMetaValue(FormDataDocument.META_PRIVILEGES, mdw.privileges);
    	if (taskInst.getComments()!=null)
    		datadoc.setMetaValue(FormDataDocument.META_TASK_COMMENT, taskInst.getComments());
    	boolean isClassicTask = OwnerType.WORK_TRANSITION_INSTANCE.equals(taskInst.getSecondaryOwnerType());
    	datadoc.setMetaValue(FormDataDocument.META_TASK_ASSIGN_STATUS, datadoc.getAssignStatus());
    	if (isClassicTask) {
    		datadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, taskInst.getOwnerId().toString());
    		datadoc.setAttribute(FormDataDocument.ATTR_NAME, taskInst.getTaskId().toString());
    		datadoc.setMetaValue("ACTIVITY_NAME", taskInst.getActivityName());
    	} else {
    		List<String> groups = taskInst.getGroups();
    		if (groups!=null) {
    			StringBuffer sb = new StringBuffer();
    			for (String g : groups) {
    				if (sb.length()>0) sb.append(",");
    				sb.append(g);
    			}
    			datadoc.setMetaValue(FormDataDocument.META_TASK_GROUPS, sb.toString());
    		}
    	}
    }

    private void update_model_server(FormDataDocument datadoc, HttpServletRequest request,
    		Long taskInstId, FormSession mdw) {
    	Enumeration<?> paramNames = request.getParameterNames();
    	while (paramNames.hasMoreElements()) {
    		String name = (String)paramNames.nextElement();
    		String value = request.getParameter(name);
    		if (name.equals("mdw_formdata")) continue;
    		try {
				datadoc.setValue(name, value);
			} catch (MbengException e) {
				logger.exception(this.logtag(taskInstId,mdw), "Failed update data for " + name, e);
			}
    	}
    }

    private void setInitializationScript(FormDataDocument datadoc, String script) {
    	datadoc.setMetaValue(FormDataDocument.META_INITIALIZATION, script);
    }

    @Override
    protected void show_dialog(String formname, Long taskInstId, Map<String,String> params,
    		FormDataDocument datadoc, FormSession mdw) {
    	try {
    		StringBuffer sb = new StringBuffer();
    		String dialogId = params.get(FormConstants.URLARG_UNIQUE_ID);
    		if (dialogId==null) {	// this is the case primarily for PAGELET forms
    			// open dialog specified as form name
    			if (formname.startsWith(FormConstants.TABLE_ROW_DIALOG_PREFIX)) {
    				String tableId = formname.substring(FormConstants.TABLE_ROW_DIALOG_PREFIX.length());
    				dialogId = tableId + "_rowdialog";
    				sb.append("table_row_view('").append(tableId).append("','")
    					.append(dialogId).append("');");
    			} else {
    				dialogId = formname;
    				sb.append("outputData('#").append(dialogId).append(" .dialogDataOut');");
    	    		sb.append("$('#").append(dialogId).append("').dialog('open');");
    			}
    			datadoc.setMetaValue(FormDataDocument.META_DIALOG, formname);
    		}  else {	// dialogId already specified - primarily for HTML forms
	    		sb.append("outputData('#").append(dialogId).append(" .dialogDataOut');");
	    		sb.append("$('#").append(dialogId).append("').dialog('open');");
    		}
    		// cannot call dialog_open as that calls inputData which may override newly updated data
    		setInitializationScript(datadoc, sb.toString());
        } catch (Exception e) {
        	String message = "Failed to load form " + formname;
            logger.exception(logtag(taskInstId,mdw), message, e);
            datadoc.addError(message + ": " + e.getMessage());
        }
    }

    @Override
    protected void show_window(String formname, Long taskInstId,
    		Map<String,String> params, FormDataDocument datadoc, FormSession mdw) {
    	try {
    		int w, h;
    		RuleSetVO formdoc = getFormDocument(formname);
    		if (formdoc!=null && formdoc.getLanguage().equals(RuleSetVO.FORM)) {
    			DomDocument formdocdom = (DomDocument)formdoc.getCompiledObject();
    			String v = formdocdom.getRootNode().getAttribute(FormConstants.FORMATTR_VW);
    			w = v!=null?(Integer.parseInt(v) + 30):800;
    			v = formdocdom.getRootNode().getAttribute(FormConstants.FORMATTR_VH);
    			h = v!=null?(Integer.parseInt(v) + 40):600;
    		} else {
    			w = 800;
    			h = 600;
    		}
    		StringBuffer sb = new StringBuffer();
    		sb.append("window.open('form?");
    		sb.append(FormConstants.URLARG_FORMNAME).append("=").append(formname);
    		boolean hasCustomInputRef = false;
     		if (params!=null && params.size()>0) {
    			for (String p : params.keySet()) {
    				if (p.equals(FormConstants.URLARG_FORMNAME)) continue;
    				sb.append("&").append(p).append("=").append(params.get(p));
    				if (p.equals(FormConstants.URLARG_INPUTREF)) hasCustomInputRef = true;
    			}
    		}
    		if (!hasCustomInputRef) {
    	   		sb.append("&").append(FormConstants.URLARG_INPUTREF).append("=copy");
        		sb.append("&").append(FormConstants.URLARG_PARENT).append("=").append(taskInstId.toString());
    		}
    		sb.append("','_blank',");
    		sb.append("'toolbar=0,location=0,directories=0,status=1,menubar=0,");
//    				sb.append("top=0,left=0,");
    		sb.append("scrollbars=1,resizable=1,bottom=0,width=")
    			.append(w).append(",height=").append(h).append("')");
    		setInitializationScript(datadoc, sb.toString());
        } catch (Exception e) {
        	String message = "Failed to load form " + formname;
            logger.exception(logtag(taskInstId,mdw), message, e);
            datadoc.addError(message + ": " + e.getMessage());
        }
    }

    /**
     * Handle HTML Ajax request (data are passed in as parameters).
     * The response can be HTML, HTML fragment, JSON and plain text
     * @param pRequest
     * @param pResponse
     */
	public void handleHtmlAjax(HttpServletRequest pRequest, HttpServletResponse pResponse) {
		String func = pRequest.getParameter("function");
		String response;
		if ("taskUserList".equals(func)) {
			try {
				String taskinstid = pRequest.getParameter("taskInstId");
				// task instance ID was got from MDW session
				HtmlMDWSession mdw = this.getMDWSession(pRequest);
				TaskInstanceVO taskInst = mdw.getWindow(new Long(taskinstid)).getTaskInstance();
				if (taskInst==null) throw new Exception("There is no task instance");
				UserVO[] users = TaskManagerAccess.getInstance().getAssignableUsersForTaskInstance(taskInst);
				StringBuffer sb = new StringBuffer();
				sb.append("<option></option>");
				for (UserVO user : users) {
					sb.append("<option value='").append(user.getCuid()).append("'>")
						.append(user.getName()).append("</option>");
				}
				response = sb.toString();
			} catch (Exception e) {
				logger.severeException(e.getMessage(), e);
				response = "<option>Exception</option>";
			}
		} else {
			try {
				if (func==null) throw new Exception("function not provided in ajax call");
				FormDataDocument datadoc = new FormDataDocument();
	    		datadoc.setAttribute(FormDataDocument.ATTR_ACTION, func);
	    		Enumeration<?> paramNames = pRequest.getParameterNames();
	    		while (paramNames.hasMoreElements()) {
	    			String key = (String) paramNames.nextElement();
	    			datadoc.setValue(key, pRequest.getParameter(key).toString());
	    		}
	    		String timeout = pRequest.getParameter(FormConstants.URLARG_TIMEOUT);
    			int timeoutSeconds = timeout==null?120:Integer.parseInt(timeout);
		    	response = (new EngineAccess()).callEngine(datadoc.formatJson(), timeoutSeconds, null);
			} catch (Exception e) {
				response = "ERROR: " + e.getClass().getName() + " - " + e.getMessage();
			}
		}
		try {
			pResponse.getWriter().write(response);
		} catch (Exception e) {
			logger.severeException(e.getMessage(), e);
		}
	}

	/**
	 * Handle a JSON request from client.
     * The response is a JSON document as well
	 * @param request request in JSON string
	 * @param httpRequest
	 * @return response in JSON string
	 */
	public String handleJsonAjax(String request, HttpServletRequest httpRequest) {
		String response;
		FormDataDocument datadoc = new FormDataDocument();
		Long taskInstId = null;
		HtmlMDWSession mdw = null;
		try {
			datadoc.loadJson(request);
			HttpSession session = httpRequest.getSession(false);
			// HTTP session is supposed to be created by hosting environment
	    	if (session==null) throw new Exception("HTTP session does not exist");
	    	mdw = (HtmlMDWSession)session.getAttribute("MDWSession");
			if (mdw==null) {
				AuthenticatedUser authuser = (AuthenticatedUser) session.getAttribute("authenticatedUser");
				if (authuser==null) {
					String cuid = datadoc.getMetaValue(FormDataDocument.META_USER);
					if (cuid==null) throw new Exception("No user name is provided for starting a session");
					authuser = TaskManagerAccess.getInstance().getUserAuthorization(cuid, true);
					session.setAttribute("authenticatedUser", authuser);
				}
				mdw = new HtmlMDWSession(authuser);
				session.setAttribute("MDWSession", mdw);
			}
			mdw.session = session;
			taskInstId = datadoc.getTaskInstanceId();
			TaskInstanceVO taskInst;
			FormWindow window;
			if (logger.isDebugEnabled())
				logger.debug(logtag(taskInstId,mdw), "json from browser: " + request);
				window = mdw.getWindow(taskInstId);
			if (window==null) {
				taskInst = TaskManagerAccess.getInstance().getTaskInstance(taskInstId);
				if (taskInst==null) throw new Exception("Task instance does not exist: " + taskInstId);
				window = new FormWindow(taskInst, null);
				mdw.putWindow(taskInstId, window);
			} else {
				taskInst = window.getTaskInstance();
	       		clear_error_and_init(datadoc);		// clear error, initialization, and prompt
			}
			String action = datadoc.getAttribute(FormDataDocument.ATTR_ACTION);
			if (action==null || action.length()==0) {
				super.loadTaskData(datadoc, taskInst);
			} else {
				CallURL callurl = new CallURL(action);
				datadoc = performAction(mdw, callurl, datadoc, taskInst);
				taskInst = window.getTaskInstance();		// taskInst may be changed by performAction
			}
			populateTaskInstanceStandardData(datadoc, taskInst, mdw);
			response = datadoc.formatJson();
			if (logger.isDebugEnabled())
				logger.debug(logtag(taskInstId,mdw), "json to browser: " + response);
		} catch (Exception e) {
			try {
				String message = e.getMessage();
				if (taskInstId!=null&&mdw!=null) logger.exception(logtag(taskInstId,mdw), message, e);
				else logger.severeException(message, e);
				datadoc.addError(message);
				response = datadoc.formatJson();
			} catch (JSONException e1) {
            	response = "{\"ERROR\":[\"error in generating JSON error messages\"]}";
			}
		}
		return response;
	}

	// TODO assign to a specified user (currently using assignee var)
	// TODO send notification by variable

}