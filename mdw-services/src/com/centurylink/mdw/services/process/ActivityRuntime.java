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
