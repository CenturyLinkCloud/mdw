/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.meta;

import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;

public class Activity extends Code
{
  // label
  private String label;
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  // icon
  private String icon = "shape:activity";
  public String getIcon() { return icon; }
  public void setIcon(String icon) { this.icon = icon; }

  // attrXml
  private String attrXml = "<PAGELET/>";
  public String getAttrXml() { return attrXml; }
  public void setAttrXml(String attrXml) { this.attrXml = attrXml; }

  // base class
  private String baseClass;
  public String getBaseClass() { return baseClass; }
  public void setBaseClass(String baseClass) { this.baseClass = baseClass; }

  /**
   * Dynamically adds the activity implementor to the designer toolbox.
   */
  public ActivityImpl createActivityImpl()
  {
    String implClass = getJavaPackage() + "." + getClassName();
    ActivityImplementorVO activityImplVO = new ActivityImplementorVO();
    activityImplVO.setImplementorClassName(implClass);
    activityImplVO.setLabel(label);
    activityImplVO.setIconName(icon);
    activityImplVO.setBaseClassName(getBaseClass());
    activityImplVO.setAttributeDescription(attrXml);

    return new ActivityImpl(activityImplVO, getPackage());
  }
}
