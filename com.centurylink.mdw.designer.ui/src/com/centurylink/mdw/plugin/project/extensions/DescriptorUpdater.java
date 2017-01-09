/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Updates text-based library descriptor files when an extension is being added or removed. 
 */
public interface DescriptorUpdater
{
  public String getFilePath();
  public String processContents(String raw, IProgressMonitor monitor) throws IOException;
}
