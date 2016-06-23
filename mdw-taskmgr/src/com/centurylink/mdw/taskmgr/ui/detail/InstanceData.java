/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.util.List;

import com.centurylink.mdw.web.ui.UIException;

/**
 * Represents a UI grouping of dynamic instance data line items.
 */
public interface InstanceData
{
  /**
   * Must return a List of InstanceDataItem objects.
   */
  public List<InstanceDataItem> getInstanceDataItems();
  
  /**
   * @return false if the instance data list is empty
   */
  public boolean isHasInstanceDataItems();
  
  /**
   * Retrieves dynamic (data-driven) name/value pairs.
   */
  public void retrieveInstanceData() throws UIException;
  
  public String getLabelWidth();
  public String getValueWidth();
  public int getLayoutColumns();
}
