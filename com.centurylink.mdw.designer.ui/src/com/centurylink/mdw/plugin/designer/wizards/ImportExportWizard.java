/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.SwtProgressMonitor;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class ImportExportWizard extends Wizard
{
  private ImportExportPage page;
  protected ImportExportPage getPage() { return page; }

  private IWorkbench workbench;
  public IWorkbench getWorkbench() { return workbench; }

  private List<WorkflowElement> elements;
  public List<WorkflowElement> getElements() { return elements; }
  public void setElements(List<WorkflowElement> elements) { this.elements = elements; }

  /**
   * for most cases selections always contain a single element
   */
  public WorkflowElement getElement()
  {
    if (elements == null || elements.isEmpty())
      return null;
    return elements.get(0);
  }
  public void setElement(WorkflowElement element)
  {
    elements = new ArrayList<WorkflowElement>();
    elements.add(element);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
    setNeedsProgressMonitor(true);
    setWindowTitle(this instanceof IExportWizard ? "MDW Export" : "MDW Import");

    page = createPage();
    page.isExport = this instanceof IExportWizard;

    if (selection != null && selection.getFirstElement() instanceof WorkflowElement) {
      elements = new ArrayList<WorkflowElement>();
      for (Object item : selection.toList())
        elements.add((WorkflowElement)item);
    }
  }

  abstract ImportExportPage createPage();
  abstract void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ValidationException, ActionCancelledException;

  /**
   * Override to reflect import operations in UI.
   */
  protected void postRunUpdates()
  {
  }

  @Override
  public void addPages()
  {
    addPage(page);
  }

  @Override
  public boolean performFinish()
  {
    IRunnableWithProgress op = new IRunnableWithProgress()
    {
      public void run(IProgressMonitor monitor) throws InvocationTargetException
      {
        ProgressMonitor progressMonitor = new SwtProgressMonitor(monitor);
        try
        {
          progressMonitor.start((page.isExport ? "Exporting from " : "Importing into ") + "'" + getProject().getLabel() + "'");
          progressMonitor.progress(5);

          performImportExport(progressMonitor);
          progressMonitor.done();
        }
        catch (ActionCancelledException ex)
        {
          throw new OperationCanceledException();
        }
        catch (Exception ex)
        {
          PluginMessages.log(ex);
          throw new InvocationTargetException(ex);
        }
      }
    };

    try
    {
      getContainer().run(true, true, op);
      postRunUpdates();
      return true;
    }
    catch (InvocationTargetException ex)
    {
      if (ex.getCause() instanceof DataAccessOfflineException)
        MessageDialog.openError(getShell(), "Export Attributes", "Server appears to be offline.");
      else
        PluginMessages.uiError(getShell(), ex, page.getTitle(), getProject());
      return false;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(getShell(), ex, page.getTitle(), getProject());
      return false;
    }
  }

  protected byte[] readFile(String filePath) throws IOException
  {
    return PluginUtil.readFile(new File(filePath));
  }

  protected void writeFile(String filePath, byte[] contents) throws IOException
  {
    PluginUtil.writeFile(new File(filePath), contents);
  }

  protected WorkflowProject getProject()
  {
    if (getElement() == null)
      return null;
    return getElement().getProject();
  }

  protected WorkflowPackage getPackage()
  {
    if (getElement() == null)
      return null;
    if (getElement() instanceof WorkflowPackage)
      return (WorkflowPackage) getElement();
    return getElement().getPackage();
  }

  /**
   * For exporting multiple packages.
   */
  protected List<WorkflowPackage> getPackages()
  {
    if (getElements() == null)
      return null;
    List<WorkflowPackage> packages = new ArrayList<WorkflowPackage>();
    for (WorkflowElement element : getElements())
      packages.add(element.getPackage());
    return packages;
  }

  protected WorkflowProcess getProcess()
  {
    WorkflowElement element = getElement();
    if (element instanceof WorkflowProcess)
      return (WorkflowProcess)element;
    else
      return null;
  }

  protected WorkflowAsset getAsset()
  {
    WorkflowElement element = getElement();
    if (element instanceof WorkflowAsset)
      return (WorkflowAsset)element;
    else
      return null;
  }

  public boolean needsProgressMonitor()
  {
    return true;
  }
}
