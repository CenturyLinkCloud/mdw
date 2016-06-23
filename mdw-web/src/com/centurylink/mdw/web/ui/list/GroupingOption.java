/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

public class GroupingOption
{
  private String id;
  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  private String label;
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  public GroupingOption(String id, String label)
  {
    this.id = id;
    this.label = label;
  }

  public String toString()
  {
    return id + "=" + label;
  }

}
