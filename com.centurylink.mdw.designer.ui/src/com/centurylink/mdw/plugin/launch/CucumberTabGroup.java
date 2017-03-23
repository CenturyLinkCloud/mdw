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

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

/**
 * This is for standalone (non-MDW) cucumber tests.
 */
public class CucumberTabGroup extends AbstractLaunchConfigurationTabGroup {
    private CucumberLaunchTab cucumberTab;

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        cucumberTab = new CucumberLaunchTab();

        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { cucumberTab,
                new JavaArgumentsTab(), new JavaJRETab(), new JavaClasspathTab(), new CommonTab() };
        setTabs(tabs);
    }
}
