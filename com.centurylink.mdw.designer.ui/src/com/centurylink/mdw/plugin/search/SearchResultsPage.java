/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.Page;

import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.value.DefaultViewerComparator;
import com.centurylink.mdw.plugin.designer.properties.value.DefaultViewerComparator.SortDirectionProvider;
import com.centurylink.mdw.model.data.work.WorkStatuses;

public class SearchResultsPage extends Page implements ISearchResultPage
{
  private Composite composite;
  private TableEditor tableEditor;
  private SearchResults searchResults;

  private ValueChangeListener valueChangeListener;
  private Map<TableColumn,SelectionListener> columnSortListeners;

  private SearchQuery searchQuery;
  public SearchQuery getSearchQuery() { return searchQuery; }
  public void setSearchQuery(SearchQuery query)
  {
    this.searchQuery = query;
    if (tableEditor != null)
    {
      tableEditor.removeValueChangeListener(valueChangeListener);
      // columns may have changed due to query type
      for (int i = 0; i < tableEditor.getTable().getColumnCount(); i++)
      {
        TableColumn column = tableEditor.getTable().getColumn(i);
        SelectionListener listener = columnSortListeners.get(column);
        if (listener != null)
          column.removeSelectionListener(listener);
      }
      tableEditor.dispose();
    }

    tableEditor = createTableEditor();
    tableEditor.render(composite);
    composite.layout(true);
    addValueChangeListener();
    addColumnSortListeners();
  }

  @Override
  public void createControl(Composite parent)
  {
    composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout());

    tableEditor = createTableEditor();
    tableEditor.render(composite);
    addValueChangeListener();
    addColumnSortListeners();
  }

  /**
   * Really double-click handler
   */
  private void addValueChangeListener()
  {
    valueChangeListener = new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        searchQuery.handleOpen((WorkflowElement)newValue);
      }
    };
    tableEditor.addValueChangeListener(valueChangeListener);
  }

  private void addColumnSortListeners()
  {
    columnSortListeners = new HashMap<TableColumn,SelectionListener>();

    final Table table = tableEditor.getTable();
    for (int i = 0; i < table.getColumnCount(); i++)
    {
      final int idx = i;
      TableColumn column = table.getColumn(i);
      SelectionListener listener = new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          TableColumn sortColumn = table.getSortColumn();
          TableColumn currentColumn = (TableColumn) e.widget;
          int direction = table.getSortDirection();
          if (sortColumn == currentColumn)
          {
            direction = direction == SWT.UP ? SWT.DOWN : SWT.UP;
          }
          else
          {
            table.setSortColumn(currentColumn);
            direction = SWT.UP;
          }
          table.setSortDirection(direction);
          tableEditor.getTableViewer().setComparator(getViewerComparator(tableEditor.getColumnSpecs().get(idx)));
        }
      };
      column.addSelectionListener(listener);
      columnSortListeners.put(column,listener);
    }
  }

  public ViewerComparator getViewerComparator(ColumnSpec colSpec)
  {
    SortDirectionProvider sdp = new SortDirectionProvider()
    {
      public int getSortDirection()
      {
        return tableEditor.getTable().getSortDirection();
      }
    };
    return new DefaultViewerComparator(colSpec, sdp);
  }

  private TableEditor createTableEditor()
  {
    TableEditor editor = new TableEditor(null, TableEditor.TYPE_TABLE);

    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec nameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "name");
    nameColSpec.width = 225;
    columnSpecs.add(nameColSpec);

    ColumnSpec versionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Version", "version");
    versionColSpec.width = 60;
    columnSpecs.add(versionColSpec);

    ColumnSpec idColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "ID", "id");
    idColSpec.width = 80;
    columnSpecs.add(idColSpec);

    ColumnSpec projectColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Project", "workflowProject");
    projectColSpec.width = 175;
    columnSpecs.add(projectColSpec);

    ColumnSpec packageColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Package", "package");
    packageColSpec.width = 175;
    columnSpecs.add(packageColSpec);

    if (searchQuery != null && searchQuery.isInstanceQuery())
    {
      ColumnSpec instanceColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID", "instanceId");
      instanceColSpec.width = 85;
      columnSpecs.add(instanceColSpec);

      ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
      instanceColSpec.width = 75;
      columnSpecs.add(statusColSpec);

      ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start", "instanceStartDate");
      startDateColSpec.width = 100;
      columnSpecs.add(startDateColSpec);

      ColumnSpec endDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "End", "instanceEndDate");
      endDateColSpec.width = 100;
      columnSpecs.add(endDateColSpec);
    }

    if (searchQuery instanceof AssetSearchQuery)
    {
      ColumnSpec lastModColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Last Modified", "modifyDate");
      lastModColSpec.width = 100;
      columnSpecs.add(lastModColSpec);
    }

    editor.setColumnSpecs(columnSpecs);

    editor.setReadOnly(true);

    editor.setContentProvider(new ResultElementContentProvider());
    editor.setLabelProvider(new ResultElementLabelProvider());

    return editor;
  }

  class ResultElementContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<WorkflowElement> rows = (List<WorkflowElement>) inputElement;
      return rows.toArray(new WorkflowElement[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class ResultElementLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      WorkflowElement workflowElement = (WorkflowElement) element;

      if (columnIndex == 0)
        return workflowElement.getIconImage();
      else if (columnIndex == 3)
        return workflowElement.getProject().getIconImage();
      else if (columnIndex == 4)
        return workflowElement.getPackageIconImage();
      else
        return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      WorkflowElement workflowElement = (WorkflowElement) element;

      switch (columnIndex)
      {
        case 0:
          return workflowElement.getName();
        case 1:
          return workflowElement.getVersionLabel();
        case 2:
          return workflowElement.getId().toString();
        case 3:
          return workflowElement.getProject().getName();
        case 4:
          return workflowElement.getPackageLabel();
        case 5:
          if ((workflowElement instanceof WorkflowProcess) && workflowElement.hasInstanceInfo())
            return ((WorkflowProcess)workflowElement).getProcessInstance().getId().toString();
          else if (workflowElement instanceof WorkflowAsset)
            return ((WorkflowAsset)workflowElement).getFormattedModifyDate();
          else
            return null;
        case 6:
          if ((workflowElement instanceof WorkflowProcess) && workflowElement.hasInstanceInfo())
            return WorkStatuses.getWorkStatuses().get(((WorkflowProcess)workflowElement).getProcessInstance().getStatusCode());
          else
            return null;
        case 7:
          if ((workflowElement instanceof WorkflowProcess) && workflowElement.hasInstanceInfo())
            return ((WorkflowProcess)workflowElement).getProcessInstance().getStartDate();
          else
            return null;
        case 8:
          if ((workflowElement instanceof WorkflowProcess) && workflowElement.hasInstanceInfo())
            return ((WorkflowProcess)workflowElement).getProcessInstance().getEndDate();
          else
            return null;
        default:
          return null;
      }
    }
  }

  @Override
  public Control getControl()
  {
    return composite;
  }

  public String getLabel()
  {
    if (searchResults == null)
      return "Process Search Results";
    else
      return "Process Search Results: " + searchResults.getLabel();
  }

  public void setInput(ISearchResult result, Object uiState)
  {
    if (result == null)
      return;

    searchResults = (SearchResults) result;
    List<WorkflowElement> elements = new ArrayList<WorkflowElement>();
    for (WorkflowElement workflowElement : searchResults.getMatchingElements())
      elements.add(workflowElement);

    tableEditor.setValue(elements);

    tableEditor.getTable().setSortColumn(tableEditor.getTable().getColumn(0));
    tableEditor.getTable().setSortDirection(SWT.UP);
    tableEditor.getTableViewer().setComparator(getViewerComparator(tableEditor.getColumnSpecs().get(0)));
  }

  public void setViewPart(ISearchResultViewPart part)
  {
  }

  public Object getUIState()
  {
    return null;
  }

  public void restoreState(IMemento memento)
  {
  }

  public void saveState(IMemento memento)
  {
  }

  public void setID(String id)
  {
  }

  public void setFocus()
  {
  }

  public String getID()
  {
    return "mdw.search.resultsPage";
  }

}
