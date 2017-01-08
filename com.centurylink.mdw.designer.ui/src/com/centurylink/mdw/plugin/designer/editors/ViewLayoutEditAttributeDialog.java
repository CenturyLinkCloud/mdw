/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.dialogs.EditAttributeDialog;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.centurylink.mdw.plugin.designer.model.ViewLayout;
import com.centurylink.swt.widgets.CTreeCombo;
import com.centurylink.swt.widgets.CTreeComboItem;

@SuppressWarnings("restriction")
public class ViewLayoutEditAttributeDialog extends EditAttributeDialog
{
  private ViewLayout viewLayout;
  protected CTreeCombo attributeValueCombo;

  public ViewLayoutEditAttributeDialog(Shell parent, ViewLayout viewLayoutModel, Element ownerElement, Attr attribute)
  {
    super(parent, ownerElement, attribute);
    this.viewLayout = viewLayoutModel;
  }

  protected Composite baseCreateDialogArea(Composite parent)
  {
    // copied from Dialog
    Composite dialogArea = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    dialogArea.setLayout(layout);
    dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));
    applyDialogFont(dialogArea);
    return dialogArea;
  }

  protected Control createDialogArea(Composite parent)
  {
    Composite dialogArea = baseCreateDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogArea, XMLCommonUIContextIds.XCUI_ATTRIBUTE_DIALOG);

    Composite composite = new Composite(dialogArea, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label attributeNameLabel = new Label(composite, SWT.NONE);
    attributeNameLabel.setText(XMLUIMessages._UI_LABEL_NAME_COLON);

    attributeNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    attributeNameField.setLayoutData(gd);
    attributeNameField.setText(getDisplayValue(attribute != null ? attribute.getName() : ""));
    attributeNameField.addModifyListener(this);

    Label attributeValueLabel = new Label(composite, SWT.NONE);
    attributeValueLabel.setText(XMLUIMessages._UI_LABEL_VALUE_COLON);

    String value = attribute != null ? attribute.getValue() : "";
    int style = SWT.SINGLE | SWT.BORDER;
    if (value.indexOf("\n") != -1)
    {
      style = SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;
    }

    String[] attrOptions = viewLayout.getAttributeOptions(attribute);
    if (attrOptions != null)
    {
      attributeValueCombo = new CTreeCombo(composite, style);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.widthHint = 300;
      gd.heightHint = 16;
      attributeValueCombo.setLayoutData(gd);
      for (String option : attrOptions)
      {
        CTreeComboItem item = new CTreeComboItem(attributeValueCombo);
        item.setText(option);
        if (option.equals(ViewLayout.VARIABLES_ATTRIBUTE_OPTION))
        {
          Map<String,List<String>> vars = viewLayout.getVariables(attribute, option);
          for (String task : vars.keySet())
          {
            CTreeComboItem taskItem = new CTreeComboItem(item);
            taskItem.setText(task);
            for (String var : vars.get(task))
            {
              CTreeComboItem varItem = new CTreeComboItem(taskItem);
              varItem.setText(var);
            }
          }
        }
      }

      attributeValueCombo.setText(getDisplayValue(attribute != null ? attribute.getValue() : ""));

      attributeValueCombo.addListener(SWT.Selection, new Listener()
      {
        public void handleEvent(Event event)
        {
          CTreeComboItem[] selItems = attributeValueCombo.getSelection();
          if (selItems.length == 1)
          {
            CTreeComboItem selItem = selItems[0];
            if (selItem.getItemCount() != 0)
            {
              // ignore non-node selection
              attributeValueCombo.setSelection(new CTreeComboItem[0]);
            }
            else
            {
              if (hasVariablesParent(selItem))
                attributeValueCombo.setText("$" + selItem.getText());
              try
              {
                Thread.sleep(200);
              }
              catch (InterruptedException ex)
              {
              }
              attributeValueCombo.dropDown(false);
            }
          }
        }
      });
    }
    else
    {
      attributeValueField = new Text(composite, style);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.widthHint = 300;
      attributeValueField.setLayoutData(gd);
      attributeValueField.setText(getDisplayValue(attribute != null ? attribute.getValue() : ""));
    }

    // error message
    Composite message = new Composite(composite, SWT.NONE);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    message.setLayout(layout);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    message.setLayoutData(gd);
    errorMessageIcon = new Label(message, SWT.NONE);
    gd = new GridData();
    gd.horizontalSpan = 1;
    gd.verticalAlignment = SWT.TOP;
    errorMessageIcon.setLayoutData(gd);
    errorMessageIcon.setImage(null);
    errorMessageLabel = new Label(message, SWT.WRAP);
    errorMessageLabel.setText(XMLUIMessages.error_message_goes_here);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 200;
    gd.heightHint = Math.max(30, errorMessageLabel.computeSize(0, 0, false).y * 2);
    gd.horizontalSpan = 1;
    gd.verticalAlignment = SWT.TOP;
    errorMessageLabel.setLayoutData(gd);

    return dialogArea;
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    if (buttonId == IDialogConstants.OK_ID)
    {
      attributeName = getModelValue(attributeNameField.getText());
      if (attributeValueCombo == null)
        attributeValue = attributeValueField.getText();
      else
        attributeValue = attributeValueCombo.getText();
      okPressed();
    }
    else if (IDialogConstants.CANCEL_ID == buttonId)
    {
      cancelPressed();
    }
  }

  private boolean hasVariablesParent(CTreeComboItem item)
  {
    CTreeComboItem parentItem = item.getParentItem();
    if (parentItem == null)
      return false;
      else if (parentItem.getText().equals(ViewLayout.VARIABLES_ATTRIBUTE_OPTION))
    return true;

    return hasVariablesParent(parentItem);
  }
}
