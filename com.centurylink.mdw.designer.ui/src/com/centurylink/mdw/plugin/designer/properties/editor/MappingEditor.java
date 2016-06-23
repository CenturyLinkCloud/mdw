/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableBinding;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class MappingEditor extends TableEditor
{
  public static final String TYPE_MAPPING = "MAPPING";

  private WorkflowProcess owningProcess;
  public WorkflowProcess getOwningProcess() { return owningProcess; }
  public void setOwningProcess(WorkflowProcess pv) { this.owningProcess = pv; }

  private Map<Integer,VariableVO> mappedVariables;
  public Map<Integer,VariableVO> getMappedVariables() { return mappedVariables; }
  public void setMappedVariables(Map<Integer,VariableVO> mappedVars) { this.mappedVariables = mappedVars; }

  private String valueAttr = "Binding";
  public String getValueAttr() { return valueAttr; }
  public void setValueAttr(String va) { this.valueAttr = va; }

  private String variableColumnLabel = "Variable";
  public String getVariableColumnLabel() { return variableColumnLabel; }
  public void setVariableColumnLabel(String vcl) { this.variableColumnLabel = vcl; }

  private String bindingColumnLabel;
  public String getBindingColumnLabel() { return bindingColumnLabel; }
  public void setBindingColumnLabel(String bcl) { this.bindingColumnLabel = bcl; }

  public MappingEditor(Activity activity)
  {
    super(activity, TYPE_MAPPING);
    setColumnDelimiter('=');
  }

  /**
   * @see com.centurylink.mdw.plugin.designer.properties.editor.TableEditor#render(org.eclipse.swt.widgets.Composite)
   */
  public void render(Composite parent)
  {
    render(parent, !isReadOnly());
  }

  public void render(Composite parent, boolean includeAddDeleteButtons)
  {
    if (getContentProvider() == null)
      setContentProvider(new MappingContentProvider());
    if (getLabelProvider() == null)
      setLabelProvider(new MappingLabelProvider());
    if (getCellModifier() == null)
      setCellModifier(new MappingCellModifier());
    if (getModelUpdater() == null)
      setModelUpdater(new MappingModelUpdater());
    if (getMappedVariables() == null && getOwningProcess() != null)
      setMappedVariables(getProcessVariables());
    if (getColumnSpecs() == null)
      setColumnSpecs(createDefaultColumnSpecs());

    super.render(parent, includeAddDeleteButtons);
  }

  public Map<Integer,VariableVO> getProcessVariables()
  {
    Map<Integer,VariableVO> processVariables = new TreeMap<Integer, VariableVO>();
    List<VariableVO> processVariableVOs = getOwningProcess().getVariables();
    for (int i = 0; i < processVariableVOs.size(); i++)
    {
      VariableVO processVariableVO = (VariableVO) processVariableVOs.get(i);
      processVariables.put(new Integer(i), processVariableVO);
    }
    return processVariables;
  }

  private String[] getVariableNames(Map<Integer,VariableVO> variables)
  {
    List<String> names = new ArrayList<String>();
    for (VariableVO variable : variables.values())
    {
      names.add(variable.getVariableName());
    }
    return names.toArray(new String[0]);
  }

  private List<ColumnSpec> createDefaultColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec variableColSpec = new ColumnSpec(PropertyEditor.TYPE_COMBO, getVariableColumnLabel(), "variable");
    variableColSpec.width = 300;
    variableColSpec.readOnly = true;
    variableColSpec.options = getVariableNames(getMappedVariables());
    columnSpecs.add(variableColSpec);

    ColumnSpec bindingColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, getBindingColumnLabel(), "binding");
    columnSpecs.add(bindingColSpec);

    return columnSpecs;
  }

  public void initValue()
  {
    Map<String,String> map = StringHelper.parseMap(getElement().getAttribute(valueAttr));
    List<VariableBinding> variableBindings = getMappedVariableBindings(map);
    setValue(variableBindings);
  }

  private List<VariableBinding> getMappedVariableBindings(Map<String,String> map)
  {
    List<VariableBinding> bindings = new ArrayList<VariableBinding>();
    for (String variableName : map.keySet())
    {
      VariableVO variableVO = getOwningProcess().getVariable(variableName);
      if (variableVO != null)
        bindings.add(new VariableBinding(variableVO, map.get(variableName)));
    }
    Collections.<VariableBinding>sort(bindings);
    return bindings;
  }

  /**
   * Converts a list of variable bindings to a string value to
   * be stored with the activity attribute.
   */
  public String serializeMapping(List<VariableBinding> bindings)
  {
    StringBuffer sb = new StringBuffer();
    boolean first = true;
    for (VariableBinding variableBinding : bindings)
    {
      if (first)
        first = false;
      else
        sb.append(getRowDelimiter());
      sb.append(variableBinding.getVariableVO().getVariableName());
      sb.append(getColumnDelimiter());
      sb.append(StringHelper.escapeWithBackslash(variableBinding.getExpression() == null ? "" : variableBinding.getExpression(), ";"));
    }
    return sb.toString();
  }

  @SuppressWarnings("rawtypes")
  public void updateModelValue(List tableValue)
  {
    getModelUpdater().updateModelValue(tableValue);
  }

  class MappingContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<VariableBinding> rows = (List<VariableBinding>) inputElement;
      return rows.toArray(new VariableBinding[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class MappingLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      VariableBinding variableBinding = (VariableBinding) element;

      switch (columnIndex)
      {
        case 0:
          return variableBinding.getVariableVO().getVariableName();
        case 1:
          return variableBinding.getExpression();
        default:
          return null;
      }
    }
  }

  class MappingCellModifier extends TableEditor.DefaultCellModifier
  {
    public Object getValue(Object element, String property)
    {
      VariableBinding variableBinding = (VariableBinding) element;
      int colIndex = getColumnIndex(property);
      switch (colIndex)
      {
        case 0:
          String varName = variableBinding.getVariableVO().getVariableName();
          for (Integer idx : getMappedVariables().keySet())
          {
            if (varName.equals(getMappedVariables().get(idx).getVariableName()))
              return idx;
          }
        case 1:
          return variableBinding.getExpression();
        default:
          return null;
      }
    }

    public void modify(Object element, String property, Object value)
    {
      TableItem item = (TableItem) element;
      VariableBinding variableBinding = (VariableBinding) item.getData();
      int colIndex = getColumnIndex(property);
      switch (colIndex)
      {
        case 0:
          variableBinding.setVariableVO(getMappedVariables().get((Integer)value));
          break;
        case 1:
          variableBinding.setExpression((String)value);
          break;
        default:
      }
      getTableViewer().update(variableBinding, null);
      updateModelValue(getTableValue());
      fireValueChanged(getTableValue());
    }
  }

  class MappingModelUpdater implements TableEditor.TableModelUpdater
  {
    public Object create()
    {
      VariableVO firstVarToBind = getMappedVariables().get(new Integer(0));
      VariableBinding variableBinding = new VariableBinding(firstVarToBind, "");
      return variableBinding;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateModelValue(List tableValue)
    {
      List<VariableBinding> variableBindings = (List<VariableBinding>) tableValue;
      List<VariableBinding> toSerialize = new ArrayList<VariableBinding>();
      for (VariableBinding potentialBinding : variableBindings)
      {
        if (!StringHelper.isEmpty(potentialBinding.getExpression()))
          toSerialize.add(potentialBinding);
      }
      String serialized = serializeMapping(toSerialize);
      getElement().setAttribute(getValueAttr(), serialized);
    }
  }
}
