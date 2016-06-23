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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.designer.dialogs.ValueDisplayDialog;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class SimulationSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }

  private PropertyEditor simPropertyEditor;
  private TableEditor responseListTableEditor;
  private PropertyEditor helpPropertyEditor;

  private SimulationContentProvider contentProvider;
  private SimulationLabelProvider labelProvider;
  private SimulationCellModifier cellModifier;
  private SimulationModelUpdater modelUpdater;
  private List<ColumnSpec> columnSpecs;

  private int responseNo = 0;

  public void setSelection(WorkflowElement selection)
  {
    activity = (Activity) selection;

    simPropertyEditor.setElement(activity);
    boolean editable = activity.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION) && activity.getProcess().overrideAttributesApplied();
    String stubMode = activity.getAttribute(WorkAttributeConstant.SIMULATION_STUB_MODE);
    if (stubMode == null)
      simPropertyEditor.setValue("Off");
    else
      simPropertyEditor.setValue(activity.getAttribute(WorkAttributeConstant.SIMULATION_STUB_MODE));
    simPropertyEditor.setEditable(editable);

    responseListTableEditor.setElement(activity);
    responseListTableEditor.setValue(getResponses());
    responseListTableEditor.setEditable(editable);

    if (activity.overrideAttributesApplied())
      helpPropertyEditor.setLabel("Simulation Mode Help");
    else
      helpPropertyEditor.setLabel("Attributes unavailable. Reload process with server online. (<A>Attributes Help</A>)");
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    if (responseListTableEditor == null)
      responseListTableEditor = new TableEditor(activity, TableEditor.TYPE_TABLE);

    if (columnSpecs == null)
      columnSpecs = createColumnSpecs();
    responseListTableEditor.setColumnSpecs(columnSpecs);

    if (contentProvider == null)
      contentProvider = new SimulationContentProvider();

    if (labelProvider == null)
      labelProvider = new SimulationLabelProvider();

    if (cellModifier == null)
      cellModifier = new SimulationCellModifier();

    if (modelUpdater == null)
      modelUpdater = new SimulationModelUpdater();

    // simulation radio button
    simPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_RADIO);
    simPropertyEditor.setLabel("Simulation Mode");
    simPropertyEditor.setWidth(85);
    ArrayList<String> simOpts = new ArrayList<String>();
    simOpts.add("On");
    simOpts.add("Off");
    simPropertyEditor.setValueOptions(simOpts);
    simPropertyEditor.setDefaultValue("Off");
    simPropertyEditor.setFireDirtyStateChange(false);
    simPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        activity.setAttribute(WorkAttributeConstant.SIMULATION_STUB_MODE, (String)newValue);
        setDirty(true);
      }
    });
    simPropertyEditor.render(composite);

    // response table
    responseListTableEditor.setWidth(500);
    responseListTableEditor.setHeight(100);
    responseListTableEditor.setFillWidth(true);
    responseListTableEditor.setReadOnly(false);
    responseListTableEditor.setFireDirtyStateChange(false);
    responseListTableEditor.setContentProvider(contentProvider);
    responseListTableEditor.setLabelProvider(labelProvider);
    responseListTableEditor.setModelUpdater(modelUpdater);
    responseListTableEditor.setCellModifier(cellModifier);
    responseListTableEditor.setLabel("Responses");
    responseListTableEditor.render(composite);
    responseListTableEditor.getTable().addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetDefaultSelected(SelectionEvent e)
      {
        SimulationResponseVO response = (SimulationResponseVO) e.item.getData();
        String responseString = response == null ? null : response.getResponse();
        ValueDisplayDialog dlg = new ValueDisplayDialog(getShell(), responseString);
        dlg.open();
      }
    });

    // save button
    createSaveButton();

    // help link
    new Label(composite, SWT.NONE); // spacer
    helpPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
    helpPropertyEditor.setLabel("Simulation Mode Help");
    helpPropertyEditor.render(composite);
    helpPropertyEditor.setValue("/MDWHub/doc/todo.html");
  }

  public List<SimulationResponseVO> getResponses()
  {
    List<AttributeVO> attrs = activity.getAttributes();
    List<SimulationResponseVO> responses = new ArrayList<SimulationResponseVO>();

    for (int i = 0; i < attrs.size(); i++)
    {
      AttributeVO att = attrs.get(i);
      String name = att.getAttributeName();
      String value = att.getAttributeValue();
      if (name.startsWith(WorkAttributeConstant.SIMULATION_RESPONSE))
      {
        SimulationResponseVO srVO = new SimulationResponseVO();
        srVO.setAttributeName(name);
        srVO.setAttributeValue(value);
        srVO.parseAttrValues(value);
        responses.add(srVO);
        String respNo = name.substring(14);
        int biggestResp = Integer.parseInt(respNo);
        if (biggestResp > responseNo)
          responseNo = biggestResp;
      }
    }

    return responses;
  }

  /**
   * Filters for activities to which Simulation applies.
   */
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;
    Activity activity = (Activity) toTest;
    if (!activity.getProject().checkRequiredVersion(5, 2) || activity.isForProcessInstance() || !activity.getProcess().isInRuleSet())
      return false;

    return activity.isAdapter() && !activity.isLdapAdapter();
  }

  private List<ColumnSpec> createColumnSpecs()
  {
    List<ColumnSpec> columnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec codeColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Response Name", "code");
    codeColSpec.width = 150;
    columnSpecs.add(codeColSpec);

    ColumnSpec chanceColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Chance", "chance");
    chanceColSpec.width = 60;
    columnSpecs.add(chanceColSpec);

    ColumnSpec respColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Response", "response");
    respColSpec.width = 400;
    respColSpec.height = 35;
    respColSpec.style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
    columnSpecs.add(respColSpec);

    return columnSpecs;
  }

  class SimulationContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<SimulationResponseVO> rows = (List<SimulationResponseVO>) inputElement;
      return rows.toArray(new SimulationResponseVO[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class SimulationLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      SimulationResponseVO simulationResponseVO = (SimulationResponseVO) element;

      switch (columnIndex)
      {
        case 0:
          return simulationResponseVO.getReturnCode();
        case 1:
          return Integer.toString(simulationResponseVO.getChance());
        case 2:
          return simulationResponseVO.getResponse();
        default:
          return null;
      }
    }
  }

  class SimulationCellModifier extends TableEditor.DefaultCellModifier
  {
    SimulationCellModifier()
    {
      responseListTableEditor.super();
    }

    public Object getValue(Object element, String property)
    {
      SimulationResponseVO srVO = (SimulationResponseVO) element;
      int colIndex = getColumnIndex(property);
      switch (colIndex)
      {
        case 0:
          return srVO.getReturnCode();
        case 1:
          return Integer.toString(srVO.getChance());
        case 2:
          return srVO.getResponse();
        default:
          return null;
      }
    }

    public void modify(Object element, String property, Object value)
    {
      TableItem item = (TableItem) element;
      SimulationResponseVO srVO = (SimulationResponseVO) item.getData();
      int colIndex = getColumnIndex(property);
      switch (colIndex)
      {
        case 0:
          srVO.setReturnCode((String) value);
          break;
        case 1:
          try
          {
            srVO.setChance(Integer.parseInt((String) value));
          }
          catch (NumberFormatException nfe)
          {
            srVO.setChance(1);
          }
          break;
        case 2:
          srVO.setResponse((String) value);
          break;
        default:
      }
      responseListTableEditor.getTableViewer().update(srVO, null);
      modelUpdater.updateModelValue(responseListTableEditor.getTableValue());
      responseListTableEditor.fireValueChanged(responseListTableEditor.getTableValue());
      setDirty(true);
    }
  }

  class SimulationModelUpdater implements TableEditor.TableModelUpdater
  {
    public Object create()
    {
      SimulationResponseVO srVO = new SimulationResponseVO();
      responseNo++;
      srVO.setAttributeName(WorkAttributeConstant.SIMULATION_RESPONSE + responseNo);
      srVO.setReturnCode("New Response");
      srVO.setChance(1);
      srVO.setResponse("New Response Content");
      setDirty(true);
      return srVO;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateModelValue(List tableValue)
    {
      List<SimulationResponseVO> srVOs = (List<SimulationResponseVO>) tableValue;
      updateAttributes(srVOs);
    }
  }

  public void updateAttributes(List<SimulationResponseVO> srVOs)
  {
    int returnCount = 1;

    // first off, identify all the attributes to remove so the correct ones can be re-added
    List<AttributeVO> attrs = activity.getAttributes();
    ArrayList<String> deleteAttrs = new ArrayList<String>();
    for (int i = 0; i < attrs.size(); i++)
    {
      String attrName = attrs.get(i).getAttributeName();
      if (attrName.startsWith(WorkAttributeConstant.SIMULATION_RESPONSE))
        deleteAttrs.add(attrs.get(i).getAttributeName());
    }

    // remove appropriate attributes
    for (int i = 0; i < deleteAttrs.size(); i++)
    {
      activity.removeAttribute(deleteAttrs.get(i));
    }

    // now add the new or existing ones
    for (int i = 0; i < srVOs.size(); i++)
    {
      String attrName = WorkAttributeConstant.SIMULATION_RESPONSE + returnCount;
      returnCount++;
      String attrValue = srVOs.get(i).getAttributeValue();
      activity.setAttribute(attrName, attrValue);
    }
  }

  class SimulationResponseVO
  {
    private String attributeName;
    private String attributeValue;
    private String returnCode;
    private int chance;
    private String response;

    public String getAttributeName()
    {
      return attributeName;
    }

    public void setAttributeName(String attributeName)
    {
      this.attributeName = attributeName;
    }

    public String getAttributeValue()
    {
      return attributeValue;
    }

    public void setAttributeValue(String attributeValue)
    {
      this.attributeValue = attributeValue;
    }

    public String getReturnCode()
    {
      return returnCode;
    }

    public void setReturnCode(String returnCode)
    {
      this.returnCode = returnCode;
      createAttributeValue();
    }

    public int getChance()
    {
      return chance;
    }

    public void setChance(int chance)
    {
      this.chance = chance;
      createAttributeValue();
    }

    public String getResponse()
    {
      return response;
    }

    public void setResponse(String response)
    {
      this.response = response;
      createAttributeValue();
    }

    public void parseAttrValues(String attrValue)
    {
      String[] values = attrValue.split(",");
      if (values.length != 3)
        return;
      this.returnCode = values[0];
      try
      {
        this.chance = Integer.parseInt(values[1]);
      }
      catch (NumberFormatException nfe)
      {
        this.chance = 1;
      }

      this.response = values[2];
    }

    private void createAttributeValue()
    {
      this.attributeValue = this.returnCode + "," + Integer.toString(this.chance) + "," + this.response;
    }
  }

  public String getOverrideAttributePrefix()
  {
    return WorkAttributeConstant.SIMULATION_ATTR_PREFIX;
  }
}