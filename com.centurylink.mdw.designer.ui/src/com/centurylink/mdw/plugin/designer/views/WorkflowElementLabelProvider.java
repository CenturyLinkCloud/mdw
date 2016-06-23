/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class WorkflowElementLabelProvider extends LabelProvider
{
  public Image getImage(Object element)
  {
    if (!(element instanceof WorkflowElement))
      throw new IllegalArgumentException("Invalid object not instance of WorkflowElement");

    return ((WorkflowElement)element).getIconImage();
  }

  public String getText(Object element)
  {
    if (!(element instanceof WorkflowElement))
      throw new IllegalArgumentException("Invalid object not instance of WorkflowElement");

    return ((WorkflowElement)element).getLabel();
  }

  public void dispose()
  {
  }
}