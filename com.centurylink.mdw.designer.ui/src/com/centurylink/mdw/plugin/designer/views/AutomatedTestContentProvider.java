/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class AutomatedTestContentProvider implements ITreeContentProvider, ElementChangeListener
{
  private static Object[] EMPTY_ARRAY = new Object[0];

  private TreeViewer treeViewer;

  @SuppressWarnings("unchecked")
  public Object[] getElements(Object inputElement)
  {
    List<AutomatedTestSuite> testSuites = (List<AutomatedTestSuite>) inputElement;
    return testSuites.toArray(new AutomatedTestSuite[0]);
  }

  public Object[] getChildren(Object parentElement)
  {
    if (parentElement instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) parentElement;
      List<WorkflowPackage> packages = new ArrayList<WorkflowPackage>();
      List<WorkflowElement> legacyTests = new ArrayList<WorkflowElement>();
      for (AutomatedTestCase testCase : testSuite.getTestCases())
      {
        if (testCase.isLegacy())
        {
          legacyTests.add(testCase);
        }
        else
        {
          WorkflowPackage pkg = testCase.getPackage();
          if (!packages.contains(pkg))
            packages.add(pkg);
        }
      }
      Folder folder = null;
      if (!legacyTests.isEmpty())
      {
        folder = new Folder("Legacy Tests");
        folder.setChildren(legacyTests);
        for (WorkflowElement legacyTest : legacyTests)
          legacyTest.setArchivedFolder(folder);
      }
      Object[] children = new Object[packages.size() + (folder == null ? 0 : 1)];
      for (int i = 0; i < packages.size(); i++)
        children[i] = packages.get(i);
      if (folder != null)
        children[children.length - 1] = folder;
      return children;
    }
    else if (parentElement instanceof WorkflowPackage)
    {
      WorkflowPackage pkg = (WorkflowPackage) parentElement;
      @SuppressWarnings("unchecked")
      AutomatedTestSuite suite = ((List<AutomatedTestSuite>)treeViewer.getInput()).get(0);
      List<AutomatedTestCase> selectedCases = new ArrayList<AutomatedTestCase>();
      for (AutomatedTestCase pkgCase : pkg.getTestCases())
      {
        if (suite.getTestCases().contains(pkgCase))
          selectedCases.add(pkgCase);
      }
      return selectedCases.toArray();
    }
    else if (parentElement instanceof Folder)
    {
      return ((Folder)parentElement).getChildren().toArray();
    }
    else if (parentElement instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) parentElement;
      if (testCase.isLegacy())
      {
        return testCase.getLegacyExpectedResults().toArray(new LegacyExpectedResults[0]);
      }
      else
      {
        AutomatedTestResults results = testCase.getExpectedResults();
        return results == null ? EMPTY_ARRAY : new Object[]{results};
      }
    }
    else
    {
      return EMPTY_ARRAY;
    }
  }

  @SuppressWarnings("unchecked")
  public Object getParent(Object element)
  {
    if (element instanceof AutomatedTestResults)
    {
      AutomatedTestResults expectedResults = (AutomatedTestResults) element;
      return expectedResults.getTestCase();
    }
    if (element instanceof LegacyExpectedResults)
    {
      LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
      return expectedResult.getTestCase();
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      if (testCase.isLegacy())
        return testCase.getArchivedFolder();
      return testCase.getTestSuite();
    }
    else if (element instanceof Folder)
    {
      Folder legacyFolder = (Folder) element;
      return ((AutomatedTestCase)legacyFolder.getChildren().get(0)).getTestSuite();
    }
    else if (element instanceof WorkflowPackage)
    {
      return ((List<AutomatedTestSuite>)treeViewer.getInput()).get(0);
    }
    return null;
  }

  public boolean hasChildren(Object element)
  {
    if (element instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
      return testSuite.getTestCases().size() > 0;
    }
    else if (element instanceof WorkflowPackage)
    {
      return true; // not added unless cases
    }
    else if (element instanceof Folder)
    {
      return true; // not added unless cases
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      if (testCase.isLegacy())
      {
        return testCase.getLegacyExpectedResults().size() > 0;
      }
      else
      {
        return testCase.getExpectedResults() != null;
      }
    }
    return false;
  }

  public void dispose()
  {
  }

  /**
   * Registers this content provider as a listener to changes on the new input
   * (for workflow element changes), and deregisters the viewer from the old input.
   * In response to these ElementChangeEvents, we update the viewer.
   */
  @SuppressWarnings("unchecked")
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
    this.treeViewer = (TreeViewer) viewer;

    if (oldInput != null)
    {
      List<AutomatedTestSuite> oldSuites = (List<AutomatedTestSuite>) oldInput;
      for (AutomatedTestSuite oldSuite : oldSuites)
      {
        oldSuite.removeElementChangeListener(this);
        oldSuite.getProject().addElementChangeListener(this);
      }
    }
    if (newInput != null)
    {
      List<AutomatedTestSuite> newSuites = (List<AutomatedTestSuite>) newInput;
      for (AutomatedTestSuite newSuite : newSuites)
      {
        newSuite.addElementChangeListener(this);
        newSuite.getProject().addElementChangeListener(this);
      }
    }
  }

  public void elementChanged(final ElementChangeEvent ece)
  {
    // updates to the treeViewer must be on the UI thread
    if (!treeViewer.getTree().isDisposed())
    {
      treeViewer.getTree().getDisplay().asyncExec(new Runnable()
      {
        public void run()
        {
          handleElementChange(ece);
        }
      });
    }
  }

  /**
   * Handles STATUS_CHANGE events.
   */
  private void handleElementChange(ElementChangeEvent ece)
  {
    if (ece.getChangeType().equals(ChangeType.STATUS_CHANGE))
    {
      treeViewer.update(ece.getElement(), null);
      if (ece.getElement() instanceof AutomatedTestCase)
      {
        AutomatedTestCase testCase = (AutomatedTestCase) ece.getElement();
        if (testCase.isLegacy())
        {
          List<LegacyExpectedResults> legacyResults = testCase.getLegacyExpectedResults();
          if (legacyResults != null)
          {
            for (LegacyExpectedResults legacyResult : legacyResults)
              treeViewer.refresh(legacyResult, true);
          }
        }
        else
        {
          AutomatedTestResults testResults = testCase.getExpectedResults();
          if (testResults != null)
            treeViewer.refresh(testResults, true);
        }
      }
    }
  }
}
