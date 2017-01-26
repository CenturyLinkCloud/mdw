/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.qwest.mbeng.MbengNode;

/**
 * Not really an editor but a viewer.
 */
public class WebEditor extends PropertyEditor
{
  public static final String TYPE_WEB = "WEB";

  private Browser widget;

  public WebEditor(WorkflowElement workflowElement)
  {
    super(workflowElement, TYPE_WEB);
  }

  public WebEditor(WorkflowElement workflowElement, MbengNode mbengNode)
  {
    super(workflowElement, mbengNode);
  }

  @Override
  public void updateWidget(String value)
  {
    widget.setText(value == null ? "" : value);
  }

  public void render(Composite parent)
  {
    // widget = createBrowser(parent);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    if (widget != null)
      widget.setEnabled(enabled);
  }

  @Override
  public void setEditable(boolean editable)
  {
    setEnabled(editable);
  }

  @Override
  public void dispose()
  {
    if (widget != null && !widget.isDisposed())
      widget.dispose();
  }

  public void setVisible(boolean visible)
  {
    if (widget != null)
      widget.setVisible(visible);
  }

  public void setFocus()
  {
    if (widget != null)
      widget.setFocus();
  }

}
