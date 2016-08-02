/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableBinding;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.MappingEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;

public class SubProcessMappingSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }

  private WorkflowProcess subProcess;
  public WorkflowProcess getSubProcess() { return subProcess; }

  private MappingEditor mappingEditor;
  private List<ColumnSpec> columnSpecs;

  private MappingLabelProvider labelProvider;
  private MappingCellModifier cellModifier;
  private MappingModelUpdater modelUpdater;

  private Map<Integer,VariableVO> inputOutputVariables;

  public void setSelection(WorkflowElement selection)
  {
    activity = (Activity) selection;
    findSubProcess();

    mappingEditor.setElement(activity);
    populateVariableBindingsTable();
    mappingEditor.setEditable(!activity.isReadOnly());
  }

  private void populateVariableBindingsTable()
  {
    if (mappingEditor != null)
      mappingEditor.disposeWidget();

    mappingEditor.setOwningProcess(subProcess);
    inputOutputVariables = getInputOutputVariables();

    columnSpecs = createColumnSpecs();
    mappingEditor.setColumnSpecs(columnSpecs);

    mappingEditor.render(composite, false);

    if (subProcess != null)
    {
      Map<String,String> map = StringHelper.parseMap(activity.getAttribute("variables"));
      // clear inapplicable bindings
      List<String> inapplicableVars = new ArrayList<String>();
      for (String varName : map.keySet())
      {
        boolean found = false;
        for (VariableVO inputVar : subProcess.getInputOutputVariables())
        {
          if (inputVar.getName().equals(varName))
          {
            found = true;
            break;
          }
        }
        if (!found)
          inapplicableVars.add(varName);
      }
      for (String inapplicableVar : inapplicableVars)
        map.remove(inapplicableVar);

      if (!inapplicableVars.isEmpty())
      {
        String newAttrVal = StringHelper.formatMap(map);
        activity.setAttribute("variables", newAttrVal);
        activity.fireAttributeValueChanged("variables", newAttrVal);
      }

      List<VariableBinding> variableBindings = getVariableBindings(map);
      mappingEditor.setValue(variableBindings);
    }

    composite.layout(true);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    mappingEditor = new MappingEditor(activity);

    if (labelProvider == null)
      labelProvider = new MappingLabelProvider();
    mappingEditor.setLabelProvider(labelProvider);

    if (cellModifier == null)
      cellModifier = new MappingCellModifier();
    mappingEditor.setCellModifier(cellModifier);

    if (modelUpdater == null)
      modelUpdater = new MappingModelUpdater();
    mappingEditor.setModelUpdater(modelUpdater);

    // widget creation is deferred until setSelection()
  }

  /**
   * Returns a list of variable bindings corresponding to the input, output
   * and input/output variables of the subprocess, with expressions populated
   * based on the activity attribute map.
   */
  private List<VariableBinding> getVariableBindings(Map<String,String> map)
  {
    List<VariableBinding> variableBindings = new ArrayList<VariableBinding>();
    for (VariableVO variableVO : inputOutputVariables.values())
    {
      VariableBinding variableBinding = new VariableBinding(variableVO, null);
      if (map.containsKey(variableVO.getVariableName()))
      {
        variableBinding.setExpression(map.get(variableVO.getVariableName()));
      }
      variableBindings.add(variableBinding);
    }
    return variableBindings;
  }

  private void findSubProcess()
  {
    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        String subProcName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        String subProcVer = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);

        subProcess = null;
        if (subProcName != null)
        {
          AssetVersionSpec spec = new AssetVersionSpec(subProcName, subProcVer);
          AssetLocator locator = new AssetLocator(activity, AssetLocator.Type.Process);
          WorkflowProcess matchingSubProc = locator.getProcessVersion(spec);
          if (matchingSubProc != null)
          {
            PluginDataAccess dataAccess = activity.getProject().getDataAccess();
            ProcessVO subProcVO = dataAccess.retrieveProcess(matchingSubProc.getName(), matchingSubProc.getVersion());
            subProcess = new WorkflowProcess(activity.getProject(), subProcVO);
          }
        }
      }
    });
  }

  /**
   * Builds a map of Integer to VariableVO for the combo box cell editor.
   */
  private Map<Integer,VariableVO> getInputOutputVariables()
  {
    Map<Integer,VariableVO> inputOutputVariables = new TreeMap<Integer, VariableVO>();
    if (subProcess != null)
    {
      List<VariableVO> inputOutputVariableVOs = subProcess.getInputOutputVariables();
      for (int i = 0; i < inputOutputVariableVOs.size(); i++)
      {
        VariableVO inputOutputVariableVO = (VariableVO) inputOutputVariableVOs.get(i);
        inputOutputVariables.put(new Integer(i), inputOutputVariableVO);
      }
    }
    return inputOutputVariables;
  }

  private List<ColumnSpec> createColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec variableColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "SubProcess Variable", "variable");
    variableColSpec.width = 175;
    variableColSpec.readOnly = true;
    columnSpecs.add(variableColSpec);

    ColumnSpec typeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Type", "type");
    typeColSpec.width = 200;
    typeColSpec.readOnly = true;
    columnSpecs.add(typeColSpec);

    ColumnSpec varModeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Mode", "mode");
    varModeColSpec.width = 125;
    varModeColSpec.readOnly = true;
    columnSpecs.add(varModeColSpec);

    ColumnSpec expressionColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Binding Expression", "expression");
    expressionColSpec.width = 175;
    columnSpecs.add(expressionColSpec);

    return columnSpecs;
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
          return variableBinding.getVariableVO().getVariableType();
        case 2:
          return lookupVariableMode(variableBinding.getVariableVO().getVariableCategory());
        case 3:
          return variableBinding.getExpression();
        default:
          return null;
      }
    }

    private String lookupVariableMode(Integer cat)
    {
      if (cat == null || cat.intValue() < 0)
        return VariableVO.VariableCategories[VariableVO.CAT_LOCAL];
      else
        return VariableVO.VariableCategories[cat.intValue()];
    }
  }

  class MappingCellModifier extends TableEditor.DefaultCellModifier
  {
    MappingCellModifier()
    {
      mappingEditor.super();
    }

    public Object getValue(Object element, String property)
    {
      VariableBinding variableBinding = (VariableBinding) element;
      int colIndex = getColumnIndex(property);
      switch (colIndex)
      {
        case 0:
          return variableBinding.getVariableVO().getVariableName();
        case 1:
          return variableBinding.getVariableVO().getVariableType();
        case 2:
          return variableBinding.getVariableVO().getVariableCategory();
        case 3:
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
          variableBinding.setVariableVO(inputOutputVariables.get((Integer)value));
          break;
        case 3:
          variableBinding.setExpression((String)value);
          break;
        default:
      }
      mappingEditor.getTableViewer().update(variableBinding, null);
      mappingEditor.updateModelValue(mappingEditor.getTableValue());
      mappingEditor.fireValueChanged(mappingEditor.getTableValue());
    }
  }

  class MappingModelUpdater implements TableEditor.TableModelUpdater
  {
    public Object create()
    {
      VariableVO firstVarToBind = inputOutputVariables.get(new Integer(0));
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
      String serialized = mappingEditor.serializeMapping(toSerialize);
      getActivity().setAttribute("variables", serialized);
    }
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    activity = (Activity) toTest;
    if (!activity.isSubProcessInvoke())
      return false;

    if (activity.isForProcessInstance())
      return false;

    PropertyEditorList propEditorList = new PropertyEditorList(activity);
    for (PropertyEditor propertyEditor : propEditorList)
    {
      if (propertyEditor.getType().equals(MappingEditor.TYPE_MAPPING))
        return true;
    }
    return false;
  }
}