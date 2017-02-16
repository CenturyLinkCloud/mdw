package com.centurylink.mdw.plugin.actions;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.dialogs.FileSaveDialog;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * This is a simple conversion, and does not take into account escaping property
 * names that have spaces, special chars, etc.
 */
public class ConvertApplicationProperties implements IObjectActionDelegate {
    private ISelection selection;

    public ISelection getSelection() {
        return selection;
    }

    private Shell shell;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            if (structuredSelection.getFirstElement() instanceof IFile) {
                final IFile inputFile = (IFile) structuredSelection.getFirstElement();
                final WorkflowProject project = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(inputFile.getProject().getName());

                String ext = project == null || project.isOsgi() ? "cfg" : "properties";
                FileSaveDialog saveDialog = new FileSaveDialog(
                        MdwPlugin.getActiveWorkbenchWindow().getShell());
                saveDialog.setFilterPath(inputFile.getParent().getRawLocation().makeAbsolute()
                        .toFile().getAbsolutePath());
                saveDialog.setFilterExtensions(new String[] { "*" + ext });
                final String filePath = saveDialog.open();
                if (filePath != null) {
                    BusyIndicator.showWhile(
                            MdwPlugin.getActiveWorkbenchWindow().getShell().getDisplay(),
                            new Runnable() {
                                public void run() {

                                    try {
                                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                                                .newInstance();
                                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                                        Document doc = dBuilder.parse(inputFile.getContents());

                                        doc.getDocumentElement().normalize();

                                        StringBuffer sb = new StringBuffer();

                                        NodeList nList = doc.getElementsByTagName("PropertyGroup");

                                        // for every property group
                                        for (int temp = 0; temp < nList.getLength(); temp++) {
                                            Node nNode = nList.item(temp);

                                            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                                                Element eElement = (Element) nNode;

                                                String propertyGroupName = eElement
                                                        .getAttribute("Name");
                                                // create PropertyGroup comment
                                                sb.append("\n");
                                                sb.append("#");
                                                sb.append(propertyGroupName);
                                                sb.append("\n");

                                                NodeList propertyList = eElement
                                                        .getElementsByTagName("Property");
                                                // for every property in the
                                                // group
                                                for (int temp2 = 0; temp2 < propertyList
                                                        .getLength(); temp2++) {
                                                    Node nNode2 = propertyList.item(temp2);
                                                    if (nNode2.getNodeType() == Node.ELEMENT_NODE) {

                                                        Element eElement2 = (Element) nNode2;
                                                        // format to:
                                                        // propertyGroup-propertyName=elementValue
                                                        sb.append(propertyGroupName);
                                                        if (project == null || project.isOsgi())
                                                            sb.append("-");
                                                        else
                                                            sb.append("/");
                                                        sb.append(eElement2.getAttribute("Name"));
                                                        sb.append("=");
                                                        sb.append(nNode2.getTextContent());
                                                        sb.append("\n");
                                                    }
                                                }
                                            }
                                        }
                                        PluginUtil.writeFile(new File(filePath),
                                                sb.toString().getBytes());
                                        inputFile.getParent().refreshLocal(IResource.DEPTH_ONE,
                                                new NullProgressMonitor());
                                    }
                                    catch (Exception ex) {
                                        PluginMessages.uiError(shell, ex,
                                                "Convert Application Properties", project);
                                    }
                                }
                            });
                }
            }
        }
    }
}
