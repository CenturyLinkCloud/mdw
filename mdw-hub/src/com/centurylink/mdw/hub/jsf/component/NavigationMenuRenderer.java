/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.myfaces.shared.renderkit.html.HTML;

import com.centurylink.mdw.web.jsf.components.NavigationLink;
import com.centurylink.mdw.web.jsf.components.NavigationMenu;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * HTML renderer for a menu comprised of navigation links.
 * Reimplemented for HTML5 engine due to MyFaces shared impl repackaging.
 */
public class NavigationMenuRenderer extends Renderer {
    public static final String RENDERER_TYPE = NavigationMenuRenderer.class.getName();

    public static final String EXPANDED_ATTR = "data-mdw-navmenu-expanded";

    public boolean getRendersChildren() {
        return true;
    }

    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        NavigationMenu navMenu = (NavigationMenu) component;
        boolean active = determineActiveItem(facesContext, navMenu);
        boolean expanded = active || navMenu.isExpanded();

        ResponseWriter writer = facesContext.getResponseWriter();

        // outer div
        writer.startElement(HTML.DIV_ELEM, navMenu);
        writer.writeAttribute(HTML.ID_ATTR, component.getClientId(), null);
        if (navMenu.isCollapsible())
            writer.writeAttribute(EXPANDED_ATTR, expanded, null);
        if (navMenu.getStyleClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getStyleClass(), "styleClass");
        if (navMenu.getStyle() != null)
            writer.writeAttribute(HTML.STYLE_ATTR, navMenu.getStyle(), "style");

        List<NavigationMenu> sibMenus = navMenu.getNavMenuSiblings();
        for (NavigationMenu sibMenu : sibMenus) {
            if (determineActiveItem(facesContext, sibMenu)) {
                clearActiveItem(facesContext, navMenu); // i'm not active
            }
        }

        if (navMenu.isCollapsible())
            renderToggleJavaScript(facesContext, navMenu, active, sibMenus);

        if (navMenu.getLabel() != null && !navMenu.getLabel().isEmpty()) {
            // header div
            writer.startElement(HTML.DIV_ELEM, navMenu);
            if (!active && navMenu.isCollapsible()) {
                writer.writeAttribute(HTML.ONCLICK_ATTR, getToggleOnClickJavaScript(navMenu, !expanded, expanded), null);
            }

            if (active && navMenu.getActiveHeaderClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getActiveHeaderClass(), "activeHeaderClass");
            else if (navMenu.getHeaderClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getHeaderClass(), "headerClass");

            if (!active) {
                if (!expanded && navMenu.getExpandImage() != null) {
                    writer.startElement(HTML.DIV_ELEM, navMenu);
                    writer.startElement(HTML.IMG_ELEM, navMenu);
                    writer.writeAttribute(HTML.ID_ATTR, navMenu.getClientId() + "_image", null);
                    writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + navMenu.getExpandImage(), "expandImage");
                    writer.writeAttribute(HTML.ALT_ATTR, "Expand", null);
                    // writer.writeAttribute(HTML.ONCLICK_ATTR, getToggleOnClickJavaScript(navMenu), null);
                    if (navMenu.getExpandCollapseImageClass() != null)
                      writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getExpandCollapseImageClass(), "expandCollapseImageClass");
                    writer.endElement(HTML.IMG_ELEM);
                    writer.endElement(HTML.DIV_ELEM);
                }
                else if (expanded && navMenu.getCollapseImage() != null) {
                    writer.startElement(HTML.DIV_ELEM, navMenu);
                    writer.startElement(HTML.IMG_ELEM, navMenu);
                    writer.writeAttribute(HTML.ID_ATTR, navMenu.getClientId() + "_image", null);
                    writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + navMenu.getCollapseImage(), "collapseImage");
                    writer.writeAttribute(HTML.ALT_ATTR, "Collapse", null);
                    //writer.writeAttribute(HTML.ONCLICK_ATTR, getToggleOnClickJavaScript(navMenu), null);
                    if (navMenu.getExpandCollapseImageClass() != null)
                      writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getExpandCollapseImageClass(), "expandCollapseImageClass");
                    writer.endElement(HTML.IMG_ELEM);
                    writer.endElement(HTML.DIV_ELEM);
                }
            }

            if (navMenu.getLabel() != null) {
                writer.startElement(HTML.DIV_ELEM, navMenu);
                if (navMenu.getLabelClass() != null)
                    writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getLabelClass(), "labelClass");
                writer.writeText(navMenu.getLabel(), "label");
                writer.endElement(HTML.DIV_ELEM);
            }

            writer.endElement(HTML.DIV_ELEM);  // header div
        }

        // items table
        writer.startElement(HTML.TABLE_ELEM, navMenu);
        writer.writeAttribute(HTML.ID_ATTR, navMenu.getClientId() + "_itemGroup", null);
        if (!active && (navMenu.isCollapsible() && !expanded))
            writer.writeAttribute(HTML.STYLE_ATTR, "display:none;", null);
        if (navMenu.getItemGroupClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, navMenu.getItemGroupClass(), "itemGroupClass");
        writer.startElement(HTML.TBODY_ELEM, navMenu);
    }

    @Override
    public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException {
        if (component.getChildCount() > 0) {
            for (UIComponent child : component.getChildren()) {
                if (!child.isRendered())
                    continue;
                child.encodeAll(facesContext);
            }
        }
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        NavigationMenu navMenu = (NavigationMenu) component;

        if (!navMenu.checkRoleAccess(facesContext))
            return;

        ResponseWriter writer = facesContext.getResponseWriter();

        // items table
        writer.endElement(HTML.TBODY_ELEM);
        writer.endElement(HTML.TABLE_ELEM);

        // outer div
        writer.endElement(HTML.DIV_ELEM);
    }

    protected String getToggleOnClickJavaScript(NavigationMenu navMenu, boolean propagate) {
        return "toggleExpandState_" + navMenu.getId() + "(" + propagate + ");";
    }

    protected String getToggleOnClickJavaScript(NavigationMenu navMenu, boolean propagate, boolean originallyExpanded) {
        return "toggleExpandState_" + navMenu.getId() + "(" + propagate + ", " + originallyExpanded + ");";
    }


    protected void renderToggleJavaScript(FacesContext facesContext, NavigationMenu navMenu, boolean active, List<NavigationMenu> sibMenus) throws IOException {
        String ctxPath = facesContext.getExternalContext().getRequestContextPath();
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.write("\n");
        writer.startElement(HTML.SCRIPT_ELEM, navMenu);
        writer.writeAttribute(HTML.TYPE_ATTR, "text/javascript", null);
        StringBuffer script = new StringBuffer();
        script.append("\n");
        script.append("function toggleExpandState_" + navMenu.getId() + "(propagate, originallyExpanded)\n");
        script.append("{\n");
        if (!active) {
            script.append("  var me = document.getElementById('" + navMenu.getClientId() + "');\n");
            script.append("  var meExp = ('true' == me.getAttribute('" + EXPANDED_ATTR + "'));\n");
            script.append("  if (propagate || originallyExpanded || meExp) \n");
            script.append("  {\n");
            script.append("    var myImg = document.getElementById('" + navMenu.getClientId() + "_image');\n");
            script.append("    if (myImg != null)\n");
            script.append("      myImg.src='" + ctxPath + "' + (meExp ? '" + navMenu.getExpandImage() + "':'" + navMenu.getCollapseImage() + "');\n");
            script.append("    document.getElementById('" + navMenu.getClientId() + "_itemGroup').style.display = (meExp ? 'none' : 'inline');\n");
            script.append("    me.setAttribute('" + EXPANDED_ATTR + "', (meExp ? 'false' : 'true'));\n");
            script.append("    if (propagate && !meExp) // i'm expanding \n");
            script.append("    {\n");
            for (NavigationMenu sibMenu : sibMenus) {
                script.append("      " + getToggleOnClickJavaScript(sibMenu, false) + "\n");
            }
            script.append("    }\n");
            script.append("  }\n");
        }
        script.append("}\n");

        writer.write(script.toString());
        writer.endElement(HTML.SCRIPT_ELEM);
    }

    protected boolean determineActiveItem(FacesContext facesContext, NavigationMenu navMenu) {
        boolean active = false;
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> sessionMap = externalContext.getSessionMap();
        String sessionAttr = navMenu.getId() + "_selectedItem";

        // check whether menu item id is explicitly specified on the request
        Map<String, String> requestParamMap = externalContext.getRequestParameterMap();
        String reqItem = requestParamMap.get(sessionAttr);
        if (reqItem != null) {
            sessionMap.put(sessionAttr, reqItem);
            active = true;
        }
        else {
            // check whether the request corresponds to a link action
            String viewId = facesContext.getViewRoot().getViewId();
            for (NavigationLink navLink : navMenu.getNavLinkDescendants()) {
                String action = navLink.getActionExpression().getExpressionString();
                if (action != null) {
                    if (FacesVariableUtil.checkFromOutcomeLeadsToViewId(action, viewId)) {
                        sessionMap.put(sessionAttr, navLink.getId());
                        active = true;
                    }
                }
            }
        }

        String activeItemId = (String) sessionMap.get(sessionAttr);
        if (activeItemId != null)
            navMenu.setActiveItem(activeItemId);
        else if (navMenu.getDefaultItem() != null)
            navMenu.setActiveItem(navMenu.getDefaultItem());

        return active;
    }

    protected void clearActiveItem(FacesContext facesContext, NavigationMenu navMenu) {
        facesContext.getExternalContext().getSessionMap().remove(navMenu.getId() + "_selectedItem");
        navMenu.setActiveItem(null);
    }

}
