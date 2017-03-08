/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.Map;

import org.codehaus.groovy.eclipse.launchers.AbstractGroovyLaunchShortcut;
import org.codehaus.groovy.eclipse.launchers.GroovyScriptLaunchShortcut;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * AutoTests for mdw6.
 */
public class AutoTestLaunchShortcut extends AbstractGroovyLaunchShortcut {

    public static final String GROUP_ID = "com.centurylink.mdw.plugin.launch.group.auto.test";
    public static final String TYPE_ID = "com.centurylink.mdw.plugin.launch.AutomatedTest";

    private AutoTestCaseRun testCaseRun;
    private TestCase testCase;
    protected TestCase getTestCase() { return testCase; }
    private WorkflowProject project;
    protected WorkflowProject getProject() { return project; }

    public AutoTestLaunchShortcut() {
    }

    public AutoTestLaunchShortcut(AutoTestCaseRun testCaseRun) {
        this.testCaseRun = testCaseRun;
        this.testCase = testCaseRun.getTestCase();
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
        return "com.centurylink.mdw.services.test.TestRunner";
    }

    @Override
    protected boolean canLaunchWithNoType() {
        return true;
    }

    @Override
    protected Map<String, String> createLaunchProperties(IType runType, IJavaProject javaProject) {
        Map<String, String> launchConfigProperties = super.createLaunchProperties(runType, javaProject);

        String vmArgs = launchConfigProperties.get(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS);
        vmArgs += "-Dmdw.test.case=" + getTestCase().getCaseName();

        if (getTestCase().isLegacy())
            vmArgs += " -Dmdw.test.cases.dir=\""
                    + project.getOldTestCasesDir().toString().replace('\\', '/') + "\"";
        else
            vmArgs += " -Dmdw.test.case.file=\""
                    + getTestCase().getCaseFile().toString().replace('\\', '/') + "\"";

        vmArgs += " -Dmdw.test.case.user=" + project.getUser().getUsername();
        vmArgs += " -Dmdw.test.server.url=" + project.getServiceUrl();

        if (testCaseRun.isStubbing()) {
            vmArgs += " -Dmdw.test.server.stub=true";
            vmArgs += " -Dmdw.test.server.stubPort="
                    + project.getServerSettings().getStubServerPort();
        }

        if (testCaseRun.isSingleServer())
            vmArgs += " -Dmdw.test.pin.to.server=true";

        if (testCaseRun.isCreateReplace())
            vmArgs += " -Dmdw.test.create.replace=true";

        vmArgs += " -Dmdw.test.results.dir=\""
                + getTestCase().getResultDirectory().toString().replace('\\', '/') + "\"";

        vmArgs += " -Dmdw.test.workflow.dir=\""
                + project.getAssetDir().toString().replace('\\', '/') + "\"";

        if (testCaseRun.isVerbose())
            vmArgs += " -Dmdw.test.verbose=true";

        if (testCaseRun.getMasterRequestId() != null)
            vmArgs += " -Dmdw.test.masterRequestId=" + testCaseRun.getMasterRequestId();

        launchConfigProperties.put(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

        launchConfigProperties.put(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, classToRun());

        return launchConfigProperties;
    }
}