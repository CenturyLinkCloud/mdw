/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class WorkflowAssetSection extends PropertySection implements ElementChangeListener
{
  private WorkflowAsset workflowAsset;
  public WorkflowAsset getWorkflowAsset() { return workflowAsset; }

  private PropertyEditor idPropertyEditor;
  private PropertyEditor namePropertyEditor;
  private PropertyEditor languagePropertyEditor;
  private PropertyEditor versionCommentPropertyEditor;
  private PropertyEditor linkPropertyEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (workflowAsset != null)
      workflowAsset.removeElementChangeListener(this);

    workflowAsset = (WorkflowAsset) selection;
    workflowAsset.addElementChangeListener(this);

    idPropertyEditor.setElement(workflowAsset);
    idPropertyEditor.setValue(workflowAsset.getIdLabel());

    namePropertyEditor.setElement(workflowAsset);
    namePropertyEditor.setValue(workflowAsset.getName());

    languagePropertyEditor.setElement(workflowAsset);
    languagePropertyEditor.setValue(workflowAsset.getLanguageFriendly());

    linkPropertyEditor.setElement(workflowAsset);
    linkPropertyEditor.setLabel("Open " + workflowAsset.getTitle());

    versionCommentPropertyEditor.setElement(workflowAsset);
    versionCommentPropertyEditor.setValue(workflowAsset.getComment());

    List<String> options = new ArrayList<String>();
    options.add("");
    if (workflowAsset.getProject().checkRequiredVersion(5, 2));
      options.addAll(getDesignerDataModel().getWorkgroupNames());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    workflowAsset = (WorkflowAsset) selection;

    // id text field
    idPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
    idPropertyEditor.setLabel("ID");
    idPropertyEditor.setWidth(150);
    idPropertyEditor.setReadOnly(true);
    idPropertyEditor.render(composite);

    // name text field
    namePropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
    namePropertyEditor.setLabel("Name");
    namePropertyEditor.setWidth(415);
    namePropertyEditor.setReadOnly(true);
    namePropertyEditor.render(composite);

    // language text field
    languagePropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
    languagePropertyEditor.setLabel("Type");
    languagePropertyEditor.setWidth(200);
    languagePropertyEditor.setReadOnly(true);
    languagePropertyEditor.render(composite);

    // link
    linkPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_LINK);
    linkPropertyEditor.setLabel("Open " + workflowAsset.getTitle());
    linkPropertyEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        openWorkflowAsset();
      }
    });
    linkPropertyEditor.render(composite);

    // version comment text field
    versionCommentPropertyEditor = new PropertyEditor(workflowAsset, PropertyEditor.TYPE_TEXT);
    versionCommentPropertyEditor.setLabel("Version Comment");
    versionCommentPropertyEditor.setMultiLine(true);
    versionCommentPropertyEditor.setHeight(75);
    versionCommentPropertyEditor.setReadOnly(true);
    versionCommentPropertyEditor.render(composite);
  }

  private void openWorkflowAsset()
  {
    workflowAsset.openFile(new NullProgressMonitor());
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getElement().equals(workflowAsset))
    {
      if (ece.getChangeType().equals(ChangeType.RENAME))
      {
        if (!namePropertyEditor.getValue().equals(ece.getNewValue()))
          namePropertyEditor.setValue(ece.getNewValue().toString());
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
