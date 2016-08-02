/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

/**
 * Listener for process editor context events.
 */
public class ProcessEditorContextListener extends WorkflowEditorPartListener
{
  public ProcessEditorContextListener(WorkflowProcess processVersion)
  {
    super(processVersion);
  }

  public void broughtToTop(WorkflowElementEditor editor)
  {
    ProcessEditor processEditor = (ProcessEditor) editor;
    // force canvas to assume current link shape
    processEditor.setCanvasLinkStyle(ProcessEditorActionBarContributor.getLinkStyle());
    processEditor.updateCanvasBackground();
  }

  public void closed(WorkflowElementEditor editor)
  {
    ProcessEditor processEditor = (ProcessEditor) editor;
    processEditor.remove();
  }
}
