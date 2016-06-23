/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.DojoLayout;

public class DojoLayoutRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.layout.LayoutContainer");
    renderDojoRequire(facesContext, component, "dijit.layout.ContentPane");
    
    super.encodeBegin(facesContext, component);
    
    DojoLayout dojoLayout = (DojoLayout) component;
    ResponseWriter writer = facesContext.getResponseWriter();
    if (dojoLayout.getLayoutAlign() != null)
      writer.writeAttribute("layoutAlign", dojoLayout.getLayoutAlign(), "layoutAlign");
    if (dojoLayout.getSizeShare() != 0)
      writer.writeAttribute("sizeShare", String.valueOf(dojoLayout.getSizeShare()), "sizeShare");
  }
  
}
