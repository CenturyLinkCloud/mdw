/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessSearchQuery extends SearchQuery
{
  private Long containedEntityId;
  public Long getContainedEntityId() { return containedEntityId; }
  public void setContainedEntityId(Long id) { this.containedEntityId = id; }

  public String containedEntityName;
  public String getContainedEntityName() { return containedEntityName; }
  public void setContainedEntityName(String name) { this.containedEntityName = name; }

  private Long invokedEntityId;
  public Long getInvokedEntityId() { return invokedEntityId; }
  public void setInvokedEntityId(Long id) { this.invokedEntityId = id; }

  public ProcessSearchQuery(List<WorkflowProject> scopedProjects, SearchType searchType, String searchPattern, boolean caseSensitive, Shell shell)
  {
    super(scopedProjects, searchType, searchPattern, caseSensitive, shell);
  }

  public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
  {
    if (getScopedProjects().isEmpty() && getSelectedPackage() != null)
      getScopedProjects().add(getSelectedPackage().getProject());

    if (getScopedProjects().isEmpty())
    {
      String msg = "No workflow projects in search scope.";
      showError(msg, "MDW Search", null);
      return new Status(IStatus.WARNING, MdwPlugin.getPluginId(), 0, msg, null);
    }

    for (WorkflowProject project : getScopedProjects())
    {
      if (getSearchType().equals(SearchType.ENTITY_BY_NAME))
      {
        for (WorkflowProcess process : project.getAllProcessVersions())
        {
          String name = isCaseSensitive() ? process.getName() : process.getName().toLowerCase();
          if ((getPattern().equals("*") || name.indexOf(getPattern()) >= 0) && (getSelectedPackage() == null || (process.getPackage() != null && process.getPackage().equals(getSelectedPackage()))))
            getSearchResults().addMatchingElement(process);
        }
      }
      else if (getSearchType().equals(SearchType.ENTITY_BY_ID))
      {
        WorkflowProcess process = project.getProcess(new Long(getPattern()));
        if (process == null && project.isRemote() && project.isFilePersist())
        {
          // could be archived process for remote VCS
          try
          {
            ProcessVO procVO = project.getDesignerProxy().getDesignerDataAccess().getProcessDefinition(new Long(getPattern()));
            process = new WorkflowProcess(project, procVO);
          }
          catch (Exception ex)
          {
            PluginMessages.log(ex);
          }
        }
        if (process != null && (getSelectedPackage() == null || (process.getPackage() != null && process.getPackage().equals(getSelectedPackage()))))
          getSearchResults().addMatchingElement(process);
      }
      else if (getSearchType().equals(SearchType.CONTAINING_ENTITY))
      {
        try
        {
          for (WorkflowProcess process : project.getDesignerProxy().getProcessesUsingActivityImpl(containedEntityId, containedEntityName))
          {
            String name = isCaseSensitive() ? process.getName() : process.getName().toLowerCase();
            if (getPattern().equals("*") || name.indexOf(getPattern()) >= 0)
              getSearchResults().addMatchingElement(process);
          }
        }
        catch (DataAccessException ex)
        {
          showError(ex, "Find Processes", project);
        }
      }
      else if (getSearchType().equals(SearchType.INVOKING_ENTITY))
      {
        try
        {
          WorkflowProcess invoked = project.getProcess(getInvokedEntityId());
          for (WorkflowProcess process : project.getDesignerProxy().findCallingProcesses(invoked))
          {
            String name = isCaseSensitive() ? process.getName() : process.getName().toLowerCase();
            if (getPattern().equals("*") || name.indexOf(getPattern()) >= 0)
              getSearchResults().addMatchingElement(process);
          }
        }
        catch (Exception ex)
        {
          showError(ex, "Calling Processes", project);
        }
      }
      else if (getSearchType().equals(SearchType.INSTANCE_BY_ENTITY_ID))
      {
        Map<String,String> criteria = new HashMap<String,String>();
        criteria.put("processId", getPattern());
        searchInstances(project, criteria);
      }
      else if (getSearchType().equals(SearchType.INSTANCE_BY_ID))
      {
        Map<String,String> criteria = new HashMap<String,String>();
        criteria.put("id", getPattern());
        searchInstances(project, criteria);
      }
      else if (getSearchType().equals(SearchType.INSTANCE_BY_MRI))
      {
        Map<String,String> criteria = new HashMap<String,String>();
        if (isCaseSensitive())
          criteria.put("masterRequestId", getPattern());
        else
          criteria.put("masterRequestIdIgnoreCase", getPattern());
        searchInstances(project, criteria);
      }
      else
      {
        showError("Unsupported search type: " + getSearchType(), "MDW Search", null);
      }
    }

    if (getSearchResults().getMatchingElements().size() == 0)
      return new Status(IStatus.WARNING, MdwPlugin.getPluginId(), 0, "No matching elements found", null);
    else
      return Status.OK_STATUS;
  }

  private void searchInstances(final WorkflowProject project, Map<String,String> criteria)
  {
    try
    {
      List<ProcessInstanceVO> instances = project.getDesignerProxy().getProcessInstances(criteria);
      for (ProcessInstanceVO instanceInfo : instances)
      {
        Long processId = instanceInfo.getProcessId();
        if (project.getProcess(processId) == null && instanceInfo.getOwner().equals(OwnerType.PROCESS_INSTANCE))
        {
          Map<String,String> newCrit = new HashMap<String,String>();
          newCrit.put("id", instanceInfo.getOwnerId().toString());
          instanceInfo = project.getDesignerProxy().getProcessInstances(newCrit).get(0);
          processId = instanceInfo.getProcessId();
        }
        if (processId != null)
        {
          WorkflowProcess processVersion = project.getProcess(processId);
          if (processVersion == null && project.isFilePersist()) // can happen for non-vcs processes or Archived processes
          {
            ProcessVO processVO = new ProcessVO();
            processVO.setProcessId(instanceInfo.getProcessId());
            processVersion = new WorkflowProcess(project, processVO);
            processVersion.setName(instanceInfo.getProcessName());
            processVersion.setVersion(RuleSetVO.parseVersion(instanceInfo.getProcessVersion()));
            processVO = project.getDataAccess().loadProcess(processVersion);
            if (processVO != null) // otherwise retrieval will be just-in-time
              processVersion.setProcessVO(processVO);
          }
          if (processVersion != null)
          {
            WorkflowProcess instance = new WorkflowProcess(processVersion);
            instance.setProcessInstance(instanceInfo);
            if (!getSearchResults().getMatchingElements().contains(instance))
              getSearchResults().addMatchingElement(instance);
          }
        }
      }
    }
    catch(Exception ex)
    {
      showError(ex, "Retrieve Process Instances", project);
    }
  }

  @Override
  public String getIcon()
  {
    return "proc_search.gif";
  }

  @Override
  public void handleOpen(WorkflowElement workflowElement)
  {
    WorkflowProcess processVersion = (WorkflowProcess) workflowElement;
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try
    {
      ProcessEditor processEditor = (ProcessEditor) page.openEditor(processVersion, "mdw.editors.process");
      processEditor.setFocus();
    }
    catch (PartInitException ex)
    {
      showError(ex, "Open Process", workflowElement.getProject());
    }
  }
}
