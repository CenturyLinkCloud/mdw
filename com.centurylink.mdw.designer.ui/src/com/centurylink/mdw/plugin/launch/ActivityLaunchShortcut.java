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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;

public class ActivityLaunchShortcut extends ProcessLaunchShortcut {
    public static final String ACTIVITY_LAUNCH_CONFIG_TYPE = "com.centurylink.mdw.plugin.launch.Activity";

    public void launch(ISelection selection, String mode) {
        Object firstElement = ((StructuredSelection) selection).getFirstElement();
        if (firstElement instanceof Activity) {
            Activity activity = (Activity) firstElement;
            try {
                boolean prevEnablement = disableBuildBeforeLaunch();
                performLaunch(activity, mode);
                setBuildBeforeLaunch(prevEnablement);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Activity Launch", activity.getProject());
            }
        }
    }

    private void performLaunch(Activity activity, String mode)
            throws InterruptedException, CoreException {
        boolean debug = mode.equals(ILaunchManager.DEBUG_MODE);
        ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(
                ACTIVITY_LAUNCH_CONFIG_TYPE, activity.getProcess(), debug);
        workingCopy.setAttribute(ActivityLaunchConfiguration.ACTIVITY_ID,
                activity.getId().toString());
        ILaunchConfiguration config = findExistingLaunchConfiguration(workingCopy,
                activity.getProject(), debug);
        if (config == null) {
            // no existing found - create a new one
            config = workingCopy.doSave();
        }

        Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
        IStructuredSelection selection = new StructuredSelection(config);
        String groupId = "com.centurylink.mdw.plugin.launch.group.mdw.activity";
        if (mode.equals(ILaunchManager.DEBUG_MODE))
            groupId += ".debug";

        DebugUITools.openLaunchConfigurationDialogOnGroup(shell, selection, groupId);
    }

}
