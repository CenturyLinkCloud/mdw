/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;


public class ActivityRuntime {
	
	public static final int STARTCASE_NORMAL = 0;
	public static final int STARTCASE_PROCESS_TERMINATED = 1;
	public static final int STARTCASE_ERROR_IN_PREPARE = 2;
	public static final int STARTCASE_RESUME_WAITING = 3;
	public static final int STARTCASE_INSTANCE_EXIST = 4;
	public static final int STARTCASE_SYNCH_COMPLETE = 5;
	public static final int STARTCASE_SYNCH_WAITING = 6;
	public static final int STARTCASE_SYNCH_HOLD = 7;
	public static final int RESUMECASE_NORMAL = 8;
	public static final int RESUMECASE_PROCESS_TERMINATED = 9;
	public static final int RESUMECASE_ACTIVITY_NOT_WAITING = 10;

	int startCase;
	BaseActivity activity;
	ActivityInstance actinst;
	ProcessInstance procinst;
	
	public int getStartCase() {
		return startCase;
	}
	
	public ActivityInstance getActivityInstance() {
		return actinst;
	}

	public ProcessInstance getProcessInstance() {
		return procinst;
	}
	
	public BaseActivity getActivity() {
		return activity;
	}
}
