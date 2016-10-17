/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;


import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.form.FormAction;
import com.centurylink.mdw.listener.FormEventHandler;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

/*
 * The class could extend only ExternalEventHandlerBase and implement
 * handleEventMessage() as a dummy method (never called).
 * But extending FormEventHandler allows to instantiate the class directly
 * from ListenerHelper.
 */

public abstract class FormActionBase extends FormEventHandler implements FormAction {

	protected Long findPackageId(FormDataDocument datadoc) {
		String packageName = datadoc.getMetaValue(FormDataDocument.META_PACKAGE_NAME);
		return packageName==null?null:PackageVOCache.getPackage(packageName).getId();
	}    
	
	protected String getProperty(Long packageId, String propertyName) {
		PackageVO pkg = PackageVOCache.getPackage(packageId);
		return pkg.getProperty(propertyName);
	}
	
	protected boolean hasRole(FormDataDocument datadoc, String group, String role) {
        String priv_string = datadoc.getMetaValue(FormDataDocument.META_PRIVILEGES);
		if (priv_string==null) return false;
		UserVO user = new UserVO("me", priv_string);
		while (group!=null) {
			if (user.hasRole(group, role)) return true;
			UserGroupVO groupObject;
			try {
				groupObject = UserGroupCache.getWorkgroup(group);
				if (group.equals(UserGroupVO.SITE_ADMIN_GROUP)) group = null;
				else if (groupObject==null) group = UserGroupVO.SITE_ADMIN_GROUP;	// case for common
				else if (groupObject.getParentGroup()==null) group = UserGroupVO.SITE_ADMIN_GROUP;
				else group = groupObject.getParentGroup();
			} catch (CachingException e) {
				group = null;
			}
		}
		return false;
	}
	
	protected boolean hasRoleInAnyGroup(FormDataDocument datadoc, String role) {
        String priv_string = datadoc.getMetaValue(FormDataDocument.META_PRIVILEGES);
		if (priv_string==null) return false;
		UserVO user = new UserVO("me", priv_string);
		for (UserGroupVO group : user.getWorkgroups()) {
			if (group.hasRole(role)) return true;
		}
		return false;
	}
	
	protected void verifyRole(FormDataDocument datadoc, String group, String role) throws UserException {
		if (!hasRole(datadoc,group,role)) throw new UserException("You need to have " + role 
				+ " role in " + group + " group to perform this function");
	}

}
