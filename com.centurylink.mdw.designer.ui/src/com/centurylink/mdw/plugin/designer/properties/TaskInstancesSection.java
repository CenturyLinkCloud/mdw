/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.data.task.TaskStatuses;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;

public class TaskInstancesSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }
  protected void setActivity(Activity activity)
  {
    this.activity = activity;
    tableEditor.setElement(activity);
    tableEditor.setValue(activity.getTaskInstances());
  }

  private TableEditor tableEditor;
  private List<ColumnSpec> columnSpecs;
  protected List<ColumnSpec> getColumnSpecs()
  {
    if (columnSpecs == null)
      columnSpecs = createColumnSpecs();
    return columnSpecs;
  }

  private IStructuredContentProvider contentProvider;
  private ITableLabelProvider labelProvider;

  public void setSelection(WorkflowElement selection)
  {
    setActivity((Activity)selection);
    setTaskInstances(activity.getTaskInstances());
  }

  protected void setTaskInstances(List<TaskInstanceVO> instances)
  {
    tableEditor.setValue(instances);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    tableEditor = new TableEditor(activity, TableEditor.TYPE_TABLE);
    tableEditor.setReadOnly(true);

    tableEditor.setColumnSpecs(getColumnSpecs());

    if (contentProvider == null)
      contentProvider = getContentProvider();
    tableEditor.setContentProvider(contentProvider);

    if (labelProvider == null)
      labelProvider = getLabelProvider();
    tableEditor.setLabelProvider(labelProvider);

    tableEditor.render(composite);

    // double-click
    tableEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        openTaskInstance((TaskInstanceVO)newValue);
      }
    });

    // right-click menu
    tableEditor.getTable().addListener(SWT.MenuDetect, new Listener()
    {
      public void handleEvent(Event event)
      {
        tableEditor.getTable().setMenu(createContextMenu(getShell()));
      }
    });
  }

  private Menu createContextMenu(Shell shell)
  {
    Menu menu = new Menu(shell, SWT.POP_UP);

    StructuredSelection selection = (StructuredSelection) tableEditor.getTableViewer().getSelection();
    if (selection.size() == 1 && selection.getFirstElement() instanceof TaskInstanceVO)
    {
      final TaskInstanceVO taskInstanceVO = (TaskInstanceVO) selection.getFirstElement();

      // view
      MenuItem taskMgrItem = new MenuItem(menu, SWT.PUSH);
      taskMgrItem.setText("View in Task Manager");
      ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/taskmgr.gif");
      taskMgrItem.setImage(imageDesc.createImage());
      taskMgrItem.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          openTaskInstance(taskInstanceVO);
        }
      });
    }

    return menu;
  }

  protected List<ColumnSpec> createColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec instanceIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Task Instance ID", "instanceId");
    instanceIdColSpec.width = 100;
    columnSpecs.add(instanceIdColSpec);

    ColumnSpec taskNameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Name", "name");
    taskNameColSpec.width = 150;
    columnSpecs.add(taskNameColSpec);

    ColumnSpec taskIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Task Def. ID", "taskId");
    taskIdColSpec.width = 100;
    columnSpecs.add(taskIdColSpec);

    ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
    statusColSpec.width = 100;
    columnSpecs.add(statusColSpec);

    ColumnSpec assigneeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Assignee", "assignee");
    assigneeColSpec.width = 100;
    columnSpecs.add(assigneeColSpec);

    ColumnSpec workgroupsColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Workgroup(s)", "workgroups");
    workgroupsColSpec.width = 150;
    columnSpecs.add(workgroupsColSpec);

    ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start", "startDate");
    startDateColSpec.width = 150;
    columnSpecs.add(startDateColSpec);

    ColumnSpec endDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "End", "endDate");
    endDateColSpec.width = 150;
    columnSpecs.add(endDateColSpec);

    return columnSpecs;
  }

  protected IStructuredContentProvider getContentProvider()
  {
    return new TaskInstanceContentProvider();
  }

  class TaskInstanceContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<TaskInstanceVO> rows = (List<TaskInstanceVO>) inputElement;
      return rows.toArray(new TaskInstanceVO[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  protected ITableLabelProvider getLabelProvider()
  {
    return new TaskInstanceLabelProvider();
  }

  class TaskInstanceLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      TaskInstanceVO taskInstanceVO = (TaskInstanceVO) element;
      ColumnSpec colspec = getColumnSpecs().get(columnIndex);
      if (colspec.property.equals("instanceId"))
        return taskInstanceVO.getTaskInstanceId().toString();
      else if (colspec.property.equals("name"))
        return taskInstanceVO.getTaskName();
      else if (colspec.property.equals("taskId"))
        return taskInstanceVO.getTaskId().toString();
      else if (colspec.property.equals("status"))
        return TaskStatuses.getTaskStatuses().get(taskInstanceVO.getStatusCode());
      else if (colspec.property.equals("assignee"))
        return taskInstanceVO.getTaskClaimUserCuid();
      else if (colspec.property.equals("workgroups"))
        return taskInstanceVO.getWorkgroupsString();
      else if (colspec.property.equals("startDate"))
        return taskInstanceVO.getStartDate() == null ? "" : taskInstanceVO.getStartDate();
      else if (colspec.property.equals("endDate"))
        return taskInstanceVO.getEndDate() == null ? "" : taskInstanceVO.getEndDate();
      else
        return null;
    }
  }

  private void openTaskInstance(TaskInstanceVO taskInstanceVO)
  {
    boolean assigned = activity.getProject().getUser().getUsername().equals(taskInstanceVO.getTaskClaimUserCuid());
    String taskInstParams = activity.getProject().getTaskInstancePath(taskInstanceVO.getTaskInstanceId(), assigned);
    WorkflowPackage packageVersion = activity.getPackage();
    String packageParam = packageVersion.isDefaultPackage() ? "" : "&packageName=" + packageVersion.getName();
    WebApp webapp = activity.getProject().checkRequiredVersion(5,  5) ? WebApp.MdwHub : WebApp.TaskManager;
    WebLaunchActions.getLaunchAction(activity.getProject(), webapp).launch(activity.getProject(), taskInstParams + packageParam);
  }

  /**
   * For IFilter interface.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    activity = (Activity) toTest;
    return (activity.hasInstanceInfo() && activity.isManualTask());
  }

}
