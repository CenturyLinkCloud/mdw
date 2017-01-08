/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

public class WorkflowSelectionProvider implements ISelectionProvider
{
  private ListenerList listeners = new ListenerList();
  private WorkflowElement selection;

  public WorkflowSelectionProvider(WorkflowElement workflowElement)
  {
    selection = workflowElement;
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener)
  {
    listeners.add(listener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener)
  {
    listeners.remove(listener);
  }

  public ISelection getSelection()
  {
    return selection;
  }

  public void setSelection(ISelection selection)
  {
    this.selection = (WorkflowElement) selection;
    fireSelectionChanged(new SelectionChangedEvent(this, selection));
  }

  public void fireSelectionChanged(final SelectionChangedEvent event)
  {
    for (int i = 0; i < listeners.getListeners().length; ++i)
    {
      final ISelectionChangedListener l = (ISelectionChangedListener) listeners.getListeners()[i];
      l.selectionChanged(event);
    }
  }

  public void clearSelection()
  {
    selection = new WorkflowElement()
    {
      public String getTitle()
      {
        return "";
      }
      public Long getId()
      {
        return null;
      }
      public String getName()
      {
        return null;
      }
      public String getIcon()
      {
        return "element.gif";
      }
      public WorkflowProject getProject()
      {
        return null;
      }
      public boolean hasInstanceInfo()
      {
        return false;
      }
      public boolean isReadOnly()
      {
        return false;
      }
      public Entity getActionEntity()
      {
        return Entity.Element;
      }
    };
  }
}
