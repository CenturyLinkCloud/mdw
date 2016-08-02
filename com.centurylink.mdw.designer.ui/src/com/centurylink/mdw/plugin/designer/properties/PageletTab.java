/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractSectionDescriptor;
import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

/**
 * Dynamic pagelet-driven tab content.  Currently always treated as override attributes:
 * TODO: Support pagelet for non-override attributes (should be easy when needed).
 * Tabs are added automatically for the appropriate workflow elements by
 * virtue of a .pagelet asset.
 * TODO: Allow pagelet def in a process's same package to override others.
 */
public class PageletTab extends AbstractTabDescriptor
{
  private String id;
  public String getId() { return id; }
  public String toString()
  {
    return id;
  }

  private String label;
  public String getLabel()
  {
    return label;
  }

  public String getOverrideAttributePrefix()
  {
    return label.toUpperCase(); // by convention (could add to pagelet schema also)
  }

  @Override
  public String getText()
  {
    return dirty ? label + " *" : label;
  }

  private boolean dirty;
  public boolean isDirty() { return dirty; }
  public void setDirty(boolean dirty)
  {
    this.dirty = dirty;
  }

  private String xml;
  public String getXml() { return xml; }

  public PageletTab(String label, String xml)
  {
    this.label = label;
    this.xml = xml;
    String name = label.toLowerCase().replaceAll(" ", "");
    this.id = "mdw.properties.tabs.custom." + name;
    List<ISectionDescriptor> sectionDescriptors = new ArrayList<ISectionDescriptor>();
    String sectionId = "mdw.properties.section.custom." + name;
    sectionDescriptors.add(new SectionDescriptor(sectionId, id));
    setSectionDescriptors(sectionDescriptors);
  }

  public String getCategory()
  {
    return "mdw.tabbedprops";
  }

  class SectionDescriptor extends AbstractSectionDescriptor
  {
    private String id;
    private String tabId;
    public SectionDescriptor(String id, String tabId)
    {
      this.id = id;
      this.tabId = tabId;
    }

    public String getId()
    {
      return id;
    }

    public ISection getSectionClass()
    {
      return new PageletSection(tabId);
    }

    public String getTargetTab()
    {
      return tabId;
    }

    @Override
    public boolean appliesTo(IWorkbenchPart part, ISelection selection)
    {
      // actually filtering is already done in TabDescriptorProvider
      return selection instanceof WorkflowElement;
    }
  }
}
