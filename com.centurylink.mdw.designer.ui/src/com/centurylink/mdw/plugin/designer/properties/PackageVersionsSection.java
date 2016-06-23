/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;

public class PackageVersionsSection extends PropertySection implements IFilter, ElementChangeListener
{
  private WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage() { return workflowPackage; }

  private TableEditor tableEditor;

  public void setSelection(WorkflowElement selection)
  {
    workflowPackage = (WorkflowPackage) selection;

    if (workflowPackage != null)
      workflowPackage.removeElementChangeListener(this);

    workflowPackage = (WorkflowPackage) selection;
    workflowPackage.addElementChangeListener(this);

    tableEditor.setElement(workflowPackage);
    setTable();
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    workflowPackage = (WorkflowPackage) selection;

    tableEditor = new TableEditor(workflowPackage, TableEditor.TYPE_TABLE);

    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec projectColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Project", "project");
    projectColSpec.width = 200;
    columnSpecs.add(projectColSpec);
    ColumnSpec versionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Version", "version");
    versionColSpec.width = 80;
    columnSpecs.add(versionColSpec);
    ColumnSpec tagsColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Tags", "tags");
    tagsColSpec.width = 150;
    columnSpecs.add(tagsColSpec);
    ColumnSpec idColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Package ID", "id");
    idColSpec.width = 100;
    columnSpecs.add(idColSpec);
    ColumnSpec lastModColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Last Modified", "lastModified");
    lastModColSpec.width = 120;
    columnSpecs.add(lastModColSpec);
    ColumnSpec packageColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Notes", "archived");
    packageColSpec.width = 150;
    columnSpecs.add(packageColSpec);
    tableEditor.setColumnSpecs(columnSpecs);

    tableEditor.setReadOnly(true);

    tableEditor.setContentProvider(new PackageVersionContentProvider());
    tableEditor.setLabelProvider(new PackageVersionLabelProvider());
    tableEditor.render(composite);
  }

  class PackageVersionContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<WorkflowPackage> rows = (List<WorkflowPackage>) inputElement;
      return rows.toArray(new WorkflowPackage[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class PackageVersionLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      if (columnIndex == 0)
        return workflowPackage.getProject().getIconImage();
      else if (columnIndex == 1)
        return workflowPackage.getIconImage();
      else
        return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      WorkflowPackage packageVersion = (WorkflowPackage) element;

      switch (columnIndex)
      {
        case 0:
          return packageVersion.getProject().getLabel();
        case 1:
          return packageVersion.getVersionLabel();
        case 2:
          return packageVersion.getTags();
        case 3:
          return packageVersion.getIdLabel();
        case 4:
          return packageVersion.getFormattedModifyDate();
        case 5:
          return packageVersion.isArchived() ? "archived" : "";
        default:
          return null;
      }
    }
  }

  /**
   * Show this section for processes that are not stubs.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowPackage))
      return false;

    return true;
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getElement().equals(workflowPackage))
    {
      if (ece.getChangeType().equals(ChangeType.RENAME) || ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
      {
        notifyLabelChange();
      }
      if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE) || ece.getChangeType().equals(ChangeType.SETTINGS_CHANGE))
      {
        tableEditor.setElement(workflowPackage);
        setTable();
      }
    }
  }

  private void setTable()
  {
    List<WorkflowPackage> packageVersions = workflowPackage.getAllPackageVersions();
    if (!tableEditor.getTable().isDisposed())
    {
      tableEditor.setValue(packageVersions);
      for (int i = 0; i < packageVersions.size(); i++)
      {
        if (packageVersions.get(i).getVersion() == workflowPackage.getVersion())
          tableEditor.getTable().select(i);
      }
    }
  }

}