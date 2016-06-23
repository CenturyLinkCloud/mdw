/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Tab extends HtmlCommandLink
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.Tab";


  public String getLabel()
  {
    return (String) getStateHelper().eval("label");
  }
  public void setLabel(String label)
  {
    getStateHelper().put("label", label);
  }

  /**
   * Comma-delimited list of roles required for tab to be visible.
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

  public void setActive(boolean active)
  {
    getStateHelper().put("active", active);
  }

  public String getImageClass()
  {
    return (String) getStateHelper().eval("imageClass");
  }
  public void setImageClass(String imageClass)
  {
    getStateHelper().put("imageClass", imageClass);
  }

  /**
   * returns true if the current user should be allowed access
   * based on the component's rolesNeeded attribute
   */
  public boolean checkRoleAccess(FacesContext context)
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

  public TabPanel getEnclosingPanel(FacesContext facesContext)
  {
    if (getParent() instanceof TabPanel)
      return (TabPanel) getParent();
    else
      return null;
  }

}
