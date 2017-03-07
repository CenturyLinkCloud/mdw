package com.centurylink.mdw.plugin.launch;

import org.codehaus.groovy.eclipse.launchers.AbstractGroovyLaunchShortcut;
import org.eclipse.debug.core.ILaunchConfigurationType;

public class GroovyAutoTestShortcut extends AbstractGroovyLaunchShortcut {

    public static final String GROOVY_SCRIPT_LAUNCH_CONFIG_ID = "org.codehaus.groovy.eclipse.groovyScriptLaunchConfiguration" ;

    @Override
    public ILaunchConfigurationType getGroovyLaunchConfigType() {
        return getLaunchManager().getLaunchConfigurationType(GROOVY_SCRIPT_LAUNCH_CONFIG_ID) ;
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