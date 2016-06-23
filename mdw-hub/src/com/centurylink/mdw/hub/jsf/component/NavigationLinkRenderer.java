/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlLinkRendererBase;

import com.centurylink.mdw.web.jsf.components.NavOutputLink;
import com.centurylink.mdw.web.jsf.components.NavigationLink;
import com.centurylink.mdw.web.jsf.components.NavigationMenu;

/**
 * HTML renderer for a navigation link.
 * Reimplemented for HTML5 engine due to MyFaces shared impl repackaging.
 */
public class NavigationLinkRenderer extends HtmlLinkRendererBase {
    public static final String RENDERER_TYPE = NavigationLinkRenderer.class.getName();

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        NavigationMenu navMenu = findNavMenuParent(component);

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(HTML.TR_ELEM, component);
        writer.startElement(HTML.TD_ELEM, component);
        writer.writeAttribute(HTML.STYLE_ATTR, "padding-right:0;", null);
        writer.startElement(HTML.DIV_ELEM, component);
        if (component instanceof NavigationLink && ((NavigationLink)component).isActive()) {
            if (navMenu.getActiveItemClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getActiveItemClass(), "activeItemClass");
            if (navMenu.getActiveLinkClass() != null)
                ((NavigationLink)component).setStyleClass(navMenu.getActiveLinkClass());
        }
        else {
            if (navMenu.getInActiveItemClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getInActiveItemClass(), "inActiveItemClass");
            if (navMenu.getInActiveLinkClass() != null) {
                if (component instanceof NavigationLink)
                    ((NavigationLink)component).setStyleClass(navMenu.getInActiveLinkClass());
                else if (component instanceof NavOutputLink)
                    ((NavOutputLink)component).setStyleClass(navMenu.getInActiveLinkClass());
            }
        }

        if (component instanceof NavigationLink)
            ((NavigationLink)component).setValue(((NavigationLink)component).getLabel());
        else if (component instanceof NavOutputLink)
            ((NavOutputLink)component).setValue(((NavOutputLink)component).getAction());

        super.encodeBegin(context, component);
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        super.encodeChildren(context, component);
    }

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        if (component instanceof NavOutputLink)
            writer.write(((NavOutputLink)component).getLabel());
        super.encodeEnd(context, component);
        writer.endElement(HTML.DIV_ELEM);
        writer.endElement(HTML.TD_ELEM);
        NavigationMenu navMenu = findNavMenuParent(component);
        if (navMenu.getActiveItemImage() != null) {
            writer.startElement(HTML.TD_ELEM, navMenu);
            writer.writeAttribute(HTML.STYLE_ATTR, "padding-left:0;", null);
            writer.startElement(HTML.DIV_ELEM, navMenu);
            String imgDivClass = navMenu.getImageBorderClass();
            if (component instanceof NavigationLink && ((NavigationLink)component).isActive()) {
                imgDivClass = imgDivClass == null ? navMenu.getActiveItemClass() : imgDivClass + (navMenu.getActiveItemClass() == null ? "" : " " + navMenu.getActiveItemClass());
                if (imgDivClass != null)
                    writer.writeAttribute(HTML.CLASS_ATTR, imgDivClass, null);
                writer.startElement(HTML.IMG_ELEM, navMenu);
                writer.writeAttribute(HTML.SRC_ATTR, context.getExternalContext().getRequestContextPath() + navMenu.getActiveItemImage(), null);
                writer.writeAttribute(HTML.ALT_ATTR, "Selected", null);
                if (navMenu.getActiveItemImageClass() != null)
                    writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getActiveItemImageClass(), null);
                writer.endElement(HTML.IMG_ELEM);
            }
            else {
                imgDivClass = imgDivClass == null ? navMenu.getInActiveItemClass() : imgDivClass + (navMenu.getInActiveItemClass() == null ? "" : " " + navMenu.getInActiveItemClass());
                if (imgDivClass != null)
                    writer.writeAttribute(HTML.CLASS_ATTR, imgDivClass, null);
            }
            writer.endElement(HTML.DIV_ELEM);
            writer.endElement(HTML.TD_ELEM);
        }
        writer.endElement(HTML.TR_ELEM);
    }

    private NavigationMenu findNavMenuParent(UIComponent component) {
        if (component.getParent() == null)
            return null; // not found

        if (component.getParent() instanceof NavigationMenu)
            return (NavigationMenu) component.getParent();
        else
            return findNavMenuParent(component.getParent());
    }
}
