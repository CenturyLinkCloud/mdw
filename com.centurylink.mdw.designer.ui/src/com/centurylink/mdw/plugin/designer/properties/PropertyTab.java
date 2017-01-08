/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabDescriptor;

/**
 * Wraps a standard Eclipse property tab.
 */
@SuppressWarnings("restriction")
public class PropertyTab extends TabDescriptor
{
  public PropertyTab(IConfigurationElement configurationElement)
  {
    super(configurationElement);
  }

  private boolean dirty;
  public boolean isDirty() { return dirty; }
  public void setDirty(boolean dirty)
  {
    this.dirty = dirty;
  }

  /**
   * Avoid discouraged access warnings.
   */
  public String getLabel()
  {
    return super.getLabel();
  }

  @Override
  public String getText()
  {
    return dirty ? getLabel() + " *" : getLabel();
  }

  public String getOverrideAttributePrefix()
  {
    // mdw.properties.tabs.simul.override
    int override = getId().indexOf(".override");
    String trimmed = getId().substring(0, override);
    return trimmed.substring(trimmed.lastIndexOf('.') + 1).toUpperCase(); // by convention
  }

}
