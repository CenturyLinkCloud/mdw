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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.dialogs.AttributeDialog;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class AttributesSection extends PropertySection implements IFilter
{
  private WorkflowElement element;
  public WorkflowElement getElement() { return element; }

  private TableEditor tableEditor;

  public void setSelection(WorkflowElement selection)
  {
    this.element = selection;

    tableEditor.setElement(element);
    tableEditor.setValue(element.getAttributes());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    this.element = selection;

    tableEditor = new TableEditor(element, TableEditor.TYPE_TABLE);

    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();
    columnSpecs.add(new ColumnSpec(PropertyEditor.TYPE_TEXT, "Attribute Name", "name"));
    columnSpecs.add(new ColumnSpec(PropertyEditor.TYPE_TEXT, "Value", "value"));
    tableEditor.setColumnSpecs(columnSpecs);

    tableEditor.setReadOnly(true);

    tableEditor.setContentProvider(new AttributeContentProvider());
    tableEditor.setLabelProvider(new AttributeLabelProvider());
    tableEditor.render(composite);
    tableEditor.getTable().addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetDefaultSelected(SelectionEvent e)
      {
        AttributeVO attributeVO = (AttributeVO) e.item.getData();
        AttributeDialog dialog = new AttributeDialog(getShell(), attributeVO);
        dialog.open();
      }
    });
  }

  class AttributeContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<AttributeVO> rows = (List<AttributeVO>) inputElement;
      return rows.toArray(new AttributeVO[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class AttributeLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      AttributeVO attributeVO = (AttributeVO) element;

      switch (columnIndex)
      {
        case 0:
          return attributeVO.getAttributeName();
        case 1:
          return attributeVO.getAttributeValue();
        default:
          return null;
      }
    }
  }

  public boolean select(Object toTest)
  {
    if (!(toTest instanceof Activity) && !(toTest instanceof WorkflowProcess)
            && !(toTest instanceof Transition) && !(toTest instanceof ActivityImpl)
            && !(toTest instanceof EmbeddedSubProcess))
      return false;

    if (toTest instanceof Activity && ((Activity)toTest).isForProcessInstance())
      return false;
    if (toTest instanceof Transition && ((Transition)toTest).isForProcessInstance())
      return false;
    if (toTest instanceof EmbeddedSubProcess && ((EmbeddedSubProcess)toTest).isForProcessInstance())
      return false;

    if (((WorkflowElement)toTest).hasInstanceInfo())
      return false;

    return true;
  }
}