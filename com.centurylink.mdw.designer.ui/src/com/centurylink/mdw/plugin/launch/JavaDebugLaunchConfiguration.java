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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;

/**
 * Special mapping for this class in plugin.xml allows custom source locator for
 * dynamic Java.
 */
@SuppressWarnings("restriction")
public class JavaDebugLaunchConfiguration extends JavaRemoteApplicationLaunchConfigurationDelegate {
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        disconnectDebugTargets();
        super.launch(configuration, mode, launch, monitor);
        debugTargets = launch.getDebugTargets();
    }

    private static IDebugTarget[] debugTargets;

    private static void disconnectDebugTargets() throws DebugException {
        if (debugTargets != null) {
            for (IDebugTarget target : debugTargets) {
                if (target.canDisconnect())
                    target.disconnect();
            }
        }
    }

}
