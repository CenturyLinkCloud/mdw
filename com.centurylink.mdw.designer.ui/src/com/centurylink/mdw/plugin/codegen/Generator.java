/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;

public class Generator {
    private Shell shell;

    public Generator(Shell shell) {
        this.shell = shell;
    }

    /**
     * creates a file in the eclipse project and opens it in the editor window
     * 
     * @param jet
     * @param monitor
     * @throws CoreException
     */
    public void createFileAndOpen(Shell shell, JetAccess jet, IProgressMonitor monitor)
            throws CoreException {
        IContainer container = jet.findOrCreateContainer(monitor, jet.getConfig().getTargetFolder(),
                jet.getConfig().getPackageName());
        IFile targetFile = container.getFile(new Path(jet.getConfig().getTargetFile()));
        targetFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
        if (targetFile.exists()) {
            if (!(MessageDialog.openQuestion(shell, "Confirm Overwrite",
                    "Target file :'" + targetFile.getLocation() + "' exists.  Overwrite?")))
                return;
        }
        IFile file = createFile(jet, monitor);
        openFile(file);
    }

    /**
     * creates a file under the eclipse project
     * 
     * @param jet
     * @param monitor
     * @return
     * @throws CoreException
     */
    public IFile createFile(JetAccess jet, IProgressMonitor monitor) throws CoreException {
        try {
            String content = jet.generate(monitor);
            return jet.save(monitor, content.getBytes());
        }
        catch (JETException ex) {
            throw createCoreException(ex);
        }
    }

    /**
     * appends the jet generated output onto the end of an existing file
     * 
     * @param jet
     * @param monitor
     * @return
     * @throws CoreException
     */
    public IFile appendFile(JetAccess jet, IProgressMonitor monitor) throws CoreException {
        try {
            String newContent = jet.generate(monitor);
            return jet.append(monitor, newContent.getBytes());
        }
        catch (IOException ex) {
            throw createCoreException(ex);
        }
        catch (JETException ex) {
            throw createCoreException(ex);
        }
    }

    /**
     * merges the jet generated output into an xml file inside of the specified
     * containing xml element (into each occurrence)
     * 
     * @param jet
     * @param monitor
     * @param elementName
     * @return IFile
     * @throws CoreException
     */
    public IFile mergeXmlFile(JetAccess jet, IProgressMonitor monitor, String elementName)
            throws CoreException {
        try {
            String newContent = jet.generate(monitor);
            return jet.mergeXml(monitor, newContent.getBytes(), elementName);
        }
        catch (IOException ex) {
            throw createCoreException(ex);
        }
        catch (JETException ex) {
            throw createCoreException(ex);
        }
    }

    public void openFile(IFile file) {
        selectAndReveal(file);
        openResource(shell, file);
    }

    protected void openResource(Shell shell, final IResource resource) {
        if (resource.getType() == IResource.FILE) {
            final IWorkbenchPage activePage = MdwPlugin.getActivePage();
            if (activePage != null) {
                final Display display = shell.getDisplay();
                if (display != null) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            try {
                                IDE.openEditor(activePage, (IFile) resource, true);
                            }
                            catch (PartInitException ex) {
                                PluginMessages.log(ex);
                            }
                        }
                    });
                }
            }
        }
    }

    protected void selectAndReveal(IResource newResource) {
        BasicNewResourceWizard.selectAndReveal(newResource, MdwPlugin.getActiveWorkbenchWindow());
    }

    private CoreException createCoreException(Exception ex) {
        return new CoreException(new Status(Status.ERROR, MdwPlugin.getPluginId(), Status.ERROR,
                "ERROR: " + ex, ex));
    }

    /**
     * Sets the MDWCommon.jar file as CLASSPATH variable in Eclipse. Needed so
     * that the JET Emitter framework can find MDWCommon at runtime.
     */
    protected void setMdwCommonClasspathVariable() throws JavaModelException {
        URL url = null;
        try {
            url = PluginUtil.getLocalResourceUrl("base/APP-INF/lib/MDWCommon.jar");
        }
        catch (IOException e) {
            int code = IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST;
            JavaModelException jme = new JavaModelException(e, code);
            throw jme;
        }
        IPath path = new Path(url.getFile());
        if (!path.equals(JavaCore.getClasspathVariable("MDW_COMMON"))) {
            JavaCore.setClasspathVariable("MDW_COMMON", path, null);
        }
    }
}
