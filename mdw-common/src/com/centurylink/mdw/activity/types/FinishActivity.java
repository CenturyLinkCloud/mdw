/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;


/**
 * Interface for all Finish Activities
 */
public interface FinishActivity extends GeneralActivity{

	String getProcessCompletionCode();
	
	boolean doNotNotifyCaller();
}
