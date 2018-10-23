/*
 * Copyright (C) 2018 CenturyLink, Inc.
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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

public class ProcessPauseActivity extends DefaultActivityImpl implements SuspendibleActivity {

    @Override
    public boolean needSuspend() throws ActivityException {
        return true;
    }

    @Override
    public boolean resume(InternalEvent eventMessageDoc) throws ActivityException {
        return true;
    }

    @Override
    public boolean resumeWaiting(InternalEvent event) throws ActivityException {
        return true;
    }
}
