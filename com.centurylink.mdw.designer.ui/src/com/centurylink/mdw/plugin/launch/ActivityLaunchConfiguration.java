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
package com.centurylink.mdw.plugin.launch;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ActivityLaunchConfiguration extends ProcessLaunchConfiguration {
    public static final String ACTIVITY_ID = "activityId";

    private Long activityId;

    public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        String activityIdStr = launchConfig.getAttribute(ACTIVITY_ID, "");
        try {
            activityId = Long.valueOf(activityIdStr);
        }
        catch (NumberFormatException ex) {
            showError("Can't locate activity ID: '" + activityIdStr + "'.", "Activity Launch",
                    null);
            return;
        }

        super.launch(launchConfig, mode, launch, monitor);
    }

    @Override
    protected void launchProcess(WorkflowProcess process, String masterRequestId, String owner,
            Long ownerId, boolean synchronous, String responseVarName,
            Map<String, String> parameters, Long activityId, boolean showLogs, int logWatchPort,
            boolean liveView) {
        super.launchProcess(process, masterRequestId, owner, ownerId, synchronous, responseVarName,
                parameters, this.activityId, showLogs, logWatchPort, liveView);
    }
}
