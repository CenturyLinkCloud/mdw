/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.dialogs.MdwInputDialog;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class ActivityImplSection extends PropertySection
{
  private ActivityImpl activityImpl;
  public ActivityImpl getActivityImpl() { return activityImpl; }

  private PropertyEditor idField;
  private PropertyEditor implClassLink;
  private PropertyEditor implClassButton;
  private PropertyEditor labelField;
  private PropertyEditor iconField;
  private PropertyEditor helpLink;

  public void setSelection(WorkflowElement selection)
  {
    activityImpl = (ActivityImpl) selection;

    idField.setElement(activityImpl);
    idField.setValue(activityImpl.getId());

    implClassLink.setElement(activityImpl);
    implClassLink.setLabel(activityImpl.getImplClassName());

    implClassButton.setElement(activityImpl);
    implClassButton.setEditable(!activityImpl.isReadOnly());

    labelField.setElement(activityImpl);
    labelField.setValue(activityImpl.getLabel());
    labelField.setEditable(!activityImpl.isReadOnly());

    iconField.setElement(activityImpl);
    iconField.setValue(activityImpl.getIconName());
    iconField.setEditable(!activityImpl.isReadOnly());

    helpLink.setElement(activityImpl);
    helpLink.setValue("/MDWHub/doc/implementor.html");
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activityImpl = (ActivityImpl) selection;

    // id text field
    idField = new PropertyEditor(activityImpl, PropertyEditor.TYPE_TEXT);
    idField.setLabel("ID");
    idField.setWidth(150);
    idField.setReadOnly(true);
    idField.render(composite);

    // impl class name link
    implClassLink = new PropertyEditor(activityImpl, PropertyEditor.TYPE_LINK);
    implClassLink.setWidth(600);
    implClassLink.setHeight(17);
    implClassLink.setFont(new FontData("Tahoma", 8, SWT.BOLD));
    implClassLink.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          activityImpl.getProject().viewSource(activityImpl.getImplClassName());
        }
      });
    implClassLink.render(composite);

    // impl class button
    implClassButton = new PropertyEditor(activityImpl, PropertyEditor.TYPE_BUTTON);
    implClassButton.setLabel("Change...");
    implClassButton.setVerticalIndent(-3);
    implClassButton.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          launchImplClassNameDialog();
        }
      });
    implClassButton.render(composite);

    // label text field
    labelField = new PropertyEditor(activityImpl, PropertyEditor.TYPE_TEXT);
    labelField.setLabel("Label");
    labelField.setWidth(200);
    labelField.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          activityImpl.setLabel((String)newValue);
        }
      });
    labelField.render(composite);

    // icon text field
    iconField = new PropertyEditor(activityImpl, PropertyEditor.TYPE_TEXT);
    iconField.setLabel("Icon");
    iconField.setWidth(200);
    iconField.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          activityImpl.setIconName((String)newValue);
        }
      });
    iconField.render(composite);

    // implementor help link
    helpLink = new PropertyEditor(activityImpl, PropertyEditor.TYPE_LINK);
    helpLink.setLabel("Activity Implementor Help");
    helpLink.render(composite);
  }

  private void launchImplClassNameDialog()
  {
    MdwInputDialog classNameDialog = new MdwInputDialog(getShell(), "Fully-Qualified Implementor Class Name", false);
    classNameDialog.setTitle("Activity Implementor");
    classNameDialog.setWidth(300);
    classNameDialog.setInput(activityImpl.getImplClassName());
    if (classNameDialog.open() == Dialog.OK)
    {
      final String newClassName = classNameDialog.getInput().trim();
      if (newClassName.length() > 0)
      {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
        {
          public void run()
          {
            activityImpl.setImplClassName(newClassName);
            activityImpl.getProject().getDesignerProxy().saveActivityImpl(activityImpl);
            activityImpl.getProject().setActivityImplClass(activityImpl.getImplClassName(), activityImpl);
            setSelection(activityImpl);
          }
        });
      }
    }
  }
}