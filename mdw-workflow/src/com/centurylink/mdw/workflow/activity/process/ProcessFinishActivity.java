/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.types.FinishActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;


/**
 * Base class for all the ProcessFinish Controlled Activity
 * This class will be extended by the custom ProcessFinish activity
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Process Stop", category=FinishActivity.class, icon="shape:stop",
        pagelet="com.centurylink.mdw.base/processFinish.pagelet")
public class ProcessFinishActivity extends DefaultActivityImpl implements FinishActivity {

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
