/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Component for a UI menu composed of navigation links.
 */
public class NavigationMenu extends UIPanel
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.common.NavigationMenu";

  @Override
  public boolean isRendered()
  {
    return super.isRendered() && checkRoleAccess(getFacesContext());
  }

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

  public String getLabel()
  {
    return (String) getStateHelper().eval("label");
  }
  public void setLabel(String label)
  {
    getStateHelper().put("label", label);
  }

  public String getLabelClass()
  {
    return (String) getStateHelper().eval("labelClass");
  }
  public void setLabelClass(String labelClass)
  {
    getStateHelper().put("labelClass", labelClass);
  }

  public String getHeaderClass()
  {
    return (String) getStateHelper().eval("headerClass");
  }
  public void setHeaderClass(String headerClass)
  {
    getStateHelper().put("headerClass", headerClass);
  }

  public String getActiveHeaderClass()
  {
    return (String) getStateHelper().eval("activeHeaderClass");
  }
  public void setActiveHeaderClass(String activeHeaderClass)
  {
    getStateHelper().put("activeHeaderClass", activeHeaderClass);
  }

  public String getActiveItemImage()
  {
    return (String) getStateHelper().eval("activeItemImage");
  }
  public void setActiveItemImage(String activeItemImage)
  {
    getStateHelper().put("activeItemImage", activeItemImage);
  }

  public String getActiveItemImageClass()
  {
    return (String) getStateHelper().eval("activeItemImageClass");
  }
  public void setActiveItemImageClass(String activeItemImageClass)
  {
    getStateHelper().put("activeItemImageClass", activeItemImageClass);
  }

  public String getImageBorderClass()
  {
    return (String) getStateHelper().eval("imageBorderClass");
  }
  public void setImageBorderClass(String imageBorderClass)
  {
    getStateHelper().put("imageBorderClass", imageBorderClass);
  }

  public boolean isCollapsible()
  {
    return (Boolean) getStateHelper().eval("collapsible", false);
  }
  public void setCollapsible(boolean collapsible)
  {
    getStateHelper().put("collapsible", collapsible);
  }

  public boolean isExpanded()
  {
    return (Boolean) getStateHelper().eval("expanded", false);
  }
  public void setExpanded(boolean expanded)
  {
    getStateHelper().put("expanded", expanded);
  }

  public String getCollapseImage()
  {
    return (String) getStateHelper().eval("collapseImage");
  }
  public void setCollapseImage(String collapseImage)
  {
    getStateHelper().put("collapseImage", collapseImage);
  }

  public String getExpandImage()
  {
    return (String) getStateHelper().eval("expandImage");
  }
  public void setExpandImage(String expandImage)
  {
    getStateHelper().put("expandImage", expandImage);
  }

  public String getExpandCollapseImageClass()
  {
    return (String) getStateHelper().eval("expandCollapseImageClass");
  }
  public void setExpandCollapseImageClass(String expandCollapseImageClass)
  {
    getStateHelper().put("expandCollapseImageClass", expandCollapseImageClass);
  }

  public String getItemGroupClass()
  {
    return (String) getStateHelper().eval("itemGroupClass");
  }
  public void setItemGroupClass(String itemGroupClass)
  {
    getStateHelper().put("itemGroupClass", itemGroupClass);
  }

  public String getDefaultItem()
  {
    return (String) getStateHelper().eval("defaultItem");
  }
  public void setDefaultItem(String defaultItem)
  {
    getStateHelper().put("defaultItem", defaultItem);
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

  public String getActiveLinkClass()
  {
    return (String) getStateHelper().eval("activeLinkClass");
  }
  public void setActiveLinkClass(String activeLinkClass)
  {
    getStateHelper().put("activeLinkClass", activeLinkClass);
  }

  public String getInActiveLinkClass()
  {
    return (String) getStateHelper().eval("inActiveLinkClass");
  }
  public void setInActiveLinkClass(String inActiveLinkClass)
  {
    getStateHelper().put("inActiveLinkClass", inActiveLinkClass);
  }

  public String getActiveItemClass()
  {
    return (String) getStateHelper().eval("activeItemClass");
  }
  public void setActiveItemClass(String activeItemClass)
  {
    getStateHelper().put("activeItemClass", activeItemClass);
  }

  public String getInActiveItemClass()
  {
    return (String) getStateHelper().eval("inActiveItemClass");
  }
  public void setInActiveItemClass(String inActiveItemClass)
  {
    getStateHelper().put("inActiveItemClass", inActiveItemClass);
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

  public void setActiveItem(String itemId)
  {
    for (NavigationLink navLink : getNavLinkDescendants())
    {
      navLink.setActive(navLink.getId().equals(itemId));
    }
  }

  public List<NavigationLink> getNavLinkDescendants()
  {
    return getNavLinkDescendants(this);
  }

  public List<NavigationLink> getNavLinkDescendants(UIComponent component)
  {
    List<NavigationLink> links = new ArrayList<NavigationLink>();
    for (UIComponent child : component.getChildren())
    {
      if (child instanceof NavigationLink)
      {
        links.add((NavigationLink)child);
      }
      else
      {
        links.addAll(getNavLinkDescendants(child));
      }
    }
    return links;
  }

  public List<NavigationMenu> getNavMenuSiblings()
  {
    List<NavigationMenu> siblings = new ArrayList<NavigationMenu>();
    if (getParent() != null)
    {
      for (UIComponent sibling : getParent().getChildren())
      {
        if (sibling instanceof NavigationMenu && !sibling.getId().equals(getId()))
          siblings.add((NavigationMenu)sibling);
      }
    }
    return siblings;
  }
}
