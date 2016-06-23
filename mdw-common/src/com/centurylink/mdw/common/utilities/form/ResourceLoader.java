/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.form;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public interface ResourceLoader {

	RuleSetVO getResource(String name, String language, int version)
	throws DataAccessException;
	
	String getProperty(String packageName, String propertyName);
	
}
