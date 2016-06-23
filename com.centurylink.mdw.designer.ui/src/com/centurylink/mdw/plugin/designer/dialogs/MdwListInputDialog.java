/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListDialog;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class MdwListInputDialog extends ListDialog
{
  private List<? extends WorkflowElement> elements;

  private String message;
  public String getMessage() { return message; }

  private String input;
  public String getInput() { return input; }
  public void setInput(String input) { this.input = input; }

  private Text inputTextArea;
  private boolean multiLine;

  private String title = "MDW Input";
  public void setTitle(String title) { this.title = title; }

  public MdwListInputDialog(Shell parent, String title, String message, List<? extends WorkflowElement> elements)
  {
    super(parent);
    this.title = title;
    this.message = message;
    this.elements = elements;
    initialize();
  }

  public MdwListInputDialog(Shell parent, String title, String message, List<? extends WorkflowElement> elements, boolean multiLine)
  {
    this(parent, title, message, elements);
    this.multiLine = multiLine;
  }

  protected void initialize()
  {
    setAddCancelButton(true);
    setContentProvider(new ArrayContentProvider());

    int width = 0;
    // calculate dialog width
    for (WorkflowElement element : elements)
    {
      if (element.getLabel().length() > width)
        width = element.getLabel().length();
    }
    if (width != 0)
      setWidthInChars(width + 2);

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

    setInput(elements);
    setTitle(title);
    setMessage(message);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText(title);

    int style = multiLine ? SWT.MULTI | SWT.BORDER : SWT.BORDER;
    inputTextArea = new Text(composite, style);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(getWidthInChars());
    inputTextArea.setLayoutData(gd);

    if (input != null)
      inputTextArea.setText(input);

    inputTextArea.setFocus();

    return composite;
  }

  @Override
  protected void okPressed()
  {
    // set the input
    input = inputTextArea.getText().trim();
    if (input.length() == 0)
      input = null;
    setReturnCode(OK);
    close();
  }
}
