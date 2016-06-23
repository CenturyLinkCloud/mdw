/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.SplitContainer;

public class SplitContainerRenderer extends DojoLayoutRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.layout.SplitContainer");
    
    super.encodeBegin(facesContext, component);
    
    SplitContainer splitContainer = (SplitContainer) component;
    ResponseWriter writer = facesContext.getResponseWriter();     
    writer.writeAttribute("orientation", splitContainer.getOrientation(), "orientation");
    writer.writeAttribute("sizerWidth", String.valueOf(splitContainer.getSizerWidth()), "sizerWidth");
    writer.writeAttribute("activeSizing", String.valueOf(splitContainer.isActiveSizing()), "activeSizing");
  }

}
