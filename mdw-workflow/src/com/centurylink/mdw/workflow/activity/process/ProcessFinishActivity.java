/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.types.FinishActivity;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;


/**
 * Base class for all the ProcessFinish Controlled Activity
 * This class will be extended by the custom ProcessFinish activity
 */
@Tracked(LogLevel.TRACE)
public class ProcessFinishActivity extends DefaultActivityImpl
    implements FinishActivity {

    private static final String ATTRIBUTE_COMPLETION_CODE = "CompletionCode";
    private static final String ATTRIBUTE_TERMINATION_ACTION = "TerminationAction";
    private static final String ATTRIBUTE_NO_NOTIFY = "DoNotNotifyCaller";

//    private static final String ATTRVALUE_DEFAULT = "Default";
    private static final String ATTRVALUE_COMPLETE_PROCESS = "Complete Process";
    private static final String ATTRVALUE_CANCEL_PROCESS = "Cancel Process";

    /**
     * Default constructor with params
     */
    public ProcessFinishActivity(){
    	super();
    }

    public String getProcessCompletionCode() {
    	String av = this.getAttributeValue(ATTRIBUTE_TERMINATION_ACTION);
    	if (av==null) return null;
    	if (av.equals(ATTRVALUE_COMPLETE_PROCESS)) {
    		av = this.getAttributeValue(ATTRIBUTE_COMPLETION_CODE);
    		return EventType.EVENTNAME_FINISH + (av==null?"":(":" + av));
    	} else if (av.equals(ATTRVALUE_CANCEL_PROCESS)) {
    		return EventType.EVENTNAME_ABORT + ":process";
    	} else return null;
    }

    public boolean doNotNotifyCaller() {
    	return "true".equalsIgnoreCase(this.getAttributeValue(ATTRIBUTE_NO_NOTIFY));
    }

}
