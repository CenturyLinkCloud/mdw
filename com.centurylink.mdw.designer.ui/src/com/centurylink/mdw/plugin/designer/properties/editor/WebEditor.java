/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.qwest.mbeng.MbengNode;

public class WebEditor extends PropertyEditor
{
  public static final String TYPE_WEB = "WEB";

  public WebEditor(WorkflowElement workflowElement)
  {
    super(workflowElement, TYPE_WEB);
  }

  public WebEditor(WorkflowElement workflowElement, MbengNode mbengNode)
  {
    super(workflowElement, mbengNode);
  }

}
