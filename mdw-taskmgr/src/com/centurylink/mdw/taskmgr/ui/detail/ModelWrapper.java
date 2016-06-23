/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

/**
 * Implementers are instances wrapping data model objects, exploiting
 * the Adapter pattern to expose the interface expected for display.
 */
public interface ModelWrapper
{
  public String getWrappedId();
  
  public Object getWrappedInstance();
}
