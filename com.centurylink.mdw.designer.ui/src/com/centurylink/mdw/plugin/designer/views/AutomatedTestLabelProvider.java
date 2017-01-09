/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class AutomatedTestLabelProvider extends LabelProvider
{
  private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  public Image getImage(Object element)
  {
    WorkflowElement workflowElement = (WorkflowElement) element;
    return workflowElement.getIconImage();
  }

  public String getText(Object element)
  {
    if (element instanceof AutomatedTestSuite)
    {
      AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
      return testSuite.getProject().getName();
    }
    else if (element instanceof WorkflowPackage)
    {
      return ((WorkflowPackage)element).getName();
    }
    else if (element instanceof Folder)
    {
      return ((Folder)element).getName();
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      return testCase.getLabel();
    }
    else if (element instanceof AutomatedTestResults)
    {
      AutomatedTestResults expectedResults = (AutomatedTestResults) element;
      return expectedResults.getLabel();
    }
    else if (element instanceof LegacyExpectedResults)
    {
      LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
      return expectedResult.getName();
    }

    return null;
  }

  public void dispose()
  {
    for (Image image : imageCache.values())
    {
      image.dispose();
    }
    imageCache.clear();
  }

}
