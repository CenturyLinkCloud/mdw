/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import javax.faces.component.UIComponent;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.web.jsf.components.Tab;
import com.centurylink.mdw.web.jsf.components.TabPanel;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;

public class NavigationTabChangeListener implements ValueChangeListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void processValueChange(ValueChangeEvent event) throws AbortProcessingException
  {
    UIComponent component = event.getComponent();

    if (!(component instanceof TabPanel))
    {
      String message = "Invalid component for TabChangeListener: " + component.getId();
      logger.severe(message);
      throw new AbortProcessingException(message);
    }

    TabPanel tabPanel = (TabPanel) component;

    try
    {
      String packageName = tabPanel.getPackageName();
      if (packageName == null)
      {
        if (FacesVariableUtil.getCurrentPackage() != null)
        {
          FacesVariableUtil.removeValue("mdwPackage");
          FacesVariableUtil.setSkin(ViewUI.getInstance().getSkin()); // reset skin
          ListManager.getInstance().invalidate();
          FilterManager.getInstance().invalidate();
        }
      }
      else
      {
        PackageVO packageVO = PackageVOCache.getPackageVO(packageName);
        if (packageVO != null)
        {
          PackageVO currentPackage = FacesVariableUtil.getCurrentPackage();
          if (currentPackage == null || !packageVO.getPackageName().equals(currentPackage.getPackageName()))
          {
            ((MDWBase)FacesVariableUtil.getValue("mdw")).setSkin(ViewUI.getInstance().getSkin());  // reset skin
            ListManager.getInstance().invalidate();
            FilterManager.getInstance().invalidate();
          }
          FacesVariableUtil.setValue("mdwPackage", packageVO);
        }
      }
    }
    catch (Exception ex)
    {
      throw new AbortProcessingException(ex.getMessage(), ex);
    }

    for (UIComponent child : tabPanel.getChildren())
    {
      if (child instanceof Tab)
      {
        Tab tab = (Tab) child;
        String action = tab.getActionExpression().getExpressionString();
        if (action == null)
        {
          String message = "Missing action attribute for TabPanel: " + tab.getId();
          logger.severe(message);
          throw new AbortProcessingException(message);
        }
      }
    }
  }
}
