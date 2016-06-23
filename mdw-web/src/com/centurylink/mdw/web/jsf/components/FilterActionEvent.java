/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIComponent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;

public class FilterActionEvent extends FacesEvent
{
  private static final long serialVersionUID = 1L;
  
  public static final String ACTION_SUBMIT = "actionSubmit";
  public static final String ACTION_RESET = "actionReset";

  private String _action;
  public String getAction() { return _action; }

  public FilterActionEvent(UIComponent component, String action)
  {
    super(component);
    _action = action;
  }

  public void processListener(FacesListener facesListener)
  {
    ((FilterActionListener)facesListener).processFilterAction(this);
  }

  public boolean isAppropriateListener(FacesListener facesListener)
  {
    return facesListener instanceof FilterActionListener;
  }
}
