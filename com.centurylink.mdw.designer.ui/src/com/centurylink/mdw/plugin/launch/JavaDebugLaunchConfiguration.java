/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
