/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.Map;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.model.FormDataDocument;

public class TabChanger extends FormActionBase {

	/**
	 * This is only to show how tab changer should look like.
	 * The implementation here does not load additional data,
	 * which the real implementation should do.
	 */
	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String tabsId = params.get(FormConstants.URLARG_TABS);
			String tabIndex = params.get(FormConstants.URLARG_TAB);
			String tabIndexField = params.get(FormConstants.URLARG_DATA);
			//
			// generate desired data here for the specified tab
			// (tabIndex starts from 0)
			//
			if (tabIndexField!=null && tabIndexField.length()>0) datadoc.setValue(tabIndexField, tabIndex);
			System.out.println("Tabs " + tabsId + " is changing to tab " + tabIndex);
		} catch (Exception e) {
			e.printStackTrace();
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}
	
	
}
