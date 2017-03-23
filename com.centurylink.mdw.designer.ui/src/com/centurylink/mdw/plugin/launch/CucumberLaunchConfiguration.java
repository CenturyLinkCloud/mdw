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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.centurylink.mdw.plugin.PluginUtil;

/**
 * For general (non-MDW) Cucumber feature tests.
 */
public class CucumberLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate
        implements ILaunchConfigurationDelegate2 {
    protected static final String CUCUMBER_CLI_MAIN = "cucumber.api.cli.Main";

    // redeclared here to avoid importing discouraged eclipse packages
    public static final String ATTR_MAPPED_RESOURCE_PATHS = "org.eclipse.debug.core.MAPPED_RESOURCE_PATHS";
    public static final String ATTR_MAPPED_RESOURCE_TYPES = "org.eclipse.debug.core.MAPPED_RESOURCE_TYPES";
    public static final String ATTR_USE_START_ON_FIRST_THREAD = "org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD";
    public static final String RESOURCE_TYPE_PROJECT = "4";

    public static final String FOLDER = "folder";
    public static final String FEATURES = "features";
    public static final String GLUE = "glue";

    public static final String DEFAULT_GLUE = "classpath:";
    public static final String DEFAULT_ARGS = "--plugin pretty --monochrome --strict";

    @Override
    public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        String folderPath = config.getAttribute(FOLDER, "");
        List<String> featurePaths = config.getAttribute(FEATURES, new ArrayList<String>());

        IVMInstall vm = verifyVMInstall(config);
        IVMRunner runner = vm.getVMRunner(mode);
        String[] classpath = getClasspath(config);
        VMRunnerConfiguration runConfig = new VMRunnerConfiguration(CUCUMBER_CLI_MAIN, classpath);

        verifyWorkingDirectory(config);

        String[] bootpath = getBootpath(config);
        runConfig.setBootClassPath(bootpath);

        List<String> vmArgs = PluginUtil
                .arrayToList(DebugPlugin.parseArguments(getVMArguments(config)));
        // TODO: add debug args based on mode?
        // vmArgs += " -Xdebug
        // -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8020";
        String featuresArg = "-Dcucumber.options=";
        for (int i = 0; i < featurePaths.size(); i++) {
            String featurePath = featurePaths.get(i);
            featuresArg += folderPath.isEmpty() ? featurePath : (folderPath + "/" + featurePath);
            if (i < featurePaths.size() - 1)
                featuresArg += " ";
        }
        vmArgs.add(featuresArg);
        runConfig.setVMArguments(vmArgs.toArray(new String[0]));

        runConfig.setWorkingDirectory(getWorkingDirectory(config).getAbsolutePath());

        List<String> args = PluginUtil
                .arrayToList(DebugPlugin.parseArguments(getProgramArguments(config)));
        args.add("--glue");
        args.add(config.getAttribute(GLUE, DEFAULT_GLUE));
        runConfig.setProgramArguments(args.toArray(new String[0]));

        runner.run(runConfig, launch, monitor);
    }
}