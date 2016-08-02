/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ListViewContentProvider implements IStructuredContentProvider
{
  public ListViewContentProvider()
  {
  }
  
  public Object[] getElements(Object inputElement)
  {
    if (inputElement instanceof Object[])
      return (Object[]) inputElement;
    else if (inputElement instanceof List)
      return ((List<?>)inputElement).toArray();
    else
      return null;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
  }

  public void dispose()
  {
  }
  
  public long getCount()
  {
    // TODO
    return 0;
  }
  
  public int getPageIndex()
  {
    // TODO
    return 0;
  }
}
