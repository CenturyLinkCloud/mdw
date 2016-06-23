/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class WorkflowAssetAttributesSection extends DesignSection implements IFilter, ElementChangeListener
{
  private WorkflowAsset workflowAsset;
  public WorkflowAsset getWorkflowAsset() { return workflowAsset; }

  protected PropertyEditorList propertyEditors;

  private Label warningLabel;
  private PropertyEditor savePropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (workflowAsset != null)
      workflowAsset.removeElementChangeListener(this);

    workflowAsset = (WorkflowAsset) selection;
    workflowAsset.addElementChangeListener(this);

    if (propertyEditors != null)
    {
      for (PropertyEditor propertyEditor : propertyEditors)
        propertyEditor.dispose();
    }
    if (warningLabel != null)
      warningLabel.dispose();
    if (savePropertyEditor != null)
      savePropertyEditor.dispose();

    if (workflowAsset.getCustomAttribute().getDefinition() == null)
    {
      warningLabel = new Label(composite, SWT.NONE);
      warningLabel.setText("Please define the custom attributes.");
    }
    else
    {
      propertyEditors = new PropertyEditorList(workflowAsset, workflowAsset.getCustomAttribute().getDefinition());
      for (PropertyEditor propertyEditor : propertyEditors)
      {
        propertyEditor.render(composite);
        propertyEditor.setValue(workflowAsset);
      }

      // save button
      savePropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_BUTTON);
      savePropertyEditor.setLabel("Save");
      savePropertyEditor.setComment("Save Attributes:");
      savePropertyEditor.setWidth(65);
      savePropertyEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          saveAttributes();
        }
      });
      savePropertyEditor.render(composite);
      savePropertyEditor.setElement(workflowAsset);
    }

    composite.layout(true);
  }


  private void saveAttributes()
  {
    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        WorkflowProject wfProj = workflowAsset.getProject();
        wfProj.getDataAccess().setAttributes("RULE_SET", workflowAsset.getId(), workflowAsset.getAttributes());
        wfProj.getDesignerProxy().getCacheRefresh().doRefresh(true, false);
      }
    });
  }

  public boolean select(Object toTest)
  {
    // currently only definition docs supported
    if (!(toTest instanceof WorkflowAsset))
      return false;

    WorkflowAsset asset = (WorkflowAsset) toTest;

    if (!asset.getProject().isMdw5())
      return false;

    CustomAttributeVO customAttribute = asset.getCustomAttribute();
    if (customAttribute == null || !customAttribute.hasDefinition())
      return false;

    if (customAttribute.hasRoles())
    {
      // check authorization
      for (String role : customAttribute.getRoles())
      {
        if (asset.isUserAuthorized(role))
          return true;
      }
      return false;
    }

    return true;
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getElement().equals(workflowAsset))
    {
      if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE))
      {
        if (propertyEditors != null)
        {
          for (PropertyEditor propertyEditor : propertyEditors)
            propertyEditor.setValue((String)null);
        }
        notifyLabelChange();
      }
    }
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (workflowAsset != null)
      workflowAsset.removeElementChangeListener(this);
  }

}