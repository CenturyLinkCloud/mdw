/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.MenuItem;

public class MenuItemRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.Menu");
    
    super.encodeBegin(facesContext, component);
    
    MenuItem menuItem = (MenuItem) component;    
    ResponseWriter writer = facesContext.getResponseWriter();
    if (menuItem.getCheckedState() != null)
      writer.writeAttribute("iconClass", new Boolean(menuItem.getCheckedState()).booleanValue() ? "menuItemCheckboxChecked" : "menuItemCheckboxUnchecked", null);
  }
}