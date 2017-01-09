/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.core.runtime.IProgressMonitor;

import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;

public class SwtProgressMonitor implements ProgressMonitor
{
  private IProgressMonitor wrappedMonitor;

  public SwtProgressMonitor(IProgressMonitor wrappedMonitor)
  {
    this.wrappedMonitor = wrappedMonitor;
  }

  IProgressMonitor getWrappedMonitor()
  {
    return wrappedMonitor;
  }

  public void start(String taskName)
  {
    if (wrappedMonitor != null)
      wrappedMonitor.beginTask(taskName, 100);
  }

  public void progress(int percentagePoints)
  {
    if (wrappedMonitor != null)
      wrappedMonitor.worked(percentagePoints);
  }

  public void subTask(String subTaskName)
  {
    if (wrappedMonitor != null)
      wrappedMonitor.subTask(subTaskName);
  }

  public void done()
  {
    if (wrappedMonitor != null)
      wrappedMonitor.done();
  }

  public boolean isCanceled()
  {
    return wrappedMonitor == null ? false : wrappedMonitor.isCanceled();
  }

  public void setCanceled(boolean canceled)
  {
    if (wrappedMonitor != null)
      wrappedMonitor.setCanceled(canceled);
  }
}
