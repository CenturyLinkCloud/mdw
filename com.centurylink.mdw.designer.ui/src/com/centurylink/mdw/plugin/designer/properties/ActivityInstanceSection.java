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
import com.centurylink.mdw.plugin.designer.dialogs.ActivityInstanceDialog;
import com.centurylink.mdw.plugin.designer.dialogs.ActivityInstanceDialog.Mode;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;

public class ActivityInstanceSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }

  private TableEditor tableEditor;
  private List<ColumnSpec> columnSpecs;

  private ActivityInstanceContentProvider contentProvider;
  private ActivityInstanceLabelProvider labelProvider;

  public void setSelection(WorkflowElement selection)
  {
    activity = (Activity) selection;

    tableEditor.setElement(activity);
    tableEditor.setValue(activity.getInstances());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    tableEditor = new TableEditor(activity, TableEditor.TYPE_TABLE);
    tableEditor.setReadOnly(true);

    if (columnSpecs == null)
      columnSpecs = createColumnSpecs();
    tableEditor.setColumnSpecs(columnSpecs);

    if (contentProvider == null)
      contentProvider = new ActivityInstanceContentProvider();
    tableEditor.setContentProvider(contentProvider);

    if (labelProvider == null)
      labelProvider = new ActivityInstanceLabelProvider();
    tableEditor.setLabelProvider(labelProvider);

    tableEditor.render(composite);

    // double-click
    tableEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        new ActivityInstanceDialog(getShell(), activity, (ActivityInstanceVO)newValue, Mode.VIEW).open();
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

  private List<ColumnSpec> createColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec instanceIdColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Instance ID", "instanceId");
    instanceIdColSpec.width = 100;
    columnSpecs.add(instanceIdColSpec);

    ColumnSpec statusColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Status", "status");
    statusColSpec.width = 100;
    columnSpecs.add(statusColSpec);

    ColumnSpec statusMessageColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Completion Code / Status Message", "statusMessage");
    statusMessageColSpec.width = 200;
    columnSpecs.add(statusMessageColSpec);

    ColumnSpec startDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Start", "startDate");
    startDateColSpec.width = 150;
    columnSpecs.add(startDateColSpec);

    ColumnSpec endDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "End", "endDate");
    endDateColSpec.width = 150;
    columnSpecs.add(endDateColSpec);

    return columnSpecs;
  }


  class ActivityInstanceContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<ActivityInstanceVO> rows = (List<ActivityInstanceVO>) inputElement;
      return rows.toArray(new ActivityInstanceVO[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class ActivityInstanceLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      ActivityInstanceVO activityInstanceVO = (ActivityInstanceVO) element;

      switch (columnIndex)
      {
        case 0:
          return activityInstanceVO.getId().toString();
        case 1:
          return WorkStatuses.getWorkStatuses().get(activityInstanceVO.getStatusCode());
        case 2:
          return activityInstanceVO.getStatusMessage();
        case 3:
          if (activityInstanceVO.getStartDate() == null)
            return "";
          return activityInstanceVO.getStartDate();
        case 4:
          if (activityInstanceVO.getEndDate() == null)
            return "";
          return activityInstanceVO.getEndDate();
        default:
          return null;
      }
    }
  }

  private Menu createContextMenu(Shell shell)
  {
    Menu menu = new Menu(shell, SWT.POP_UP);

    StructuredSelection selection = (StructuredSelection) tableEditor.getTableViewer().getSelection();
    if (selection.size() == 1 && selection.getFirstElement() instanceof ActivityInstanceVO)
    {
      final ActivityInstanceVO activityInstanceVO = (ActivityInstanceVO) selection.getFirstElement();

      if (activity.getProcess().isUserAuthorized(UserRoleVO.PROCESS_EXECUTION))
      {
        // view details
        MenuItem detailsItem = new MenuItem(menu, SWT.PUSH);
        detailsItem.setText("View");
        ImageDescriptor detailsImageDesc = MdwPlugin.getImageDescriptor("icons/details.gif");
        detailsItem.setImage(detailsImageDesc.createImage());
        detailsItem.addSelectionListener(new SelectionAdapter()
        {
          public void widgetSelected(SelectionEvent e)
          {
            new ActivityInstanceDialog(getShell(), activity, activityInstanceVO, Mode.VIEW).open();
          }
        });

        // retry
        MenuItem retryItem = new MenuItem(menu, SWT.PUSH);
        retryItem.setText("Retry");
        ImageDescriptor retryImageDesc = MdwPlugin.getImageDescriptor("icons/retry.gif");
        retryItem.setImage(retryImageDesc.createImage());
        retryItem.addSelectionListener(new SelectionAdapter()
        {
          public void widgetSelected(SelectionEvent e)
          {
            new ActivityInstanceDialog(getShell(), activity, activityInstanceVO, Mode.RETRY).open();
          }
        });

        // skip
        MenuItem skipItem = new MenuItem(menu, SWT.PUSH);
        skipItem.setText("Proceed");
        ImageDescriptor skipImageDesc = MdwPlugin.getImageDescriptor("icons/skip.gif");
        skipItem.setImage(skipImageDesc.createImage());
        skipItem.addSelectionListener(new SelectionAdapter()
        {
          public void widgetSelected(SelectionEvent e)
          {
            new ActivityInstanceDialog(getShell(), activity, activityInstanceVO, Mode.SKIP).open();
          }
        });
      }
    }

    return menu;
  }

  /**
   * For IFilter interface.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    activity = (Activity) toTest;
    return activity.hasInstanceInfo();
  }
}