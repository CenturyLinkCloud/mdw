/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.search;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.WorkbenchException;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AssetSearchQuery extends SearchQuery
{
  private String resourceType;
  public String getResourceType() { return resourceType; }
  public void setResourceType(String type) { this.resourceType = type; }

  private String containedText;
  public String getContainedText() { return containedText; }
  public void setContainedText(String text) { this.containedText = text; }

  public AssetSearchQuery(List<WorkflowProject> scopedProjects, SearchType searchType, String searchPattern, boolean caseSensitive, Shell shell)
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
      if (getSearchType().equals(SearchType.ENTITY_BY_NAME) || (getSearchType().equals(SearchType.ENTITY_BY_ID) && getPattern().equals("*")))
      {
        if (getResourceType().equalsIgnoreCase("External Event Handler"))
        {
          for (ExternalEvent eventHandler : project.getAllExternalEvents())
          {
            String name = isCaseSensitive() ? eventHandler.getName() : eventHandler.getName().toLowerCase();
            if ((getPattern().equals("*") || name.indexOf(getPattern()) >= 0) && (getSelectedPackage() == null || (eventHandler.getPackage() != null && eventHandler.getPackage().equals(getSelectedPackage()))))
              getSearchResults().addMatchingElement(eventHandler);
          }
        }
        else
        {
          for (WorkflowAsset asset : project.getAllWorkflowAssets())
          {
            if (asset.getLanguage().equalsIgnoreCase(getResourceType()))
            {
              String name = isCaseSensitive() ? asset.getName() : asset.getName().toLowerCase();
              if ((getPattern().equals("*") || name.indexOf(getPattern()) >= 0) && (getSelectedPackage() == null || (asset.getPackage() != null && asset.getPackage().equals(getSelectedPackage()))))
                getSearchResults().addMatchingElement(asset);
            }
          }
        }
      }
      else if (getSearchType().equals(SearchType.CONTAINING_TEXT))
      {
        if (getResourceType().equalsIgnoreCase("External Event Handler"))
        {
          for (ExternalEvent eventHandler : project.getAllExternalEvents())
          {
            String name = isCaseSensitive() ? eventHandler.getName() : eventHandler.getName().toLowerCase();
            if ((getPattern().equals("*") || name.indexOf(getPattern()) >= 0) && (getSelectedPackage() == null || (eventHandler.getPackage() != null && eventHandler.getPackage().equals(getSelectedPackage()))))
            {
              // resource name pattern is matched, check for contained text
              String searchText = isCaseSensitive() ? getContainedText() : getContainedText().toLowerCase();
              searchText = searchText.replaceAll("\\*", "");
              if (eventHandler.getMessagePattern().indexOf(searchText) >= 0)
                getSearchResults().addMatchingElement(eventHandler);
            }
          }
        }
        else
        {
          for (WorkflowAsset asset : project.getAllWorkflowAssets())
          {
            if (asset.getLanguage().equalsIgnoreCase(getResourceType()))
            {
              String name = isCaseSensitive() ? asset.getName() : asset.getName().toLowerCase();
              if ((getPattern().equals("*") || name.indexOf(getPattern()) >= 0) && (getSelectedPackage() == null || (asset.getPackage() != null && asset.getPackage().equals(getSelectedPackage()))))
              {
                // resource name pattern is matched, check for contained text
                asset = asset.getProject().getDesignerProxy().loadWorkflowAsset(asset);
                if (!asset.isBinary())
                {
                  String content = isCaseSensitive() ? asset.getContent() : asset.getContent().toLowerCase();
                  String searchText = isCaseSensitive() ? getContainedText() : getContainedText().toLowerCase();
                  searchText = searchText.replaceAll("\\*", "");
                  if (content.indexOf(searchText) >= 0)
                    getSearchResults().addMatchingElement(asset);
                }
              }
            }
          }
        }
      }
      else if (getSearchType().equals(SearchType.ENTITY_BY_ID))
      {
        WorkflowAsset asset = project.getAsset(new Long(getPattern()));
        if (asset != null && asset.getLanguage().equalsIgnoreCase(getResourceType()))
          getSearchResults().addMatchingElement(asset);
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

  @Override
  public String getIcon()
  {
    return "resource_search.gif";
  }

  @Override
  public void handleOpen(WorkflowElement workflowElement)
  {
    if (workflowElement instanceof WorkflowAsset)
    {
      openWorkflowAsset((WorkflowAsset)workflowElement);
    }
    else if (workflowElement instanceof ExternalEvent)
    {
      openExternalEvent((ExternalEvent)workflowElement);
    }
  }

  private void openWorkflowAsset(WorkflowAsset asset)
  {
    asset.openFile(new NullProgressMonitor());
  }

  private void openExternalEvent(ExternalEvent eventHandler)
  {
    try
    {
      IWorkbenchPage page = MdwPlugin.getActivePage();
      ProcessExplorerView processExplorer = (ProcessExplorerView) page.findView(ProcessExplorerView.VIEW_ID);
      if (processExplorer != null)
      {
        processExplorer.setFocus();
        processExplorer.select(eventHandler);
        page.showView("org.eclipse.ui.views.PropertySheet");
      }
    }
    catch (WorkbenchException ex)
    {
      PluginMessages.uiError(MdwPlugin.getShell(), ex, "Event Handler", eventHandler.getProject());
    }
  }
}
