/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

public class ElementChangeEvent
{
  public enum ChangeType
  {
    ELEMENT_CREATE,
    ELEMENT_DELETE,
    RENAME,
    VERSION_CHANGE,
    LABEL_CHANGE,
    STATUS_CHANGE,
    SETTINGS_CHANGE,
    PROPERTIES_CHANGE
  }

  private ChangeType changeType;
  public ChangeType getChangeType() { return changeType; }
  public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
  
  private WorkflowElement element;
  public WorkflowElement getElement() { return element; }
  public void setElement(WorkflowElement element) { this.element = element; }
  
  private Object newValue;
  public Object getNewValue() { return newValue; }
  public void setNewValue(Object newValue) { this.newValue = newValue; }
  
  public ElementChangeEvent(ChangeType changeType, WorkflowElement element)
  {
    this.changeType = changeType;
    this.element = element;
  }
}
