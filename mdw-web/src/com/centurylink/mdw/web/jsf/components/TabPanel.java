/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;

public class TabPanel extends UIPanel
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.common.TabPanel";

  public String getStyleClass()
  {
    return (String) getStateHelper().eval("styleClass");
  }
  public void setStyleClass(String styleClass)
  {
    getStateHelper().put("styleClass", styleClass);
  }

  public String getStyle()
  {
    return (String) getStateHelper().eval("style");
  }
  public void setStyle(String style)
  {
    getStateHelper().put("style", style);
  }

  public String getHeaderClass()
  {
    return (String) getStateHelper().eval("headerClass");
  }
  public void setHeaderClass(String headerClass)
  {
    getStateHelper().put("headerClass", headerClass);
  }

  public String getSpacerImage()
  {
    return (String) getStateHelper().eval("spacerImage");
  }
  public void setSpacerImage(String spacerImage)
  {
    getStateHelper().put("spacerImage", spacerImage);
  }

  public String getSpacerClass()
  {
    return (String) getStateHelper().eval("spacerClass");
  }
  public void setSpacerClass(String spacerClass)
  {
    getStateHelper().put("spacerClass", spacerClass);
  }

  public String getActiveHeaderCellClass()
  {
    return (String) getStateHelper().eval("activeHeaderCellClass");
  }
  public void setActiveHeaderCellClass(String activeHeaderCellClass)
  {
    getStateHelper().put("activeHeaderCellClass", activeHeaderCellClass);
  }

  public String getInActiveHeaderCellClass()
  {
    return (String) getStateHelper().eval("inActiveHeaderCellClass");
  }
  public void setInActiveHeaderCellClass(String inActiveHeaderCellClass)
  {
    getStateHelper().put("inActiveHeaderCellClass", inActiveHeaderCellClass);
  }

  public String getTabClass()
  {
    return (String) getStateHelper().eval("tabClass");
  }
  public void setTabClass(String tabClass)
  {
    getStateHelper().put("tabClass", tabClass);
  }

  public String getActiveTabClass()
  {
    return (String) getStateHelper().eval("activeTabClass");
  }
  public void setActiveTabClass(String activeTabClass)
  {
    getStateHelper().put("activeTabClass", activeTabClass);
  }

  public String getInActiveTabClass()
  {
    return (String) getStateHelper().eval("inActiveTabClass");
  }
  public void setInActiveTabClass(String inActiveTabClass)
  {
    getStateHelper().put("inActiveTabClass", inActiveTabClass);
  }

  public String getFirstTabClass()
  {
    return (String) getStateHelper().eval("firstTabClass");
  }
  public void setFirstTabClass(String firstTabClass)
  {
    getStateHelper().put("firstTabClass", firstTabClass);
  }

  public String getLastTabClass()
  {
    return (String) getStateHelper().eval("lastTabClass");
  }
  public void setLastTabClass(String lastTabClass)
  {
    getStateHelper().put("lastTabClass", lastTabClass);
  }

  public String getActiveTabImage()
  {
    return (String) getStateHelper().eval("activeTabImage");
  }
  public void setActiveTabImage(String activeTabImage)
  {
    getStateHelper().put("activeTabImage", activeTabImage);
  }

  public String getActiveTabImageClass()
  {
    return (String) getStateHelper().eval("activeTabImageClass");
  }
  public void setActiveTabImageClass(String activeTabImageClass)
  {
    getStateHelper().put("activeTabImageClass", activeTabImageClass);
  }

  public String getActiveTabHeaderClass()
  {
    return (String) getStateHelper().eval("activeTabHeaderClass");
  }
  public void setActiveTabHeaderClass(String activeTabHeaderClass)
  {
    getStateHelper().put("activeTabHeaderClass", activeTabHeaderClass);
  }

  public String getHeaderSideBorderClass()
  {
    return (String) getStateHelper().eval("headerSideBorderClass");
  }
  public void setHeaderSideBorderClass(String headerSideBorderClass)
  {
    getStateHelper().put("headerSideBorderClass", headerSideBorderClass);
  }

  public String getHeaderSideCellClass()
  {
    return (String) getStateHelper().eval("headerSideCellClass");
  }
  public void setHeaderSideCellClass(String headerSideCellClass)
  {
    getStateHelper().put("headerSideCellClass", headerSideCellClass);
  }

  public String getDefaultTab()
  {
    return (String) getStateHelper().eval("defaultTab");
  }
  public void setDefaultTab(String defaultTab)
  {
    getStateHelper().put("defaultTab", defaultTab);
  }

  public String getTabChangeListener()
  {
    return (String) getStateHelper().eval("tabChangeListener");
  }
  public void setTabChangeListener(String tabChangeListener)
  {
    getStateHelper().put("tabChangeListener", tabChangeListener);
  }

  public String getPackageName()
  {
    return (String) getStateHelper().eval("packageName");
  }
  public void setPackageName(String packageName)
  {
    getStateHelper().put("packageName", packageName);
  }

  public String getActiveTab()
  {
    for (UIComponent child : getChildren())
    {
      if (child instanceof Tab)
      {
        Tab tab = (Tab) child;
        if (tab.isActive())
          return tab.getId();
      }
    }
    return null;
  }

  public void setActiveTab(String tabId)
  {
    for (UIComponent child : getChildren())
    {
      if (child instanceof Tab)
      {
        Tab tab = (Tab) child;
        tab.setActive(tab.getId().equals(tabId));
      }
    }
  }
}
