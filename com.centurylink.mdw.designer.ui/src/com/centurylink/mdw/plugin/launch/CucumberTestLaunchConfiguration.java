/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 * Private launch configuration for Cucumber MDW test cases.
 * (See CucumberLaunchConfiguration for non-MDW Cucumber tests).
 */
public class CucumberTestLaunchConfiguration extends CucumberLaunchConfiguration
{
  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
  {
    IVMInstall vm = verifyVMInstall(config);
    IVMRunner runner = vm.getVMRunner(mode);
    String[] classpath = getClasspath(config);
    VMRunnerConfiguration runConfig = new VMRunnerConfiguration(CUCUMBER_CLI_MAIN, classpath);

    verifyWorkingDirectory(config);

    String[] bootpath = getBootpath(config);
    runConfig.setBootClassPath(bootpath);
    runConfig.setVMArguments(DebugPlugin.parseArguments(getVMArguments(config)));
    runConfig.setWorkingDirectory(getWorkingDirectory(config).getAbsolutePath());

    String[] args = DebugPlugin.parseArguments(getProgramArguments(config));
    runConfig.setProgramArguments(args);

    runner.run(runConfig, launch, monitor);
  }
}
