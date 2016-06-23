/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;

public class DocumentStorage extends WorkflowElement implements IStorage
{
  private String name;
  private String contents;
  private DocumentReference docRef;
  public DocumentReference getDocRef() { return docRef; }

  public DocumentStorage(WorkflowProject workflowProj, String name, String contents)
  {
    setProject(workflowProj);
    this.name = name;
    this.contents = contents;
  }

  public DocumentStorage(WorkflowProject workflowProj, DocumentReference docRef)
  {
    setProject(workflowProj);
    this.docRef = docRef;
  }

  public InputStream getContents() throws CoreException
  {
    if (contents != null)
    {
      return new ByteArrayInputStream(contents.getBytes());
    }
    else
    {
      DocumentVO document = getProject().getDesignerProxy().getDocument(docRef);
      return new ByteArrayInputStream(document.getContent().getBytes());
    }
  }

  public String getName()
  {
    if (name != null)
      return name + ".xml";
    else
      return docRef.toString();
  }

  public IPath getFullPath()
  {
    return null;
  }

  public boolean isReadOnly()
  {
    return true;
  }
  public boolean isUserAllowedToEdit()
  {
    return false;
  }

  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter)
  {
    return null;
  }

  @Override
  public String getIcon()
  {
    return "doc.gif";
  }

  public Entity getActionEntity()
  {
    return Entity.Document;
  }

  @Override
  public Long getId()
  {
    return null;
  }

  @Override
  public String getTitle()
  {
    return getName();
  }

  @Override
  public boolean hasInstanceInfo()
  {
    return false;
  }

}
