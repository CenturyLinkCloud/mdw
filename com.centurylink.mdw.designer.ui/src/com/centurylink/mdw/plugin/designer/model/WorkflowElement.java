/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class WorkflowElement implements IStructuredSelection, IAdaptable
{
  public abstract String getTitle();
  public abstract Long getId();
  public abstract String getName();
  public abstract String getIcon();
  public abstract boolean isReadOnly();
  public abstract boolean hasInstanceInfo();
  public abstract Entity getActionEntity();

  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  public void setProject(WorkflowProject project) { this.project = project; }

  private WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage()
  {
    if (workflowPackage == null)
      return project == null || project.isFilePersist() ? null : project.getDefaultPackage();

    return workflowPackage;
  }
  public void setPackage(WorkflowPackage workflowPackage) { this.workflowPackage = workflowPackage; }
  public boolean isInDefaultPackage()
  {
    return getPackage() == null || getPackage().isDefaultPackage();
  }

  public String getLabel()
  {
    return getName();  // override to include version if applicable
  }

  public String getHexId()
  {
    return Long.toHexString(getId());
  }

  public String getProjectPrefix()
  {
    String prefix = "";
    if (getProject() != null)
      prefix += getProject().getName() + ": ";
    return prefix;
  }

  public String getPath()
  {
    String path = getProjectPrefix();
    if (getProject() != null && !isInDefaultPackage())
      path += getPackage().getName() + "/";
    return path;
  }

  public String getLabelWithPackage()
  {
    if (isInDefaultPackage())
      return getLabel();
    else
      return getPackage().getName() + "/" + getLabel();
  }

  public String getFullPathLabel()
  {
    return getPath() + getLabel();
  }

  private Map<String,Image> icons = new HashMap<String,Image>();
  public Map<String,Image> getIcons() { return icons; }
  public Image getIconImage()
  {
    return getIconImage(getIcon());
  }
  public Image getIconImage(String icon)
  {
    if (icons.get(icon) == null)
    {
      ImageDescriptor imageDescriptor = MdwPlugin.getImageDescriptor("icons/" + icon);
      Image iconImage = imageDescriptor.createImage();
      icons.put(icon, iconImage);
    }
    return icons.get(icon);
  }

  public Image getPackageIconImage()
  {
    if (getPackage() == null)
      return getProject().getDefaultPackage().getIconImage();
    else
      return getPackage().getIconImage();
  }

  public String getPackageLabel()
  {
    if (getPackage() == null)
      return PackageVO.DEFAULT_PACKAGE_NAME;
    else
      return getPackage().getName() + " " + getPackage().getVersionLabel();
  }

  // any element may be contained in a folder
  private Folder folder;
  public Folder getArchivedFolder() { return folder; }
  public void setArchivedFolder(Folder f) { this.folder = f; }

  public boolean isEmpty()
  {
    return false;
  }

  public boolean isArchived()
  {
    return false;
  }

  public boolean isHomogeneous(WorkflowElement other)
  {
    if (other == null || !other.getClass().getName().equals(this.getClass().getName()))
      return false;

    return true;
  }

  public Object getFirstElement()
  {
    return this;
  }

  public List<AttributeVO> getAttributes()
  {
    return null;  // currently overridden by Activity, ActivityImpl, Transition, WorkflowProcess and EmbeddedSubProcess
  }

  /**
   * Gets the specified attribute
   * @param attrName
   * @return the string value, null if not found
   */
  public String getAttribute(String attrName)
  {
    if (getAttributes() != null)
    {
      for (AttributeVO attribute : getAttributes())
      {
        if (attribute.getAttributeName().equals(attrName))
          return attribute.getAttributeValue();
      }
    }
    return null;  // not found
  }

  public void setAttribute(String name, String value)
  {
    if (!(this instanceof AttributeHolder))
      throw new UnsupportedOperationException("WorkflowElement does not implement AttributeHolder: " + this.getClass().getName());
    ((AttributeHolder)this).setAttribute(name, value);
  }

  public String getVersionLabel()
  {
    return null;  // overridden by versionable elements
  }

  @SuppressWarnings("rawtypes")
  public Iterator iterator()
  {
    return new Iterator()
    {
      boolean retrieved = false;

      public boolean hasNext()
      {
        return !retrieved;
      }

      public Object next()
      {
        retrieved = true;
        return WorkflowElement.this;
      }

      public void remove()
      {
      }
    };
  }

  public int size()
  {
    return 1;
  }

  public Object[] toArray()
  {
    Object[] ret = new Object[1];
    ret[0] = this;
    return ret;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public List toList()
  {
    List list = new ArrayList();
    list.add(this);
    return list;
  }

  private List<DirtyStateListener> dirtyStateListeners = new Vector<DirtyStateListener>();
  public List<DirtyStateListener> getDirtyStateListeners() { return dirtyStateListeners; }

  public void addDirtyStateListener(DirtyStateListener listener)
  {
    dirtyStateListeners.add(listener);
  }

  public void removeDirtyStateListener(DirtyStateListener listener)
  {
    dirtyStateListeners.remove(listener);
  }

  public void fireDirtyStateChanged(boolean dirty)
  {
    for (DirtyStateListener listener : dirtyStateListeners)
      listener.dirtyStateChanged(dirty);
  }

  private List<ElementChangeListener> elementChangeListeners = new Vector<ElementChangeListener>();
  public List<ElementChangeListener> getElementChangeListeners() { return elementChangeListeners; }

  public void addElementChangeListener(ElementChangeListener listener)
  {
    elementChangeListeners.add(listener);
  }

  public void removeElementChangeListener(ElementChangeListener listener)
  {
    elementChangeListeners.remove(listener);
  }

  public void fireElementChangeEvent(ChangeType changeType, Object newValue)
  {
    fireElementChangeEvent(this, changeType, newValue);
  }

  public void fireElementChangeEvent(WorkflowElement workflowElement, ChangeType changeType, Object newValue)
  {
    for (ElementChangeListener listener : getElementChangeListeners())
    {
      ElementChangeEvent ece = new ElementChangeEvent(changeType, workflowElement);
      ece.setNewValue(newValue);
      listener.elementChanged(ece);
    }
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof WorkflowElement) || o == null)
      return false;
    WorkflowElement other = (WorkflowElement) o;

    if (this.getProject() == null || !this.getProject().equals(other.getProject()))
      return false;

    if (!this.getClass().equals(other.getClass()))
      return false;

    if (this.getId() == null)
      return other.getId() == null;
    return this.getId().equals(other.getId());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public Object getAdapter(Class adapter)
  {
    if (adapter.equals(IProject.class) && getProject() != null)
      return getProject().getSourceProject();
    else
      return null;
  }

  protected Long convertToLong(String string)
  {
    if (string == null)
      return null;
    String converted = "";
    for (int i = 0; i < string.length() && converted.length() < 16; i++)
    {
      converted += (int)string.charAt(i);
    }
    return new Long(converted);
  }

  public static Map<String,String> getArtifactFileExtensions()
  {
    return RuleSetVO.getLanguageToExtension();
  }

  private static List<String> resourceTypes;
  public static List<String> getResourceTypes()
  {
    if (resourceTypes == null)
    {
      resourceTypes = new Vector<String>();
      for (String type : getArtifactFileExtensions().keySet())
      {
        boolean alreadyPresent = false;
        for (String resourceType : resourceTypes)
        {
          if (resourceType.equalsIgnoreCase(type))
          {
            alreadyPresent = true;
            break;
          }
        }
        if (!alreadyPresent)
          resourceTypes.add(type);
      }
      Collections.sort(resourceTypes);
    }
    return resourceTypes;
  }

  public void dispose()
  {
    for (Image iconImage : icons.values())
      iconImage.dispose();
  }

  /**
   * Only relevant for file-based asset persistence.
   */
  private IResource resource;
  public IResource getResource() { return resource; }
  public void setResource(IResource resource) { this.resource = resource; }

  /**
   * Convenience method for checking package level auth for an asset.
   */
  public boolean isUserAuthorized(String role)
  {
    if (getProject().isFilePersist() && UserRoleVO.ASSET_DESIGN.equals(role))
    {
      if (getProject().isRemote())
      {
        if (!getProject().isGitVcs() || getProject().getMdwVcsRepository().isGitProjectSync())
          return false; // only unlocked remote projects can be edited
      }
      else
      {
        return true;
      }
    }

    return getPackage() == null ? false : getPackage().isUserAuthorized(role);
  }

  /**
   * Override for elements related to a process.
   */
  public Long getProcessId()
  {
    return null;
  }

  /**
   * Override for elements that support override attributes.
   */
  public boolean overrideAttributesApplied()
  {
    return false;
  }

  private List<String> dirtyOverrideAttrPrefixes;
  public boolean isOverrideAttributeDirty(String prefix)
  {
    if (prefix == null || dirtyOverrideAttrPrefixes == null)
      return false;
    else
      return dirtyOverrideAttrPrefixes.contains(prefix);
  }
  public void setOverrideAttributeDirty(String prefix, boolean dirty)
  {
    if (prefix == null)
      return;
    if (dirty)
    {
      if (dirtyOverrideAttrPrefixes == null)
        dirtyOverrideAttrPrefixes = new ArrayList<String>();
      if (!dirtyOverrideAttrPrefixes.contains(prefix))
        dirtyOverrideAttrPrefixes.add(prefix);
    }
    else
    {
      if (dirtyOverrideAttrPrefixes != null)
        dirtyOverrideAttrPrefixes.remove(prefix);
    }
  }

  public boolean isAnyOverrideAttributeDirty()
  {
    return dirtyOverrideAttrPrefixes != null && !dirtyOverrideAttrPrefixes.isEmpty();
  }
  public void clearOverrideAttributesDirty()
  {
    dirtyOverrideAttrPrefixes = null;
  }
}
