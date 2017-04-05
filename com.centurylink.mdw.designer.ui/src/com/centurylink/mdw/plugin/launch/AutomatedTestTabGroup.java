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
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.centurylink.mdw.plugin.PluginMessages;

public class AutomatedTestTabGroup extends AbstractLaunchConfigurationTabGroup {
    private ILaunchConfigurationDialog dialog;
    private FunctionTestLaunchTab functionTestTab;
    private LoadTestLaunchTab loadTestTab;

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        this.dialog = dialog;
        functionTestTab = new FunctionTestLaunchTab(ILaunchManager.DEBUG_MODE.equals(mode));
        loadTestTab = new LoadTestLaunchTab();

        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { functionTestTab,
                loadTestTab, new EnvironmentTab(), new CommonTab() };
        setTabs(tabs);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        super.initializeFrom(configuration);

        try {
            if (configuration.getAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, false))
                dialog.setActiveTab(loadTestTab);
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }
    }

}
