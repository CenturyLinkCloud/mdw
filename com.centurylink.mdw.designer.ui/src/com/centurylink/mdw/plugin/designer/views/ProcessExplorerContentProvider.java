/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.SwtProgressMonitor;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessExplorerContentProvider implements ITreeContentProvider, ElementChangeListener
{
  private static Object[] EMPTY_ARRAY = new Object[0];

  private TreeViewer treeViewer;

  @SuppressWarnings("unchecked")
  public Object[] getElements(Object inputElement)
  {
    List<WorkflowProject> workflowProjects = (List<WorkflowProject>)inputElement;
    return workflowProjects.toArray(new WorkflowProject[0]);
  }

  public Object[] getChildren(Object parentElement)
  {
    if (parentElement instanceof WorkflowProject)
    {
      final WorkflowProject workflowProject = (WorkflowProject) parentElement;

      if (!workflowProject.isLoaded())
      {
        try
        {
          IRunnableWithProgress loader = new IRunnableWithProgress()
          {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
              ProgressMonitor progressMonitor = new SwtProgressMonitor(monitor);
              progressMonitor.start("Loading " + workflowProject.getLabel());
              progressMonitor.progress(5);
              try
              {
                workflowProject.initialize(progressMonitor);
                if (workflowProject.getDataAccess() != null)
                {
                  workflowProject.getTopLevelUserVisiblePackages(progressMonitor);
                  workflowProject.getArchivedUserVisiblePackagesFolder(progressMonitor);
                  progressMonitor.done();
                }
              }
              catch (Exception ex)
              {
                throw new InvocationTargetException(ex);
              }
            }
          };
          ProgressMonitorDialog progMonDlg = new MdwProgressMonitorDialog(MdwPlugin.getShell());
          progMonDlg.run(true, false, loader);
        }
        catch (InvocationTargetException itx)
        {
          if (itx.getCause() instanceof DataAccessOfflineException)
          {
            PluginMessages.log(itx);
            MessageDialog.openError(MdwPlugin.getShell(), "Load Workflow Project", itx.getCause().getMessage());
            return new WorkflowPackage[0];
          }
          else
          {
            PluginMessages.uiError(itx, "Load Workflow Project", workflowProject);
            return new WorkflowPackage[0];
          }
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(ex, "Load Workflow Project", workflowProject);
          return new WorkflowPackage[0];
        }
      }

      if (workflowProject.getDataAccess() == null)
        return new WorkflowPackage[0];

      List<WorkflowPackage> topLevelPackages = workflowProject.getTopLevelUserVisiblePackages();
      Folder archivedPackageFolder = workflowProject.getArchivedUserVisiblePackagesFolder();

      int size = topLevelPackages.size();
      boolean showArchived = isShowArchivedItems(workflowProject);
      if (showArchived)
        size++;

      AutomatedTestSuite testSuite = workflowProject.getLegacyTestSuite();
      if (testSuite != null && !testSuite.isEmpty())
        size++;

      Object[] objects = new Object[size];
      for (int i = 0; i < topLevelPackages.size(); i++)
        objects[i] = topLevelPackages.get(i);

      int cur = topLevelPackages.size();
      if (showArchived)
      {
        objects[cur] = archivedPackageFolder;
        cur++;
      }

      if (testSuite != null && !testSuite.isEmpty())
        objects[cur] = testSuite;

      return objects;
    }
    else if (parentElement instanceof WorkflowPackage)
    {
      WorkflowPackage packageVersion = (WorkflowPackage) parentElement;
      if (packageVersion.isArchived() && packageVersion.hasDescendantPackageVersions())
      {
        return packageVersion.getDescendantPackageVersions().toArray(new WorkflowPackage[0]);
      }
      else
      {
        List<WorkflowElement> elements = new ArrayList<WorkflowElement>();
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();

        if (!prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_PROCESSES_IN_PEX))
          elements.addAll(packageVersion.getProcesses());
        if (!prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_WORKFLOW_ASSETS_IN_PEX))
          elements.addAll(packageVersion.getAssets());
        if (!prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_EVENT_HANDLERS_IN_PEX))
          elements.addAll(packageVersion.getExternalEvents());
        if (prefsStore.getBoolean(PreferenceConstants.PREFS_SHOW_ACTIVITY_IMPLEMENTORS_IN_PEX))
          elements.addAll(packageVersion.getActivityImpls());
        if (!prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_TASK_TEMPLATES_IN_PEX))
          elements.addAll(packageVersion.getTaskTemplates());
        elements.addAll(packageVersion.getChildFolders());

        if (isSortPackageContentsAtoZ())
        {
          Collections.sort(elements, new Comparator<WorkflowElement>()
          {
            public int compare(WorkflowElement e1, WorkflowElement e2)
            {
              return e1.getLabel().compareToIgnoreCase(e2.getLabel());
            }
          });
        }

        return elements.toArray(new Object[0]);
      }
    }
    else if (parentElement instanceof WorkflowProcess)
    {
      WorkflowProcess processVersion = (WorkflowProcess) parentElement;
      if (processVersion.hasDescendantProcessVersions())
      {
        return processVersion.getDescendantProcessVersions().toArray(new WorkflowProcess[0]);
      }
      else
      {
        return EMPTY_ARRAY;
      }
    }
    else if (parentElement instanceof Folder)
    {
      Folder folder = (Folder) parentElement;
      return folder.getChildren().toArray(new WorkflowElement[0]);
    }
    else if (parentElement instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) parentElement;
      return testSuite.getTestCases().toArray(new AutomatedTestCase[0]);
    }
    else if (parentElement instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) parentElement;
      if (!testCase.isLegacy())
        return EMPTY_ARRAY;
      List<LegacyExpectedResults> expectedResults = testCase.getLegacyExpectedResults();
      List<File> files = testCase.getFiles();
      Object[] objects = new Object[expectedResults.size() + files.size()];
      for (int i = 0; i < expectedResults.size(); i++)
        objects[i] = expectedResults.get(i);
      for (int i = expectedResults.size(); i < objects.length; i++)
        objects[i] = files.get(i - expectedResults.size());
      return objects;
    }
    else
    {
      return EMPTY_ARRAY;
    }
  }

  public boolean isSortPackageContentsAtoZ()
  {
    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
    return prefsStore.getBoolean(PreferenceConstants.PREFS_SORT_PACKAGE_CONTENTS_A_TO_Z);
  }

  public boolean isShowArchivedItems(WorkflowProject workflowProject)
  {
    if (workflowProject.isRemote() && workflowProject.isFilePersist()) {
      if (!workflowProject.getMdwVcsRepository().isSyncAssetArchive())
        return false;
    }
    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
    return !prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_ARCHIVED_ITEMS_IN_PEX);
  }

  public Object getParent(Object element)
  {
    if (element instanceof WorkflowProject)
    {
      return null;
    }
    else if (element instanceof WorkflowPackage)
    {
      WorkflowPackage packageVersion = (WorkflowPackage) element;
      if (packageVersion.isArchived())
      {
        if (packageVersion.isTopLevel())
          return packageVersion.getArchivedFolder();
        else
          return packageVersion.getTopLevelVersion();
      }
      else
      {
        return packageVersion.getProject();
      }
    }
    else if (element instanceof WorkflowProcess)
    {
      WorkflowProcess processVersion = (WorkflowProcess) element;
      if (processVersion.isTopLevel())
      {
        if (processVersion.getPackage() != null)
          return processVersion.getPackage();
        else
          return processVersion.getProject().getDefaultPackage();
      }
      else
      {
        return processVersion.getTopLevelVersion();
      }
    }
    else if (element instanceof Folder)
    {
      Folder folder = (Folder) element;
      return folder.getProject();
    }
    else if (element instanceof ExternalEvent)
    {
      ExternalEvent externalEvent = (ExternalEvent) element;
      if (externalEvent.isInDefaultPackage())
        return externalEvent.getProject().getDefaultPackage();
      else
        return externalEvent.getPackage();
    }
    else if (element instanceof WorkflowAsset)
    {
      WorkflowAsset asset = (WorkflowAsset) element;
      if (asset.isInDefaultPackage())
        return asset.getProject().getDefaultPackage();
      else
        return asset.getPackage();
    }
    else if (element instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
      return testSuite.getProject();
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      if (testCase.isLegacy())
        return testCase.getTestSuite();
    }
    else if (element instanceof LegacyExpectedResults)
    {
      LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
      return expectedResult.getTestCase();
    }
    else if (element instanceof File)
    {
      File file = (File) element;
      return file.getParent();
    }
    return null;
  }

  public boolean hasChildren(Object element)
  {
    if (element instanceof WorkflowProject)
    {
      return true;
    }
    else if (element instanceof WorkflowPackage)
    {
      WorkflowPackage packageVersion = (WorkflowPackage) element;
      IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
      return packageVersion.hasDescendantPackageVersions()
        || (packageVersion.hasProcesses() && !prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_PROCESSES_IN_PEX))
        || (packageVersion.hasAssets() && !prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_WORKFLOW_ASSETS_IN_PEX))
        || (packageVersion.hasExternalEvents() && !prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_EVENT_HANDLERS_IN_PEX))
        || (packageVersion.hasActivityImpls() && prefsStore.getBoolean(PreferenceConstants.PREFS_SHOW_ACTIVITY_IMPLEMENTORS_IN_PEX))
        || (packageVersion.hasTaskTemplates() && !prefsStore.getBoolean(PreferenceConstants.PREFS_FILTER_TASK_TEMPLATES_IN_PEX))
        || packageVersion.hasChildFolders();
    }
    else if (element instanceof WorkflowProcess)
    {
      WorkflowProcess processVersion = (WorkflowProcess) element;
      return processVersion.hasDescendantProcessVersions();
    }
    else if (element instanceof Folder)
    {
      Folder packageFolder = (Folder) element;
      return packageFolder.hasChildren();
    }
    else if (element instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
      return testSuite.getTestCases().size() > 0;
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      return testCase.isLegacy() && testCase.getLegacyExpectedResults().size() > 0 || testCase.getFiles().size() > 0;
    }
    else
    {
      return false;
    }
  }

  public void dispose()
  {
    dispose(treeViewer.getInput());
  }

  private void dispose(Object element)
  {
    if (element instanceof WorkflowElement)
      ((WorkflowElement)element).dispose();
    for (Object child : getChildren(element))
      dispose(child);
  }

  /**
   * Registers this content provider as a listener to changes on the new input
   * (for workflow element changes), and deregisters the viewer from the old input.
   * In response to these ElementChangeEvents, we update the viewer.
   *
   * For now we are only listening for changes to top level processes and packages
   * as well as definition documents such as scripts, templates and pages.
   */
  @SuppressWarnings("unchecked")
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
    this.treeViewer = (TreeViewer) viewer;
    viewer.setSelection(null);

    if (newInput != oldInput)
    {
      if (oldInput != null)
      {
        List<WorkflowProject> oldProjects = (List<WorkflowProject>) oldInput;
        for (WorkflowProject oldProject : oldProjects)
          oldProject.removeElementChangeListener(this);
      }
      if (newInput != null)
      {
        List<WorkflowProject> newProjects = (List<WorkflowProject>) newInput;
        for (WorkflowProject newProject : newProjects)
          newProject.addElementChangeListener(this);
      }
    }
  }

  public void elementChanged(final ElementChangeEvent ece)
  {
    if (!treeViewer.getTree().isDisposed())
    {
      // updates to the treeViewer must be on the UI thread
      treeViewer.getTree().getDisplay().asyncExec(new Runnable()
      {
        public void run()
        {
          handleElementChange(ece);
        }
      });
    }
  }

  private void handleElementChange(ElementChangeEvent ece)
  {
    if (ece.getChangeType().equals(ChangeType.ELEMENT_CREATE))
    {
      Object parent = getParent(ece.getElement());
      if (parent == null)
      {
        treeViewer.refresh(true);
        ece.getElement().addElementChangeListener(this);
      }
      else
      {
        treeViewer.refresh(parent, true);
      }
      treeViewer.expandToLevel(ece.getElement(), 0);
      treeViewer.setSelection(ece.getElement());
    }
    else if (ece.getChangeType().equals(ChangeType.ELEMENT_DELETE))
    {
      Object parent = getParent(ece.getElement());
      if (parent != null)
      {
        if (ece.getElement() instanceof ActivityImpl)
          treeViewer.refresh(); // impl can be associated with multiple packages
        else
          treeViewer.refresh(parent, true);
      }
      else
      {
        // must be a workflowProject
        ece.getElement().removeElementChangeListener(this);
        treeViewer.refresh();
      }
    }
    else if (ece.getChangeType().equals(ChangeType.RENAME))
    {
      treeViewer.refresh(ece.getElement(), true);
      treeViewer.getControl().forceFocus();
    }
    else if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
    {
      treeViewer.refresh(ece.getElement(), true);
      if (ece.getElement() instanceof WorkflowPackage)
        treeViewer.refresh(ece.getElement().getProject().getArchivedUserVisiblePackagesFolder(), true);
    }
    else if (ece.getChangeType().equals(ChangeType.LABEL_CHANGE))
    {
      treeViewer.update(ece.getElement(), null);
    }
    else if (ece.getChangeType().equals(ChangeType.SETTINGS_CHANGE))
    {
      // only applies for workflow projects
      if (ece.getElement() instanceof WorkflowProject)
      {
        WorkflowProject workflowProject = (WorkflowProject) ece.getElement();
        if (ece.getNewValue() instanceof JdbcDataSource)
        {
          treeViewer.collapseToLevel(workflowProject, TreeViewer.ALL_LEVELS);
          treeViewer.refresh(workflowProject, true);
        }
        else if (ece.getNewValue() == null)
        {
          // general refresh
          treeViewer.refresh(workflowProject, true);
        }
      }
      else
      {
        treeViewer.refresh(ece.getElement(), true);
      }
    }
    else if (ece.getChangeType().equals(ChangeType.STATUS_CHANGE))
    {
      treeViewer.update(ece.getElement(), null);
      if (ece.getElement() instanceof AutomatedTestCase)
      {
        AutomatedTestCase testCase = (AutomatedTestCase) ece.getElement();
        Object testResults;
        if (testCase.isLegacy())
          testResults = testCase.getLegacyExpectedResults();
        else
          testResults = testCase.getExpectedResults();
        if (testResults != null)
          treeViewer.refresh(testResults, true);
      }
    }
  }
}
