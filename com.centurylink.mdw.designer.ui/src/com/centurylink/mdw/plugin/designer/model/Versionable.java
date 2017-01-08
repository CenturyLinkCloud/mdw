/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.Date;

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public interface Versionable
{
  public enum Increment
  {
    Major,
    Minor,
    Overwrite
  }

  public WorkflowProject getProject();

  public String getName();
  public String getTitle();

  public int getVersion();
  public void setVersion(int version);
  public String getVersionLabel();
  public String getVersionString();

  public int getNextMajorVersion();
  public int getNextMinorVersion();

  public int parseVersion(String versionString) throws NumberFormatException;
  public String formatVersion(int version);

  public String getLockingUser();
  public void setLockingUser(String lockUser);
  public Date getModifyDate();
  public void setModifyDate(Date modDate);

  public String getExtension();
}
