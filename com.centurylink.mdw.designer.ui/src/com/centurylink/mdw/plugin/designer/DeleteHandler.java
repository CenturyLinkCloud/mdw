/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


public class DeleteHandler extends AbstractHandler
{
  ProcessCanvasWrapper designerCanvasWrapper;
  
  public DeleteHandler()
  {
    
  }
  
  public DeleteHandler(ProcessCanvasWrapper designerCanvasWrapper)
  {
    this.designerCanvasWrapper = designerCanvasWrapper;
  }
  
  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    designerCanvasWrapper.deleteSelection();
    return null;
  }
}
