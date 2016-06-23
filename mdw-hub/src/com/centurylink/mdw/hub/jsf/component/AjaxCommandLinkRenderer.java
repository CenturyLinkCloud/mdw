/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import static org.richfaces.renderkit.RenderKitUtils.attributes;
import static org.richfaces.renderkit.RenderKitUtils.renderPassThroughAttributes;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.renderkit.RenderKitUtils.Attributes;
import org.richfaces.renderkit.html.CommandLinkRenderer;

/**
 * Replace RichFaces renderer to make this work with the MDW DataScroller component.
 */
public class AjaxCommandLinkRenderer extends CommandLinkRenderer {

    public static final String RENDERER_TYPE = AjaxCommandLinkRenderer.class.getName();
    
    private static final Attributes PASS_THROUGH_ATTRIBUTES = attributes()
      .generic("accesskey","accesskey")
      .generic("charset","charset")
      .generic("class","styleClass")
      .generic("coords","coords")
      .generic("dir","dir")
      .generic("hreflang","hreflang")
      .generic("lang","lang")
      .generic("onblur","onblur")
      .generic("ondblclick","ondblclick","dblclick")
      .generic("onfocus","onfocus")
      .generic("onkeydown","onkeydown","keydown")
      .generic("onkeypress","onkeypress","keypress")
      .generic("onkeyup","onkeyup","keyup")
      .generic("onmousedown","onmousedown","mousedown")
      .generic("onmousemove","onmousemove","mousemove")
      .generic("onmouseout","onmouseout","mouseout")
      .generic("onmouseover","onmouseover","mouseover")
      .generic("onmouseup","onmouseup","mouseup")
      .generic("rel","rel")
      .generic("rev","rev")
      .generic("role","role")
      .generic("shape","shape")
      .generic("style","style")
      .generic("tabindex","tabindex")
      .generic("title","title")
      .generic("type","type");

    private static final Attributes PASS_THROUGH_ATTRIBUTES2 = attributes()
      .generic("class", "styleClass")
      .generic("dir", "dir")
      .generic("lang", "lang")
      .generic("onclick","onclick","action","click")
      .generic("ondblclick","ondblclick","dblclick")
      .generic("onkeydown","onkeydown","keydown")
      .generic("onkeypress","onkeypress","keypress")
      .generic("onkeyup","onkeyup","keyup")
      .generic("onmousedown","onmousedown","mousedown")
      .generic("onmousemove","onmousemove","mousemove")
      .generic("onmouseout","onmouseout","mouseout")
      .generic("onmouseover","onmouseover","mouseover")
      .generic("onmouseup","onmouseup","mouseup")
      .generic("role","role")
      .generic("style", "style")
      .generic("title", "title");

    @Override
    public void doEncodeBegin(ResponseWriter responseWriter, FacesContext facesContext, UIComponent component) throws IOException {
        String clientId = component.getClientId(facesContext);
        if (!isDisabled(component)) {
            responseWriter.startElement("a", component);
            responseWriter.writeURIAttribute("href", "#", null);
            if (clientId != null && clientId.length() > 0) {
                responseWriter.writeAttribute("id", clientId, null);
            }
            if (clientId != null && clientId.length() > 0) {
                responseWriter.writeAttribute("name", clientId, null);
            }
            String value = getOnClick(facesContext,component);
            if (null != value && value.length()>0) {
              responseWriter.writeAttribute("onclick", value, null);
            }

            renderPassThroughAttributes(facesContext, component, PASS_THROUGH_ATTRIBUTES);

            Object text = component.getAttributes().get("value");
            if (text != null)
                responseWriter.writeText(text, null);

            //renderChildren(facesContext, component);
        }
        else {
            responseWriter.startElement("span", component);
            if (clientId != null && clientId.length() > 0) {
                responseWriter.writeAttribute("id", clientId, null);
            }

            renderPassThroughAttributes(facesContext, component, PASS_THROUGH_ATTRIBUTES2);
            Object text = component.getAttributes().get("value");
            if (text != null)
                responseWriter.writeText(text, null);
            //renderChildren(facesContext, component);
        }
    }

    @Override
    public void doEncodeEnd(ResponseWriter responseWriter, FacesContext facesContext, UIComponent component) throws IOException {
        if (!isDisabled(component)) {
            responseWriter.endElement("a");
        }
        else {
            responseWriter.endElement("span");
        }
    }
    
    private boolean isDisabled(UIComponent component) {
        Object disabledObj = component.getAttributes().get("disabled");
        return disabledObj != null && Boolean.valueOf(disabledObj.toString());
    }
}
