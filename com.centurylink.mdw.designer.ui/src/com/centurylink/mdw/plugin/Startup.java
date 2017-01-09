/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;

public class Startup implements IStartup
{
  public void earlyStartup()
  {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(WorkflowProjectManager.getInstance(), IResourceChangeEvent.POST_CHANGE);
  }
}
