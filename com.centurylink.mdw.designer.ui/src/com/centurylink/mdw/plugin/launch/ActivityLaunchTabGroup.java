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

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaConnectTab;

public class ActivityLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
    private ActivityLaunchMainTab mainTab;
    private ProcessVariablesTab variablesTab;
    private JavaConnectTab connectTab;
    private CommonTab commonTab;

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        mainTab = new ActivityLaunchMainTab();
        variablesTab = new ProcessVariablesTab();
        commonTab = new CommonTab();
        if (mode.equals(ILaunchManager.DEBUG_MODE))
            connectTab = new JavaConnectTab();

        ILaunchConfigurationTab[] tabs;
        if (mode.equals(ILaunchManager.DEBUG_MODE)) {
            tabs = new ILaunchConfigurationTab[] { mainTab, variablesTab, connectTab, commonTab };
        }
        else {
            tabs = new ILaunchConfigurationTab[] { mainTab, variablesTab, commonTab };
        }

        setTabs(tabs);
    }
}