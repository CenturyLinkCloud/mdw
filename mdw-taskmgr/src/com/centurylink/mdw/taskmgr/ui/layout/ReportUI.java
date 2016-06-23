/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;


public class ReportUI extends UI
{
  public String id;
  public String name;
  public String version;
  public String type;
  public String comments;
  public String packageName;
  private double height;
  private double width=100;
  private double left;
  private double top;
  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }
  public String getName()
  {
    return name;
  }
  public void setName(String name)
  {
    this.name = name;
  }
  public String getVersion()
  {
    return version;
  }
  public void setVersion(String version)
  {
    this.version = version;
  }
  public String getType()
  {
    return type;
  }
  public void setType(String type)
  {
    this.type = type;
  }
  public String getComments()
  {
    return comments;
  }
  public void setComments(String comments)
  {
    this.comments = comments;
  }
  public String getPackageName()
  {
    return packageName;
  }
  public void setPackageName(String packageName)
  {
    this.packageName = packageName;
  }
  public double getHeight()
  {
    return height;
  }
  public void setHeight(double height)
  {
    this.height = height;
  }
  public double getWidth()
  {
    return width;
  }
  public void setWidth(double width)
  {
    this.width = width;
  }
  public double getLeft()
  {
    return left;
  }
  public void setLeft(double left)
  {
    this.left = left;
  }
  public double getTop()
  {
    return top;
  }
  public void setTop(double top)
  {
    this.top = top;
  }
  @Override
  public String toString()
  {
    return "ReportUI [id=" + id + ", name=" + name + ", version=" + version + ", type=" + type
        + ", comments=" + comments + ", packageName=" + packageName + ", height=" + height
        + ", width=" + width + ", left=" + left + ", top=" + top + "]";
  }

}