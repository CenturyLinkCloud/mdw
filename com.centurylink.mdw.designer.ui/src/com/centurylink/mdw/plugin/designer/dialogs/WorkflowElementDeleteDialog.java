/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class WorkflowElementDeleteDialog extends ListDialog
{
  private static final int PROMPT = 888;

  private List<WorkflowElement> elements;
  public List<WorkflowElement> getElements() { return elements; }

  private boolean includeInstances;
  public boolean isIncludeInstances() { return includeInstances; }

  private Button includeInstancesCheckbox;

  public WorkflowElementDeleteDialog(Shell shell, List<WorkflowElement> workflowElements)
  {
    super(shell);
    this.elements = workflowElements;

    setContentProvider(new ArrayContentProvider());

    int width = 0;
    // calculate dialog width
    for (WorkflowElement workflowElement : workflowElements)
    {
      if (workflowElement.getLabel().length() > width)
        width = workflowElement.getLabel().length();
    }
    setLabelProvider(new LabelProvider()
    {
      public Image getImage(Object element)
      {
        return ((WorkflowElement)element).getIconImage();
      }
      public String getText(Object element)
      {
        return ((WorkflowElement)element).getLabel();
      }
    });

    setInput(workflowElements);
    setTitle("Confirm Delete");
    setMessage("Delete the following workflow elements?");
    if (width != 0)
      setWidthInChars(width + 2);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Delete Workflow Elements");

    boolean showIncludeInstances = false;
    for (WorkflowElement workflowElement : elements)
    {
      if (workflowElement instanceof WorkflowProcess)
      {
        showIncludeInstances = true;
        break;
      }
    }
    if (showIncludeInstances)
    {
      includeInstancesCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
      includeInstancesCheckbox.setText("Include instances if they exist");
    }

    return composite;
  }

  @Override
  protected void okPressed()
  {
    if (includeInstancesCheckbox != null)
      includeInstances = includeInstancesCheckbox.getSelection();
    super.okPressed();
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, PROMPT, "Prompt", false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    if (buttonId == PROMPT)
    {
      setReturnCode(PROMPT);
      close();
    }
    else
    {
      super.buttonPressed(buttonId);
    }
  }
}
