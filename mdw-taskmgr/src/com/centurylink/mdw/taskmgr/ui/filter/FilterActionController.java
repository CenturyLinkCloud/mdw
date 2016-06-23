/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.filter;

import javax.faces.application.NavigationHandler;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.web.jsf.components.DataTable;
import com.centurylink.mdw.web.jsf.components.Filter;
import com.centurylink.mdw.web.jsf.components.FilterActionEvent;
import com.centurylink.mdw.web.jsf.components.FilterActionListener;

public class FilterActionController implements FilterActionListener
{
  public void processFilterAction(FilterActionEvent event)
  {
    UIComponent component = event.getComponent();
    if (!(component instanceof Filter))
      throw new IllegalStateException("Action source is not a Filter: " + component);

    Filter filter = (Filter) component;

    if (event.getAction().equals(FilterActionEvent.ACTION_RESET))
    {
      FilterManager.getInstance().invalidate();
      ListManager.getInstance().invalidate();
      FacesContext.getCurrentInstance().renderResponse();
    }

    // reset the associated dataTable component
    DataTable dataTable = findDataTableChild(filter.getParent());
    if (dataTable != null)
      dataTable.setFirst(0);

    if (filter.getFilterAction() != null && filter.getFilterAction().trim().length() > 0)
    {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      NavigationHandler navigator = facesContext.getApplication().getNavigationHandler();
      navigator.handleNavigation(facesContext, null, filter.getFilterAction());
    }
  }

  private DataTable findDataTableChild(UIComponent ancestor)
  {
    for (int i = 0; i < ancestor.getChildCount(); i++)
    {
      UIComponent child = (UIComponent) ancestor.getChildren().get(i);
      if (child instanceof DataTable)
      {
        return (DataTable) child;
      }
      else
      {
        UIComponent next = findDataTableChild(child);
        if (next != null)
          return (DataTable) next;
      }
    }

    return null; // not found
  }
}