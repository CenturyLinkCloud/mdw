/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public class ToolbarRenderer extends DojoLayoutRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.Toolbar");
    super.encodeBegin(facesContext, component);
  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    super.encodeEnd(facesContext, component);
  }  
}
