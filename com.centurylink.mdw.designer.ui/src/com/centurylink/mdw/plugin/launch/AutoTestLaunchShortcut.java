/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.codehaus.groovy.eclipse.launchers.AbstractGroovyLaunchShortcut;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.codehaus.groovy.eclipse.launchers.GroovyScriptLaunchShortcut;

/**
 * AutoTests for mdw6.
 */
public class AutoTestLaunchShortcut extends AbstractGroovyLaunchShortcut {

    public static final String GROUP_ID = "com.centurylink.mdw.plugin.launch.group.auto.test";
    public static final String TYPE_ID = "com.centurylink.mdw.plugin.launch.AutomatedTest";

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
        return "groovy.ui.GroovyMain";
    }


    @Override
    protected boolean canLaunchWithNoType() {
        return false;
    }
}