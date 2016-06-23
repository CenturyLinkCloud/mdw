/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

@Deprecated
public class FormSession {

	private AuthenticatedUser user;
	private UserVO actAsUser;
	private Map<Long,FormWindow> windows;
	private FormWindow firstWindow;

	public FormSession(AuthenticatedUser user) {
		this.user = user;
		actAsUser = null;
		firstWindow = null;
		windows = new HashMap<Long,FormWindow>();
	}

    public String getCuid() { return actAsUser!=null?actAsUser.getCuid():user!=null?user.getCuid():null; }

    public void setUser(AuthenticatedUser user) { this.user = user; }

    public UserVO getRealUser() { return user; }

    public UserVO getActAsUser() { return actAsUser==null?user:actAsUser; }

	public void setActAsUser(UserVO actAsUser) {
		this.actAsUser = actAsUser;
	}

	public FormWindow getWindow(Long taskInstId) {
		return windows.get(taskInstId);
	}

	public void putWindow(Long taskInstId, FormWindow window) {
		if (firstWindow==null) firstWindow = window;
		windows.put(taskInstId, window);
	}

	public void logoff() {
		windows.clear();
		firstWindow = null;
	}

	public void deleteClosedWindows() {
		List<Long> closed = new ArrayList<Long>();
		for (Long tid : windows.keySet()) {
			FormWindow window = windows.get(tid);
			if (window.isToDelete()) closed.add(tid);
		}
		for (Long tid : closed) {
			windows.remove(tid);
		}
	}

	public FormWindow getFirstWindow() {
		return firstWindow;
	}

	public boolean hasRole(String group, String role) {
		UserVO actingUser = actAsUser!=null?actAsUser:user;
		while (group!=null) {
			if (actingUser.hasRole(group, role)) return true;
			UserGroupVO g;
			try {
				g = UserGroupCache.getWorkgroup(group);
				if (group.equals(UserGroupVO.SITE_ADMIN_GROUP)) group = null;
				else if (g==null) group = null;
				else if (g.getParentGroup()==null) group = UserGroupVO.SITE_ADMIN_GROUP;
				else group = g.getParentGroup();
			} catch (CachingException e) {
				group = null;
			}
		}
		return false;
	}

	public void verifyRole(String group, String role) throws UserException {
		if (!hasRole(group,role)) throw new UserException("You need to have " + role
				+ " role in " + group + " group to perform this function");
	}

}
