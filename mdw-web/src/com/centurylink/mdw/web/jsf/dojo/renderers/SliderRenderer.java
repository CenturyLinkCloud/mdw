/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.Slider;

public class SliderRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.form.Slider");
    super.encodeBegin(facesContext, component);
    
    Slider slider = (Slider) component;
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.writeAttribute("maximum", "100", null);
    writer.writeAttribute("discreteValues", "100", "null");
    writer.writeAttribute("intermediateChanges", String.valueOf(slider.isIntermediateChanges()), "intermediateChanges");
  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement("div");
    writer.write("\n"); 
  }  
}
