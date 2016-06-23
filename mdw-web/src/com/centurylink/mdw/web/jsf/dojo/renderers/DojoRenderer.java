/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import com.centurylink.mdw.web.jsf.dojo.components.DojoComponent;

public abstract class DojoRenderer extends Renderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    DojoComponent dojo = (DojoComponent) component;    
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("div", dojo);
    writer.writeAttribute("dojoType", dojo.getDojoType(), "dojoType");
    if (dojo.getId() != null)
      writer.writeAttribute("id", dojo.getId(), "id");
    if (dojo.getAlign() != null)
      writer.writeAttribute("align", dojo.getAlign(), "align");
    if (dojo.getStyleClass() != null)
      writer.writeAttribute("class", dojo.getStyleClass(), "styleClass");
    if (dojo.getStyle() != null)
      writer.writeAttribute("style", dojo.getStyle(), "style");
    if (dojo.getOnclick() != null)
      writer.writeAttribute("onclick", dojo.getOnclick(), "onclick");
    if (dojo.getOnchange() != null)
      writer.writeAttribute("onchange", dojo.getOnchange(), "onchange");
    if (dojo.getOnmouseover() != null)
      writer.writeAttribute("onmouseover", dojo.getOnmouseover(), "onmouseover");
    if (dojo.isDisabled())
      writer.writeAttribute("disabled", "true", "disabled");
  }

  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement("div");
    writer.write("\n");    
  }

  public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException
  {
    DojoComponent dojo = (DojoComponent) component;    
    ResponseWriter writer = facesContext.getResponseWriter();
    if (dojo.getLabel() != null)
    {
      writer.write("\n");
      writer.startElement("span", dojo);
      if (dojo.getLabelClass() != null)
        writer.writeAttribute("class", dojo.getLabelClass(), null);
      writer.write(dojo.getLabel());
      writer.endElement("span");
      writer.write("\n");
    }
    super.encodeChildren(facesContext, component);
  }
  
  /**
   * Render a javascript reference (only once per request).
   */
  protected void renderJavascriptResource(FacesContext facesContext, UIComponent component, String scriptUrl) throws IOException
  {
    Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
    Boolean alreadyRendered = (Boolean) requestMap.get("renderedScript=" + scriptUrl);
    if (alreadyRendered == Boolean.TRUE)
      return;
    
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("script", component);
    writer.writeAttribute("type", "text/javascript", null);
    writer.writeAttribute("src", scriptUrl, null);
    writer.write(' ');
    writer.endElement("script");
    
    requestMap.put("renderedScript=" + scriptUrl, Boolean.TRUE);    
  }
  
  protected void renderDojoRequire(FacesContext facesContext, UIComponent component, String dojoRequire) throws IOException
  {
    Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
    Boolean alreadyRendered = (Boolean) requestMap.get("renderedDojoRequire=" + dojoRequire);
    if (alreadyRendered == Boolean.TRUE)
      return;
    
    renderJavascript(facesContext, component, "dojo.require(\"" + dojoRequire + "\");");
    
    requestMap.put("renderedDojoRequire=" + dojoRequire, Boolean.TRUE);    
  }
  
  protected void renderJavascript(FacesContext facesContext, UIComponent component, String script) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("script", component);
    writer.writeAttribute("type", "text/javascript", null);
    writer.write(script);
    writer.endElement("script");    
  }  
}
