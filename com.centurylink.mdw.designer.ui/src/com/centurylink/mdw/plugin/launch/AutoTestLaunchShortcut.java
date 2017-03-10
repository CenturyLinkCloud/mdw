/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
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

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * AutoTests for mdw6.
 */
public class AutoTestLaunchShortcut extends AbstractGroovyLaunchShortcut {

    private AutoTestCaseRun testCaseRun;
    private WorkflowProject project;

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

    @Override
    protected Map<String, String> createLaunchProperties(IType runType, IJavaProject javaProject) {
        Map<String,String> launchConfigProperties = super.createLaunchProperties(runType, javaProject);

        String vmArgs = launchConfigProperties.get(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS);

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
            } catch (JavaModelException e) {
                GroovyCore.errorRunningGroovy(e);
                return;
            }
            runType = findClassToRun(types);
            if (runType == null) {
                GroovyCore.errorRunningGroovy(new Exception("Unable to find run type: " + unit));
                return;
            }
        }

        Map<String,String> launchConfigProperties = createLaunchProperties(runType, javaProject);

        try {
            ILaunchConfigurationWorkingCopy workingConfig = findOrCreateLaunchConfig(launchConfigProperties,
                    runType != null ? runType.getElementName() : javaProject.getElementName());
            workingConfig.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, Arrays.asList(
                            JavaRuntime.computeDefaultRuntimeClassPath(javaProject)));
            ILaunchConfiguration config = workingConfig.doSave();
            DebugPlugin.getDefault().addDebugEventListener(new AutoTestDebugListener(config, testCaseRun.getTestCase(), testCaseRun.getLog()));
            DebugUITools.launch(config, mode);
        }
        catch (CoreException e) {
            GroovyCore.errorRunningGroovyFile((IFile) unit.getResource(), e);
        }
    }
}