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

import java.util.Arrays;
import java.util.Map;

import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.launchers.AbstractGroovyLaunchShortcut;
import org.codehaus.groovy.eclipse.launchers.GroovyScriptLaunchShortcut;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * AutoTests for mdw6.
 */
public class AutoTestLaunchShortcut extends AbstractGroovyLaunchShortcut {

    private AutoTestCaseRun testCaseRun;
    private WorkflowProject project;

    private AutoTestDebugListener debugListener;

    ILaunchConfiguration getLaunchConfiguration() {
        if (debugListener == null)
            return null;
        return debugListener.getLaunchConfig();
    }

    public AutoTestLaunchShortcut() {
    }

    public AutoTestLaunchShortcut(AutoTestCaseRun testCaseRun) {
        this.testCaseRun = testCaseRun;
        this.project = testCaseRun.getAutoTestCase().getProject();
    }

    @Override
    public ILaunchConfigurationType getGroovyLaunchConfigType() {
        return getLaunchManager().getLaunchConfigurationType(GroovyScriptLaunchShortcut.GROOVY_SCRIPT_LAUNCH_CONFIG_ID) ;
    }

    @Override
    protected String applicationOrConsole() {
        return "script";
    }


    @Override
    protected String classToRun() {
        return "com.centurylink.mdw.services.test.TestCaseMain";
    }

    @Override
    protected boolean canLaunchWithNoType() {
        return true;
    }

    private String vmArgs;

    @Override
    protected Map<String,String> createLaunchProperties(IType runType, IJavaProject javaProject) {
        Map<String,String> launchConfigProperties = super.createLaunchProperties(runType, javaProject);

        launchConfigProperties.put(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                "com.centurylink.mdw.services.test.TestCaseMain$GroovyStarter");

        vmArgs = launchConfigProperties.get(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS);

        vmArgs += " -Dmdw.runtime.env=standalone";

        vmArgs += " -Dmdw.asset.root=" + project.getAssetDir();

        vmArgs += " -Dmdw.test.server.url=" + project.getServiceUrl();

        if (testCaseRun.isStubbing()) {
            vmArgs += " -Dmdw.test.server.stubPort="
                    + project.getServerSettings().getStubServerPort();
        }

        if (testCaseRun.isSingleServer())
            vmArgs += " -Dmdw.test.pin.to.server=true";

        if (testCaseRun.isCreateReplace())
            vmArgs += " -Dmdw.test.create.replace=true";

        if (testCaseRun.isVerbose())
            vmArgs += " -Dmdw.test.verbose=true";

        vmArgs += " -Dmdw.test.user=" + project.getUser().getUsername();

        if (testCaseRun.getMasterRequestId() != null)
            vmArgs += " -Dmdw.test.master.request.id=" + testCaseRun.getMasterRequestId();

        launchConfigProperties.put(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

        return launchConfigProperties;
    }

    @Override
    protected void launchGroovy(ICompilationUnit unit, IJavaProject javaProject, String mode) {
        IType runType = null;

        // if unit is null, then we are not looking for a run type
        if (unit != null) {
            IType[] types = null;
            try {
                types = unit.getAllTypes();
            }
            catch (JavaModelException ex) {
                GroovyCore.errorRunningGroovy(ex);
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
            runType = findClassToRun(types);
            if (runType == null) {
                GroovyCore.errorRunningGroovy(new Exception("Unable to find run type: " + unit));
                throw new IllegalStateException("Unable to find run type: " + unit);
            }
        }

        Map<String,String> launchConfigProperties = createLaunchProperties(runType, javaProject);

        try {
            ILaunchConfigurationWorkingCopy workingConfig = findOrCreateLaunchConfig(launchConfigProperties,
                    runType != null ? runType.getElementName() : javaProject.getElementName());
            workingConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, Arrays.asList(
                        JavaRuntime.computeDefaultRuntimeClassPath(javaProject)));
            workingConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
            ILaunchConfiguration launchConfig = workingConfig.doSave();
            DebugPlugin.getDefault().addDebugEventListener(new AutoTestDebugListener(launchConfig, testCaseRun.getTestCase(), testCaseRun.getLog()));
            DebugUITools.launch(launchConfig, mode);

            // don't return until execution complete
            try {
                while (testCaseRun.getTestCase().getStatus().equals(TestCase.STATUS_RUNNING)
                        || testCaseRun.getTestCase().getStatus().equals(TestCase.STATUS_WAITING))
                    Thread.sleep(500);
            }
            catch (InterruptedException ex) {
            }
        }
        catch (CoreException ex) {
            MessageDialog.openError(MdwPlugin.getShell(), "Autotest Error", ex.getMessage());
            GroovyCore.errorRunningGroovyFile((IFile) unit.getResource(), ex);
        }
    }
}