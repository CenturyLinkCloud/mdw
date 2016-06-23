/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ProcessHierarchyContentProvider implements ITreeContentProvider, ElementChangeListener
{
  private WorkflowProcess process;
  private LinkedProcess startProcess;
  private List<LinkedProcess> topLevelCallers;

  public Object[] getElements(Object inputElement)
  {
    if (inputElement == null)
      return new Object[0];
    if (!(inputElement instanceof WorkflowProcess))
      throw new IllegalArgumentException("Invalid object not instance of WorkflowProcess");
    if (topLevelCallers == null || !inputElement.equals(process))
    {
      process = (WorkflowProcess) inputElement;
      if (process.hasInstanceInfo())
        return new Object[0]; // not relevant (instance hierarchy)

      topLevelCallers = new ArrayList<LinkedProcess>();
      try
      {
        addTopLevelCallers(process);
        for (LinkedProcess topLevelCaller : topLevelCallers)
          addCalledHierarchy(topLevelCaller);
        // find the start process in the hierarchy
        for (LinkedProcess caller : topLevelCallers)
        {
          if (caller.getProcess().equals(process))
          {
            startProcess = caller;
            break;
          }
          else
          {
            LinkedProcess found = findStartProcess(caller);
            if (found != null)
            {
              startProcess = found;
              break;
            }
          }
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Process Hierarchy", process.getProject());
      }
    }

    return topLevelCallers.toArray(new LinkedProcess[0]);
  }

  private void addTopLevelCallers(WorkflowProcess called) throws DataAccessException, RemoteException
  {
    List<WorkflowProcess> immediateCallers = called.getProject().getDesignerProxy().findCallingProcesses(called);
    if (immediateCallers.isEmpty())
    {
      topLevelCallers.add(new LinkedProcess(called));
    }
    else
    {
      for (WorkflowProcess caller : immediateCallers)
        addTopLevelCallers(caller);
    }
  }

  private void addCalledHierarchy(LinkedProcess caller) throws DataAccessException, RemoteException
  {
    WorkflowProcess callerProcess = caller.getProcess();
    for (WorkflowProcess calledProcess : callerProcess.getProject().getDesignerProxy().findCalledProcesses(callerProcess).toArray(new WorkflowProcess[0]))
    {
      LinkedProcess child = new LinkedProcess(calledProcess);
      child.setParent(caller);
      caller.getChildren().add(child);
      addCalledHierarchy(child);
    }
  }

  private LinkedProcess findStartProcess(LinkedProcess caller)
  {
    for (LinkedProcess called : caller.getChildren())
    {
      if (called.getProcess().equals(process))
      {
        return called;
      }
      else
      {
        LinkedProcess found = findStartProcess(called);
        if (found != null)
          return found;
      }
    }
    return null;
  }

  public Object[] getChildren(Object parentElement)
  {
    if (!(parentElement instanceof LinkedProcess))
      return null;

    return ((LinkedProcess)parentElement).getChildren().toArray(new LinkedProcess[0]);
  }

  public boolean hasChildren(Object element)
  {
    if (!(element instanceof LinkedProcess))
      return false;

    return ((LinkedProcess)element).getChildren().size() > 0;
  }

  public Object getParent(Object element)
  {
    if (!(element instanceof LinkedProcess))
      return null;

    return ((LinkedProcess)element).getParent();
  }

  public ISelection getInitialSelection()
  {
    return new StructuredSelection(startProcess);
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
    // TODO Auto-generated method stub

  }

  public void elementChanged(ElementChangeEvent ece)
  {
    // TODO Auto-generated method stub

  }

  public void dispose()
  {
  }

  public class LinkedProcess
  {
    private WorkflowProcess process;
    public WorkflowProcess getProcess() { return process; }

    private LinkedProcess parent;
    public LinkedProcess getParent() { return parent; }
    public void setParent(LinkedProcess parent) { this.parent = parent; }

    private List<LinkedProcess> children = new ArrayList<LinkedProcess>();
    public List<LinkedProcess> getChildren() { return children; }
    public void setChildren(List<LinkedProcess> children) { this.children = children; }

    LinkedProcess(WorkflowProcess process)
    {
      this.process = process;
    }
  }
}
