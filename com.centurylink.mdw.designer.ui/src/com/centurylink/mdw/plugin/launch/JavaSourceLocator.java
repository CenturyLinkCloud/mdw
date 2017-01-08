/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

public class JavaSourceLocator extends AbstractSourceLookupDirector {
  
  private static Set<String> filteredTypes;
  
  static
  {
    filteredTypes = new HashSet<String>();
    filteredTypes.add(ProjectSourceContainer.TYPE_ID);
    filteredTypes.add(WorkspaceSourceContainer.TYPE_ID);
    filteredTypes.add("org.eclipse.debug.ui.containerType.workingSet");
  }
  
  public void initializeParticipants()
  {
    addParticipants(new ISourceLookupParticipant[]
    {
      new SourceLookupParticipant()
    });
  }

  public boolean supportsSourceContainerType(ISourceContainerType type)
  {
    return !filteredTypes.contains(type.getId());
  }
  
  class SourceLookupParticipant extends JavaSourceLookupParticipant
  {
    @Override
    public String getSourceName(Object object) throws CoreException
    {
      String sourceName = super.getSourceName(object);
      if (sourceName != null)
      {
        int idx = sourceName.indexOf(" from StringJavaFileObject");
        if (idx > 0)
          sourceName = sourceName.substring(0, idx);
      }
      return sourceName;
    }
  }
}