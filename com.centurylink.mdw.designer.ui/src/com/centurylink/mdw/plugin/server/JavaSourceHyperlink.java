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
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.sourcelookup.IJavaSourceLocation;
import org.eclipse.jdt.launching.sourcelookup.PackageFragmentRootSourceLocation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Hyperlink for java source code referenced in a stack trace.
 */
@SuppressWarnings("deprecation")
public class JavaSourceHyperlink implements IConsoleHyperlink {

    private String line;
    private IJavaProject project;

    /**
     * creates an instance of a JavaSourceHyperlink based on a line of output
     *
     * @param line
     * @param project
     * @return
     */
    public static JavaSourceHyperlink create(String line, IJavaProject project) {
        if (line.indexOf(".java:") >= 0) {
            return new JavaSourceHyperlink(line, project);
        }
        else {
            return null;
        }
    }

    /**
     * don't instantiate directly, use create()
     * 
     * @param line
     * @param project
     */
    private JavaSourceHyperlink(String line, IJavaProject project) {
        this.line = line;
        this.project = project;
    }

    @SuppressWarnings("restriction")
    public void linkActivated() {
        String linkText = getLinkText();
        if (linkText.startsWith("string:///")) // dynamic java
        {
            try {
                WorkflowProject workflowProj = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(project);

                // package
                String path = linkText.substring(10);
                int slashIdx = path.indexOf('/');
                String pkg = slashIdx > 0 ? path.substring(0, slashIdx) : null;

                // asset
                int javaIdx = path.indexOf(".java");
                String assetName = path.substring(slashIdx > 0 ? slashIdx + 1 : 0, javaIdx);

                // lineNum
                String lineNum = "";
                int i = javaIdx + 6;
                while (i < path.length() && Character.isDigit(path.charAt(i)))
                    lineNum += path.charAt(i++);

                WorkflowAsset asset = workflowProj.getAsset(pkg, assetName);
                if (asset == null) {
                    cantFindSourceWarning();
                }
                else {
                    asset.openFile(new NullProgressMonitor());
                    if (lineNum.length() > 0) {
                        ITextEditor editor = (ITextEditor) asset.getFileEditor();
                        if (editor != null) {
                            IDocument document = editor.getDocumentProvider()
                                    .getDocument(editor.getEditorInput());
                            IRegion lineInfo = document
                                    .getLineInformation(Integer.parseInt(lineNum) - 1);
                            if (lineInfo != null)
                                editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
                        }
                    }
                }
            }
            catch (Exception ex) {
                cantFindSourceWarning();
                PluginMessages.log(ex);
            }
            return;
        }

        int lineNumber = getLineNumber();
        // documents start at 0
        if (lineNumber > 0)
            lineNumber--;

        try {
            Object sourceElement = getJavaElement(getTypeName());
            if (sourceElement != null) {
                IDebugModelPresentation presentation = org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin
                        .getDefault().getModelPresentation();
                IEditorInput editorInput = presentation.getEditorInput(sourceElement);
                if (editorInput != null) {
                    String editorId = presentation.getEditorId(editorInput, sourceElement);
                    if (editorId != null) {
                        IEditorPart editorPart = org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin
                                .getActivePage().openEditor(editorInput, editorId);
                        if (editorPart instanceof ITextEditor && lineNumber >= 0) {
                            ITextEditor textEditor = (ITextEditor) editorPart;
                            IDocumentProvider provider = textEditor.getDocumentProvider();
                            provider.connect(editorInput);
                            IDocument document = provider.getDocument(editorInput);
                            try {
                                IRegion line = document.getLineInformation(lineNumber);
                                textEditor.selectAndReveal(line.getOffset(), line.getLength());
                            }
                            catch (BadLocationException e) {
                            }
                            provider.disconnect(editorInput);
                        }
                        return;
                    }
                }
            }
        }
        catch (CoreException ex) {
            cantFindSourceWarning();
            PluginMessages.log(ex);
            return;
        }
        // did not find source
        cantFindSourceWarning();

    }

    /**
     * returns the fully qualified name of the type to open
     */
    protected String getTypeName() {
        String linkText = getLinkText();
        int index = linkText.indexOf('(');
        if (index >= 0) {
            String typeName = linkText.substring(0, index);
            // remove the method name
            index = typeName.lastIndexOf('.');
            int innerClassIndex = typeName.lastIndexOf('$', index);
            if (innerClassIndex != -1)
                index = innerClassIndex;
            if (index >= 0) {
                typeName = typeName.substring(0, index);
            }
            return typeName;
        }
        else {
            PluginMessages.log("Can't parse type name: " + getLinkText());
            return null;
        }
    }

    /**
     * returns the line number associated with the stack trace or 1 if none
     */
    protected int getLineNumber() {
        String linkText = getLinkText();
        int index = linkText.lastIndexOf(':');
        String numText = linkText.substring(index + 1, linkText.length());
        try {
            return Integer.parseInt(numText);
        }
        catch (NumberFormatException ex) {
            PluginMessages.log(ex);
            return 1;
        }
    }

    /**
     * returns the full text of the link
     */
    protected String getLinkText() {
        int endIdx = line.indexOf(".java:") + 6;
        while (endIdx < line.length() - 1 && Character.isDigit(line.charAt(endIdx)))
            endIdx++;
        int startIdx = line.lastIndexOf(" ", endIdx - 1);
        if (startIdx < 0 || endIdx < 0 || startIdx > endIdx)
            return null; // can't parse
        return line.substring(startIdx + 1, endIdx);
    }

    /**
     * find the source element in the project
     *
     * @param type
     * @return the source element
     * @throws JavaModelException
     * @throws CoreException
     */
    protected Object getJavaElement(String type) throws JavaModelException, CoreException {
        Object element = null;

        if (project == null) {
            cantFindSourceWarning();
            return null;
        }

        if (element == null) {
            QualifiedName qName = new QualifiedName(MdwPlugin.getPluginId(),
                    "MdwSourceLookupAllJavaProjects");
            String setting = project.getProject().getPersistentProperty(qName);
            boolean check = (setting != null && Boolean.valueOf(setting));
            if (!check) {
                String msg = "Can't locate source code for:\n" + getTypeName()
                        + (project == null ? "" : " in project " + project.getProject().getName());
                String toggleMessage = "Look in all workspace Java projects for this deployment.";
                MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                        MdwPlugin.getShell(), "Java Source Lookup", msg, toggleMessage, false, null,
                        null);
                check = dialog.getToggleState();
                project.getProject().setPersistentProperty(qName, String.valueOf(check));
            }
            if (check) {
                IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
                for (IJavaProject javaProj : javaModel.getJavaProjects()) {
                    Object javaElem = getJavaElement(javaProj, type);
                    if (javaElem != null) {
                        element = javaElem;
                        break;
                    }
                }
            }
        }
        return element;
    }

    protected Object getJavaElement(IJavaProject project, String type)
            throws JavaModelException, CoreException {
        if (project == null)
            return null;

        IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();

        for (int i = 0; i < roots.length; i++) {
            PackageFragmentRootSourceLocation loc = new PackageFragmentRootSourceLocation(roots[i]);
            IJavaSourceLocation rootLoc = (IJavaSourceLocation) loc;
            Object element = rootLoc.findSourceElement(type);
            if (element != null) {
                return element;
            }
        }

        // not found
        return null;
    }

    public int getLinkStart() {
        String linkText = getLinkText();
        return linkText == null ? 0 : line.indexOf(linkText);
    }

    public int getLinkEnd() {
        String linkText = getLinkText();
        return linkText == null ? 0 : line.indexOf(linkText) + linkText.length();
    }

    public void linkEntered() {
    }

    public void linkExited() {
    }

    private void cantFindSourceWarning() {
        String msg = "Can't locate source code for:\n" + getTypeName()
                + "\nMake sure an MDW workflow project is deployed.";
        MessageDialog.openWarning(MdwPlugin.getShell(), "No Java Element Selected", msg);
    }
}