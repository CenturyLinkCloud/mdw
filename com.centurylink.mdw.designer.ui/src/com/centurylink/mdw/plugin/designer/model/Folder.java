/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Logical container for workflow elements.
 */
public class Folder extends WorkflowElement implements Comparable<Folder>
{
  private List<WorkflowElement> children;
  public List<WorkflowElement> getChildren() { return children; }
  public void setChildren(List<WorkflowElement> children) { this.children = children; }
  public boolean hasChildren()
  {
    return children != null && children.size() > 0;
  }
  public void addChild(WorkflowElement child)
  {
    if (children == null)
      children = new ArrayList<WorkflowElement>();
    children.add(child);
    if (child instanceof Folder)
      ((Folder)child).parent = this;
    else if (child instanceof File)
      ((File)child).setParent(this);
  }
  public List<Folder> getSubFolders()
  {
    List<Folder> subfolders = new ArrayList<Folder>();
    if (children != null)
    {
      for (WorkflowElement child : children)
      {
        if (child instanceof Folder)
          subfolders.add((Folder)child);
      }
    }
    return subfolders;
  }

  private Folder parent;
  public Folder getParent() { return parent; }
  public boolean hasParent() { return parent != null; }

  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  private String icon = "folder.gif";
  public void setIcon(String icon) { this.icon = icon; }
  public String getIcon() { return icon; }

  public Entity getActionEntity()
  {
    return Entity.Folder;
  }

  public Folder(String name)
  {
    this.name = name;
  }

  public Folder(String name, WorkflowProject workflowProject)
  {
    setProject(workflowProject);
    this.name = name;
  }

  @Override
  public String getTitle()
  {
    return "Folder";
  }

  @Override
  public Long getId()
  {
    return convertToLong(getName());
  }

  public boolean isReadOnly()
  {
    return true;
  }

  public boolean hasInstanceInfo()
  {
    return false;
  }

  public int compareTo(Folder other)
  {
    return getName().compareTo(other.getName());
  }

  public boolean equals(Object other)
  {
    if (other instanceof Folder && getProject() == null && ((Folder)other).getProject() == null)
      return getPath().equals(((Folder)other).getPath());
    else
      return super.equals(other);
  }

  public String getPath()
  {
    if (hasParent())
      return getParent().getName() + "/" + getName();
    else
      return getName();
  }

}
