/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.ListConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;

public class WorkflowAssetCustomSection extends PropertySection implements IFilter
{
  private WorkflowAsset workflowAsset;
  public WorkflowAsset getWorkflowAsset() { return workflowAsset; }

  private PropertyEditor customAttrsPropEditor;
  private PropertyEditor rolesPropertyEditor;
  private PropertyEditor savePropertyEditor;
  private PropertyEditor helpLinkPropertyEditor;

  private CustomAttributeVO customAttribute;

  public void setSelection(WorkflowElement selection)
  {
    workflowAsset = (WorkflowAsset) selection;

    customAttrsPropEditor.setElement(workflowAsset);
    rolesPropertyEditor.setElement(workflowAsset);
    savePropertyEditor.setElement(workflowAsset);

    PluginDataAccess dataAccess = workflowAsset.getProject().getDataAccess();
    customAttribute = dataAccess.getAssetCustomAttribute(workflowAsset.getLanguage());
    if (customAttribute != null)
    {
      customAttrsPropEditor.setValue(customAttribute.getDefinition());
      rolesPropertyEditor.setValue(customAttribute.getRoles());
    }

    helpLinkPropertyEditor.setElement(workflowAsset);
    helpLinkPropertyEditor.setValue("/MDWHub/doc/customAttributes.html");

  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    workflowAsset = (WorkflowAsset) selection;

    // attr definition text area
    customAttrsPropEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
    customAttrsPropEditor.setLabel("Custom Attr Definition\n(Pagelet Syntax)");
    customAttrsPropEditor.setMultiLine(true);
    customAttrsPropEditor.setWidth(475);
    customAttrsPropEditor.setHeight(80);
    customAttrsPropEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        String def = newValue == null || ((String)newValue).length() == 0 ? null : (String)newValue;
        if (customAttribute == null)
          customAttribute = new CustomAttributeVO("RULE_SET", workflowAsset.getLanguage());

        customAttribute.setDefinition(def);
      }
    });
    customAttrsPropEditor.render(composite);

    // roles picklist
    rolesPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_PICKLIST);
    rolesPropertyEditor.setValueConverter(new ListConverter());
    rolesPropertyEditor.setLabel("Custom Attr Edit Roles:Unselected~Permitted");

    PluginDataAccess dataAccess = workflowAsset.getProject().getDataAccess();
    rolesPropertyEditor.setValueOptions(dataAccess.getRoleNames(false));
    rolesPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      @SuppressWarnings("unchecked")
      public void propertyValueChanged(Object newValue)
      {
        if (customAttribute == null)
          customAttribute = new CustomAttributeVO("RULE_SET", workflowAsset.getLanguage());

        if (newValue == null)
          customAttribute.setRoles(null);
        else
          customAttribute.setRoles((List<String>)newValue);
      }
    });
    rolesPropertyEditor.render(composite);
    rolesPropertyEditor.setValue("");

    // save button
    savePropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_BUTTON);
    savePropertyEditor.setLabel("Save");
    savePropertyEditor.setComment("Save Custom Attrs:");
    savePropertyEditor.setWidth(65);
    savePropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        saveCustomAttribute();
      }
    });
    savePropertyEditor.render(composite);

    // help link
    helpLinkPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_LINK);
    helpLinkPropertyEditor.setLabel("Custom Attributes Help");
    helpLinkPropertyEditor.render(composite);
  }

  private void saveCustomAttribute()
  {
    if (customAttribute != null)
    {
      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
      {
        public void run()
        {
          workflowAsset.getProject().getDataAccess().setAssetCustomAttribute(customAttribute);
        }
      });
    }
  }

  public boolean select(Object toTest)
  {
    // currently only workflow assets supported
    if (!(toTest instanceof WorkflowAsset))
      return false;

    WorkflowAsset asset = (WorkflowAsset) toTest;

    if (!asset.getProject().isMdw5())
      return false;

    return asset.isUserAuthorized(UserRoleVO.ASSET_DESIGN);
  }
}