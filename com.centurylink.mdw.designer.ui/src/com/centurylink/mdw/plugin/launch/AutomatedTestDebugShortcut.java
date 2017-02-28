package com.centurylink.mdw.plugin.launch;

import org.codehaus.groovy.eclipse.launchers.GroovyScriptLaunchShortcut;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
//Todo : need to implement
public class AutomatedTestDebugShortcut extends GroovyScriptLaunchShortcut{
    public static final String GROUP_ID = "com.centurylink.mdw.plugin.debug.group.automated.test";
    public static final String TYPE_ID = "com.centurylink.mdw.plugin.debug.AutomatedTest";

    /**
     * Launches from the source file.
     *
     * @see ILaunchShortcut#launch
     */
    public void launch(IEditorPart editor, String mode) {

        System.out.println("Debug shortcut invoked");

    }

}
