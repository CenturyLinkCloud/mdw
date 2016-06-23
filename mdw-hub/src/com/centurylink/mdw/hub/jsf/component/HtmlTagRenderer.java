/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

public class HtmlTagRenderer extends Renderer {

    public static final String RENDERER_TYPE = HtmlTagRenderer.class.getName();

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        HtmlTag htmlTag = (HtmlTag) component;
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.write("<html xmlns=\"http://www.w3.org/1999/xhtml\"");
        if (htmlTag.getManifest() != null && !htmlTag.getManifest().trim().isEmpty())
            writer.write(" manifest=\"" + facesContext.getExternalContext().getRequestContextPath() + htmlTag.getManifest() + "\"");
        writer.write(">\n");
    }

    @Override
    public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException {
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        facesContext.getResponseWriter().write("\n</html>");
    } 
}
