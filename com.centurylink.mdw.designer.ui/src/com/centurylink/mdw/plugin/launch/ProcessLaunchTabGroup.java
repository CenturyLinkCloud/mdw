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
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaConnectTab;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.launch.ExternalEventTab.Mode;

public class ProcessLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
    private ProcessLaunchMainTab mainTab;
    private ProcessVariablesTab variablesTab;
    private ExternalEventTab externalEventTab;
    private NotifyProcessTab notifyProcessTab;
    private JavaConnectTab connectTab;
    private CommonTab commonTab;

    private ILaunchConfigurationDialog dialog;

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        this.dialog = dialog;

        mainTab = new ProcessLaunchMainTab();
        variablesTab = new ProcessVariablesTab();
        externalEventTab = new ExternalEventTab(Mode.ProcessLaunch);
        notifyProcessTab = new NotifyProcessTab();
        commonTab = new CommonTab();
        if (mode.equals(ILaunchManager.DEBUG_MODE))
            connectTab = new JavaConnectTab();

        ILaunchConfigurationTab[] tabs;
        if (mode.equals(ILaunchManager.DEBUG_MODE)) {
            tabs = new ILaunchConfigurationTab[] { mainTab, variablesTab, externalEventTab,
                    notifyProcessTab, connectTab, commonTab };
        }
        else {
            tabs = new ILaunchConfigurationTab[] { mainTab, variablesTab, externalEventTab,
                    notifyProcessTab, commonTab };
        }

        setTabs(tabs);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        super.initializeFrom(configuration);

        try {
            if (configuration.getAttribute(ProcessLaunchConfiguration.LAUNCH_VIA_EXTERNAL_EVENT,
                    false))
                dialog.setActiveTab(externalEventTab);
            else if (configuration.getAttribute(ProcessLaunchConfiguration.NOTIFY_PROCESS, false))
                dialog.setActiveTab(notifyProcessTab);
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }
    }
}
