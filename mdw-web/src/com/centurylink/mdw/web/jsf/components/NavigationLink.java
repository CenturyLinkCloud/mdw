/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Component for a UI navigation link.
 */
public class NavigationLink extends HtmlCommandLink
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.NavigationLink";

  public boolean isImmediate()
  {
    return true;
  }

  @Override
  public boolean isRendered()
  {
    return super.isRendered() && checkRoleAccess(getFacesContext());
  }

  public String getLabel()
  {
    return (String)getStateHelper().eval("label");
  }
  public void setLabel(String label)
  {
    getStateHelper().put("label", label);
  }

  /**
   * Comma-delimited list of roles required for item to be visible.
   */
  public String getRolesNeeded()
  {
    return (String) getStateHelper().eval("rolesNeeded");
  }
  public void setRolesNeeded(String rolesNeeded)
  {
    getStateHelper().put("rolesNeeded", rolesNeeded);
  }

  public boolean isActive()
  {
    return (Boolean)getStateHelper().eval("active", false);
  }
  public boolean hasActiveValue()
  {
    return getStateHelper().get("active") != null;
  }
  public void setActive(boolean active)
  {
    getStateHelper().put("active", active);
  }

  /**
   * returns true if the current user should be allowed access
   * based on the component's rolesNeeded attribute
   */
  public boolean checkRoleAccess(FacesContext facesContext)
  {
    if (getRolesNeeded() == null)
      return true;

    AuthenticatedUser user = FacesVariableUtil.getCurrentUser();

    String[] neededRoles = getRolesNeeded().split(",");
    for (int i = 0; i < neededRoles.length; i++)
    {
      if (!user.isInRoleForAnyGroup(neededRoles[i]))
        return false;
    }
    return true;
  }

}
