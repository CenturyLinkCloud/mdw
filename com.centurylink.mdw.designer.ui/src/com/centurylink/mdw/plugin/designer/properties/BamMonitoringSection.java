/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.event.BamEventsValidator;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.BamMessageDefValueConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.qwest.mbeng.MbengException;

public class BamMonitoringSection extends PropertySection implements IFilter
{
  private WorkflowElement element;
  public WorkflowElement getElement() { return element; }

  private PropertyEditor triggerPropertyEditor;
  private PropertyEditor separatorPropertyEditor;
  private PropertyEditor bamMessagePropertyEditor;
  private PropertyEditor helpPropertyEditor;

  private String selectedTrigger = WorkAttributeConstant.BAM_START_MSGDEF;
  private Map<String,BamMessageDefinition> bamEventDefs = new HashMap<String,BamMessageDefinition>();
  private BamMessageDefValueConverter valueConverter = new BamMessageDefValueConverter();

  private BamEventsValidator bamEvtVal = new BamEventsValidator();

  public void setSelection(WorkflowElement element)
  {
    this.element = element;

    bamEventDefs.put(WorkAttributeConstant.BAM_START_MSGDEF, getValue(WorkAttributeConstant.BAM_START_MSGDEF));
    bamEventDefs.put(WorkAttributeConstant.BAM_FINISH_MSGDEF, getValue(WorkAttributeConstant.BAM_FINISH_MSGDEF));

    triggerPropertyEditor.setElement(null);  // suppress dirty state
    triggerPropertyEditor.setValue("Start");
    selectedTrigger = WorkAttributeConstant.BAM_START_MSGDEF;

    separatorPropertyEditor.setElement(element);

    boolean editable = isEditable();

    bamMessagePropertyEditor.setElement(element);
    bamMessagePropertyEditor.setValueConverter(valueConverter);
    bamMessagePropertyEditor.setValue(bamEventDefs.get(WorkAttributeConstant.BAM_START_MSGDEF));
    bamMessagePropertyEditor.setEditable(editable);

    if (element.overrideAttributesApplied())
      helpPropertyEditor.setLabel("BAM Monitoring Help");
    else
      helpPropertyEditor.setLabel("Attributes unavailable. Reload process with server online. (<A>Attributes Help</A>)");

  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    element = (WorkflowElement) selection;

    // trigger dropdown
    triggerPropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_COMBO);
    triggerPropertyEditor.setLabel("BAM Trigger");
    triggerPropertyEditor.setWidth(100);
    triggerPropertyEditor.setReadOnly(true);
    ArrayList<String> triggerOpts = new ArrayList<String>();
    triggerOpts.add("Start");
    triggerOpts.add("Finish");
    triggerPropertyEditor.setValueOptions(triggerOpts);
    triggerPropertyEditor.setFireDirtyStateChange(false);
    triggerPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        if (newValue.equals("Start"))
          selectedTrigger = WorkAttributeConstant.BAM_START_MSGDEF;
        else if (newValue.equals("Finish"))
          selectedTrigger = WorkAttributeConstant.BAM_FINISH_MSGDEF;
        if(bamEventDefs.get(selectedTrigger) == null){
          bamEventDefs.put(selectedTrigger, getValue(selectedTrigger));
        }
        bamMessagePropertyEditor.setValue(bamEventDefs.get(selectedTrigger));
        bamMessagePropertyEditor.setEditable(isEditable()); // otherwise reverts Add/Delete buttons to enabled
      }
    });
    triggerPropertyEditor.render(composite);

    separatorPropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_SEPARATOR);
    separatorPropertyEditor.setWidth(500);
    separatorPropertyEditor.render(composite);

    // bam message def
    bamMessagePropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_BAM_MESSAGE);
    bamMessagePropertyEditor.setLabel("BAM Message");
    bamMessagePropertyEditor.setWidth(500);
    bamMessagePropertyEditor.setFireDirtyStateChange(false);
    bamMessagePropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        BamMessageDefinition bamMessageDef = (BamMessageDefinition) newValue;
        bamEventDefs.put(selectedTrigger, bamMessageDef);
        element.setAttribute(selectedTrigger, valueConverter.toPropertyValue(bamMessageDef));
        setDirty(true);
      }
    });
    bamMessagePropertyEditor.render(composite);

    createSaveButton();

    // help link
    new Label(composite, SWT.NONE); // spacer
    helpPropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_LINK);
    helpPropertyEditor.setLabel("BAM Monitoring Help");
    helpPropertyEditor.render(composite);
    helpPropertyEditor.setValue("/MDWHub/doc/bam.html");
  }

  private boolean isEditable()
  {
    if (element instanceof WorkflowProcess)
    {
      WorkflowProcess pv = (WorkflowProcess) element;
      return pv.isUserAuthorized(UserRoleVO.ASSET_DESIGN) && pv.overrideAttributesApplied();
    }
    else if (element instanceof Activity)
    {
      Activity act = (Activity) element;
      return act.isUserAuthorized(UserRoleVO.ASSET_DESIGN) && act.getProcess().overrideAttributesApplied();
    }
    else if (element instanceof Transition)
    {
      Transition trans = (Transition) element;
      return trans.isUserAuthorized(UserRoleVO.ASSET_DESIGN) && trans.getProcess().overrideAttributesApplied();
    }
    else {
      return false;
    }
  }

  private BamMessageDefinition getValue(String attribute)
  {
    String attrVal = element.getAttribute(attribute);
    if (attrVal == null)
      return new BamMessageDefinition(attribute.equals(WorkAttributeConstant.BAM_START_MSGDEF));
    else
      return (BamMessageDefinition)valueConverter.toModelValue(attrVal);
  }

  public String getOverrideAttributePrefix()
  {
    return WorkAttributeConstant.BAM_ATTR_PREFIX;
  }

  @Override
  protected void saveOverrideAttributes()
  {
    // remove empty bam defs
    Map<String,BamMessageDefinition> defs = new HashMap<String,BamMessageDefinition>();
    for (String key : bamEventDefs.keySet())
    {
      BamMessageDefinition def = bamEventDefs.get(key);
      def.setAttributes(removeEmpty(def.getAttributes()));
      if (!def.isEmpty())
        defs.put(key, def);
    }
    bamEventDefs = defs;
    // sync attributes
    for (String overrideAttr : getOverrideAttributes().keySet()) {
      if (!bamEventDefs.containsKey(overrideAttr))
        element.setAttribute(overrideAttr, null); // to delete
    }
    try
    {
      for (String key : bamEventDefs.keySet())
        element.setAttribute(key, bamEventDefs.get(key).format());
      String msg = bamEvtVal.validateBamEventAttributes(bamEventDefs);
      if (msg.length() > 0)
      {
        msg += "\n Please correct the above errors before you continue.";
        MessageDialog.openError(getShell(), "Save Monitoring Attributes", msg);
      }
      else
      {
        super.saveOverrideAttributes();
      }
    }
    catch (MbengException ex)
    {
      PluginMessages.uiError(getShell(), ex, "Save Monitoring Attributes", element.getProject());
    }
  }

  protected List<AttributeVO> removeEmpty(List<AttributeVO> attributes)
  {
    List<AttributeVO> attrs = new ArrayList<AttributeVO>();
    if (attributes != null)
    {
      for (AttributeVO attr : attributes)
      {
        if (attr.getAttributeName() != null && !attr.getAttributeName().trim().isEmpty() &&
            attr.getAttributeValue() != null && !attr.getAttributeValue().trim().isEmpty())
        {
          attrs.add(attr);
        }
      }
    }
    return attrs;
  }

  /**
   * Filters for activities, processes and transitions.
   */
  public boolean select(Object toTest)
  {
    if (toTest instanceof Activity || toTest instanceof WorkflowProcess)
    {
      element = (WorkflowElement) toTest;
      if (!element.getProject().checkRequiredVersion(5, 2))
        return false;
      if (element instanceof Activity  && !((Activity)element).isForProcessInstance()) {
        return ((Activity)element).getProcess().isInRuleSet();
      }
      if (element instanceof WorkflowProcess && !((WorkflowProcess)element).hasInstanceInfo()) {
        return ((WorkflowProcess)element).isInRuleSet();
      }
    }

    return false;
  }
}