/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.value.DefaultViewerComparator;
import com.centurylink.mdw.plugin.designer.properties.value.DefaultViewerComparator.SortDirectionProvider;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

/**
 * Interfaces for dynamically-configured lists (ala MDWTaskView.xml).
 * Similar to SortableList for webapps.
 *
 */
public class ListView extends ViewPart
{
  private String viewId;
  public String getViewId() { return viewId; }

  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  public void setProject(WorkflowProject project) { this.project = project; }

  private List<ColumnSpec> columnSpecs;
  private String[] columnProps;

  private Table table;
  private TableViewer tableViewer;
  public TableViewer getTableViewer() { return tableViewer; }

  private ListViewContentProvider contentProvider;
  private ListViewLabelProvider labelProvider;
  private ListViewActionGroup actionGroup;

  public ListView(String viewId)
  {
    this.viewId = viewId;
  }

  // must override
  public Object[] getElements()
  {
    return null;
  }

  @Override
  public void createPartControl(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout
    GridLayout gl = new GridLayout();
    gl.numColumns = 10;
    composite.setLayout(gl);

    columnSpecs = createColumnSpecs();
    columnProps = getColumnProps(columnSpecs);

    createTable(composite);
    tableViewer = createTableViewer(table);
    contentProvider = new ListViewContentProvider();
    tableViewer.setContentProvider(contentProvider);
    labelProvider = new ListViewLabelProvider(columnSpecs);
    tableViewer.setLabelProvider(labelProvider);

    actionGroup = new ListViewActionGroup(this);
    IActionBars actionBars = getViewSite().getActionBars();
    actionGroup.fillActionBars(actionBars);

    table.setSortColumn(table.getColumn(0));
    table.setSortDirection(SWT.UP);
    tableViewer.setComparator(getViewerComparator(columnSpecs.get(0)));

    // PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, MdwPlugin.getPluginId() + ".task_list_help");
  }

  @Override
  public void setFocus()
  {
    // TODO Auto-generated method stub

  }

  protected List<ColumnSpec> createColumnSpecs()
  {
    // TODO create colspecs from TaskView.xml
    // must be overridden for now
    return null;
  }

  protected String[] getColumnProps(List<ColumnSpec> columnSpecs)
  {
    String[] columnProps = new String[columnSpecs.size()];
    for (int i = 0; i < columnSpecs.size(); i++)
    {
      columnProps[i] = columnSpecs.get(i).property;
    }
    return columnProps;
  }

  private void createTable(Composite parent)
  {
    int style = SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;

    table = new Table(parent, style);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 10;
    table.setLayoutData(gridData);
    table.setLinesVisible(true);
    table.setHeaderVisible(true);

    for (int i = 0; i < columnSpecs.size(); i++)
    {
      ColumnSpec colSpec = columnSpecs.get(i);
      int styles = SWT.LEFT;
      if (colSpec.readOnly)
        style = style | SWT.READ_ONLY;
      TableColumn column = new TableColumn(table, styles, i);
      column.setText(colSpec.label);
      column.setWidth(colSpec.width);
      column.setResizable(colSpec.resizable);
      column.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          TableColumn sortColumn = tableViewer.getTable().getSortColumn();
          TableColumn currentColumn = (TableColumn) e.widget;
          int direction = tableViewer.getTable().getSortDirection();
          if (sortColumn == currentColumn)
          {
            direction = direction == SWT.UP ? SWT.DOWN : SWT.UP;
          }
          else
          {
            tableViewer.getTable().setSortColumn(currentColumn);
            direction = SWT.UP;
          }
          tableViewer.getTable().setSortDirection(direction);

          // TODO handle column sort
//          Sort sort = contentProvider.getSort();
//          sort.setSort(currentColumn.getText());
//          sort.setAscending(direction == SWT.DOWN);
          refreshTable();
        }
      });
    }

    // double-click
    table.addSelectionListener(new SelectionAdapter()
      {
        public void widgetDefaultSelected(SelectionEvent e)
        {
          handleOpen(e.item.getData());
        }
      });

    // right-click menu
    table.addListener(SWT.MenuDetect, new Listener()
    {
      public void handleEvent(Event event)
      {
        table.setMenu(createContextMenu(table.getShell()));
      }
    });

    // auto-adjust column width
    table.addControlListener(new ControlAdapter()
    {
      @Override
      public void controlResized(ControlEvent e)
      {
        int tableWidth = table.getBounds().width;
        int cumulative = 0;
        TableColumn[] tableColumns = table.getColumns();
        for (int i = 0; i < tableColumns.length; i++)
        {
          if (i == tableColumns.length - 1)
            tableColumns[i].setWidth(tableWidth - cumulative - 25);
          cumulative += tableColumns[i].getWidth();
        }
      }
    });

  }

  private TableViewer createTableViewer(Table table)
  {
    TableViewer tableViewer = new TableViewer(table);
    tableViewer.setUseHashlookup(true);
    tableViewer.setColumnProperties(columnProps);

    return tableViewer;
  }

  public ViewerComparator getViewerComparator(ColumnSpec colSpec)
  {
    SortDirectionProvider sdp = new SortDirectionProvider()
    {
      public int getSortDirection()
      {
        return table.getSortDirection();
      }
    };
    return new DefaultViewerComparator(colSpec, sdp);
  }

  protected void handleOpen(Object element)
  {
    // TODO
  }

  private Menu createContextMenu(Shell shell)
  {
    Menu menu = new Menu(shell, SWT.POP_UP);

    final StructuredSelection selection = (StructuredSelection) getTableViewer().getSelection();
    if (selection.size() == 1  && selection.getFirstElement() instanceof ProcessInstanceVO)
    {
      final Object element = selection.getFirstElement();

      MenuItem openItem = new MenuItem(menu, SWT.PUSH);
      openItem.setText("Open");
      //ImageDescriptor openImageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
      //openItem.setImage(openImageDesc.createImage());
      openItem.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          handleOpen(element);
        }
      });
    }

    return menu;
  }

  public void refreshTable()
  {
    BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        tableViewer.setInput(getElements());
        long count = contentProvider.getCount();
        int pageIdx = contentProvider.getPageIndex();
        int pageSize = 0; // TODO contentProvider.getFilter().getPageSize().intValue();

        String info = null;
        if (count == 0)
        {
          info = "No matching results found";
        }
        else
        {
          int base = (pageIdx - 1) * pageSize;

          int first = base + 1;
          long last = count > (base + pageSize) ? first + pageSize - 1 : base + count % pageSize;

          info = "Showing: " + first + " to " + last + " of " + count;
        }
        System.out.println("info: " + info);

//        countLabel.setText(info);
//        countLabel.pack();
      }
    });
  }

}
