/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ProcessVersionsSection extends PropertySection implements IFilter, ElementChangeListener
{
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  private TableEditor tableEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (process != null)
      process.removeElementChangeListener(this);

    process = (WorkflowProcess) selection;
    process.addElementChangeListener(this);

    tableEditor.setElement(process);

    setTable();
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    process = (WorkflowProcess) selection;

    tableEditor = new TableEditor(process, TableEditor.TYPE_TABLE);

    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec packageColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Package", "package");
    packageColSpec.width = 160;
    columnSpecs.add(packageColSpec);
    ColumnSpec versionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Version", "version");
    versionColSpec.width = 60;
    columnSpecs.add(versionColSpec);
    ColumnSpec idColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "ID", "id");
    idColSpec.width = 65;
    columnSpecs.add(idColSpec);
    ColumnSpec createDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Created", "createDate");
    createDateColSpec.width = 110;
    columnSpecs.add(createDateColSpec);
    ColumnSpec userColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "User", "user");
    userColSpec.width = 75;
    columnSpecs.add(userColSpec);
    ColumnSpec commentsColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Comments", "comments");
    commentsColSpec.width = 200;
    columnSpecs.add(commentsColSpec);
    ColumnSpec lockedToColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Locked To", "lockedTo");
    lockedToColSpec.width = 75;
    columnSpecs.add(lockedToColSpec);
    ColumnSpec modDateColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Last Modified", "modDate");
    modDateColSpec.width = 110;
    columnSpecs.add(modDateColSpec);

    tableEditor.setColumnSpecs(columnSpecs);

    tableEditor.setReadOnly(true);

    tableEditor.setContentProvider(new ProcessVersionContentProvider());
    tableEditor.setLabelProvider(new ProcessVersionLabelProvider());

    tableEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        openProcess((WorkflowProcess)newValue);
      }
    });

    tableEditor.render(composite);

    // right-click menu
    tableEditor.getTable().addListener(SWT.MenuDetect, new Listener()
    {
      public void handleEvent(Event event)
      {
        tableEditor.getTable().setMenu(createContextMenu(tableEditor.getTable().getShell()));
      }
    });
  }

  class ProcessVersionContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<WorkflowProcess> rows = (List<WorkflowProcess>) inputElement;
      return rows.toArray(new WorkflowProcess[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class ProcessVersionLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      if (columnIndex == 0)
        return process.getPackageIconImage();
      else if (columnIndex == 2)
        return process.getIconImage();
      else
        return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      WorkflowProcess processVersion = (WorkflowProcess) element;

      switch (columnIndex)
      {
        case 0:
          return processVersion.getPackageLabel();
        case 1:
          return processVersion.getVersionLabel();
        case 2:
          return processVersion.getIdLabel();
        case 3:
          return processVersion.getFormattedCreateDate();
        case 4:
          if (processVersion.getCreateUser() == null)
            return "";
          return processVersion.getCreateUser();
        case 5:
          if (processVersion.getDescription() == null)
            return "";
          return processVersion.getDescription().replaceAll("\\\n", " ~ ");
        case 6:
          if (processVersion.getLockingUser() == null)
            return "";
          return processVersion.getLockingUser();
        case 7:
          return processVersion.getFormattedModifyDate();
        default:
          return null;
      }
    }
  }

  private void openProcess(WorkflowProcess processVersion)
  {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try
    {
      ProcessEditor processEditor = (ProcessEditor) page.openEditor(processVersion, "mdw.editors.process");
      processEditor.setFocus();
    }
    catch (PartInitException ex)
    {
      PluginMessages.uiError(composite.getShell(), ex, "Open Process", processVersion.getProject());
    }
  }

  private Menu createContextMenu(Shell shell)
  {
    Menu menu = new Menu(shell, SWT.POP_UP);

    final StructuredSelection selection = (StructuredSelection) tableEditor.getTableViewer().getSelection();
    if (selection.size() == 1  && selection.getFirstElement() instanceof WorkflowProcess)
    {
      final WorkflowProcess processVer = (WorkflowProcess) selection.getFirstElement();

      MenuItem openItem = new MenuItem(menu, SWT.PUSH);
      openItem.setText("Open");
      ImageDescriptor openImageDesc = MdwPlugin.getImageDescriptor("icons/process.gif");
      openItem.setImage(openImageDesc.createImage());
      openItem.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          openProcess(processVer);
        }
      });
    }
    // delete
    if (!selection.isEmpty() && !process.getProject().isProduction()
        && process.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)
        && (selection.size() == 1 && ((WorkflowProcess)selection.getFirstElement()).isLatest()) || MdwPlugin.getSettings().isAllowDeleteArchivedProcesses())
    {
      MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
      deleteItem.setText("Delete...");
      ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
      deleteItem.setImage(deleteImageDesc.createImage());
      deleteItem.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          if (selection.size() >= 1 && selection.getFirstElement() instanceof WorkflowProcess)
          {
            WorkflowProcess[] processVers = new WorkflowProcess[selection.size()];
            int idx = 0;
            for (Iterator<?> iter = selection.iterator(); iter.hasNext(); )
            {
              processVers[idx] = (WorkflowProcess)iter.next();
              idx++;
            }
            WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
            actionHandler.delete(processVers);
            boolean removedSelected = false;
            for (WorkflowProcess pv : processVers)
            {
              if (pv.equals(process))
                removedSelected = true;
              else
                process.remove(pv);
            }
            if (removedSelected)
            {
              WorkflowProcess sel = null;
              for (WorkflowProcess toSel : process.getAllProcessVersions())
              {
                if (!toSel.equals(process))
                {
                  sel = toSel;
                  break;
                }
              }
              setSelection(sel);
            }
            else
            {
              setSelection(process);  // just force refresh
            }
          }
        }
      });
    }

    return menu;
  }


  /**
   * Show this section for processes that are not stubs.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowProcess))
      return false;

    WorkflowProcess processVersion = (WorkflowProcess) toTest;
    return !processVersion.isStub() && !processVersion.hasInstanceInfo();
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getElement().equals(process))
    {
      if (ece.getChangeType().equals(ChangeType.RENAME) || ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
      {
        notifyLabelChange();
      }
      if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
      {
        tableEditor.setElement(process);
        setTable();
      }
    }
  }

  private void setTable()
  {
    if (!tableEditor.getTable().isDisposed())
    {
      if (process == null)
      {
        tableEditor.setValue(new ArrayList<WorkflowProcess>());
      }
      else
      {
        List<WorkflowProcess> processVersions = process.getAllProcessVersions();
        tableEditor.setValue(processVersions);
        for (int i = 0; i < processVersions.size(); i++)
        {
          if (processVersions.get(i).getVersion() == process.getVersion())
          {
            tableEditor.getTable().select(i);
            tableEditor.getTable().showSelection();
          }
        }
      }
    }
  }
}