/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.form;

import java.util.Map;

import com.centurylink.mdw.model.FormDataDocument;

public interface FormAction {
	
	FormDataDocument handleAction(FormDataDocument datadoc, Map<String,String> params);

}
